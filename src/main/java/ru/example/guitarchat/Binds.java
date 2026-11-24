package ru.example.guitarchat;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;

import org.lwjgl.glfw.GLFW;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class Binds {
    public static final String[] NOTES = {
            "E2", "A2", "D3", "G3", "B3", "C4", "D4", "E4", "F4", "G4", "A4", "C5"
    };
    public static final String[] ACTIONS = {
            "none", "jump", "sprint", "sneak", "attack", "use", "inventory", "drop", "swap_offhand",
            "forward", "back", "left", "right",
            "toggle_forward", "toggle_back", "toggle_left", "toggle_right", "stop_move",
            "look_left", "look_right", "look_up", "look_down",
            "look_left_step", "look_right_step", "look_up_step", "look_down_step",
            "ui_up", "ui_down", "ui_left", "ui_right", "ui_confirm", "ui_cancel",
            "bow",
            "hotbar_1", "hotbar_2", "hotbar_3", "hotbar_4", "hotbar_5", "hotbar_6", "hotbar_7", "hotbar_8", "hotbar_9"
    };

    public static float volumeThreshold = 0.025f;
    public static float sfmThreshold = 0.30f;
    public static float lookSensitivity = 4.0f;
    public static float lookStepDegrees = 22.5f;
    public static float uiCursorStep = 8.0f;

    public static final Map<String, String> binds = new LinkedHashMap<>();
    public static final Map<String, String> chords = new LinkedHashMap<>();

    private static final Set<String> HELD_ACTIONS = Set.of("forward", "back", "left", "right", "sprint", "sneak");
    private static final Set<String> LOOK_ACTIONS = Set.of("look_left", "look_right", "look_up", "look_down");
    private static final Set<String> LOOK_STEP_ACTIONS = Set.of("look_left_step", "look_right_step", "look_up_step", "look_down_step");
    private static final Set<String> UI_MOVE_ACTIONS = Set.of("ui_up", "ui_down", "ui_left", "ui_right");

    private static final Set<String> prevNotes = new HashSet<>();
    private static String lastSticky = "";
    private static String lastTriggered = "";
    private static long lastTriggerMs = 0;

    private static String heldAction = null;
    private static int holdLeft = 0;

    private static boolean tFwd = false, tBack = false, tLeft = false, tRight = false;

    private static final Map<String, Long> chordLast = new HashMap<>();

    private static boolean invNoteAbsent = true;
    private static long invLastToggle = 0;

    static {
        resetDefaults();
    }

    public static void resetDefaults() {
        binds.clear();
        for (String n : NOTES) binds.put(n, "none");
        binds.put("E2", "jump");
        binds.put("A2", "attack");
        binds.put("D3", "toggle_forward");
        binds.put("G3", "toggle_left");
        binds.put("B3", "toggle_right");
        binds.put("C4", "stop_move");
        binds.put("D4", "sprint");
        binds.put("E4", "look_right_step");
        binds.put("F4", "look_left_step");
        binds.put("G4", "use");
        binds.put("A4", "inventory");
        binds.put("C5", "drop");
        chords.clear();
    }

    public static Path configPath() {
        return net.neoforged.fml.loading.FMLPaths.CONFIGDIR.get().resolve("guitarchat.toml");
    }

    public static void load() {
        Path p = configPath();
        if (!Files.exists(p)) {
            resetDefaults();
            save();
            return;
        }
        try {
            Map<String, String> parsed = new LinkedHashMap<>();
            Map<String, String> parsedChords = new LinkedHashMap<>();
            boolean inBinds = false;
            for (String line : Files.readAllLines(p, StandardCharsets.UTF_8)) {
                String s = line.trim();
                if (s.isEmpty() || s.startsWith("#")) continue;
                if (s.equals("[binds]")) { inBinds = true; continue; }
                int eq = s.indexOf('=');
                if (eq <= 0) continue;
                String key = s.substring(0, eq).trim();
                String val = s.substring(eq + 1).trim();
                if (val.startsWith("\"") && val.endsWith("\"") && val.length() >= 2) {
                    val = val.substring(1, val.length() - 1);
                }
                if (!inBinds) {
                    if (key.equals("volumeThreshold")) volumeThreshold = parseFloat(val, volumeThreshold);
                    else if (key.equals("sfmThreshold")) sfmThreshold = parseFloat(val, sfmThreshold);
                    else if (key.equals("lookSensitivity")) lookSensitivity = parseFloat(val, lookSensitivity);
                    else if (key.equals("lookStepDegrees")) lookStepDegrees = parseFloat(val, lookStepDegrees);
                    else if (key.equals("uiCursorStep")) uiCursorStep = parseFloat(val, uiCursorStep);
                } else if (key.contains("+")) {
                    parsedChords.put(normalizeChord(key), normalize(val));
                } else {
                    parsed.put(key, normalize(val));
                }
            }
            for (String n : NOTES) {
                if (parsed.containsKey(n)) binds.put(n, parsed.get(n));
            }
            chords.clear();
            chords.putAll(parsedChords);
        } catch (IOException e) {
            resetDefaults();
        }
    }

    private static float parseFloat(String s, float def) {
        try { return Float.parseFloat(s); } catch (NumberFormatException e) { return def; }
    }

    public static String normalize(String action) {
        String a = action.trim().toLowerCase(Locale.ROOT);
        for (String known : ACTIONS) {
            if (known.equals(a)) return a;
        }
        return "none";
    }

    private static String normalizeChord(String key) {
        String[] parts = key.split("\\+");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) sb.append('+');
            sb.append(normalize(parts[i]));
        }
        return sb.toString();
    }

    public static void save() {
        Path p = configPath();
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("# GuitarChat configuration\n");
            sb.append("volumeThreshold = ").append(volumeThreshold).append('\n');
            sb.append("sfmThreshold = ").append(sfmThreshold).append('\n');
            sb.append("lookSensitivity = ").append(lookSensitivity).append('\n');
            sb.append("lookStepDegrees = ").append(lookStepDegrees).append('\n');
            sb.append("uiCursorStep = ").append(uiCursorStep).append('\n');
            sb.append("\n[binds]\n");
            for (Map.Entry<String, String> e : binds.entrySet()) {
                sb.append(e.getKey()).append(" = \"").append(e.getValue()).append("\"\n");
            }
            for (Map.Entry<String, String> e : chords.entrySet()) {
                sb.append(e.getKey()).append(" = \"").append(e.getValue()).append("\"\n");
            }
            Files.createDirectories(p.getParent());
            Files.writeString(p, sb.toString(), StandardCharsets.UTF_8);
        } catch (IOException ignored) {
        }
    }

    private static boolean isInventoryScreen(net.minecraft.client.gui.screens.Screen s) {
        if (s == null) return false;
        return s.getClass().getName().contains("InventoryScreen");
    }

    private static String inventoryNote() {
        for (Map.Entry<String, String> en : binds.entrySet()) {
            if ("inventory".equals(en.getValue())) return en.getKey();
        }
        return null;
    }

    public static void tick(Set<String> notes, String sticky, boolean rejected) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) { releaseAll(); return; }

        if (mc.screen != null) { handleScreen(notes, sticky, rejected, mc); return; }

        applyToggles(mc);

        for (String n : notes) {
            if (!prevNotes.contains(n)) trigger(n);
        }
        prevNotes.clear();
        prevNotes.addAll(notes);

        if (rejected) {
            lastSticky = "";
        } else if (sticky != null && !sticky.isEmpty()) {
            if (!sticky.equals(lastSticky)) {
                lastSticky = sticky;
                trigger(sticky);
            } else if (!notes.isEmpty() && notes.contains(sticky)) {
                trigger(sticky);
            }
        } else {
            lastSticky = "";
        }

        checkChords(notes);
        tickHold();
    }

    private static void handleScreen(Set<String> notes, String sticky, boolean rejected, Minecraft mc) {
        releaseMovementKeys(mc);
        releaseHeld();

        String invNote = inventoryNote();
        if (invNote != null) {
            boolean present = notes.contains(invNote);
            if (!present) {
                invNoteAbsent = true;
            } else if (invNoteAbsent && System.currentTimeMillis() - invLastToggle > 900) {
                invNoteAbsent = false;
                invLastToggle = System.currentTimeMillis();
                if (isInventoryScreen(mc.screen)) mc.setScreen(null);
            }
        }

        for (String n : notes) {
            if (!prevNotes.contains(n)) triggerUi(n);
        }
        prevNotes.clear();
        prevNotes.addAll(notes);

        if (rejected) {
            lastSticky = "";
        } else if (sticky != null && !sticky.isEmpty()) {
            if (!sticky.equals(lastSticky)) {
                lastSticky = sticky;
                triggerUi(sticky);
            } else if (!notes.isEmpty() && notes.contains(sticky)) {
                triggerUi(sticky);
            }
        } else {
            lastSticky = "";
        }

        tickHold();
    }

    private static void trigger(String note) {
        String a = binds.getOrDefault(note, "none");
        if (a.equals("none")) return;
        long now = System.currentTimeMillis();
        if (a.equals(lastTriggered) && now - lastTriggerMs < 400) return;
        lastTriggered = a;
        lastTriggerMs = now;
        fire(a);
    }

    private static void triggerUi(String note) {
        String a = binds.getOrDefault(note, "none");
        if (a.equals("none")) return;
        long now = System.currentTimeMillis();
        if (a.equals(lastTriggered) && now - lastTriggerMs < 300) return;
        lastTriggered = a;
        lastTriggerMs = now;
        fire(a);
    }

    private static void fire(String a) {
        Minecraft mc = Minecraft.getInstance();
        boolean screenOpen = mc.screen != null;

        if (a.equals("inventory")) {
            long now = System.currentTimeMillis();
            if (now - invLastToggle < 600) return;
            invLastToggle = now;
            invNoteAbsent = false;
            if (screenOpen) {
                if (isInventoryScreen(mc.screen)) mc.setScreen(null);
                return;
            }
            holdAction(a, 6);
            return;
        }

        if (screenOpen && !UI_MOVE_ACTIONS.contains(a) && !a.equals("ui_confirm") && !a.equals("ui_cancel")) return;

        switch (a) {
            case "ui_confirm":
                clickMouse(GLFW.GLFW_MOUSE_BUTTON_1);
                return;
            case "ui_cancel":
                if (mc.screen != null) mc.screen.onClose();
                return;
            case "toggle_forward": tFwd = true; return;
            case "toggle_back": tBack = true; return;
            case "toggle_left": tLeft = true; return;
            case "toggle_right": tRight = true; return;
            case "stop_move": tFwd = tBack = tLeft = tRight = false; return;
            case "bow":
                holdAction(a, 28);
                return;
        }
        if (LOOK_STEP_ACTIONS.contains(a)) {
            holdAction(a, 8);
            return;
        }
        int dur = HELD_ACTIONS.contains(a) || LOOK_ACTIONS.contains(a) || UI_MOVE_ACTIONS.contains(a) ? 20 : 6;
        holdAction(a, dur);
    }

    private static void checkChords(Set<String> notes) {
        if (chords.isEmpty() || notes.size() < 2) return;
        long now = System.currentTimeMillis();
        for (Map.Entry<String, String> e : chords.entrySet()) {
            boolean all = true;
            for (String p : e.getKey().split("\\+")) {
                if (!notes.contains(p)) { all = false; break; }
            }
            if (!all) continue;
            long last = chordLast.getOrDefault(e.getValue(), 0L);
            if (now - last < 800) continue;
            chordLast.put(e.getValue(), now);
            fire(e.getValue());
        }
    }

    private static void holdAction(String a, int ticks) {
        if (heldAction != null) releaseHeld();
        heldAction = a;
        holdLeft = ticks;
        KeyMapping k = keyFor(Minecraft.getInstance().options, a);
        if (k != null) {
            k.setDown(true);
            if (!HELD_ACTIONS.contains(a)) KeyMapping.click(k.getKey());
        }
    }

    private static void tickHold() {
        if (heldAction == null) return;
        Minecraft mc = Minecraft.getInstance();
        if (LOOK_ACTIONS.contains(heldAction) && mc.screen == null && mc.player != null) {
            float s = lookSensitivity;
            switch (heldAction) {
                case "look_right": mc.player.turn(s, 0); break;
                case "look_left": mc.player.turn(-s, 0); break;
                case "look_down": mc.player.turn(0, s); break;
                case "look_up": mc.player.turn(0, -s); break;
            }
        } else if (LOOK_STEP_ACTIONS.contains(heldAction) && mc.screen == null && mc.player != null) {
            float s = lookStepDegrees / 8f / 0.15f;
            switch (heldAction) {
                case "look_right_step": mc.player.turn(s, 0); break;
                case "look_left_step": mc.player.turn(-s, 0); break;
                case "look_down_step": mc.player.turn(0, s); break;
                case "look_up_step": mc.player.turn(0, -s); break;
            }
        } else if (UI_MOVE_ACTIONS.contains(heldAction) && mc.screen != null) {
            moveCursor(heldAction);
        }
        if (--holdLeft <= 0) releaseHeld();
    }

    private static void moveCursor(String a) {
        Minecraft mc = Minecraft.getInstance();
        double scale = mc.getWindow().getGuiScale();
        if (scale <= 0) scale = 1;
        double x = mc.mouseHandler.xpos() / scale;
        double y = mc.mouseHandler.ypos() / scale;
        switch (a) {
            case "ui_left": x -= uiCursorStep; break;
            case "ui_right": x += uiCursorStep; break;
            case "ui_up": y -= uiCursorStep; break;
            case "ui_down": y += uiCursorStep; break;
        }
        x = Math.max(0, Math.min(x, mc.getWindow().getGuiScaledWidth()));
        y = Math.max(0, Math.min(y, mc.getWindow().getGuiScaledHeight()));
        GLFW.glfwSetCursorPos(mc.getWindow().getWindow(), x * scale, y * scale);
    }

    private static void clickMouse(int btn) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen == null) return;
        double scale = mc.getWindow().getGuiScale();
        if (scale <= 0) scale = 1;
        double x = mc.mouseHandler.xpos() / scale;
        double y = mc.mouseHandler.ypos() / scale;
        mc.screen.mouseClicked(x, y, btn);
    }

    private static void applyToggles(Minecraft mc) {
        Options o = mc.options;
        press(o.keyUp, tFwd);
        press(o.keyDown, tBack);
        press(o.keyLeft, tLeft);
        press(o.keyRight, tRight);
    }

    private static void releaseMovementKeys(Minecraft mc) {
        Options o = mc.options;
        press(o.keyUp, false);
        press(o.keyDown, false);
        press(o.keyLeft, false);
        press(o.keyRight, false);
        press(o.keySprint, false);
        press(o.keyShift, false);
    }

    private static void press(KeyMapping k, boolean down) {
        if (k != null) k.setDown(down);
    }

    private static void releaseHeld() {
        if (heldAction != null) {
            KeyMapping k = keyFor(Minecraft.getInstance().options, heldAction);
            if (k != null) k.setDown(false);
        }
        heldAction = null;
        holdLeft = 0;
    }

    private static KeyMapping keyFor(Options o, String a) {
        switch (a) {
            case "forward": return o.keyUp;
            case "back": return o.keyDown;
            case "left": return o.keyLeft;
            case "right": return o.keyRight;
            case "jump": return o.keyJump;
            case "sprint": return o.keySprint;
            case "sneak": return o.keyShift;
            case "attack": return o.keyAttack;
            case "use": return o.keyUse;
            case "inventory": return o.keyInventory;
            case "drop": return o.keyDrop;
            case "swap_offhand": return o.keySwapOffhand;
            default:
                if (a.startsWith("hotbar_")) {
                    try {
                        int i = Integer.parseInt(a.substring(7));
                        if (i >= 1 && i <= 9) return o.keyMappings[i - 1];
                    } catch (NumberFormatException ignored) {
                    }
                }
                return null;
        }
    }

    public static String activeToggles() {
        StringBuilder sb = new StringBuilder();
        if (tFwd) sb.append("forward ");
        if (tBack) sb.append("back ");
        if (tLeft) sb.append("left ");
        if (tRight) sb.append("right ");
        if (heldAction != null && (heldAction.equals("sprint") || heldAction.equals("sneak") || heldAction.equals("bow")))
            sb.append(heldAction);
        return sb.toString().trim();
    }
    public static void apply(Set<String> notes) {
        tick(notes, "", false);
    }

    public static void releaseAll() {
        releaseHeld();
        tFwd = tBack = tLeft = tRight = false;
        Minecraft mc = Minecraft.getInstance();
        releaseMovementKeys(mc);
        prevNotes.clear();
        lastSticky = "";
    }
}