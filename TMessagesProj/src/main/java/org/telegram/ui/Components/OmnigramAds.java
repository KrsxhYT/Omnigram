package org.telegram.ui.Components;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdLoader;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.android.gms.ads.nativead.NativeAd;
import com.google.android.gms.ads.nativead.NativeAdOptions;
import com.google.android.ump.ConsentInformation;
import com.google.android.ump.ConsentRequestParameters;
import com.google.android.ump.UserMessagingPlatform;

import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.Utilities;
import org.telegram.messenger.voip.VoIPService;

/**
 * All AdMob logic lives here: consent (Google UMP), SDK start, the native ad for the chat list and the interstitial
 * shown at natural transitions. The UI only asks this class for the current ad or to try showing the interstitial.
 */
public final class OmnigramAds {
    private OmnigramAds() {}

    public static final String APP_ID = "ca-app-pub-6519952735559123~4793901256";
    public static final String NATIVE_UNIT = "ca-app-pub-6519952735559123/5835520188";
    public static final String INTERSTITIAL_UNIT = "ca-app-pub-6519952735559123/3700311433";

    private static final long INTERSTITIAL_COOLDOWN_MS = 4L * 60 * 1000;
    private static final int INTERSTITIAL_EVERY_N_TRANSITIONS = 3;
    private static final long NATIVE_REFRESH_MS = 30L * 60 * 1000;

    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private static boolean consentStarted;
    private static boolean sdkReady;
    private static NativeAd nativeAd;
    private static boolean loadingNative;
    private static long nativeLoadedAt;
    private static InterstitialAd interstitial;
    private static boolean loadingInterstitial;
    private static boolean showingInterstitial;
    private static long lastInterstitialShown;
    private static int chatClosedCount;
    private static boolean chatJustClosed;

    /** The user's own "No Ads" switch turns every ad off. */
    public static boolean enabled() {
        return !OmnigramPrefs.noAds();
    }

    // ------------------------------------------------------------------ start (consent first)

    public static void init(final Activity activity) {
        if (activity == null || consentStarted || !enabled()) return;
        consentStarted = true;
        try {
            final ConsentInformation info = UserMessagingPlatform.getConsentInformation(activity);
            ConsentRequestParameters params = new ConsentRequestParameters.Builder().build();
            info.requestConsentInfoUpdate(activity, params,
                    () -> UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity, formError -> {
                        if (info.canRequestAds()) startSdk(activity.getApplicationContext());
                    }),
                    requestError -> {
                        if (info.canRequestAds()) startSdk(activity.getApplicationContext());
                    });
            // consent from an earlier session may already allow ads
            if (info.canRequestAds()) startSdk(activity.getApplicationContext());
        } catch (Throwable t) {
            consentStarted = false;
        }
    }

    private static void startSdk(final Context app) {
        if (sdkReady) return;
        sdkReady = true;
        Utilities.globalQueue.postRunnable(() -> {
            try {
                MobileAds.initialize(app, status -> MAIN.post(() -> {
                    loadNative(app);
                    loadInterstitial(app);
                }));
            } catch (Throwable t) {
                sdkReady = false;
            }
        });
    }

    // ------------------------------------------------------------------ native ad for the chat list

    public static NativeAd getNativeAd() {
        return enabled() ? nativeAd : null;
    }

    public static boolean canShowNative() {
        return enabled() && nativeAd != null;
    }

    public static void loadNative(final Context context) {
        if (!enabled() || !sdkReady || loadingNative) return;
        if (nativeAd != null && System.currentTimeMillis() - nativeLoadedAt < NATIVE_REFRESH_MS) return;
        loadingNative = true;
        try {
            AdLoader loader = new AdLoader.Builder(context, NATIVE_UNIT)
                    .forNativeAd(ad -> {
                        NativeAd old = nativeAd;
                        nativeAd = ad;
                        nativeLoadedAt = System.currentTimeMillis();
                        loadingNative = false;
                        if (old != null) old.destroy();
                        NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.dialogsNeedReload);
                    })
                    .withAdListener(new AdListener() {
                        @Override
                        public void onAdFailedToLoad(LoadAdError error) {
                            loadingNative = false;
                        }
                    })
                    .withNativeAdOptions(new NativeAdOptions.Builder().build())
                    .build();
            loader.loadAd(new AdRequest.Builder().build());
        } catch (Throwable t) {
            loadingNative = false;
        }
    }

    /** Called when the chat list becomes visible again: refreshes an old ad. */
    public static void refreshNativeIfNeeded(Context context) {
        try { loadNative(context.getApplicationContext()); } catch (Throwable ignore) {}
    }

    // ------------------------------------------------------------------ interstitial

    public static void loadInterstitial(final Context context) {
        if (!enabled() || !sdkReady || loadingInterstitial || interstitial != null) return;
        loadingInterstitial = true;
        try {
            InterstitialAd.load(context, INTERSTITIAL_UNIT, new AdRequest.Builder().build(), new InterstitialAdLoadCallback() {
                @Override
                public void onAdLoaded(InterstitialAd ad) {
                    interstitial = ad;
                    loadingInterstitial = false;
                }

                @Override
                public void onAdFailedToLoad(LoadAdError error) {
                    interstitial = null;
                    loadingInterstitial = false;
                }
            });
        } catch (Throwable t) {
            loadingInterstitial = false;
        }
    }

    /** Call when a chat screen closes: one natural transition. */
    public static void onChatClosed() {
        chatClosedCount++;
        chatJustClosed = true;
    }

    /**
     * Shows the interstitial only when the user just came back from a conversation, enough time and enough
     * transitions have passed, no call is running and an ad is really loaded.
     */
    public static void maybeShowInterstitial(final Activity activity) {
        try {
            if (!chatJustClosed) return;
            chatJustClosed = false;
            if (activity == null || activity.isFinishing() || !enabled() || !sdkReady || showingInterstitial) return;
            if (interstitial == null) {
                loadInterstitial(activity.getApplicationContext());
                return;
            }
            if (chatClosedCount < INTERSTITIAL_EVERY_N_TRANSITIONS) return;
            if (System.currentTimeMillis() - lastInterstitialShown < INTERSTITIAL_COOLDOWN_MS) return;
            if (VoIPService.getSharedInstance() != null) return; // never during a call
            final InterstitialAd ad = interstitial;
            interstitial = null;
            ad.setFullScreenContentCallback(new FullScreenContentCallback() {
                @Override
                public void onAdDismissedFullScreenContent() {
                    showingInterstitial = false;
                    loadInterstitial(activity.getApplicationContext());
                }

                @Override
                public void onAdFailedToShowFullScreenContent(AdError adError) {
                    showingInterstitial = false;
                    loadInterstitial(activity.getApplicationContext());
                }

                @Override
                public void onAdShowedFullScreenContent() {
                    showingInterstitial = true;
                    lastInterstitialShown = System.currentTimeMillis();
                    chatClosedCount = 0;
                }
            });
            ad.show(activity);
        } catch (Throwable t) {
            showingInterstitial = false;
        }
    }

    // ------------------------------------------------------------------ cleanup

    public static void release() {
        try {
            if (nativeAd != null) {
                nativeAd.destroy();
                nativeAd = null;
            }
            interstitial = null;
        } catch (Throwable ignore) {}
    }
}
