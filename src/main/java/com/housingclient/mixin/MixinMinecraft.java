package com.housingclient.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerControllerMP;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Minecraft.class)
public class MixinMinecraft {

    @Redirect(method = "func_71407_l", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/PlayerControllerMP;func_78765_e()V"))
    private void nullCheckUpdateController(PlayerControllerMP instance) {
        if (Minecraft.getMinecraft().thePlayer != null) {
            instance.updateController();
        }
    }
}
