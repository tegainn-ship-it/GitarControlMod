package ru.example.guitarchat;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public class GuitarChatConfigScreen extends Screen {
    private static final int BOX_W = 96;

    private final Screen parent;
    private final List<EditBox> bindFields = new ArrayList<>();
    private final List<String> bindNotes = new ArrayList<>();
    private EditBox volumeField;
    private EditBox sfmField;

    public GuitarChatConfigScreen(Screen parent) {
        super(Component.literal("GuitarChat"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int cx = this.width / 2;
        int left = cx - 190;
        int right = cx + 14;
        int y = 46;

        volumeField = new EditBox(this.font, left + 96, y, BOX_W, 18, Component.literal("volumeThreshold"));
        volumeField.setValue(trim(Binds.volumeThreshold));
        addRenderableWidget(volumeField);

        sfmField = new EditBox(this.font, left + 96, y + 24, BOX_W, 18, Component.literal("sfmThreshold"));
        sfmField.setValue(trim(Binds.sfmThreshold));
        addRenderableWidget(sfmField);

        int half = (Binds.NOTES.length + 1) / 2;
        for (int i = 0; i < Binds.NOTES.length; i++) {
            String note = Binds.NOTES[i];
            int col = (i < half) ? 0 : 1;
            int r = (i < half) ? i : i - half;
            int x = (col == 0 ? left : right) + 46;
            int ry = y + 54 + r * 22;
            EditBox b = new EditBox(this.font, x, ry, BOX_W, 18, Component.literal(note));
            b.setValue(Binds.binds.getOrDefault(note, "none"));
            b.setMaxLength(16);
            addRenderableWidget(b);
            bindFields.add(b);
            bindNotes.add(note);
        }

        addRenderableWidget(Button.builder(Component.literal("Save"), b -> save())
                .bounds(cx - 160, this.height - 28, 100, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Recalibrate"), b -> {
                    AudioEngine.get().startCalibration();
                    onClose();
                })
                .bounds(cx - 50, this.height - 28, 100, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Cancel"), b -> onClose())
                .bounds(cx + 60, this.height - 28, 100, 20).build());
    }

    private static String trim(float f) {
        String s = Float.toString(f);
        return s;
    }

    private void save() {
        try {
            float v = Float.parseFloat(volumeField.getValue().trim());
            Binds.volumeThreshold = Math.max(0.001f, Math.min(1.0f, v));
        } catch (NumberFormatException ignored) {
        }
        try {
            float s = Float.parseFloat(sfmField.getValue().trim());
            Binds.sfmThreshold = Math.max(0.0f, Math.min(1.0f, s));
        } catch (NumberFormatException ignored) {
        }
        for (int i = 0; i < bindFields.size(); i++) {
            Binds.binds.put(bindNotes.get(i), Binds.normalize(bindFields.get(i).getValue()));
        }
        Binds.save();
        onClose();
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        renderBackground(g, mouseX, mouseY, delta);
        g.drawCenteredString(this.font, this.title, this.width / 2, 11, 0xFFFFFFFF);

        int cx = this.width / 2;
        int left = cx - 190;
        int right = cx + 14;
        int y = 46;

        g.fill(cx - 70, 6, cx + 70, 26, 0x90000000); g.fill(left - 6, y - 16, right + BOX_W + 6, y + 54 + ((Binds.NOTES.length + 1) / 2) * 22 + 6, 0x90000000); g.fill(cx - 200, this.height - 52, cx + 200, this.height - 22, 0x90000000); g.drawString(this.font, "Volume", left, y + 5, 0xFFFFFFFF, true);
        g.drawString(this.font, "Voice cut", left, y + 29, 0xFFFFFFFF, true);

        int half = (Binds.NOTES.length + 1) / 2;
        for (int i = 0; i < Binds.NOTES.length; i++) {
            int col = (i < half) ? 0 : 1;
            int r = (i < half) ? i : i - half;
            int x = (col == 0 ? left : right);
            int ry = y + 54 + r * 22;
            g.drawString(this.font, Binds.NOTES[i], x, ry + 5, 0xFFFFD080, true);
        }

        g.drawString(this.font, "G opens this menu. Actions: jump, sprint, attack...", cx - 190, this.height - 44, 0xFFE0E0E0, true);

        super.render(g, mouseX, mouseY, delta);
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(parent);
    }
}
