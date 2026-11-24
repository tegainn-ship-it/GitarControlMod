package ru.example.guitarchat;

import de.maxhenkel.voicechat.api.VoicechatClientApi;
import de.maxhenkel.voicechat.api.ForgeVoicechatPlugin;
import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import de.maxhenkel.voicechat.api.events.MicrophonePacketEvent;
import de.maxhenkel.voicechat.api.opus.OpusDecoder;

@ForgeVoicechatPlugin
public class GuitarChatPlugin implements VoicechatPlugin {
    private OpusDecoder decoder;

    @Override
    public String getPluginId() {
        return GuitarChat.MODID;
    }

    @Override
    public void initialize(de.maxhenkel.voicechat.api.VoicechatApi api) {
        if (api instanceof VoicechatClientApi clientApi) {
            decoder = clientApi.createDecoder();
        }
    }

    @Override
    public void registerEvents(EventRegistration registration) {
        registration.registerEvent(MicrophonePacketEvent.class, event -> {
            if (decoder == null) return;
            short[] audio = decoder.decode(event.getPacket().getOpusEncodedData());
            AudioEngine.get().offer(audio);
        });
    }
}