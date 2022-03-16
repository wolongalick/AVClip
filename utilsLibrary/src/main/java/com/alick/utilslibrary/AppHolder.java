package com.alick.utilslibrary;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Context;

import java.lang.reflect.InvocationTargetException;

/**
 * 功能: Application持有者
 * 作者: 崔兴旺
 * 日期: 2020/3/6 0006
 */
public class AppHolder {
    private static Application sApplication;

    private AppHolder() {
    }

    /**
     * 初始化
     *
     * @param context 任意context Application|Activity|Service|ContextProvider|BroadcastReceiver
     * @date 2019-06-20 11:40
     * @author wangzhenzhou
     */
    public static void init(Context context) {
        if (null == context) {
            init(getApplicationByReflect());
            return;
        }
        sApplication = (Application) context.getApplicationContext();
    }

    /**
     * 初始化
     *
     * @param application Application
     * @date 2019-06-20 11:40
     * @author wangzhenzhou
     */
    public static void init(final Application application) {
        if (sApplication == null) {
            if (application == null) {
                AppHolder.sApplication = getApplicationByReflect();
            } else {
                AppHolder.sApplication = application;
            }
        }
    }

    /**
     * 获取持有的Application
     *
     * @return android.app.Application
     * @date 2019-06-20 11:43
     * @author wangzhenzhou
     */
    public static Application getApp() {
        if (sApplication != null) {
            return sApplication;
        }
        Application app = getApplicationByReflect();
        init(app);
        return app;
    }

    /**
     * 反射获取Application实例
     *
     * @return android.app.Application
     * @date 2019-06-20 11:43
     * @author wangzhenzhou
     */
    private static Application getApplicationByReflect() {
        try {
            @SuppressLint("PrivateApi")
            Class<?> activityThread = Class.forName("android.app.ActivityThread");
            Object thread = activityThread.getMethod("currentActivityThread").invoke(null);
            Object app    = activityThread.getMethod("getApplication").invoke(thread);
            if (app == null) {
                throw new NullPointerException("u should init first");
            }
            return (Application) app;
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        throw new NullPointerException("u should init first");
    }
}
