package betteradvancements.neoforge;

import betteradvancements.common.platform.IEventHelper;
import betteradvancements.common.platform.IPlatformHelper;
import betteradvancements.neoforge.config.ConfigValues;

import java.util.List;

public class NeoForgePlatformHelper implements IPlatformHelper {
    private final NeoForgeEventHelper eventHelper = new NeoForgeEventHelper();
    private final NeoForgeAdvancementVisitor advancementVisitor = new NeoForgeAdvancementVisitor();

    @Override
    public String getPlatformName() {
        return "NeoForge";
    }

    @Override
    public IEventHelper getEventHelper() {
        return eventHelper;
    }

    @Override
    public NeoForgeAdvancementVisitor getAdvancementVisitor() {
        return advancementVisitor;
    }

    @Override
    public void saveCustomTabOrder(List<String> order) {
        ConfigValues.saveCustomTabOrder(order);
    }
}
