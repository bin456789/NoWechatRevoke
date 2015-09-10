package bin.xposed.NoWechatRevoke;

import android.content.Context;
import android.os.Bundle;
import android.widget.Toast;

import java.util.Map;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;

public class Main implements IXposedHookLoadPackage {

    Context context;

    public void handleLoadPackage(final LoadPackageParam lpparam) throws Throwable {
        if (!lpparam.packageName.equals("com.tencent.mm"))
            return;


        //get context
        findAndHookMethod("com.tencent.mm.ui.LauncherUI", lpparam.classLoader, "onCreate", Bundle.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                context = (Context) param.thisObject;
            }
        });


        //map
        findAndHookMethod("com.tencent.mm.sdk.platformtools.p", lpparam.classLoader, "B", String.class, String.class, String.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                Map map = (Map) param.getResult();
                if (map == null)
                    return;

                String type = (String) map.get(".sysmsg.$type");
                if (type == null)
                    return;

                if (type.equals("revokemsg")) {
                    map.put(".sysmsg.$type", null);
                    param.setResult(map);

                    //Toast
                    if (context != null) {
                        String replacemsg = (String) map.get(".sysmsg.revokemsg.replacemsg");
                        replacemsg = replacemsg.replaceAll("撤回了一条消息$", "尝试撤回一条消息");
                        Toast.makeText(context, replacemsg, Toast.LENGTH_LONG).show();
                    }
                }
            }
        });
    }
}