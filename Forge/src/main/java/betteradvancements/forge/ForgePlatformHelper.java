package betteradvancements.forge;

import betteradvancements.common.platform.IEventHelper;
import betteradvancements.common.platform.IPlatformHelper;
import betteradvancements.forge.config.ConfigValues;

import java.util.List;

public class ForgePlatformHelper implements IPlatformHelper {
    private final ForgeEventHelper eventHelper = new ForgeEventHelper();
    private final ForgeAdvancementVisitor advancementVisitor = new ForgeAdvancementVisitor();

    @Override
    public String getPlatformName() {
        return "Forge";
    }

    @Override
    public IEventHelper getEventHelper() {
        return eventHelper;
    }

    @Override
    public ForgeAdvancementVisitor getAdvancementVisitor() {
        return advancementVisitor;
    }

    @Override
    public void saveCustomTabOrder(List<String> order) {
        ConfigValues.saveCustomTabOrder(order);
    }
}
