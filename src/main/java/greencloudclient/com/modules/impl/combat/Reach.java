package greencloudclient.com.modules.impl.combat;

import greencloudclient.com.modules.Category;
import greencloudclient.com.modules.Module;
import greencloudclient.com.settings.BooleanSetting;
import greencloudclient.com.settings.NumberSetting;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemAxe;
import net.minecraft.item.ItemSword;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import net.minecraftforge.client.event.MouseEvent;

import java.util.List;
import java.util.Random;

public class Reach extends Module {
    private final NumberSetting reach = new NumberSetting("Reach", this, 3.1, 3.3, 3.0, 6.0, 0.05, true);
    private final BooleanSetting weaponOnly = new BooleanSetting("Weapon Only", this, true);
    private final BooleanSetting movingOnly = new BooleanSetting("Moving Only", this, false);
    private final BooleanSetting sprintOnly = new BooleanSetting("Sprint Only", this, false);
    private final Random random = new Random();
    private double effectiveReach;
    private long nextReachUpdate;

    public Reach() {
        super("Reach", Category.COMBAT);
        this.addSettings(reach, weaponOnly, movingOnly, sprintOnly);
    }

    @Override
    public void onEnable() {
        effectiveReach = reach.getValue();
        nextReachUpdate = 0L;
        super.onEnable();
    }

    @SubscribeEvent
    public void onMouse(MouseEvent event) {
        if (event.button == 0 && event.buttonstate && mc.thePlayer != null && mc.theWorld != null) {
            updateMouseOver(1.0f);
        }
    }

    public void updateMouseOver(float partialTicks) {
        if (mc.thePlayer == null || mc.theWorld == null || !validateConditions()) return;

        Object[] result = getEntity(getEffectiveReach(), 0.0, partialTicks);
        if (result == null) return;

        Entity target = (Entity) result[0];
        Vec3 hitVec = (Vec3) result[1];
        mc.objectMouseOver = new MovingObjectPosition(target, hitVec);
        mc.pointedEntity = target;
    }

    private double getEffectiveReach() {
        long now = System.currentTimeMillis();
        if (now >= nextReachUpdate) {
            double minimum = Math.min(reach.getValue(), reach.maxValue);
            double maximum = Math.max(reach.getValue(), reach.maxValue);
            effectiveReach = minimum + random.nextDouble() * (maximum - minimum);
            nextReachUpdate = now + 180L + random.nextInt(181);
        }
        return effectiveReach;
    }

    private boolean validateConditions() {
        if (mc.currentScreen != null) return false;

        if (weaponOnly.enabled) {
            if (mc.thePlayer.getHeldItem() == null) return false;
            boolean holdingWeapon = mc.thePlayer.getHeldItem().getItem() instanceof ItemSword ||
                    mc.thePlayer.getHeldItem().getItem() instanceof ItemAxe;
            if (!holdingWeapon) return false;
        }

        if (movingOnly.enabled && mc.thePlayer.moveForward == 0.0f && mc.thePlayer.moveStrafing == 0.0f) {
            return false;
        }

        if (sprintOnly.enabled && !mc.thePlayer.isSprinting()) {
            return false;
        }

        return true;
    }

    private Object[] getEntity(double reachDist, double expand, float partialTicks) {
        Entity renderViewEntity = mc.getRenderViewEntity();
        if (renderViewEntity == null) return null;

        Entity target = null;
        Vec3 hitVec = null;
        Vec3 eyePos = renderViewEntity.getPositionEyes(partialTicks);
        Vec3 lookVec = renderViewEntity.getLook(partialTicks);
        Vec3 reachVector = eyePos.addVector(lookVec.xCoord * reachDist, lookVec.yCoord * reachDist, lookVec.zCoord * reachDist);

        AxisAlignedBB searchBox = renderViewEntity.getEntityBoundingBox()
                .addCoord(lookVec.xCoord * reachDist, lookVec.yCoord * reachDist, lookVec.zCoord * reachDist)
                .expand(1.0, 1.0, 1.0);

        List<Entity> entityList = mc.theWorld.getEntitiesWithinAABBExcludingEntity(renderViewEntity, searchBox);
        double closestDistance = reachDist;
        if (mc.objectMouseOver != null
                && mc.objectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK
                && mc.objectMouseOver.hitVec != null) {
            closestDistance = Math.min(closestDistance, eyePos.distanceTo(mc.objectMouseOver.hitVec));
        }

        for (Entity entity : entityList) {
            if (!entity.canBeCollidedWith() || !(entity instanceof EntityLivingBase)) continue;

            float collisionSize = entity.getCollisionBorderSize();
            AxisAlignedBB boundingBox = entity.getEntityBoundingBox().expand(collisionSize, collisionSize, collisionSize).expand(expand, expand, expand);
            MovingObjectPosition intercept = boundingBox.calculateIntercept(eyePos, reachVector);

            if (boundingBox.isVecInside(eyePos)) {
                if (closestDistance >= 0.0) {
                    target = entity;
                    hitVec = (intercept == null) ? eyePos : intercept.hitVec;
                    closestDistance = 0.0;
                }
            } else if (intercept != null) {
                double distanceToHit = eyePos.distanceTo(intercept.hitVec);
                if (distanceToHit < closestDistance || closestDistance == 0.0) {
                    target = entity;
                    hitVec = intercept.hitVec;
                    closestDistance = distanceToHit;
                }
            }
        }

        if (target != null) {
            return new Object[]{target, hitVec};
        }

        return null;
    }
}
