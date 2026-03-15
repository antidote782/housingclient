package com.housingclient.mixin;

import com.housingclient.HousingClient;
import com.housingclient.module.modules.miscellaneous.AutoBegModule;

import net.minecraft.nbt.NBTTagList;
import org.spongepowered.asm.mixin.Mixin;

import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.client.gui.GuiScreenBook")
public class MixinGuiScreenBook {

    @Inject(method = "func_73866_w_", at = @At("RETURN"), remap = false)
    public void onInitGui(CallbackInfo ci) {
        try {
            AutoBegModule autobeg = HousingClient.getInstance().getModuleManager().getModule(AutoBegModule.class);
            if (autobeg != null && autobeg.isEnabled()) {
                // Find the NBTTagList field by type (more robust than name in obfuscated
                // environments)
                NBTTagList bookPages = null;
                for (java.lang.reflect.Field f : this.getClass().getDeclaredFields()) {
                    if (f.getType().equals(NBTTagList.class)) {
                        f.setAccessible(true);
                        bookPages = (NBTTagList) f.get(this);
                        break;
                    }
                }

                if (bookPages == null) {
                    // Try superclass if not found in current (rare for this class but safe)
                    for (java.lang.reflect.Field f : this.getClass().getSuperclass().getDeclaredFields()) {
                        if (f.getType().equals(NBTTagList.class)) {
                            f.setAccessible(true);
                            bookPages = (NBTTagList) f.get(this);
                            break;
                        }
                    }
                }

                if (bookPages != null) {
                    autobeg.checkAndAcceptGift(bookPages);
                }
            }
        } catch (Exception e) {
            // Ignored
        }
    }
}
