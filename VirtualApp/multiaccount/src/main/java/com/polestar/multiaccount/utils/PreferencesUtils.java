package com.polestar.multiaccount.utils;


import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import com.polestar.multiaccount.MApp;
import com.polestar.multiaccount.constant.AppConstants;

import java.io.File;

/**
 * Created by yxx on 2016/7/15.
 */
public class PreferencesUtils {

    public static String PREFERENCE_NAME = AppConstants.PREFERENCE_NAME;

    private PreferencesUtils() {
        throw new AssertionError();
    }

    /**
     * put string preferences
     *
     * @param context
     * @param key     The name of the preference to modify
     * @param value   The new value for the preference
     * @return True if the new values were successfully written to persistent storage.
     */
    public static boolean putString(Context context, String key, String value) {
        SharedPreferences settings = context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(key, value);
        return editor.commit();
    }

    /**
     * get string preferences
     *
     * @param context
     * @param key     The name of the preference to retrieve
     * @return The preference value if it exists, or null. Throws ClassCastException if there is a preference with this
     * name that is not a string
     * @see #getString(Context, String, String)
     */
    public static String getString(Context context, String key) {
        return getString(context, key, null);
    }

    /**
     * get string preferences
     *
     * @param context
     * @param key          The name of the preference to retrieve
     * @param defaultValue Value to return if this preference does not exist
     * @return The preference value if it exists, or defValue. Throws ClassCastException if there is a preference with
     * this name that is not a string
     */
    public static String getString(Context context, String key, String defaultValue) {
        if (context == null) return defaultValue;
        SharedPreferences settings = context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE);
        return settings.getString(key, defaultValue);
    }

    /**
     * put int preferences
     *
     * @param context
     * @param key     The name of the preference to modify
     * @param value   The new value for the preference
     * @return True if the new values were successfully written to persistent storage.
     */
    public static boolean putInt(Context context, String key, int value) {
        SharedPreferences settings = context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.putInt(key, value);
        return editor.commit();
    }

    /**
     * get int preferences
     *
     * @param context
     * @param key     The name of the preference to retrieve
     * @return The preference value if it exists, or -1. Throws ClassCastException if there is a preference with this
     * name that is not a int
     * @see #getInt(Context, String, int)
     */
    public static int getInt(Context context, String key) {
        return getInt(context, key, -1);
    }

    /**
     * get int preferences
     *
     * @param context
     * @param key          The name of the preference to retrieve
     * @param defaultValue Value to return if this preference does not exist
     * @return The preference value if it exists, or defValue. Throws ClassCastException if there is a preference with
     * this name that is not a int
     */
    public static int getInt(Context context, String key, int defaultValue) {
        SharedPreferences settings = context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE);
        return settings.getInt(key, defaultValue);
    }

    /**
     * put long preferences
     *
     * @param context
     * @param key     The name of the preference to modify
     * @param value   The new value for the preference
     * @return True if the new values were successfully written to persistent storage.
     */
    public static boolean putLong(Context context, String key, long value) {
        SharedPreferences settings = context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.putLong(key, value);
        return editor.commit();
    }

    /**
     * get long preferences
     *
     * @param context
     * @param key     The name of the preference to retrieve
     * @return The preference value if it exists, or -1. Throws ClassCastException if there is a preference with this
     * name that is not a long
     * @see #getLong(Context, String, long)
     */
    public static long getLong(Context context, String key) {
        return getLong(context, key, -1);
    }

    /**
     * get long preferences
     *
     * @param context
     * @param key          The name of the preference to retrieve
     * @param defaultValue Value to return if this preference does not exist
     * @return The preference value if it exists, or defValue. Throws ClassCastException if there is a preference with
     * this name that is not a long
     */
    public static long getLong(Context context, String key, long defaultValue) {
        if (context == null) return defaultValue;
        SharedPreferences settings = context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE);
        return settings.getLong(key, defaultValue);
    }

    /**
     * put float preferences
     *
     * @param context
     * @param key     The name of the preference to modify
     * @param value   The new value for the preference
     * @return True if the new values were successfully written to persistent storage.
     */
    public static boolean putFloat(Context context, String key, float value) {
        SharedPreferences settings = context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.putFloat(key, value);
        return editor.commit();
    }

    /**
     * get float preferences
     *
     * @param context
     * @param key     The name of the preference to retrieve
     * @return The preference value if it exists, or -1. Throws ClassCastException if there is a preference with this
     * name that is not a float
     * @see #getFloat(Context, String, float)
     */
    public static float getFloat(Context context, String key) {
        return getFloat(context, key, -1);
    }

    /**
     * get float preferences
     *
     * @param context
     * @param key          The name of the preference to retrieve
     * @param defaultValue Value to return if this preference does not exist
     * @return The preference value if it exists, or defValue. Throws ClassCastException if there is a preference with
     * this name that is not a float
     */
    public static float getFloat(Context context, String key, float defaultValue) {
        if (context == null) return defaultValue;
        SharedPreferences settings = context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE);
        return settings.getFloat(key, defaultValue);
    }

    /**
     * put boolean preferences
     *
     * @param context
     * @param key     The name of the preference to modify
     * @param value   The new value for the preference
     * @return True if the new values were successfully written to persistent storage.
     */
    public static boolean putBoolean(Context context, String key, boolean value) {
        SharedPreferences settings = context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean(key, value);
        return editor.commit();
    }

    /**
     * get boolean preferences, default is false
     *
     * @param context
     * @param key     The name of the preference to retrieve
     * @return The preference value if it exists, or false. Throws ClassCastException if there is a preference with this
     * name that is not a boolean
     * @see #getBoolean(Context, String, boolean)
     */
    public static boolean getBoolean(Context context, String key) {
        return getBoolean(context, key, false);
    }

    /**
     * get boolean preferences
     *
     * @param context
     * @param key          The name of the preference to retrieve
     * @param defaultValue Value to return if this preference does not exist
     * @return The preference value if it exists, or defValue. Throws ClassCastException if there is a preference with
     * this name that is not a boolean
     */
    public static boolean getBoolean(Context context, String key, boolean defaultValue) {
        if (context == null) return  defaultValue;
        SharedPreferences settings = context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE);
        return settings.getBoolean(key, defaultValue);
    }

    public static void setCloneGuideShowed(Context c){
        putBoolean(c, AppConstants.PreferencesKey.SHOWN_CLONE_GUIDE, true);
    }

    public static boolean hasShownCloneGuide(Context c){
        return getBoolean(c, AppConstants.PreferencesKey.SHOWN_CLONE_GUIDE, false);
    }

    public static boolean hasShownLongClickGuide(Context c){
        return getBoolean(c, AppConstants.PreferencesKey.SHOWN_LONG_CLICK_GUIDE, false);
    }

    public static void setLongClickGuideShowed(Context c){
        putBoolean(c, AppConstants.PreferencesKey.SHOWN_LONG_CLICK_GUIDE, true);
    }

    public static String getEncodedPatternPassword(Context c) {
        return getString(c, AppConstants.PreferencesKey.ENCODED_PATTERN_PWD);
    }

    public static void setEncodedPatternPassword(Context c, String pwd) {
        putString(c, AppConstants.PreferencesKey.ENCODED_PATTERN_PWD, pwd );
    }

    public static void setLockScreen(Context c, boolean b) {
        putBoolean(c,AppConstants.PreferencesKey.IS_LOCKER_SCREEN, b);
    }

    public static boolean isLockScreen(Context c ) {
        return getBoolean(c,AppConstants.PreferencesKey.IS_LOCKER_SCREEN, false);
    }

    public static boolean isSafeQuestionSet(Context c ) {
        return !TextUtils.isEmpty(getSafeAnswer(c));
    }

    public static String getSafeAnswer(Context c) {
        return getString(c, AppConstants.PreferencesKey.SAFE_QUESTION_ANSWER, null);
    }

    public static void setSafeAnswer(Context c, String answer) {
        putString(c, AppConstants.PreferencesKey.SAFE_QUESTION_ANSWER, answer);
    }

    public static int getSafeQuestionId(Context c) {
        return getInt(c, AppConstants.PreferencesKey.SAFE_QUESTION_ID, 0);
    }

    public static void setSafeQuestionId(Context c, int id) {
        putInt(c, AppConstants.PreferencesKey.SAFE_QUESTION_ID, id);
    }

    public static void setCustomizedQuestion(Context c, String strQuestion) {
        putString(c, AppConstants.PreferencesKey.CUSTOMIZED_SAFE_QUESTION, strQuestion);
    }

    public static String getCustomizedQuestion(Context c) {
        return getString(c, AppConstants.PreferencesKey.CUSTOMIZED_SAFE_QUESTION, null);
    }

    public static boolean getAppLockInVisiablePatternPath(Context c) {
        return getBoolean(c, AppConstants.PreferencesKey.APP_LOCK_INVISIBLE_PATTERN_PATH, false);
    }

    public static void setLockerEnabled(Context c, boolean enabled) {
        putBoolean(c,AppConstants.PreferencesKey.LOCKER_FEATURE_ENABLED,enabled);
    }

    public static boolean isLockerEnabled(Context c) {
        return getBoolean(c,AppConstants.PreferencesKey.LOCKER_FEATURE_ENABLED,false);
    }

    public static void setLoveApp(  boolean b) {
        putInt(MApp.getApp(),"love_app", b? 1:-1);
    }

    public static int getLoveApp() {
        return getInt(MApp.getApp(),"love_app", 0);
    }

    public static void setRated(boolean b) {
        putBoolean(MApp.getApp(),"is_rated", b);
    }

    public static boolean isRated() {
        return getBoolean(MApp.getApp(),"is_rated", false);
    }

    public static void updateRateDialogTime(Context c) {
        putLong(c, "last_rate_dialog", System.currentTimeMillis());
    }

    public static void updateIconAdClickTime(Context c) {
        putLong(c, "last_icon_ad_click", System.currentTimeMillis());
    }


    public static long getRateDialogTime(Context c) {
        long last = getLong(c, "last_rate_dialog",-1);
        if (last == -1) {
            last = CommonUtils.getInstallTime(c, c.getPackageName());
        }
        return last;
    }

    public static long getLastIconAdClickTime(Context c) {
        return getLong(c, "last_icon_ad_click",-1);
    }

    public static void setApplockGuideShowed() {
        putBoolean(MApp.getApp(),"app_lock_guide_showed", true);
    }

    public static boolean isApplockGuideShowed() {
        return getBoolean(MApp.getApp(),"app_lock_guide_showed",false);
    }

    public static boolean isFirstStart(String name) {
        return getLong(MApp.getApp(), name+"_first_start", -1) == -1;
    }

    public static void setStarted(String name) {
        putLong(MApp.getApp(), name+"_first_start", System.currentTimeMillis());
    }

    public static void setShortCutCreated() {
        putBoolean(MApp.getApp(),"super_clone_shortcut",true);
    }

    public static boolean isShortCutCreated() {
        return getBoolean(MApp.getApp(),"super_clone_shortcut",false)
                || hasShownCloneGuide(MApp.getApp());
        //avoid create short cut for updated user
    }

    public static void setInstallChannel(String channel) {
        putString(MApp.getApp(), "install_channel", channel);
    }

    public static String getInstallChannel() {
        return getString(MApp.getApp(), "install_channel", null);
    }

    public static void setGMSEnable(boolean enable) {
        File stateFile = new File(MApp.getApp().getFilesDir(), "gms_disable");
        try {
            if (!enable) {
                stateFile.createNewFile();
            } else {
                stateFile.delete();
            }
        } catch (Throwable e) {
            MLogs.logBug(MLogs.getStackTraceString(e));
        }
    }

    public static boolean isGMSEnable() {
        File stateFile = new File(MApp.getApp().getFilesDir(), "gms_disable");
        return !stateFile.exists();
    }

    public static void setStartPageStatus(boolean shown) {
        putBoolean(MApp.getApp(), "start_page_status", shown);
    }

    public static boolean hasShownStartPage() {
        //return false;
        return getBoolean(MApp.getApp(), "start_page_status");
    }
}
