package com.ravex.civilization.npc;

import net.minecraft.core.component.DataComponents;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Gives a freshly spawned random NPC a randomized, role-appropriate loadout. Armor and the main-hand tool/weapon are put
 * directly into the real equipment slots (so they are rendered on the model); spare tools and food go in the inventory.
 * Every NPC gets at least a basic pickaxe and axe somewhere, so jobs never fail just because nothing was ever handed out.
 */
public final class NpcOutfit {
    // tiers: 0 leather, 1 chainmail, 2 iron, 3 gold, 4 diamond
    private static final Item[][] ARMOR = {
            {Items.LEATHER_HELMET, Items.LEATHER_CHESTPLATE, Items.LEATHER_LEGGINGS, Items.LEATHER_BOOTS},
            {Items.CHAINMAIL_HELMET, Items.CHAINMAIL_CHESTPLATE, Items.CHAINMAIL_LEGGINGS, Items.CHAINMAIL_BOOTS},
            {Items.IRON_HELMET, Items.IRON_CHESTPLATE, Items.IRON_LEGGINGS, Items.IRON_BOOTS},
            {Items.GOLDEN_HELMET, Items.GOLDEN_CHESTPLATE, Items.GOLDEN_LEGGINGS, Items.GOLDEN_BOOTS},
            {Items.DIAMOND_HELMET, Items.DIAMOND_CHESTPLATE, Items.DIAMOND_LEGGINGS, Items.DIAMOND_BOOTS}};
    private static final EquipmentSlot[] ARMOR_SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
    // tool tiers: 0 wood, 1 stone, 2 iron, 3 diamond
    private static final Item[] SWORDS = {Items.WOODEN_SWORD, Items.STONE_SWORD, Items.IRON_SWORD, Items.DIAMOND_SWORD};
    private static final Item[] AXES = {Items.WOODEN_AXE, Items.STONE_AXE, Items.IRON_AXE, Items.DIAMOND_AXE};
    private static final Item[] PICKAXES = {Items.WOODEN_PICKAXE, Items.STONE_PICKAXE, Items.IRON_PICKAXE, Items.DIAMOND_PICKAXE};
    private static final Item[] SHOVELS = {Items.WOODEN_SHOVEL, Items.STONE_SHOVEL, Items.IRON_SHOVEL, Items.DIAMOND_SHOVEL};
    private static final Item[] HOES = {Items.WOODEN_HOE, Items.STONE_HOE, Items.IRON_HOE, Items.DIAMOND_HOE};

    /** Weighted armor tier roll; -1 means "no armor, plain clothes". */
    static int rollTier(RandomSource r) {
        int v = r.nextInt(100);
        if (v < 12) return -1;
        if (v < 45) return 0;
        if (v < 57) return 1;
        if (v < 85) return 2;
        if (v < 93) return 3;
        return 4;
    }

    public static void apply(NpcEntity npc, String role, RandomSource r) {
        int baseTier = rollTier(r);
        int dye = Mth.hsvToRgb(r.nextFloat(), 0.45F + r.nextFloat() * 0.5F, 0.55F + r.nextFloat() * 0.4F);

        // ---- armor (visible) -------------------------------------------------------------------------------------
        if (baseTier >= 0) {
            int worn = 0;
            for (int i = 0; i < 4; i++) {
                if (r.nextInt(100) >= 85) continue;                    // some pieces missing
                int tier = r.nextInt(100) < 25 ? Math.min(4, Math.max(0, baseTier + r.nextInt(3) - 1)) : baseTier; // mix and match
                ItemStack piece = new ItemStack(ARMOR[tier][i]);
                if (tier == 0) piece.set(DataComponents.DYED_COLOR, new DyedItemColor(dye, true));
                npc.setItemSlot(ARMOR_SLOTS[i], piece);
                worn++;
            }
            if (worn == 0) {
                ItemStack piece = new ItemStack(ARMOR[baseTier][1]);
                if (baseTier == 0) piece.set(DataComponents.DYED_COLOR, new DyedItemColor(dye, true));
                npc.setItemSlot(EquipmentSlot.CHEST, piece);
            }
        }

        // ---- tools/weapons: tier follows armor quality a little --------------------------------------------------
        int tt = Mth.clamp((baseTier < 0 ? 0 : baseTier == 0 ? 0 : baseTier == 1 ? 1 : baseTier == 2 ? 2 : baseTier == 3 ? 1 : 3)
                + r.nextInt(2) - (r.nextInt(3) == 0 ? 1 : 0), 0, 3);
        int low = Math.max(0, tt - 1);
        Item main;
        switch (role) {
            case "lumberjack" -> {
                main = AXES[tt];
                npc.inventory.addItem(new ItemStack(PICKAXES[low]));
            }
            case "miner" -> {
                main = PICKAXES[tt];
                npc.inventory.addItem(new ItemStack(AXES[low]));
            }
            case "farmer" -> {
                main = HOES[Math.min(tt, 2)];
                npc.inventory.addItem(new ItemStack(AXES[low]));
                npc.inventory.addItem(new ItemStack(PICKAXES[low]));
            }
            case "builder" -> {
                main = AXES[tt];
                npc.inventory.addItem(new ItemStack(SHOVELS[low]));
                npc.inventory.addItem(new ItemStack(PICKAXES[low]));
            }
            case "cook" -> {
                main = SWORDS[low];
                npc.inventory.addItem(new ItemStack(AXES[low]));
                npc.inventory.addItem(new ItemStack(PICKAXES[0]));
            }
            default -> { // guard, hunter, explorer, warrior
                main = SWORDS[tt];
                npc.inventory.addItem(new ItemStack(AXES[low]));
                npc.inventory.addItem(new ItemStack(PICKAXES[low]));
            }
        }
        npc.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(main));
        boolean fighter = role.equals("guard") || role.equals("hunter") || role.equals("warrior");
        if (r.nextInt(100) < (fighter ? 70 : 20)) npc.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.SHIELD));
        if (!fighter && r.nextInt(100) < 60) npc.inventory.addItem(new ItemStack(SWORDS[low]));

        // ---- food ------------------------------------------------------------------------------------------------
        npc.inventory.addItem(new ItemStack(Items.BREAD, 2 + r.nextInt(5)));
        if (role.equals("hunter") || role.equals("cook")) npc.inventory.addItem(new ItemStack(Items.COOKED_BEEF, 2 + r.nextInt(4)));
        else if (r.nextInt(3) == 0) npc.inventory.addItem(new ItemStack(Items.APPLE, 1 + r.nextInt(3)));
    }

    private NpcOutfit() {}
}
