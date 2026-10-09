// Encapsulates a selection change so it can be executed, undone, and redone.
package command;

import java.util.List;
import java.util.function.BiConsumer;

public class ChangeSelectionCommand implements Command {

    //המצב לפני ואחרי - גם המילה הנבחרת וגם המסלול (יכולים להיות null)
    private final String oldFocus, newFocus;
    private final List<String> oldPath, newPath;

    //callback שמבצע את השינוי בפועל ב-MainView
    private final BiConsumer<String, List<String>> applySelection;

    //בנאי - שומר את המצב הישן והחדש ואת ה-callback
    public ChangeSelectionCommand(String oldFocus, List<String> oldPath,
                                  String newFocus, List<String> newPath,
                                  BiConsumer<String, List<String>> applySelection) {
        this.oldFocus = oldFocus;
        this.oldPath  = oldPath;
        this.newFocus = newFocus;
        this.newPath  = newPath;
        this.applySelection = applySelection;
    }

    @Override
    //מפעיל את הבחירה החדשה
    public void execute() {
        applySelection.accept(newFocus, newPath);
    }

    @Override
    //מחזיר את הבחירה הישנה (יכול להיות גם "כלום נבחר" אם oldFocus הוא null)
    public void undo() {
        applySelection.accept(oldFocus, oldPath);
    }

    @Override
    //נחזיר את שם הפעולה
    public String getName() {
        return "Change Selection";
    }
}
