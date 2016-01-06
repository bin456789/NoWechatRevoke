package bin.xposed.NoWechatRevoke;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;

import java.util.ArrayList;
import java.util.Map;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

import static android.text.TextUtils.isEmpty;
import static de.robv.android.xposed.XposedHelpers.callMethod;
import static de.robv.android.xposed.XposedHelpers.callStaticMethod;
import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;
import static de.robv.android.xposed.XposedHelpers.getObjectField;
import static de.robv.android.xposed.XposedHelpers.newInstance;


public class Main implements IXposedHookLoadPackage {
    String TAG = "NoWechatRevoke";

    public void handleLoadPackage(final LoadPackageParam lpparam) throws Throwable {

        if (lpparam.packageName.equals("com.tencent.mobileqq")) {
            findAndHookMethod("com.tencent.mobileqq.app.message.QQMessageFacade", lpparam.classLoader,
                    "a", ArrayList.class, boolean.class, XC_MethodReplacement.DO_NOTHING);
        }

        if (lpparam.packageName.equals(Wechat.PACKAGE_NAME)) {
            //get version
            final Object activityThread = callStaticMethod(findClass("android.app.ActivityThread", null), "currentActivityThread");
            final Context systemContext = (Context) callMethod(activityThread, "getSystemContext");
            final String versionName = systemContext.getPackageManager().getPackageInfo(lpparam.packageName, 0).versionName;


            //current ver
            final String[] versionNameSplits = versionName.split("\\.");
            final String shortVersion = versionNameSplits[0] + "." + versionNameSplits[1] + "." + versionNameSplits[2];
            if (!Wechat.init(shortVersion, lpparam.classLoader))
                return;


            //map
            findAndHookMethod(Wechat.MAP_CLASS_NAME, lpparam.classLoader, Wechat.MAP_FUNCTION_NAME, String.class, String.class, String.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    @SuppressWarnings("unchecked")
                    Map<String, String> map = (Map<String, String>) param.getResult();
                    if (map == null)
                        return;

                    String type = map.get(".sysmsg.$type");
                    if (isEmpty(type))
                        return;

                    if (type.equals("revokemsg")) {
                        map.put(".sysmsg.$type", null);
                        param.setResult(map);

                        String replacemsg = map.get(".sysmsg.revokemsg.replacemsg");
                        replacemsg = replacemsg.replaceAll("撤回了一条消息$", "尝试撤回一条消息");

                        //Notification
                        Context mmContext = (Context) callStaticMethod(Wechat.FIND_CLASS_Z, "getContext");
                        Intent resultIntent = new Intent().setClassName(mmContext, "com.tencent.mm.ui.LauncherUI");
                        PendingIntent resultPendingIntent = TaskStackBuilder
                                .create(mmContext)
                                .addNextIntent(resultIntent)
                                .getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
                        NotificationCompat.Builder mBuilder =
                                new NotificationCompat.Builder(mmContext)
                                        .setAutoCancel(true)
                                        .setSmallIcon(android.R.drawable.ic_dialog_info)
                                        .setContentIntent(resultPendingIntent)
                                        .setContentTitle(replacemsg);

                        //db
                        Object dbContext = getObjectField(callMethod(callStaticMethod(Wechat.FIND_CLASS_AH, Wechat.DB_CONTEXT_STRINGS[1]), Wechat.DB_CONTEXT_STRINGS[2]), Wechat.DB_CONTEXT_STRINGS[3]);
                        String[] sqlArgs = {map.get(".sysmsg.revokemsg.newmsgid")};
                        Cursor localCursor = (Cursor) callMethod(dbContext, "rawQuery", "select type, content, imgPath from message where msgsvrid=?", sqlArgs);
                        if (localCursor.moveToFirst()) {
                            int $type = localCursor.getInt(0);
                            //show and update content only if content is text
                            if ($type == 1) {
                                String $content = localCursor.getString(1);
                                mBuilder.setContentText($content);
                                //modify
                                ContentValues contentValues = new ContentValues();
                                contentValues.put("content", $content + " (已撤回)");
                                callMethod(dbContext, "update", "message", contentValues, "msgsvrid=?", sqlArgs);
                            } else if ($type == 3) {
                                String $imgPath = localCursor.getString(2);
                                if (!isEmpty($imgPath)) {
                                    String sdcardImgPath = (String) callMethod(newInstance(Wechat.FIND_CLASS_F, dbContext), Wechat.HC, $imgPath);
                                    //bitmap might be blank
                                    Bitmap bitmap = BitmapFactory.decodeFile(sdcardImgPath);
                                    mBuilder.setLargeIcon(bitmap);
                                }
                            }
                        }
                        NotificationManager mNotificationManager = (NotificationManager) mmContext.getSystemService(Context.NOTIFICATION_SERVICE);
                        mNotificationManager.notify(0, mBuilder.build());
                    }
                }
            });
        }
    }
}

