// Encapsulates a PCA-axis change so it can be executed, undone, and redone.
package command;

import java.util.function.BiConsumer;

public class ChangeAxesCommand implements Command {

    //הצירים לפני ואחרי השינוי - נשמרים בבנאי כדי שנוכל לבטל
    private final int oldX, oldY, newX, newY;

    //callback שמבצע את השינוי בפועל
    //הפקודה לא מכירה את ה-View ישירות - רק "מה לעשות", לא "מי עושה"
    //כך חבילת command לא תלויה בחבילת view
    private final BiConsumer<Integer, Integer> applyAxes;

    //בנאי - שומר את המצב הישן והחדש ואת ה-callback
    public ChangeAxesCommand(int oldX, int oldY, int newX, int newY,
                             BiConsumer<Integer, Integer> applyAxes) {
        this.oldX = oldX;
        this.oldY = oldY;
        this.newX = newX;
        this.newY = newY;
        this.applyAxes = applyAxes;
    }

    @Override
    //מפעיל את הצירים החדשים
    public void execute() {
        applyAxes.accept(newX, newY);
    }

    @Override
    //מחזיר את הצירים הישנים
    public void undo() {
        applyAxes.accept(oldX, oldY);
    }

    @Override
    //נחזיר את שם הפעולה
    public String getName() {
        return "Change Axes";
    }
}
