package com.housingclient.module.modules.visuals;

import com.housingclient.module.Category;
import com.housingclient.module.Module;
import com.housingclient.module.ModuleMode;
import com.housingclient.module.settings.BooleanSetting;
import com.housingclient.module.settings.NumberSetting;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import net.minecraft.network.play.client.C10PacketCreativeInventoryAction;

import java.awt.Color;

/**
 * Rainbow Armor Module - Smooth rainbow gradient on leather armor.
 * 
 * Only available in Creative mode. Sends C10PacketCreativeInventory
 * to set dyed leather armor in the player's equipment slots.
 * 
 * Rate-limited to avoid packet kicks. Uses minimal packets:
 * only 4 packets per update cycle (one per armor slot).
 * 
 * Armor slot IDs for C10PacketCreativeInventory:
 *   5 = Helmet, 6 = Chestplate, 7 = Leggings, 8 = Boots
 */
public class RainbowArmorModule extends Module {

    private final NumberSetting speed = new NumberSetting("Speed", "Color cycle speed", 1.0, 0.1, 5.0, 0.1);
    private final NumberSetting updateRate = new NumberSetting("Update Rate", "Ticks between color updates", 5, 1, 20);
    private final BooleanSetting excludeHelmet = new BooleanSetting("Exclude Helmet Slot",
            "Don't change the helmet slot when enabled", false);

    /** Current base hue (0.0 - 1.0) */
    private float hue = 0.0f;

    /** Tick counter for rate limiting */
    private int tickCounter = 0;

    /** Last applied color per slot to avoid unnecessary packets */
    private final int[] lastColor = new int[4];

    /** Leather armor item IDs */
    private static final String[] ARMOR_ITEMS = {
            "minecraft:leather_helmet",
            "minecraft:leather_chestplate",
            "minecraft:leather_leggings",
            "minecraft:leather_boots"
    };

    /** Armor slot IDs for C10PacketCreativeInventory (5=helmet, 6=chest, 7=legs, 8=boots) */
    private static final int[] ARMOR_SLOTS = { 5, 6, 7, 8 };

    /** Hue offset per armor piece for gradient effect (evenly spaced) */
    private static final float[] HUE_OFFSETS = { 0.0f, 0.08f, 0.16f, 0.24f };

    public RainbowArmorModule() {
        super("Rainbow Armor", "Smooth rainbow leather armor (Creative only)", Category.MISCELLANEOUS, ModuleMode.BOTH);
        addSetting(speed);
        addSetting(updateRate);
        addSetting(excludeHelmet);

        // Initialize last colors to -1 (force first update)
        for (int i = 0; i < lastColor.length; i++) {
            lastColor[i] = -1;
        }
    }

    @Override
    public boolean isAvailable() {
        return mc.thePlayer != null && mc.thePlayer.capabilities.isCreativeMode;
    }

    @Override
    protected void onEnable() {
        if (mc.thePlayer == null || !mc.thePlayer.capabilities.isCreativeMode) {
            if (mc.thePlayer != null) {
                mc.thePlayer.addChatMessage(new net.minecraft.util.ChatComponentText(
                        "\u00A7c\u00A7lRainbow Armor \u00A7r\u00A7cYou must be in Creative mode to use this module."));
            }
            setEnabled(false);
            return;
        }

        // Reset state
        hue = 0.0f;
        tickCounter = 0;
        for (int i = 0; i < lastColor.length; i++) {
            lastColor[i] = -1;
        }

        // Immediately equip armor with initial colors
        sendArmorUpdate();
    }

    @Override
    protected void onDisable() {
        if (mc.thePlayer == null || mc.getNetHandler() == null) {
            return;
        }

        // Only clear armor if we're still in creative mode
        if (mc.thePlayer.capabilities.isCreativeMode) {
            // Clear all armor slots by sending empty stacks
            for (int i = 0; i < ARMOR_SLOTS.length; i++) {
                // If excluding helmet, don't clear slot 0 (Helmet)
                if (i == 0 && excludeHelmet.isEnabled()) continue;

                try {
                    mc.getNetHandler().addToSendQueue(new C10PacketCreativeInventoryAction(ARMOR_SLOTS[i], null));
                } catch (Exception e) {
                    // Ignore
                }
            }
        }
    }

    @Override
    public void onTick() {
        if (mc.thePlayer == null || mc.theWorld == null) {
            return;
        }

        // Disable if no longer in creative
        if (!mc.thePlayer.capabilities.isCreativeMode) {
            setEnabled(false);
            return;
        }

        // Rate limit updates
        tickCounter++;
        if (tickCounter < updateRate.getIntValue()) {
            return;
        }
        tickCounter = 0;

        // Advance hue based on speed
        float hueStep = speed.getFloatValue() * 0.02f; // Scale speed to a reasonable hue increment
        hue += hueStep;
        if (hue >= 1.0f) {
            hue -= 1.0f;
        }

        sendArmorUpdate();
    }

    /**
     * Sends armor color update packets. Only sends packets for slots
     * whose color actually changed to minimize packet load.
     */
    private void sendArmorUpdate() {
        if (mc.getNetHandler() == null) {
            return;
        }

        for (int i = 0; i < 4; i++) {
            // Skip helmet if exclusion is enabled
            if (i == 0 && excludeHelmet.isEnabled()) {
                continue;
            }

            // Compatibility with Player Crasher: Skip Helmet (index 0) or Boots (index 3) if Player Crasher is flashing
            if (i == 0 || i == 3) {
                com.housingclient.module.modules.exploit.PlayerCrasherModule crasher = 
                        com.housingclient.HousingClient.getInstance().getModuleManager().getModule(com.housingclient.module.modules.exploit.PlayerCrasherModule.class);
                if (crasher != null && crasher.isFlashing()) {
                    continue;
                }
            }

            // Calculate color for this armor piece with gradient offset
            float pieceHue = (hue + HUE_OFFSETS[i]) % 1.0f;
            int rgb = Color.HSBtoRGB(pieceHue, 1.0f, 1.0f) & 0x00FFFFFF; // Strip alpha

            // Skip if color hasn't changed
            if (rgb == lastColor[i]) {
                continue;
            }
            lastColor[i] = rgb;

            // Create dyed leather armor ItemStack
            ItemStack armorStack = createDyedArmor(i, rgb);
            if (armorStack == null) continue;

            try {
                mc.getNetHandler().addToSendQueue(new C10PacketCreativeInventoryAction(ARMOR_SLOTS[i], armorStack));
            } catch (Exception e) {
                // Ignore packet errors
            }
        }
    }

    /**
     * Creates a leather armor ItemStack with the specified dye color.
     * Uses vanilla NBT structure: {display:{color:RGB_INT}}
     */
    private ItemStack createDyedArmor(int armorIndex, int color) {
        // Get the item by resource name
        net.minecraft.item.Item item = net.minecraft.item.Item.getByNameOrId(ARMOR_ITEMS[armorIndex]);
        if (item == null) {
            return null;
        }

        ItemStack stack = new ItemStack(item);

        // Set leather dye color via NBT: {display:{color:INT}}
        NBTTagCompound display = new NBTTagCompound();
        display.setInteger("color", color);
        stack.setTagInfo("display", display);

        return stack;
    }

    @Override
    public String getDisplayInfo() {
        return null;
    }

    @Override
    protected boolean shouldShowNotification() {
        return true;
    }
}
