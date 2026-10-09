// Manages command execution and the undo/redo stacks.
package command;

import java.util.Stack;

public class CommandManager {

    //מחסנית הפעולות שבוצעו - הפעולה האחרונה תמיד למעלה
    private final Stack<Command> undoStack = new Stack<>();

    //מחסנית הפעולות שבוטלו - מאפשרת Redo
    private final Stack<Command> redoStack = new Stack<>();

    //מבצע פעולה חדשה ושומר אותה בהיסטוריה
    public void executeCommand(Command cmd) {
        cmd.execute();
        undoStack.push(cmd);
        //פעולה חדשה מבטלת את היסטוריית ה-Redo
        //כי אחרי שביטלנו פעולות וביצענו פעולה חדשה - ה"עתיד" הישן כבר לא רלוונטי
        //(בדיוק כמו ב-Word או Photoshop)
        redoStack.clear();
    }

    //מבטל את הפעולה האחרונה שבוצעה
    public void undo() {
        //אם אין מה לבטל - לא עושים כלום
        if (undoStack.isEmpty()) return;
        //שולפים את הפעולה האחרונה, מבטלים אותה ומעבירים למחסנית ה-Redo
        Command cmd = undoStack.pop();
        cmd.undo();
        redoStack.push(cmd);
    }

    //מבצע מחדש את הפעולה האחרונה שבוטלה
    public void redo() {
        //אם אין מה לבצע מחדש - לא עושים כלום
        if (redoStack.isEmpty()) return;
        //שולפים את הפעולה שבוטלה, מבצעים אותה שוב ומחזירים למחסנית ה-Undo
        Command cmd = redoStack.pop();
        cmd.execute();
        undoStack.push(cmd);
    }
}
