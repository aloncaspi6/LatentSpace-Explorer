package distance;

//ממשק המגדיר חוזה לחישוב מרחק בין שני ווקטורים
public interface DistanceMetric {

    //פונקציית חישוב המרחק, ניתן לחשה מרחק על ידי קו ישר, או על ידי הזווית בין שני ההוקטורים
    double compute(double[] v1, double[] v2);

    //פונקציית גט רגילה על מנת להחזיר את סוג המרחק שחושב
    String getName();
}