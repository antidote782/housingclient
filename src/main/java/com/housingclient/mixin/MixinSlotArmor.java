package com.housingclient.mixin;

import com.housingclient.HousingClient;
import com.housingclient.module.modules.exploit.WearableItemsModule;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// Targets the anonymous Slot subclass for armor slots inside ContainerPlayer
@Mixin(targets = "net.minecraft.inventory.ContainerPlayer$1")
public class MixinSlotArmor {

    @Inject(method = { "isItemValid", "func_75214_a" }, at = @At("HEAD"), cancellable = true)
    private void onIsItemValid(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        WearableItemsModule wearable = HousingClient.getInstance().getModuleManager()
                .getModule(WearableItemsModule.class);
        if (wearable != null && wearable.isEnabled()) {
            cir.setReturnValue(true);
        }
    }
}
