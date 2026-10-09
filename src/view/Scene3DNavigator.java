// Manages the navigation and camera state for the 3D scene.
package view;

import model.WordVector;

public class Scene3DNavigator {

    private double rotX = -0.3;
    private double rotY = 0.3;
    private double zoom = 400;
    private double panX = 0;
    private double panY = 0;

    public void rotate(double dx, double dy) {
        rotY += dx / 200.0;
        rotX += dy / 200.0;
    }

    public void zoomAt(double mouseX, double mouseY, double factor) {
        panX += (mouseX - panX) * (1 - factor);
        panY += (mouseY - panY) * (1 - factor);
        zoom *= factor;
        zoom = Math.max(50, Math.min(5000, zoom));
    }

    public void centerOnWord(WordVector word, double[] bounds,
                             int pcaX, int pcaY, int pcaZ) {
        if (word == null) return;
        double[] v = word.getPcaVector();

        double x = normalize(v[pcaX], bounds[0], bounds[1]);
        double y = normalize(v[pcaY], bounds[2], bounds[3]);
        double z = normalize(v[pcaZ], bounds[4], bounds[5]);

        double cosY = Math.cos(rotY), sinY = Math.sin(rotY);
        double x1 = x * cosY - z * sinY;
        double z1 = x * sinY + z * cosY;

        double cosX = Math.cos(rotX), sinX = Math.sin(rotX);
        double y1 = y * cosX - z1 * sinX;
        double z2 = y * sinX + z1 * cosX;

        double depth = z2 + 2;
        panX = -(x1 * zoom / depth);
        panY = y1 * zoom / depth;
    }

    private double normalize(double val, double min, double max) {
        if (max == min) return 0;
        return 2.0 * (val - min) / (max - min) - 1.0;
    }

    public double getRotX() { return rotX; }
    public double getRotY() { return rotY; }
    public double getZoom() { return zoom; }
    public double getPanX() { return panX; }
    public double getPanY() { return panY; }
}
