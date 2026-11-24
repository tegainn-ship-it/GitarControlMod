package ru.example.guitarchat;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

@net.neoforged.fml.common.EventBusSubscriber(modid = GuitarChat.MODID, value = Dist.CLIENT)
public final class Hooks {

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        AudioEngine engine = AudioEngine.get();

        if (mc.player == null) {
            if (engine.isCalibrating() || engine.isCalibrated()) engine.resetSession();
            Binds.releaseAll();
            return;
        }

        engine.tick();

        while (Keybinds.OPEN_CONFIG.consumeClick()) {
            Binds.releaseAll();
            mc.setScreen(new GuitarChatConfigScreen(null));
        }

        Binds.tick(engine.activeNotes(), engine.lastNote, engine.lastRejected);
    }

    @SubscribeEvent
    public static void onHud(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) return;
        AudioEngine engine = AudioEngine.get();
        var g = event.getGuiGraphics();

        if (engine.isCalibrating()) {
            g.drawString(mc.font, "Calibrating... stay quiet (" + engine.secondsLeft() + "s)", 4, 4, 0xFFFFAA00);
            return;
        }
        if (!engine.isCalibrated()) {
            g.drawString(mc.font, "GuitarChat: not calibrated", 4, 4, 0xFFFF5555);
            return;
        }

        int i = 0;
        for (String n : Binds.NOTES) {
            String a = Binds.binds.getOrDefault(n, "none");
            if (a.equals("none")) continue;
            boolean on = engine.activeNotes().contains(n);
            int col = i % 4, row = i / 4;
            int x = 4 + col * 85, y = 4 + row * 11;
            String txt = n + ":" + a + (on ? " *" : "");
            g.drawString(mc.font, txt, x, y, on ? 0xFF55FF55 : 0xFF999999);
            i++;
        }

        int rows = (i + 3) / 4;
        int yStatus = 4 + rows * 11;
        String mv = Binds.activeToggles();
        if (!mv.isEmpty()) {
            g.drawString(mc.font, "MOVING: " + mv, 4, yStatus, 0xFF55FF55);
            yStatus += 11;
        }
        if (engine.lastRejected) {
            g.drawString(mc.font, "~ voice/noise", 4, yStatus, 0xFFFF5555);
        }
    }
}