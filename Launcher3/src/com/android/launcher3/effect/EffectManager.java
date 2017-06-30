package com.android.launcher3.effect;

import java.lang.ref.WeakReference;
import java.util.HashMap;

import com.android.launcher3.Launcher;
import com.android.launcher3.LauncherAppState;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.res.Resources;
import android.content.res.Resources.NotFoundException;
import android.graphics.drawable.Drawable;
import android.util.Log;

public class EffectManager {
    private static final String TAG = "EffectManager";

    private HashMap<String, Effect> mEffects;

    public static final String SP_KEY_SELECTED_EFFECT = "current_page_switch_effect";

    private WeakReference<Launcher> mLauncherRef;

    private EffectManager() {
        mEffects = new HashMap();
    }

    private static class SingletonHolder {
        private static final EffectManager INSTANCE = new EffectManager();
    }

    public static EffectManager getInstance() {
        return SingletonHolder.INSTANCE;
    }

    public void loadThirdPartyEffects() {
        // TODO: load from external package
        // public abstract List<ApplicationInfo> getInstalledApplications(int flags);
        // public abstract Resources getResourcesForApplication(ApplicationInfo app)

        // get name resource id
        // Bundle.getInt(String key)

        // load name
        // public String getString(int id) throws NotFoundException {

        // get drawable resource id
        // Bundle.getInt(String key)

        // load drawable
        // public Drawable getDrawable(int id) throws NotFoundException {

        //Log.d(TAG, "loadEffects, " + mEffects.size() + " found.");
    }

    public void addEffect(Effect effect) {
        if (effect != null) {
            mEffects.put(effect.getIdentify(), effect);
            if (mLauncherRef != null && mLauncherRef.get() != null) {
                mLauncherRef.get().addEffect(this, effect);
            }
        }
    }

    public Effect getEffect(String identify) {
        if (identify != null && mEffects.containsKey(identify)) {
            return mEffects.get(identify);
        }
        return null;
    }

    public void persistCurrentEffect(Effect effect) {
        LauncherAppState appState = LauncherAppState.getInstance();
        SharedPreferences sp = appState.getContext().getSharedPreferences(
                appState.getSharedPreferencesKey(), Context.MODE_PRIVATE);
        if (effect == null) {
            sp.edit().remove(SP_KEY_SELECTED_EFFECT).apply();
        } else {
            String identify = effect.getIdentify();
            sp.edit().putString(SP_KEY_SELECTED_EFFECT, identify).apply();
        }
    }

    public Effect getCurrentEffect() {
        LauncherAppState appState = LauncherAppState.getInstance();
        SharedPreferences sp = appState.getContext().getSharedPreferences(
                appState.getSharedPreferencesKey(), Context.MODE_PRIVATE);
        String identify = sp.getString(SP_KEY_SELECTED_EFFECT, null);
        Effect effect = getEffect(identify);
        if (effect == null) {
            effect = getEffect(ClassicEffect.IDENTIFY);
        }
        return effect;
    }

    public void setLauncher(Launcher launcher) {
        mLauncherRef = new WeakReference<Launcher>(launcher);
    }
}
