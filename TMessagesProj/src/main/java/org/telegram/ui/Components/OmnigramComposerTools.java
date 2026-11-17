package org.telegram.ui.Components;

import android.text.SpannableStringBuilder;

import java.util.ArrayList;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Offline text helpers for the Omnigram composer tools: Markdown/HTML converter, auto fix, thread splitter,
 * list/table converter and emoji suggestions. Everything works on plain strings + runs so it can be applied
 * to the composer through the normal Telegram formatting calls (real MessageEntity types).
 */
public final class OmnigramComposerTools {

    private OmnigramComposerTools() {}

    public static final int BOLD = 1, ITALIC = 2, UNDERLINE = 3, STRIKE = 4, SPOILER = 5, CODE = 6, QUOTE = 7, LINK = 8;

    public static class Run {
        public final int start, end, type;
        public final String url;

        Run(int start, int end, int type, String url) {
            this.start = start;
            this.end = end;
            this.type = type;
            this.url = url;
        }
    }

    public static class Converted {
        public String text = "";
        public final ArrayList<Run> runs = new ArrayList<>();
    }

    // ------------------------------------------------------------------ Markdown / HTML

    public static boolean looksLikeHtml(String s) {
        return Pattern.compile("<\\s*/?\\s*[a-zA-Z][^>]*>").matcher(s).find();
    }

    public static boolean looksLikeMarkdown(String s) {
        return s.contains("**") || s.contains("__") || s.contains("~~") || s.contains("||") || s.contains("`")
                || Pattern.compile("\\[[^\\]]+\\]\\([^)]+\\)").matcher(s).find()
                || Pattern.compile("(?m)^\\s*(#{1,6} |> |[-*+] |\\d+[.)] )").matcher(s).find();
    }

    public static Converted convert(String src) {
        if (looksLikeHtml(src)) return fromHtml(src);
        return fromMarkdown(src);
    }

    // ---- Markdown
    public static Converted fromMarkdown(String src) {
        Converted c = new Converted();
        StringBuilder out = new StringBuilder();
        String[] lines = src.replace("\r\n", "\n").replace('\r', '\n').split("\n", -1);
        boolean inFence = false;
        int fenceStart = 0;
        for (int li = 0; li < lines.length; li++) {
            String line = lines[li];
            String trimmed = line.trim();
            if (trimmed.startsWith("```")) {
                if (!inFence) {
                    inFence = true;
                    fenceStart = out.length();
                } else {
                    inFence = false;
                    int end = out.length();
                    if (end > fenceStart && out.charAt(end - 1) == '\n') end--;
                    if (end > fenceStart) c.runs.add(new Run(fenceStart, end, CODE, null));
                }
                continue;
            }
            if (inFence) {
                out.append(line);
                if (li < lines.length - 1) out.append('\n');
                continue;
            }
            int lineStart = out.length();
            int quoteType = 0;
            int headingType = 0;
            String body = line;
            if (trimmed.startsWith(">")) {
                body = trimmed.replaceFirst("^>+\\s?", "");
                quoteType = QUOTE;
            } else if (trimmed.matches("^#{1,6}\\s+.*")) {
                body = trimmed.replaceFirst("^#{1,6}\\s+", "");
                headingType = BOLD;
            } else if (trimmed.matches("^[-*+]\\s+.*")) {
                body = "\u2022 " + trimmed.replaceFirst("^[-*+]\\s+", "");
            } else if (trimmed.matches("^\\d+[.)]\\s+.*")) {
                body = trimmed.replaceFirst("^(\\d+)[.)]\\s+", "$1. ");
            } else if (trimmed.matches("^(-{3,}|\\*{3,}|_{3,})$")) {
                body = "\u2014\u2014\u2014";
            }
            inline(body, out, c.runs);
            if (quoteType != 0 && out.length() > lineStart) c.runs.add(new Run(lineStart, out.length(), QUOTE, null));
            if (headingType != 0 && out.length() > lineStart) c.runs.add(new Run(lineStart, out.length(), BOLD, null));
            if (li < lines.length - 1) out.append('\n');
        }
        if (inFence) {
            int end = out.length();
            if (end > fenceStart) c.runs.add(new Run(fenceStart, end, CODE, null));
        }
        c.text = out.toString();
        return c;
    }

    private static void inline(String s, StringBuilder out, ArrayList<Run> runs) {
        int i = 0;
        int n = s.length();
        while (i < n) {
            char ch = s.charAt(i);
            if (ch == '\\' && i + 1 < n && "\\`*_{}[]()#+-.!~|>".indexOf(s.charAt(i + 1)) >= 0) {
                out.append(s.charAt(i + 1));
                i += 2;
                continue;
            }
            if (ch == '`') {
                int e = s.indexOf('`', i + 1);
                if (e > i + 1) {
                    int st = out.length();
                    out.append(s, i + 1, e);
                    runs.add(new Run(st, out.length(), CODE, null));
                    i = e + 1;
                    continue;
                }
            }
            if (s.startsWith("**", i) || s.startsWith("__", i)) {
                String m = s.substring(i, i + 2);
                int e = s.indexOf(m, i + 2);
                if (e > i + 2) {
                    int st = out.length();
                    inline(s.substring(i + 2, e), out, runs);
                    runs.add(new Run(st, out.length(), BOLD, null));
                    i = e + 2;
                    continue;
                }
            }
            if (s.startsWith("~~", i)) {
                int e = s.indexOf("~~", i + 2);
                if (e > i + 2) {
                    int st = out.length();
                    inline(s.substring(i + 2, e), out, runs);
                    runs.add(new Run(st, out.length(), STRIKE, null));
                    i = e + 2;
                    continue;
                }
            }
            if (s.startsWith("||", i)) {
                int e = s.indexOf("||", i + 2);
                if (e > i + 2) {
                    int st = out.length();
                    inline(s.substring(i + 2, e), out, runs);
                    runs.add(new Run(st, out.length(), SPOILER, null));
                    i = e + 2;
                    continue;
                }
            }
            if (ch == '[') {
                int close = s.indexOf("](", i + 1);
                if (close > i) {
                    int endUrl = s.indexOf(')', close + 2);
                    if (endUrl > close) {
                        String label = s.substring(i + 1, close);
                        String url = s.substring(close + 2, endUrl).trim();
                        int st = out.length();
                        inline(label, out, runs);
                        if (out.length() > st) runs.add(new Run(st, out.length(), LINK, url));
                        i = endUrl + 1;
                        continue;
                    }
                }
            }
            if ((ch == '*' || ch == '_') && i + 1 < n && s.charAt(i + 1) != ' ') {
                boolean wordBefore = ch == '_' && i > 0 && Character.isLetterOrDigit(s.charAt(i - 1));
                int e = s.indexOf(ch, i + 1);
                if (!wordBefore && e > i + 1 && s.charAt(e - 1) != ' ') {
                    int st = out.length();
                    inline(s.substring(i + 1, e), out, runs);
                    runs.add(new Run(st, out.length(), ITALIC, null));
                    i = e + 1;
                    continue;
                }
            }
            out.append(ch);
            i++;
        }
    }

    // ---- HTML
    private static String unescape(String s) {
        return s.replace("&nbsp;", " ").replace("&lt;", "<").replace("&gt;", ">").replace("&quot;", "\"")
                .replace("&#39;", "'").replace("&apos;", "'").replace("&amp;", "&");
    }

    private static class Open {
        final String tag;
        final int type;
        final int start;
        final String url;

        Open(String tag, int type, int start, String url) {
            this.tag = tag;
            this.type = type;
            this.start = start;
            this.url = url;
        }
    }

    public static Converted fromHtml(String src) {
        Converted c = new Converted();
        StringBuilder out = new StringBuilder();
        ArrayList<Open> stack = new ArrayList<>();
        Matcher m = Pattern.compile("<(/?)\\s*([a-zA-Z0-9-]+)([^>]*)>").matcher(src);
        int last = 0;
        while (m.find()) {
            out.append(unescape(src.substring(last, m.start())));
            last = m.end();
            boolean closing = m.group(1).length() > 0;
            String tag = m.group(2).toLowerCase(Locale.ROOT);
            String attrs = m.group(3) == null ? "" : m.group(3);
            int type = 0;
            switch (tag) {
                case "b": case "strong": type = BOLD; break;
                case "i": case "em": type = ITALIC; break;
                case "u": case "ins": type = UNDERLINE; break;
                case "s": case "strike": case "del": type = STRIKE; break;
                case "code": case "pre": type = CODE; break;
                case "a": type = LINK; break;
                case "blockquote": type = QUOTE; break;
                case "tg-spoiler": type = SPOILER; break;
                case "span":
                    if (attrs.contains("tg-spoiler")) type = SPOILER;
                    break;
                default: break;
            }
            if (!closing) {
                if (tag.equals("br")) {
                    out.append('\n');
                    continue;
                }
                if (tag.equals("li")) {
                    out.append("\u2022 ");
                    continue;
                }
                if (type != 0) {
                    String url = null;
                    if (type == LINK) {
                        Matcher hm = Pattern.compile("href\\s*=\\s*[\"']([^\"']+)[\"']").matcher(attrs);
                        if (hm.find()) url = unescape(hm.group(1));
                    }
                    stack.add(new Open(tag, type, out.length(), url));
                }
            } else {
                if (type != 0) {
                    for (int i = stack.size() - 1; i >= 0; i--) {
                        Open o = stack.get(i);
                        if (o.tag.equals(tag)) {
                            stack.remove(i);
                            if (out.length() > o.start && !(o.type == LINK && o.url == null)) {
                                c.runs.add(new Run(o.start, out.length(), o.type, o.url));
                            }
                            break;
                        }
                    }
                } else if (tag.equals("p") || tag.equals("div") || tag.equals("li") || tag.matches("h[1-6]")
                        || tag.equals("ul") || tag.equals("ol") || tag.equals("tr")) {
                    if (out.length() > 0 && out.charAt(out.length() - 1) != '\n') out.append('\n');
                }
            }
            // every other tag (script, style, img, table ...) is simply dropped
        }
        out.append(unescape(src.substring(last)));
        // unclosed tags: close at the end so nothing is lost
        for (int i = stack.size() - 1; i >= 0; i--) {
            Open o = stack.get(i);
            if (out.length() > o.start && !(o.type == LINK && o.url == null)) {
                c.runs.add(new Run(o.start, out.length(), o.type, o.url));
            }
        }
        c.text = out.toString().replaceAll("\n{3,}", "\n\n").trim();
        // trimming only removes the ends, so shift is limited to leading whitespace we cut
        return fixRunBounds(c, out.toString());
    }

    private static Converted fixRunBounds(Converted c, String original) {
        int lead = 0;
        String collapsed = original.replaceAll("\n{3,}", "\n\n");
        while (lead < collapsed.length() && Character.isWhitespace(collapsed.charAt(lead))) lead++;
        if (collapsed.length() == original.length() && lead == 0) {
            // runs are valid against c.text length
            ArrayList<Run> ok = new ArrayList<>();
            for (Run r : c.runs) {
                int e = Math.min(r.end, c.text.length());
                if (r.start < e) ok.add(new Run(r.start, e, r.type, r.url));
            }
            c.runs.clear();
            c.runs.addAll(ok);
            return c;
        }
        // offsets may have shifted because of collapsing: drop formatting rather than mis-apply it
        c.runs.clear();
        return c;
    }

    // ------------------------------------------------------------------ Auto fix

    public static String autoFix(String s) {
        String t = s.replace("\r\n", "\n").replace('\r', '\n').replace('\u00A0', ' ');
        // incorrect escapes such as \. \! \- in plain text
        t = t.replaceAll("\\\\([.!\\-()#+=>{}|~])", "$1");
        // spaces
        t = t.replaceAll("[ \\t]+\\n", "\n");
        t = t.replaceAll("(?<=\\S)[ \\t]{2,}(?=\\S)", " ");
        t = t.replaceAll("[ \\t]+([,;:!?])", "$1");
        t = t.replaceAll("([,;!?])(?=[A-Za-z\\u0400-\\u04FF])", "$1 ");
        // messy paragraph spacing
        t = t.replaceAll("\\n{3,}", "\n\n");
        t = t.trim();
        // unclosed markdown markers: close them at the end of the message
        String[] markers = {"**", "__", "~~", "||"};
        for (String mk : markers) {
            int count = 0;
            int idx = 0;
            while ((idx = t.indexOf(mk, idx)) >= 0) {
                count++;
                idx += mk.length();
            }
            if (count % 2 == 1) t = t + mk;
        }
        int ticks = 0;
        for (int i = 0; i < t.length(); i++) if (t.charAt(i) == '`') ticks++;
        if (ticks % 2 == 1) t = t + "`";
        return t;
    }

    // ------------------------------------------------------------------ Thread splitter

    public static ArrayList<CharSequence> splitThread(CharSequence text, int maxLen, boolean numbering) {
        int reserve = numbering ? 12 : 0;
        int limit = Math.max(200, maxLen - reserve);
        ArrayList<int[]> ranges = new ArrayList<>();
        int len = text.length();
        int pos = 0;
        while (pos < len) {
            while (pos < len && Character.isWhitespace(text.charAt(pos))) pos++;
            if (pos >= len) break;
            int end = Math.min(len, pos + limit);
            if (end < len) {
                int cut = findBoundary(text, pos, end, limit);
                end = cut;
            }
            int e = end;
            while (e > pos && Character.isWhitespace(text.charAt(e - 1))) e--;
            if (e > pos) ranges.add(new int[]{pos, e});
            pos = Math.max(end, pos + 1);
        }
        ArrayList<CharSequence> parts = new ArrayList<>();
        int total = ranges.size();
        for (int i = 0; i < total; i++) {
            int[] r = ranges.get(i);
            SpannableStringBuilder sb = new SpannableStringBuilder();
            if (numbering && total > 1) sb.append(String.valueOf(i + 1)).append('/').append(String.valueOf(total)).append('\n');
            sb.append(text, r[0], r[1]);
            parts.add(sb);
        }
        return parts;
    }

    private static int findBoundary(CharSequence text, int pos, int end, int limit) {
        int minCut = pos + limit / 3;
        for (int i = end; i > minCut; i--) {
            if (i < text.length() && text.charAt(i) == '\n' && i > 0 && text.charAt(i - 1) == '\n') return i;
        }
        for (int i = end; i > minCut; i--) {
            char c = text.charAt(i - 1);
            if ((c == '.' || c == '!' || c == '?' || c == '\n') && (i >= text.length() || Character.isWhitespace(text.charAt(i)))) return i;
        }
        for (int i = end; i > minCut; i--) {
            if (Character.isWhitespace(text.charAt(i - 1))) return i;
        }
        return end;
    }

    // ------------------------------------------------------------------ List / table converter

    /** Returns converted text and whether it should be shown as a monospace block (tables). */
    public static Converted convertListOrTable(String src) {
        Converted c = new Converted();
        String t = src.replace("\r\n", "\n").replace('\r', '\n').trim();
        String[] lines = t.split("\n");
        boolean tabular = false;
        int tabs = 0, pipes = 0, commas = 0;
        for (String l : lines) {
            if (l.indexOf('\t') >= 0) tabs++;
            if (l.indexOf('|') >= 0) pipes++;
            if (l.indexOf(',') >= 0) commas++;
        }
        char sep = 0;
        if (lines.length >= 2 && tabs >= lines.length - 1) sep = '\t';
        else if (lines.length >= 2 && pipes >= lines.length - 1) sep = '|';
        else if (lines.length >= 3 && commas == lines.length && !t.contains(". ")) sep = ',';
        tabular = sep != 0;
        if (tabular) {
            ArrayList<String[]> rows = new ArrayList<>();
            int cols = 0;
            for (String l : lines) {
                String row = l;
                if (sep == '|') row = row.trim().replaceAll("^\\||\\|$", "");
                if (sep == '|' && row.matches("[\\s:|-]+")) continue; // markdown separator row
                String[] cells = sep == '|' ? row.split("\\|", -1) : sep == '\t' ? row.split("\t", -1) : row.split(",", -1);
                for (int i = 0; i < cells.length; i++) cells[i] = cells[i].trim();
                rows.add(cells);
                cols = Math.max(cols, cells.length);
            }
            int[] w = new int[cols];
            for (String[] r : rows) for (int i = 0; i < r.length; i++) w[i] = Math.max(w[i], r[i].length());
            StringBuilder sb = new StringBuilder();
            for (int ri = 0; ri < rows.size(); ri++) {
                String[] r = rows.get(ri);
                for (int i = 0; i < cols; i++) {
                    String cell = i < r.length ? r[i] : "";
                    sb.append(cell);
                    for (int k = cell.length(); k < w[i]; k++) sb.append(' ');
                    if (i < cols - 1) sb.append(" | ");
                }
                while (sb.length() > 0 && sb.charAt(sb.length() - 1) == ' ') sb.setLength(sb.length() - 1);
                sb.append('\n');
                if (ri == 0 && rows.size() > 1) {
                    for (int i = 0; i < cols; i++) {
                        for (int k = 0; k < w[i]; k++) sb.append('-');
                        if (i < cols - 1) sb.append("-+-");
                    }
                    sb.append('\n');
                }
            }
            String text = sb.toString().replaceAll("\\n+$", "");
            c.text = text;
            c.runs.add(new Run(0, text.length(), CODE, null));
            return c;
        }
        // plain bullet / numbered lists
        StringBuilder sb = new StringBuilder();
        int num = 0;
        for (String l : lines) {
            String x = l.trim();
            if (x.isEmpty()) {
                sb.append('\n');
                continue;
            }
            if (x.matches("^[-*+\u2022\u25AA\u25CF\u2023]\\s*.*")) {
                sb.append("\u2022 ").append(x.replaceFirst("^[-*+\u2022\u25AA\u25CF\u2023]\\s*", "")).append('\n');
            } else if (x.matches("^\\(?\\d+[.)]\\s*.*")) {
                num++;
                sb.append(num).append(". ").append(x.replaceFirst("^\\(?\\d+[.)]\\s*", "")).append('\n');
            } else {
                sb.append(x).append('\n');
            }
        }
        c.text = sb.toString().replaceAll("\\n+$", "");
        return c;
    }

    // ------------------------------------------------------------------ Emoji suggestions

    public static final String[] DENSITY_NAMES = {"None", "Subtle", "Balanced", "Fun", "Heavy"};
    private static final int[] DENSITY_MAX = {0, 2, 4, 7, 12};

    private static final String[][] EMOJI_RULES = {
            {"\uD83C\uDF89", "congrat", "celebrat", "party", "birthday", "winner", "won ", "hooray", "yay"},
            {"\u26A0\uFE0F", "warning", "careful", "caution", "danger", "alert", "beware", "attention"},
            {"\uD83D\uDE4F", "thank", "thanks", "grateful", "appreciate", "please"},
            {"\u2764\uFE0F", "love", "adore", "heart"},
            {"\uD83D\uDE80", "launch", "released", "release", "new version", "introducing", "coming soon"},
            {"\uD83D\uDD04", "update", "updated", "upgrade", "changelog"},
            {"\u2757", "important", "urgent", "must", "reminder", "don't forget"},
            {"\u2705", "done", "success", "completed", "fixed", "approved", "ready"},
            {"\u274C", "error", "failed", "fail", "bug", "cancel", "problem", "wrong"},
            {"\uD83D\uDCB0", "money", "price", "payment", "paid", "cost", "salary", "cash"},
            {"\u23F0", "deadline", "tomorrow", "tonight", "hurry", "today at", "time"},
            {"\uD83D\uDCE2", "announcement", "announce", "news", "notice", "attention everyone"},
            {"\uD83C\uDFF7\uFE0F", "sale", "discount", "offer", "deal", "% off"},
            {"\uD83D\uDC4B", "welcome", "hello", "hi everyone", "hey", "good morning"},
            {"\uD83D\uDE04", "happy", "glad", "great", "awesome", "fun", "enjoy"},
            {"\uD83D\uDE22", "sad", "sorry", "unfortunately", "miss you"},
            {"\uD83D\uDD25", "hot", "fire", "trending", "amazing", "best"},
            {"\uD83D\uDCA1", "idea", "tip", "suggest", "hint", "trick"},
            {"\uD83C\uDFB5", "music", "song", "playlist", "listen"},
            {"\uD83D\uDCDD", "note", "notes", "write", "document", "form"},
            {"\uD83D\uDCC5", "date", "schedule", "meeting", "event", "monday", "friday"},
            {"\uD83D\uDD12", "secure", "security", "private", "password", "locked"},
            {"\uD83D\uDC4D", "agree", "good job", "nice", "well done", "okay"},
    };

    public static class EmojiSuggestion {
        public final int position; // insertion index in the original text
        public final String emoji;
        public final String reason;
        public boolean selected = true;

        EmojiSuggestion(int position, String emoji, String reason) {
            this.position = position;
            this.emoji = emoji;
            this.reason = reason;
        }
    }

    public static ArrayList<EmojiSuggestion> suggestEmojis(String text, int density) {
        ArrayList<EmojiSuggestion> out = new ArrayList<>();
        if (density <= 0 || text == null || text.trim().isEmpty()) return out;
        int max = DENSITY_MAX[Math.min(density, DENSITY_MAX.length - 1)];
        int perSentence = density >= 4 ? 2 : 1;
        String lower = text.toLowerCase(Locale.ROOT);
        // sentence ranges
        Matcher sm = Pattern.compile("[^.!?\\n]+[.!?]*").matcher(text);
        java.util.HashSet<String> used = new java.util.HashSet<>();
        while (sm.find() && out.size() < max) {
            String sentence = lower.substring(sm.start(), sm.end());
            if (sentence.trim().length() < 3) continue;
            int added = 0;
            for (String[] rule : EMOJI_RULES) {
                if (added >= perSentence || out.size() >= max) break;
                String emoji = rule[0];
                // a very low density never repeats the same emoji
                if (density <= 2 && used.contains(emoji)) continue;
                for (int k = 1; k < rule.length; k++) {
                    if (sentence.contains(rule[k])) {
                        int pos = sm.end();
                        while (pos > sm.start() && Character.isWhitespace(text.charAt(pos - 1))) pos--;
                        out.add(new EmojiSuggestion(pos, emoji, rule[k].trim()));
                        used.add(emoji);
                        added++;
                        break;
                    }
                }
            }
        }
        return out;
    }

    public static String applyEmojis(String text, ArrayList<EmojiSuggestion> suggestions) {
        ArrayList<EmojiSuggestion> chosen = new ArrayList<>();
        for (EmojiSuggestion s : suggestions) if (s.selected) chosen.add(s);
        java.util.Collections.sort(chosen, (a, b) -> Integer.compare(b.position, a.position));
        StringBuilder sb = new StringBuilder(text);
        for (EmojiSuggestion s : chosen) {
            int p = Math.min(s.position, sb.length());
            sb.insert(p, " " + s.emoji);
        }
        return sb.toString();
    }

    // ------------------------------------------------------------------ Welcome templates

    public static String fillTemplate(String template, String name, String group, String link) {
        return template.replace("{name}", name).replace("{group}", group).replace("{link}", link);
    }
}
