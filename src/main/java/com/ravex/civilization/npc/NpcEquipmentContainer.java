package com.ravex.civilization.npc;

import net.minecraft.world.Container;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Exposes the NPC's real equipment slots (head, chest, legs, feet, main hand, off hand) as a Container for the menu. */
public class NpcEquipmentContainer implements Container {
    public static final EquipmentSlot[] SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS,
            EquipmentSlot.FEET, EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND};

    private final NpcEntity npc;

    public NpcEquipmentContainer(NpcEntity npc) {
        this.npc = npc;
    }

    @Override
    public int getContainerSize() { return SLOTS.length; }

    @Override
    public boolean isEmpty() {
        for (EquipmentSlot s : SLOTS) if (!npc.getItemBySlot(s).isEmpty()) return false;
        return true;
    }

    @Override
    public ItemStack getItem(int i) { return npc.getItemBySlot(SLOTS[i]); }

    @Override
    public ItemStack removeItem(int i, int count) {
        ItemStack cur = npc.getItemBySlot(SLOTS[i]);
        if (cur.isEmpty()) return ItemStack.EMPTY;
        ItemStack out = cur.split(count);
        if (cur.isEmpty()) npc.setItemSlot(SLOTS[i], ItemStack.EMPTY);
        return out;
    }

    @Override
    public ItemStack removeItemNoUpdate(int i) {
        ItemStack cur = npc.getItemBySlot(SLOTS[i]);
        npc.setItemSlot(SLOTS[i], ItemStack.EMPTY);
        return cur;
    }

    @Override
    public void setItem(int i, ItemStack stack) { npc.setItemSlot(SLOTS[i], stack); }

    @Override
    public void setChanged() {}

    @Override
    public boolean stillValid(Player player) { return npc.isAlive() && player.distanceToSqr(npc) < 64.0; }

    @Override
    public void clearContent() {
        for (EquipmentSlot s : SLOTS) npc.setItemSlot(s, ItemStack.EMPTY);
    }
}
