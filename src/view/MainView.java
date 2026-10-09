// Builds the main interface and coordinates application state.
package view;

import command.ChangeSelectionCommand;
import command.CommandManager;
import javafx.beans.binding.Bindings;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Alert;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import model.JsonVectorSource;
import model.LatentSpaceModel;
import model.PythonEmbedder;
import model.VectorSource;
import java.util.List;
import java.util.function.Consumer;

// אחראי בלעדית על: הרכבת ה-layout וניהול המצב (בחירה, צירים, מצב 2D/3D)
// כל ציור מואצל ל-CanvasRenderer
// כל ניווט בעכבר (גרירה/זום/לחיצה) מואצל ל-CanvasNavigator
// כל פאנל UI בנוי על ידי מחלקה נפרדת: ControlsPanel, DistancePanel, InfoPanel
// כל חלון נפרד מנוהל על ידי מחלקת View משלו
// כל שינוי בחירה (מילה/מסלול) עובר דרך CommandManager כדי לאפשר Undo/Redo
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

    // אחראי על כל הניווט בקנבס - MainView רק מקבל ממנו דיווח על לחיצות
    private CanvasNavigator navigator;

    // מנהל הפעולות המרכזי - כל הפקודות במערכת עוברות דרכו
    private final CommandManager commandManager = new CommandManager();

    private static final int WIDTH  = 900;
    private static final int HEIGHT = 700;

    private List<String> pathWords = null;

    private boolean is3D   = false;
    private View3D  view3D = null;
    private VBox root;

    // כשחלון VectorLabView פתוח במצב "בחר מהמפה",
    // הוא מכניס לכאן Consumer<String> שמקבל כל מילה שנלחצת על הקנבס.
    // כשהמצב מסתיים (Done / סגירת חלון) — מוחזר null.
    private Consumer<String> mapPickListener = null;

    @Override
    public void start(Stage stage) {
        // שלב 1: הרצת סקריפט הפייתון שיוצר את קבצי הווקטורים (אם הם עוד לא קיימים)
        // הג'אווה מפעילה את הפייתון כתהליך חיצוני ומחכה לסיומו
        try {
            PythonEmbedder.ensureVectors("embedder.py", "full_vectors.json", "pca_vectors.json");
        } catch (Exception ex) {
            new Alert(Alert.AlertType.ERROR,
                    "Vector files could not be generated:\n" + ex.getMessage()).showAndWait();
            return;
        }

        // שלב 2: טעינת הווקטורים - אם הקובץ חסר או פגום נציג הודעה ברורה ולא נקרוס
        try {
            VectorSource source = new JsonVectorSource("full_vectors.json", "pca_vectors.json");
            model = new LatentSpaceModel(source.load());
        } catch (Exception ex) {
            new Alert(Alert.AlertType.ERROR,
                    "Failed to load vector files:\n" + ex.getMessage() +
                            "\n\nMake sure embedder.py ran successfully and the JSON files exist.").showAndWait();
            return;
        }

        canvas    = new Canvas(WIDTH, HEIGHT);
        gc        = canvas.getGraphicsContext2D();
        renderer  = new CanvasRenderer(canvas, gc, model, pcaX, pcaY);
        infoPanel = new InfoPanel(model);

        // הניווטור מקבל callback ללחיצה על מילה ו-callback לציור מחדש
        navigator = new CanvasNavigator(canvas, renderer, model,
                this::onCanvasWordClicked, this::drawCanvas);
        renderer.setNavigator(navigator);

        // ControlsPanel מקבל את מנהל הפעולות כדי לעטוף שינויי צירים ומטריקה בפקודות
        HBox controls    = new ControlsPanel(model,
                commandManager,
                this::focusWord,
                this::onAxesChanged,
                this::onKChanged,
                this::showPath,
                this::setMapPickListener).build();

        HBox distanceBar = new DistancePanel(model, this::enter3DMode).build();

        // הפעלת הניווטור על הקנבס והרכבת האזור המרכזי
        navigator.activate();
        HBox mainArea = new HBox(canvas, infoPanel.getContainer());
        mainArea.setStyle("-fx-background-color: black;");

        root = new VBox(controls, distanceBar, mainArea);
        Scene scene = new Scene(root);

        // קיצורי מקלדת: Ctrl+Z לביטול, Ctrl+Y לביצוע מחדש
        scene.getAccelerators().put(
                new KeyCodeCombination(KeyCode.Z, KeyCombination.SHORTCUT_DOWN),
                commandManager::undo);
        scene.getAccelerators().put(
                new KeyCodeCombination(KeyCode.Y, KeyCombination.SHORTCUT_DOWN),
                commandManager::redo);

        stage.setScene(scene);
        stage.setTitle("LatentSpace Explorer");
        stage.show();

        canvas.widthProperty().addListener((obs, oldWidth, newWidth) -> {
            if (is3D && view3D != null) {
                view3D.drawScene();
            } else {
                drawCanvas();
            }
        });
        canvas.widthProperty().bind(Bindings.max(0,
                mainArea.widthProperty().subtract(infoPanel.getContainer().widthProperty())));

        drawCanvas();
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

    // ---- דיווח מהניווטור על לחיצה על מילה ----

    // הניווטור זיהה לחיצה על מילה - כאן מחליטים מה עושים איתה:
    // אם VectorLabView מחכה לבחירה מהמפה — נשלח לשם, אחרת focusWord רגיל
    private void onCanvasWordClicked(String word) {
        if (mapPickListener != null) {
            mapPickListener.accept(word);
        } else {
            focusWord(word);
        }
    }

    // ---- פעולות על הקנבס (עטופות בפקודות) ----

    // נקרא מכל נקודות הכניסה בממשק: חיפוש, לחיצה על נקודה, תוצאות Subspace
    // עוטף את שינוי הבחירה בפקודה ששומרת את המצב הקודם - כך אפשר לבטל
    public void focusWord(String word) {
        // אם המילה לא קיימת - מציגים הודעה במקום להתעלם בשקט
        if (model.getWord(word) == null) {
            new Alert(Alert.AlertType.WARNING, "Word not found: " + word).show();
            return;
        }
        commandManager.executeCommand(new ChangeSelectionCommand(
                focusedWord, pathWords,   // המצב הישן - נשמר בשביל undo
                word, null,               // המצב החדש - בחירת מילה מאפסת את המסלול
                this::applySelection));
    }

    // נקרא מ-VectorLabView כשמחושבת משוואה - מציג את המסלול הווקטורי
    public void showPath(List<String> path) {
        commandManager.executeCommand(new ChangeSelectionCommand(
                focusedWord, pathWords,               // המצב הישן
                path.get(path.size() - 1), path,      // החדש: התוצאה נבחרת + המסלול מוצג
                this::applySelection));
    }

    // מבצע את שינוי הבחירה בפועל - הפקודות קוראות לפונקציה הזו ב-execute וב-undo
    // רק כאן משתנה המצב; יצירת פקודות קורית רק בנקודות הכניסה מהממשק
    // כך undo לא יוצר פקודה חדשה בטעות (מניעת לולאה אינסופית)
    private void applySelection(String word, List<String> path) {
        focusedWord = word;
        pathWords   = path;
        if (is3D && view3D != null) {
            if (word != null) view3D.setFocusedWord(word);
            view3D.drawScene();
        } else {
            // המרכוז מואצל לניווטור - הוא אחראי על כל חישובי המסך
            if (word != null) navigator.centerOnWord(word);
            drawCanvas();
        }
        infoPanel.update(focusedWord, kNeighbors);
    }

    private void drawCanvas() {
        renderer.drawCanvas(focusedWord, kNeighbors, pathWords);
    }

    // ---- מצב בחירה מהמפה ----

    // VectorLabView קורא לפונקציה הזו דרך ה-setMapPickListener callback:
    // listener != null  → מצב בחירה פעיל, כל לחיצה על הקנבס הולכת ל-VectorLabView
    // listener == null  → מצב רגיל, לחיצה מבצעת focusWord
    public void setMapPickListener(Consumer<String> listener) {
        this.mapPickListener = listener;
    }

    // ---- מעבר בין מצב 2D ל-3D ----

    private void enter3DMode() {
        if (is3D) return;
        is3D = true;
        view3D = new View3D(model, canvas, gc, pcaX, pcaY, kNeighbors,
                this::focusWord, this::exit3DMode);
        root.getChildren().add(2, view3D.getBar3D());
        view3D.activate();
    }

    private void exit3DMode() {
        if (!is3D) return;
        is3D = false;
        root.getChildren().remove(view3D.getBar3D());
        view3D = null;
        // חזרה ל-2D: הניווטור רושם מחדש את אירועי העכבר שלו על הקנבס
        navigator.activate();
        drawCanvas();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
