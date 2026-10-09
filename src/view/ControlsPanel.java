// Manages the first control-panel row and connects user actions to search, PCA axes, K, metric selection, undo/redo, and additional views.
package view;

import command.ChangeAxesCommand;
import command.ChangeMetricCommand;
import command.CommandManager;
import distance.CosineDistance;
import distance.DistanceMetric;
import distance.EuclideanDistance;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.util.StringConverter;
import model.LatentSpaceModel;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class ControlsPanel {

    private final LatentSpaceModel model;
    private final Consumer<String> onFocusWord;
    private final BiConsumer<Integer, Integer> onAxesChanged;
    private final Consumer<Integer> onKChanged;
    private final Consumer<List<String>> onPathReady;
    // callback ל-MainView כדי להפעיל/לבטל מצב בחירה מהמפה עבור VectorLabView
    private final Consumer<Consumer<String>> setMapPickListener;

    // מנהל הפעולות - כל שינוי צירים או מטריקה עובר דרכו כדי לאפשר Undo/Redo
    private final CommandManager commandManager;

    // הפקדים נשמרים כשדות כדי שפקודות undo יוכלו לעדכן גם את התצוגה שלהם
    private Spinner<Integer> spinX;
    private Spinner<Integer> spinY;
    private ComboBox<DistanceMetric> metricBox;

    // דגל שמונע לולאה אינסופית:
    // כשפקודה (undo/redo) מעדכנת את הספינר, ה-listener של הספינר נורה שוב -
    // בלי הדגל הוא היה יוצר פקודה חדשה על שינוי שנגרם מפקודה קיימת
    private boolean updatingFromCommand = false;

    public ControlsPanel(LatentSpaceModel model,
                         CommandManager commandManager,
                         Consumer<String> onFocusWord,
                         BiConsumer<Integer, Integer> onAxesChanged,
                         Consumer<Integer> onKChanged,
                         Consumer<List<String>> onPathReady,
                         Consumer<Consumer<String>> setMapPickListener) {
        this.model              = model;
        this.commandManager     = commandManager;
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

        spinX = new Spinner<>(0, 49, 0);
        spinY = new Spinner<>(0, 49, 1);
        // כשהמשתמש משנה ציר - יוצרים פקודה עם הערכים הישנים והחדשים ומריצים דרך המנהל
        // ה-listener נותן לנו את הערך הישן (old) בחינם - בדיוק מה שהפקודה צריכה בשביל undo
        spinX.valueProperty().addListener((obs, old, val) -> {
            if (updatingFromCommand) return;
            commandManager.executeCommand(new ChangeAxesCommand(
                    old, spinY.getValue(), val, spinY.getValue(), this::applyAxes));
        });
        spinY.valueProperty().addListener((obs, old, val) -> {
            if (updatingFromCommand) return;
            commandManager.executeCommand(new ChangeAxesCommand(
                    spinX.getValue(), old, spinX.getValue(), val, this::applyAxes));
        });

        Spinner<Integer> spinK = new Spinner<>(1, 20, 5);
        spinK.valueProperty().addListener((obs, old, val) -> onKChanged.accept(val));

        metricBox = new ComboBox<>();
        metricBox.getItems().addAll(new EuclideanDistance(), new CosineDistance());
        metricBox.setCellFactory(listView -> createMetricCell());
        metricBox.setButtonCell(createMetricCell());
        metricBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(DistanceMetric metric) {
                return metric == null ? "" : metric.getName();
            }

            @Override
            public DistanceMetric fromString(String name) {
                return null;
            }
        });
        metricBox.setValue(model.getMetric());
        // כשהמשתמש מחליף מטריקה - יוצרים פקודה ששומרת את המטריקה הישנה מהמודל
        metricBox.setOnAction(e -> {
            if (updatingFromCommand) return;
            DistanceMetric oldMetric = model.getMetric();
            DistanceMetric newMetric = metricBox.getValue();
            // אם לא באמת השתנה כלום - לא יוצרים פקודה מיותרת
            if (oldMetric.getName().equals(newMetric.getName())) return;
            commandManager.executeCommand(new ChangeMetricCommand(
                    oldMetric, newMetric, this::applyMetric));
        });

        // מעביר את setMapPickListener ל-VectorLabView כדי שיוכל לבקש בחירה מהמפה
        Button arithmeticBtn = new Button("Vector Lab");
        arithmeticBtn.setOnAction(e ->
                new VectorLabView(model, onPathReady, setMapPickListener).show()
        );

        Button subspaceBtn = new Button("Subspace");
        subspaceBtn.setOnAction(e -> new SubspaceView(model, onFocusWord::accept).show());

        // כפתורי Undo/Redo - פשוט קוראים למנהל הפעולות, הוא כבר יודע מה לבטל
        Button undoBtn = new Button("↩ Undo");
        undoBtn.setMinWidth(80);
        undoBtn.setPrefWidth(80);
        undoBtn.setOnAction(e -> commandManager.undo());
        Button redoBtn = new Button("↪ Redo");
        redoBtn.setMinWidth(80);
        redoBtn.setPrefWidth(80);
        redoBtn.setOnAction(e -> commandManager.redo());

        HBox controls = new HBox(10,
                new Label("Search:"), searchField, searchBtn,
                new Label("PC-X:"), spinX,
                new Label("PC-Y:"), spinY,
                new Label("K:"), spinK,
                new Label("Metric:"), metricBox,
                arithmeticBtn, subspaceBtn,
                undoBtn, redoBtn
        );
        controls.setPadding(new Insets(8));
        return controls;
    }

    // ---- פונקציות apply - הפקודות קוראות להן ב-execute וב-undo ----

    // מפעיל שינוי צירים בפועל: מעדכן את הספינרים בממשק + את הקנבס
    // מרים את הדגל כדי שעדכון הספינרים לא ייצור פקודה חדשה
    private void applyAxes(int x, int y) {
        updatingFromCommand = true;
        spinX.getValueFactory().setValue(x);
        spinY.getValueFactory().setValue(y);
        updatingFromCommand = false;
        onAxesChanged.accept(x, y);
    }

    // מפעיל החלפת מטריקה בפועל: מעדכן את המודל + את ה-ComboBox בממשק
    private void applyMetric(DistanceMetric metric) {
        updatingFromCommand = true;
        metricBox.setValue(metric);
        updatingFromCommand = false;
        model.setMetric(metric);
    }

    private ListCell<DistanceMetric> createMetricCell() {
        return new ListCell<>() {
            @Override
            protected void updateItem(DistanceMetric metric, boolean empty) {
                super.updateItem(metric, empty);
                setText(empty || metric == null ? null : metric.getName());
            }
        };
    }
}
