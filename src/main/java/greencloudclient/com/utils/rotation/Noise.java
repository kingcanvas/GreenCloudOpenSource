package greencloudclient.com.utils.rotation;

import java.util.Random;

public class Noise {

    private final long startNanos = System.nanoTime();
    private long seed = new Random().nextLong();

    public void reseed() {
        seed = new Random().nextLong();
    }

    public double seconds() {
        return (System.nanoTime() - startNanos) / 1e9;
    }

    public double fractal(double position, long channel) {
        return sample(position, channel) * 0.62
                + sample(position * 2.03, channel + 101L) * 0.28
                + sample(position * 4.11, channel + 211L) * 0.1;
    }

    private double sample(double position, long channel) {
        long left = (long) Math.floor(position);
        double blend = position - left;
        blend = blend * blend * (3.0 - 2.0 * blend);
        double first = value(left, channel);
        double second = value(left + 1L, channel);
        return first + (second - first) * blend;
    }

    private double value(long index, long channel) {
        long v = index + seed + channel * 0x9E3779B97F4A7C15L;
        v = (v ^ (v >>> 30)) * 0xBF58476D1CE4E5B9L;
        v = (v ^ (v >>> 27)) * 0x94D049BB133111EBL;
        v ^= v >>> 31;
        return ((v >>> 11) * 1.1102230246251565E-16) * 2.0 - 1.0;
    }
}
