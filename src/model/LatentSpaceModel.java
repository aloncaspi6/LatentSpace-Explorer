package model;

import distance.DistanceMetric;
import distance.EuclideanDistance;
import java.util.*;

//נתחיל בלהגדיר את המרחב שלנו
// אחראי בלעדית על ניהול הנתונים — המפה של המילים ומטריקת המרחק
// כל החישובים המתמטיים מואצלים ל-VectorMath
public class LatentSpaceModel {

    //נגדיר מטריצה ומפה שיכילו את הווקטורים
    private final Map<String, WordVector> words;
    private DistanceMetric metric;

    // אחראי על כל החישובים המתמטיים
    private VectorMath math;

    //בנאי ופונקצייות set וget
    public LatentSpaceModel(Map<String, WordVector> words) {
        this.words  = words;
        this.metric = new EuclideanDistance();
        this.math   = new VectorMath(words, metric);
    }

    public void setMetric(DistanceMetric metric) {
        this.metric = metric;
        // יצירת VectorMath חדש עם המטריקה המעודכנת
        this.math = new VectorMath(words, metric);
    }

    public DistanceMetric getMetric() {
        return metric;
    }

    public Map<String, WordVector> getWords() {
        return words;
    }

    public WordVector getWord(String word) {
        return words.get(word);
    }

    //פונקצייה שמחשבת מרחק בין שני ווקטורים במטריצה
    public double computeDistance(String word1, String word2) {
        WordVector w1 = words.get(word1);
        WordVector w2 = words.get(word2);
        if (w1 == null || w2 == null) throw new IllegalArgumentException("Word not found");
        return metric.compute(w1.getFullVector(), w2.getFullVector());
    }

    // ---- האצלה ל-VectorMath ----

    public List<WordVector> getNearestNeighbors(String word, int k) {
        return math.getNearestNeighbors(word, k);
    }

    public double[] computeArithmetic(List<String> wordNames, List<Integer> signs) {
        return math.computeArithmetic(wordNames, signs);
    }

    public WordVector findClosestExcluding(double[] vector, List<String> exclude) {
        return math.findClosestExcluding(vector, exclude);
    }

    public WordVector findClosest(double[] vector) {
        return math.findClosest(vector);
    }

    public double[] computeCentroid(List<String> wordNames) {
        return math.computeCentroid(wordNames);
    }

    public List<WordVector> getNearestToCentroid(double[] centroid, int k) {
        return math.getNearestToCentroid(centroid, k);
    }

    public double[] computeCustomAxis(String word1, String word2) {
        return math.computeCustomAxis(word1, word2);
    }

    public double projectOnAxis(double[] vector, double[] axis) {
        return math.projectOnAxis(vector, axis);
    }
}