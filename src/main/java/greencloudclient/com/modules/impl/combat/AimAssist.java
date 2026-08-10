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

public class AimAssist extends Module {
    
    public static AimAssist instance;
    
    private final NumberSetting speed = new NumberSetting("Speed", this, 3.5, 1, 255, 0.1);
    private final NumberSetting fov = new NumberSetting("FOV", this, 30, 90, 1.0, 360, 0.5, true);
    private final NumberSetting range = new NumberSetting("Range", this, 4.5, 1, 8, 0.1);
    private final BooleanSetting requireWeapon = new BooleanSetting("Require Weapon", this, true);
    private final BooleanSetting teammateCheck = new BooleanSetting("Teammate Check", this, true);
    private final BooleanSetting breakBlocks = new BooleanSetting("Break Blocks", this, true);
    
    private boolean hasWarned = false;
    
    public AimAssist() {
        super("AimAssist", Category.COMBAT);
        instance = this;
        addSettings(speed, fov, range, requireWeapon, teammateCheck, breakBlocks);
    }
    
    @Override
    public void onEnable() {
        super.onEnable();
        hasWarned = false;
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
        if (target == null) return;
        
        double targetY = target.posY + target.height * 0.7f;
        
        float[] rots = getRotations(
                target.posX,
                targetY,
                target.posZ
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
        
        ScorePlayerTeam entityTeam =
                mc.theWorld.getScoreboard().getPlayersTeam(entity.getName());
        
        ScorePlayerTeam targetTeam =
                mc.theWorld.getScoreboard().getPlayersTeam(target.getName());
        
        if (entityTeam != null && targetTeam != null) {
            if (entityTeam.isSameTeam(targetTeam)) {
                return true;
            }
            
            if (entityTeam.getChatFormat() != null
                    && targetTeam.getChatFormat() != null
                    && entityTeam.getChatFormat() == targetTeam.getChatFormat()) {
                return true;
            }
            
            String entityPrefix = entityTeam.getColorPrefix();
            String targetPrefix = targetTeam.getColorPrefix();
            
            if (entityPrefix != null && targetPrefix != null) {
                Character entityColor = getColorCode(entityPrefix);
                Character targetColor = getColorCode(targetPrefix);
                
                if (entityColor != null
                        && targetColor != null
                        && entityColor.equals(targetColor)) {
                    return true;
                }
            }
        }
        
        String entityName = getFormattedPlayerName(entity);
        String targetName = getFormattedPlayerName(target);
        
        Character entityColor = getColorCode(entityName);
        Character targetColor = getColorCode(targetName);
        
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
    
    private Character getColorCode(String text) {
        if (text == null || text.length() < 2) {
            return null;
        }
        
        for (int i = 0; i < text.length() - 1; i++) {
            if (text.charAt(i) == '§') {
                char code = Character.toLowerCase(text.charAt(i + 1));
                
                if ((code >= '0' && code <= '9')
                        || (code >= 'a' && code <= 'f')) {
                    return code;
                }
            }
        }
        
        return null;
    }
}
