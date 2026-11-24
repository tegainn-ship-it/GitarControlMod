package ru.example.guitarchat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;

@Mod(GuitarChat.MODID)
public class GuitarChat {

    public static final String MODID = "guitarchat";

    public GuitarChat(IEventBus modEventBus) {
        modEventBus.addListener(this::onClientSetup);
        NeoForge.EVENT_BUS.addListener(this::onClientTick);
        NeoForge.EVENT_BUS.addListener(this::onJoin);
    }

    private void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(Binds::load);
    }

    private void onJoin(ClientPlayerNetworkEvent.LoggingIn event) {
        AudioEngine.get().startCalibration();
    }

    private void onClientTick(ClientTickEvent.Post event) {
        AudioEngine.get().tick();
        GuitarChatActions.tick();
    }

}
