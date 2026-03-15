package com.housingclient.mixin;

import com.housingclient.HousingClient;
import com.housingclient.module.modules.exploit.SignFillModule;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiEditSign;
import net.minecraft.tileentity.TileEntitySign;
import net.minecraft.util.ChatComponentText;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiEditSign.class)
public abstract class MixinGuiEditSign extends GuiScreen {

    @Shadow
    private TileEntitySign field_146848_f; // tileSign

    @Shadow
    private int field_146851_h; // editLine

    /**
     * Intercept the sign GUI initialization. If Sign Fill is enabled,
     * write the configured text into the sign and close it immediately.
     * 
     * This uses initGui because it fires after the TileEntitySign is set
     * but before the GUI is displayed. By writing to signText[] and then
     * calling the done method (which sends C12PacketUpdateSign), the sign
     * is updated through the normal vanilla flow — no custom packets needed.
     */
    @Inject(method = "func_73866_w_", at = @At("RETURN"))
    private void onInitGui(CallbackInfo ci) {
        if (HousingClient.getInstance() == null || HousingClient.getInstance().getModuleManager() == null) {
            return;
        }

        SignFillModule signFill = HousingClient.getInstance().getModuleManager().getModule(SignFillModule.class);
        if (signFill == null || !signFill.isEnabled()) {
            return;
        }

        TileEntitySign sign = this.field_146848_f;
        if (sign == null) {
            return;
        }

        // Write configured text to the sign tile entity
        for (int i = 0; i < 4; i++) {
            String lineText = signFill.getLine(i);
            if (lineText == null)
                lineText = "";
            sign.signText[i] = new ChatComponentText(lineText);
        }

        // Mark the sign as finalized and send the update packet
        // This mirrors what happens when the player presses "Done" in vanilla:
        // - sign.markDirty() is called
        // - C12PacketUpdateSign is sent with the signText
        // - The GUI closes
        sign.markDirty();

        // Use mc.addScheduledTask to close the GUI on the next tick,
        // ensuring the init sequence completes cleanly before closing
        mc.addScheduledTask(new Runnable() {
            @Override
            public void run() {
                // This triggers GuiEditSign.onGuiClosed() which sends C12PacketUpdateSign
                mc.displayGuiScreen(null);
            }
        });
    }

    @Inject(method = "func_73869_a", at = @At("HEAD"), cancellable = true)
    private void onKeyTyped(char typedChar, int keyCode, CallbackInfo ci) {
        // Ctrl+V or Cmd+V paste support
        if (isCtrlKeyDown() && (keyCode == 47)) { // 47 = V key
            String clipboard = getClipboardString();
            if (clipboard != null && !clipboard.isEmpty()) {
                TileEntitySign sign = this.field_146848_f;
                int line = this.field_146851_h;
                if (sign != null && line >= 0 && line < 4) {
                    String current = sign.signText[line] != null
                            ? sign.signText[line].getUnformattedText()
                            : "";
                    // Remove newlines from clipboard, take first line only
                    String pasteText = clipboard.split("\n")[0].split("\r")[0];
                    String newText = current + pasteText;
                    // Sign line limit is 15 characters in vanilla, but we allow any text
                    if (newText.length() > 90) {
                        newText = newText.substring(0, 90);
                    }
                    sign.signText[line] = new ChatComponentText(newText);
                }
                ci.cancel();
            }
        }
    }
}
