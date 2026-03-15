package com.housingclient.mixin;

import net.minecraftforge.fml.common.network.handshake.FMLHandshakeMessage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

@Mixin(FMLHandshakeMessage.ModList.class)
public class MixinFMLHandshakeMessage {

    @Shadow(remap = false)
    private Map<String, String> modTags;

    @Inject(method = "toBytes", at = @At("HEAD"), cancellable = true, remap = false)
    private void onToBytes(io.netty.buffer.ByteBuf buffer, CallbackInfo ci) {
        if (!net.minecraft.client.Minecraft.getMinecraft().isSingleplayer() && modTags.containsKey("housingclient")) {
            ci.cancel();
            int size = modTags.size() - 1;
            net.minecraftforge.fml.common.network.ByteBufUtils.writeVarInt(buffer, size, 2);
            for (Map.Entry<String, String> modTag : modTags.entrySet()) {
                if (modTag.getKey().equals("housingclient"))
                    continue;
                net.minecraftforge.fml.common.network.ByteBufUtils.writeUTF8String(buffer, modTag.getKey());
                net.minecraftforge.fml.common.network.ByteBufUtils.writeUTF8String(buffer, modTag.getValue());
            }
        }
    }
}
