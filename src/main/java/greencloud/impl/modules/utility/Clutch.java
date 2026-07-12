package greencloud.impl.modules.utility;

import greencloud.impl.modules.Category;
import greencloud.impl.modules.Module;
import greencloud.impl.settings.BooleanSetting;
import greencloud.impl.settings.NumberSetting;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.MovementInput;
import net.minecraft.util.Vec3;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.Random;

public class Clutch extends Module {

    private final NumberSetting scanRadius = new NumberSetting("Scan Radius", this, 4.0, 1.0, 6.0, 1.0);
    private final NumberSetting reachDist = new NumberSetting("Reach", this, 5.0, 3.0, 6.0, 0.1);
    private final NumberSetting rotSpeed = new NumberSetting("Rotation Speed", this, 80.0, 10.0, 90.0, 1.0);
    private final BooleanSetting movementFix = new BooleanSetting("Movement Fix", this, true);
    private final BooleanSetting silentAim = new BooleanSetting("Silent Aim", this, false);
    private final BooleanSetting voidOnly = new BooleanSetting("Void Only", this, false);
    private final BooleanSetting autoDisable = new BooleanSetting("Auto Disable", this, false);

    private static final EnumFacing[] FACES = {
            EnumFacing.DOWN, EnumFacing.NORTH, EnumFacing.SOUTH, EnumFacing.EAST, EnumFacing.WEST
    };

    private static final double LEAD_TICKS = 1.0;
    private static final int PREDICT_TICKS = 14;

    private final Random rng = new Random();

    private float serverYaw, serverPitch, prevServerYaw, prevServerPitch;
    private float naturalYaw, naturalPitch;
    private boolean silentAimCamActive;
    private int aimTicks;
    private boolean engaged;

    private float preclutchYaw, preclutchPitch, preclutchPrevYaw, preclutchPrevPitch;

    private MovementInput originalInput;

    private int cooldown = 0;
    private boolean pendingDisable = false;
    private float lastPlaceYaw, lastPlacePitch;
    private int resetTimer = 0;

    public Clutch() {
        super("Clutch", "Predicts fatal falls and bridges onto the most optimal block to save you", Category.UTILITY);
        addSettings(scanRadius, reachDist, rotSpeed, movementFix, silentAim, voidOnly, autoDisable);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        cooldown = 0;
        aimTicks = 0;
        engaged = false;
        pendingDisable = false;
        lastPlaceYaw = 0f;
        lastPlacePitch = 0f;
        resetTimer = 0;

        if (mc.thePlayer != null) {
            serverYaw = mc.thePlayer.rotationYaw;
            serverPitch = mc.thePlayer.rotationPitch;
            prevServerYaw = serverYaw;
            prevServerPitch = serverPitch;
            naturalYaw = mc.thePlayer.rotationYaw;
            naturalPitch = mc.thePlayer.rotationPitch;
            silentAimCamActive = false;
            preclutchYaw = mc.thePlayer.rotationYaw;
            preclutchPitch = mc.thePlayer.rotationPitch;
            preclutchPrevYaw = mc.thePlayer.prevRotationYaw;
            preclutchPrevPitch = mc.thePlayer.prevRotationPitch;

            MovementInput cur = mc.thePlayer.movementInput;
            originalInput = (cur instanceof ClutchMovementInput) ? ((ClutchMovementInput) cur).parent : cur;
            mc.thePlayer.movementInput = new ClutchMovementInput(originalInput);
        }
    }

    @Override
    public void onDisable() {
        if (mc.thePlayer != null && engaged) {
            if (silentAim.enabled && silentAimCamActive) {
                mc.thePlayer.rotationYaw = naturalYaw;
                mc.thePlayer.rotationPitch = naturalPitch;
                mc.thePlayer.prevRotationYaw = naturalYaw;
                mc.thePlayer.prevRotationPitch = naturalPitch;
            } else if (!silentAim.enabled) {
                mc.thePlayer.rotationYaw = preclutchYaw;
                mc.thePlayer.rotationPitch = preclutchPitch;
                mc.thePlayer.prevRotationYaw = preclutchPrevYaw;
                mc.thePlayer.prevRotationPitch = preclutchPrevPitch;
            }
        }
        engaged = false;
        aimTicks = 0;
        resetTimer = 0;
        silentAimCamActive = false;
        if (mc.thePlayer != null && originalInput != null) {
            mc.thePlayer.movementInput = originalInput;
        }
        super.onDisable();
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null) return;

        if (event.phase == TickEvent.Phase.START) {
            prevServerYaw = serverYaw;
            prevServerPitch = serverPitch;

            if (engaged && silentAim.enabled) {
                if (silentAimCamActive) {
                    float dYaw = MathHelper.wrapAngleTo180_float(mc.thePlayer.rotationYaw - serverYaw);
                    naturalYaw += dYaw;
                    naturalPitch = MathHelper.clamp_float(
                            naturalPitch + (mc.thePlayer.rotationPitch - serverPitch), -90f, 90f);
                }
                mc.thePlayer.rotationYaw = naturalYaw;
                mc.thePlayer.rotationPitch = naturalPitch;
                mc.thePlayer.prevRotationYaw = naturalYaw;
                mc.thePlayer.prevRotationPitch = naturalPitch;
            } else if (engaged) {
                mc.thePlayer.rotationYaw = serverYaw;
                mc.thePlayer.rotationPitch = serverPitch;
            }

            tick();

            if (engaged && !silentAim.enabled) {
                mc.thePlayer.rotationYaw = serverYaw;
                mc.thePlayer.rotationPitch = serverPitch;
            }
        } else if (event.phase == TickEvent.Phase.END) {
            if (engaged) {
                if (silentAim.enabled) {
                    naturalYaw = mc.thePlayer.rotationYaw;
                    naturalPitch = mc.thePlayer.rotationPitch;
                    mc.thePlayer.rotationYaw = serverYaw;
                    mc.thePlayer.rotationPitch = serverPitch;
                    mc.thePlayer.prevRotationYaw = prevServerYaw;
                    mc.thePlayer.prevRotationPitch = prevServerPitch;
                    silentAimCamActive = true;
                } else {
                    mc.thePlayer.rotationYaw = serverYaw;
                    mc.thePlayer.rotationPitch = serverPitch;
                    mc.thePlayer.prevRotationYaw = prevServerYaw;
                    mc.thePlayer.prevRotationPitch = prevServerPitch;
                    silentAimCamActive = false;
                }
            } else {
                silentAimCamActive = false;
            }
        }
    }

    private void tick() {
        if (pendingDisable) {
            resetTimer = 0;
            engaged = false;
            idle();
            pendingDisable = false;
            toggle();
            return;
        }

        if (mc.thePlayer.onGround || mc.thePlayer.capabilities.isFlying
                || mc.thePlayer.isOnLadder()
                || mc.thePlayer.isInWater() || mc.thePlayer.isInLava()
                || findBlockSlot() == -1) {
            engaged = false;
            idle();
            return;
        }

        if (!engaged) {
            Prediction p = predict(PREDICT_TICKS);
            if (!p.danger || (voidOnly.enabled && !p.intoVoid)) {
                idle();
                return;
            }
            preclutchYaw = mc.thePlayer.rotationYaw;
            preclutchPitch = mc.thePlayer.rotationPitch;
            preclutchPrevYaw = mc.thePlayer.prevRotationYaw;
            preclutchPrevPitch = mc.thePlayer.prevRotationPitch;
            engaged = true;
            resetTimer = 0;
        }

        if (cooldown > 0) cooldown--;

        if (hasFootingBelow()) {
            engaged = false;
            idle();
            return;
        }

        PlaceTarget target = findOptimalPlacement((int) scanRadius.getValue());
        if (target == null) {
            idle();
            return;
        }

        float[] want = rotationsTo(target.hitVec);

        aimTicks++;
        float ramp = Math.min(1.0f, aimTicks / 5.0f);
        float speed = Math.max(3f, (float) rotSpeed.getValue() * ramp * (0.85f + rng.nextFloat() * 0.30f));
        boolean converged = stepRotation(want[0], want[1], speed);

        if (cooldown <= 0 && converged) {
            if (rng.nextInt(3) == 0) {
                float gcd = gcdStep();
                serverYaw += (rng.nextBoolean() ? 1 : -1) * gcd;
                serverPitch = MathHelper.clamp_float(serverPitch + (rng.nextBoolean() ? 0.5f : -0.5f) * gcd, -90f, 90f);
            }
            PlaceTarget pl = resolvePlacement(target, serverYaw, serverPitch);
            if (pl != null) sendPlace(pl);
        }
    }

    private void idle() {
        aimTicks = 0;
        resetTimer = 0;
        serverYaw = mc.thePlayer.rotationYaw;
        serverPitch = mc.thePlayer.rotationPitch;
        naturalYaw = mc.thePlayer.rotationYaw;
        naturalPitch = mc.thePlayer.rotationPitch;
    }

    private Prediction predict(int ticks) {
        double px = mc.thePlayer.posX, py = mc.thePlayer.posY, pz = mc.thePlayer.posZ;
        double mx = mc.thePlayer.motionX, my = mc.thePlayer.motionY, mz = mc.thePlayer.motionZ;

        Prediction p = new Prediction();
        for (int i = 0; i < ticks; i++) {
            my = (my - 0.08) * 0.98;
            py += my; px += mx; pz += mz;
            mx *= 0.91; mz *= 0.91;

            BlockPos feet = new BlockPos(px, py - 0.1, pz);
            if (isSolid(feet) || isSolid(feet.down())) return p;
        }
        p.danger = my < 0;

        p.intoVoid = true;
        if (p.danger) {
            int startY = MathHelper.floor_double(py);
            int blockX = MathHelper.floor_double(px);
            int blockZ = MathHelper.floor_double(pz);
            for (int y = startY; y >= 0; y--) {
                if (isSolid(new BlockPos(blockX, y, blockZ))) { p.intoVoid = false; break; }
            }
        }
        return p;
    }

    private PlaceTarget findOptimalPlacement(int radius) {
        final double reach = reachDist.getValue();
        final Vec3 eyes = mc.thePlayer.getPositionEyes(1.0f);
        final double[] lead = leadPosition(LEAD_TICKS);
        final double px = lead[0], pz = lead[2];
        final int baseX = MathHelper.floor_double(px);
        final int baseZ = MathHelper.floor_double(pz);
        final int feetY = MathHelper.floor_double(lead[1] - 0.1);
        final double catchCap = 0.8;

        PlaceTarget best = null;
        double bestScore = -Double.MAX_VALUE;

        for (int dy = 0; dy >= -2; dy--) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    BlockPos cell = new BlockPos(baseX + dx, feetY + dy, baseZ + dz);
                    if (!isReplaceable(cell)) continue;

                    double offX = Math.abs((cell.getX() + 0.5) - px);
                    double offZ = Math.abs((cell.getZ() + 0.5) - pz);
                    double horiz = Math.hypot((cell.getX() + 0.5) - px, (cell.getZ() + 0.5) - pz);
                    boolean catches = offX < catchCap && offZ < catchCap;

                    if (!catches && horiz > 3.0) continue;
                    if (!catches && !isReplaceable(cell.up())) continue;

                    for (EnumFacing face : FACES) {
                        BlockPos support = cell.offset(face);
                        if (!isSolid(support)) continue;

                        EnumFacing clickFace = face.getOpposite();
                        if (!isReplaceable(support.offset(clickFace))) continue;

                        Vec3 hit = closestPointOnFace(support, clickFace);
                        double dist = eyes.distanceTo(hit);
                        if (dist > reach) continue;

                        double score = (catches ? 1000.0 : 0.0)
                                - horiz * 10.0
                                - Math.abs(dy) * 3.0
                                + (face == EnumFacing.DOWN ? 0.5 : 0.0)
                                + (reach - dist) * 0.1;

                        if (score > bestScore) {
                            bestScore = score;
                            best = new PlaceTarget(support, clickFace, hit);
                        }
                    }
                }
            }
        }
        return best;
    }

    private PlaceTarget resolvePlacement(PlaceTarget target, float yaw, float pitch) {
        MovingObjectPosition mop = getPolarIntercept(yaw, pitch);
        if (mop == null || mop.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK) return null;
        if (!mop.getBlockPos().equals(target.against)) return null;
        BlockPos filled = mop.getBlockPos().offset(mop.sideHit);
        if (!isReplaceable(filled)) return null;
        double dist = mc.thePlayer.getPositionEyes(1.0f).distanceTo(mop.hitVec);
        if (dist > Math.min(reachDist.getValue(), 4.5) - 0.25) return null;
        return new PlaceTarget(mop.getBlockPos(), mop.sideHit, mop.hitVec);
    }

    private double[] leadPosition(double ticks) {
        double x = mc.thePlayer.posX, y = mc.thePlayer.posY, z = mc.thePlayer.posZ;
        double mx = mc.thePlayer.motionX, my = mc.thePlayer.motionY, mz = mc.thePlayer.motionZ;
        for (int i = 0; i < ticks; i++) {
            my = (my - 0.08) * 0.98;
            x += mx; y += my; z += mz;
            mx *= 0.91; mz *= 0.91;
        }
        return new double[]{ x, y, z };
    }

    private boolean hasFootingBelow() {
        BlockPos feet = new BlockPos(mc.thePlayer.posX, mc.thePlayer.posY - 0.1, mc.thePlayer.posZ);
        return isSolid(feet) || isSolid(feet.down()) || isSolid(feet.down(2));
    }

    private boolean isMoving() {
        return mc.gameSettings.keyBindForward.isKeyDown()
                || mc.gameSettings.keyBindBack.isKeyDown()
                || mc.gameSettings.keyBindLeft.isKeyDown()
                || mc.gameSettings.keyBindRight.isKeyDown();
    }

    private float[] rotationsTo(Vec3 hit) {
        Vec3 eyes = mc.thePlayer.getPositionEyes(1.0f);
        double dx = hit.xCoord - eyes.xCoord;
        double dy = hit.yCoord - eyes.yCoord;
        double dz = hit.zCoord - eyes.zCoord;
        double dist = Math.sqrt(dx * dx + dz * dz);
        float yaw = (float)(Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
        float pitch = (float)(-Math.toDegrees(Math.atan2(dy, dist)));
        return new float[]{ yaw, pitch };
    }

    private boolean stepRotation(float targetYaw, float targetPitch, float spd) {
        float gcd = gcdStep();

        float diffYaw = MathHelper.wrapAngleTo180_float(targetYaw - serverYaw);
        float diffPitch = targetPitch - serverPitch;

        float dYaw = MathHelper.clamp_float(diffYaw, -spd, spd);
        float dPitch = MathHelper.clamp_float(diffPitch, -spd, spd);

        serverYaw += snapToGCD(dYaw, diffYaw, gcd);
        serverPitch = MathHelper.clamp_float(serverPitch + snapToGCD(dPitch, diffPitch, gcd), -90f, 90f);

        return Math.abs(MathHelper.wrapAngleTo180_float(targetYaw - serverYaw)) < 3.0f
                && Math.abs(targetPitch - serverPitch) < 3.0f;
    }

    private float snapToGCD(float move, float diff, float gcd) {
        float snapped = Math.round(move / gcd) * gcd;
        if (diff > 0) return Math.min(snapped, diff);
        if (diff < 0) return Math.max(snapped, diff);
        return 0f;
    }

    private float gcdStep() {
        float f = mc.gameSettings.mouseSensitivity * 0.6F + 0.2F;
        float step = f * f * f * 8.0F * 0.15f;
        return step == 0 ? 0.0001f : step;
    }

    private void sendPlace(PlaceTarget pt) {
        int slot = findBlockSlot();
        if (slot == -1) return;

        KeyBinding.setKeyBindState(mc.gameSettings.keyBindUseItem.getKeyCode(), false);

        int savedSlot = mc.thePlayer.inventory.currentItem;
        mc.thePlayer.inventory.currentItem = slot;
        ItemStack stack = mc.thePlayer.inventory.getStackInSlot(slot);

        if (mc.playerController.onPlayerRightClick(mc.thePlayer, mc.theWorld, stack,
                pt.against, pt.face, pt.hitVec)) {
            mc.thePlayer.swingItem();
            lastPlaceYaw = serverYaw;
            lastPlacePitch = serverPitch;
            cooldown = 0;
            if (autoDisable.enabled) pendingDisable = true;
        }
        mc.thePlayer.inventory.currentItem = savedSlot;
    }

    private MovingObjectPosition getPolarIntercept(float yaw, float pitch) {
        Vec3 eyes = mc.thePlayer.getPositionEyes(1.0f);
        float f1 = MathHelper.cos(-yaw * 0.017453292F - (float) Math.PI);
        float f2 = MathHelper.sin(-yaw * 0.017453292F - (float) Math.PI);
        float f3 = -MathHelper.cos(-pitch * 0.017453292F);
        float f4 = MathHelper.sin(-pitch * 0.017453292F);
        Vec3 look = new Vec3(f2 * f3, f4, f1 * f3);
        double r = reachDist.getValue();
        Vec3 end = eyes.addVector(look.xCoord * r, look.yCoord * r, look.zCoord * r);
        return mc.theWorld.rayTraceBlocks(eyes, end, false, true, false);
    }

    private Vec3 closestPointOnFace(BlockPos pos, EnumFacing face) {
        Vec3 eyes = mc.thePlayer.getPositionEyes(1.0f);
        double minX = pos.getX() + 0.05, maxX = pos.getX() + 0.95;
        double minY = pos.getY() + 0.05, maxY = pos.getY() + 0.95;
        double minZ = pos.getZ() + 0.05, maxZ = pos.getZ() + 0.95;

        double x = MathHelper.clamp_double(eyes.xCoord, minX, maxX);
        double y = MathHelper.clamp_double(eyes.yCoord, minY, maxY);
        double z = MathHelper.clamp_double(eyes.zCoord, minZ, maxZ);

        if (face.getFrontOffsetX() != 0) x = pos.getX() + (face.getFrontOffsetX() > 0 ? 1.0 : 0.0);
        if (face.getFrontOffsetY() != 0) y = pos.getY() + (face.getFrontOffsetY() > 0 ? 1.0 : 0.0);
        if (face.getFrontOffsetZ() != 0) z = pos.getZ() + (face.getFrontOffsetZ() > 0 ? 1.0 : 0.0);

        return new Vec3(x, y, z);
    }

    private boolean isReplaceable(BlockPos pos) {
        if (!mc.theWorld.isBlockLoaded(pos)) return false;
        return mc.theWorld.getBlockState(pos).getBlock().isReplaceable(mc.theWorld, pos);
    }

    private boolean isSolid(BlockPos pos) {
        if (!mc.theWorld.isBlockLoaded(pos)) return false;
        IBlockState state = mc.theWorld.getBlockState(pos);
        Block block = state.getBlock();
        Material mat = block.getMaterial();
        if (mat == Material.air || mat.isLiquid()) return false;
        return block.isFullBlock() || block.isFullCube() || mc.theWorld.isSideSolid(pos, EnumFacing.UP, false);
    }

    private int findBlockSlot() {
        int current = mc.thePlayer.inventory.currentItem;
        if (isUsableBlock(current)) return current;
        for (int i = 0; i < 9; i++) {
            if (i != current && isUsableBlock(i)) return i;
        }
        return -1;
    }

    private boolean isUsableBlock(int slot) {
        ItemStack stack = mc.thePlayer.inventory.getStackInSlot(slot);
        if (stack == null || !(stack.getItem() instanceof ItemBlock)) return false;
        Material mat = ((ItemBlock) stack.getItem()).getBlock().getMaterial();
        return mat != Material.air && !mat.isLiquid();
    }

    private class ClutchMovementInput extends MovementInput {
        private final MovementInput parent;
        ClutchMovementInput(MovementInput parent) { this.parent = parent; }

        @Override
        public void updatePlayerMoveState() {
            parent.updatePlayerMoveState();
        this.jump = parent.jump;
        this.sneak = parent.sneak;
        this.moveForward = parent.moveForward;
        this.moveStrafe = parent.moveStrafe;

            if (!movementFix.enabled || !engaged) return;

            float forward = parent.moveForward;
            float strafe = parent.moveStrafe;
            if (forward == 0 && strafe == 0) return;

            float moveAngle = preclutchYaw;
            if (forward < 0) moveAngle += 180;
            float strafeOffset = 90 * (forward > 0 ? 0.5f : forward < 0 ? -0.5f : 1f);
            if (strafe > 0) moveAngle -= strafeOffset;
            if (strafe < 0) moveAngle += strafeOffset;

            float diff = MathHelper.wrapAngleTo180_float(moveAngle - serverYaw);

            int calcForward = 0, calcStrafe = 0;

            if      (diff > -22.5f  && diff <=  22.5f)  { calcForward =  1; }
            else if (diff >  22.5f  && diff <=  67.5f)  { calcForward =  1; calcStrafe = -1; }
            else if (diff >  67.5f  && diff <= 112.5f)  { calcStrafe  = -1; }
            else if (diff > 112.5f  && diff <= 157.5f)  { calcForward = -1; calcStrafe = -1; }
            else if (diff >  157.5f || diff <= -157.5f) { calcForward = -1; }
            else if (diff > -157.5f && diff <= -112.5f) { calcForward = -1; calcStrafe =  1; }
            else if (diff > -112.5f && diff <=  -67.5f) { calcStrafe  =  1; }
            else if (diff >  -67.5f && diff <=  -22.5f) { calcForward =  1; calcStrafe =  1; }

            float multiplier = parent.sneak ? 0.3f : 1.0f;
            this.moveForward = calcForward * multiplier;
            this.moveStrafe  = calcStrafe  * multiplier;
        }
    }

    private static class Prediction {
        boolean danger;
        boolean intoVoid;
    }

    private static class PlaceTarget {
        final BlockPos   against;
        final EnumFacing face;
        final Vec3       hitVec;
        PlaceTarget(BlockPos against, EnumFacing face, Vec3 hitVec) {
            this.against = against;
            this.face = face;
            this.hitVec = hitVec;
        }
    }
}
