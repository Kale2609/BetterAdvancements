package betteradvancements.common.util;

public class ColorHelper {
    /**
     * Convert to integer RGBA value
     * Uses 255 as A value
     *
     * @param r integer red
     * @param g integer green
     * @param b integer blue
     *
     * @return single integer representation of the given ints
     */
    public static int RGB(int r, int g, int b)
    {
        return RGBA(r, g, b, 255);
    }

    /**
     * Convert to integer RGBA value
     *
     * @param r integer red
     * @param g integer green
     * @param b integer blue
     * @param a integer alpha
     *
     * @return single integer representation of the given ints
     */
    public static int RGBA(int r, int g, int b, int a)
    {
        return ((a & 255) << 24) | ((r & 255) << 16) | ((g & 255) << 8) | (b & 255);
    }

    /**
     * Convert to integer RGBA value
     * Uses 1.0F as A value
     *
     * @param red float red
     * @param green float green
     * @param blue float blue
     *
     * @return single integer representation of the given floats
     */
    public static int RGB(float red, float green, float blue)
    {
        return RGBA((int) (red * 255), (int) (green * 255), (int) (blue * 255), 255);
    }

    /**
     * Convert to integer RGBA value
     *
     * @param red float red
     * @param green float green
     * @param blue float blue
     * @param alpha float alpha
     *
     * @return single integer representation of the given floats
     */
    public static int RGB(float red, float green, float blue, float alpha)
    {
        return RGBA((int) (red * 255), (int) (green * 255), (int) (blue * 255), (int) (alpha * 255));
    }

    /**
     * Convert an #RRGGBB value to a int colour
     *
     * @param colour the #RRGGBB value
     * @return the int colour value or an {@link java.lang.IllegalArgumentException} if a mal formed input is given
     */
    public static int RGB(String colour)
    {
        if (!colour.startsWith("#") || !(colour.length() == 7)) throw new IllegalArgumentException("Use #RRGGBB format");
        return RGB(Integer.parseInt(colour.substring(1, 3), 16), Integer.parseInt(colour.substring(3, 5), 16), Integer.parseInt(colour.substring(5, 7), 16));
    }

    /**
     * Convert a #AARRGGBB value to an ARGB int.
     *
     * @param colour the #AARRGGBB value
     * @return the packed ARGB colour
     * @throws IllegalArgumentException if the input is not exactly #AARRGGBB
     */
    public static int ARGB(String colour) {
        if (!isARGB(colour)) {
            throw new IllegalArgumentException("Use #AARRGGBB format");
        }
        return (int) Long.parseLong(colour.substring(1), 16);
    }

    public static boolean isARGB(String colour) {
        if (colour == null || colour.length() != 9 || colour.charAt(0) != '#') {
            return false;
        }

        for (int i = 1; i < colour.length(); i++) {
            char c = colour.charAt(i);
            boolean decimal = c >= '0' && c <= '9';
            boolean upperHex = c >= 'A' && c <= 'F';
            boolean lowerHex = c >= 'a' && c <= 'f';
            if (!decimal && !upperHex && !lowerHex) {
                return false;
            }
        }
        return true;
    }

    public static String asARGBString(int colour) {
        return String.format("#%08X", colour);
    }

    public static String asRGBString(int colour) {
        return "#" + Integer.toHexString(colour).toUpperCase().substring(2);
    }
}
