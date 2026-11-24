package ru.example.guitarchat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class PitchDetector {
    public static class Result {
        public final List<String> notes;
        public final boolean rejected;

        public Result(List<String> notes, boolean rejected) {
            this.notes = notes;
            this.rejected = rejected;
        }
    }

    private static final int FFT_SIZE = 2048;
    private static final double MIN_FREQ = 70.0;
    private static final double MAX_FREQ = 1000.0;
    private static final int MAX_POLYPHONY = 3;

    private static final String[] NOTE_NAMES = {
            "E2", "A2", "D3", "G3", "B3", "C4", "D4", "E4", "F4", "G4", "A4", "C5"
    };
    private static final double[] NOTE_FREQS = {
            82.41, 110.00, 146.83, 196.00, 246.94,
            261.63, 293.66, 329.63, 349.23, 392.00, 440.00, 523.25
    };

    private float sfmThreshold = 0.30f;
    private final float sampleRate = 48000f;

    private final double[] window = new double[FFT_SIZE];
    private final double[] re = new double[FFT_SIZE];
    private final double[] im = new double[FFT_SIZE];
    private final double[] spectrum = new double[FFT_SIZE / 2];

    public PitchDetector() {
        for (int i = 0; i < FFT_SIZE; i++) {
            window[i] = 0.5 * (1 - Math.cos(2 * Math.PI * i / (FFT_SIZE - 1)));
        }
    }

    public void setSfmThreshold(float t) {
        this.sfmThreshold = t;
    }

    public Result process(short[] samples) {
        int n = Math.min(samples.length, FFT_SIZE);
        for (int i = 0; i < FFT_SIZE; i++) {
            double v = (i < n ? samples[i] : 0) / 32768.0;
            re[i] = v * window[i];
            im[i] = 0;
        }
        fft(re, im);

        for (int i = 0; i < FFT_SIZE / 2; i++) {
            spectrum[i] = re[i] * re[i] + im[i] * im[i];
        }

        double geoSum = 0, linSum = 0;
        int bins = 0;
        int lo = Math.max(1, (int) (80.0 * FFT_SIZE / sampleRate));
        int hi = (int) (1200.0 * FFT_SIZE / sampleRate);
        if (hi >= spectrum.length) hi = spectrum.length - 1;
        for (int i = lo; i <= hi; i++) {
            double mag = Math.sqrt(spectrum[i]) + 1e-12;
            geoSum += Math.log(mag);
            linSum += mag;
            bins++;
        }
        if (bins == 0) return null;
        double sfm = Math.exp(geoSum / bins) / (linSum / bins);
        if (sfm > sfmThreshold) {
            return new Result(Collections.emptyList(), true);
        }

        double[] spec = spectrum.clone();
        List<Double> freqs = new ArrayList<>();
        for (int k = 0; k < MAX_POLYPHONY; k++) {
            int best = -1;
            double bestVal = 0;
            int minBin = (int) (MIN_FREQ * FFT_SIZE / sampleRate);
            int maxBin = (int) (MAX_FREQ * FFT_SIZE / sampleRate);
            for (int i = minBin; i <= maxBin && i < spec.length; i++) {
                if (spec[i] > bestVal) { bestVal = spec[i]; best = i; }
            }
            if (best < 0 || bestVal <= 0) break;
            double peak = best + parabolic(spec, best);
            double freq = peak * sampleRate / FFT_SIZE;
            if (freq < MIN_FREQ || freq > MAX_FREQ) break;
            freqs.add(freq);
            for (int d = -3; d <= 3; d++) {
                int b = best + d;
                if (b >= 0 && b < spec.length) spec[b] *= 0.05;
            }
            for (int h = 2; h <= 6; h++) {
                int bin = (int) Math.round(freq * h * FFT_SIZE / sampleRate);
                for (int d = -2; d <= 2; d++) {
                    int b = bin + d;
                    if (b >= 0 && b < spec.length) spec[b] *= 0.05;
                }
            }
        }

        List<String> notes = new ArrayList<>();
        for (double f : freqs) {
            String note = nearestNote(f);
            if (note != null && !notes.contains(note)) notes.add(note);
        }
        if (notes.isEmpty()) return new Result(Collections.emptyList(), true);
        return new Result(notes, false);
    }

    private static double parabolic(double[] s, int i) {
        if (i <= 0 || i >= s.length - 1) return 0;
        double a = Math.log(s[i - 1] + 1e-12);
        double b = Math.log(s[i] + 1e-12);
        double c = Math.log(s[i + 1] + 1e-12);
        double denom = a - 2 * b + c;
        if (Math.abs(denom) < 1e-12) return 0;
        double d = 0.5 * (a - c) / denom;
        if (Double.isNaN(d)) return 0;
        return Math.max(-0.5, Math.min(0.5, d));
    }

    private String nearestNote(double freq) {
        String best = null;
        double bestErr = Double.MAX_VALUE;
        for (int i = 0; i < NOTE_NAMES.length; i++) {
            double err = Math.abs(12 * Math.log(freq / NOTE_FREQS[i]) / Math.log(2));
            if (err < bestErr) { bestErr = err; best = NOTE_NAMES[i]; }
        }
        return bestErr < 0.75 ? best : null;
    }

    private static void fft(double[] re, double[] im) {
        int n = re.length;
        for (int i = 1, j = 0; i < n; i++) {
            int bit = n >> 1;
            for (; (j & bit) != 0; bit >>= 1) j ^= bit;
            j |= bit;
            if (i < j) {
                double tr = re[i]; re[i] = re[j]; re[j] = tr;
                double ti = im[i]; im[i] = im[j]; im[j] = ti;
            }
        }
        for (int len = 2; len <= n; len <<= 1) {
            double ang = -2 * Math.PI / len;
            double wr = Math.cos(ang), wi = Math.sin(ang);
            for (int i = 0; i < n; i += len) {
                double cr = 1, ci = 0;
                for (int k = 0; k < len / 2; k++) {
                    double ur = re[i + k], ui = im[i + k];
                    double vr = re[i + k + len / 2] * cr - im[i + k + len / 2] * ci;
                    double vi = re[i + k + len / 2] * ci + im[i + k + len / 2] * cr;
                    re[i + k] = ur + vr;
                    im[i + k] = ui + vi;
                    re[i + k + len / 2] = ur - vr;
                    im[i + k + len / 2] = ui - vi;
                    double ncr = cr * wr - ci * wi;
                    ci = cr * wi + ci * wr;
                    cr = ncr;
                }
            }
        }
    }
}