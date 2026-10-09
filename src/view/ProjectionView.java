// Displays all words projected onto an axis defined by two selected words.
package view;

import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import model.LatentSpaceModel;
import model.WordVector;
import java.util.ArrayList;
import java.util.List;

public class ProjectionView {

    private final LatentSpaceModel model;

    public ProjectionView(LatentSpaceModel model) {
        this.model = model;
    }

    public void show(String word1, String word2) {
        //מחשב את הווקטור בין שני מילים
        double[] axis = model.computeCustomAxis(word1, word2);

        //עובר על כל המילים ומחשב את ההטלה על הציר שנבנה
        List<Double> projections = new ArrayList<>();
        List<String> wordNames = new ArrayList<>();

        for (WordVector wv : model.getWords().values()) {
            double proj = model.projectOnAxis(wv.getFullVector(), axis);
            projections.add(proj);
            wordNames.add(wv.getWord());
        }

        //על אותו עיקרון של חישוב הגבולות ממקודם, מוצא נקודות מקסימום ומינימום כדי לשמור על יחס תקין על הציר
        double minP = projections.stream().mapToDouble(Double::doubleValue).min().getAsDouble();
        double maxP = projections.stream().mapToDouble(Double::doubleValue).max().getAsDouble();

        //יוצר קנבס חדש לחלון הזה, בגדלים שנדרוש
        Canvas projCanvas = new Canvas(900, 200);
        GraphicsContext pgc = projCanvas.getGraphicsContext2D();

        // פונקציית ציור מחדש - מנקה ומצייר את הציר והתוויות מחדש
        Runnable redraw = () -> {
            pgc.setFill(Color.BLACK);
            pgc.fillRect(0, 0, 900, 200);
            pgc.setStroke(Color.LIGHTGRAY);
            pgc.strokeLine(50, 100, 850, 100);
            pgc.setFill(Color.ORANGE);
            pgc.fillText(word1, 50, 125);
            pgc.fillText(word2, 820, 125);

            //עובר על כל המילים וממיר את נקודת ההטלה שלו על הציר לנקודה על המסך
            for (int i = 0; i < projections.size(); i++) {
                double px = 50 + (projections.get(i) - minP) / (maxP - minP) * 800;
                String w = wordNames.get(i);
                // אם הנקודה שנבדקת היא אחת מהשניים שיצרו את הציר תצבע אותה בכתום ותגדיל, כל השאר באפור באותו גודל
                if (w.equals(word1) || w.equals(word2)) {
                    pgc.setFill(Color.ORANGE);
                    pgc.fillOval(px - 4, 94, 8, 8);
                } else {
                    pgc.setFill(Color.WHITE.deriveColor(0, 1, 1, 0.75));
                    pgc.fillOval(px - 2, 96, 4, 4);
                }
            }
        };

        redraw.run();

        //כל פעם שהעכבר זז תצייר הכל מחדש ותוציא את הנקודה שלו על הציר
        projCanvas.setOnMouseMoved(e -> {
            redraw.run();
            double mx = e.getX();
            //עובר על כל המילים ובודק האם העכבר במרחק של 5 פיקסלים ממנה, אם כן תכתוב את המילה למעלה בלבן
            for (int i = 0; i < projections.size(); i++) {
                double px = 50 + (projections.get(i) - minP) / (maxP - minP) * 800;
                if (Math.abs(px - mx) < 5) {
                    pgc.setFill(Color.WHITE);
                    pgc.fillText(wordNames.get(i), px - 10, 75);
                    break;
                }
            }
        });

        //יוצר את החלון החדש נותן לו כותרת, מכניס את הציורים לבפנים
        Stage projStage = new Stage();
        projStage.setTitle("Projection: " + word1 + " → " + word2);
        projStage.setScene(new Scene(new VBox(projCanvas)));
        projStage.show();
    }
}
