package view;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import model.LatentSpaceModel;

// אחראי בלעדית על שורת הפקדים השנייה:
// חישוב מרחק סמנטי בין שתי מילים, הטלה על ציר, ומעבר למצב 3D
public class DistancePanel {

    private final LatentSpaceModel model;
    // callback למעבר למצב 3D — נשלח ל-MainView
    private final Runnable onEnter3D;

    public DistancePanel(LatentSpaceModel model, Runnable onEnter3D) {
        this.model = model;
        this.onEnter3D = onEnter3D;
    }

    public HBox build() {
        // Distance panel
        // יוצרים תיבת טקסט להזנת המילים לחישוב המרחק
        TextField distWord1 = new TextField();
        distWord1.setPromptText("Word 1");
        distWord1.setPrefWidth(100);
        TextField distWord2 = new TextField();
        distWord2.setPromptText("Word 2");
        distWord2.setPrefWidth(100);
        Label distResult = new Label("Distance: —");
        // יצירת כפתור להפעלת חישוב המרחק
        Button distBtn = new Button("Compute");
        // כאשר נלחץ על הכפתור, נסיר רווחים ונעביר לאותיות קטנות את שני המילים ונחשב מרחק
        distBtn.setOnAction(e -> {
            try {
                double dist = model.computeDistance(
                        distWord1.getText().trim().toLowerCase(),
                        distWord2.getText().trim().toLowerCase()
                );
                // מעדכן את המרחק בתיבה שלו ותופס שגיאות אם יש
                distResult.setText(String.format("Distance (%s): %.4f", model.getMetric().getName(), dist));
            } catch (IllegalArgumentException ex) {
                distResult.setText("Word not found!");
            }
        });

        // Custom Projection panel
        // יוצרים תיבת טקסט, ממש כמו במרחק רק לחישוב הטלה
        TextField projWord1 = new TextField();
        projWord1.setPromptText("Word 1");
        projWord1.setPrefWidth(130);
        TextField projWord2 = new TextField();
        projWord2.setPromptText("Word 2");
        projWord2.setPrefWidth(130);
        // יצירת כפתור ההפעלה של חישוב ההטלה, מורידם רווחים והופכים לאותיות קטנות
        Button projBtn = new Button("Project");
        projBtn.setOnAction(e -> {
            try {
                new ProjectionView(model).show(
                        projWord1.getText().trim().toLowerCase(),
                        projWord2.getText().trim().toLowerCase()
                );
            } catch (IllegalArgumentException ex) {
                new Alert(Alert.AlertType.ERROR, "Word not found!").show();
            }
        });

        // כפתור מעבר למצב 3D — בלחיצה מפעיל את ה-callback שמנוהל ב-MainView
        Button view3DBtn = new Button("3D View");
        view3DBtn.setOnAction(e -> onEnter3D.run());

        // בונה תיבה מאוזנת ומוסיף אליה את החישוב מרחק, חישוב הטלה, וכפתור ה-3D
        HBox distPanel = new HBox(10,
                new Label("Distance:"), distWord1, distWord2, distBtn, distResult,
                new Separator(),
                new Label("Project axis:"), projWord1, projWord2, projBtn,
                new Separator(),
                view3DBtn
        );
        distPanel.setPadding(new Insets(8));
        return distPanel;
    }
}