// Provides the vector arithmetic lab interface.
package view;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import model.LatentSpaceModel;
import model.WordVector;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

// אחראי בלעדית על חלון ה-Vector Arithmetic Lab
// תומך בבחירת מילים הן על ידי הקלדה והן על ידי לחיצה על המפה הראשית
public class VectorLabView {

    private final LatentSpaceModel model;
    private final Consumer<List<String>> onPathReady;

    // callback ל-MainView: קריאה אליו עם Consumer<String> מפעילה מצב בחירה במפה,
    // קריאה עם null מבטלת אותו
    private final Consumer<Consumer<String>> onMapPickRequested;

    // האינדקס של השורה שממתינה לבחירה מהמפה (-1 = אין)
    private int pickingRowIndex = -1;

    // MainView מעביר את setMapPickListener כ-callback:
    // VectorLabView קורא לו עם Consumer<String> כשרוצה להפעיל מצב בחירה מהמפה,
    // ועם null כשרוצה לבטל את המצב
    public VectorLabView(LatentSpaceModel model,
                          Consumer<List<String>> onPathReady,
                          Consumer<Consumer<String>> setMapPickListener) {
        this.model = model;
        this.onPathReady = onPathReady;
        this.onMapPickRequested = setMapPickListener;
    }

    public void show() {
        List<TextField> wordFields = new ArrayList<>();
        List<ComboBox<String>> signBoxes = new ArrayList<>();
        // כפתורי "Pick" — אחד לכל שורה
        List<Button> pickButtons = new ArrayList<>();

        VBox rows = new VBox(8);

        Label resultLabel = new Label("Result: —");
        resultLabel.setStyle("-fx-text-fill: orange; -fx-font-weight: bold; -fx-font-size: 13;");

        // הודעת סטטוס — מציגה "Click a word on the map..." כשמצב הבחירה פעיל
        Label statusLabel = new Label("");
        statusLabel.setStyle("-fx-text-fill: lightblue; -fx-font-style: italic; -fx-font-size: 11;");

        Stage stage = new Stage();

        Runnable[] rebuildRows = new Runnable[1];

        // כשמילה נבחרת מהמפה — ממלאים את השדה המתאים ומבטלים את מצב הבחירה
        Consumer<String> onWordPickedFromMap = word -> {
            javafx.application.Platform.runLater(() -> {
                if (pickingRowIndex >= 0 && pickingRowIndex < wordFields.size()) {
                    wordFields.get(pickingRowIndex).setText(word);
                }
                pickingRowIndex = -1;
                statusLabel.setText("");
                for (Button pb : pickButtons) {
                    pb.setStyle("-fx-base: gray;");
                    pb.setText("Pick");
                }
                onMapPickRequested.accept(null);
                stage.toFront();
            });
        };

        rebuildRows[0] = () -> {
            rows.getChildren().clear();
            pickButtons.clear();

            for (int i = 0; i < wordFields.size(); i++) {
                final int idx = i;

                Label lbl = new Label("V" + (idx + 1) + ":");
                lbl.setStyle("-fx-text-fill: lightgray;");
                lbl.setPrefWidth(28);

                // כפתור "Pick" — לחיצה עליו מפעילה מצב בחירה מהמפה לשורה הזו
                Button pickBtn = new Button("Pick");
                pickBtn.setStyle("-fx-base: gray; -fx-font-size: 10; -fx-padding: 2 6 2 6;");
                pickBtn.setOnAction(ev -> {
                    if (pickingRowIndex == idx) {
                        // לחיצה שנייה על אותו Pick — ביטול הבחירה
                        pickingRowIndex = -1;
                        statusLabel.setText("");
                        pickBtn.setStyle("-fx-base: gray;");
                        pickBtn.setText("Pick");
                        onMapPickRequested.accept(null);
                    } else {
                        // הפעלת מצב בחירה לשורה הזו
                        pickingRowIndex = idx;
                        statusLabel.setText("Click a word on the map for V" + (idx + 1) + "...");
                        // צביעת כפתור הנבחר בכחול, שאר הכפתורים חוזרים לאפור
                        for (int j = 0; j < pickButtons.size(); j++) {
                            pickButtons.get(j).setStyle("-fx-base: " + (j == idx ? "blue" : "gray") + ";");
                            pickButtons.get(j).setText(j == idx ? "✓ Picking" : "Pick");
                        }
                        // רישום ה-listener במפה — MainView ישלח לכאן כל לחיצה
                        onMapPickRequested.accept(onWordPickedFromMap);
                        // מחזירים פוקוס לחלון הראשי כדי שיוכל לקלוט לחיצות על הקנבס
                        stage.toBack();
                    }
                });
                pickButtons.add(pickBtn);

                // כפתור הסרת שורה — מופיע רק אם יש יותר מ-2 שורות
                Button removeBtn = new Button("✕");
                removeBtn.setStyle("-fx-base: red; -fx-font-size: 10; -fx-padding: 2 6 2 6;");
                removeBtn.setVisible(wordFields.size() > 2);
                removeBtn.setOnAction(ev -> {
                    // אם מסירים שורה שבמצב בחירה — מבטלים
                    if (pickingRowIndex == idx) {
                        pickingRowIndex = -1;
                        statusLabel.setText("");
                        onMapPickRequested.accept(null);
                    } else if (pickingRowIndex > idx) {
                        // האינדקס הפעיל זזה אחד אחורה
                        pickingRowIndex--;
                    }
                    wordFields.remove(idx);
                    signBoxes.remove(idx);
                    stage.setHeight(stage.getHeight() - 40);
                    rebuildRows[0].run();
                });

                HBox row = new HBox(8, lbl, wordFields.get(i), signBoxes.get(i), pickBtn, removeBtn);
                row.setAlignment(Pos.CENTER_LEFT);
                rows.getChildren().add(row);
            }
        };

        Runnable addRow = () -> {
            TextField tf = new TextField();
            tf.setPromptText("V" + (wordFields.size() + 1) + " word...");
            tf.setPrefWidth(140);
            wordFields.add(tf);

            ComboBox<String> signBox = new ComboBox<>();
            signBox.getItems().addAll("+", "-");
            signBox.setValue(wordFields.size() <= 2 ? "+" : "-");
            signBoxes.add(signBox);

            rebuildRows[0].run();
        };

        // מקרה בסיס: 2 שורות
        addRow.run();
        addRow.run();

        Button addBtn = new Button("＋ Add Vector");
        addBtn.setStyle("-fx-base: lightgreen;");
        addBtn.setOnAction(e -> {
            addRow.run();
            stage.setHeight(stage.getHeight() + 40);
        });

        Button computeBtn = new Button("Compute");
        computeBtn.setStyle("-fx-base: blue; -fx-font-weight: bold;");
        computeBtn.setOnAction(e -> {
            try {
                List<String> wordNames = new ArrayList<>();
                List<Integer> signs    = new ArrayList<>();

                for (int i = 0; i < wordFields.size(); i++) {
                    String w = wordFields.get(i).getText().trim().toLowerCase();
                    if (!w.isEmpty()) {
                        wordNames.add(w);
                        signs.add(signBoxes.get(i).getValue().equals("+") ? 1 : -1);
                    }
                }

                if (wordNames.isEmpty()) {
                    resultLabel.setText("Error: enter at least one word");
                    return;
                }

                double[] resultVec  = model.computeArithmetic(wordNames, signs);
                WordVector closest  = model.findClosestExcluding(resultVec, wordNames);

                resultLabel.setText("Result: " + closest.getWord());

                List<String> path = new ArrayList<>(wordNames);
                path.add(closest.getWord());
                onPathReady.accept(path);

            } catch (IllegalArgumentException ex) {
                resultLabel.setText("Error: " + ex.getMessage());
            }
        });

        // כשהחלון נסגר — ביטול מצב הבחירה במפה
        stage.setOnCloseRequest(e -> onMapPickRequested.accept(null));

        HBox bottomBar = new HBox(10, addBtn, computeBtn);
        bottomBar.setAlignment(Pos.CENTER_LEFT);

        VBox layout = new VBox(12, rows, bottomBar, statusLabel, resultLabel);
        layout.setPadding(new Insets(15));
        layout.setStyle("-fx-background-color: black;");

        stage.setTitle("Vector Arithmetic Lab");
        stage.setScene(new Scene(layout, 380, 175));
        // initStyle UTILITY — החלון לא לוקח פוקוס מהחלון הראשי בעת לחיצה על Pick
        stage.initStyle(javafx.stage.StageStyle.UTILITY);
        stage.show();
    }
}
