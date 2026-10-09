// Draws the 2D vector visualization on the canvas.
package view;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import model.LatentSpaceModel;
import model.WordVector;
import java.util.List;

// אחראי בלעדית על ציור הקנבס וחישובי קואורדינטות
// MainView מחזיק instance של מחלקה זו ומשתמש בה לכל פעולת ציור
public class CanvasRenderer {

    private final Canvas canvas;
    private final GraphicsContext gc;
    private final LatentSpaceModel model;
    private CanvasNavigator navigator;

    private int pcaX;
    private int pcaY;

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

    public void setNavigator(CanvasNavigator navigator) { this.navigator = navigator; }

    public int    getPcaX()    { return pcaX;    }
    public int    getPcaY()    { return pcaY;    }

    // ---- ציור ראשי ----

    // יוצרת את הקנבס המלא, צבע רק כהה לפי הגדלים שדרשנו
    public void drawCanvas(String focusedWord, int kNeighbors, List<String> pathWords) {
        double W = canvas.getWidth();
        double H = canvas.getHeight();

        gc.setFill(Color.BLACK);
        gc.fillRect(0, 0, W, H);

        // מחשב את המינימום ומקסימום של הנקודות, ממיר אותם לנקודות על הקנבס
        double[] bounds = computeBounds();

        // אם מילה נבחרה תיקח את הווקטור שלה ואת ה-k שכנים שלה
        List<WordVector> neighbors = null;
        WordVector focused = focusedWord != null ? model.getWord(focusedWord) : null;
        if (focused != null) {
            neighbors = model.getNearestNeighbors(focusedWord, kNeighbors);
        }

        drawRegularPoints(focused, neighbors, bounds);
        drawNeighborLines(focused, neighbors, bounds);

        // ציור המסלול הווקטורי אם קיים — חצים בין נקודות V1 → V2 → V3 → result
        if (pathWords != null && pathWords.size() >= 2) {
            drawPath(pathWords, bounds);
        }

        drawHighlightedPoints(focused, neighbors, bounds);
        drawHighlightedLabels(focused, neighbors, bounds);
    }

    private void drawRegularPoints(WordVector focused, List<WordVector> neighbors, double[] bounds) {
        gc.setFill(Color.WHITE.deriveColor(0, 1, 1, 0.75));
        for (WordVector wv : model.getWords().values()) {
            if (wv == focused || neighbors != null && neighbors.contains(wv)) continue;
            double px = toScreenX(wv.getPcaVector()[pcaX], bounds);
            double py = toScreenY(wv.getPcaVector()[pcaY], bounds);
            gc.fillOval(px - POINT_RADIUS, py - POINT_RADIUS, POINT_RADIUS * 2, POINT_RADIUS * 2);
        }
    }

    private void drawNeighborLines(WordVector focused, List<WordVector> neighbors, double[] bounds) {
        if (focused == null || neighbors == null) return;
        double fx = toScreenX(focused.getPcaVector()[pcaX], bounds);
        double fy = toScreenY(focused.getPcaVector()[pcaY], bounds);
        gc.setStroke(Color.LIME.deriveColor(0, 1, 1, 0.8));
        gc.setLineWidth(2);
        for (WordVector neighbor : neighbors) {
            double px = toScreenX(neighbor.getPcaVector()[pcaX], bounds);
            double py = toScreenY(neighbor.getPcaVector()[pcaY], bounds);
            gc.strokeLine(px, py, fx, fy);
        }
        gc.setLineWidth(1);
    }

    private void drawHighlightedPoints(WordVector focused, List<WordVector> neighbors, double[] bounds) {
        if (neighbors != null) {
            gc.setFill(Color.LIME);
            for (WordVector neighbor : neighbors) {
                double px = toScreenX(neighbor.getPcaVector()[pcaX], bounds);
                double py = toScreenY(neighbor.getPcaVector()[pcaY], bounds);
                gc.fillOval(px - 4, py - 4, 8, 8);
            }
        }
        if (focused != null) {
            double px = toScreenX(focused.getPcaVector()[pcaX], bounds);
            double py = toScreenY(focused.getPcaVector()[pcaY], bounds);
            gc.setFill(Color.ORANGE);
            gc.fillOval(px - 6, py - 6, 12, 12);
        }
    }

    private void drawHighlightedLabels(WordVector focused, List<WordVector> neighbors, double[] bounds) {
        Font previousFont = gc.getFont();
        gc.setFont(Font.font(14));
        gc.setStroke(Color.BLACK);
        gc.setLineWidth(3);
        if (neighbors != null) {
            for (WordVector neighbor : neighbors) {
                drawOutlinedLabel(neighbor, 6, Color.LIME, bounds);
            }
        }
        if (focused != null) {
            drawOutlinedLabel(focused, 8, Color.ORANGE, bounds);
        }
        gc.setFont(previousFont);
        gc.setLineWidth(1);
    }

    private void drawOutlinedLabel(WordVector wv, double xOffset, Color color, double[] bounds) {
        double px = toScreenX(wv.getPcaVector()[pcaX], bounds) + xOffset;
        double py = toScreenY(wv.getPcaVector()[pcaY], bounds);
        gc.strokeText(wv.getWord(), px, py);
        gc.setFill(color);
        gc.fillText(wv.getWord(), px, py);
    }

    // מצייר חצים בין נקודות המסלול על הקנבס
    // כל נקודה מסומנת בעיגול ציאן, עם תווית שם המילה ומספר שלב
    private void drawPath(List<String> pathWords, double[] bounds) {
        gc.setStroke(Color.CYAN.deriveColor(0, 1, 1, 0.9));
        gc.setLineWidth(2);

        for (int i = 0; i < pathWords.size(); i++) {
            WordVector wv = model.getWord(pathWords.get(i));
            if (wv == null) continue;

            double px = toScreenX(wv.getPcaVector()[pcaX], bounds);
            double py = toScreenY(wv.getPcaVector()[pcaY], bounds);

            // עיגול ציאן לכל נקודת מסלול
            gc.setFill(Color.CYAN);
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
        gc.setFill(Color.CYAN.deriveColor(0, 1, 1, 0.9));
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
        return (toRawScreenX(val, bounds) + navigator.getOffsetX()) * navigator.getScale();
    }

    public double toScreenY(double val, double[] bounds) {
        return (toRawScreenY(val, bounds) + navigator.getOffsetY()) * navigator.getScale();
    }
}
