// Draws and projects the 3D vector visualization.
package view;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import model.LatentSpaceModel;
import model.WordVector;
import java.util.ArrayList;
import java.util.List;

// אחראי בלעדית על ציור הסצנה התלת-ממדית וחישובי ההיטל הפרספקטיבי
// View3D מחזיק instance של מחלקה זו ומשתמש בה לכל פעולת ציור
public class Scene3DRenderer {

    private final Canvas canvas;
    private final GraphicsContext gc;
    private final LatentSpaceModel model;
    private final Scene3DNavigator navigator;

    // צירי ה-PCA שיוצגו בשלושת הממדים
    private int pcaX;
    private int pcaY;
    private int pcaZ;

    // גודל הקנבס — נשלף מהקנבס עצמו
    private double W, H;

    // רשימת נקודות מוקרנות לצורך זיהוי לחיצות ב-View3D
    private final List<Object[]> projectedPoints = new ArrayList<>();

    public Scene3DRenderer(Canvas canvas, GraphicsContext gc, LatentSpaceModel model,
                           int pcaX, int pcaY, int pcaZ, Scene3DNavigator navigator) {
        this.canvas = canvas;
        this.gc     = gc;
        this.model  = model;
        this.navigator = navigator;
        this.pcaX   = pcaX;
        this.pcaY   = pcaY;
        this.pcaZ   = pcaZ;
        this.W      = canvas.getWidth();
        this.H      = canvas.getHeight();
    }

    public void setPcaZ(int pcaZ) { this.pcaZ = pcaZ; }
    public int getPcaX() { return pcaX; }
    public int getPcaY() { return pcaY; }
    public int getPcaZ() { return pcaZ; }

    public List<Object[]> getProjectedPoints() { return projectedPoints; }

    // ---- ציור הסצנה ----

    // מצייר את כל נקודות המילים לאחר הסיבוב וההיטל הפרספקטיבי
    public void drawScene(String focusedWord, int kNeighbors) {
        W = canvas.getWidth();
        H = canvas.getHeight();

        // רקע כהה
        gc.setFill(Color.BLACK);
        gc.fillRect(0, 0, W, H);

        // חישוב גבולות ה-PCA לנרמול הקואורדינטות
        double[] bounds = computeBounds();

        // נאפס את רשימת הנקודות המוקרנות לפני כל ציור מחדש
        projectedPoints.clear();

        List<WordVector> neighbors = null;
        WordVector focused = focusedWord != null ? model.getWord(focusedWord) : null;
        if (focused != null) {
            neighbors = model.getNearestNeighbors(focusedWord, kNeighbors);
        }

        drawRegularPoints(focused, neighbors, bounds);
        drawNeighborLines(focused, neighbors, bounds);
        drawHighlightedPoints(focused, neighbors, bounds);

        // הוראות שימוש בפינה התחתונה
        gc.setFill(Color.LIGHTGRAY);
        gc.fillText("Drag to rotate  |  Scroll to zoom  |  Click to select  |  3D Mode", 10, H - 10);

        drawHighlightedLabels(focused, neighbors, bounds);
    }

    private void drawRegularPoints(WordVector focused, List<WordVector> neighbors, double[] bounds) {
        gc.setFill(Color.WHITE.deriveColor(0, 1, 1, 0.75));
        for (WordVector wv : model.getWords().values()) {
            double[] p = project(wv, bounds);
            // שמירת הנקודה המוקרנת לצורך זיהוי לחיצות
            projectedPoints.add(new Object[]{wv, p[0], p[1]});
            if (wv == focused || neighbors != null && neighbors.contains(wv)) continue;
            gc.fillOval(p[0] - 2, p[1] - 2, 4, 4);
        }
    }

    private void drawNeighborLines(WordVector focused, List<WordVector> neighbors, double[] bounds) {
        if (focused == null || neighbors == null) return;
        double[] fp = project(focused, bounds);
        gc.setStroke(Color.LIME.deriveColor(0, 1, 1, 0.95));
        gc.setLineWidth(3);
        for (WordVector neighbor : neighbors) {
            double[] np = project(neighbor, bounds);
            gc.strokeLine(fp[0], fp[1], np[0], np[1]);
        }
        gc.setLineWidth(1);
    }

    private void drawHighlightedPoints(WordVector focused, List<WordVector> neighbors, double[] bounds) {
        if (neighbors != null) {
            gc.setFill(Color.LIME);
            for (WordVector neighbor : neighbors) {
                double[] p = project(neighbor, bounds);
                gc.fillOval(p[0] - 4, p[1] - 4, 8, 8);
            }
        }
        if (focused != null) {
            double[] p = project(focused, bounds);
            gc.setFill(Color.ORANGE);
            gc.fillOval(p[0] - 7, p[1] - 7, 14, 14);
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
            drawOutlinedLabel(focused, 10, Color.ORANGE, bounds);
        }
        gc.setFont(previousFont);
        gc.setLineWidth(1);
    }

    private void drawOutlinedLabel(WordVector wv, double xOffset, Color color, double[] bounds) {
        double[] p = project(wv, bounds);
        double x = p[0] + xOffset;
        gc.strokeText(wv.getWord(), x, p[1]);
        gc.setFill(color);
        gc.fillText(wv.getWord(), x, p[1]);
    }

    // ---- היטל תלת-ממד לדו-ממד ----

    // ממיר ווקטור PCA לנקודת מסך לאחר סיבוב + היטל פרספקטיבי
    public double[] project(WordVector wv, double[] bounds) {
        double[] v = wv.getPcaVector();

        // נרמול קואורדינטות ל[-1, 1] לפי גבולות ה-PCA
        double x = normalize(v[pcaX], bounds[0], bounds[1]);
        double y = normalize(v[pcaY], bounds[2], bounds[3]);
        double z = normalize(v[pcaZ], bounds[4], bounds[5]);

        // סיבוב סביב ציר Y (שמאל-ימין בעת גרירה אופקית)
        double cosY = Math.cos(navigator.getRotY()), sinY = Math.sin(navigator.getRotY());
        double x1 = x * cosY - z * sinY;
        double z1 = x * sinY + z * cosY;

        // סיבוב סביב ציר X (למעלה-למטה בעת גרירה אנכית)
        double cosX = Math.cos(navigator.getRotX()), sinX = Math.sin(navigator.getRotX());
        double y1 = y * cosX - z1 * sinX;
        double z2 = y * sinX + z1 * cosX;

        // היטל פרספקטיבי — ככל שהנקודה רחוקה יותר (z גדול), היא נראית קטנה יותר
        // depth+2 כדי למנוע חלוקה באפס
        double depth   = z2 + 2;
        double screenX = W / 2.0 + navigator.getPanX() + x1 * navigator.getZoom() / depth;
        double screenY = H / 2.0 + navigator.getPanY() - y1 * navigator.getZoom() / depth;

        return new double[]{screenX, screenY};
    }

    // ממיר ערך לטווח [-1, 1] לפי מינימום ומקסימום
    private double normalize(double val, double min, double max) {
        if (max == min) return 0;
        return 2.0 * (val - min) / (max - min) - 1.0;
    }

    // חישוב גבולות ה-PCA בשלושת הצירים הנבחרים
    public double[] computeBounds() {
        double minX = Double.MAX_VALUE, maxX = -Double.MAX_VALUE;
        double minY = Double.MAX_VALUE, maxY = -Double.MAX_VALUE;
        double minZ = Double.MAX_VALUE, maxZ = -Double.MAX_VALUE;

        for (WordVector wv : model.getWords().values()) {
            double[] v = wv.getPcaVector();
            if (v[pcaX] < minX) minX = v[pcaX];
            if (v[pcaX] > maxX) maxX = v[pcaX];
            if (v[pcaY] < minY) minY = v[pcaY];
            if (v[pcaY] > maxY) maxY = v[pcaY];
            if (v[pcaZ] < minZ) minZ = v[pcaZ];
            if (v[pcaZ] > maxZ) maxZ = v[pcaZ];
        }
        return new double[]{minX, maxX, minY, maxY, minZ, maxZ};
    }
}
