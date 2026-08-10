package greencloudclient.com.modules.impl.combat;

import greencloudclient.com.managers.notification.NotificationManager;
import greencloudclient.com.modules.Category;
import greencloudclient.com.modules.Module;
import greencloudclient.com.settings.BooleanSetting;
import greencloudclient.com.settings.NumberSetting;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.util.MathHelper;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.Random;

public class AimAssist extends Module {
    
    public static AimAssist instance;
    
    private final NumberSetting speed = new NumberSetting("Speed", this, 3.5, 1, 255, 0.1);
    private final NumberSetting fov = new NumberSetting("FOV", this, 30, 90, 1.0, 360, 0.5, true);
    private final NumberSetting range = new NumberSetting("Range", this, 4.5, 1, 8, 0.1);
    private final BooleanSetting requireWeapon = new BooleanSetting("Require Weapon", this, true);
    private final BooleanSetting teammateCheck = new BooleanSetting("Teammate Check", this, true);
    private final BooleanSetting breakBlocks = new BooleanSetting("Break Blocks", this, true);
    private final BooleanSetting randomization = new BooleanSetting("Randomization", this, true);
    private final NumberSetting randomizationAmount = new NumberSetting("Randomization Amount", this, 0.3, 0.0, 1.0, 0.05, () -> randomization.enabled);
    
    private boolean hasWarned = false;
    private final Random random = new Random();
    private EntityPlayer randomizedTarget;
    private double currentOffsetX;
    private double currentOffsetZ;
    private double currentHeightFactor = 0.7;
    private double targetOffsetX;
    private double targetOffsetZ;
    private double targetHeightFactor = 0.7;
    private long nextRandomizationTime;
    
    public AimAssist() {
        super("AimAssist", Category.COMBAT);
        instance = this;
        addSettings(speed, fov, range, requireWeapon, teammateCheck, breakBlocks, randomization, randomizationAmount);
    }
    
    @Override
    public void onEnable() {
        super.onEnable();
        hasWarned = false;
        resetRandomization();
        checkSpeedWarning();
    }
    
    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null || mc.currentScreen != null) return;
        
        checkSpeedWarning();
        
        if (event.phase == TickEvent.Phase.END) tickNormal();
    }
    
    private void checkSpeedWarning() {
        if (speed.getValue() > 20 && !hasWarned) {
            NotificationManager.getInstance().addNotification(
                    "AimAssist",
                    "higher then 20 speed we fr.",
                    NotificationManager.NotificationType.WARNING,
                    2500
            );
            hasWarned = true;
        } else if (speed.getValue() <= 20 && hasWarned) {
            hasWarned = false;
        }
    }
    
    private void tickNormal() {
        if (!mc.gameSettings.keyBindAttack.isKeyDown() || !passesWeaponCheck()) return;
        
        if (breakBlocks.enabled) {
            if (mc.playerController.getIsHittingBlock()) return;
            
            if (mc.objectMouseOver != null
                    && mc.objectMouseOver.typeOfHit == net.minecraft.util.MovingObjectPosition.MovingObjectType.BLOCK) {
                return;
            }
        }
        
        EntityPlayer target = getBestTarget();
        if (target == null) {
            resetRandomization();
            return;
        }

        double[] aimPoint = getAimPoint(target);
        
        float[] rots = getRotations(
                aimPoint[0],
                aimPoint[1],
                aimPoint[2]
        );
        
        float yawDiff = wrapDeg(rots[0] - mc.thePlayer.rotationYaw);
        float pitDiff = rots[1] - mc.thePlayer.rotationPitch;
        
        float gcd = getGCD();
        
        if (Math.abs(yawDiff) < gcd * 0.5f
                && Math.abs(pitDiff) < gcd * 0.5f) {
            return;
        }
        
        float spd = (float) speed.getValue();
        float distFactor = MathHelper.clamp_float(
                Math.abs(yawDiff) / 20f,
                0.2f,
                1.0f
        );
        
        float baseMove = spd * distFactor;
        
        float moveYaw = MathHelper.clamp_float(
                yawDiff,
                -baseMove,
                baseMove
        );
        
        float movePitch = MathHelper.clamp_float(
                pitDiff,
                -baseMove,
                baseMove
        );
        
        mc.thePlayer.rotationYaw += snapToGCD(
                moveYaw,
                yawDiff,
                gcd
        );
        
        mc.thePlayer.rotationPitch += snapToGCD(
                movePitch,
                pitDiff,
                gcd
        );
    }

    private double[] getAimPoint(EntityPlayer target) {
        if (!randomization.enabled || randomizationAmount.getValue() <= 0.0) {
            randomizedTarget = null;
            return new double[]{target.posX, target.posY + target.height * 0.7, target.posZ};
        }

        long now = System.currentTimeMillis();
        if (target != randomizedTarget) {
            randomizedTarget = target;
            chooseRandomizedPoint(target, now);
            currentOffsetX = targetOffsetX;
            currentOffsetZ = targetOffsetZ;
            currentHeightFactor = targetHeightFactor;
        } else if (now >= nextRandomizationTime) {
            chooseRandomizedPoint(target, now);
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

    private void chooseRandomizedPoint(EntityPlayer target, long now) {
        double amount = randomizationAmount.getValue();
        double dx = target.posX - mc.thePlayer.posX;
        double dz = target.posZ - mc.thePlayer.posZ;
        double horizontalDistance = Math.max(0.0001, Math.sqrt(dx * dx + dz * dz));
        double lateral = (random.nextDouble() * 2.0 - 1.0) * target.width * 0.18 * amount;
        double depth = (random.nextDouble() * 2.0 - 1.0) * target.width * 0.05 * amount;
        targetOffsetX = -dz / horizontalDistance * lateral + dx / horizontalDistance * depth;
        targetOffsetZ = dx / horizontalDistance * lateral + dz / horizontalDistance * depth;
        targetHeightFactor = 0.68 + (random.nextDouble() * 2.0 - 1.0) * 0.04 * amount;
        nextRandomizationTime = now + 120L + random.nextInt(161);
    }

    private void resetRandomization() {
        randomizedTarget = null;
        currentOffsetX = 0.0;
        currentOffsetZ = 0.0;
        currentHeightFactor = 0.7;
        targetOffsetX = 0.0;
        targetOffsetZ = 0.0;
        targetHeightFactor = 0.7;
        nextRandomizationTime = 0L;
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
        double dx = x - mc.thePlayer.posX;
        double dy = y - (mc.thePlayer.posY + mc.thePlayer.getEyeHeight());
        double dz = z - mc.thePlayer.posZ;
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
