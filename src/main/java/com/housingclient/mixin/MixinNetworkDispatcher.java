package com.housingclient.mixin;

import com.housingclient.HousingClient;
import com.housingclient.utils.BungeePacketQueue;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S3FPacketCustomPayload;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraftforge.fml.common.network.handshake.NetworkDispatcher", remap = false)
public class MixinNetworkDispatcher {

    /**
     * Forge's NetworkDispatcher intercepts S3FPacketCustomPayload (Custom Payload)
     * packets
     * before they reach NetworkManager. For "REGISTER" and "UNREGISTER" channels,
     * it
     * immediately fires a CustomPacketRegistrationEvent.
     * 
     * In Forge 1.8.9, this event constructor explicitly casts the NetworkManager's
     * NetHandler to INetHandlerPlayClient. If BungeeCord sends these packets during
     * the LOGIN phase, it crashes the client with a ClassCastException.
     * 
     * We intercept here, identify REGISTER/UNREGISTER packets, and if we are still
     * in the Login state, we queue them to be processed later.
     */
    @Inject(method = "channelRead0(Lio/netty/channel/ChannelHandlerContext;Lnet/minecraft/network/Packet;)V", at = @At("HEAD"), cancellable = true)
    private void onChannelRead0(ChannelHandlerContext ctx, Packet<?> msg, CallbackInfo ci) {
        if (msg instanceof S3FPacketCustomPayload) {
            S3FPacketCustomPayload packet = (S3FPacketCustomPayload) msg;

            // Get channel name. We try the human-readable name first, then the SRG name.
            // S3FPacketCustomPayload.getChannelName() -> func_149176_c
            String channel = "";
            try {
                channel = packet.getChannelName();
            } catch (Throwable t) {
                try {
                    java.lang.reflect.Method m = packet.getClass().getMethod("func_149176_c");
                    channel = (String) m.invoke(packet);
                } catch (Throwable t2) {
                    // Fail-safe: if we can't get the channel, we don't intercept.
                    return;
                }
            }

            if ("REGISTER".equals(channel) || "UNREGISTER".equals(channel)) {
                net.minecraft.network.INetHandler handler = HousingClient.getMinecraft().getNetHandler();

                // If the handler is null, or if it's a Login handler, we must queue.
                // An early "REGISTER" packet without a set handler yet is common in Bungee
                // transitions.
                boolean isNotReady = (handler == null)
                        || (handler instanceof net.minecraft.network.login.INetHandlerLoginClient)
                        || (handler.getClass().getName().contains("NetHandlerLoginClient"));

                if (isNotReady) {
                    BungeePacketQueue.delayPacketQueue.add(msg);
                    ci.cancel();
                }
            }
        }
    }
}
