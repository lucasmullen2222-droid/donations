package donations.client;

public class Alert {
    public final String name;
    public final double amount;
    public long startMs = -1;

    public Alert(String name, double amount) {
        this.name = name;
        this.amount = amount;
    }

    /**
     * 1 = 1M basic, 2 = 5M kinda good, 3 = 15M better,
     * 4 = 25M cool, 5 = 50M insane, 6 = 100M+ OMEGA
     */
    public int tier() {
        if (amount >= 100_000_000) return 6;
        if (amount >= 50_000_000) return 5;
        if (amount >= 25_000_000) return 4;
        if (amount >= 15_000_000) return 3;
        if (amount >= 5_000_000) return 2;
        return 1;
    }

    public long durationMs() {
        return switch (tier()) {
            case 6 -> 14000;
            case 5 -> 10000;
            case 4 -> 8000;
            case 3 -> 6500;
            case 2 -> 5000;
            default -> 4000;
        };
    }

    public String tag() {
        return switch (tier()) {
            case 6 -> "OMEGA!!!";
            case 5 -> "INSANE!!";
            case 4 -> "COOL!";
            case 3 -> "BETTER!";
            case 2 -> "NICE";
            default -> "PAID";
        };
    }

    public static String format(double v) {
        if (v >= 1e12) return trim(v / 1e12) + "T";
        if (v >= 1e9) return trim(v / 1e9) + "B";
        if (v >= 1e6) return trim(v / 1e6) + "M";
        if (v >= 1e3) return trim(v / 1e3) + "K";
        return trim(v);
    }

    private static String trim(double d) {
        String s = String.format("%.2f", d);
        if (s.contains(".")) s = s.replaceAll("0+$", "").replaceAll("\\.$", "");
        return s;
    }

    public String amountText() { return "$" + format(amount); }
}
