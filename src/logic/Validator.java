package logic;

public class Validator {
    public static boolean notBlank(String text) {
        return text != null && !text.isBlank();
    }

    public static boolean validEmail(String text) {
        return notBlank(text) && text.trim().matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    }

    public static boolean validPhone(String text) {
        return notBlank(text) && text.trim().matches("^\\+?[0-9]{7,15}$");
    }

    public static boolean positiveInt(String text) {
        if (!notBlank(text)) {
            return false;
        }
        try {
            return Integer.parseInt(text.trim()) > 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}