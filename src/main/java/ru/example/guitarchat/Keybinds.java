package ru.example.guitarchat;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;

@net.neoforged.fml.common.EventBusSubscriber(modid = GuitarChat.MODID, value = Dist.CLIENT, bus = net.neoforged.fml.common.EventBusSubscriber.Bus.MOD)
public final class Keybinds {
    public static final KeyMapping OPEN_CONFIG = new KeyMapping(
            "key.guitarchat.open",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_G,
            "key.categories.guitarchat");

    @SubscribeEvent
    public static void onRegisterKeys(RegisterKeyMappingsEvent event) {
        event.register(OPEN_CONFIG);
    }
}


