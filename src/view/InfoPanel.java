// Displays the selected word and its nearest neighbors.
package view;

import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import model.LatentSpaceModel;
import model.WordVector;
import java.util.List;

// אחראי בלעדית על פאנל הצד:
// הצגת המילה הנבחרת ורשימת K השכנים הקרובים ביותר שלה
public class InfoPanel {

    private final LatentSpaceModel model;
    private final VBox container;

    public InfoPanel(LatentSpaceModel model) {
        this.model = model;
        // תיבת מידע, כאשר לוחצים או מחפשים נקודה זה מציג את הבחירה ואת השכנים
        container = new VBox(10);
        container.setPadding(new Insets(10));
        container.setPrefWidth(200);
        container.setStyle("-fx-background-color: black;");
    }

    // מחזיר את הרכיב עצמו — MainView מוסיף אותו ל-layout
    public VBox getContainer() {
        return container;
    }

    // מנקה ומאכלס מחדש את הפאנל לפי המילה הנבחרת
    public void update(String focusedWord, int kNeighbors) {
        // מנקה כל מה שנמצא בפאנל בצד
        container.getChildren().clear();
        if (focusedWord == null) return;

        // מוסיף את המילה הנבחרת בכתב מודגש בצבע כתום
        Label title = new Label("Selected: " + focusedWord);
        title.setStyle("-fx-text-fill: orange; -fx-font-weight: bold; -fx-font-size: 13;");
        container.getChildren().add(title);

        // מוסיף כותרת של השכנים הכי קרובים לפי הרשימה שלהם
        // ואז מוסיף את השכנים בצבע ירוק עם המרחקים שלהם מהנקודה הנבחרת
        Label neighborsTitle = new Label("Nearest neighbors:");
        neighborsTitle.setStyle("-fx-text-fill: lightgray; -fx-font-size: 11;");
        container.getChildren().add(neighborsTitle);

        // הלולאה שעוברת K פעמים לפי דרישה ומחזירה את K השכנים עם המספר
        List<WordVector> neighbors = model.getNearestNeighbors(focusedWord, kNeighbors);
        for (int i = 0; i < neighbors.size(); i++) {
            WordVector n = neighbors.get(i);
            double dist = model.computeDistance(focusedWord, n.getWord());
            Label l = new Label((i + 1) + ". " + n.getWord() + String.format("  (%.2f)", dist));
            l.setStyle("-fx-text-fill: lime; -fx-font-size: 11;");
            container.getChildren().add(l);
        }
    }
}
