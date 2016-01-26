package bin.xposed.NoWechatRevoke;

import java.lang.reflect.Constructor;
import java.util.regex.Pattern;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;

import static de.robv.android.xposed.XposedHelpers.findClass;
import static de.robv.android.xposed.XposedHelpers.findConstructorExact;

public final class Wechat {
    final static String PACKAGE_NAME = "com.tencent.mm";
    static String VERSION;
    static ClassLoader CLASS_LOADER;

    //map
    static String MAP_CLASS_NAME;
    static String MAP_FUNCTION_NAME;
    final static Pattern PATTERN = Pattern.compile("撤回了一条消息$");

    //db
    static String[] DB_CONTEXT_STRINGS;
    static Class FIND_CLASS_AH;

    //msgId
    static Object MESSAGE_TABLE_CONTEXT;
    static String[] UPDATE_MSGID_STRINGS;


    public static boolean init(String version, ClassLoader classLoader) {
        Wechat.CLASS_LOADER = classLoader;
        Wechat.VERSION = version;

        switch (version) {
            case "6.0.0":
                setArgs(new String[]{"u", "D"}, new String[]{"bh", "sS", "qQ", "dGo"}, new String[]{"Cv", "aSC"});
                Constructor CONSTRUCTOR_AY = findConstructorExact(findClass("com.tencent.mm.storage.ay", CLASS_LOADER),
                        findClass("com.tencent.mm.at.h", CLASS_LOADER));

                XposedBridge.hookMethod(CONSTRUCTOR_AY, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        MESSAGE_TABLE_CONTEXT = param.thisObject;
                    }
                });
                return true;

            case "6.0.2": {
                setArgs(new String[]{"n", "B"}, new String[]{"au", "Cj", "Ad", "esa"}, new String[]{"EQ", "bjk"});

                Constructor CONSTRUCTOR_AP = findConstructorExact(findClass("com.tencent.mm.storage.ap", CLASS_LOADER),
                        findClass("com.tencent.mm.ap.g", CLASS_LOADER),
                        findClass("com.tencent.mm.storage.am", CLASS_LOADER),
                        findClass("com.tencent.mm.storage.an", CLASS_LOADER));

                XposedBridge.hookMethod(CONSTRUCTOR_AP, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        MESSAGE_TABLE_CONTEXT = param.thisObject;
                    }
                });

                return true;
            }


            case "6.1.0": {
                setArgs(new String[]{"o", "B"}, new String[]{"av", "CM", "AD", "eHj"}, new String[]{"Go", "bpj"});

                Constructor CONSTRUCTOR_AP = findConstructorExact(findClass("com.tencent.mm.storage.ap", CLASS_LOADER),
                        findClass("com.tencent.mm.aq.g", CLASS_LOADER),
                        findClass("com.tencent.mm.storage.am", CLASS_LOADER),
                        findClass("com.tencent.mm.storage.an", CLASS_LOADER));

                XposedBridge.hookMethod(CONSTRUCTOR_AP, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        MESSAGE_TABLE_CONTEXT = param.thisObject;
                    }
                });

                return true;
            }

            case "6.2.0":
                setArgs(new String[]{"p", "z"}, new String[]{"ax", "tg", "rf", "bpS"}, new String[]{"aHT"});
                return true;

            case "6.2.2":
                setArgs(new String[]{"q", "A"}, new String[]{"ax", "to", "rn", "brZ"}, new String[]{"aJg"});
                return true;

            case "6.2.4":
                setArgs(new String[]{"p", "B"}, new String[]{"ag", "tv", "ru", "btk"}, new String[]{"aLE"});
                return true;

            case "6.2.5":
                setArgs(new String[]{"p", "B"}, new String[]{"ah", "tI", "rG", "bvg"}, new String[]{"aNH"});
                return true;

            case "6.3.5":
                setArgs(new String[]{"q", "C"}, new String[]{"ai", "tO", "rM", "bts"}, new String[]{"aPy"});
                return true;

            case "6.3.7":
                setArgs(new String[]{"r", "H"}, new String[]{"ah", "tn", "rk", "bvh"}, new String[]{"aRo"});
                return true;

            case "6.3.8":
                setArgs(new String[]{"r", "I"}, new String[]{"ah", "tl", "rj", "bww"}, new String[]{"aTT"});
                return true;

            case "6.3.9":
                setArgs(new String[]{"q", "J"}, new String[]{"ah", "tr", "rk", "bzj"}, new String[]{"aVP"});
                return true;

            case "6.3.11":
                setArgs(new String[]{"q", "J"}, new String[]{"ah", "tD", "rs", "bCw"}, new String[]{"aXP"});
                return true;

            default:
                return false;
        }
    }

    public static void setArgs(String[] mapStrings, String[] dbContextStrings, String[] updateMsgIdStrings) {
        //map
        MAP_CLASS_NAME = "com.tencent.mm.sdk.platformtools." + mapStrings[0];
        MAP_FUNCTION_NAME = mapStrings[1];

        //db
        DB_CONTEXT_STRINGS = dbContextStrings;
        FIND_CLASS_AH = findClass("com.tencent.mm.model." + dbContextStrings[0], CLASS_LOADER);

        //msgId
        UPDATE_MSGID_STRINGS = updateMsgIdStrings;
    }
}