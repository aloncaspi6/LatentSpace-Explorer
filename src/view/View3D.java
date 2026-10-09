package view;

import javafx.geometry.Insets;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import model.LatentSpaceModel;
import model.WordVector;
import java.util.function.Consumer;

// אחראי בלעדית על מצב ה-3D של הקנבס הראשי
// לא פותח חלון חדש — עובד על אותו קנבס, infoPanel ו-topBar שמגיעים מ-MainView
// MainView מעבירה את הרכיבים בעת מעבר למצב 3D, ומחזירה אליה את השליטה ביציאה
// כל ציור מואצל ל-Scene3DRenderer
public class View3D {

    private final LatentSpaceModel model;

    // הרכיבים של MainView שעליהם View3D פועלת ישירות
    private final Canvas canvas;
    private final VBox infoPanel;

    // אחראי על כל הציור וחישובי ההיטל הפרספקטיבי
    private final Scene3DRenderer renderer;

    // הפאנל שמתווסף לשורה השנייה בזמן מצב 3D (ספינר ציר Z + כפתור יציאה)
    private final HBox bar3D;

    // callback לחזרה למצב 2D — נקרא כשלוחצים Exit 3D
    private final Runnable onExit;

    // המילה שנבחרה על ידי לחיצה
    private String focusedWord = null;

    // כמות שכנים להציג
    private int kNeighbors;

    // שמירת נקודת הלחיצה האחרונה לצורך גרירה
    private double lastMouseX, lastMouseY;
    // דגל שמונע בחירת מילה בסוף גרירה
    private boolean dragging = false;

    // callback לעדכון המילה הנבחרת — קורא ל-focusWord של MainView כדי שה-infoPanel יתעדכן
    private final Consumer<String> onWordSelected;

    public View3D(LatentSpaceModel model, Canvas canvas, GraphicsContext gc,
                  VBox infoPanel, int pcaX, int pcaY, int kNeighbors,
                  Consumer<String> onWordSelected, Runnable onExit) {
        this.model         = model;
        this.canvas        = canvas;
        this.infoPanel     = infoPanel;
        this.kNeighbors    = kNeighbors;
        this.onWordSelected = onWordSelected;
        this.onExit        = onExit;

        // יצירת ה-renderer — מכאן והלאה כל ציור עובר דרכו
        // נורש את צירי ה-PCA ו-K מהמצב הנוכחי של MainView
        renderer = new Scene3DRenderer(canvas, gc, model, pcaX, pcaY, 2);

        // ---- בניית פאנל ה-3D שיתווסף כשורה שלישית ----
        // רק ציר Z + כפתור יציאה — שאר הפיצ'רים (חיפוש, מרחק, K, arithmetic וכו') נשארים בשורות 1 ו-2 כרגיל
        Spinner<Integer> spinZ = new Spinner<>(0, 49, 2);
        spinZ.valueProperty().addListener((obs, old, val) -> { renderer.setPcaZ(val); drawScene(); });

        // כפתור יציאה מ-3D — מחזיר את השליטה ל-MainView
        Button exitBtn = new Button("Exit 3D");
        exitBtn.setStyle("-fx-base: #f38ba8;");
        exitBtn.setOnAction(e -> onExit.run());

        Label modeLabel = new Label("🌐 3D Mode — drag to rotate, scroll to zoom");
        modeLabel.setStyle("-fx-text-fill: #89dceb; -fx-font-style: italic;");

        bar3D = new HBox(10,
                new Label("PC-Z:"), spinZ,
                new Separator(),
                modeLabel,
                new Separator(),
                exitBtn
        );
        bar3D.setPadding(new Insets(6, 8, 6, 8));
        bar3D.setStyle("-fx-background-color: #181825;");
        for (javafx.scene.Node n : bar3D.getChildren()) {
            if (n instanceof Label l && !l.getStyle().contains("#89dceb"))
                l.setStyle("-fx-text-fill: #cdd6f4;");
        }
    }

    // מחזיר את פאנל ה-3D — MainView מוסיף אותו לתצוגה
    public HBox getBar3D() {
        return bar3D;
    }

    // נקרא מ-MainView כשעוברים למצב 3D — רושם אירועים ומצייר
    public void activate() {
        // גרירה — סיבוב הסצנה (מחליף את התנהגות הגרירה הרגילה של MainView)
        canvas.setOnMousePressed(e -> {
            lastMouseX = e.getX();
            lastMouseY = e.getY();
            // איפוס דגל הגרירה בכל לחיצה חדשה
            dragging = false;
        });

        canvas.setOnMouseDragged(e -> {
            double dx = e.getX() - lastMouseX;
            double dy = e.getY() - lastMouseY;
            // נסמן גרירה רק אחרי תזוזה של 3 פיקסלים — כמו ב-MainView
            if (Math.abs(dx) > 3 || Math.abs(dy) > 3) {
                dragging = true;
            }
            // עדכון הסיבוב לפי תזוזת העכבר (חלוקה ב-200 לריכוך התנועה)
            renderer.addRotY(dx / 200.0);
            renderer.addRotX(dy / 200.0);
            lastMouseX = e.getX();
            lastMouseY = e.getY();
            drawScene();
        });

        canvas.setOnMouseClicked(e -> {
            // אם גררנו — נתעלם מהלחיצה
            if (dragging) return;
            selectWordAt(e.getX(), e.getY());
        });

        // זום עם גלגלת העכבר — הנקודה מתחת לעכבר נשארת במקומה
        canvas.setOnScroll(e -> {
            double factor = e.getDeltaY() > 0 ? 1.05 : 0.95;
            // מיקום העכבר יחסית למרכז הקנבס
            double mx = e.getX() - renderer.getW() / 2.0;
            double my = e.getY() - renderer.getH() / 2.0;
            // screenX = W/2 + panX + contrib*zoom
            // רוצים שהנקודה מתחת לעכבר (screenX=mouseX) תישאר:
            // panX_new + contrib*zoom*factor = panX_old + contrib*zoom
            // contrib = (mouseX - W/2 - panX) / zoom  (קבוע)
            // => panX_new = panX_old + (mx - panX) * (1 - factor)
            renderer.setPan(
                    renderer.getPanX() + (mx - renderer.getPanX()) * (1 - factor),
                    renderer.getPanY() + (my - renderer.getPanY()) * (1 - factor)
            );
            renderer.addZoom(factor);
            drawScene();
        });

        drawScene();
    }

    // מאפשר ל-MainView לעדכן את המילה הנבחרת ומרכז אותה בסצנה
    public void setFocusedWord(String word) {
        this.focusedWord = word;
        renderer.centerOnWord(word);
    }

    // מואצל ל-Scene3DRenderer — View3D לא מכיל יותר לוגיקת ציור
    public void drawScene() {
        renderer.drawScene(focusedWord, kNeighbors);
    }

    // בוחר את המילה הכי קרובה לנקודת הלחיצה על הקנבס
    private void selectWordAt(double mx, double my) {
        WordVector closest = null;
        double minDist = Double.MAX_VALUE;

        for (Object[] entry : renderer.getProjectedPoints()) {
            WordVector wv = (WordVector) entry[0];
            double px = (double) entry[1];
            double py = (double) entry[2];
            double d  = Math.hypot(px - mx, py - my);
            if (d < minDist) {
                minDist = d;
                closest = wv;
            }
        }

        // נקבל לחיצה רק אם המרחק קטן מ-10 פיקסלים — כדי למנוע בחירה מקרית
        if (closest != null && minDist < 10) {
            focusedWord = closest.getWord();
            // נודיע ל-MainView על הבחירה — הוא יעדכן את ה-infoPanel
            onWordSelected.accept(focusedWord);
            drawScene();
        }
    }
}