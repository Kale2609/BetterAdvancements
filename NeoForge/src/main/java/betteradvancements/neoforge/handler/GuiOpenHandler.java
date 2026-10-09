package betteradvancements.neoforge.handler;

import betteradvancements.common.gui.BetterAdvancementsScreen;
import betteradvancements.common.gui.BetterAdvancementsScreenButton;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.advancements.AdvancementsScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;


public class GuiOpenHandler {
    public static final GuiOpenHandler instance = new GuiOpenHandler();

    private GuiOpenHandler() {

    }

    @SubscribeEvent
    public void onGuiOpen(ScreenEvent.Opening event) {
        if (event.getScreen() instanceof AdvancementsScreen) {
            event.setCanceled(true);
            Minecraft mc = Minecraft.getInstance();
            mc.setScreen(new BetterAdvancementsScreen(mc.player.connection.getAdvancements()));
        }
    }

    @SubscribeEvent
    public void onGuiOpened(final ScreenEvent.Init.Post event) {
        if (event.getScreen() instanceof InventoryScreen) {
            if (BetterAdvancementsScreenButton.addToInventory) {
                InventoryScreen guiInventory = (InventoryScreen) event.getScreen();
                event.addListener(new BetterAdvancementsScreenButton(guiInventory.getGuiLeft() + guiInventory.getXSize(), guiInventory.getGuiTop(), Component.literal("BA")));
            }
        }
    }
}
