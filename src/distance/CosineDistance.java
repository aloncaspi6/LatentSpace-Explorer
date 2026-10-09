// Calculates cosine distance between two vectors based on their directional similarity.
package distance;
public class CosineDistance implements DistanceMetric {
    @Override
    public double compute(double[] v1, double[] v2) {
        //נשתמש בנוסחא
        //A*B=|A| * |B| * cos(θ)
        //dot-מכפלה סקלרית
        //norm1/2-הגודל של הווקטר בריבוע
        double dot = 0, norm1 = 0, norm2 = 0;
        for (int i = 0; i < v1.length; i++) {
            dot += v1[i] * v2[i];
            norm1 += v1[i] * v1[i];
            norm2 += v2[i] * v2[i];
        }
        if (norm1 == 0 || norm2 == 0) return 1.0;
        //נסדר את הנוסחא ונקבל
        //cos(θ)= A*B/|A|*|B|
        //נבצע אחד פחות הזווית מכיוון שכאשר הם שווים נקבל זווית 1- תוצאה0, נקבל -1 כאשל הם הפוכים
        return 1.0 - (dot / (Math.sqrt(norm1) * Math.sqrt(norm2)));
    }

    @Override
    //נחזיר את סוג המרחק
    public String getName() {
        return "Cosine";
    }
}
