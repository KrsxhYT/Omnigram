package org.telegram.ui;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;

import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.R;

public final class LauncherIconController {
    private static final String ICON_PREFS = "omnigram_launcher_icon";
    private static final String SELECTED_ICON_KEY = "selected_icon";

    private LauncherIconController() {}

    /**
     * Repairs only invalid launcher states. Avoid rewriting PackageManager
     * component state on every application startup when one valid icon is
     * already enabled.
     */
    public static void tryFixLauncherIconIfNeeded() {
        Context context = ApplicationLoader.applicationContext;
        if (context == null) return;

        LauncherIcon enabledIcon = null;
        int enabledCount = 0;
        for (LauncherIcon icon : LauncherIcon.values()) {
            if (isEnabled(icon)) {
                enabledCount++;
                if (enabledIcon == null) {
                    enabledIcon = icon;
                }
            }
        }

        String savedName = context.getSharedPreferences(ICON_PREFS, Context.MODE_PRIVATE)
                .getString(SELECTED_ICON_KEY, null);
        LauncherIcon savedIcon = null;
        if (savedName != null) {
            try {
                savedIcon = LauncherIcon.valueOf(savedName);
            } catch (IllegalArgumentException ignored) {
                // An obsolete or invalid saved value is repaired below.
            }
        }

        if (enabledCount == 1 && (savedIcon == null || savedIcon == enabledIcon)) {
            if (savedIcon == null && enabledIcon != null) {
                context.getSharedPreferences(ICON_PREFS, Context.MODE_PRIVATE)
                        .edit().putString(SELECTED_ICON_KEY, enabledIcon.name()).apply();
            }
            return;
        }

        // Prefer the saved choice when recovering an interrupted icon switch.
        setIcon(savedIcon != null ? savedIcon : (enabledIcon != null ? enabledIcon : LauncherIcon.DEFAULT));
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

        // Persist the intended choice before touching PackageManager so startup can
        // recover the same icon if the process is interrupted during the switch.
        context.getSharedPreferences(ICON_PREFS, Context.MODE_PRIVATE)
                .edit().putString(SELECTED_ICON_KEY, icon.name()).apply();

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
            // Never disable another launcher entry unless the fallback is confirmed enabled.
            pm.setComponentEnabledSetting(
                    LauncherIcon.DEFAULT.getComponentName(context),
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                    PackageManager.DONT_KILL_APP
            );
        } catch (RuntimeException error) {
            FileLog.e(error);
            return;
        }

        context.getSharedPreferences(ICON_PREFS, Context.MODE_PRIVATE)
                .edit().putString(SELECTED_ICON_KEY, LauncherIcon.DEFAULT.name()).apply();

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
        DEFAULT("DefaultIcon", R.mipmap.ic_launcher, R.mipmap.ic_launcher, R.drawable.omni_icon_clear, R.string.AppIconDefault),
        ITACHI("VintageIcon", R.mipmap.icon_2_launcher, R.mipmap.icon_2_launcher, R.drawable.omni_icon_clear, R.string.AppIconVintage),
        DISCORD("AquaIcon", R.mipmap.icon_3_launcher, R.mipmap.icon_3_launcher, R.drawable.omni_icon_clear, R.string.AppIconAqua),
        GOOGLE("TurboIcon", R.mipmap.icon_5_launcher, R.mipmap.icon_5_launcher, R.drawable.omni_icon_clear, R.string.AppIconTurbo),
        IMPOSTER("NoxIcon", R.mipmap.icon_6_launcher, R.mipmap.icon_6_launcher, R.drawable.omni_icon_clear, R.string.AppIconNox),
        EVIL("EvilIcon", R.mipmap.icon_7_launcher, R.mipmap.icon_7_launcher, R.drawable.omni_icon_clear, R.string.AppIconEvil),
        PREMIUM("PremiumIcon", R.mipmap.icon_4_launcher, R.mipmap.icon_4_launcher, R.drawable.omni_icon_clear, R.string.AppIconPremium);

        public final String key;
        public final int preview;
        public final int background;
        public final int foreground;
        public final int title;
        private ComponentName componentName;

        LauncherIcon(String key, int preview, int background, int foreground, int title) {
            this.key = key;
            this.preview = preview;
            this.background = background;
            this.foreground = foreground;
            this.title = title;
        }

        /** Fixed Omnigram names, so the cloud language pack can never override them. */
        public String getDisplayName() {
            switch (this) {
                case ITACHI: return "OmniTgam";
                case DISCORD: return "Discord";
                case GOOGLE: return "Google";
                case IMPOSTER: return "Imposter";
                case EVIL: return "Evil";
                case PREMIUM: return "Premium";
                default: return "Default";
            }
        }

        public ComponentName getComponentName(Context context) {
            if (componentName == null) {
                componentName = new ComponentName(context.getPackageName(), "org.telegram.messenger." + key);
            }
            return componentName;
        }
    }
}
