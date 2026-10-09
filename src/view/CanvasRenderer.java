package view;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import model.LatentSpaceModel;
import model.WordVector;
import java.util.List;

// אחראי בלעדית על ציור הקנבס וחישובי קואורדינטות
// MainView מחזיק instance של מחלקה זו ומשתמש בה לכל פעולת ציור
public class CanvasRenderer {

    private final Canvas canvas;
    private final GraphicsContext gc;
    private final LatentSpaceModel model;

    private int pcaX;
    private int pcaY;
    private double offsetX = 0;
    private double offsetY = 0;
    private double scale  = 1.0;

    private static final double POINT_RADIUS = 3;

    public CanvasRenderer(Canvas canvas, GraphicsContext gc, LatentSpaceModel model,
                          int pcaX, int pcaY) {
        this.canvas = canvas;
        this.gc     = gc;
        this.model  = model;
        this.pcaX   = pcaX;
        this.pcaY   = pcaY;
    }

    // ---- setters לעדכון מצב מ-MainView ----

    public void setPcaAxes(int pcaX, int pcaY) {
        this.pcaX = pcaX;
        this.pcaY = pcaY;
    }

    public void setOffset(double offsetX, double offsetY) {
        this.offsetX = offsetX;
        this.offsetY = offsetY;
    }

    public void setScale(double scale) { this.scale = scale; }

    public double getOffsetX() { return offsetX; }
    public double getOffsetY() { return offsetY; }
    public double getScale()   { return scale;   }
    public int    getPcaX()    { return pcaX;    }
    public int    getPcaY()    { return pcaY;    }

    // ---- ציור ראשי ----

    // יוצרת את הקנבס המלא, צבע רק כהה לפי הגדלים שדרשנו
    public void drawCanvas(String focusedWord, int kNeighbors, List<String> pathWords) {
        double W = canvas.getWidth();
        double H = canvas.getHeight();

        gc.setFill(Color.web("#1e1e2e"));
        gc.fillRect(0, 0, W, H);

        // מחשב את המינימום ומקסימום של הנקודות, ממיר אותם לנקודות על הקנבס
        double[] bounds = computeBounds();

        // אם מילה נבחרה תיקח את הווקטור שלה ואת ה-k שכנים שלה
        List<WordVector> neighbors = null;
        WordVector focused = focusedWord != null ? model.getWord(focusedWord) : null;
        if (focused != null) {
            neighbors = model.getNearestNeighbors(focusedWord, kNeighbors);
        }

        // עובר על כל הווקטורים, לכל ווקטור ממיר את הקורדינטות לנקודה על המסך
        for (WordVector wv : model.getWords().values()) {
            double px = toScreenX(wv.getPcaVector()[pcaX], bounds);
            double py = toScreenY(wv.getPcaVector()[pcaY], bounds);

            // שני דגלים, האם הווקטור הנתון הוא הנבחר או השכנים
            boolean isFocused  = wv == focused;
            boolean isNeighbor = neighbors != null && neighbors.contains(wv);

            // אם הוא הנבחר, תגדיל אותו, תצבע בכתום ושים את המילה שהוא מייצג ליד
            if (isFocused) {
                gc.setFill(Color.web("#ffaa00"));
                gc.fillOval(px - 6, py - 6, 12, 12);
                gc.fillText(wv.getWord(), px + 8, py);
                // אם הוא שכן, תצבע בירוק, תגדיל ושים את המילה ליד
            } else if (isNeighbor) {
                gc.setFill(Color.web("#40ff80"));
                gc.fillOval(px - 4, py - 4, 8, 8);
                gc.fillText(wv.getWord(), px + 6, py);
                double fx = toScreenX(focused.getPcaVector()[pcaX], bounds);
                double fy = toScreenY(focused.getPcaVector()[pcaY], bounds);
                gc.setStroke(Color.web("#89dceb", 0.4));
                gc.strokeLine(px, py, fx, fy);
                // אחרת תצבע בצבע רגיל, בלי המילה ליד
            } else {
                gc.setFill(Color.web("#cdd6f4", 0.6));
                gc.fillOval(px - POINT_RADIUS, py - POINT_RADIUS, POINT_RADIUS * 2, POINT_RADIUS * 2);
            }
        }

        // ציור המסלול הווקטורי אם קיים — חצים בין נקודות V1 → V2 → V3 → result
        if (pathWords != null && pathWords.size() >= 2) {
            drawPath(pathWords, bounds);
        }
    }

    // מצייר חצים בין נקודות המסלול על הקנבס
    // כל נקודה מסומנת בעיגול סגול, עם תווית שם המילה ומספר שלב
    private void drawPath(List<String> pathWords, double[] bounds) {
        gc.setStroke(Color.web("#cc88ff", 0.9));
        gc.setLineWidth(2);

        for (int i = 0; i < pathWords.size(); i++) {
            WordVector wv = model.getWord(pathWords.get(i));
            if (wv == null) continue;

            double px = toScreenX(wv.getPcaVector()[pcaX], bounds);
            double py = toScreenY(wv.getPcaVector()[pcaY], bounds);

            // עיגול סגול לכל נקודת מסלול
            gc.setFill(Color.web("#cc88ff"));
            gc.fillOval(px - 5, py - 5, 10, 10);

            // תווית: מספר שלב + שם המילה
            String label = (i == pathWords.size() - 1)
                    ? "→ " + wv.getWord()        // התוצאה מסומנת בחץ
                    : "V" + (i + 1) + ": " + wv.getWord();
            gc.fillText(label, px + 8, py - 4);

            // קו-חץ מהנקודה הנוכחית לבאה
            if (i < pathWords.size() - 1) {
                WordVector next = model.getWord(pathWords.get(i + 1));
                if (next == null) continue;
                double nx = toScreenX(next.getPcaVector()[pcaX], bounds);
                double ny = toScreenY(next.getPcaVector()[pcaY], bounds);
                gc.strokeLine(px, py, nx, ny);
                drawArrowHead(px, py, nx, ny);
            }
        }
        gc.setLineWidth(1);
    }

    // מצייר ראש חץ בקצה הקו
    private void drawArrowHead(double x1, double y1, double x2, double y2) {
        double angle     = Math.atan2(y2 - y1, x2 - x1);
        double arrowSize = 8;
        gc.setFill(Color.web("#cc88ff", 0.9));
        double ax1 = x2 - arrowSize * Math.cos(angle - Math.PI / 6);
        double ay1 = y2 - arrowSize * Math.sin(angle - Math.PI / 6);
        double ax2 = x2 - arrowSize * Math.cos(angle + Math.PI / 6);
        double ay2 = y2 - arrowSize * Math.sin(angle + Math.PI / 6);
        gc.fillPolygon(new double[]{x2, ax1, ax2}, new double[]{y2, ay1, ay2}, 3);
    }

    // ---- חישובי קואורדינטות ----

    // חישוב גבולות של הנקודות על המסך, כדי לשמור על סדר גודל בין נקודות
    public double[] computeBounds() {
        // נתחיל במרחק הכי גדול שאפשר לשמור
        double minX = Double.MAX_VALUE, maxX = -Double.MAX_VALUE;
        double minY = Double.MAX_VALUE, maxY = -Double.MAX_VALUE;
        // נעבור על כל המילים ונעדכן את המקסימלי ומינימלי בכל ציר
        for (WordVector wv : model.getWords().values()) {
            double x = wv.getPcaVector()[pcaX];
            double y = wv.getPcaVector()[pcaY];
            if (x < minX) minX = x;
            if (x > maxX) maxX = x;
            if (y < minY) minY = y;
            if (y > maxY) maxY = y;
        }
        return new double[]{minX, maxX, minY, maxY};
    }

    // ממיר קורדינטות לנקודות על המסך
    // (val - min) / (max - min) מוצא את המקום היחסי
    public double toRawScreenX(double val, double[] bounds) {
        return (val - bounds[0]) / (bounds[1] - bounds[0]) * canvas.getWidth();
    }

    // אותו דבר לY רק נחסר מאחד מכיוון שבGUI שלנו הגובה מתחיל מ0 למעלה וככל שיורדים הוא גדל,
    // אז נהפוך את זה על ידי חיסור מאחד
    public double toRawScreenY(double val, double[] bounds) {
        return (1.0 - (val - bounds[2]) / (bounds[3] - bounds[2])) * canvas.getHeight();
    }

    // ״המקום האמיתי״ של הנקודה על המסך, לאחר זום
    public double toScreenX(double val, double[] bounds) {
        return (toRawScreenX(val, bounds) + offsetX) * scale;
    }

    public double toScreenY(double val, double[] bounds) {
        return (toRawScreenY(val, bounds) + offsetY) * scale;
    }
}