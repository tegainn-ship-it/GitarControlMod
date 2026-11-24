package ru.example.guitarchat;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

public final class AudioEngine {
    public static final int CALIBRATION_DURATION_MS = 10000;
    public static final int FRAME_SIZE = 2048;
    private static final AudioEngine INSTANCE = new AudioEngine();

    public static AudioEngine get() {
        return INSTANCE;
    }

    private final PitchDetector detector = new PitchDetector();
    private final short[] frame = new short[FRAME_SIZE];
    private int framePos = 0;

    private long calibrationStart = -1;
    private double calibrationSum = 0;
    private int calibrationCount = 0;
    private volatile boolean calibrating = false;
    private volatile boolean calibrated = false;

    public volatile long packetCount = 0;
    public volatile boolean micOk = false;
    public volatile Set<String> activeNotes = Collections.emptySet();
    public volatile String lastNote = "";
    public volatile boolean lastRejected = false;
    public volatile float lastRms = 0;

    private AudioEngine() {
    }

    public void offer(short[] samples) {
        if (samples == null || samples.length == 0) return;
        packetCount++;
        for (short s : samples) {
            frame[framePos++] = s;
            if (framePos == FRAME_SIZE) {
                framePos = 0;
                processFrame(frame);
            }
        }
    }

    private void processFrame(short[] buf) {
        double sum = 0;
        for (short s : buf) sum += (double) s * s;
        float rms = (float) Math.sqrt(sum / buf.length) / 32768f;
        lastRms = rms;

        if (calibrating) {
            calibrationSum += rms;
            calibrationCount++;
            return;
        }
        if (!calibrated) return;

        if (rms < Binds.volumeThreshold) {
            activeNotes = Collections.emptySet();
            lastNote = "";
            lastRejected = false;
            return;
        }

        detector.setSfmThreshold(Binds.sfmThreshold);
        PitchDetector.Result r = detector.process(buf);
        if (r == null) return;

        if (r.rejected || r.notes.isEmpty()) {
            activeNotes = Collections.emptySet();
            lastNote = "";
            lastRejected = true;
            return;
        }

        activeNotes = Collections.unmodifiableSet(new LinkedHashSet<>(r.notes));
        lastNote = String.join(" ", r.notes);
        lastRejected = false;
    }

    public void tick() {
        if (calibrating && System.currentTimeMillis() - calibrationStart >= CALIBRATION_DURATION_MS) {
            finishCalibration();
        }
    }

    public void startCalibration() {
        MicCapture.start();
        calibrationStart = System.currentTimeMillis();
        calibrationSum = 0;
        calibrationCount = 0;
        calibrating = true;
    }

    public boolean isCalibrating() { return calibrating; }

    public boolean isCalibrated() { return calibrated; }

    public int secondsLeft() {
        if (!calibrating) return 0;
        return (int) Math.ceil((CALIBRATION_DURATION_MS - (System.currentTimeMillis() - calibrationStart)) / 1000.0);
    }

    private void finishCalibration() {
        calibrating = false;
        calibrated = true;
        if (calibrationCount == 0) return;
        float avg = (float) (calibrationSum / calibrationCount);
        float t = avg * 1.5f;
        if (t < 0.005f) t = 0.005f;
        if (t > 0.5f) t = 0.5f;
        Binds.volumeThreshold = t;
        Binds.save();
    }

    public void resetSession() {
        calibrating = false;
        calibrated = false;
        activeNotes = Collections.emptySet();
        lastNote = "";
        lastRejected = false;
    }

    public Set<String> activeNotes() {
        return activeNotes;
    }
}