package bin.xposed.NoWechatRevoke;

import static de.robv.android.xposed.XposedHelpers.findClass;

public final class Wechat {
    final static String PACKAGE_NAME = "com.tencent.mm";
    static ClassLoader CLASS_LOADER;

    //map
    static String MAP_CLASS_NAME;
    static String MAP_FUNCTION_NAME;

    //context
    static Class FIND_CLASS_Z;

    //db
    static String[] DB_CONTEXT_STRINGS;
    static Class FIND_CLASS_AH;

    //img
    static Class FIND_CLASS_F;
    static String HC;

    public static void setArgs(String[] mapStrings, String mmContextString, String[] dbContextStrings, String[] imgContextStrings) {
        //map
        MAP_CLASS_NAME = "com.tencent.mm.sdk.platformtools." + mapStrings[0];
        MAP_FUNCTION_NAME = mapStrings[1];

        //context
        FIND_CLASS_Z = findClass("com.tencent.mm.sdk.platformtools." + mmContextString, CLASS_LOADER);

        //db
        DB_CONTEXT_STRINGS = dbContextStrings;
        FIND_CLASS_AH = findClass("com.tencent.mm.model." + dbContextStrings[0], CLASS_LOADER);

        //img
        FIND_CLASS_F = findClass("com.tencent.mm." + imgContextStrings[0] + "." + imgContextStrings[1], CLASS_LOADER);
        HC = imgContextStrings[2];
    }

    public static boolean init(String version, ClassLoader classLoader) {
        Wechat.CLASS_LOADER = classLoader;

        switch (version) {
            case "6.0.0":
                setArgs(new String[]{"u", "D"}, "ai", new String[]{"bh", "sS", "qQ", "dGo"}, new String[]{"z", "n", "hO"});
                return true;

            case "6.0.2":
                setArgs(new String[]{"n", "B"}, "x", new String[]{"au", "Cj", "Ad", "esa"}, new String[]{"y", "g", "iN"});
                return true;

            case "6.1.0":
                setArgs(new String[]{"o", "B"}, "y", new String[]{"av", "CM", "AD", "eHj"}, new String[]{"y", "g", "je"});
                return true;

            case "6.2.0":
                setArgs(new String[]{"p", "z"}, "aa", new String[]{"ax", "tg", "rf", "bpS"}, new String[]{"y", "g", "gK"});
                return true;

            case "6.2.2":
                setArgs(new String[]{"q", "A"}, "ab", new String[]{"ax", "to", "rn", "brZ"}, new String[]{"y", "g", "hg"});
                return true;

            case "6.2.4":
                setArgs(new String[]{"p", "B"}, "x", new String[]{"ag", "tv", "ru", "btk"}, new String[]{"y", "f", "hf"});
                return true;

            case "6.2.5":
                setArgs(new String[]{"p", "B"}, "x", new String[]{"ah", "tI", "rG", "bvg"}, new String[]{"z", "f", "hs"});
                return true;

            case "6.3.5":
                setArgs(new String[]{"q", "C"}, "y", new String[]{"ai", "tO", "rM", "bts"}, new String[]{"z", "f", "hv"});
                return true;

            case "6.3.7":
                setArgs(new String[]{"r", "H"}, "z", new String[]{"ah", "tn", "rk", "bvh"}, new String[]{"z", "f", "hy"});
                return true;

            case "6.3.8":
                setArgs(new String[]{"r", "I"}, "z", new String[]{"ah", "tl", "rj", "bww"}, new String[]{"z", "f", "hC"});
                return true;

            case "6.3.9":
                setArgs(new String[]{"q", "J"}, "y", new String[]{"ah", "tr", "rk", "bzj"}, new String[]{"aa", "f", "hF"});
                return true;

            default:
                return false;
        }
    }
}