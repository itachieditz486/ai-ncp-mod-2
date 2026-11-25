package com.ravex.civilization.client;

import com.ravex.civilization.registry.ModEntities;
import com.ravex.civilization.registry.ModMenus;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

public final class ClientSetup {
    public static void init(IEventBus modBus, ModContainer container) {
        modBus.addListener(ClientSetup::registerRenderers);
        modBus.addListener(ClientSetup::registerScreens);
        // Mods list > RAVEX AI Civilization > Config
        container.registerExtensionPoint(IConfigScreenFactory.class, (mod, parent) -> new RavexConfigScreen(parent));
    }

    private static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.NPC.get(), NpcRenderer::new);
    }

    private static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenus.NPC.get(), NpcScreen::new);
    }

    private ClientSetup() {}
}
