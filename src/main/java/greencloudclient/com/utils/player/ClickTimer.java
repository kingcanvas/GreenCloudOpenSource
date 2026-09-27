package greencloudclient.com.utils.player;

import java.util.Random;

public class ClickTimer {

    public static final String[] MODES = {"Normal", "Extra", "Extra+"};

    private final Random random = new Random();
    private double speedFactor = 1.0;
    private int clicksInCurrentStreak = 0;
    private int streakTarget = 10;

    public void reset() {
        speedFactor = 1.0;
        clicksInCurrentStreak = 0;
        streakTarget = 5 + random.nextInt(10);
    }

    public long nextDelay(double min, double max, String mode) {
        boolean extra = "Extra".equals(mode);
        boolean extraPlus = "Extra+".equals(mode);
        double targetCPS = min + random.nextDouble() * (max - min);

        if (extra || extraPlus) {
            double outlierRoll = random.nextDouble();
            double outlierChance = extraPlus ? 0.08 : 0.04;
            if (outlierRoll < outlierChance) {
                return 10 + random.nextInt(25);
            } else if (outlierRoll < outlierChance + 0.03) {
                return 220 + random.nextInt(150);
            }

            if (clicksInCurrentStreak >= streakTarget) {
                clicksInCurrentStreak = 0;
                streakTarget = 5 + random.nextInt(10);

                double roll = random.nextDouble();
                if (extraPlus) {
                    if (roll < 0.30) {
                        speedFactor = 0.50 + random.nextDouble() * 0.30;
                    } else if (roll < 0.60) {
                        speedFactor = 1.10 + random.nextDouble() * 0.20;
                    } else {
                        speedFactor = 0.80 + random.nextDouble() * 0.40;
                    }
                } else {
                    if (roll < 0.20) {
                        speedFactor = 0.65 + random.nextDouble() * 0.20;
                    } else if (roll < 0.40) {
                        speedFactor = 1.05 + random.nextDouble() * 0.15;
                    } else {
                        speedFactor = 0.85 + random.nextDouble() * 0.20;
                    }
                }
            }

            clicksInCurrentStreak++;
            targetCPS *= speedFactor;
        }

        targetCPS = Math.max(2.0, Math.min(targetCPS, max + 3.0));

        long delay = (long) (1000.0 / targetCPS);

        double gaussianMod = extraPlus ? 6.0 : (extra ? 3.0 : 0.0);
        if (gaussianMod > 0) {
            delay += (long) (random.nextGaussian() * gaussianMod);
        }

        return Math.max(10, delay);
    }
}
