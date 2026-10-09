// Defines the common contract for commands that support execute and undo operations.
package command;

public interface Command {

    //מבצע את הפעולה
    void execute();

    //מבטל את הפעולה - מחזיר את המערכת למצב שלפני execute
    void undo();

    //פונקציית גט רגילה שמחזירה את שם הפעולה (שימושי לדיבוג)
    String getName();
}
