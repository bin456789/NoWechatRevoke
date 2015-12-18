package bin.xposed.NoWechatRevoke;

import android.app.Activity;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

import static de.robv.android.xposed.XposedHelpers.callMethod;
import static de.robv.android.xposed.XposedHelpers.callStaticMethod;
import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;
import static de.robv.android.xposed.XposedHelpers.getObjectField;

public class Main implements IXposedHookLoadPackage {

    String TAG = "NoWechatRevoke";

    Context mmContext;
    Activity mmActivity;

    //sql context
    Class<?> AH;
    Class<?> C;
    Class<?> AE;
    Object tl;
    Object rj;
    Object bww;
    boolean isInitSqlContext = false;

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
            MMMap.put("6.0.0", new MMArgs("u", "D"));
            MMMap.put("6.0.2", new MMArgs("n", "B"));
            MMMap.put("6.1.0", new MMArgs("o", "B"));
            MMMap.put("6.2.0", new MMArgs("p", "z"));
            MMMap.put("6.2.2", new MMArgs("q", "A"));
            MMMap.put("6.2.4", new MMArgs("p", "B"));
            MMMap.put("6.2.5", new MMArgs("p", "B"));
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
                            if (map == null)
                                return;

                            String type = map.get(".sysmsg.$type");
                            if (type == null)
                                return;

                            if (type.equals("revokemsg")) {
                                map.put(".sysmsg.$type", null);
                                param.setResult(map);


                                String replacemsg = map.get(".sysmsg.revokemsg.replacemsg");
                                replacemsg = replacemsg.replaceAll("撤回了一条消息$", "尝试撤回一条消息");

                                //sql context
                                if (!isInitSqlContext) {
                                    AH = findClass("com.tencent.mm.model.ah", lpparam.classLoader);
                                    C = findClass("com.tencent.mm.model.c", lpparam.classLoader);
                                    AE = findClass("com.tencent.mm.storage.ae", lpparam.classLoader);
                                    tl = callStaticMethod(AH, "tl");
                                    rj = callMethod(tl, "rj");
                                    bww = getObjectField(rj, "bww");
                                    isInitSqlContext = true;
                                }

                                String msgid = map.get(".sysmsg.revokemsg.newmsgid");
                                String sql = "select type, content from message where msgsvrid=?";
                                String[] sqlArgs = {msgid};

                                String notifyContent;
                                Cursor localCursor = (Cursor) callMethod(bww, "rawQuery", sql, sqlArgs);
                                if (localCursor.moveToFirst()) {
                                    int $type = localCursor.getInt(0);
                                    //show and update content only if content is text
                                    if ($type == 1) {
                                        String $content = notifyContent = localCursor.getString(1);
                                        ContentValues contentValues = new ContentValues();
                                        contentValues.put("content", $content + " (已撤回)");
                                        callMethod(bww, "update", "message", contentValues, "msgsvrid=?", sqlArgs);
                                    } else {
                                        notifyContent = "";
                                    }


                                    if (mmContext != null) {
                                        //Notification
                                        NotificationCompat.Builder mBuilder =
                                                new NotificationCompat.Builder(mmContext)
                                                        .setAutoCancel(true)
                                                        .setSmallIcon(android.R.drawable.ic_dialog_info)
                                                        .setContentTitle(replacemsg)
                                                        .setContentText(notifyContent);

                                        Intent resultIntent = new Intent(mmActivity, mmActivity.getClass());
                                        TaskStackBuilder stackBuilder = TaskStackBuilder.create(mmContext);
                                        stackBuilder.addParentStack(mmActivity.getClass());
                                        stackBuilder.addNextIntent(resultIntent);
                                        PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
                                        mBuilder.setContentIntent(resultPendingIntent);
                                        NotificationManager mNotificationManager = (NotificationManager) mmContext.getSystemService(Context.NOTIFICATION_SERVICE);
                                        mNotificationManager.notify(0, mBuilder.build());


//                                        //不需要v4包，需要设置项目为compile sdk version 15
//                                        NotificationManager notificationManager = (NotificationManager) mmContext.getSystemService(Context.NOTIFICATION_SERVICE);
//                                        Notification notification = new Notification(android.R.drawable.ic_dialog_info, replacemsg, System.currentTimeMillis());
//                                        Intent notificationIntent = new Intent(mmContext, mmContext.getClass());
//                                        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
//                                        PendingIntent intent = PendingIntent.getActivity(mmContext, 0, notificationIntent, 0);
//                                        notification.setLatestEventInfo(mmContext, "replacemsg", notifyContent, intent);
//                                        notification.flags |= Notification.FLAG_AUTO_CANCEL;
//                                        notification.defaults |= Notification.DEFAULT_SOUND;
//                                        notification.defaults |= Notification.DEFAULT_VIBRATE;
//                                        notificationManager.notify(0, notification);
                                    }
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

            findAndHookMethod(className, lpparam.classLoader, functionName, ArrayList.class, boolean.class, XC_MethodReplacement.DO_NOTHING);
        }
    }
}
