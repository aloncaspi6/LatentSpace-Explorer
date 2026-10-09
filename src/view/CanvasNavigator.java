// Manages navigation and point selection on the 2D canvas.
package view;

import javafx.scene.canvas.Canvas;
import model.LatentSpaceModel;
import model.WordVector;
import java.util.function.Consumer;

// אחראי בלעדית על הניווט בקנבס הדו-ממדי: גרירה (pan), זום, וזיהוי לחיצה על נקודה
// MainView נשאר אחראי רק על הרכבת המסך וניהול המצב - כל חישובי העכבר עברו לכאן
// כשמזוהה לחיצה על מילה - הניווטור רק מדווח דרך callback, ההחלטה מה לעשות נשארת ב-MainView
public class CanvasNavigator {

    private final Canvas canvas;
    private final CanvasRenderer renderer;
    private final LatentSpaceModel model;

    // callback ל-MainView: נקרא כשהמשתמש לוחץ על מילה בקנבס
    // הניווטור לא מחליט מה עושים עם הלחיצה (focus רגיל או בחירה ל-Vector Lab)
    private final Consumer<String> onWordClicked;

    // callback לציור מחדש - נקרא אחרי כל גרירה או זום
    private final Runnable redraw;

    // שמירת נקודת ההתחלה של הגרירה וההיסט באותו רגע
    private double dragStartX, dragStartY;
    private double offsetXAtDrag, offsetYAtDrag;
    private double offsetX = 0;
    private double offsetY = 0;
    private double scale = 1.0;
    // דגל שמונע בחירת מילה בסוף גרירה - גרירה היא לא לחיצה
    private boolean dragging = false;

    public CanvasNavigator(Canvas canvas, CanvasRenderer renderer, LatentSpaceModel model,
                           Consumer<String> onWordClicked, Runnable redraw) {
        this.canvas        = canvas;
        this.renderer      = renderer;
        this.model         = model;
        this.onWordClicked = onWordClicked;
        this.redraw        = redraw;
    }

    // רושם את כל אירועי העכבר על הקנבס
    // נקרא בהפעלה הראשונה וגם בחזרה ממצב 3D (שמחליף את האירועים בשלו)
    public void activate() {
        // לחיצה - שומרים את נקודת ההתחלה ואת ההיסט הנוכחי לקראת גרירה אפשרית
        canvas.setOnMousePressed(e -> {
            dragStartX    = e.getX();
            dragStartY    = e.getY();
            offsetXAtDrag = offsetX;
            offsetYAtDrag = offsetY;
            dragging      = false;
        });

        // גרירה - מזיזים את ההיסט לפי תזוזת העכבר (מחולק בסקייל כדי שהתנועה תתאים לזום)
        canvas.setOnMouseDragged(e -> {
            double dx = e.getX() - dragStartX;
            double dy = e.getY() - dragStartY;
            // נסמן גרירה רק אחרי תזוזה של 3 פיקסלים - כדי לא לפספס לחיצות רגילות
            if (Math.abs(dx) > 3 || Math.abs(dy) > 3) {
                dragging = true;
                offsetX = offsetXAtDrag + dx / scale;
                offsetY = offsetYAtDrag + dy / scale;
                redraw.run();
            }
        });

        // לחיצה - מזהים את המילה הקרובה ביותר לנקודת הלחיצה ומדווחים ל-MainView
        canvas.setOnMouseClicked(e -> {
            // אם גררנו - זו לא לחיצה, מתעלמים
            if (dragging) return;
            String word = findWordAt(e.getX(), e.getY());
            if (word != null) onWordClicked.accept(word);
        });

        // זום עם גלגלת העכבר - הנקודה מתחת לעכבר נשארת במקומה
        canvas.setOnScroll(e -> {
            double zoomFactor = e.getDeltaY() > 0 ? 1.05 : 0.95;
            double mouseX     = e.getX();
            double mouseY     = e.getY();
            double s          = scale;
            offsetX -= mouseX / s - mouseX / (s * zoomFactor);
            offsetY -= mouseY / s - mouseY / (s * zoomFactor);
            scale = s * zoomFactor;
            redraw.run();
        });
    }

    // מוצא את המילה שהנקודה שלה על המסך הכי קרובה לנקודת הלחיצה
    // מחזיר null אם אין מילה במרחק של עד 10 פיקסלים - כדי למנוע בחירה מקרית
    private String findWordAt(double mx, double my) {
        double[] bounds = renderer.computeBounds();
        // הצירים הנוכחיים נלקחים מה-renderer - כך הניווטור תמיד מסונכרן עם התצוגה
        int pcaX = renderer.getPcaX();
        int pcaY = renderer.getPcaY();

        WordVector closest     = null;
        double     closestDist = 10;
        // עוברים על כל המילים וממירים כל אחת לנקודת מסך, בודקים מרחק מהעכבר
        for (WordVector wv : model.getWords().values()) {
            double px = renderer.toScreenX(wv.getPcaVector()[pcaX], bounds);
            double py = renderer.toScreenY(wv.getPcaVector()[pcaY], bounds);
            double d  = Math.hypot(px - mx, py - my);
            if (d < closestDist) {
                closestDist = d;
                closest     = wv;
            }
        }
        return closest == null ? null : closest.getWord();
    }

    // מזיז את ההיסט כך שהמילה הנתונה תופיע במרכז הקנבס
    // עבר לכאן מ-MainView כי זה חישוב ניווט - לא ניהול מצב
    public void centerOnWord(String word) {
        WordVector wv = model.getWord(word);
        if (wv == null) return;
        double[] bounds = renderer.computeBounds();
        double rawX = renderer.toRawScreenX(wv.getPcaVector()[renderer.getPcaX()], bounds);
        double rawY = renderer.toRawScreenY(wv.getPcaVector()[renderer.getPcaY()], bounds);
        offsetX = (canvas.getWidth()  / 2.0 / scale) - rawX;
        offsetY = (canvas.getHeight() / 2.0 / scale) - rawY;
    }

    public double getOffsetX() { return offsetX; }
    public double getOffsetY() { return offsetY; }
    public double getScale()   { return scale;   }
}
