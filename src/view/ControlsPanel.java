package view;

import distance.CosineDistance;
import distance.EuclideanDistance;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import model.LatentSpaceModel;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

// אחראי בלעדית על שורת הפקדים הראשונה:
// חיפוש מילה, בחירת צירי PCA, כמות שכנים K, בחירת מטריקת מרחק, ופתיחת חלונות
public class ControlsPanel {

    private final LatentSpaceModel model;
    private final Consumer<String> onFocusWord;
    private final BiConsumer<Integer, Integer> onAxesChanged;
    private final Consumer<Integer> onKChanged;
    private final Consumer<List<String>> onPathReady;
    // callback ל-MainView כדי להפעיל/לבטל מצב בחירה מהמפה עבור ArithmeticView
    private final Consumer<Consumer<String>> setMapPickListener;

    public ControlsPanel(LatentSpaceModel model,
                         Consumer<String> onFocusWord,
                         BiConsumer<Integer, Integer> onAxesChanged,
                         Consumer<Integer> onKChanged,
                         Consumer<List<String>> onPathReady,
                         Consumer<Consumer<String>> setMapPickListener) {
        this.model              = model;
        this.onFocusWord        = onFocusWord;
        this.onAxesChanged      = onAxesChanged;
        this.onKChanged         = onKChanged;
        this.onPathReady        = onPathReady;
        this.setMapPickListener = setMapPickListener;
    }

    public HBox build() {
        TextField searchField = new TextField();
        searchField.setPromptText("Search word...");
        Button searchBtn = new Button("Search");
        searchBtn.setOnAction(e -> onFocusWord.accept(searchField.getText().trim().toLowerCase()));

        Spinner<Integer> spinX = new Spinner<>(0, 49, 0);
        Spinner<Integer> spinY = new Spinner<>(0, 49, 1);
        spinX.valueProperty().addListener((obs, old, val) -> onAxesChanged.accept(val, spinY.getValue()));
        spinY.valueProperty().addListener((obs, old, val) -> onAxesChanged.accept(spinX.getValue(), val));

        Spinner<Integer> spinK = new Spinner<>(1, 20, 5);
        spinK.valueProperty().addListener((obs, old, val) -> onKChanged.accept(val));

        ComboBox<String> metricBox = new ComboBox<>();
        metricBox.getItems().addAll("Euclidean", "Cosine");
        metricBox.setValue("Euclidean");
        metricBox.setOnAction(e -> {
            model.setMetric(metricBox.getValue().equals("Cosine")
                    ? new CosineDistance()
                    : new EuclideanDistance());
        });

        // מעביר את setMapPickListener ל-ArithmeticView כדי שיוכל לבקש בחירה מהמפה
        Button arithmeticBtn = new Button("Vector Lab");
        arithmeticBtn.setOnAction(e ->
                new ArithmeticView(model, onFocusWord::accept, onPathReady, setMapPickListener).show()
        );

        Button subspaceBtn = new Button("Subspace");
        subspaceBtn.setOnAction(e -> new SubspaceView(model, onFocusWord::accept).show());

        HBox controls = new HBox(10,
                new Label("Search:"), searchField, searchBtn,
                new Label("PC-X:"), spinX,
                new Label("PC-Y:"), spinY,
                new Label("K:"), spinK,
                new Label("Metric:"), metricBox,
                arithmeticBtn, subspaceBtn
        );
        controls.setPadding(new Insets(8));
        return controls;
    }
}