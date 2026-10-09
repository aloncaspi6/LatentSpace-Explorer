// Performs mathematical operations on word vectors, including distances, neighbors, arithmetic, centroids, and projections.
package model;

import distance.DistanceMetric;
import java.util.*;

public class VectorMath {

    private final Map<String, WordVector> words;
    private final DistanceMetric metric;

    public VectorMath(Map<String, WordVector> words, DistanceMetric metric) {
        this.words  = words;
        this.metric = metric;
    }

    public double computeDistance(String word1, String word2) {
        WordVector w1 = words.get(word1);
        WordVector w2 = words.get(word2);
        if (w1 == null || w2 == null) throw new IllegalArgumentException("Word not found");
        return metric.compute(w1.getFullVector(), w2.getFullVector());
    }

    //פונקצייה שתמצא את השכן הקרוב ביותר
    public List<WordVector> getNearestNeighbors(String word, int k) {
        WordVector target = words.get(word);
        if (target == null) throw new IllegalArgumentException("Word not found: " + word);

        //משתמשים בפונקצייה של java.util אומרים לה למיין את הווקטורים לפי המרחק מהמילה הנבחרת
        PriorityQueue<WordVector> pq = new PriorityQueue<>(
                Comparator.comparingDouble(w -> metric.compute(target.getFullVector(), w.getFullVector()))
        );

        //נוסיף את כל המילים שהם לא המילה שנבחרה למיון לפונקצית מיון
        for (WordVector w : words.values()) {
            if (!w.getWord().equals(word)) pq.add(w);
        }

        //לאחר המיון נשלוף את k האיברים הראשונים, ההכי קרובים למילה הנבחרת ונוסיף אותם לרשימה
        List<WordVector> result = new ArrayList<>();
        for (int i = 0; i < k && !pq.isEmpty(); i++) {
            result.add(pq.poll());
        }
        return result;
    }

    // פונקציה לחישוב V1 ± V2 ± V3 — מקבלת שמות מילים ורשימת סימנים (+1 או -1)
    public double[] computeArithmetic(List<String> wordNames, List<Integer> signs) {
        int dims = words.values().iterator().next().getFullVector().length;
        double[] result = new double[dims];

        for (int i = 0; i < wordNames.size(); i++) {
            WordVector wv = words.get(wordNames.get(i));
            if (wv == null) throw new IllegalArgumentException("Word not found: " + wordNames.get(i));
            double[] vec = wv.getFullVector();
            int sign = signs.get(i);
            for (int j = 0; j < dims; j++) {
                result[j] += sign * vec[j];
            }
        }
        return result;
    }

    // מציאת השכן הקרוב ביותר תוך סינון מילות הקלט — כך שהתוצאה לא תהיה אחת מהמילים שהוזנו
    public WordVector findClosestExcluding(double[] vector, List<String> exclude) {
        WordVector best = null;
        double bestDist = Double.MAX_VALUE;
        for (WordVector w : words.values()) {
            if (exclude.contains(w.getWord())) continue;
            double d = metric.compute(vector, w.getFullVector());
            if (d < bestDist) {
                bestDist = d;
                best = w;
            }
        }
        return best;
    }

    // מחשבת ממוצע חשבוני של קבוצת מילים — וקטור ה"מרכז" של הקבוצה
    public double[] computeCentroid(List<String> wordNames) {
        if (wordNames.isEmpty()) throw new IllegalArgumentException("No words provided");
        int dims = words.values().iterator().next().getFullVector().length;
        double[] centroid = new double[dims];

        for (String name : wordNames) {
            WordVector wv = words.get(name);
            if (wv == null) throw new IllegalArgumentException("Word not found: " + name);
            for (int i = 0; i < dims; i++) {
                centroid[i] += wv.getFullVector()[i];
            }
        }
        // מחלקים בכמות המילים כדי לקבל ממוצע
        for (int i = 0; i < dims; i++) {
            centroid[i] /= wordNames.size();
        }
        return centroid;
    }

    // מוצאת K מילים הקרובות ביותר לווקטור נתון (למשל centroid)
    public List<WordVector> getNearestToCentroid(double[] centroid, int k) {
        PriorityQueue<WordVector> pq = new PriorityQueue<>(
                Comparator.comparingDouble(w -> metric.compute(centroid, w.getFullVector()))
        );
        pq.addAll(words.values());

        List<WordVector> result = new ArrayList<>();
        for (int i = 0; i < k && !pq.isEmpty(); i++) {
            result.add(pq.poll());
        }
        return result;
    }

    //פונקצייה לחישוב הציר בין שני מילים שנבחר
    public double[] computeCustomAxis(String word1, String word2) {
        WordVector w1 = words.get(word1);
        WordVector w2 = words.get(word2);
        if (w1 == null || w2 == null) throw new IllegalArgumentException("Word not found");

        //ניקח את כמות הווקטורים שיש למילים, נרוץ על כל ווקטור נחסר את הראשון מהשני ונקבל את הציר הרצוי
        double[] axis = new double[w1.getFullVector().length];
        for (int i = 0; i < axis.length; i++) {
            axis[i] = w2.getFullVector()[i] - w1.getFullVector()[i];
        }
        return axis;
    }

    //נציג את המילים על הציר
    public double projectOnAxis(double[] vector, double[] axis) {
        //נחשב היטל סקלרי
        //(A*B)/|B|
        //projection= (vector*axis)/|axis|
        //הגודל של הציר הוא המרחק מנקודה אחת לשנייה, האיבר הI של הציר הוא המרחק בין נקודה 2 לנקודה אחת במימד I
        double dot = 0, norm = 0;
        for (int i = 0; i < axis.length; i++) {
            dot  += vector[i] * axis[i];
            norm += axis[i] * axis[i];
        }
        return dot / Math.sqrt(norm);
    }
}
