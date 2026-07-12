package greencloud.impl.modules.combat;

import greencloud.impl.modules.Category;
import greencloud.impl.modules.Module;
import greencloud.impl.settings.BooleanSetting;
import greencloud.impl.settings.NumberSetting;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemAxe;
import net.minecraft.item.ItemSword;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import greencloud.impl.settings.ModeSetting;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemAxe;
import net.minecraft.item.ItemSword;
import net.minecraft.network.play.client.C02PacketUseEntity;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.List;

public class Reach extends Module {
    private final NumberSetting reach = new NumberSetting("Reach", this, 3.1, 3.3, 3.0, 6.0, 0.05, true);
    private final BooleanSetting weaponOnly = new BooleanSetting("Weapon Only", this, true);
    private final BooleanSetting movingOnly = new BooleanSetting("Moving Only", this, false);
    private final BooleanSetting sprintOnly = new BooleanSetting("Sprint Only", this, false);

    public Reach() {
        super("Reach", "Gives you more Combat Reach.", Category.COMBAT);
        this.addSettings(reach, weaponOnly, movingOnly, sprintOnly);
    }

    @SubscribeEvent
    public void onMouse(MouseEvent event) {
        if (event.button == 0 && event.buttonstate && mc.thePlayer != null && mc.theWorld != null) {
            if (!validateConditions()) return;

            double currentReach = reach.getValue() + (Math.random() * (reach.maxValue - reach.getValue()));
            Object[] result = getEntity(currentReach, 0.0);
            if (result != null) {
                Entity target = (Entity) result[0];
                Vec3 hitVec = (Vec3) result[1];

                mc.objectMouseOver = new MovingObjectPosition(target, hitVec);
                mc.pointedEntity = target;
            }
        }
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

    private Object[] getEntity(double reachDist, double expand) {
        Entity renderViewEntity = mc.getRenderViewEntity();
        if (renderViewEntity == null) return null;

        Entity target = null;
        Vec3 hitVec = null;
        Vec3 eyePos = renderViewEntity.getPositionEyes(1.0f);
        Vec3 lookVec = renderViewEntity.getLook(1.0f);
        Vec3 reachVector = eyePos.addVector(lookVec.xCoord * reachDist, lookVec.yCoord * reachDist, lookVec.zCoord * reachDist);

        AxisAlignedBB searchBox = renderViewEntity.getEntityBoundingBox()
                .addCoord(lookVec.xCoord * reachDist, lookVec.yCoord * reachDist, lookVec.zCoord * reachDist)
                .expand(1.0, 1.0, 1.0);

        List<Entity> entityList = mc.theWorld.getEntitiesWithinAABBExcludingEntity(renderViewEntity, searchBox);
        double closestDistance = reachDist;

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