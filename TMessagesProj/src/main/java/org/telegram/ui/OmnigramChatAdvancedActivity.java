package org.telegram.ui;

import android.content.Context;
import android.view.View;
import android.widget.LinearLayout;

import org.telegram.ui.Components.OmnigramComposerPrefs;

/** Omni Preference > Chat > Advanced Settings: every composer tool can be switched on or off here. */
public class OmnigramChatAdvancedActivity extends OmnigramSettingsBase {

    @Override
    public View createView(Context context) {
        View root = buildRoot(context, "Advanced Settings");

        section(context, "Composer button");
        LinearLayout a = card(context);
        toggle(context, a, "Context menu actions", "Shows a Tools button above the message box while you type. All tools below open from it",
                OmnigramComposerPrefs.CONTEXT_MENU, true);
        toggle(context, a, "Character counter", "Live counter with Telegram's message and caption limits",
                OmnigramComposerPrefs.COUNTER, true);

        section(context, "Formatting");
        LinearLayout b = card(context);
        toggle(context, b, "Formatting toolbar", "Bold, italic, underline, strike, spoiler, code, code block, quote, link and mention for selected text",
                OmnigramComposerPrefs.FORMAT_TOOLBAR, true);
        toggle(context, b, "Markdown / HTML converter", "Turns pasted Markdown or HTML into Telegram formatting. The original stays recoverable",
                OmnigramComposerPrefs.CONVERTER, true);
        toggle(context, b, "Live preview", "Preview card above the message box. Nothing is sent until you press Send",
                OmnigramComposerPrefs.LIVE_PREVIEW, true);
        toggle(context, b, "Auto fix formatting", "Repairs broken markers, spaces and paragraph spacing. The original draft is kept",
                OmnigramComposerPrefs.AUTO_FIX, true);
        toggle(context, b, "Undo / redo formatting", "Local history of composer changes, separate from Telegram",
                OmnigramComposerPrefs.UNDO_REDO, true);

        section(context, "Long text");
        LinearLayout c = card(context);
        toggle(context, c, "Thread splitter", "Splits a long text into numbered messages (1/5, 2/5 ...) you can edit before sending",
                OmnigramComposerPrefs.THREAD_SPLITTER, true);
        toggle(context, c, "List / table converter", "Cleans bullet and numbered lists, turns tables into aligned monospace text",
                OmnigramComposerPrefs.LIST_TABLE, true);

        section(context, "Writing help");
        LinearLayout d = card(context);
        toggle(context, d, "Grammar + clarity fix", "Opens Telegram's own AI editor (server side, Telegram decides who can use it)",
                OmnigramComposerPrefs.GRAMMAR_AI, true);
        toggle(context, d, "Translate + localize", "Opens Telegram's AI editor translate tab. Links and @usernames stay as they are",
                OmnigramComposerPrefs.TRANSLATE_AI, true);
        toggle(context, d, "Context emoji suggestions", "Suggests fitting emojis from the words you wrote, you choose which to add",
                OmnigramComposerPrefs.EMOJI_SUGGEST, true);
        toggle(context, d, "Emoji density slider", "None, Subtle, Balanced, Fun or Heavy",
                OmnigramComposerPrefs.DENSITY_SLIDER, true);

        section(context, "Group admins");
        LinearLayout e = card(context);
        toggle(context, e, "Welcome / auto-reply formatter", "Reusable templates with {name}, {group}, {link}. Automatic sending needs a Telegram bot in your group",
                OmnigramComposerPrefs.WELCOME, true);
        return root;
    }
}
