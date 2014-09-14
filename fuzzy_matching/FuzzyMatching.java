package fuzzymatching;

public class FuzzyMatching {

  private String str1, str2;
    private int[][] f;

    /**
     * This method returns the distance between two Strings
     * in Damerau-Levenstein Metric.<br>
     * The returned value gives the number of basic transformations
     * needed to convert str2 into str1.<br>
     */
    public int calculateDistance(String s1, String s2) {
        str1 = s1;
        str2 = s2;
        f = new int[str1.length() + 1][str2.length() + 1];
        f[0][0] = 0;
        for (int i = 0; i < str1.length(); i++) f[i][0] = i;
        for (int j = 0; j < str2.length(); j++) f[0][j] = j;
        for (int i = 1; i < str1.length(); i++) {
            for (int j = 1; j < str2.length(); j++) {
                int left = Math.min(f[i - 1][j] + 1, f[i][j - 1] + 1);
                int right;
                if (i > 1 && j > 1)
                    right = Math.min(f[i - 1][j - 1] + distance(i, j), f[i - 2][j - 2] + distance(i - 1, j) + distance(i, j - 1) + 1);
                else
                    right = f[i - 1][j - 1] + distance(i, j);
                f[i][j] = Math.min(left, right);
            }
        }
        return f[str1.length() - 1][str2.length() - 1];
    }

    private int distance(int i2, int j2) {
        if (i2 > 0 && j2 > 0 && i2 <= str1.length() && j2 <= str2.length()) {
            if (str1.charAt(i2 - 1) == str2.charAt(j2 - 1))
                return 0;
            else
                return 1;
        } else
            return 0;
    }
    public static void main(String[] args) {
       FuzzyMatching d = new FuzzyMatching();
       System.out.println(d.calculateDistance("גטבמנדסüךא","בטבמנדסüךמי"));
    }
    
}
