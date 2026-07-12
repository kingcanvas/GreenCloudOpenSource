package greencloud.impl.utils.player;

import net.minecraft.client.Minecraft;
import net.minecraft.network.Packet;
import net.minecraft.network.NetworkManager;
import java.lang.reflect.Field;

public class PacketUtils {

    private static final Minecraft mc = Minecraft.getMinecraft();


    private static Field channelField;

    static {
        try {

            for (Field field : NetworkManager.class.getDeclaredFields()) {
                if (field.getType().getName().equals("io.netty.channel.Channel")) {
                    field.setAccessible(true);
                    channelField = field;
                    break;
                }
            }
        } catch (Exception e) {

            System.err.println("GreenCloud: Failed to initialize PacketUtils channel field.");
            e.printStackTrace();
        }
    }


    public static void sendPacket(Packet<?> packet) {
        if (mc.thePlayer != null && mc.thePlayer.sendQueue != null) {
            mc.thePlayer.sendQueue.addToSendQueue(packet);
        }
    }


    public static void sendPacketNoEvent(Packet<?> packet) {
        if (mc.thePlayer == null || mc.thePlayer.sendQueue == null) return;

        NetworkManager netManager = mc.thePlayer.sendQueue.getNetworkManager();

        if (netManager == null) return;

        try {

            io.netty.channel.Channel channel = (io.netty.channel.Channel) channelField.get(netManager);


            if (channel != null && channel.isOpen()) {
                channel.writeAndFlush(packet);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}