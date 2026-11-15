package donations.client;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Reads DonutSMP style lines like "Player paid you $2.5M" or "Player paid you $ 100K". */
public class PaymentParser {
    private static final Pattern PAID_YOU = Pattern.compile(
        "(\\w{3,16}) paid you \\$\\s*([\\d,]*\\.?\\d+)\\s*([kKmMbBtT])?");

    public static Alert parse(String line) {
        Matcher m = PAID_YOU.matcher(line);
        if (!m.find()) return null;
        double v = parseNumber(m.group(2), m.group(3));
        return new Alert(m.group(1), v);
    }

    /** "25m" -> 25,000,000. Used by the /dono test command too. */
    public static double parseNumber(String num, String suffix) {
        double v = Double.parseDouble(num.replace(",", ""));
        if (suffix == null || suffix.isEmpty()) return v;
        return switch (Character.toLowerCase(suffix.charAt(0))) {
            case 'k' -> v * 1e3;
            case 'm' -> v * 1e6;
            case 'b' -> v * 1e9;
            case 't' -> v * 1e12;
            default -> v;
        };
    }

    public static double parseAmountArg(String s) {
        Matcher m = Pattern.compile("([\\d,]*\\.?\\d+)\\s*([kKmMbBtT])?").matcher(s.replace("$", ""));
        if (!m.matches()) throw new IllegalArgumentException("bad amount");
        return parseNumber(m.group(1), m.group(2));
    }
}
