package greencloud.impl.utils.network;

import greencloud.event.PacketEvent;
import greencloud.impl.logger.Log;
import greencloud.impl.logger.Logger;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;

public class NettyInjector {

    private static final Logger log = Log.get(NettyInjector.class);

    public static void init() {
        log.info("Registering Netty packet injector");
        try {
            MinecraftForge.EVENT_BUS.register(new NettyInjector());
            log.debug("Netty injector registered on event bus");
        } catch (Exception e) {
            log.error("Failed to register Netty packet injector", e);
        }
    }

    @SubscribeEvent
    public void onClientConnected(FMLNetworkEvent.ClientConnectedToServerEvent event) {
        try {
            if (!event.manager.channel().pipeline().names().contains("greencloud_hook")) {
                event.manager.channel().pipeline().addBefore("packet_handler", "greencloud_hook", new ChannelDuplexHandler() {

                    @Override
                    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                        if (msg instanceof net.minecraft.network.Packet) {
                            try {
                                PacketEvent packetEvent = new PacketEvent((net.minecraft.network.Packet<?>) msg, PacketEvent.Direction.INCOMING);
                                MinecraftForge.EVENT_BUS.post(packetEvent);
                                if (packetEvent.isCanceled()) return;
                            } catch (Exception e) {
                                log.error("Exception processing incoming packet: " + msg.getClass().getSimpleName(), e);
                            }
                        }
                        super.channelRead(ctx, msg);
                    }

                    @Override
                    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
                        if (msg instanceof net.minecraft.network.Packet) {
                            try {
                                PacketEvent packetEvent = new PacketEvent((net.minecraft.network.Packet<?>) msg, PacketEvent.Direction.OUTGOING);
                                MinecraftForge.EVENT_BUS.post(packetEvent);
                                if (packetEvent.isCanceled()) return;
                            } catch (Exception e) {
                                log.error("Exception processing outgoing packet: " + msg.getClass().getSimpleName(), e);
                            }
                        }
                        super.write(ctx, msg, promise);
                    }
                });
                log.info("Netty pipeline hook injected successfully");
            } else {
                log.debug("Netty pipeline hook already present, skipping injection");
            }
        } catch (Exception e) {
            log.error("Failed to inject Netty pipeline hook", e);
        }
    }
}
