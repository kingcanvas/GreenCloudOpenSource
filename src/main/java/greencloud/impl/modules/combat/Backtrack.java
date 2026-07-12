package greencloud.impl.modules.combat;

import greencloud.event.PacketEvent;
import greencloud.impl.modules.Category;
import greencloud.impl.modules.Module;
import greencloud.impl.modules.render.HUD;
import greencloud.impl.settings.BooleanSetting;
import greencloud.impl.settings.NumberSetting;
import greencloud.impl.utils.TimerUtil;
import greencloud.impl.utils.render.GreenRender;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.network.Packet;
import net.minecraft.network.play.INetHandlerPlayClient;
import net.minecraft.network.play.client.C02PacketUseEntity;
import net.minecraft.network.play.server.*;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.Vec3;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;
import org.lwjgl.opengl.GL11;

import java.awt.Color;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ThreadLocalRandom;

public class Backtrack extends Module {

    private final NumberSetting delayMs = new NumberSetting("Delay", this, 80.0, 120.0, 0.0, 400.0, 10.0, true);
    private final NumberSetting maxRange = new NumberSetting("Max Server Reach", this, 6.0, 3.0, 8.0, 0.1);
    private final BooleanSetting smartOnly = new BooleanSetting("Smart Mode", this, true);
    private final BooleanSetting render = new BooleanSetting("Render", this, true);

    private final ConcurrentLinkedQueue<TimedPacket> packetQueue = new ConcurrentLinkedQueue<>();
    private final TimerUtil timer = new TimerUtil();

    private volatile EntityLivingBase target;
    private volatile Vec3 realServerPos;
    private volatile Vec3 prevServerPos;

    private long lastWorldLoad;
    private boolean isFlushing;

    private double renderX, renderY, renderZ;
    private double prevRenderX, prevRenderY, prevRenderZ;
    private boolean renderInit;
    private float renderAlpha;

    public Backtrack() {
        super("Backtrack", "Delays enemy position packets to extend reach.", Category.COMBAT);
        addSettings(delayMs, maxRange, smartOnly, render);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        resetState();
    }

    @Override
    public void onDisable() {
        super.onDisable();
        forceReleaseAll();
    }

    @SubscribeEvent
    public void onDisconnect(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        packetQueue.clear();
        resetState();
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START || mc.thePlayer == null || mc.theWorld == null) return;

        EntityLivingBase t = target;
        Vec3 sp = realServerPos;

        if (t != null) {
            if (smartOnly.enabled && mc.objectMouseOver != null && mc.objectMouseOver.entityHit == t)
                isFlushing = true;

            if (!t.isEntityAlive() || mc.thePlayer.getDistanceToEntity(t) > 7.0) {
                isFlushing = true;
                target = null;
            } else if (sp != null) {
                double dist = mc.thePlayer.getDistance(sp.xCoord, sp.yCoord, sp.zCoord);
                if (dist > maxRange.getValue()) isFlushing = true;
            }
        } else if (!packetQueue.isEmpty()) {
            isFlushing = true;
        }

        processPackets();

        t = target;
        sp = realServerPos;
        if (t != null && sp != null) {
            double destX = !packetQueue.isEmpty() ? sp.xCoord : t.posX;
            double destY = !packetQueue.isEmpty() ? sp.yCoord : t.posY;
            double destZ = !packetQueue.isEmpty() ? sp.zCoord : t.posZ;

            if (!renderInit) {
                renderX = prevRenderX = destX;
                renderY = prevRenderY = destY;
                renderZ = prevRenderZ = destZ;
                renderInit = true;
            } else {
                prevRenderX = renderX;
                prevRenderY = renderY;
                prevRenderZ = renderZ;
                
                double lerp = 0.15 + (0.35 * (1.0 - Math.min(1.0, packetQueue.size() / 25.0)));
                renderX += (destX - renderX) * lerp;
                renderY += (destY - renderY) * lerp;
                renderZ += (destZ - renderZ) * lerp;
            }
        }

        renderAlpha = (target != null)
                ? Math.min(1f, renderAlpha + 0.12f)
                : Math.max(0f, renderAlpha - 0.08f);
    }

    @SubscribeEvent
    public void onPacket(PacketEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null) return;
        if (System.currentTimeMillis() - lastWorldLoad < 1000L) return;

        Packet<?> packet = event.getPacket();

        if (event.getDirection() == PacketEvent.Direction.OUTGOING) {
            if (!(packet instanceof C02PacketUseEntity)) return;
            C02PacketUseEntity use = (C02PacketUseEntity) packet;
            if (use.getAction() != C02PacketUseEntity.Action.ATTACK) return;
            Entity hit = use.getEntityFromWorld(mc.theWorld);
            if (!(hit instanceof EntityLivingBase) || hit == mc.thePlayer) return;

            EntityLivingBase newTarget = (EntityLivingBase) hit;
            if (target != newTarget) {
                forceReleaseAll();
                target = newTarget;
                realServerPos = new Vec3(newTarget.posX, newTarget.posY, newTarget.posZ);
                prevServerPos = realServerPos;
                isFlushing = false;
                renderX = prevRenderX = newTarget.posX;
                renderY = prevRenderY = newTarget.posY;
                renderZ = prevRenderZ = newTarget.posZ;
                renderInit = true;
            }
            return;
        }

        if (event.getDirection() == PacketEvent.Direction.INCOMING) {
            if (packet instanceof S08PacketPlayerPosLook || packet instanceof S07PacketRespawn) {
                forceReleaseAll();
                if (packet instanceof S07PacketRespawn) lastWorldLoad = System.currentTimeMillis();
                return;
            }

            if ((packet instanceof S12PacketEntityVelocity
                    && ((S12PacketEntityVelocity) packet).getEntityID() == mc.thePlayer.getEntityId())
                    || packet instanceof S27PacketExplosion) {
                forceReleaseAll();
                return;
            }

            EntityLivingBase t = target;
            if (t == null) return;
            if (!isTargetPacket(packet, t)) return;

            prevServerPos = realServerPos;
            updateRealServerPosition(packet);

            event.setCanceled(true);
            long min = (long) delayMs.getValue();
            long max = (long) delayMs.maxValue;
            long delay = min + (max > min ? ThreadLocalRandom.current().nextLong(max - min) : 0);
            packetQueue.add(new TimedPacket(packet, System.currentTimeMillis() + delay));
        }
    }

    private void processPackets() {
        if (packetQueue.isEmpty()) {
            if (isFlushing && target == null) isFlushing = false;
            return;
        }

        long now = System.currentTimeMillis();
        int maxPerTick = isFlushing ? 50 : Math.max(8, packetQueue.size() / 2);
        int processed = 0;

        while (!packetQueue.isEmpty() && processed < maxPerTick) {
            TimedPacket tp = packetQueue.peek();
            if (!isFlushing && now < tp.releaseTime) break;
            packetQueue.poll();
            dispatchIncoming(tp.packet);
            processed++;
        }
    }

    private void forceReleaseAll() {
        TimedPacket tp;
        while ((tp = packetQueue.poll()) != null) dispatchIncoming(tp.packet);
        resetState();
    }

    @SuppressWarnings("unchecked")
    private void dispatchIncoming(Packet<?> packet) {
        if (mc.getNetHandler() == null) return;
        try { ((Packet<INetHandlerPlayClient>) packet).processPacket(mc.getNetHandler()); }
        catch (Exception ignored) {}
    }

    private void resetState() {
        target = null;
        realServerPos = null;
        prevServerPos = null;
        isFlushing = false;
    }

    private void updateRealServerPosition(Packet<?> packet) {
        Vec3 pos = realServerPos;
        if (pos == null) return;
        double x = pos.xCoord, y = pos.yCoord, z = pos.zCoord;
        if (packet instanceof S14PacketEntity) {
            S14PacketEntity s14 = (S14PacketEntity) packet;
            x += s14.func_149062_c() / 32.0;
            y += s14.func_149061_d() / 32.0;
            z += s14.func_149064_e() / 32.0;
        } else if (packet instanceof S18PacketEntityTeleport) {
            S18PacketEntityTeleport s18 = (S18PacketEntityTeleport) packet;
            x = s18.getX() / 32.0;
            y = s18.getY() / 32.0;
            z = s18.getZ() / 32.0;
        }
        realServerPos = new Vec3(x, y, z);
    }

    private boolean isTargetPacket(Packet<?> packet, EntityLivingBase t) {
        if (t == null) return false;
        int id = t.getEntityId();
        if (packet instanceof S14PacketEntity)         return ((S14PacketEntity) packet).getEntity(mc.theWorld) == t;
        if (packet instanceof S18PacketEntityTeleport) return ((S18PacketEntityTeleport) packet).getEntityId() == id;
        if (packet instanceof S19PacketEntityStatus)   return ((S19PacketEntityStatus) packet).getEntity(mc.theWorld) == t;
        if (packet instanceof S12PacketEntityVelocity) return ((S12PacketEntityVelocity) packet).getEntityID() == id;
        if (packet instanceof S0BPacketAnimation)      return ((S0BPacketAnimation) packet).getEntityID() == id;
        if (packet instanceof S1CPacketEntityMetadata) return ((S1CPacketEntityMetadata) packet).getEntityId() == id;
        return false;
    }

    @SubscribeEvent
    public void onRender3D(RenderWorldLastEvent event) {
        if (!render.enabled || renderAlpha < 0.01f || !renderInit) return;

        EntityLivingBase t = target;
        if (t == null && renderAlpha < 0.01f) return;

        float pt = event.partialTicks;

        double ix = prevRenderX + (renderX - prevRenderX) * pt;
        double iy = prevRenderY + (renderY - prevRenderY) * pt;
        double iz = prevRenderZ + (renderZ - prevRenderZ) * pt;

        double rx = ix - mc.getRenderManager().viewerPosX;
        double ry = iy - mc.getRenderManager().viewerPosY;
        double rz = iz - mc.getRenderManager().viewerPosZ;

        double w = (t != null ? t.width  : 0.6) / 2.0 + 0.05;
        double h = (t != null ? t.height : 1.8) + 0.1;

        Color base    = new Color(HUD.getColor(), true);
        Color fill    = GreenRender.withAlpha(base, renderAlpha * 0.15f);
        Color outline = GreenRender.withAlpha(base, renderAlpha * 0.9f);
        Color edge    = GreenRender.withAlpha(base, renderAlpha * 0.5f);

        AxisAlignedBB bb = new AxisAlignedBB(rx - w, ry, rz - w, rx + w, ry + h, rz + w);

        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GL11.glPushMatrix();
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glDepthMask(false);

        GL11.glColor4f(fill.getRed() / 255f, fill.getGreen() / 255f, fill.getBlue() / 255f, fill.getAlpha() / 255f);
        drawFilledBox(bb);

        GL11.glLineWidth(1.0f);
        GL11.glColor4f(edge.getRed() / 255f, edge.getGreen() / 255f, edge.getBlue() / 255f, edge.getAlpha() / 255f);
        drawBottomTopEdges(bb);

        GL11.glLineWidth(1.5f);
        GL11.glColor4f(outline.getRed() / 255f, outline.getGreen() / 255f, outline.getBlue() / 255f, outline.getAlpha() / 255f);
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
        GL11.glVertex3d(x0, y0, z0); GL11.glVertex3d(x1, y0, z0);
        GL11.glVertex3d(x1, y0, z1); GL11.glVertex3d(x0, y0, z1);
        GL11.glVertex3d(x0, y1, z0); GL11.glVertex3d(x0, y1, z1);
        GL11.glVertex3d(x1, y1, z1); GL11.glVertex3d(x1, y1, z0);
        GL11.glVertex3d(x0, y0, z0); GL11.glVertex3d(x0, y1, z0);
        GL11.glVertex3d(x1, y1, z0); GL11.glVertex3d(x1, y0, z0);
        GL11.glVertex3d(x1, y0, z1); GL11.glVertex3d(x1, y1, z1);
        GL11.glVertex3d(x0, y1, z1); GL11.glVertex3d(x0, y0, z1);
        GL11.glVertex3d(x0, y0, z0); GL11.glVertex3d(x0, y0, z1);
        GL11.glVertex3d(x0, y1, z1); GL11.glVertex3d(x0, y1, z0);
        GL11.glVertex3d(x1, y0, z0); GL11.glVertex3d(x1, y1, z0);
        GL11.glVertex3d(x1, y1, z1); GL11.glVertex3d(x1, y0, z1);
        GL11.glEnd();
    }

    private void drawBottomTopEdges(AxisAlignedBB b) {
        double x0 = b.minX, y0 = b.minY, z0 = b.minZ;
        double x1 = b.maxX, y1 = b.maxY, z1 = b.maxZ;
        GL11.glBegin(GL11.GL_LINE_LOOP);
        GL11.glVertex3d(x0, y0, z0); GL11.glVertex3d(x1, y0, z0);
        GL11.glVertex3d(x1, y0, z1); GL11.glVertex3d(x0, y0, z1);
        GL11.glEnd();
        GL11.glBegin(GL11.GL_LINE_LOOP);
        GL11.glVertex3d(x0, y1, z0); GL11.glVertex3d(x1, y1, z0);
        GL11.glVertex3d(x1, y1, z1); GL11.glVertex3d(x0, y1, z1);
        GL11.glEnd();
    }

    private static class TimedPacket {
        final Packet<?> packet;
        final long      releaseTime;
        TimedPacket(Packet<?> p, long t) { packet = p; releaseTime = t; }
    }
}