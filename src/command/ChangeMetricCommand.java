// Encapsulates a distance-metric change so it can be executed, undone, and redone.
package command;

import distance.DistanceMetric;
import java.util.function.Consumer;

public class ChangeMetricCommand implements Command {

    //המטריקה לפני ואחרי השינוי
    private final DistanceMetric oldMetric, newMetric;

    //callback שמבצע את ההחלפה בפועל (עדכון המודל + ה-ComboBox בממשק)
    private final Consumer<DistanceMetric> applyMetric;

    //בנאי - שומר את המצב הישן והחדש ואת ה-callback
    public ChangeMetricCommand(DistanceMetric oldMetric, DistanceMetric newMetric,
                               Consumer<DistanceMetric> applyMetric) {
        this.oldMetric = oldMetric;
        this.newMetric = newMetric;
        this.applyMetric = applyMetric;
    }

    @Override
    //מפעיל את המטריקה החדשה
    public void execute() {
        applyMetric.accept(newMetric);
    }

    @Override
    //מחזיר את המטריקה הישנה
    public void undo() {
        applyMetric.accept(oldMetric);
    }

    @Override
    //נחזיר את שם הפעולה
    public String getName() {
        return "Change Metric";
    }
}
