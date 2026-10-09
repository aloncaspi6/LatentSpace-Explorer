// Defines the contract for loading word vectors from a data source.
package model;

import java.util.Map;

public interface VectorSource {

    // טוען את כל ווקטורי המילים ומחזיר מפה של שם-מילה → WordVector
    // זורק Exception אם הטעינה נכשלה (קובץ חסר, פורמט שגוי וכו')
    Map<String, WordVector> load() throws Exception;
}
