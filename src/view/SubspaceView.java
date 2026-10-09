package view;

import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import model.LatentSpaceModel;
import model.WordVector;
import java.util.ArrayList;
import java.util.List;

// אחראי בלעדית על חלון Subspace Grouping
// המשתמש בוחר קבוצת מילים, המערכת מחשבת Centroid ומציגה K מילים קרובות
public class SubspaceView {

    private final LatentSpaceModel model;
    private final ArithmeticView.WordFocusCallback onWordFound;

    public SubspaceView(LatentSpaceModel model, ArithmeticView.WordFocusCallback onWordFound) {
        this.model = model;
        this.onWordFound = onWordFound;
    }

    public void show() {
        // רשימת CheckBoxes — כל מילה עם תיבת סימון, ללא צורך ב-Cmd/Ctrl
        List<CheckBox> checkBoxes = new ArrayList<>();
        VBox checkList = new VBox(4);
        checkList.setPadding(new Insets(4));
        model.getWords().keySet().stream().sorted().forEach(word -> {
            CheckBox cb = new CheckBox(word);
            cb.setStyle("-fx-text-fill: #cdd6f4;");
            checkBoxes.add(cb);
            checkList.getChildren().add(cb);
        });

        // עוטפים את הרשימה בScrollPane כדי שניתן לגלול
        ScrollPane scrollPane = new ScrollPane(checkList);
        scrollPane.setPrefHeight(250);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: #181825; -fx-background: #181825;");

        // Spinner לבחירת K — כמה שכנים להציג
        Spinner<Integer> spinK = new Spinner<>(1, 20, 5);
        spinK.setEditable(true);

        Label resultTitle = new Label("Closest to centroid:");
        resultTitle.setStyle("-fx-text-fill: #cdd6f4; -fx-font-weight: bold;");

        VBox resultBox = new VBox(4);

        Button computeBtn = new Button("Compute Centroid");
        computeBtn.setOnAction(e -> {
            // נאסוף את כל המילים שסומנו עם ✅
            List<String> selected = new ArrayList<>();
            for (CheckBox cb : checkBoxes) {
                if (cb.isSelected()) selected.add(cb.getText());
            }

            if (selected.isEmpty()) {
                resultBox.getChildren().clear();
                resultBox.getChildren().add(styledLabel("Select at least one word", "#ff5555"));
                return;
            }

            try {
                // נחשב את הCentroid ונמצא K מילים קרובות
                double[] centroid = model.computeCentroid(selected);
                List<WordVector> nearest = model.getNearestToCentroid(centroid, spinK.getValue());

                resultBox.getChildren().clear();
                for (int i = 0; i < nearest.size(); i++) {
                    WordVector wv = nearest.get(i);
                    double dist = model.getMetric().compute(centroid, wv.getFullVector());
                    Label lbl = styledLabel(
                            (i + 1) + ". " + wv.getWord() + String.format("  (%.2f)", dist),
                            "#40ff80"
                    );
                    // לחיצה על תוצאה מדגישה אותה על הקנבס הראשי
                    lbl.setOnMouseClicked(ev -> onWordFound.onFocus(wv.getWord()));
                    lbl.setStyle(lbl.getStyle() + "; -fx-cursor: hand;");
                    resultBox.getChildren().add(lbl);
                }

            } catch (IllegalArgumentException ex) {
                resultBox.getChildren().clear();
                resultBox.getChildren().add(styledLabel("Error: " + ex.getMessage(), "#ff5555"));
            }
        });

        HBox kRow = new HBox(8, new Label("K:"), spinK, computeBtn);
        kRow.setStyle("-fx-alignment: center-left;");

        Label listTitle = new Label("Select words:");
        listTitle.setStyle("-fx-text-fill: #cdd6f4; -fx-font-weight: bold;");

        VBox layout = new VBox(10, listTitle, scrollPane, kRow, resultTitle, resultBox);
        layout.setPadding(new Insets(15));
        layout.setStyle("-fx-background-color: #1e1e2e;");

        Stage stage = new Stage();
        stage.setTitle("Subspace Grouping");
        stage.setScene(new Scene(layout, 300, 500));
        stage.show();
    }

    // פונקציית עזר ליצירת תווית מסוגננת עם צבע נתון
    private Label styledLabel(String text, String color) {
        Label l = new Label(text);
        l.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 11;");
        return l;
    }
}