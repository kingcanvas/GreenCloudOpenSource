package greencloudclient.com.utils.rotation;

import net.minecraft.entity.Entity;

import java.util.ArrayList;
import java.util.List;

public class TargetTracker {

    private static final int HISTORY = 40;
    private static final double MAX_VELOCITY = 0.8;

    private final List<double[]> history = new ArrayList<>();
    private Entity tracked;
    private long acquiredAt;

    public void clear() {
        tracked = null;
        history.clear();
    }

    public void track(Entity entity) {
        if (entity != tracked) {
            tracked = entity;
            history.clear();
            acquiredAt = System.currentTimeMillis();
        }
        history.add(new double[]{entity.posX, entity.posY, entity.posZ});
        if (history.size() > HISTORY) history.remove(0);
    }

    public boolean isReacting(float reactionMs) {
        return tracked != null && System.currentTimeMillis() - acquiredAt < reactionMs;
    }

    public double[] perceive(float reactionMs, float lead) {
        int n = history.size();
        if (n == 0) return null;
        float delay = Math.min(reactionMs / 50f, n - 1);
        double[] seen = sample(n - 1 - delay);
        double[] before = sample(n - 2 - delay);
        double vx = clampVel(seen[0] - before[0]) * lead;
        double vz = clampVel(seen[2] - before[2]) * lead;
        return new double[]{seen[0] + vx * delay, seen[1], seen[2] + vz * delay, vx, vz};
    }

    private double[] sample(float index) {
        int n = history.size();
        index = Math.max(0f, Math.min(index, n - 1));
        int i = (int) Math.floor(index);
        int j = Math.min(i + 1, n - 1);
        float t = index - i;
        double[] a = history.get(i), b = history.get(j);
        return new double[]{a[0] + (b[0] - a[0]) * t, a[1] + (b[1] - a[1]) * t, a[2] + (b[2] - a[2]) * t};
    }

    private static double clampVel(double v) {
        return Math.max(-MAX_VELOCITY, Math.min(MAX_VELOCITY, v));
    }
}
