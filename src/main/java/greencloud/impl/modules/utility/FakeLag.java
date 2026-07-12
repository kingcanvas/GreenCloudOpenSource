package greencloud.impl.modules.utility;

import greencloud.event.PacketEvent;
import greencloud.impl.modules.Category;
import greencloud.impl.modules.Module;
import greencloud.impl.modules.render.HUD;
import greencloud.impl.settings.BooleanSetting;
import greencloud.impl.settings.ModeSetting;
import greencloud.impl.settings.NumberSetting;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.C00PacketKeepAlive;
import net.minecraft.network.play.client.C01PacketChatMessage;
import net.minecraft.network.play.client.C02PacketUseEntity;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.network.play.client.C0APacketAnimation;
import net.minecraft.network.play.client.C0FPacketConfirmTransaction;
import net.minecraft.network.play.server.S08PacketPlayerPosLook;
import net.minecraft.network.play.server.S12PacketEntityVelocity;
import net.minecraft.network.play.server.S19PacketEntityStatus;
import net.minecraft.util.AxisAlignedBB;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.opengl.GL11;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

import java.lang.reflect.Field;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.Random;

public class FakeLag extends Module {

    private final NumberSetting  maxDelay = new NumberSetting("Max Delay", this, 300, 50, 1000, 10);
    private final NumberSetting  cooldownTime = new NumberSetting("Cooldown", this, 500, 100, 5000, 10);
    private final BooleanSetting flushOnHit = new BooleanSetting("Flush on Hit", this, true);
    private final BooleanSetting delayPing = new BooleanSetting("Delay Ping", this, false);
    private final NumberSetting  maxPackets = new NumberSetting("Max Packets", this, 20, 5, 50, 1);
    private final BooleanSetting showIndicator = new BooleanSetting("Render Box", this, true);

    private final Queue<Packet<?>> packetQueue = new ConcurrentLinkedQueue<>();

    private double serverX, serverY, serverZ;
    private double freezeX, freezeY, freezeZ;

    private double renderX, renderY, renderZ;
    private double prevRenderX, prevRenderY, prevRenderZ;
    private float renderAlpha = 0f;

    private boolean posInit = false;
    private boolean isHolding = false;
    private boolean inCooldown = false;
    private boolean flushing = false;
    private long stateStartTime = 0;

    public FakeLag() {
        super("FakeLag", "Chokes and bursts packets.", Category.UTILITY);
        addSettings(maxDelay, cooldownTime, flushOnHit, delayPing, maxPackets, showIndicator);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        packetQueue.clear();
        isHolding = false;
        inCooldown = false;
        flushing = false;
        posInit = false;
        renderAlpha = 0f;

        if (mc.thePlayer != null) {
            serverX = mc.thePlayer.posX;
            serverY = mc.thePlayer.posY;
            serverZ = mc.thePlayer.posZ;

            renderX = prevRenderX = freezeX = serverX;
            renderY = prevRenderY = freezeY = serverY;
            renderZ = prevRenderZ = freezeZ = serverZ;

            posInit = true;
            beginHold(System.currentTimeMillis());
        }
    }

    @Override
    public void onDisable() {
        super.onDisable();
        flushAll();
        posInit = false;
        isHolding = false;
        inCooldown = false;
        renderAlpha = 0f;
    }

    private void beginHold(long now) {
        isHolding = true;
        inCooldown = false;
        stateStartTime = now;
        if (mc.thePlayer != null) {
            freezeX = mc.thePlayer.posX;
            freezeY = mc.thePlayer.posY;
            freezeZ = mc.thePlayer.posZ;
        }
    }

    private void beginCooldown(long now) {
        isHolding = false;
        inCooldown = true;
        stateStartTime = now;
    }

    private boolean isBypassPacket(Packet<?> packet) {
        if (packet instanceof C00PacketKeepAlive || packet instanceof C0FPacketConfirmTransaction) {
            return !delayPing.enabled;
        }
        return packet instanceof C01PacketChatMessage;
    }

    @SubscribeEvent
    public void onPacket(PacketEvent event) {
        if (mc.thePlayer == null || flushing) return;

        Packet<?> packet = event.getPacket();

        if (event.getDirection() == PacketEvent.Direction.INCOMING) {
            if (packet instanceof S08PacketPlayerPosLook) {
                flushAll();
            } else if (packet instanceof S12PacketEntityVelocity) {
                if (((S12PacketEntityVelocity) packet).getEntityID() == mc.thePlayer.getEntityId()) {
                    flushAll();
                }
            } else if (packet instanceof S19PacketEntityStatus) {
                if (((S19PacketEntityStatus) packet).getEntity(mc.theWorld) == mc.thePlayer) {
                    flushAll();
                }
            }
            return;
        }

        if (event.getDirection() == PacketEvent.Direction.OUTGOING) {
            if (isBypassPacket(packet)) return;

            if (packet instanceof C03PacketPlayer) {
                C03PacketPlayer c03 = (C03PacketPlayer) packet;
                if (c03.isMoving()) {
                    serverX = c03.getPositionX();
                    serverY = c03.getPositionY();
                    serverZ = c03.getPositionZ();
                    posInit = true;
                }
            } else if (packet instanceof C02PacketUseEntity) {
                C02PacketUseEntity c02 = (C02PacketUseEntity) packet;
                if (c02.getAction() == C02PacketUseEntity.Action.ATTACK) {
                    if (flushOnHit.enabled) {
                        packetQueue.add(packet);
                        event.setCanceled(true);
                        flushAll();
                        return;
                    }
                }
            }
            


            if (!isHolding || inCooldown) return;

            long movePackets = packetQueue.stream().filter(p -> p instanceof C03PacketPlayer).count();
            if (movePackets >= maxPackets.getValue()) {
                flushAll();
                return;
            }

            packetQueue.add(packet);
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onTick(TickEvent.PlayerTickEvent event) {
        if (mc.thePlayer == null || event.phase != TickEvent.Phase.END) return;

        long now = System.currentTimeMillis();
        long elapsed = now - stateStartTime;

        if (isHolding && !inCooldown) {
            if (elapsed >= (long) maxDelay.getValue()) {
                flushAll(now);
            }
        } else if (inCooldown) {
            if (elapsed >= (long) cooldownTime.getValue()) {
                beginHold(now);
            }
        } else {
            beginHold(now);
        }

        boolean choking = isHolding && !inCooldown && !packetQueue.isEmpty();
        renderAlpha = choking ? 1.0f : 0.0f;

        if (posInit) {
            prevRenderX = renderX;
            prevRenderY = renderY;
            prevRenderZ = renderZ;

            if (choking) {
                renderX = freezeX;
                renderY = freezeY;
                renderZ = freezeZ;
            } else {
                renderX = mc.thePlayer.posX;
                renderY = mc.thePlayer.posY;
                renderZ = mc.thePlayer.posZ;
            }
        }
    }

    private void flushAll() {
        flushAll(System.currentTimeMillis());
    }

    private void flushAll(long now) {
        if (flushing) return;
        flushing = true;

        if (mc.getNetHandler() != null) {
            while (!packetQueue.isEmpty()) {
                Packet<?> p = packetQueue.poll();
                if (p != null) {
                    mc.getNetHandler().getNetworkManager().sendPacket(p);
                }
            }
        } else {
            packetQueue.clear();
        }

        flushing = false;
        beginCooldown(now);
    }

    @SubscribeEvent
    public void onRenderWorldLast(RenderWorldLastEvent event) {
        if (!showIndicator.enabled || renderAlpha < 0.01f || !posInit) return;

        float pt = event.partialTicks;

        double ix = prevRenderX + (renderX - prevRenderX) * pt;
        double iy = prevRenderY + (renderY - prevRenderY) * pt;
        double iz = prevRenderZ + (renderZ - prevRenderZ) * pt;

        double rx = ix - mc.getRenderManager().viewerPosX;
        double ry = iy - mc.getRenderManager().viewerPosY;
        double rz = iz - mc.getRenderManager().viewerPosZ;

        AxisAlignedBB bb = new AxisAlignedBB(rx - 0.3, ry, rz - 0.3, rx + 0.3, ry + 1.8, rz + 0.3);

        int hex = HUD.getColor();
        float r = (hex >> 16 & 0xFF) / 255.0f;
        float g = (hex >> 8 & 0xFF) / 255.0f;
        float b = (hex & 0xFF) / 255.0f;

        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GL11.glPushMatrix();
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glDepthMask(false);

        GL11.glColor4f(r, g, b, renderAlpha * 0.12f);
        drawFilledBox(bb);

        GL11.glLineWidth(1.0f);
        GL11.glColor4f(r, g, b, renderAlpha * 0.5f);
        drawRing(bb.minX, bb.minY, bb.minZ, bb.maxX, bb.maxZ);
        drawRing(bb.minX, bb.maxY, bb.minZ, bb.maxX, bb.maxZ);

        GL11.glLineWidth(1.5f);
        GL11.glColor4f(r, g, b, renderAlpha * 0.9f);
        RenderGlobal.drawSelectionBoundingBox(bb);

        GL11.glDepthMask(true);
        GL11.glPopMatrix();
        GL11.glPopAttrib();

        GlStateManager.enableTexture2D();
        GlStateManager.enableDepth();
        GlStateManager.disableBlend();
        GlStateManager.color(1f, 1f, 1f, 1f);
    }

    private void drawFilledBox(AxisAlignedBB b) {
        double x0 = b.minX, y0 = b.minY, z0 = b.minZ;
        double x1 = b.maxX, y1 = b.maxY, z1 = b.maxZ;
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glVertex3d(x0, y0, z0); GL11.glVertex3d(x1, y0, z0); GL11.glVertex3d(x1, y0, z1); GL11.glVertex3d(x0, y0, z1);
        GL11.glVertex3d(x0, y1, z0); GL11.glVertex3d(x0, y1, z1); GL11.glVertex3d(x1, y1, z1); GL11.glVertex3d(x1, y1, z0);
        GL11.glVertex3d(x0, y0, z0); GL11.glVertex3d(x0, y1, z0); GL11.glVertex3d(x1, y1, z0); GL11.glVertex3d(x1, y0, z0);
        GL11.glVertex3d(x1, y0, z0); GL11.glVertex3d(x1, y1, z0); GL11.glVertex3d(x1, y1, z1); GL11.glVertex3d(x1, y0, z1);
        GL11.glVertex3d(x0, y0, z1); GL11.glVertex3d(x1, y0, z1); GL11.glVertex3d(x1, y1, z1); GL11.glVertex3d(x0, y1, z1);
        GL11.glVertex3d(x0, y0, z0); GL11.glVertex3d(x0, y0, z1); GL11.glVertex3d(x0, y1, z1); GL11.glVertex3d(x0, y1, z0);
        GL11.glEnd();
    }

    private void drawRing(double x0, double y, double z0, double x1, double z1) {
        GL11.glBegin(GL11.GL_LINE_LOOP);
        GL11.glVertex3d(x0, y, z0);
        GL11.glVertex3d(x1, y, z0);
        GL11.glVertex3d(x1, y, z1);
        GL11.glVertex3d(x0, y, z1);
        GL11.glEnd();
    }
}