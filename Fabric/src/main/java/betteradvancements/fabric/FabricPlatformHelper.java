package betteradvancements.fabric;

import betteradvancements.common.platform.IAdvancementVisitor;
import betteradvancements.common.platform.IEventHelper;
import betteradvancements.common.platform.IPlatformHelper;
import betteradvancements.fabric.config.ConfigFileHandler;

import java.util.List;

public class FabricPlatformHelper implements IPlatformHelper {
    private final FabricEventHelper eventHelper = new FabricEventHelper();
    private final FabricAdvancementVisitor advancementVisitor = new FabricAdvancementVisitor();

    @Override
    public String getPlatformName() {
        return "Fabric";
    }

    @Override
    public IEventHelper getEventHelper() {
        return eventHelper;
    }

    @Override
    public IAdvancementVisitor getAdvancementVisitor() {
        return advancementVisitor;
    }

    @Override
    public void saveCustomTabOrder(List<String> order) {
        ConfigFileHandler.writeToConfig();
    }
}
