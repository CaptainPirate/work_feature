/*===================================================================================================*	
 *  when  |      who     |    keyword           |        why         |         what                  *	
 *===================================================================================================*	
 *20160407|mengzhiming.wt|porting A1s enjoynotes| Port_EnjoyNotes | porting A1s enjoynotes to CMCC N2*	
*====================================================================================================*/

package com.wingtech.note;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class PreferenceUtils {
    private static final String KEY_BG_RANDOM_APPEAR = "pref_key_bg_random_appear";
    private static final String PREFERENCE_FONT_SIZE = "pref_font_size";
    private static final String PREFERENCE_SKETCH_COLOR = "pref_sketch_color";
    private static final String PREFERENCE_SKETCH_WIDTH = "pref_sketch_width";

    public static int getBgAppear(Context paramContext) {
        SharedPreferences localSharedPreferences = PreferenceManager
                .getDefaultSharedPreferences(paramContext);
        int i = localSharedPreferences.getInt(KEY_BG_RANDOM_APPEAR, 0);
        return i;
    }

    public static void setBgAppear(Context paramContext, int paramInt) {
        setIntegerPreference(paramContext, KEY_BG_RANDOM_APPEAR, paramInt);
    }

    private static boolean getBooleanPreference(Context paramContext, String paramString,
            boolean paramBoolean) {
        return PreferenceManager.getDefaultSharedPreferences(paramContext).getBoolean(paramString,
                paramBoolean);
    }

    public static int getFontSize(Context paramContext, int paramInt) {
        SharedPreferences localSharedPreferences = PreferenceManager
                .getDefaultSharedPreferences(paramContext);
        int i = localSharedPreferences.getInt(PREFERENCE_FONT_SIZE, paramInt);
        if (i >= ResourceParser.TextAppearanceResources.getResourcesSize()) {
            i = paramInt;
            localSharedPreferences.edit().putInt(PREFERENCE_FONT_SIZE, i).apply();
        }
        return i;
    }

    public static void setFontSize(Context paramContext, int paramInt) {
        setIntegerPreference(paramContext, PREFERENCE_FONT_SIZE, paramInt);
    }

    private static void setIntegerPreference(Context paramContext, String paramString, int paramInt) {
        PreferenceManager.getDefaultSharedPreferences(paramContext).edit()
                .putInt(paramString, paramInt).apply();
    }

    public static void setSketchColor(Context paramContext, int paramInt) {
        setIntegerPreference(paramContext, PREFERENCE_SKETCH_COLOR, paramInt);
    }

    public static void setSketchWidth(Context paramContext, int paramInt) {
        setIntegerPreference(paramContext, PREFERENCE_SKETCH_WIDTH, paramInt);
    }

    public static int getSketchColor(Context paramContext, int paramInt) {
        SharedPreferences localSharedPreferences = PreferenceManager
                .getDefaultSharedPreferences(paramContext);
        int i = localSharedPreferences.getInt(PREFERENCE_SKETCH_COLOR, paramInt);
        // if (i >= ResourceParser.SketchResources.getSketchColorSize()) {
        // i = paramInt;
        // localSharedPreferences.edit().putInt(PREFERENCE_SKETCH_COLOR,
        // i).apply();
        // }
        return i;
    }

    public static int getSketchWidth(Context paramContext, int paramInt) {
        SharedPreferences localSharedPreferences = PreferenceManager
                .getDefaultSharedPreferences(paramContext);
        int i = localSharedPreferences.getInt(PREFERENCE_SKETCH_WIDTH, paramInt);
        // if (i >= ResourceParser.SketchResources.getSketchWidthSize()) {
        // i = paramInt;
        // localSharedPreferences.edit().putInt(PREFERENCE_SKETCH_WIDTH,
        // i).apply();
        // }
        return i;
    }
}
