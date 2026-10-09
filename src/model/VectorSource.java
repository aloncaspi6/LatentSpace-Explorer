package model;

import java.util.Map;

// ממשק לטעינת ווקטורי מילים מכל מקור נתונים
// הפרדה זו מאפשרת להחליף את מקור הנתונים (JSON, CSV, API וכו')
// בלי לשנות שורה אחת בשאר המערכת
public interface VectorSource {

    // טוען את כל ווקטורי המילים ומחזיר מפה של שם-מילה → WordVector
    // זורק Exception אם הטעינה נכשלה (קובץ חסר, פורמט שגוי וכו')
    Map<String, WordVector> load() throws Exception;
}