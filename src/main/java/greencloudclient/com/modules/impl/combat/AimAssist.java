package greencloudclient.com.modules.impl.combat;

import greencloudclient.com.modules.Category;
import greencloudclient.com.modules.Module;
import greencloudclient.com.settings.BooleanSetting;
import greencloudclient.com.settings.ModeSetting;
import greencloudclient.com.settings.MultiModeSetting;
import greencloudclient.com.settings.NumberSetting;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.util.MathHelper;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.Random;
import java.util.List;

public class AimAssist extends Module {
    
    public static AimAssist instance;
    
    private final ModeSetting mode = new ModeSetting("Mode", this, "Normal", "Normal", "Blatant");
    private final NumberSetting speed = new NumberSetting("Speed", this, 5.0, 1.0, 10.0, 0.1, () -> mode.is("Normal"));
    private final NumberSetting fov = new NumberSetting("FOV", this, 30, 90, 1.0, 360, 0.5, true);
    private final NumberSetting range = new NumberSetting("Range", this, 4.5, 1, 8, 0.1);
    private final MultiModeSetting aimingParts = new MultiModeSetting("Aiming Parts", this,
            new String[]{"Head", "Chest", "Stomach", "Legs"}, "Head", "Chest");
    private final BooleanSetting requireWeapon = new BooleanSetting("Require Weapon", this, true);
    private final BooleanSetting teammateCheck = new BooleanSetting("Teammate Check", this, true);
    private final BooleanSetting breakBlocks = new BooleanSetting("Break Blocks", this, true);
    private final BooleanSetting noise = new BooleanSetting("Noise", this, true, () -> mode.is("Normal"));
    private final NumberSetting noiseAmount = new NumberSetting("Noise Amount", this, 0.3, 0.0, 1.0, 0.05,
            () -> mode.is("Normal") && noise.enabled);
    
    private EntityPlayer aimTarget;
    private long lastFrameNanos = -1L;
    private float residualYaw, residualPitch;
    private final Random random = new Random();
    private EntityPlayer noiseTarget;
    private double currentOffsetX;
    private double currentOffsetZ;
    private double currentHeightFactor = 0.7;
    private double targetOffsetX;
    private double targetOffsetZ;
    private double targetHeightFactor = 0.7;
    private String currentAimingPart;
    private long nextAimingPartTime;
    private long noiseStartTime;
    private long noiseSeed;
    
    public AimAssist() {
        super("AimAssist", Category.COMBAT);
        instance = this;
        addSettings(mode, speed, fov, range, aimingParts, requireWeapon, teammateCheck, breakBlocks, noise, noiseAmount);
    }
    
    @Override
    public void onEnable() {
        super.onEnable();
        aimTarget = null;
        lastFrameNanos = -1L;
        residualYaw = residualPitch = 0f;
        resetNoise();
    }
    
    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (mc.thePlayer == null || mc.theWorld == null || mc.currentScreen != null || !canAim()) {
            aimTarget = null;
            return;
        }

        aimTarget = getBestTarget();
        if (aimTarget == null) {
            resetNoise();
            return;
        }
        getAimPoint(aimTarget);
    }

    @SubscribeEvent
    public void onRenderTick(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;
        long now = System.nanoTime();
        float dt = lastFrameNanos < 0 ? 0f : Math.min(0.1f, (now - lastFrameNanos) / 1e9f);
        lastFrameNanos = now;

        if (mc.thePlayer == null || mc.theWorld == null || mc.currentScreen != null) return;
        if (aimTarget == null || aimTarget.isDead || !canAim()) {
            residualYaw = residualPitch = 0f;
            return;
        }

        float pt = event.renderTickTime;
        EntityPlayer t = aimTarget;
        double tx = t.lastTickPosX + (t.posX - t.lastTickPosX) * pt;
        double ty = t.lastTickPosY + (t.posY - t.lastTickPosY) * pt;
        double tz = t.lastTickPosZ + (t.posZ - t.lastTickPosZ) * pt;
        boolean blatant = mode.is("Blatant");
        double[] point = blatant
                ? new double[]{tx, ty + t.height * getAimingPartHeight(), tz}
                : new double[]{tx + currentOffsetX, ty + t.height * currentHeightFactor, tz + currentOffsetZ};

        float[] rots = getRotations(point[0], point[1], point[2], pt);
        float yawDiff = wrapDeg(rots[0] - mc.thePlayer.rotationYaw);
        float pitDiff = rots[1] - mc.thePlayer.rotationPitch;

        float moveYaw, movePitch;
        if (blatant) {
            moveYaw = yawDiff;
            movePitch = pitDiff;
            residualYaw = residualPitch = 0f;
        } else {
            float rate = 2f + (float) speed.getValue() * 2.8f;
            float ease = 1f - (float) Math.exp(-rate * dt);
            moveYaw = yawDiff * ease + residualYaw;
            movePitch = pitDiff * ease + residualPitch;
        }

        float gcd = getGCD();
        float snappedYaw = snapToGCD(moveYaw, yawDiff, gcd);
        float snappedPitch = snapToGCD(movePitch, pitDiff, gcd);
        if (!blatant) {
            residualYaw = MathHelper.clamp_float(moveYaw - snappedYaw, -gcd, gcd);
            residualPitch = MathHelper.clamp_float(movePitch - snappedPitch, -gcd, gcd);
        }
        if (snappedYaw == 0f && snappedPitch == 0f) return;

        mc.thePlayer.setAngles(snappedYaw / 0.15f, -snappedPitch / 0.15f);
    }

    private boolean canAim() {
        if (!mc.gameSettings.keyBindAttack.isKeyDown() || !passesWeaponCheck()) return false;
        if (breakBlocks.enabled) {
            if (mc.playerController.getIsHittingBlock()) return false;
            if (mc.objectMouseOver != null
                    && mc.objectMouseOver.typeOfHit == net.minecraft.util.MovingObjectPosition.MovingObjectType.BLOCK) {
                return false;
            }
        }
        return true;
    }

    private double[] getAimPoint(EntityPlayer target) {
        long now = System.currentTimeMillis();
        boolean targetChanged = target != noiseTarget;
        updateAimingPart(now, targetChanged);
        double partHeight = getAimingPartHeight();

        if (!noise.enabled || noiseAmount.getValue() <= 0.0) {
            noiseTarget = target;
            targetOffsetX = 0.0;
            targetOffsetZ = 0.0;
            targetHeightFactor = partHeight;
            if (targetChanged) {
                currentOffsetX = 0.0;
                currentOffsetZ = 0.0;
                currentHeightFactor = partHeight;
            } else {
                currentOffsetX += (targetOffsetX - currentOffsetX) * 0.22;
                currentOffsetZ += (targetOffsetZ - currentOffsetZ) * 0.22;
                currentHeightFactor += (targetHeightFactor - currentHeightFactor) * 0.22;
            }
            return new double[]{
                    target.posX + currentOffsetX,
                    target.posY + target.height * currentHeightFactor,
                    target.posZ + currentOffsetZ
            };
        }

        noiseTarget = target;
        updateNoisePoint(target);
        if (targetChanged) {
            currentOffsetX = targetOffsetX;
            currentOffsetZ = targetOffsetZ;
            currentHeightFactor = targetHeightFactor;
        }

        double smoothing = 0.22;
        currentOffsetX += (targetOffsetX - currentOffsetX) * smoothing;
        currentOffsetZ += (targetOffsetZ - currentOffsetZ) * smoothing;
        currentHeightFactor += (targetHeightFactor - currentHeightFactor) * smoothing;

        return new double[]{
                target.posX + currentOffsetX,
                target.posY + target.height * currentHeightFactor,
                target.posZ + currentOffsetZ
        };
    }

    private void updateNoisePoint(EntityPlayer target) {
        double amount = noiseAmount.getValue();
        double seconds = Math.max(0.0, (System.nanoTime() - noiseStartTime) / 1_000_000_000.0);
        double dx = target.posX - mc.thePlayer.posX;
        double dz = target.posZ - mc.thePlayer.posZ;
        double horizontalDistance = Math.max(0.0001, Math.sqrt(dx * dx + dz * dz));
        double lateral = fractalNoise(seconds * 1.15, 11L) * target.width * 0.14 * amount;
        double depth = fractalNoise(seconds * 0.82, 37L) * target.width * 0.045 * amount;
        targetOffsetX = -dz / horizontalDistance * lateral + dx / horizontalDistance * depth;
        targetOffsetZ = dx / horizontalDistance * lateral + dz / horizontalDistance * depth;
        targetHeightFactor = getAimingPartHeight() + fractalNoise(seconds * 0.7, 73L) * 0.03 * amount;
    }

    private double fractalNoise(double position, long channel) {
        return sampleNoise(position, channel) * 0.62
                + sampleNoise(position * 2.03, channel + 101L) * 0.28
                + sampleNoise(position * 4.11, channel + 211L) * 0.1;
    }

    private double sampleNoise(double position, long channel) {
        long left = (long) Math.floor(position);
        double blend = position - left;
        blend = blend * blend * (3.0 - 2.0 * blend);
        double first = noiseValue(left, channel);
        double second = noiseValue(left + 1L, channel);
        return first + (second - first) * blend;
    }

    private double noiseValue(long index, long channel) {
        long value = index + noiseSeed + channel * 0x9E3779B97F4A7C15L;
        value = (value ^ (value >>> 30)) * 0xBF58476D1CE4E5B9L;
        value = (value ^ (value >>> 27)) * 0x94D049BB133111EBL;
        value ^= value >>> 31;
        return ((value >>> 11) * 1.1102230246251565E-16) * 2.0 - 1.0;
    }

    private boolean updateAimingPart(long now, boolean targetChanged) {
        List<String> selected = aimingParts.getSelectedModes();
        if (selected.isEmpty()) {
            boolean changed = !"Chest".equals(currentAimingPart);
            currentAimingPart = "Chest";
            nextAimingPartTime = now + 900L;
            return changed;
        }

        int currentIndex = selected.indexOf(currentAimingPart);
        boolean needsSelection = targetChanged || currentIndex < 0 || now >= nextAimingPartTime;
        if (!needsSelection) return false;

        String previous = currentAimingPart;
        if (selected.size() == 1) {
            currentAimingPart = selected.get(0);
        } else if (currentIndex >= 0 && !targetChanged) {
            int nextIndex = random.nextInt(selected.size() - 1);
            if (nextIndex >= currentIndex) nextIndex++;
            currentAimingPart = selected.get(nextIndex);
        } else {
            currentAimingPart = selected.get(random.nextInt(selected.size()));
        }
        nextAimingPartTime = now + 700L + random.nextInt(701);
        return previous == null || !previous.equals(currentAimingPart);
    }

    private double getAimingPartHeight() {
        if ("Head".equals(currentAimingPart)) return 0.88;
        if ("Stomach".equals(currentAimingPart)) return 0.5;
        if ("Legs".equals(currentAimingPart)) return 0.28;
        return 0.68;
    }

    private void resetNoise() {
        noiseTarget = null;
        currentOffsetX = 0.0;
        currentOffsetZ = 0.0;
        currentHeightFactor = 0.7;
        targetOffsetX = 0.0;
        targetOffsetZ = 0.0;
        targetHeightFactor = 0.7;
        currentAimingPart = null;
        nextAimingPartTime = 0L;
        noiseStartTime = System.nanoTime();
        noiseSeed = random.nextLong();
    }
    
    private float getGCD() {
        float f = mc.gameSettings.mouseSensitivity * 0.6F + 0.2F;
        float gcd = f * f * f * 1.2F;
        return gcd > 0 ? gcd : 0.0001f;
    }
    
    private float snapToGCD(float move, float diff, float gcd) {
        float snapped = Math.round(move / gcd) * gcd;
        
        if (diff > 0) {
            return Math.min(snapped, diff);
        }
        
        if (diff < 0) {
            return Math.max(snapped, diff);
        }
        
        return 0f;
    }
    
    private float[] getRotations(double x, double y, double z) {
        return getRotations(x, y, z, 1f);
    }

    private float[] getRotations(double x, double y, double z, float pt) {
        double dx = x - (mc.thePlayer.lastTickPosX + (mc.thePlayer.posX - mc.thePlayer.lastTickPosX) * pt);
        double dy = y - (mc.thePlayer.lastTickPosY + (mc.thePlayer.posY - mc.thePlayer.lastTickPosY) * pt
                + mc.thePlayer.getEyeHeight());
        double dz = z - (mc.thePlayer.lastTickPosZ + (mc.thePlayer.posZ - mc.thePlayer.lastTickPosZ) * pt);
        double dist = Math.sqrt(dx * dx + dz * dz);
        
        return new float[]{
                (float) Math.toDegrees(Math.atan2(dz, dx)) - 90f,
                (float) -Math.toDegrees(Math.atan2(dy, dist))
        };
    }
    
    public EntityPlayer getBestTarget() {
        float rSq = (float) (range.getValue() * range.getValue());
        float halfFov = (float) fov.getValue() / 2f;
        
        EntityPlayer best = null;
        double bestDist = Double.MAX_VALUE;
        
        for (net.minecraft.entity.Entity entity : mc.theWorld.loadedEntityList) {
            if (!(entity instanceof EntityPlayer)) continue;
            
            EntityPlayer p = (EntityPlayer) entity;
            
            if (p == mc.thePlayer || p.isDead || p.isInvisible()) continue;
            
            if (teammateCheck.enabled && isTeam(p, mc.thePlayer)) {
                continue;
            }
            
            double d = mc.thePlayer.getDistanceSqToEntity(p);
            
            if (d < rSq && mc.thePlayer.canEntityBeSeen(p)) {
                float[] r = getRotations(
                        p.posX,
                        p.posY + p.height * 0.5,
                        p.posZ
                );
                
                float yawDiff = Math.abs(
                        wrapDeg(r[0] - mc.thePlayer.rotationYaw)
                );
                
                if (yawDiff <= halfFov && d < bestDist) {
                    bestDist = d;
                    best = p;
                }
            }
        }
        
        return best;
    }
    
    private float wrapDeg(float d) {
        d %= 360f;
        
        if (d >= 180f) d -= 360f;
        if (d < -180f) d += 360f;
        
        return d;
    }
    
    private boolean passesWeaponCheck() {
        if (!requireWeapon.enabled) return true;
        
        net.minecraft.item.ItemStack held = mc.thePlayer.getHeldItem();
        
        return held != null
                && (held.getItem() instanceof net.minecraft.item.ItemSword
                || held.getItem() instanceof net.minecraft.item.ItemAxe);
    }
    
    private boolean isTeam(EntityPlayer entity, EntityPlayer target) {
        if (entity == null || target == null) return false;

        if (target.isOnSameTeam(entity) || entity.isOnSameTeam(target)) return true;
        
        ScorePlayerTeam entityTeam =
                mc.theWorld.getScoreboard().getPlayersTeam(entity.getName());
        
        ScorePlayerTeam targetTeam =
                mc.theWorld.getScoreboard().getPlayersTeam(target.getName());

        if (entityTeam != null && targetTeam != null) {
            Character entityTeamColor = getLastColorCode(entityTeam.getColorPrefix());
            Character targetTeamColor = getLastColorCode(targetTeam.getColorPrefix());
            return entityTeamColor != null && entityTeamColor.equals(targetTeamColor);
        }

        if (entityTeam != null || targetTeam != null) return false;
        
        String entityName = getFormattedPlayerName(entity);
        String targetName = getFormattedPlayerName(target);
        
        Character entityColor = getLastColorCode(entityName);
        Character targetColor = getLastColorCode(targetName);
        
        if (entityColor != null
                && targetColor != null
                && entityColor.equals(targetColor)) {
            return true;
        }
        
        return false;
    }
    
    private String getFormattedPlayerName(EntityPlayer player) {
        if (player.getDisplayName() != null) {
            return player.getDisplayName().getFormattedText();
        }
        
        return player.getName();
    }
    
    private Character getLastColorCode(String text) {
        if (text == null || text.length() < 2) {
            return null;
        }

        Character color = null;
        for (int i = 0; i < text.length() - 1; i++) {
            if (text.charAt(i) == '§') {
                char code = Character.toLowerCase(text.charAt(i + 1));
                
                if ((code >= '0' && code <= '9')
                        || (code >= 'a' && code <= 'f')) {
                    color = code;
                }
            }
        }

        return color;
    }
}
