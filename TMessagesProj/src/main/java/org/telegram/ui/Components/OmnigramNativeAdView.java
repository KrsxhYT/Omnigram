package org.telegram.ui.Components;

import android.content.Context;
import android.graphics.Outline;
import android.graphics.Typeface;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.gms.ads.nativead.NativeAd;
import com.google.android.gms.ads.nativead.NativeAdView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.ui.ActionBar.Theme;

/** A chat-list row for an AdMob native ad. It is clearly labeled and never looks like a real conversation. */
public class OmnigramNativeAdView extends LinearLayout {

    private final NativeAdView adView;

    private final ImageView icon;
    private final TextView headline;
    private final TextView body;
    private final TextView cta;
    private final TextView badge;

    public OmnigramNativeAdView(Context context) {
        super(context);
        setOrientation(VERTICAL);
        setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));

        // NativeAdView is final in the Google Mobile Ads SDK; host it instead of subclassing it.
        adView = new NativeAdView(context);
        adView.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));

        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(AndroidUtilities.dp(14), AndroidUtilities.dp(10), AndroidUtilities.dp(14), AndroidUtilities.dp(10));

        icon = new ImageView(context);
        icon.setScaleType(ImageView.ScaleType.CENTER_CROP);
        icon.setClipToOutline(true);
        icon.setOutlineProvider(new ViewOutlineProvider() {
            @Override
            public void getOutline(View view, Outline outline) {
                outline.setOval(0, 0, view.getWidth(), view.getHeight());
            }
        });
        icon.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray));
        row.addView(icon, new LinearLayout.LayoutParams(AndroidUtilities.dp(54), AndroidUtilities.dp(54)));

        LinearLayout texts = new LinearLayout(context);
        texts.setOrientation(LinearLayout.VERTICAL);

        LinearLayout top = new LinearLayout(context);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);
        badge = new TextView(context);
        badge.setText("Ad");
        badge.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 11);
        badge.setTypeface(Typeface.DEFAULT_BOLD);
        badge.setTextColor(0xFFFFFFFF);
        badge.setPadding(AndroidUtilities.dp(6), AndroidUtilities.dp(1), AndroidUtilities.dp(6), AndroidUtilities.dp(1));
        badge.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(6), Theme.getColor(Theme.key_featuredStickers_addButton)));
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        bp.rightMargin = AndroidUtilities.dp(8);
        top.addView(badge, bp);
        headline = new TextView(context);
        headline.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        headline.setTypeface(Typeface.DEFAULT_BOLD);
        headline.setSingleLine(true);
        headline.setEllipsize(TextUtils.TruncateAt.END);
        headline.setTextColor(Theme.getColor(Theme.key_chats_name));
        top.addView(headline, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        texts.addView(top);

        body = new TextView(context);
        body.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        body.setMaxLines(2);
        body.setEllipsize(TextUtils.TruncateAt.END);
        body.setTextColor(Theme.getColor(Theme.key_chats_message));
        body.setPadding(0, AndroidUtilities.dp(3), 0, 0);
        texts.addView(body);

        cta = new TextView(context);
        cta.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
        cta.setTypeface(Typeface.DEFAULT_BOLD);
        cta.setTextColor(Theme.getColor(Theme.key_featuredStickers_addButton));
        cta.setPadding(0, AndroidUtilities.dp(5), 0, 0);
        texts.addView(cta);

        LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        tp.leftMargin = AndroidUtilities.dp(12);
        tp.rightMargin = AndroidUtilities.dp(18); // room for the AdChoices icon
        row.addView(texts, tp);

        adView.addView(row, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        addView(adView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        adView.setIconView(icon);
        adView.setHeadlineView(headline);
        adView.setBodyView(body);
        adView.setCallToActionView(cta);
    }

    public void bind(NativeAd ad) {
        if (ad == null) return;
        headline.setText(ad.getHeadline());
        if (ad.getBody() != null) {
            body.setText(ad.getBody());
        } else if (ad.getAdvertiser() != null) {
            body.setText(ad.getAdvertiser());
        } else {
            body.setText("");
        }
        if (ad.getCallToAction() != null) {
            cta.setText(ad.getCallToAction());
            cta.setVisibility(View.VISIBLE);
        } else {
            cta.setVisibility(View.GONE);
        }
        if (ad.getIcon() != null) {
            icon.setImageDrawable(ad.getIcon().getDrawable());
        } else {
            icon.setImageDrawable(null);
        }
        adView.setNativeAd(ad);
    }
}
