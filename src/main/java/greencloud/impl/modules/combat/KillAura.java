package greencloud.impl.modules.combat;

import greencloud.GreenCloud;
import greencloud.impl.modules.Category;
import greencloud.impl.modules.Module;
import greencloud.impl.settings.NumberSetting;
import greencloud.impl.settings.BooleanSetting;
import greencloud.impl.settings.ModeSetting;
import greencloud.impl.utils.RotationManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import greencloud.impl.utils.player.MoveUtil;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovementInput;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

public class KillAura extends Module {

    private final ModeSetting mode = new ModeSetting("Mode", this, "Normal", "Normal", "Legit");

    private final NumberSetting cps = new NumberSetting("CPS", this, 9.0, 13.0, 1.0, 20.0, 0.5, true);

    private final NumberSetting rotationSpeed = new NumberSetting("Rotation Speed", this, 15.0, 1.0, 50.0, 1.0) {
        public boolean isVisible() { return mode.is("Normal"); }
        public boolean isHidden() { return !mode.is("Normal"); }
    };
    private final NumberSetting fov = new NumberSetting("FOV", this, 180.0, 30.0, 360.0, 10.0) {
        public boolean isVisible() { return mode.is("Normal"); }
        public boolean isHidden() { return !mode.is("Normal"); }
    };
    private final NumberSetting preAttackRange = new NumberSetting("Pre-Attack Range", this, 3.0, 1.0, 6.0, 0.1) {
        public boolean isVisible() { return mode.is("Normal"); }
        public boolean isHidden() { return !mode.is("Normal"); }
    };

    private final NumberSetting legitFov = new NumberSetting("Legit FOV", this, 45.0, 5.0, 360.0, 1.0) {
        public boolean isVisible() { return mode.is("Legit"); }
        public boolean isHidden() { return !mode.is("Legit"); }
    };
    private final NumberSetting legitSpeed = new NumberSetting("Legit Speed", this, 3.0, 0.5, 10.0, 0.1) {
        public boolean isVisible() { return mode.is("Legit"); }
        public boolean isHidden() { return !mode.is("Legit"); }
    };
    private final NumberSetting legitRange = new NumberSetting("Legit Range", this, 4.5, 2.0, 8.0, 0.1) {
        public boolean isVisible() { return mode.is("Legit"); }
        public boolean isHidden() { return !mode.is("Legit"); }
    };
    private final BooleanSetting blockCheck = new BooleanSetting("Block Check", this, true) {
        public boolean isVisible() { return mode.is("Legit"); }
        public boolean isHidden() { return !mode.is("Legit"); }
    };

    private EntityLivingBase target = null;
    private long nextDelay = 0L;
    private double delayAccumulator = 0.0;
    private long lastTickTime = 0L;

    private double aimOffsetX = 0;
    private double aimOffsetY = 0;
    private int offsetTimer = 0;

    private float smoothYaw = 0f;
    private float smoothPitch = 0f;

    private float[] cachedRotations = null;

    private MovementInput originalKAInput;

    private float normYaw, normPitch, normPrevYaw, normPrevPitch;
    private float normSavedYaw, normSavedPitch, normSavedPrevYaw, normSavedPrevPitch;
    private float normSRYO, normSRYH, normSRP, normSPRYO, normSPRYH, normSPRP;
    private boolean normSpoofing;
    private boolean normCoasting;
    private int normAimTicks;
    private float normNoiseX, normNoiseY;

    private double aimOffXSmooth, aimOffYSmooth, aimOffZSmooth;
    private double aimOffXTarget, aimOffYTarget, aimOffZTarget;
    private int aimPointTimer;
    private EntityLivingBase lastTarget;

    private final Random random = new Random();
    private double speedFactor = 1.0;
    private int clicksInCurrentStreak = 0;
    private int streakTarget = 10;

    public KillAura() {
        super("KillAura", "Automatically attacks nearby entities", Category.COMBAT);
        addSettings(
                mode, cps,
                rotationSpeed, fov, preAttackRange,
                legitFov, legitSpeed, legitRange, blockCheck
        );
    }

    @Override
    public void onEnable() {
        super.onEnable();
        target = null;
        cachedRotations = null;
        lastTickTime = System.currentTimeMillis();
        delayAccumulator = 0.0;
        clicksInCurrentStreak = 0;
        streakTarget = 5 + random.nextInt(10);
        nextDelay = calculateDelay();
        smoothYaw = 0f;
        smoothPitch = 0f;
        normSpoofing = false;
        normCoasting = false;
        normAimTicks = 0;
        normNoiseX = normNoiseY = 0f;
        aimOffXSmooth = aimOffYSmooth = aimOffZSmooth = 0;
        aimOffXTarget = aimOffYTarget = aimOffZTarget = 0;
        aimPointTimer = 0;
        lastTarget = null;
        if (mc.thePlayer != null) {
            normYaw = mc.thePlayer.rotationYaw;
            normPitch = mc.thePlayer.rotationPitch;
            normPrevYaw = normYaw;
            normPrevPitch = normPitch;

            MovementInput cur = mc.thePlayer.movementInput;
            originalKAInput = (cur instanceof KAMovementInput) ? ((KAMovementInput) cur).parent : cur;
            mc.thePlayer.movementInput = new KAMovementInput(originalKAInput);
        }
        RotationManager.getInstance().reset();
    }

    @Override
    public void onDisable() {
        if (normSpoofing && mc.thePlayer != null) {
            mc.thePlayer.rotationYaw = normSavedYaw;
            mc.thePlayer.rotationPitch = normSavedPitch;
            mc.thePlayer.prevRotationYaw = normSavedPrevYaw;
            mc.thePlayer.prevRotationPitch = normSavedPrevPitch;
            normSpoofing = false;
        }
        if (mc.thePlayer != null && originalKAInput != null) {
            mc.thePlayer.movementInput = originalKAInput;
            originalKAInput = null;
        }
        RotationManager.getInstance().reset();
        target = null;
        cachedRotations = null;
        super.onDisable();
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null || mc.currentScreen != null) return;

        if (event.phase == TickEvent.Phase.START) {
            if (mode.is("Legit")) {
                if (normSpoofing) {
                    mc.thePlayer.rotationYaw = normSavedYaw;
                    mc.thePlayer.rotationPitch = normSavedPitch;
                    mc.thePlayer.prevRotationYaw = normSavedYaw;
                    mc.thePlayer.prevRotationPitch = normSavedPitch;
                    normSpoofing = false;
                }
                RotationManager.getInstance().reset();
                target = getBestLegitTarget();

                if (target != null) {
                    boolean isBlocking = blockCheck.enabled
                            && mc.playerController != null
                            && mc.playerController.getIsHittingBlock();

                    if (!isBlocking) {
                        cachedRotations = getRotations(target);
                        rotateOffset();
                    } else {
                        cachedRotations = null;
                    }
                } else {
                    cachedRotations = null;
                }
            } else {
                RotationManager.getInstance().reset();
                tickNormalMode();
            }

            long now = System.currentTimeMillis();
            long elapsed = now - lastTickTime;
            lastTickTime = now;
            if (elapsed > 200) elapsed = 50;

            delayAccumulator += elapsed;

            double attackDist = getAttackDist();
            int clicksThisTick = 0;

            while (delayAccumulator >= nextDelay && clicksThisTick < 2) {
                boolean shouldClick = false;

                if (mode.is("Legit")) {
                    if (mc.objectMouseOver != null
                            && mc.objectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.ENTITY
                            && mc.thePlayer.getDistanceToEntity(mc.objectMouseOver.entityHit) <= attackDist) {
                        shouldClick = true;
                    }
                } else {
                    if (target != null && mc.thePlayer.getDistanceToEntity(target) <= attackDist) {
                        Vec3 eyes = mc.thePlayer.getPositionEyes(1.0f);
                        float cosYaw = MathHelper.cos(-normYaw * 0.017453292F - (float) Math.PI);
                        float sinYaw = MathHelper.sin(-normYaw * 0.017453292F - (float) Math.PI);
                        float cosPitch = -MathHelper.cos(-normPitch * 0.017453292F);
                        float sinPitch = MathHelper.sin(-normPitch * 0.017453292F);
                        Vec3 look = new Vec3(sinYaw * cosPitch, sinPitch, cosYaw * cosPitch);
                        Vec3 end = eyes.addVector(look.xCoord * attackDist,
                                                   look.yCoord * attackDist,
                                                   look.zCoord * attackDist);
                        AxisAlignedBB aabb =
                                target.getEntityBoundingBox().expand(0.1, 0.1, 0.1);
                        MovingObjectPosition intercept = aabb.calculateIntercept(eyes, end);
                        if (intercept != null) {
                            mc.objectMouseOver = new MovingObjectPosition(target);
                            shouldClick = true;
                        }
                    }
                }

                if (shouldClick) {
                    AutoClicker ac = getAutoClicker();
                    if (ac != null) ac.click();
                    clicksThisTick++;
                }

                delayAccumulator -= nextDelay;
                nextDelay = calculateDelay();
            }
        } else if (event.phase == TickEvent.Phase.END) {
            if (mode.is("Legit") && target != null) {
                aimAtTarget();
            } else if (mode.is("Normal") && normSpoofing) {
                mc.thePlayer.rotationYaw = normSavedYaw;
                mc.thePlayer.rotationPitch = normSavedPitch;
                mc.thePlayer.prevRotationYaw = normSavedYaw;
                mc.thePlayer.prevRotationPitch = normSavedPitch;
                normSpoofing = false;
            }
        }
    }

    private void tickNormalMode() {
        normPrevYaw = normYaw;
        normPrevPitch = normPitch;
        target = findNormalTarget();

        if (target != null) {
            normCoasting = false;
            if (target != lastTarget) {
                aimOffXSmooth = aimOffYSmooth = aimOffZSmooth = 0;
                aimOffXTarget = aimOffYTarget = aimOffZTarget = 0;
                aimPointTimer = 0;
                lastTarget = target;
            }
            updateAimPoint(target);
            float[] want = getNormRotations(target);
            if (want != null) {
                stepNormRotation(want[0], want[1]);
                applyNormRotation();
            }
        } else {
            lastTarget = null;
            aimPointTimer = 0;

            if (normAimTicks > 0 && !normCoasting) {
                normCoasting = true;
            }

            if (normCoasting) {
                float naturalYaw = mc.thePlayer.rotationYaw;
                float naturalPitch = mc.thePlayer.rotationPitch;
                float gcd = getNormGCD();
                float yawDiff = getAngleDifference(naturalYaw, normYaw);
                float pitchDiff = naturalPitch - normPitch;

                if (Math.abs(yawDiff) < gcd * 1.5f && Math.abs(pitchDiff) < gcd * 1.5f) {
                    normCoasting = false;
                    normAimTicks = 0;
                    normNoiseX = normNoiseY = 0f;
                    normYaw = naturalYaw;
                    normPitch = naturalPitch;
                } else {
                    float spd = Math.max(8f, (float) rotationSpeed.getValue() * 0.5f);
                    float moveYaw = MathHelper.clamp_float(yawDiff, -spd, spd);
                    float movePitch = MathHelper.clamp_float(pitchDiff, -spd, spd);
                    normYaw += snapLegitGCD(moveYaw, yawDiff, gcd);
                    normPitch = MathHelper.clamp_float(normPitch + snapLegitGCD(movePitch, pitchDiff, gcd), -90f, 90f);
                    applyNormRotation();
                }
                return;
            }

            normAimTicks = 0;
            normNoiseX = normNoiseY = 0f;
            normYaw = mc.thePlayer.rotationYaw;
            normPitch = mc.thePlayer.rotationPitch;
        }
    }

    private void updateAimPoint(EntityLivingBase entity) {
        aimPointTimer--;
        if (aimPointTimer <= 0) {
            double hw = entity.width * 0.35;
            double h = entity.height;
            aimOffXTarget = (random.nextDouble() * 2 - 1) * hw;
            aimOffZTarget = (random.nextDouble() * 2 - 1) * hw;
            double[] yLevels = { h * 0.85, h * 0.65, h * 0.45, h * 0.2 };
            aimOffYTarget = yLevels[random.nextInt(yLevels.length)] + random.nextGaussian() * 0.04;
            aimPointTimer = 8 + random.nextInt(16);
        }
        double alpha = 0.10 + random.nextDouble() * 0.08;
        aimOffXSmooth += (aimOffXTarget - aimOffXSmooth) * alpha;
        aimOffYSmooth += (aimOffYTarget - aimOffYSmooth) * alpha;
        aimOffZSmooth += (aimOffZTarget - aimOffZSmooth) * alpha;
    }

    private float[] getNormRotations(EntityLivingBase entity) {
        double x = entity.posX + aimOffXSmooth;
        double y = entity.posY + aimOffYSmooth;
        double z = entity.posZ + aimOffZSmooth;
        double dx = x - mc.thePlayer.posX;
        double dy = y - (mc.thePlayer.posY + mc.thePlayer.getEyeHeight());
        double dz = z - mc.thePlayer.posZ;
        double dist = Math.sqrt(dx * dx + dz * dz);
        if (dist == 0) return null;
        float yaw = (float)(Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
        float pitch = (float)(-Math.toDegrees(Math.atan2(dy, dist)));
        return new float[]{yaw, pitch};
    }

    private void stepNormRotation(float targetYaw, float targetPitch) {
        float speed = (float) rotationSpeed.getValue();
        float gcd = getNormGCD();

        normNoiseX = MathHelper.clamp_float(normNoiseX + (random.nextFloat() - 0.5f) * 1.5f, -2f, 2f);
        normNoiseY = MathHelper.clamp_float(normNoiseY + (random.nextFloat() - 0.5f) * 0.8f, -1f, 1f);

        float easeIn = Math.min(1.0f, (normAimTicks + 1) / 6.0f);

        float dYaw = getAngleDifference(targetYaw, normYaw);
        float dPitch = targetPitch - normPitch;

        dYaw = MathHelper.clamp_float(dYaw, -speed * easeIn, speed * easeIn);
        dPitch = MathHelper.clamp_float(dPitch, -speed * easeIn, speed * easeIn);

        normYaw += Math.round(dYaw / gcd + normNoiseX) * gcd;
        normPitch = MathHelper.clamp_float(normPitch + Math.round(dPitch / gcd + normNoiseY) * gcd, -90f, 90f);
    }

    private void applyNormRotation() {
        if (!normSpoofing) {
            normSavedYaw = mc.thePlayer.rotationYaw;
            normSavedPitch = mc.thePlayer.rotationPitch;
            normSavedPrevYaw = mc.thePlayer.prevRotationYaw;
            normSavedPrevPitch = mc.thePlayer.prevRotationPitch;
        }
        mc.thePlayer.rotationYaw = normYaw;
        mc.thePlayer.rotationPitch = normPitch;
        mc.thePlayer.prevRotationYaw = normPrevYaw;
        mc.thePlayer.prevRotationPitch = normPrevPitch;
        normSpoofing = true;
        normAimTicks++;
    }

    private float getNormGCD() {
        float f = mc.gameSettings.mouseSensitivity * 0.6F + 0.2F;
        float step = f * f * f * 8.0F * 0.15f;
        return step == 0 ? 0.0001f : step;
    }

    @SubscribeEvent
    public void onRenderPlayerPre(RenderPlayerEvent.Pre event) {
        if (!normSpoofing || !mode.is("Normal") || event.entityPlayer != mc.thePlayer) return;
        normSRYO = event.entityPlayer.renderYawOffset;
        normSRYH = event.entityPlayer.rotationYawHead;
        normSRP = event.entityPlayer.rotationPitch;
        normSPRYO = event.entityPlayer.prevRenderYawOffset;
        normSPRYH = event.entityPlayer.prevRotationYawHead;
        normSPRP = event.entityPlayer.prevRotationPitch;

        event.entityPlayer.renderYawOffset = normYaw;
        event.entityPlayer.rotationYawHead = normYaw;
        event.entityPlayer.rotationPitch = normPitch;
        event.entityPlayer.prevRenderYawOffset = normPrevYaw;
        event.entityPlayer.prevRotationYawHead = normPrevYaw;
        event.entityPlayer.prevRotationPitch = normPrevPitch;
    }

    @SubscribeEvent
    public void onRenderPlayerPost(RenderPlayerEvent.Post event) {
        if (!normSpoofing || !mode.is("Normal") || event.entityPlayer != mc.thePlayer) return;
        event.entityPlayer.renderYawOffset = normSRYO;
        event.entityPlayer.rotationYawHead = normSRYH;
        event.entityPlayer.rotationPitch = normSRP;
        event.entityPlayer.prevRenderYawOffset = normSPRYO;
        event.entityPlayer.prevRotationYawHead = normSPRYH;
        event.entityPlayer.prevRotationPitch = normSPRP;
    }

    private void aimAtTarget() {
        if (cachedRotations == null) return;
        float targetYaw = cachedRotations[0] + (float) aimOffsetX;
        float targetPitch = cachedRotations[1] + (float) aimOffsetY;

        float yawDiff = getAngleDifference(targetYaw, mc.thePlayer.rotationYaw);
        float pitchDiff = targetPitch - mc.thePlayer.rotationPitch;

        float gcd = getNormGCD();
        if (Math.abs(yawDiff) < gcd * 0.5f && Math.abs(pitchDiff) < gcd * 0.5f) return;

        float spd = (float) legitSpeed.getValue();
        float angularDist = (float) Math.sqrt(yawDiff * yawDiff + pitchDiff * pitchDiff);
        float distFactor = MathHelper.clamp_float(angularDist / 20f, 0.2f, 1.0f);
        float baseMove = spd * distFactor;

        float moveYaw = MathHelper.clamp_float(yawDiff, -baseMove, baseMove);
        float movePitch = MathHelper.clamp_float(pitchDiff, -baseMove, baseMove);

        mc.thePlayer.rotationYaw += snapLegitGCD(moveYaw, yawDiff, gcd);
        mc.thePlayer.rotationPitch  = MathHelper.clamp_float(
            mc.thePlayer.rotationPitch + snapLegitGCD(movePitch, pitchDiff, gcd), -90f, 90f);
    }

    private float snapLegitGCD(float move, float diff, float gcd) {
        float snapped = Math.round(move / gcd) * gcd;
        if (diff > 0) return Math.min(snapped, diff);
        if (diff < 0) return Math.max(snapped, diff);
        return 0f;
    }

    private float clamp(float val, float min, float max) {
        return Math.max(min, Math.min(max, val));
    }

    private void rotateOffset() {
        offsetTimer--;
        if (offsetTimer <= 0) {
            aimOffsetX = random.nextGaussian() * 1.5;
            aimOffsetY = random.nextGaussian() * 1.5;
            offsetTimer = 5 + random.nextInt(10);
        }
    }

    private float floorToGCD(float value) {
        float sens = mc.gameSettings.mouseSensitivity * 0.6F + 0.2F;
        float step = sens * sens * sens * 8.0F * 0.15F;
        float sign = value < 0 ? -1f : 1f;
        int steps = (int)(Math.abs(value) / step);
        return sign * steps * step;
    }

    private EntityPlayer getBestLegitTarget() {
        EntityPlayer bestTarget = null;
        double rangeSq = legitRange.getValue() * legitRange.getValue();
        double closestFovSq = legitFov.getValue() * legitFov.getValue();

        List<EntityPlayer> players = mc.theWorld.playerEntities;
        int size = players.size();
        for (int i = 0; i < size; i++) {
            EntityPlayer player = players.get(i);
            if (player == mc.thePlayer) continue;
            if (player.isDead || player.getHealth() <= 0 || player.isInvisible()) continue;
            if (isTeam(player)) continue;
            if (mc.thePlayer.getDistanceSqToEntity(player) > rangeSq) continue;

            float[] rotations = getRotations(player);
            if (rotations == null) continue;

            float yawDiff = Math.abs(getAngleDifference(rotations[0], mc.thePlayer.rotationYaw));
            float pitchDiff = Math.abs(rotations[1] - mc.thePlayer.rotationPitch);
            double fovDistSq = yawDiff * yawDiff + pitchDiff * pitchDiff;

            if (fovDistSq < closestFovSq) {
                closestFovSq = fovDistSq;
                bestTarget = player;
            }
        }
        return bestTarget;
    }

    private float[] getRotations(EntityLivingBase entity) {
        double diffX = entity.posX - mc.thePlayer.posX;
        double diffY = (entity.posY + entity.getEyeHeight() * 0.8) - (mc.thePlayer.posY + mc.thePlayer.getEyeHeight());
        double diffZ = entity.posZ - mc.thePlayer.posZ;
        double distance = Math.sqrt(diffX * diffX + diffZ * diffZ);
        if (distance == 0.0) return null;

        float yaw = (float) (Math.toDegrees(Math.atan2(diffZ, diffX)) - 90.0);
        float pitch = (float) (-Math.toDegrees(Math.atan2(diffY, distance)));
        return new float[]{yaw, pitch};
    }

    private float getAngleDifference(float target, float current) {
        return ((((target - current) % 360f) + 540f) % 360f) - 180f;
    }

    private EntityLivingBase findNormalTarget() {
        List<EntityLivingBase> candidates = new ArrayList<>();
        double scanRange = getAttackDist() + 2.0;

        for (Entity entity : mc.theWorld.loadedEntityList) {
            if (!(entity instanceof EntityLivingBase)) continue;
            EntityLivingBase living = (EntityLivingBase) entity;
            if (!isValidNormalTarget(living)) continue;
            if (mc.thePlayer.getDistanceToEntity(living) > scanRange) continue;
            if (fov.getValue() < 360 && getAngleToEntity(living) > fov.getValue()) continue;
            candidates.add(living);
        }

        if (candidates.isEmpty()) return null;
        candidates.sort(Comparator.comparingDouble(e -> mc.thePlayer.getDistanceToEntity(e)));
        return candidates.get(0);
    }

    private boolean isValidNormalTarget(EntityLivingBase entity) {
        if (entity == mc.thePlayer) return false;
        if (entity.isDead || entity.getHealth() <= 0) return false;
        if (!(entity instanceof EntityPlayer)) return false;
        if (isTeam((EntityPlayer) entity)) return false;
        return true;
    }

    private boolean isTeam(EntityPlayer entity) {
        String targetName = entity.getDisplayName().getFormattedText().replace("§r", "");
        String clientName = mc.thePlayer.getDisplayName().getFormattedText().replace("§r", "");
        if (targetName.startsWith("§") && clientName.startsWith("§")) {
            return targetName.charAt(1) == clientName.charAt(1);
        }
        return false;
    }

    private double getAngleToEntity(Entity entity) {
        double dx = entity.posX - mc.thePlayer.posX;
        double dz = entity.posZ - mc.thePlayer.posZ;
        double yawToE = Math.toDegrees(Math.atan2(dz, dx)) - 90.0;
        double diff = yawToE - mc.thePlayer.rotationYaw;
        while (diff > 180.0)  diff -= 360.0;
        while (diff < -180.0) diff += 360.0;
        return Math.abs(diff);
    }

    private double getAttackDist() {
        return mode.is("Legit") ? legitRange.getValue() : preAttackRange.getValue();
    }

    private long calculateDelay() {
        double roll = random.nextDouble();
        if (roll < 0.04) return 10 + random.nextInt(25);
        if (roll < 0.07) return 220 + random.nextInt(150);

        double min = cps.getValue();
        double max = cps.maxValue;

        if (clicksInCurrentStreak >= streakTarget) {
            clicksInCurrentStreak = 0;
            streakTarget = 5 + random.nextInt(10);
            double r = random.nextDouble();
            if (r < 0.20)      speedFactor = 0.65 + random.nextDouble() * 0.20;
            else if (r < 0.40) speedFactor = 1.05 + random.nextDouble() * 0.15;
            else               speedFactor = 0.85 + random.nextDouble() * 0.20;
        }
        clicksInCurrentStreak++;

        double targetCPS = Math.max(2.0, Math.min((min + random.nextDouble() * (max - min)) * speedFactor, max + 3.0));
        return Math.max(10, (long)(1000.0 / targetCPS) + (long)(random.nextGaussian() * 3.0));
    }

    private AutoClicker getAutoClicker() {
        return GreenCloud.moduleManager == null ? null
                : (AutoClicker) GreenCloud.moduleManager.getModule(AutoClicker.class);
    }

    public EntityLivingBase getTarget() { return target; }

    private class KAMovementInput extends MovementInput {
        final MovementInput parent;
        KAMovementInput(MovementInput parent) { this.parent = parent; }

        @Override
        public void updatePlayerMoveState() {
            parent.updatePlayerMoveState();
            this.jump  = parent.jump;
            this.sneak = parent.sneak;
            this.moveForward = parent.moveForward;
            this.moveStrafe  = parent.moveStrafe;

            if (!normSpoofing || !mode.is("Normal")) return;

            MoveUtil.fixMovement(this, normYaw, normSavedYaw, parent.moveForward, parent.moveStrafe);
        }
    }
}