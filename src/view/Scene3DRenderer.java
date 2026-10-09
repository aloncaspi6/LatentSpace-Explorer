package view;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
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

    // צירי ה-PCA שיוצגו בשלושת הממדים
    private int pcaX;
    private int pcaY;
    private int pcaZ;

    // סיבוב הסצנה — זוויות סביב צירי Y ו-X (בעזרת גרירת עכבר)
    private double rotY = 0.3;
    private double rotX = -0.3;

    // מרחק הצופה מהסצנה — נשתנה על ידי גלגלת העכבר
    private double zoom = 400;

    // הזזת מרכז הסצנה — מאפשר מרכוז על מילה ספציפית
    private double panX = 0;
    private double panY = 0;

    // גודל הקנבס — נשלף מהקנבס עצמו
    private double W, H;

    // רשימת נקודות מוקרנות לצורך זיהוי לחיצות ב-View3D
    private final List<Object[]> projectedPoints = new ArrayList<>();

    public Scene3DRenderer(Canvas canvas, GraphicsContext gc, LatentSpaceModel model,
                           int pcaX, int pcaY, int pcaZ) {
        this.canvas = canvas;
        this.gc     = gc;
        this.model  = model;
        this.pcaX   = pcaX;
        this.pcaY   = pcaY;
        this.pcaZ   = pcaZ;
        this.W      = canvas.getWidth();
        this.H      = canvas.getHeight();
    }

    // ---- setters לעדכון מצב מ-View3D ----

    public void setPcaZ(int pcaZ)          { this.pcaZ = pcaZ; }
    public void addRotY(double delta)      { this.rotY += delta; }
    public void addRotX(double delta)      { this.rotX += delta; }
    public void addZoom(double factor)     {
        zoom *= factor;
        zoom = Math.max(50, Math.min(5000, zoom));
    }
    public void setPan(double panX, double panY) {
        this.panX = panX;
        this.panY = panY;
    }
    public double getPanX()  { return panX; }
    public double getPanY()  { return panY; }
    public double getZoom()  { return zoom; }
    public double getW()     { return W; }
    public double getH()     { return H; }

    public List<Object[]> getProjectedPoints() { return projectedPoints; }

    // ---- ציור הסצנה ----

    // מצייר את כל נקודות המילים לאחר הסיבוב וההיטל הפרספקטיבי
    public void drawScene(String focusedWord, int kNeighbors) {
        // רקע כהה
        gc.setFill(Color.web("#1e1e2e"));
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

        // נקדים לצייר קווים (שכנים) לפני הנקודות כדי שהנקודות יהיו מעל הקווים
        if (focused != null && neighbors != null) {
            double[] fp = project(focused, bounds);
            gc.setStroke(Color.web("#89dceb", 0.3));
            gc.setLineWidth(1);
            for (WordVector nb : neighbors) {
                double[] np = project(nb, bounds);
                gc.strokeLine(fp[0], fp[1], np[0], np[1]);
            }
        }

        // ציור הנקודות
        for (WordVector wv : model.getWords().values()) {
            double[] p = project(wv, bounds);
            // שמירת הנקודה המוקרנת לצורך זיהוי לחיצות
            projectedPoints.add(new Object[]{wv, p[0], p[1]});

            boolean isFocused  = wv == focused;
            boolean isNeighbor = neighbors != null && neighbors.contains(wv);

            if (isFocused) {
                // הנקודה הנבחרת — כתומה וגדולה, עם תווית
                gc.setFill(Color.web("#ffaa00"));
                gc.fillOval(p[0] - 7, p[1] - 7, 14, 14);
                gc.fillText(wv.getWord(), p[0] + 10, p[1]);
            } else if (isNeighbor) {
                // שכנים — ירוקים עם תווית
                gc.setFill(Color.web("#40ff80"));
                gc.fillOval(p[0] - 4, p[1] - 4, 8, 8);
                gc.fillText(wv.getWord(), p[0] + 6, p[1]);
            } else {
                // נקודה רגילה — כחולה-אפורה, שקופה למחצה
                gc.setFill(Color.web("#cdd6f4", 0.5));
                gc.fillOval(p[0] - 2, p[1] - 2, 4, 4);
            }
        }

        // הוראות שימוש בפינה התחתונה
        gc.setFill(Color.web("#585b70"));
        gc.fillText("Drag to rotate  |  Scroll to zoom  |  Click to select  |  3D Mode", 10, H - 10);
    }

    // מזיז את ה-pan כך שהמילה הנבחרת תהיה במרכז הקנבס
    public void centerOnWord(String word) {
        WordVector wv = model.getWord(word);
        if (wv == null) return;
        double[] bounds = computeBounds();
        double[] v = wv.getPcaVector();

        // נרמול קואורדינטות ל[-1,1]
        double x = normalize(v[pcaX], bounds[0], bounds[1]);
        double y = normalize(v[pcaY], bounds[2], bounds[3]);
        double z = normalize(v[pcaZ], bounds[4], bounds[5]);

        // סיבוב סביב Y
        double cosY = Math.cos(rotY), sinY = Math.sin(rotY);
        double x1 = x * cosY - z * sinY;
        double z1 = x * sinY + z * cosY;

        // סיבוב סביב X
        double cosX = Math.cos(rotX), sinX = Math.sin(rotX);
        double y1 = y * cosX - z1 * sinX;
        double z2 = y * sinX + z1 * cosX;

        // נחשב את המיקום המוקרן ללא pan, ונקבע pan כך שהמילה תופיע במרכז
        // screenX = W/2 + panX + rawX = W/2  =>  panX = -rawX
        // screenY = H/2 + panY - rawY = H/2  =>  panY = rawY
        double depth = z2 + 2;
        panX = -(x1 * zoom / depth);
        panY =   y1 * zoom / depth;
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
        double cosY = Math.cos(rotY), sinY = Math.sin(rotY);
        double x1 = x * cosY - z * sinY;
        double z1 = x * sinY + z * cosY;

        // סיבוב סביב ציר X (למעלה-למטה בעת גרירה אנכית)
        double cosX = Math.cos(rotX), sinX = Math.sin(rotX);
        double y1 = y * cosX - z1 * sinX;
        double z2 = y * sinX + z1 * cosX;

        // היטל פרספקטיבי — ככל שהנקודה רחוקה יותר (z גדול), היא נראית קטנה יותר
        // depth+2 כדי למנוע חלוקה באפס
        double depth   = z2 + 2;
        double screenX = W / 2.0 + panX + x1 * zoom / depth;
        double screenY = H / 2.0 + panY - y1 * zoom / depth;

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