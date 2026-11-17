package org.telegram.ui.Components;

import android.content.Context;
import android.speech.tts.TextToSpeech;
import android.widget.Toast;

import java.util.Locale;

/** Reads a message out loud with the phone's text-to-speech engine. Tap Speak again to stop. */
public final class OmnigramSpeaker {
    private OmnigramSpeaker() {}

    private static TextToSpeech tts;
    private static boolean ready;
    private static String pending;

    public static void speak(Context context, CharSequence text) {
        if (context == null || text == null || text.toString().trim().isEmpty()) {
            return;
        }
        final Context app = context.getApplicationContext();
        final String s = text.toString();
        try {
            if (tts != null && ready && tts.isSpeaking()) {
                tts.stop();
                return;
            }
            if (tts == null) {
                pending = s;
                tts = new TextToSpeech(app, status -> {
                    ready = status == TextToSpeech.SUCCESS;
                    if (!ready) {
                        Toast.makeText(app, "Text to speech is not available on this phone", Toast.LENGTH_SHORT).show();
                        tts = null;
                        return;
                    }
                    pickLanguage();
                    if (pending != null) {
                        say(pending);
                        pending = null;
                    }
                });
            } else if (ready) {
                say(s);
            }
        } catch (Throwable t) {
            Toast.makeText(app, "Could not start text to speech", Toast.LENGTH_SHORT).show();
        }
    }

    private static void pickLanguage() {
        try {
            int r = tts.setLanguage(Locale.getDefault());
            if (r == TextToSpeech.LANG_MISSING_DATA || r == TextToSpeech.LANG_NOT_SUPPORTED) {
                tts.setLanguage(Locale.US);
            }
        } catch (Throwable ignore) {}
    }

    private static void say(String s) {
        try {
            String text = s.length() > 3900 ? s.substring(0, 3900) : s;
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "omnigram_speak");
        } catch (Throwable ignore) {}
    }
}
