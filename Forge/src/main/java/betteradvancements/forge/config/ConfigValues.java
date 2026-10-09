package betteradvancements.forge.config;

import betteradvancements.common.advancements.BetterDisplayInfo;
import betteradvancements.common.gui.BetterAdvancementTabType;
import betteradvancements.common.gui.BetterAdvancementsScreen;
import betteradvancements.common.gui.BetterAdvancementTab;
import betteradvancements.common.gui.BetterAdvancementsScreenButton;
import betteradvancements.common.util.ColorHelper;
import betteradvancements.common.util.CriteriaDetail;
import betteradvancements.common.util.CriterionGrid;
import betteradvancements.common.util.TabSortMode;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.ForgeConfigSpec;

import java.util.ArrayList;
import java.util.List;

public class ConfigValues {
    private static final String LEGACY_TAB_SORT_MODE = "LEGACY";

    public static ForgeConfigSpec.ConfigValue<String> defaultUncompletedIconColor;
    public static ForgeConfigSpec.ConfigValue<String> defaultUncompletedTitleColor;
    public static ForgeConfigSpec.ConfigValue<String> defaultCompletedIconColor;
    public static ForgeConfigSpec.ConfigValue<String> defaultCompletedTitleColor;

    public static ForgeConfigSpec.BooleanValue doFade;
    public static ForgeConfigSpec.BooleanValue showDebugCoordinates;
    public static ForgeConfigSpec.BooleanValue orderTabsAlphabetically;
    public static ForgeConfigSpec.ConfigValue<String> tabSortMode;
    public static ForgeConfigSpec.ConfigValue<List<? extends String>> customTabOrder;
    public static ForgeConfigSpec.IntValue uiScaling;

    public static ForgeConfigSpec.ConfigValue<String> detailLevel;
    public static ForgeConfigSpec.BooleanValue requiresShift;
    public static ForgeConfigSpec.BooleanValue sortCriteriaAlphabetically;
    public static ForgeConfigSpec.BooleanValue addToInventory;

    public static ForgeConfigSpec.BooleanValue defaultDrawDirectLines;
    public static ForgeConfigSpec.BooleanValue defaultHideLines;
    public static ForgeConfigSpec.ConfigValue<String> defaultCompletedLineColor;
    public static ForgeConfigSpec.ConfigValue<String>  defaultUncompletedLineColor;

    public static ForgeConfigSpec.BooleanValue onlyUseAboveAdvancementTabs;

    public static ForgeConfigSpec build() {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        defaultUncompletedIconColor = builder.define("defaultUncompletedIconColor", BetterDisplayInfo.defaultMinecraftUncompletedIconColor);
        defaultUncompletedTitleColor = builder.define("defaultUncompletedTitleColor", BetterDisplayInfo.defaultMinecraftUncompletedTitleColor);
        defaultCompletedIconColor = builder.define("defaultCompletedIconColor", BetterDisplayInfo.defaultMinecraftCompletedIconColor);
        defaultCompletedTitleColor = builder.define("defaultCompletedTitleColor", BetterDisplayInfo.defaultMinecraftCompletedTitleColor);

        doFade = builder.define("doAdvancementsBackgroundFade", true);
        showDebugCoordinates = builder.define("showDebugCoordinates", false);
        orderTabsAlphabetically = builder
            .comment("Legacy setting. Prefer tabSortMode; retained to migrate older configs.")
            .define("orderTabsAlphabetically", false);
        List<String> tabSortModes = new ArrayList<>();
        tabSortModes.add(LEGACY_TAB_SORT_MODE);
        tabSortModes.addAll(java.util.Arrays.stream(TabSortMode.values()).map(TabSortMode::name).toList());
        tabSortMode = builder
            .comment(
                "LEGACY follows orderTabsAlphabetically for backwards compatibility.",
                "Set ORIGINAL, ALPHABETICAL, COMPLETION, or CUSTOM to use the new sorter directly.")
            .defineInList("tabSortMode", LEGACY_TAB_SORT_MODE, tabSortModes);
        customTabOrder = builder
            .comment("Persisted advancement root ids used when tabSortMode is CUSTOM.")
            .defineListAllowEmpty("customTabOrder", List.<String>of(), ConfigValues::isValidAdvancementId);
        uiScaling = builder.comment("Values below 50% might give odd results, use on own risk ;)").defineInRange("uiScaling", 100, 1, 100);

        detailLevel = builder.comment(CriteriaDetail.comments()).defineInList("criteriaDetail", CriteriaDetail.DEFAULT.getName(), CriteriaDetail.names());
        requiresShift = builder.define("criteriaDetailRequiresShift", false);
        sortCriteriaAlphabetically = builder
            .comment("Sort displayed advancement criteria alphabetically. When both states are shown, completed criteria are grouped first.")
            .define("sortCriteriaAlphabetically", true);
        addToInventory = builder.define("addInventoryButton", false);

        defaultDrawDirectLines = builder.define("defaultDrawDirectLines", false);
        defaultHideLines = builder.define("defaultHideLines", false);
        defaultCompletedLineColor = builder.define("defaultCompletedLineColor", "#FFFFFF");
        defaultUncompletedLineColor = builder.define("defaultUncompletedLineColor", "#FFFFFF");

        onlyUseAboveAdvancementTabs = builder.define("onlyUseAboveAdvancementTabs", false);

        return builder.build();
    }

    public static void pushChanges() {
        BetterDisplayInfo.defaultUncompletedIconColor = ColorHelper.RGB(defaultUncompletedIconColor.get());
        BetterDisplayInfo.defaultUncompletedTitleColor = ColorHelper.RGB(defaultUncompletedTitleColor.get());
        BetterDisplayInfo.defaultCompletedIconColor = ColorHelper.RGB(defaultCompletedIconColor.get());
        BetterDisplayInfo.defaultCompletedTitleColor = ColorHelper.RGB(defaultCompletedTitleColor.get());

        BetterAdvancementTab.doFade = doFade.get();
        BetterAdvancementsScreen.showDebugCoordinates = showDebugCoordinates.get();
        String configuredTabSortModeName = tabSortMode.get();
        TabSortMode configuredTabSortMode = LEGACY_TAB_SORT_MODE.equalsIgnoreCase(configuredTabSortModeName)
            ? (orderTabsAlphabetically.get() ? TabSortMode.ALPHABETICAL : TabSortMode.ORIGINAL)
            : TabSortMode.fromName(configuredTabSortModeName);
        BetterAdvancementsScreen.setTabSortMode(configuredTabSortMode);
        BetterAdvancementsScreen.setCustomTabOrder(customTabOrder.get());
        BetterAdvancementsScreen.uiScaling = uiScaling.get();

        CriterionGrid.detailLevel = CriteriaDetail.fromName(detailLevel.get());
        CriterionGrid.requiresShift = requiresShift.get();
        CriterionGrid.sortAlphabetically = sortCriteriaAlphabetically.get();
        BetterAdvancementsScreenButton.addToInventory = addToInventory.get();

        BetterDisplayInfo.defaultDrawDirectLines = defaultDrawDirectLines.get();
        BetterDisplayInfo.defaultHideLines = defaultHideLines.get();
        BetterDisplayInfo.defaultCompletedLineColor = ColorHelper.RGB(defaultCompletedLineColor.get());
        BetterDisplayInfo.defaultUncompletedLineColor = ColorHelper.RGB(defaultUncompletedLineColor.get());

        BetterAdvancementTabType.onlyUseAbove = onlyUseAboveAdvancementTabs.get();
    }
    public static void saveCustomTabOrder(List<String> order) {
        customTabOrder.set(new ArrayList<>(order));
        customTabOrder.save();
    }

    private static boolean isValidAdvancementId(Object value) {
        return value instanceof String id
            && id.length() <= 256
            && ResourceLocation.tryParse(id) != null;
    }


}
