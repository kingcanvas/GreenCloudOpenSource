package greencloud.impl.modules.combat;

import greencloud.impl.modules.Category;
import greencloud.impl.modules.Module;
import greencloud.impl.settings.BooleanSetting;
import greencloud.impl.settings.NumberSetting;
import net.minecraft.block.BlockAir;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Mouse;

public class AimAssist extends Module {

    public static AimAssist instance;

    private final NumberSetting  speed         = new NumberSetting ("Speed",          this, 3.5, 1, 10, 0.1);
    private final NumberSetting fov         = new NumberSetting("FOV", this, 30, 90, 1.0, 120, 0.5, true);
    private final NumberSetting  range         = new NumberSetting ("Range",          this, 4.5, 1, 8, 0.1);
    private final BooleanSetting requireWeapon = new BooleanSetting("Require Weapon", this, true);
    private final BooleanSetting teammateCheck = new BooleanSetting("Teammate Check", this, true);

    public AimAssist() {
        super("AimAssist", "Smoothly pulls your crosshair to targets.", Category.COMBAT);
        instance = this;
        addSettings(speed, fov, range, requireWeapon, teammateCheck);
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null || mc.currentScreen != null) return;
        if (event.phase == TickEvent.Phase.END) tickNormal();
    }

    private void tickNormal() {
        if (!Mouse.isButtonDown(0) || !passesWeaponCheck()) return;

        EntityPlayer target = getBestTarget();
        if (target == null) return;

        double targetY = target.posY + (target.height * 0.7f);
        float[] rots    = getRotations(target.posX, targetY, target.posZ);
        float   yawDiff = wrapDeg(rots[0] - mc.thePlayer.rotationYaw);
        float   pitDiff = rots[1] - mc.thePlayer.rotationPitch;

        float gcd = getGCD();
        // Dead-zone: skip if already within sub-GCD resolution to prevent oscillation
        if (Math.abs(yawDiff) < gcd * 0.5f && Math.abs(pitDiff) < gcd * 0.5f) return;

        float spd = (float) speed.getValue();
        float distFactor = MathHelper.clamp_float(Math.abs(yawDiff) / 20f, 0.2f, 1.0f);
        float baseMove = spd * distFactor;

        float moveYaw   = MathHelper.clamp_float(yawDiff, -baseMove, baseMove);
        float movePitch = MathHelper.clamp_float(pitDiff, -baseMove, baseMove);

        mc.thePlayer.rotationYaw   += snapToGCD(moveYaw, yawDiff, gcd);
        mc.thePlayer.rotationPitch += snapToGCD(movePitch, pitDiff, gcd);
    }

    private float getGCD() {
        float f = mc.gameSettings.mouseSensitivity * 0.6F + 0.2F;
        float gcd = f * f * f * 1.2F;
        return gcd > 0 ? gcd : 0.0001f;
    }

    private float snapToGCD(float move, float diff, float gcd) {
        float snapped = Math.round(move / gcd) * gcd;
        // Never overshoot past the actual angle difference
        if (diff > 0) return Math.min(snapped, diff);
        if (diff < 0) return Math.max(snapped, diff);
        return 0f;
    }

    private float[] getRotations(double x, double y, double z) {
        double dx   = x - mc.thePlayer.posX;
        double dy   = y - (mc.thePlayer.posY + mc.thePlayer.getEyeHeight());
        double dz   = z - mc.thePlayer.posZ;
        double dist = Math.sqrt(dx * dx + dz * dz);
        return new float[]{
                (float) Math.toDegrees(Math.atan2(dz, dx)) - 90f,
                (float) -Math.toDegrees(Math.atan2(dy, dist))
        };
    }

    public EntityPlayer getBestTarget() {
        float        rSq      = (float)(range.getValue() * range.getValue());
        float        halfFov  = (float) fov.getValue() / 2f;
        EntityPlayer best     = null;
        double       bestDist = Double.MAX_VALUE;

        for (net.minecraft.entity.Entity entity : mc.theWorld.loadedEntityList) {
            if (!(entity instanceof EntityPlayer)) continue;
            EntityPlayer p = (EntityPlayer) entity;
            if (p == mc.thePlayer || p.isDead || p.isInvisible()) continue;
            if (teammateCheck.enabled && isTeam(p)) continue;

            double d = mc.thePlayer.getDistanceSqToEntity(p);
            if (d < rSq && mc.thePlayer.canEntityBeSeen(p)) {
                float[] r       = getRotations(p.posX, p.posY + p.height * 0.5, p.posZ);
                float   yawDiff = Math.abs(wrapDeg(r[0] - mc.thePlayer.rotationYaw));
                if (yawDiff <= halfFov && d < bestDist) {
                    bestDist = d;
                    best     = p;
                }
            }
        }
        return best;
    }

    private float wrapDeg(float d) {
        d %= 360f;
        if (d >= 180f)  d -= 360f;
        if (d < -180f)  d += 360f;
        return d;
    }

    private boolean passesWeaponCheck() {
        if (!requireWeapon.enabled) return true;
        net.minecraft.item.ItemStack held = mc.thePlayer.getHeldItem();
        return held != null && (held.getItem() instanceof net.minecraft.item.ItemSword
                || held.getItem() instanceof net.minecraft.item.ItemAxe);
    }

    private boolean isTeam(EntityPlayer entity) {
        String targetName = entity.getDisplayName().getFormattedText().replace("§r", "");
        String clientName = mc.thePlayer.getDisplayName().getFormattedText().replace("§r", "");
        if (targetName.startsWith("§") && clientName.startsWith("§")) {
            return targetName.charAt(1) == clientName.charAt(1);
        }
        return false;
    }
}