package org.telegram.ui;

import android.content.Context;
import android.graphics.Typeface;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.Theme;

/** Omni Preference > Support: wallet addresses with coin logos. Tap an address (or Copy) to copy it. */
public class OmnigramSupportActivity extends OmnigramSettingsBase {

    @Override
    public View createView(Context context) {
        View root = buildRoot(context, "Support");

        LinearLayout hero = new LinearLayout(context);
        hero.setOrientation(LinearLayout.VERTICAL);
        hero.setGravity(Gravity.CENTER_HORIZONTAL);
        hero.setPadding(AndroidUtilities.dp(8), AndroidUtilities.dp(6), AndroidUtilities.dp(8), AndroidUtilities.dp(6));
        TextView title = new TextView(context);
        title.setText("Support Omnigram");
        title.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 22);
        title.setTypeface(AndroidUtilities.bold());
        title.setTextColor(getThemedColor(Theme.key_windowBackgroundWhiteBlackText));
        hero.addView(title);
        TextView sub = new TextView(context);
        sub.setText("Thank you for helping Omnigram grow. Tap an address to copy it, then send from your wallet. Please double-check the network before sending.");
        sub.setGravity(Gravity.CENTER);
        sub.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        sub.setTextColor(getThemedColor(Theme.key_windowBackgroundWhiteGrayText2));
        sub.setPadding(0, AndroidUtilities.dp(6), 0, AndroidUtilities.dp(4));
        hero.addView(sub);
        list.addView(hero, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        coin(context, R.drawable.omni_coin_usdt, "USDT", "Tether", "0xEB2231923f02923bbaA2570Dc1BF2f2E015C8575", "0xC5d7F6dCf2c0ef6edCC0860645Dbbf5064E2a1dc");
        coin(context, R.drawable.omni_coin_btc, "BTC", "Bitcoin", "bc1qrlvu0t7fpzsns7uw445mmu3r7ghw0z742t0nfr", "bc1p3wxxt38fyw8hrgj0635aq7dwzv0f5fj6wf2vh0uq20z0ve6jryvs4er0x3");
        coin(context, R.drawable.omni_coin_trx, "TRX", "TRON", "TU1JqK6e88BNUeG5S8ZuQPN2YRQM1kQ6b4", "TLMbgJS8gSp8PpkJCgWUP5x9Dkp82ttPsZ");
        coin(context, R.drawable.omni_coin_ton, "TON / GRAM", "Toncoin", "UQAAxDkIESq_VP2pCzmU9DLcGyUlKi_tW3IcOPLq6_CRDjvt");
        return root;
    }

    private void coin(Context context, int logo, String symbol, String name, String... addresses) {
        LinearLayout c = card(context);
        c.setPadding(0, AndroidUtilities.dp(6), 0, AndroidUtilities.dp(6));

        LinearLayout head = new LinearLayout(context);
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.setGravity(Gravity.CENTER_VERTICAL);
        head.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(10), AndroidUtilities.dp(16), AndroidUtilities.dp(6));
        ImageView icon = new ImageView(context);
        icon.setImageResource(logo);
        icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
        head.addView(icon, new LinearLayout.LayoutParams(AndroidUtilities.dp(42), AndroidUtilities.dp(42)));
        LinearLayout names = new LinearLayout(context);
        names.setOrientation(LinearLayout.VERTICAL);
        TextView sym = new TextView(context);
        sym.setText(symbol);
        sym.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 18);
        sym.setTypeface(AndroidUtilities.bold());
        sym.setTextColor(getThemedColor(Theme.key_windowBackgroundWhiteBlackText));
        names.addView(sym);
        TextView nm = new TextView(context);
        nm.setText(name);
        nm.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
        nm.setTextColor(getThemedColor(Theme.key_windowBackgroundWhiteGrayText2));
        names.addView(nm);
        LinearLayout.LayoutParams np = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        np.leftMargin = AndroidUtilities.dp(14);
        head.addView(names, np);
        c.addView(head, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        for (int i = 0; i < addresses.length; i++) {
            final String addr = addresses[i];
            final String label = addresses.length > 1 ? "Address " + (i + 1) : "Address";
            LinearLayout block = new LinearLayout(context);
            block.setOrientation(LinearLayout.HORIZONTAL);
            block.setGravity(Gravity.CENTER_VERTICAL);
            block.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(12), getThemedColor(Theme.key_windowBackgroundGray)));
            block.setPadding(AndroidUtilities.dp(14), AndroidUtilities.dp(10), AndroidUtilities.dp(10), AndroidUtilities.dp(10));

            LinearLayout texts = new LinearLayout(context);
            texts.setOrientation(LinearLayout.VERTICAL);
            TextView lab = new TextView(context);
            lab.setText(label);
            lab.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 11);
            lab.setTextColor(getThemedColor(Theme.key_windowBackgroundWhiteGrayText2));
            texts.addView(lab);
            TextView t = new TextView(context);
            t.setText(wrapAddress(addr));
            t.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
            t.setTypeface(Typeface.MONOSPACE);
            t.setTextColor(getThemedColor(Theme.key_windowBackgroundWhiteBlackText));
            t.setPadding(0, AndroidUtilities.dp(2), 0, 0);
            texts.addView(t);
            block.addView(texts, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

            TextView copy = new TextView(context);
            copy.setText("Copy");
            copy.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
            copy.setTypeface(AndroidUtilities.bold());
            copy.setTextColor(0xFFFFFFFF);
            copy.setPadding(AndroidUtilities.dp(14), AndroidUtilities.dp(8), AndroidUtilities.dp(14), AndroidUtilities.dp(8));
            copy.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(12), getThemedColor(Theme.key_featuredStickers_addButton)));
            block.addView(copy, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

            View.OnClickListener l = v -> {
                AndroidUtilities.addToClipboard(addr);
                copy.setText("Copied");
                v.postDelayed(() -> copy.setText("Copy"), 1400);
                Toast.makeText(context, symbol + " address copied", Toast.LENGTH_SHORT).show();
            };
            block.setOnClickListener(l);
            copy.setOnClickListener(l);
            LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            bp.setMargins(AndroidUtilities.dp(12), AndroidUtilities.dp(4), AndroidUtilities.dp(12), AndroidUtilities.dp(6));
            c.addView(block, bp);
        }
    }

    /** Break a long address into two lines so it reads easily without hiding any character. */
    private static String wrapAddress(String a) {
        if (a.length() <= 30) return a;
        int mid = a.length() / 2;
        return a.substring(0, mid) + "\n" + a.substring(mid);
    }
}
