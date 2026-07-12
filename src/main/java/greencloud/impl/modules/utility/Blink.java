package greencloud.impl.modules.utility;

import greencloud.event.PacketEvent;
import greencloud.impl.modules.Category;
import greencloud.impl.modules.Module;
import greencloud.impl.settings.BooleanSetting;
import greencloud.impl.settings.NumberSetting;
import greencloud.impl.utils.render.RenderUtil;
import net.minecraft.client.entity.EntityOtherPlayerMP;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.C01PacketChatMessage;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.network.play.client.C14PacketTabComplete;
import net.minecraft.network.play.client.C15PacketClientSettings;
import net.minecraft.network.play.server.S08PacketPlayerPosLook;
import net.minecraft.util.Vec3;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.opengl.GL11;

import java.awt.Color;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;

public class Blink extends Module {

    public static Blink instance;

    private final BooleanSetting breadCrumbs = new BooleanSetting("BreadCrumbs", this, true);
    private final NumberSetting maxPackets   = new NumberSetting("Max Packets", this, 200, 10, 500, 10);
    private final BooleanSetting autoSend    = new BooleanSetting("Auto-Send", this, true);
    private final NumberSetting autoSendTicks = new NumberSetting("Auto-Send Ticks", this, 80, 10, 400, 5, () -> autoSend.enabled);

    private final Queue<Packet<?>> packets = new ConcurrentLinkedQueue<>();
    private final List<Vec3> breadcrumbPositions = new CopyOnWriteArrayList<>();
    private EntityOtherPlayerMP ghost;
    private int ticks = 0;
    private boolean flushing = false;

    public Blink() {
        super("Blink", "Holds all packets and bursts them on disable.", Category.UTILITY);
        addSettings(breadCrumbs, maxPackets, autoSend, autoSendTicks);
        instance = this;
    }

    @Override
    public void onEnable() {
        if (mc.thePlayer == null || mc.theWorld == null) {
            setToggled(false);
            return;
        }

        super.onEnable();

        packets.clear();
        breadcrumbPositions.clear();
        ticks = 0;
        flushing = false;

        spawnGhost();
        breadcrumbPositions.add(mc.thePlayer.getPositionVector());
    }

    @Override
    public void onDisable() {
        super.onDisable();

        flush();
        removeGhost();
        breadcrumbPositions.clear();
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || mc.thePlayer == null || mc.theWorld == null) return;

        if (autoSend.enabled) {
            ticks++;
            if (ticks >= (int) autoSendTicks.value) {
                mc.addScheduledTask(() -> setToggled(false));
                return;
            }
        }

        if (packets.size() >= (int) maxPackets.value) {
            mc.addScheduledTask(() -> setToggled(false));
            return;
        }

        if (breadCrumbs.enabled) {
            Vec3 currentPos = mc.thePlayer.getPositionVector();
            if (breadcrumbPositions.isEmpty() || breadcrumbPositions.get(breadcrumbPositions.size() - 1).distanceTo(currentPos) > 0.01) {
                breadcrumbPositions.add(currentPos);
            }
        }
    }

    @SubscribeEvent
    public void onPacket(PacketEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null || flushing) return;

        Packet<?> packet = event.getPacket();

        if (event.getDirection() == PacketEvent.Direction.INCOMING) {
            if (packet instanceof S08PacketPlayerPosLook) {
                mc.addScheduledTask(() -> setToggled(false));
            }
            return;
        }

        if (event.getDirection() != PacketEvent.Direction.OUTGOING) return;

        if (packet instanceof C01PacketChatMessage
                || packet instanceof C15PacketClientSettings
                || packet instanceof C14PacketTabComplete) {
            return;
        }

        packets.add(packet);
        event.setCanceled(true);
    }

    public void flush() {
        if (flushing || mc.getNetHandler() == null) return;
        flushing = true;

        try {
            while (!packets.isEmpty()) {
                Packet<?> p = packets.poll();
                if (p != null) {
                    mc.getNetHandler().getNetworkManager().sendPacket(p);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            flushing = false;
            packets.clear();
        }
    }

    @SubscribeEvent
    public void onRenderWorld(RenderWorldLastEvent event) {
        if (!breadCrumbs.enabled || breadcrumbPositions.isEmpty() || mc.thePlayer == null) return;

        double renderPosX = mc.getRenderManager().viewerPosX;
        double renderPosY = mc.getRenderManager().viewerPosY;
        double renderPosZ = mc.getRenderManager().viewerPosZ;

        GL11.glPushMatrix();
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glLineWidth(2.5f);

        RenderUtil.glColor(new Color(60, 200, 80).getRGB());

        GL11.glBegin(GL11.GL_LINE_STRIP);
        for (Vec3 pos : breadcrumbPositions) {
            GL11.glVertex3d(pos.xCoord - renderPosX, pos.yCoord - renderPosY, pos.zCoord - renderPosZ);
        }

        float partialTicks = event.partialTicks;
        double currentX = mc.thePlayer.lastTickPosX + (mc.thePlayer.posX - mc.thePlayer.lastTickPosX) * partialTicks;
        double currentY = mc.thePlayer.lastTickPosY + (mc.thePlayer.posY - mc.thePlayer.lastTickPosY) * partialTicks;
        double currentZ = mc.thePlayer.lastTickPosZ + (mc.thePlayer.posZ - mc.thePlayer.lastTickPosZ) * partialTicks;
        GL11.glVertex3d(currentX - renderPosX, currentY - renderPosY, currentZ - renderPosZ);

        GL11.glEnd();

        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glPopMatrix();
    }

    private void spawnGhost() {
        try {
            ghost = new EntityOtherPlayerMP(mc.theWorld, mc.thePlayer.getGameProfile());
            ghost.copyLocationAndAnglesFrom(mc.thePlayer);
            ghost.rotationYawHead = mc.thePlayer.rotationYawHead;
            ghost.renderYawOffset = mc.thePlayer.renderYawOffset;

            ghost.inventory.copyInventory(mc.thePlayer.inventory);

            mc.theWorld.addEntityToWorld(-9999, ghost);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void removeGhost() {
        if (ghost != null && mc.theWorld != null) {
            try {
                mc.theWorld.removeEntityFromWorld(-9999);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            ghost = null;
        }
    }
}