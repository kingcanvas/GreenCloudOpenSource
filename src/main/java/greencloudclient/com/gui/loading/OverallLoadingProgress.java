package greencloudclient.com.gui.loading;

import net.minecraftforge.fml.common.ProgressManager;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

final class OverallLoadingProgress {

    private static final float FIRST_PHASE_END = 0.82f;
    private static final float LATER_PHASE_SHARE = 0.72f;
    private static final float PRE_FINISH_LIMIT = 0.985f;
    private ProgressManager.ProgressBar rootPhase;
    private float phaseStart;
    private float phaseEnd;
    private float displayed;

    void reset() {
        rootPhase = null;
        phaseStart = 0.0f;
        phaseEnd = FIRST_PHASE_END;
        displayed = 0.0f;
    }

    float update() {
        List<ProgressManager.ProgressBar> bars = activeBars();
        if (bars.isEmpty()) return displayed;

        ProgressManager.ProgressBar currentRoot = bars.get(0);
        if (currentRoot != rootPhase) beginPhase(currentRoot);

        float localProgress = nestedProgress(bars);
        float target = phaseStart + (phaseEnd - phaseStart) * localProgress;
        displayed = Math.max(displayed, Math.min(PRE_FINISH_LIMIT, target));
        return displayed;
    }

    private void beginPhase(ProgressManager.ProgressBar currentRoot) {
        if (rootPhase == null) {
            phaseStart = 0.0f;
            phaseEnd = FIRST_PHASE_END;
        } else {
            phaseStart = displayed;
            phaseEnd = phaseStart + (PRE_FINISH_LIMIT - phaseStart) * LATER_PHASE_SHARE;
        }
        rootPhase = currentRoot;
    }

    private static List<ProgressManager.ProgressBar> activeBars() {
        List<ProgressManager.ProgressBar> bars = new ArrayList<>();
        Iterator<ProgressManager.ProgressBar> iterator = ProgressManager.barIterator();
        while (iterator.hasNext()) bars.add(iterator.next());
        return bars;
    }

    private static float nestedProgress(List<ProgressManager.ProgressBar> bars) {
        float progress = 0.0f;
        for (int i = bars.size() - 1; i >= 0; i--) {
            ProgressManager.ProgressBar bar = bars.get(i);
            if (bar.getSteps() > 0) progress = (bar.getStep() + progress) / bar.getSteps();
        }
        return Math.max(0.0f, Math.min(1.0f, progress));
    }
}
