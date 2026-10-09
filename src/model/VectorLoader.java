//תהליך הטעינה של הווקטורים למערכת שלנו
package model;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Map;

public class VectorLoader {
    //נתחיל ביצירת ״מפה״ של השם של המילה והווקטור בצורת משתנה מספרי
    //ניצור שני מפות לכל אחד מהייצוגים שלנו, ייצוג מלא או ייצוג חלקי
    public static Map<String, WordVector> load(String fullPath, String pcaPath) throws Exception {
        Map<String, double[]> fullMap = loadJsonFile(fullPath);
        Map<String, double[]> pcaMap = loadJsonFile(pcaPath);
        //לאחר מכן נעבור על כל מילה שנמצאת בשני המפות וניצור ״מפה״ שמורכת משם המילה והווקטור שלה לפי איך שהגדרנו אותו
        Map<String, WordVector> result = new HashMap<>();
        for (String word : fullMap.keySet()) {
            if (pcaMap.containsKey(word)) {
                result.put(word, new WordVector(word, fullMap.get(word), pcaMap.get(word)));
            }
        }
        return result;
    }

    private static Map<String, double[]> loadJsonFile(String path) throws Exception {
        //יוצרים קובץ JSON, ויוצרים מפה ריקה שנמלא בהמשך
        JSONParser parser = new JSONParser();
        Map<String, double[]> map = new HashMap<>();

        //נבדוק האם הקובץ קיים לפני שננסה לפתוח אותו
        if (!new java.io.File(path).exists()) {
            throw new Exception("File not found: " + path);
        }

        //ננסה לפתוח את הקובץ, אם לא נצליח נזרוק שגיאה
        JSONArray array;
        try {
            array = (JSONArray) parser.parse(new FileReader(path));
        } catch (Exception e) {
            throw new Exception("Failed to parse JSON file: " + path + " — " + e.getMessage());
        }

        //עוברים על כל איבר במערך ומוציאים את השם ואת הווקטורים שלו
        for (Object obj : array) {
            JSONObject entry = (JSONObject) obj;
            String word = (String) entry.get("word");
            JSONArray vecArr = (JSONArray) entry.get("vector");

            //לאחר מכן נעבור בכל איבר ספציפי ונשמור את כל הווקטורים שלו במפה
            //נמיר כל ווקטור מJSON לdouble ואז נשמור במפה
            double[] vector = new double[vecArr.size()];
            for (int i = 0; i < vecArr.size(); i++) {
                vector[i] = ((Number) vecArr.get(i)).doubleValue();
            }
            map.put(word, vector);
        }
        return map;
    }
}
