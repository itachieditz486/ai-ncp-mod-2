package com.ravex.civilization.npc;

import com.ravex.civilization.registry.ModMenus;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.TieredItem;

/**
 * NPC inventory menu (right-click an NPC). Slots: 0-26 NPC inventory, 27-32 equipment (head, chest, legs, feet,
 * main hand, off hand), 33-68 player inventory. The server menu is backed by the NPC's real inventory and equipment;
 * the client menu uses plain containers that the vanilla menu protocol keeps in sync.
 */
public class NpcMenu extends AbstractContainerMenu {
    public static final int NPC_SLOTS = 27;
    public static final int EQUIP_START = 27;
    public static final int PLAYER_START = 33;
    public static final int PLAYER_END = 69;

    private final NpcEntity npc;

    /** Server side. */
    public NpcMenu(int id, Inventory playerInv, NpcEntity npc) {
        this(id, playerInv, npc, npc.inventory, new NpcEquipmentContainer(npc));
    }

    /** Client side (opened from the network packet). */
    public NpcMenu(int id, Inventory playerInv, RegistryFriendlyByteBuf buf) {
        this(id, playerInv, lookup(playerInv, buf), new SimpleContainer(NPC_SLOTS), new SimpleContainer(6));
    }

    private static NpcEntity lookup(Inventory playerInv, RegistryFriendlyByteBuf buf) {
        Entity e = playerInv.player.level().getEntity(buf.readVarInt());
        return e instanceof NpcEntity n ? n : null;
    }

    private NpcMenu(int id, Inventory playerInv, NpcEntity npc, Container npcInv, Container equip) {
        super(ModMenus.NPC.get(), id);
        this.npc = npc;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(npcInv, col + row * 9, 8 + col * 18, 106 + row * 18));
            }
        }
        addSlot(new EquipSlot(equip, 0, 8, 18, EquipmentSlot.HEAD));
        addSlot(new EquipSlot(equip, 1, 8, 36, EquipmentSlot.CHEST));
        addSlot(new EquipSlot(equip, 2, 8, 54, EquipmentSlot.LEGS));
        addSlot(new EquipSlot(equip, 3, 8, 72, EquipmentSlot.FEET));
        addSlot(new EquipSlot(equip, 4, 152, 36, EquipmentSlot.MAINHAND));
        addSlot(new EquipSlot(equip, 5, 152, 54, EquipmentSlot.OFFHAND));
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInv, col + row * 9 + 9, 8 + col * 18, 170 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInv, col, 8 + col * 18, 228));
        }
    }

    public NpcEntity npc() { return npc; }

    /** Which items may go into which equipment slot. */
    public static boolean fits(EquipmentSlot es, ItemStack s) {
        switch (es) {
            case HEAD, CHEST, LEGS, FEET -> {
                if (s.getItem() instanceof ArmorItem a && a.getType().getSlot() == es) return true;
                return es == EquipmentSlot.CHEST && s.is(Items.ELYTRA);
            }
            default -> {
                return true; // main hand / off hand accept any item (tools, weapons, shields, food...)
            }
        }
    }

    private static class EquipSlot extends Slot {
        private final EquipmentSlot es;

        EquipSlot(Container c, int idx, int x, int y, EquipmentSlot es) {
            super(c, idx, x, y);
            this.es = es;
        }

        @Override
        public boolean mayPlace(ItemStack stack) { return fits(es, stack); }

        @Override
        public int getMaxStackSize() {
            return es.getType() == EquipmentSlot.Type.HUMANOID_ARMOR ? 1 : super.getMaxStackSize();
        }
    }

    @Override
    public void broadcastChanges() {
        if (npc != null && !npc.level().isClientSide) npc.holdForGui();
        super.broadcastChanges();
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();

        if (index < EQUIP_START) {
            if (!moveItemStackTo(stack, PLAYER_START, PLAYER_END, true)) return ItemStack.EMPTY;
        } else if (index < PLAYER_START) {
            if (!moveItemStackTo(stack, PLAYER_START, PLAYER_END, true) && !moveItemStackTo(stack, 0, NPC_SLOTS, false))
                return ItemStack.EMPTY;
        } else {
            boolean moved = false;
            for (int i = EQUIP_START; i < EQUIP_START + 4 && !moved; i++) {
                Slot es = this.slots.get(i);
                if (!es.hasItem() && es.mayPlace(stack)) moved = moveItemStackTo(stack, i, i + 1, false);
            }
            if (!moved && stack.getItem() instanceof ShieldItem) moved = moveItemStackTo(stack, EQUIP_START + 5, EQUIP_START + 6, false);
            if (!moved && stack.getItem() instanceof TieredItem) moved = moveItemStackTo(stack, EQUIP_START + 4, EQUIP_START + 5, false);
            if (!moved && !moveItemStackTo(stack, 0, NPC_SLOTS, false)) return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        if (stack.getCount() == copy.getCount()) return ItemStack.EMPTY;
        slot.onTake(player, stack);
        return copy;
    }

    @Override
    public boolean stillValid(Player player) {
        return npc != null && npc.isAlive() && player.distanceToSqr(npc) < 64.0;
    }
}
