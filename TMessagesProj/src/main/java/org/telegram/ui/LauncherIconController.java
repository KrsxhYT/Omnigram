package org.telegram.ui;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;

import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.R;

public final class LauncherIconController {
    private LauncherIconController() {}

    /**
     * Repairs only invalid launcher states. Avoid rewriting PackageManager
     * component state on every application startup when one valid icon is
     * already enabled.
     */
    public static void tryFixLauncherIconIfNeeded() {
        Context context = ApplicationLoader.applicationContext;
        if (context == null) return;

        LauncherIcon selected = null;
        int enabledCount = 0;
        for (LauncherIcon icon : LauncherIcon.values()) {
            if (isEnabled(icon)) {
                enabledCount++;
                if (selected == null) {
                    selected = icon;
                }
            }
        }

        if (enabledCount == 1) {
            return;
        }

        setIcon(selected == null ? LauncherIcon.DEFAULT : selected);
    }

    public static boolean isEnabled(LauncherIcon icon) {
        Context context = ApplicationLoader.applicationContext;
        if (context == null || icon == null) return false;
        try {
            int state = context.getPackageManager().getComponentEnabledSetting(icon.getComponentName(context));
            return state == PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                    || (state == PackageManager.COMPONENT_ENABLED_STATE_DEFAULT && icon == LauncherIcon.DEFAULT);
        } catch (RuntimeException error) {
            FileLog.e(error);
            return false;
        }
    }

    /**
     * Enable the new launcher alias first, then disable the old aliases.
     * This prevents a transient state where Android has no launcher entry.
     */
    public static void setIcon(LauncherIcon icon) {
        Context context = ApplicationLoader.applicationContext;
        if (context == null || icon == null) return;

        PackageManager pm = context.getPackageManager();
        try {
            pm.setComponentEnabledSetting(
                    icon.getComponentName(context),
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                    PackageManager.DONT_KILL_APP
            );
        } catch (RuntimeException error) {
            FileLog.e(error);
            recoverDefault(pm, context);
            return;
        }

        for (LauncherIcon other : LauncherIcon.values()) {
            if (other == icon) continue;
            try {
                pm.setComponentEnabledSetting(
                        other.getComponentName(context),
                        PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                        PackageManager.DONT_KILL_APP
                );
            } catch (RuntimeException error) {
                // Never crash Settings or disable the successfully enabled target.
                FileLog.e(error);
            }
        }
    }

    private static void recoverDefault(PackageManager pm, Context context) {
        try {
            pm.setComponentEnabledSetting(
                    LauncherIcon.DEFAULT.getComponentName(context),
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                    PackageManager.DONT_KILL_APP
            );
        } catch (RuntimeException error) {
            FileLog.e(error);
        }
        for (LauncherIcon other : LauncherIcon.values()) {
            if (other == LauncherIcon.DEFAULT) continue;
            try {
                pm.setComponentEnabledSetting(
                        other.getComponentName(context),
                        PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                        PackageManager.DONT_KILL_APP
                );
            } catch (RuntimeException error) {
                FileLog.e(error);
            }
        }
    }

    public enum LauncherIcon {
        DEFAULT("DefaultIcon", R.mipmap.ic_launcher, R.string.AppIconDefault),
        ITACHI("VintageIcon", R.mipmap.icon_2_launcher, R.string.AppIconVintage),
        DISCORD("AquaIcon", R.mipmap.icon_3_launcher, R.string.AppIconAqua),
        PREMIUM("PremiumIcon", R.mipmap.icon_4_launcher, R.string.AppIconPremium),
        GOOGLE("TurboIcon", R.mipmap.icon_5_launcher, R.string.AppIconTurbo),
        IMPOSTER("NoxIcon", R.mipmap.icon_6_launcher, R.string.AppIconNox),
        EVIL("EvilIcon", R.mipmap.icon_7_launcher, R.string.AppIconEvil);

        public final String key;
        public final int preview;
        public final int title;
        private ComponentName componentName;

        LauncherIcon(String key, int preview, int title) {
            this.key = key;
            this.preview = preview;
            this.title = title;
        }

        public ComponentName getComponentName(Context context) {
            if (componentName == null) {
                componentName = new ComponentName(context.getPackageName(), "org.telegram.messenger." + key);
            }
            return componentName;
        }
    }
}
