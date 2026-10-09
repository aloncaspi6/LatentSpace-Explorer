package distance;

//נוסחת חישוב מרחק רגילה
public class EuclideanDistance implements DistanceMetric {
    @Override

    //√((v1[0]-v2[0])² + (v1[1]-v2[1])² + ... + (v1[99]-v2[99])²)
    public double compute(double[] v1, double[] v2) {
        double sum = 0;
        for (int i = 0; i < v1.length; i++) {
            double diff = v1[i] - v2[i];
            sum += diff * diff;
        }
        return Math.sqrt(sum);
    }

    @Override
    //נחזיר את סוג המרחק
    public String getName() {
        return "Euclidean";
    }
}