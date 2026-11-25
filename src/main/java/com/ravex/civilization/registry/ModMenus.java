package com.ravex.civilization.registry;

import com.ravex.civilization.RavexCiv;
import com.ravex.civilization.npc.NpcMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, RavexCiv.MOD_ID);

    public static final DeferredHolder<MenuType<?>, MenuType<NpcMenu>> NPC =
            MENUS.register("npc", () -> IMenuTypeExtension.create((id, inv, buf) -> new NpcMenu(id, inv, buf)));

    private ModMenus() {}
}
