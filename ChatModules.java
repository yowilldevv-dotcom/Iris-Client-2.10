package dev.iris.client.modules;

import dev.iris.client.core.Iris;
import dev.iris.client.module.Category;
import dev.iris.client.module.Module;
import dev.iris.client.module.ModuleManager;
import dev.iris.client.setting.BoolSetting;
import dev.iris.client.setting.NumberSetting;
import dev.iris.client.setting.TextSetting;
import dev.iris.client.util.Format;
import dev.iris.client.util.Mc;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Chat-category modules: filtering, notifications, automation and logging built on the chat hooks of {@link Module}. */
public final class ChatModules {
    private ChatModules() {}

    public static void register(ModuleManager mm) {
        mm.register(
                new ChatFilter(),
                new SpamCollapse(),
                new MentionNotifier(),
                new KeywordNotifier(),
                new AutoReply(),
                new ChatSuffix(),
                new AutoCapitalize(),
                new JoinLeaveNotifier(),
                new DeathMessageNotifier(),
                new AdvancementNotifier(),
                new ChatLogger(),
                new SlowModeGuard()
        );
    }

    // ---- shared helpers ----

    /** Splits a comma separated list into trimmed, lower-case, non-empty entries. */
    static String[] splitWords(String csv) {
        if (csv == null || csv.isBlank()) return new String[0];
        String[] parts = csv.split(",");
        int n = 0;
        for (String p : parts) {
            String t = p.trim().toLowerCase(Locale.ROOT);
            if (!t.isEmpty()) parts[n++] = t;
        }
        return java.util.Arrays.copyOf(parts, n);
    }

    /** True when {@code lower} (already lower-case) contains {@code word}; optionally only as a whole word. */
    static boolean containsWord(String lower, String word, boolean wholeWord) {
        int from = 0;
        while (true) {
            int i = lower.indexOf(word, from);
            if (i < 0) return false;
            if (!wholeWord) return true;
            boolean leftOk = i == 0 || !isWordChar(lower.charAt(i - 1));
            int end = i + word.length();
            boolean rightOk = end >= lower.length() || !isWordChar(lower.charAt(end));
            if (leftOk && rightOk) return true;
            from = i + 1;
        }
    }

    private static boolean isWordChar(char c) { return Character.isLetterOrDigit(c) || c == '_'; }

    /** Heuristic: vanilla chat renders own lines as "<name> text"; many servers use "name: text" or "[rank] name:". */
    static boolean isOwnMessage(String plain, String name) {
        if (name == null || name.isEmpty()) return false;
        if (plain.startsWith("<" + name + ">")) return true;
        if (plain.startsWith(name + ":") || plain.startsWith(name + " »") || plain.startsWith(name + " >")) return true;
        int colon = plain.indexOf(':');
        return colon > 0 && colon < 40 && plain.substring(0, colon).endsWith(name);
    }

    static String ownName() {
        try {
            return Mc.mc().getUser().getName();
        } catch (RuntimeException e) {
            return "";
        }
    }

    // ---- modules ----

    /** Hides chat messages containing any of the user's words. */
    static final class ChatFilter extends Module {
        private final TextSetting words = add(new TextSetting("Words", "Comma separated words or phrases to hide.", "", 200));
        private final BoolSetting whole = add(new BoolSetting("Whole Words", "Only match complete words.", false));
        private String[] list = new String[0];
        private int hidden;

        ChatFilter() {
            super("Chat Filter", "Hides chat messages that contain any of your chosen words.", Category.CHAT);
            words.onChange(v -> list = splitWords(v));
            list = splitWords(words.get());
        }

        @Override protected boolean requiresWorld() { return false; }
        @Override protected void onEnable() { hidden = 0; }
        @Override public String info() { return hidden == 0 ? "" : hidden + " hidden"; }

        @Override public boolean allowIncomingChat(String plain) {
            if (list.length == 0) return true;
            String lower = plain.toLowerCase(Locale.ROOT);
            boolean w = whole.on();
            for (String s : list) {
                if (containsWord(lower, s, w)) {
                    hidden++;
                    return false;
                }
            }
            return true;
        }
    }

    /** Suppresses identical chat lines repeated within a time window. */
    static final class SpamCollapse extends Module {
        private final NumberSetting window = add(new NumberSetting("Window", "Seconds during which a repeated line is hidden.", 10, 1, 120, 1));
        private final Map<String, Long> seen = new HashMap<>();
        private int suppressed;

        SpamCollapse() { super("Spam Collapse", "Hides identical chat lines that repeat within a short time.", Category.CHAT); }

        @Override protected boolean requiresWorld() { return false; }
        @Override protected void onEnable() { suppressed = 0; seen.clear(); }
        @Override protected void onDisable() { seen.clear(); }
        @Override public void onWorldLeave() { seen.clear(); }
        @Override public String info() { return suppressed == 0 ? "" : suppressed + " hidden"; }

        @Override public boolean allowIncomingChat(String plain) {
            long now = System.currentTimeMillis();
            long windowMs = window.intValue() * 1000L;
            if (seen.size() > 256) {
                for (Iterator<Map.Entry<String, Long>> it = seen.entrySet().iterator(); it.hasNext(); ) {
                    if (now - it.next().getValue() > windowMs) it.remove();
                }
                if (seen.size() > 512) seen.clear();
            }
            Long prev = seen.put(plain, now);
            if (prev != null && now - prev <= windowMs) {
                suppressed++;
                return false;
            }
            return true;
        }
    }

    /** Toast when your own name appears in someone else's message. */
    static final class MentionNotifier extends Module {
        private final NumberSetting cooldown = add(new NumberSetting("Cooldown", "Minimum seconds between notifications.", 3, 0, 60, 1));
        private long last;

        MentionNotifier() { super("Mention Notifier", "Shows a notification when someone mentions your name in chat.", Category.CHAT); }

        @Override protected boolean requiresWorld() { return false; }

        @Override public boolean allowIncomingChat(String plain) {
            String name = ownName();
            if (name.isEmpty() || isOwnMessage(plain, name)) return true;
            String lower = plain.toLowerCase(Locale.ROOT);
            if (lower.endsWith(" joined the game") || lower.endsWith(" left the game")) return true;
            if (!containsWord(lower, name.toLowerCase(Locale.ROOT), true)) return true;
            long now = System.currentTimeMillis();
            if (now - last < cooldown.intValue() * 1000L) return true;
            last = now;
            notifications().info("Mention", Format.ellipsize(plain, 80));
            return true;
        }
    }

    /** Toast when a chat line contains one of the user's keywords. */
    static final class KeywordNotifier extends Module {
        private final TextSetting words = add(new TextSetting("Keywords", "Comma separated words that trigger a notification.", "", 200));
        private final BoolSetting whole = add(new BoolSetting("Whole Words", "Only match complete words.", true));
        private final BoolSetting skipOwn = add(new BoolSetting("Skip Own", "Ignore your own messages.", true));
        private final NumberSetting cooldown = add(new NumberSetting("Cooldown", "Minimum seconds between notifications.", 3, 0, 60, 1));
        private String[] list = new String[0];
        private long last;

        KeywordNotifier() {
            super("Keyword Notifier", "Shows a notification when chat contains one of your keywords.", Category.CHAT);
            words.onChange(v -> list = splitWords(v));
            list = splitWords(words.get());
        }

        @Override protected boolean requiresWorld() { return false; }

        @Override public boolean allowIncomingChat(String plain) {
            if (list.length == 0) return true;
            if (skipOwn.on() && isOwnMessage(plain, ownName())) return true;
            String lower = plain.toLowerCase(Locale.ROOT);
            boolean w = whole.on();
            for (String s : list) {
                if (!containsWord(lower, s, w)) continue;
                long now = System.currentTimeMillis();
                if (now - last >= cooldown.intValue() * 1000L) {
                    last = now;
                    notifications().info("Keyword: " + s, Format.ellipsize(plain, 80));
                }
                break;
            }
            return true;
        }
    }

    /** Replies with a fixed message when chat contains a trigger text, rate limited. */
    static final class AutoReply extends Module {
        private final TextSetting trigger = add(new TextSetting("Trigger", "Text that triggers the reply (case-insensitive).", "", 60));
        private final TextSetting reply = add(new TextSetting("Reply", "Message to send. Must not start with '/'.", "", 100));
        private final NumberSetting cooldown = add(new NumberSetting("Cooldown", "Minimum seconds between replies.", 30, 5, 600, 1));
        private long last;
        private int sent;

        AutoReply() { super("Auto Reply", "Sends your chosen reply when someone's message contains your trigger text.", Category.CHAT); }

        @Override protected void onEnable() { last = 0; sent = 0; }
        @Override public String info() { return sent == 0 ? "" : sent + " sent"; }

        @Override public boolean allowIncomingChat(String plain) {
            String t = trigger.get().trim();
            String r = reply.get().trim();
            if (t.isEmpty() || r.isEmpty() || r.startsWith("/") || !Mc.inWorld()) return true;
            if (isOwnMessage(plain, ownName()) || plain.contains(r)) return true;
            if (!plain.toLowerCase(Locale.ROOT).contains(t.toLowerCase(Locale.ROOT))) return true;
            long now = System.currentTimeMillis();
            if (last != 0 && now - last < cooldown.intValue() * 1000L) return true;
            last = now;
            sent++;
            mc.execute(() -> Mc.sendChat(r));
            return true;
        }
    }

    /** Appends a suffix to normal chat messages. */
    static final class ChatSuffix extends Module {
        private final TextSetting suffix = add(new TextSetting("Suffix", "Text appended to every chat message.", " | Iris", 32));

        ChatSuffix() { super("Chat Suffix", "Appends a suffix to your chat messages (commands are left alone).", Category.CHAT); }

        @Override protected boolean requiresWorld() { return false; }

        @Override public String onOutgoingChat(String message) {
            String s = suffix.get();
            if (s.isEmpty() || message.isEmpty() || message.startsWith("/") || message.endsWith(s)) return message;
            if (message.length() + s.length() > 256) return message;
            return message + s;
        }
    }

    /** Capitalizes the first letter of messages and a lone "i". */
    static final class AutoCapitalize extends Module {
        private final BoolSetting fixI = add(new BoolSetting("Fix I", "Also turn a lone \"i\" into \"I\".", true));

        AutoCapitalize() { super("Auto Capitalize", "Capitalizes the start of your chat messages.", Category.CHAT); }

        @Override protected boolean requiresWorld() { return false; }

        @Override public String onOutgoingChat(String message) {
            if (message.isEmpty() || message.startsWith("/")) return message;
            String out = message;
            if (fixI.on()) out = out.replaceAll("(?<![\\p{L}\\p{N}'])i(?![\\p{L}\\p{N}])", "I");
            if (Character.isLowerCase(out.charAt(0))) out = Character.toUpperCase(out.charAt(0)) + out.substring(1);
            return out;
        }
    }

    /** Notifications for players joining or leaving. */
    static final class JoinLeaveNotifier extends Module {
        private static final Pattern PATTERN = Pattern.compile("^(\\S{1,32}) (joined|left) the game$");
        private final BoolSetting joins = add(new BoolSetting("Joins", "Notify when a player joins.", true));
        private final BoolSetting leaves = add(new BoolSetting("Leaves", "Notify when a player leaves.", true));

        JoinLeaveNotifier() { super("Join Leave Notifier", "Shows a notification when players join or leave the server.", Category.CHAT); }

        @Override protected boolean requiresWorld() { return false; }

        @Override public boolean allowIncomingChat(String plain) {
            if (!plain.endsWith(" the game")) return true;
            Matcher m = PATTERN.matcher(plain.trim());
            if (!m.matches()) return true;
            boolean join = m.group(2).equals("joined");
            if (join ? joins.on() : leaves.on()) {
                if (join) notifications().success("Player joined", m.group(1));
                else notifications().info("Player left", m.group(1));
            }
            return true;
        }
    }

    /** Notifications for death messages, with the death position for your own deaths. */
    static final class DeathMessageNotifier extends Module {
        private static final String[] FRAGMENTS = {
                " was slain by ", " was shot by ", " was killed", " was blown up by", " was fireballed by",
                " was pricked to death", " was squashed", " was impaled", " was pummeled by", " was skewered",
                " fell from ", " fell off ", " fell out of the world", " hit the ground too hard", " drowned",
                " burned to death", " went up in flames", " tried to swim in lava", " blew up", " starved to death",
                " suffocated in a wall", " withered away", " experienced kinetic energy", " died", " froze to death",
                " was struck by lightning", " discovered the floor was lava", " walked into "
        };
        private final BoolSetting selfOnly = add(new BoolSetting("Self Only", "Only notify about your own deaths.", false));

        DeathMessageNotifier() { super("Death Message Notifier", "Shows a notification for death messages and where you died.", Category.CHAT); }

        @Override protected boolean requiresWorld() { return false; }

        @Override public boolean allowIncomingChat(String plain) {
            int sp = plain.indexOf(' ');
            if (sp < 3 || sp > 17) return true;
            String who = plain.substring(0, sp);
            for (int i = 0; i < who.length(); i++) {
                char c = who.charAt(i);
                if (!(Character.isLetterOrDigit(c) || c == '_')) return true; // chat lines like "<Bob> ..." are not death messages
            }
            boolean match = false;
            for (String f : FRAGMENTS) {
                if (plain.indexOf(f, sp - 1) >= 0) { match = true; break; }
            }
            if (!match) return true;
            boolean self = who.equals(ownName());
            if (selfOnly.on() && !self) return true;
            if (self && Mc.inWorld()) {
                var p = Mc.player();
                notifications().warn("You died", "At " + (int) Math.floor(p.getX()) + ", " + (int) Math.floor(p.getY())
                        + ", " + (int) Math.floor(p.getZ()));
            } else {
                notifications().info("Death", Format.ellipsize(plain, 80));
            }
            return true;
        }
    }

    /** Notifications for advancements, challenges and goals. */
    static final class AdvancementNotifier extends Module {
        private static final Pattern PATTERN =
                Pattern.compile("^(\\S{1,32}) has (made the advancement|completed the challenge|reached the goal) \\[(.+)]$");
        private final BoolSetting selfOnly = add(new BoolSetting("Self Only", "Only notify about your own advancements.", false));

        AdvancementNotifier() { super("Advancement Notifier", "Shows a notification when someone earns an advancement.", Category.CHAT); }

        @Override protected boolean requiresWorld() { return false; }

        @Override public boolean allowIncomingChat(String plain) {
            if (plain.indexOf(" has ") < 0) return true;
            Matcher m = PATTERN.matcher(plain.trim());
            if (!m.matches()) return true;
            if (selfOnly.on() && !m.group(1).equals(ownName())) return true;
            String kind = m.group(2).startsWith("completed") ? "Challenge" : m.group(2).startsWith("reached") ? "Goal" : "Advancement";
            notifications().success(kind, m.group(1) + ": " + m.group(3));
            return true;
        }
    }

    /** Appends every chat line to config/irisclient/logs/chat-YYYY-MM-DD.log, buffered. */
    static final class ChatLogger extends Module {
        private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss");
        private static final int MAX_BUFFER_CHARS = 64 * 1024;
        private final NumberSetting flushSeconds = add(new NumberSetting("Flush Interval", "Seconds between writes to disk.", 30, 5, 300, 5));
        private final StringBuilder buffer = new StringBuilder();
        private LocalDate bufferDate;
        private int ticks;
        private boolean warned;
        private int lines;

        ChatLogger() { super("Chat Logger", "Saves chat to a daily log file in the Iris config folder.", Category.CHAT); }

        @Override protected boolean requiresWorld() { return false; }
        @Override protected void onEnable() { warned = false; lines = 0; ticks = 0; }
        @Override protected void onDisable() { flush(); }
        @Override public void onWorldLeave() { flush(); }
        @Override public String info() { return lines == 0 ? "" : lines + " lines"; }

        @Override public void onTick() {
            if (++ticks >= flushSeconds.intValue() * 20) {
                ticks = 0;
                flush();
            }
        }

        @Override public boolean allowIncomingChat(String plain) {
            LocalDate today = LocalDate.now();
            if (bufferDate != null && !bufferDate.equals(today)) flush();
            bufferDate = today;
            buffer.append('[').append(LocalTime.now().format(TIME)).append("] ")
                    .append(plain.replace('\n', ' ').replace('\r', ' ')).append('\n');
            lines++;
            if (buffer.length() > MAX_BUFFER_CHARS) flush();
            return true;
        }

        private void flush() {
            if (buffer.length() == 0 || bufferDate == null) return;
            String text = buffer.toString();
            buffer.setLength(0);
            try {
                Path dir = Iris.config().root().resolve("logs");
                Files.createDirectories(dir);
                Files.writeString(dir.resolve("chat-" + bufferDate + ".log"), text, StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            } catch (IOException | RuntimeException e) {
                if (!warned) {
                    warned = true;
                    Iris.LOGGER.warn("Chat Logger could not write its log file", e);
                    notifications().error("Chat Logger", "Could not write the log file.");
                }
            }
        }
    }

    /** Blocks outgoing messages sent faster than a minimum interval. */
    static final class SlowModeGuard extends Module {
        private final NumberSetting delay = add(new NumberSetting("Delay", "Minimum milliseconds between messages.", 1500, 250, 10000, 250));
        private final BoolSetting commands = add(new BoolSetting("Include Commands", "Also rate limit slash commands.", false));
        private long lastSent;
        private long lastWarn;

        SlowModeGuard() { super("Slow Mode Guard", "Blocks chat messages you send faster than your chosen delay.", Category.CHAT); }

        @Override protected boolean requiresWorld() { return false; }
        @Override protected void onEnable() { lastSent = 0; }
        @Override public String info() { return delay.intValue() + "ms"; }

        @Override public String onOutgoingChat(String message) {
            if (message.startsWith("/") && !commands.on()) return message;
            long now = System.currentTimeMillis();
            if (now - lastSent < delay.intValue()) {
                if (now - lastWarn > 2000) {
                    lastWarn = now;
                    notifications().warn("Slow Mode Guard", "Message blocked: wait " + (delay.intValue() - (now - lastSent)) + " ms.");
                }
                return null;
            }
            lastSent = now;
            return message;
        }
    }
}
