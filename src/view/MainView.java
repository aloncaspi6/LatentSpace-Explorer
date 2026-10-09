package view;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import model.JsonVectorSource;
import model.LatentSpaceModel;
import model.VectorSource;
import model.WordVector;
import java.util.List;
import java.util.function.Consumer;

// אחראי בלעדית על: הרכבת ה-layout, אירועי קנבס, וניווט (זום/גרירה/לחיצה)
// כל ציור מואצל ל-CanvasRenderer
// כל פאנל UI בנוי על ידי מחלקה נפרדת: ControlsPanel, DistancePanel, InfoPanel
// כל חלון נפרד מנוהל על ידי מחלקת View משלו
public class MainView extends Application {

    private LatentSpaceModel model;
    private Canvas canvas;
    private GraphicsContext gc;

    private int pcaX = 0;
    private int pcaY = 1;
    private String focusedWord = null;
    private int kNeighbors = 5;

    private CanvasRenderer renderer;
    private InfoPanel infoPanel;

    private double dragStartX, dragStartY;
    private double offsetXAtDrag, offsetYAtDrag;
    private boolean dragging = false;

    private static final int WIDTH  = 900;
    private static final int HEIGHT = 700;

    private List<String> pathWords = null;

    private boolean is3D   = false;
    private View3D  view3D = null;
    private VBox root;

    // כשחלון ArithmeticView פתוח במצב "בחר מהמפה",
    // הוא מכניס לכאן Consumer<String> שמקבל כל מילה שנלחצת על הקנבס.
    // כשהמצב מסתיים (Done / סגירת חלון) — מוחזר null.
    private Consumer<String> mapPickListener = null;

    @Override
    public void start(Stage stage) throws Exception {
        VectorSource source = new JsonVectorSource("full_vectors.json", "pca_vectors.json");
        model = new LatentSpaceModel(source.load());

        canvas    = new Canvas(WIDTH, HEIGHT);
        gc        = canvas.getGraphicsContext2D();
        renderer  = new CanvasRenderer(canvas, gc, model, pcaX, pcaY);
        infoPanel = new InfoPanel(model);

        HBox controls    = new ControlsPanel(model,
                this::focusWord,
                this::onAxesChanged,
                this::onKChanged,
                this::showPath,
                this::setMapPickListener).build();

        HBox distanceBar = new DistancePanel(model, this::enter3DMode).build();
        HBox mainArea    = buildMainArea();

        root = new VBox(controls, distanceBar, mainArea);
        stage.setScene(new Scene(root));
        stage.setTitle("LatentSpace Explorer");
        stage.show();

        drawCanvas();
    }

    // ---- בניית אזור הקנבס + פאנל הצד ----

    private HBox buildMainArea() {
        setupCanvasEvents();
        return new HBox(canvas, infoPanel.getContainer());
    }

    // ---- callbacks מהפאנלים ----

    private void onAxesChanged(int x, int y) {
        pcaX = x;
        pcaY = y;
        renderer.setPcaAxes(pcaX, pcaY);
        drawCanvas();
    }

    private void onKChanged(int k) {
        kNeighbors = k;
        drawCanvas();
    }

    // ---- אירועי הקנבס ----

    private void setupCanvasEvents() {
        canvas.setOnMousePressed(e -> {
            dragStartX    = e.getX();
            dragStartY    = e.getY();
            offsetXAtDrag = renderer.getOffsetX();
            offsetYAtDrag = renderer.getOffsetY();
            dragging      = false;
        });

        canvas.setOnMouseDragged(e -> {
            double dx = e.getX() - dragStartX;
            double dy = e.getY() - dragStartY;
            if (Math.abs(dx) > 3 || Math.abs(dy) > 3) {
                dragging = true;
                renderer.setOffset(
                        offsetXAtDrag + dx / renderer.getScale(),
                        offsetYAtDrag + dy / renderer.getScale()
                );
                drawCanvas();
            }
        });

        canvas.setOnMouseClicked(e -> {
            if (dragging) return;
            double mx = e.getX();
            double my = e.getY();
            double[] bounds = renderer.computeBounds();

            // מוצאים את המילה הקרובה ביותר לנקודת הלחיצה
            WordVector closest     = null;
            double     closestDist = 10;
            for (WordVector wv : model.getWords().values()) {
                double px = renderer.toScreenX(wv.getPcaVector()[pcaX], bounds);
                double py = renderer.toScreenY(wv.getPcaVector()[pcaY], bounds);
                double d  = Math.hypot(px - mx, py - my);
                if (d < closestDist) {
                    closestDist = d;
                    closest     = wv;
                }
            }

            if (closest == null) return;

            // אם ArithmeticView מחכה לבחירה מהמפה — נשלח לשם ולא נבצע focusWord רגיל
            if (mapPickListener != null) {
                mapPickListener.accept(closest.getWord());
            } else {
                focusWord(closest.getWord());
            }
        });

        canvas.setOnScroll(e -> {
            double zoomFactor = e.getDeltaY() > 0 ? 1.05 : 0.95;
            double mouseX     = e.getX();
            double mouseY     = e.getY();
            double s          = renderer.getScale();
            renderer.setOffset(
                    renderer.getOffsetX() - (mouseX / s - mouseX / (s * zoomFactor)),
                    renderer.getOffsetY() - (mouseY / s - mouseY / (s * zoomFactor))
            );
            renderer.setScale(s * zoomFactor);
            drawCanvas();
        });
    }

    // ---- פעולות על הקנבס ----

    public void focusWord(String word) {
        if (model.getWord(word) == null) return;
        pathWords   = null;
        focusedWord = word;
        if (is3D && view3D != null) {
            view3D.setFocusedWord(word);
            view3D.drawScene();
        } else {
            centerOnWord(word);
            drawCanvas();
        }
        infoPanel.update(focusedWord, kNeighbors);
    }

    public void showPath(List<String> path) {
        pathWords   = path;
        focusedWord = path.get(path.size() - 1);
        centerOnWord(focusedWord);
        drawCanvas();
        infoPanel.update(focusedWord, kNeighbors);
    }

    private void centerOnWord(String word) {
        WordVector wv = model.getWord(word);
        if (wv == null) return;
        double[] bounds = renderer.computeBounds();
        double rawX = renderer.toRawScreenX(wv.getPcaVector()[pcaX], bounds);
        double rawY = renderer.toRawScreenY(wv.getPcaVector()[pcaY], bounds);
        renderer.setOffset(
                (WIDTH  / 2.0 / renderer.getScale()) - rawX,
                (HEIGHT / 2.0 / renderer.getScale()) - rawY
        );
    }

    private void drawCanvas() {
        renderer.drawCanvas(focusedWord, kNeighbors, pathWords);
    }

    // ---- מצב בחירה מהמפה ----

    // ArithmeticView קורא לפונקציה הזו דרך ה-setMapPickListener callback:
    // listener != null  → מצב בחירה פעיל, כל לחיצה על הקנבס הולכת ל-ArithmeticView
    // listener == null  → מצב רגיל, לחיצה מבצעת focusWord
    public void setMapPickListener(Consumer<String> listener) {
        this.mapPickListener = listener;
    }

    // ---- מעבר בין מצב 2D ל-3D ----

    private void enter3DMode() {
        if (is3D) return;
        is3D = true;
        view3D = new View3D(model, canvas, gc, infoPanel.getContainer(), pcaX, pcaY, kNeighbors,
                this::focusWord, this::exit3DMode);
        root.getChildren().add(2, view3D.getBar3D());
        view3D.activate();
    }

    private void exit3DMode() {
        if (!is3D) return;
        is3D = false;
        root.getChildren().remove(view3D.getBar3D());
        view3D = null;
        setupCanvasEvents();
        drawCanvas();
    }

    public static void main(String[] args) {
        launch(args);
    }
}