// Manages the application's word-vector data and active distance metric, delegating mathematical operations to VectorMath.
package model;

import distance.DistanceMetric;
import distance.EuclideanDistance;
import java.util.*;

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

    //פונקצייה שמחשבת מרחק בין שני ווקטורים metric
    public double computeDistance(String word1, String word2) {

        return math.computeDistance(word1, word2);
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
