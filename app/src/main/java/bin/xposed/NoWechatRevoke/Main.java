package bin.xposed.NoWechatRevoke;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

import static de.robv.android.xposed.XposedHelpers.callMethod;
import static de.robv.android.xposed.XposedHelpers.callStaticMethod;
import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;

public class Main implements IXposedHookLoadPackage {

    Context mmContext;
    Activity mmActivity;

    public void handleLoadPackage(final LoadPackageParam lpparam) throws Throwable {
        if (lpparam.packageName.equals("com.tencent.mm")) {

            //get version
            Object activityThread = callStaticMethod(findClass("android.app.ActivityThread", null), "currentActivityThread");
            Context systemContext = (Context) callMethod(activityThread, "getSystemContext");
            String versionName = systemContext.getPackageManager().getPackageInfo(lpparam.packageName, 0).versionName;

            //wechat versions map
            class MMArgs {
                String className;
                String functionName;

                MMArgs(String className, String functionName) {
                    this.className = className;
                    this.functionName = functionName;
                }
            }

            Map<String, MMArgs> MMMap = new HashMap<String, MMArgs>();
            MMMap.put("6.2.0", new MMArgs("p", "z"));
            MMMap.put("6.2.2", new MMArgs("q", "A"));
            MMMap.put("6.2.4", new MMArgs("p", "B"));
            MMMap.put("6.2.5", new MMArgs("p", "B"));
            MMMap.put("6.3.0", new MMArgs("q", "C"));
            MMMap.put("6.3.5", new MMArgs("q", "C"));
            MMMap.put("6.3.7", new MMArgs("r", "H"));
            MMMap.put("6.3.8", new MMArgs("r", "I"));

            //current ver
            String[] versionNameSplits = versionName.split("\\.");
            String shortVersion = versionNameSplits[0] + "." + versionNameSplits[1] + "." + versionNameSplits[2];
            MMArgs currentArgs = MMMap.get(shortVersion);
            if (currentArgs == null)
                return;

            //get context
            findAndHookMethod("com.tencent.mm.ui.LauncherUI", lpparam.classLoader, "onCreate", Bundle.class, new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            mmContext = (Context) param.thisObject;
                            mmActivity = (Activity) mmContext;
                        }
                    }
            );

            //map
            findAndHookMethod("com.tencent.mm.sdk.platformtools." + currentArgs.className, lpparam.classLoader, currentArgs.functionName, String.class, String.class, String.class, new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            @SuppressWarnings("unchecked")
                            Map<String, String> map = (Map<String, String>) param.getResult();

                            String type = map.get(".sysmsg.$type");
                            if (type == null)
                                return;

                            if (type.equals("revokemsg")) {
                                map.put(".sysmsg.$type", null);
                                param.setResult(map);

                                if (mmContext != null) {
                                    String replacemsg = map.get(".sysmsg.revokemsg.replacemsg");
                                    replacemsg = replacemsg.replaceAll("撤回了一条消息$", "尝试撤回一条消息");

                                    //Toast
                                    //Toast.makeText(mmContext, replacemsg, Toast.LENGTH_LONG).show();

                                    //Notification
                                    NotificationCompat.Builder mBuilder =
                                            new NotificationCompat.Builder(mmContext)
                                                    .setAutoCancel(true)
                                                    .setSmallIcon(android.R.drawable.ic_dialog_info)
                                                    .setContentTitle("微信撤回")
                                                    .setContentText(replacemsg).setDefaults(Notification.DEFAULT_ALL);
                                    Intent resultIntent = new Intent(mmActivity, mmActivity.getClass());
                                    TaskStackBuilder stackBuilder = TaskStackBuilder.create(mmContext);
                                    stackBuilder.addParentStack(mmActivity.getClass());
                                    stackBuilder.addNextIntent(resultIntent);
                                    PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
                                    mBuilder.setContentIntent(resultPendingIntent);
                                    NotificationManager mNotificationManager = (NotificationManager) mmContext.getSystemService(Context.NOTIFICATION_SERVICE);
                                    mNotificationManager.notify(0, mBuilder.build());
                                }
                            }

                        }
                    }
            );
        }

        if (lpparam.packageName.equals("com.tencent.mobileqq")) {

            //hook args
            String className = "com.tencent.mobileqq.app.message.QQMessageFacade";
            String functionName = "a";

            findAndHookMethod(className, lpparam.classLoader, functionName, ArrayList.class, boolean.class, new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            param.setResult(null);
                        }
                    }
            );
        }
    }
}
