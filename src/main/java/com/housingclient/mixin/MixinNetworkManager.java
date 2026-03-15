package com.housingclient.mixin;

import com.housingclient.HousingClient;

import com.housingclient.module.modules.exploit.BlinkModule;
import com.housingclient.module.modules.exploit.PacketMultiplierModule;
import com.housingclient.module.modules.visuals.FreeCamModule;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.C0BPacketEntityAction;
import net.minecraft.network.play.client.C02PacketUseEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(NetworkManager.class)
public class MixinNetworkManager {

    /**
     * Intercepts OUTGOING packets.
     * SRG: func_179290_a (sendPacket)
     */
    @Inject(method = "func_179290_a", at = @At("HEAD"), cancellable = true)
    private void onSendPacket(Packet<?> packet, CallbackInfo ci) {

        // Blink
        try {
            BlinkModule blink = HousingClient.getInstance().getModuleManager().getModule(BlinkModule.class);
            if (blink != null && BlinkModule.shouldCancelPacket(packet)) {
                ci.cancel();
            }
        } catch (Throwable t) {
            // System.err.println("[HousingClient] Blink Mixin Error: " + t.getMessage());
        }

        // Packet Multiplier - multiply GUI click packets
        try {
            if (PacketMultiplierModule.shouldMultiplyPacket(packet)) {
                ci.cancel();
            }
        } catch (Throwable t) {
            // ignore
        }

        // FreeCam - suppress sneak packets so server doesn't see camera movement
        // sneaking
        try {
            if (packet instanceof C0BPacketEntityAction) {
                FreeCamModule freeCam = HousingClient.getInstance().getModuleManager().getModule(FreeCamModule.class);
                if (freeCam != null && freeCam.isEnabled()) {
                    C0BPacketEntityAction actionPacket = (C0BPacketEntityAction) packet;
                    C0BPacketEntityAction.Action action = actionPacket.getAction();
                    if (action == C0BPacketEntityAction.Action.START_SNEAKING
                            || action == C0BPacketEntityAction.Action.STOP_SNEAKING) {
                        ci.cancel();
                    }
                }
            }
        } catch (Throwable t) {
            // ignore
        }

        // FreeCam - suppress self-interaction kicks
        try {
            if (packet instanceof C02PacketUseEntity) {
                FreeCamModule freeCam = HousingClient.getInstance().getModuleManager().getModule(FreeCamModule.class);
                if (freeCam != null && freeCam.isEnabled()) {
                    C02PacketUseEntity usePacket = (C02PacketUseEntity) packet;
                    int targetId = -1;

                    try {
                        // SRG mapping for entityId
                        java.lang.reflect.Field f = usePacket.getClass().getDeclaredField("field_149567_a");
                        f.setAccessible(true);
                        targetId = f.getInt(usePacket);
                    } catch (Exception e1) {
                        try {
                            // Deobfuscated fallback
                            java.lang.reflect.Field f = usePacket.getClass().getDeclaredField("entityId");
                            f.setAccessible(true);
                            targetId = f.getInt(usePacket);
                        } catch (Exception e2) {
                            // ignore
                        }
                    }

                    if (targetId != -1 && targetId == HousingClient.getMinecraft().thePlayer.getEntityId()) {
                        ci.cancel();
                    }
                }
            }
        } catch (Throwable t) {
            // ignore
        }
    }

    @Inject(method = "channelRead0(Lio/netty/channel/ChannelHandlerContext;Lnet/minecraft/network/Packet;)V", at = @At("HEAD"), cancellable = true, remap = false)
    private void onChannelRead0(io.netty.channel.ChannelHandlerContext context, Packet<?> packet, CallbackInfo ci) {
        NetworkManager manager = (NetworkManager) (Object) this;
        final net.minecraft.network.INetHandler handler = manager.getNetHandler();

        // If we receive a PLAY packet but the client is still strictly in LOGIN state
        // due to Forge/Bungee desync
        if (handler instanceof net.minecraft.client.network.NetHandlerLoginClient) {
            String className = packet.getClass().getName();

            // Catch Vanilla Server Play Packets
            if (className.startsWith("net.minecraft.network.play.server.")) {
                com.housingclient.utils.BungeePacketQueue.delayPacketQueue.add(packet);
                ci.cancel();
            }
            // Catch Forge Custom Payloads (Plugin Messages) that trigger
            // ClientCustomPacketEvent which casts to NetHandlerPlayClient!
            else if (className.equals("net.minecraftforge.fml.common.network.internal.FMLProxyPacket")) {
                net.minecraftforge.fml.common.network.internal.FMLProxyPacket proxyPacket = (net.minecraftforge.fml.common.network.internal.FMLProxyPacket) packet;
                String channel = proxyPacket.channel();

                // Do NOT block the Forge Handshake Channels! Those are required to finish the
                // login!
                if (!"FML|HS".equals(channel)) {
                    com.housingclient.utils.BungeePacketQueue.delayPacketQueue.add(packet);
                    ci.cancel();
                }
            }
        }
    }
}
