package bin.xposed.NoWechatRevoke;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;

import java.util.Map;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

import static android.text.TextUtils.isEmpty;
import static de.robv.android.xposed.XposedHelpers.callMethod;
import static de.robv.android.xposed.XposedHelpers.callStaticMethod;
import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;
import static de.robv.android.xposed.XposedHelpers.getObjectField;


public class Main implements IXposedHookLoadPackage {

    public void handleLoadPackage(final LoadPackageParam lpparam) throws Throwable {

//        //qq
//        if (lpparam.packageName.equals("com.tencent.mobileqq")) {
//            findAndHookMethod("com.tencent.mobileqq.app.message.QQMessageFacade", lpparam.classLoader,
//                    "a", ArrayList.class, boolean.class, XC_MethodReplacement.DO_NOTHING);
//        }

        //wechat
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
                        replacemsg = Wechat.PATTERN.matcher(replacemsg).replaceAll("尝试撤回上条消息");

                        //db
                        Object dbContext = getObjectField(callMethod(callStaticMethod(Wechat.FIND_CLASS_AH, Wechat.DB_CONTEXT_STRINGS[1]), Wechat.DB_CONTEXT_STRINGS[2]), Wechat.DB_CONTEXT_STRINGS[3]);

//                        //better way but not working
//                        String sql = " insert into message"
//                                + " (msgId,msgSvrId,type,status,createTime,talker,talkerId,content)"
//                                + " select"
//                                + " max(msgId)+1,msgSvrId,10000,status,createTime+1,talker,talkerId,'上条已撤回' as content"
//                                + " from message where msgSvrId =" + map.get(".sysmsg.revokemsg.newmsgid")
//                                + " limit 1;";
//                        callMethod(dbContext, "rawQuery", sql, null);

                        String[] sqlArgs = {map.get(".sysmsg.revokemsg.newmsgid")};
                        Cursor messageCursor = (Cursor) callMethod(dbContext, "rawQuery", "select * from message where msgsvrid=?", sqlArgs);

                        if (messageCursor.moveToFirst()) {
                            //thanks to fkzhang
                            ContentValues v = new ContentValues();
                            v.put("msgSvrId", map.get(".sysmsg.revokemsg.newmsgid"));
                            v.put("type", 10000);
                            v.put("status", messageCursor.getInt(messageCursor.getColumnIndex("status")));
                            v.put("createTime", messageCursor.getLong(messageCursor.getColumnIndex("createTime")) + 1);
                            v.put("talker", messageCursor.getString(messageCursor.getColumnIndex("talker")));
                            v.put("content", replacemsg);

                            //6.0.0 has no talkerId
                            int colIndex = messageCursor.getColumnIndex("talkerId");
                            if (colIndex >= 0)
                                v.put("talkerId", messageCursor.getLong(colIndex));

                            //get next msgId, unsafe?
                            Cursor getMaxMsdIdCursor = (Cursor) callMethod(dbContext, "rawQuery", "SELECT max(msgId) FROM message", null);
                            if (getMaxMsdIdCursor.moveToFirst()) {
                                v.put("msgId", getMaxMsdIdCursor.getLong(0) + 1);
                                //insert
                                callMethod(dbContext, "insert", "message", null, v);

                                //refresh msgId
                                if (Wechat.UPDATE_MSGID_STRINGS.length == 2) {
                                    callMethod(callMethod(Wechat.MESSAGE_TABLE_CONTEXT, Wechat.UPDATE_MSGID_STRINGS[0], "message"), Wechat.UPDATE_MSGID_STRINGS[1]);
                                } else {
                                    callMethod(callMethod(callStaticMethod(Wechat.FIND_CLASS_AH, Wechat.DB_CONTEXT_STRINGS[1]), Wechat.DB_CONTEXT_STRINGS[2]), Wechat.UPDATE_MSGID_STRINGS[0]);
                                }
                            }
                            getMaxMsdIdCursor.close();
                        }
                        messageCursor.close();
                    }
                }
            });
        }
    }
}

