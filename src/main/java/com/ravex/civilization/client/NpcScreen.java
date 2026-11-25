package com.ravex.civilization.client;

import com.ravex.civilization.npc.NpcEntity;
import com.ravex.civilization.npc.NpcMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

/** Right-click inventory screen for NPCs (drawn with plain fills, no extra texture assets). */
public class NpcScreen extends AbstractContainerScreen<NpcMenu> {
    public NpcScreen(NpcMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 176;
        this.imageHeight = 252;
        this.inventoryLabelY = 160;
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos, y = this.topPos;
        g.fill(x, y, x + imageWidth, y + imageHeight, 0xFF555555);
        g.fill(x + 1, y + 1, x + imageWidth - 1, y + imageHeight - 1, 0xFFC6C6C6);
        for (Slot s : this.menu.slots) {
            g.fill(x + s.x - 1, y + s.y - 1, x + s.x + 17, y + s.y + 17, 0xFF373737);
            g.fill(x + s.x, y + s.y, x + s.x + 16, y + s.y + 16, 0xFF8B8B8B);
        }
        NpcEntity npc = this.menu.npc();
        if (npc != null) {
            g.fill(x + 30, y + 16, x + 146, y + 92, 0xFF202020);
            InventoryScreen.renderEntityInInventoryFollowsMouse(g, x + 31, y + 17, x + 145, y + 91, 32, 0.0625F,
                    mouseX, mouseY, npc);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(this.font, this.title, 8, 6, 0x404040, false);
        g.drawString(this.font, "Inventory", 8, 95, 0x404040, false);
        g.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 0x404040, false);
        NpcEntity npc = this.menu.npc();
        if (npc != null) {
            g.drawString(this.font, String.format("HP %.0f/%.0f", npc.getHealth(), npc.getMaxHealth()), 100, 6, 0x802020, false);
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        this.renderTooltip(g, mouseX, mouseY);
    }
}
