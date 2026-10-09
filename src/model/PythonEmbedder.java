// Ensures vector files exist and runs the Python embedding script when they need to be generated.
package model;

import java.io.File;

public class PythonEmbedder {

    //מריץ את סקריפט הפייתון רק אם קבצי הפלט עדיין לא קיימים
    //מקבל את שם הסקריפט ואת רשימת קבצי הפלט שהוא אמור לייצר
    public static void ensureVectors(String scriptPath, String... outputFiles) throws Exception {
        //נבדוק האם כל קבצי הפלט כבר קיימים - אם כן, אין צורך להריץ שוב
        boolean allExist = true;
        for (String f : outputFiles) {
            if (!new File(f).exists()) {
                allExist = false;
                break;
            }
        }
        if (allExist) return;

        //נוודא שהסקריפט עצמו קיים לפני שננסה להריץ אותו
        if (!new File(scriptPath).exists()) {
            throw new Exception("Python script not found: " + scriptPath);
        }

        //הרצת פייתון מתוך ג'אווה - בדיוק כמו בדוגמה מהמטלה
        ProcessBuilder pb = new ProcessBuilder("python3", scriptPath);
        //מפנה את הפלט של הפייתון לקונסול של הג'אווה - כך רואים הודעות ושגיאות
        pb.inheritIO();
        Process p = pb.start();

        //המתנה לסיום יצירת הקובץ על ידי הפייתון
        int exitCode = p.waitFor();

        //אם הפייתון סיים עם קוד שגיאה - נזרוק שגיאה עם הקוד
        if (exitCode != 0) {
            throw new Exception("Python script failed with exit code: " + exitCode);
        }
    }
}
