package ru.example.guitarchat;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.TargetDataLine;

public final class MicCapture {
    private static MicCapture instance;

    public static void start() {
        if (instance != null) return;
        instance = new MicCapture();
        Thread t = new Thread(instance::run, "GuitarChat-Mic");
        t.setDaemon(true);
        t.start();
    }

    private void run() {
        try {
            AudioFormat format = new AudioFormat(48000f, 16, 1, true, false);
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
            if (!AudioSystem.isLineSupported(info)) {
                System.out.println("[GuitarChat] mic line not supported");
                return;
            }
            TargetDataLine line = (TargetDataLine) AudioSystem.getLine(info);
            line.open(format, 4096);
            line.start();
            AudioEngine.get().micOk = true;
            System.out.println("[GuitarChat] mic capture started");

            byte[] buf = new byte[4096];
            short[] samples = new short[2048];
            int filled = 0;
            while (true) {
                int read = line.read(buf, 0, buf.length);
                int shorts = read / 2;
                for (int i = 0; i < shorts; i++) {
                    int lo = buf[2 * i] & 0xFF;
                    int hi = buf[2 * i + 1];
                    samples[filled++] = (short) ((hi << 8) | lo);
                    if (filled == 2048) {
                        filled = 0;
                        AudioEngine.get().offer(samples.clone());
                    }
                }
            }
        } catch (Exception ex) {
            System.out.println("[GuitarChat] mic capture failed: " + ex);
        }
    }
}