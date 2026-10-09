package betteradvancements.common.platform;

import java.util.List;

public interface IPlatformHelper {
    String getPlatformName();

    IEventHelper getEventHelper();

    IAdvancementVisitor getAdvancementVisitor();

    void saveCustomTabOrder(List<String> order);
}
