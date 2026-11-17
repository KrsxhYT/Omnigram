package org.telegram.ui;

import android.content.Context;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.ui.ActionBar.Theme;

/** Omni Preference > Support: wallet addresses. Tap an address to copy it. */
public class OmnigramSupportActivity extends OmnigramSettingsBase {

    @Override
    public View createView(Context context) {
        View root = buildRoot(context, "Support");

        TextView intro = new TextView(context);
        intro.setText("Thank you for supporting Omnigram. Tap any address to copy it, then pick the one you want to send to. Please double-check the network before sending.");
        intro.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        intro.setTextColor(getThemedColor(Theme.key_windowBackgroundWhiteGrayText2));
        intro.setPadding(AndroidUtilities.dp(4), AndroidUtilities.dp(8), AndroidUtilities.dp(4), AndroidUtilities.dp(4));
        list.addView(intro);

        wallet(context, "USDT", "0xEB2231923f02923bbaA2570Dc1BF2f2E015C8575", "0xC5d7F6dCf2c0ef6edCC0860645Dbbf5064E2a1dc");
        wallet(context, "BTC", "bc1qrlvu0t7fpzsns7uw445mmu3r7ghw0z742t0nfr", "bc1p3wxxt38fyw8hrgj0635aq7dwzv0f5fj6wf2vh0uq20z0ve6jryvs4er0x3");
        wallet(context, "TRX", "TU1JqK6e88BNUeG5S8ZuQPN2YRQM1kQ6b4", "TLMbgJS8gSp8PpkJCgWUP5x9Dkp82ttPsZ");
        wallet(context, "TON / GRAM", "UQAAxDkIESq_VP2pCzmU9DLcGyUlKi_tW3IcOPLq6_CRDjvt");
        return root;
    }

    private void wallet(Context context, String coin, String... addresses) {
        section(context, coin);
        LinearLayout c = card(context);
        for (int i = 0; i < addresses.length; i++) {
            final String addr = addresses[i];
            if (i > 0) {
                TextView or = new TextView(context);
                or.setText("or");
                or.setGravity(Gravity.CENTER);
                or.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
                or.setTextColor(getThemedColor(Theme.key_windowBackgroundWhiteGrayText2));
                c.addView(or, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            }
            TextView t = new TextView(context);
            t.setText(addr);
            t.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
            t.setTypeface(android.graphics.Typeface.MONOSPACE);
            t.setTextColor(getThemedColor(Theme.key_windowBackgroundWhiteBlackText));
            t.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(14), AndroidUtilities.dp(16), AndroidUtilities.dp(14));
            t.setBackground(Theme.createSelectorDrawable(getThemedColor(Theme.key_listSelector), 2));
            t.setOnClickListener(v -> {
                AndroidUtilities.addToClipboard(addr);
                Toast.makeText(context, coin + " address copied", Toast.LENGTH_SHORT).show();
            });
            c.addView(t, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        }
    }
}
