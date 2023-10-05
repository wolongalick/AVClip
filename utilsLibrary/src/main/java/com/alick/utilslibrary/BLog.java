//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.alick.utilslibrary;

import android.annotation.SuppressLint;
import android.text.TextUtils;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.util.Random;

public class BLog {
    private static final String  DEFAULT_MESSAGE = "execute";
    private static final String  LINE_SEPARATOR  = System.getProperty("line.separator");
    private static final String  NULL_TIPS       = "Log with null object";
    private static final String  PARAM           = "Param";
    private static final String  NULL            = "null";
    public static final  int     JSON_INDENT     = 4;
    public static        boolean OPEN_LOG        = BuildConfig.DEBUG;
    public static        boolean OPEN_DOT_LOG        = false;
    public static        boolean DEBUG_LOG        = BuildConfig.DEBUG;

    private BLog() {
    }

    public static void v() {
        printLog(L.V, (String) null, "execute");
    }

    public static void v(Object msg) {
        printLog(L.V, (String) null, msg);
    }

    public static void v(String name, boolean value) {
        printLog(L.V, (String) null, name + ":" + (value ? "true" : "false"));
    }

    public static void v(String tag, Object... objects) {
        printLog(L.V, tag, objects);
    }

    public static void d() {
        printLog(L.D, (String) null, "execute");
    }

    public static void d(String msg) {
        printLog(L.D, (String) null, msg);
    }

    public static void d(String name, boolean value) {
        printLog(L.D, (String) null, name + ":" + (value ? "true" : "false"));
    }

    public static void d(String tag, Object... objects) {
        printLog(L.D, tag, objects);
    }

    public static void i() {
        printLog(L.I, (String) null, "execute");
    }

    public static void i(String msg) {
        printLog(L.I, (String) null, msg);
    }

    public static void i(String name, boolean value) {
        printLog(L.I, (String) null, name + ":" + (value ? "true" : "false"));
    }

    public static void i(String tag, Object... objects) {
        printLog(L.I, tag, objects);
    }

    public static void w() {
        printLog(L.W, (String) null, "execute");
    }

    public static void w(String msg) {
        printLog(L.W, (String) null, msg);
    }

    public static void w(String name, boolean value) {
        printLog(L.W, (String) null, name + ":" + (value ? "true" : "false"));
    }

    public static void w(String tag, Object... objects) {
        printLog(L.W, tag, objects);
    }

    public static void e() {
        printLog(L.E, (String) null, "execute");
    }

    public static void e(String msg) {
        printLog(L.E, (String) null, msg);
    }

    public static void e(String name, boolean value) {
        printLog(L.E, (String) null, name + ":" + (value ? "true" : "false"));
    }

    public static void e(String tag, Object... objects) {
        printLog(L.E, tag, objects);
    }

    public static void a() {
        printLog(L.A, (String) null, "execute");
    }

    public static void a(Object msg) {
        printLog(L.A, (String) null, msg);
    }

    public static void a(String name, boolean value) {
        printLog(L.A, (String) null, name + ":" + (value ? "true" : "false"));
    }

    public static void a(String tag, Object... objects) {
        printLog(L.A, tag, objects);
    }

    public static void json(String jsonFormat) {
        printLog(L.JSON, (String) null, jsonFormat);
    }

    public static void json(String tag, String jsonFormat) {
        printLog(L.JSON, tag, jsonFormat);
    }

    public static void xml(String xml) {
        printLog(L.XML, (String) null, xml);
    }

    public static void xml(String tag, String xml) {
        printLog(L.XML, tag, xml);
    }

    public static void file(File targetDirectory, Object msg) {
        printFile((String) null, targetDirectory, (String) null, msg);
    }

    public static void file(String tag, File targetDirectory, Object msg) {
        printFile(tag, targetDirectory, (String) null, msg);
    }

    public static void file(String tag, File targetDirectory, String fileName, Object msg) {
        printFile(tag, targetDirectory, fileName, msg);
    }

    @SuppressLint({"SwitchIntDef"})
    private static void printLog(L type, String tagStr, Object... objects) {
        if (!OPEN_LOG) {
            return;
        }
        String[] contents   = wrapperContent(tagStr, objects);
        String   tag        = contents[0];
        String   msg        = contents[1];
        String   headString = contents[2];
        switch (type) {
            case JSON:
                printJson(tag, msg, headString);
                break;
            default:
                printDefault(type, tag, headString + msg);
        }

    }

    private static void printFile(String tagStr, File targetDirectory, String fileName, Object objectMsg) {
        if (!OPEN_LOG) {
            return;
        }
        String[] contents   = wrapperContent(tagStr, objectMsg);
        String   tag        = contents[0];
        String   msg        = contents[1];
        String   headString = contents[2];
        printFile(tag, targetDirectory, fileName, headString, msg);
    }

    private static String[] wrapperContent(String tagStr, Object... objects) {
        StackTraceElement[] stackTrace      = Thread.currentThread().getStackTrace();
        int                 index           = 5;
        String              className       = stackTrace[index].getFileName();
        String              methodName      = stackTrace[index].getMethodName();
        int                 lineNumber      = stackTrace[index].getLineNumber();
        String              methodNameShort = methodName.substring(0, 1).toUpperCase() + methodName.substring(1);
        StringBuilder       stringBuilder   = new StringBuilder();
        stringBuilder.append("(").append(className).append(":").append(lineNumber).append(")#").append(methodNameShort).append(" :");
        String tag        = tagStr == null ? className : tagStr;
        String msg        = objects == null ? "Log with null object" : getObjectsString(objects);
        String headString = stringBuilder.toString();
        return new String[]{tag, msg, headString};
    }

    private static String getObjectsString(Object... objects) {
        if (objects.length == 0) {
            return "";
        } else if (objects.length > 1) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("\n");

            for (int i = 0; i < objects.length; ++i) {
                Object object = objects[i];
                if (object == null) {
                    stringBuilder.append("Param").append("[").append(i).append("]").append(" = ").append("null").append("\n");
                } else {
                    stringBuilder.append("Param").append("[").append(i).append("]").append(" = ").append(object.toString()).append("\n");
                }
            }

            return stringBuilder.toString();
        } else {
            Object object = objects[0];
            return object == null ? "null" : object.toString();
        }
    }

    public static void printDefault(L type, String tag, String msg) {
        int index      = 0;
        int maxLength  = 4000;
        int countOfSub = msg.length() / maxLength;
        if (countOfSub > 0) {
            for (int i = 0; i < countOfSub; ++i) {
                String sub = msg.substring(index, index + maxLength);
                printSub(type, tag, sub);
                index += maxLength;
            }

            printSub(type, tag, msg.substring(index, msg.length()));
        } else {
            printSub(type, tag, msg);
        }

    }

    private static void printSub(L type, String tag, String sub) {
        switch (type) {
            case V:
                Log.v(tag, sub);
                break;
            case D:
                Log.d(tag, sub);
                break;
            case I:
                Log.i(tag, sub);
                break;
            case W:
                Log.w(tag, sub);
                break;
            case E:
                Log.e(tag, sub);
                break;
            case A:
                Log.wtf(tag, sub);
        }

    }

    public static void printJson(String tag, String msg, String headString) {
        String message;
        try {
            if (msg.startsWith("{")) {
                JSONObject jsonObject = new JSONObject(msg);
                message = jsonObject.toString(4);
            } else if (msg.startsWith("[")) {
                JSONArray jsonArray = new JSONArray(msg);
                message = jsonArray.toString(4);
            } else {
                message = msg;
            }
        } catch (JSONException var9) {
            message = msg;
        }

        printLine(tag, true);
        message = headString + LINE_SEPARATOR + message;
        String[] lines = message.split(LINE_SEPARATOR);
        String[] var5  = lines;
        int      var6  = lines.length;

        for (int var7 = 0; var7 < var6; ++var7) {
            String line = var5[var7];
            Log.d(tag, "║ " + line);
        }

        printLine(tag, false);
    }

    public static void printFile(String tag, File targetDirectory, String fileName, String headString, String msg) {
        fileName = fileName == null ? getFileName() : fileName;
        if (save(targetDirectory, fileName, msg)) {
            Log.d(tag, headString + " save log success ! location is >>>" + targetDirectory.getAbsolutePath() + "/" + fileName);
        } else {
            Log.e(tag, headString + "save log fails !");
        }

    }

    private static boolean save(File dic, String fileName, String msg) {
        File file = new File(dic, fileName);

        try {
            OutputStream       outputStream       = new FileOutputStream(file);
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(outputStream, "UTF-8");
            outputStreamWriter.write(msg);
            outputStreamWriter.flush();
            outputStream.close();
            return true;
        } catch (FileNotFoundException var6) {
            var6.printStackTrace();
            return false;
        } catch (UnsupportedEncodingException var7) {
            var7.printStackTrace();
            return false;
        } catch (IOException var8) {
            var8.printStackTrace();
            return false;
        } catch (Exception var9) {
            var9.printStackTrace();
            return false;
        }
    }

    private static String getFileName() {
        Random        random        = new Random();
        StringBuilder stringBuilder = new StringBuilder("GSLog_");
        stringBuilder.append(Long.toString(System.currentTimeMillis() + (long) random.nextInt(10000)).substring(4));
        stringBuilder.append(".txt");
        return stringBuilder.toString();
    }

    public static boolean isEmpty(String line) {
        return TextUtils.isEmpty(line) || line.equals("\n") || line.equals("\t") || TextUtils.isEmpty(line.trim());
    }

    public static void printLine(String tag, boolean isTop) {
        if (isTop) {
            Log.d(tag, "╔═══════════════════════════════════════════════════════════════════════════════════════");
        } else {
            Log.d(tag, "╚═══════════════════════════════════════════════════════════════════════════════════════");
        }

    }

    public static enum L {
        V(0),
        D(1),
        I(2),
        W(3),
        E(4),
        A(5),
        JSON(6),
        XML(7);

        int value;

        private L(int value) {
            this.value = value;
        }
    }
}
