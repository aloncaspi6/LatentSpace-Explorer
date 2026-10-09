// Represents a word together with its full embedding vector and its PCA-reduced vector.
package model;

public class WordVector {
    //ממה יהיה מורכב הווקטור עצמו
    private  final String word;
    private final double[] fullVector;
    private final double[] pcaVector;
    //בנאי סטנדרטי לווקטור
    public WordVector(String word,double[] fullVector,double[] pcaVector) {
        this.word = word;
        this.fullVector = fullVector;
        this.pcaVector = pcaVector;
    }
    //פונקציות get לכל המשתנים בווקטור
    public String getWord() {
        return word;
    }
    public double[] getFullVector() {
        return fullVector;
    }
    public double[] getPcaVector() {
        return pcaVector;
    }
    //המרה של הווקטור לצורה שניתן לקרוא אותה
    public String toString() {
        return "model.WordVector{word='" + word + "', fullDims=" + fullVector.length +
                ", pcaDims=" + pcaVector.length + "}";
    }
}
