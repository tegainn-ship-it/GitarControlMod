package ru.example.guitarchat;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public final class GuitarChatActions {

    private static Set<String> prev = Set.of();
    private static final Map<String, Integer> releaseTimers = new HashMap<>();

    private GuitarChatActions() {
    }

    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        AudioEngine ae = AudioEngine.get();
        boolean active = mc.player != null && mc.screen == null && ae.isCalibrated() && !ae.isCalibrating();
        Set<String> now = active ? ae.activeNotes() : Set.of();

        for (String n : prev) {
            if (!now.contains(n)) release(n);
        }
        for (String n : now) {
            if (!prev.contains(n)) press(n, mc);
        }
        prev = now;

        for (String key : releaseTimers.keySet().toArray(new String[0])) {
            int left = releaseTimers.get(key) - 1;
            if (left <= 0) {
                setKey(key, false);
                releaseTimers.remove(key);
            } else {
                releaseTimers.put(key, left);
            }
        }
    }

    private static void press(String note, Minecraft mc) {
        String action = Binds.binds.getOrDefault(note, "none");
        if (action.equals("none")) return;
        if (action.startsWith("hotbar_")) {
            try {
                int slot = Integer.parseInt(action.substring(7)) - 1;
                if (slot >= 0 && slot < 9) mc.player.getInventory().selected = slot;
            } catch (NumberFormatException ignored) {
            }
            return;
        }
        if (keyFor(action) == null) return;
        setKey(action, true);
        releaseTimers.put(action, 2);
    }

    private static void release(String note) {
        String action = Binds.binds.getOrDefault(note, "none");
        if (keyFor(action) != null) setKey(action, false);
    }

    private static void setKey(String action, boolean down) {
        KeyMapping k = keyFor(action);
        if (k != null) k.setDown(down);
    }

    private static KeyMapping keyFor(String action) {
        Minecraft mc = Minecraft.getInstance();
        switch (action) {
            case "jump": return mc.options.keyJump;
            case "sprint": return mc.options.keySprint;
            case "sneak": return mc.options.keyShift;
            case "forward": return mc.options.keyUp;
            case "back": return mc.options.keyDown;
            case "left": return mc.options.keyLeft;
            case "right": return mc.options.keyRight;
            case "attack": return mc.options.keyAttack;
            case "use": return mc.options.keyUse;
            case "inventory": return mc.options.keyInventory;
            case "drop": return mc.options.keyDrop;
            case "swap_offhand": return mc.options.keySwapOffhand;
            default: return null;
        }
    }
}