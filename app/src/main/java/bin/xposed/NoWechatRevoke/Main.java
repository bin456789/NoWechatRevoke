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
import android.widget.Toast;

import java.util.ArrayList;
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

            //hook args
            String className = "com.tencent.mm.sdk.platformtools.";
            String functionName;

            //only support 6.2.5+
            if (versionName.compareTo("6.2.5") < 0) {
                return;
            }

            //6.2.5
            if (versionName.startsWith("6.2.5")) {
                className += "p";
                functionName = "B";
            }
            //6.3.0, 6.3.5
            else if (versionName.startsWith("6.3.0")
                    || versionName.startsWith("6.3.5")) {
                className += "q";
                functionName = "C";
            }
            //6.3.7
            else {
                className += "r";
                functionName = "H";
            }

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
            findAndHookMethod(className, lpparam.classLoader, functionName, String.class, String.class, String.class, new XC_MethodHook() {
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
                                if (mmContext != null) {
                                    String replacemsg = (String) map.get(".sysmsg.revokemsg.replacemsg");
                                    replacemsg = replacemsg.replaceAll("撤回了一条消息$", "尝试撤回一条消息");
                                    Toast.makeText(mmContext, replacemsg, Toast.LENGTH_LONG).show();

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
