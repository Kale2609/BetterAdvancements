package betteradvancements.neoforge.config;

import betteradvancements.common.advancements.BetterDisplayInfo;
import betteradvancements.common.gui.BetterAdvancementTabType;
import betteradvancements.common.gui.BetterAdvancementsScreen;
import betteradvancements.common.gui.BetterAdvancementTab;
import betteradvancements.common.gui.BetterAdvancementsScreenButton;
import betteradvancements.common.util.ColorHelper;
import betteradvancements.common.util.CriteriaDetail;
import betteradvancements.common.util.CriterionGrid;
import net.neoforged.neoforge.common.ModConfigSpec;

public class ConfigValues {

    public static ModConfigSpec.ConfigValue<String> defaultUncompletedIconColor;
    public static ModConfigSpec.ConfigValue<String> defaultUncompletedTitleColor;
    public static ModConfigSpec.ConfigValue<String> defaultCompletedIconColor;
    public static ModConfigSpec.ConfigValue<String> defaultCompletedTitleColor;

    public static ModConfigSpec.BooleanValue doFade;
    public static ModConfigSpec.BooleanValue showDebugCoordinates;
    public static ModConfigSpec.BooleanValue orderTabsAlphabetically;
    public static ModConfigSpec.IntValue uiScaling;

    public static ModConfigSpec.ConfigValue<String> detailLevel;
    public static ModConfigSpec.BooleanValue requiresShift;
    public static ModConfigSpec.BooleanValue addToInventory;

    public static ModConfigSpec.BooleanValue defaultDrawDirectLines;
    public static ModConfigSpec.BooleanValue defaultHideLines;
    public static ModConfigSpec.ConfigValue<String> defaultCompletedLineColor;
    public static ModConfigSpec.ConfigValue<String>  defaultUncompletedLineColor;

    public static ModConfigSpec.BooleanValue onlyUseAboveAdvancementTabs;
    public static ModConfigSpec.BooleanValue backgroundProgressEnabled;
    public static ModConfigSpec.ConfigValue<String> backgroundProgressMode;
    public static ModConfigSpec.ConfigValue<String> backgroundProgressDirection;
    public static ModConfigSpec.ConfigValue<String> backgroundProgressColor;
    public static ModConfigSpec.ConfigValue<String> backgroundCompletedColor;

    public static ModConfigSpec build() {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        defaultUncompletedIconColor = builder.define("defaultUncompletedIconColor", BetterDisplayInfo.defaultMinecraftUncompletedIconColor);
        defaultUncompletedTitleColor = builder.define("defaultUncompletedTitleColor", BetterDisplayInfo.defaultMinecraftUncompletedTitleColor);
        defaultCompletedIconColor = builder.define("defaultCompletedIconColor", BetterDisplayInfo.defaultMinecraftCompletedIconColor);
        defaultCompletedTitleColor = builder.define("defaultCompletedTitleColor", BetterDisplayInfo.defaultMinecraftCompletedTitleColor);

        doFade = builder.define("doAdvancementsBackgroundFade", true);
        showDebugCoordinates = builder.define("showDebugCoordinates", false);
        orderTabsAlphabetically = builder.define("orderTabsAlphabetically", false);
        uiScaling = builder.comment("Values below 50% might give odd results, use on own risk ;)").defineInRange("uiScaling", 100, 1, 100);

        detailLevel = builder.comment(CriteriaDetail.comments()).defineInList("criteriaDetail", CriteriaDetail.DEFAULT.getName(), CriteriaDetail.names());
        requiresShift = builder.define("criteriaDetailRequiresShift", false);
        addToInventory = builder.define("addInventoryButton", false);

        defaultDrawDirectLines = builder.define("defaultDrawDirectLines", false);
        defaultHideLines = builder.define("defaultHideLines", false);
        defaultCompletedLineColor = builder.define("defaultCompletedLineColor", "#FFFFFF");
        defaultUncompletedLineColor = builder.define("defaultUncompletedLineColor", "#FFFFFF");

        onlyUseAboveAdvancementTabs = builder.define("onlyUseAboveAdvancementTabs", false);
        backgroundProgressEnabled = builder
            .comment("Enable completion-based coloring of advancement tab buttons.")
            .define("backgroundProgressEnabled", BetterAdvancementTab.DEFAULT_BACKGROUND_PROGRESS_ENABLED);
        backgroundProgressMode = builder
            .comment("OFF, COMPLETION_COLOR, or PROGRESS_FILL.")
            .defineInList(
                "backgroundProgressMode",
                BetterAdvancementTab.DEFAULT_BACKGROUND_PROGRESS_MODE.name(),
                java.util.Arrays.stream(BetterAdvancementTab.BackgroundProgressMode.values())
                    .map(BetterAdvancementTab.BackgroundProgressMode::name)
                    .toList());
        backgroundProgressDirection = builder
            .comment("LEFT_TO_RIGHT, RIGHT_TO_LEFT, TOP_TO_BOTTOM, or BOTTOM_TO_TOP.")
            .defineInList(
                "backgroundProgressDirection",
                BetterAdvancementTab.DEFAULT_BACKGROUND_PROGRESS_DIRECTION.name(),
                java.util.Arrays.stream(BetterAdvancementTab.BackgroundProgressDirection.values())
                    .map(BetterAdvancementTab.BackgroundProgressDirection::name)
                    .toList());
        backgroundProgressColor = builder
            .comment("ARGB tint color for partial tab-button progress, in #AARRGGBB format.")
            .define(
                "backgroundProgressColor",
                ColorHelper.asARGBString(BetterAdvancementTab.DEFAULT_BACKGROUND_PROGRESS_COLOR),
                ConfigValues::isValidArgb);
        backgroundCompletedColor = builder
            .comment("ARGB tint color used when the tab is fully completed, in #AARRGGBB format.")
            .define(
                "backgroundCompletedColor",
                ColorHelper.asARGBString(BetterAdvancementTab.DEFAULT_BACKGROUND_COMPLETED_COLOR),
                ConfigValues::isValidArgb);

        return builder.build();
    }

    public static void pushChanges() {
        BetterDisplayInfo.defaultUncompletedIconColor = ColorHelper.RGB(defaultUncompletedIconColor.get());
        BetterDisplayInfo.defaultUncompletedTitleColor = ColorHelper.RGB(defaultUncompletedTitleColor.get());
        BetterDisplayInfo.defaultCompletedIconColor = ColorHelper.RGB(defaultCompletedIconColor.get());
        BetterDisplayInfo.defaultCompletedTitleColor = ColorHelper.RGB(defaultCompletedTitleColor.get());

        BetterAdvancementTab.doFade = doFade.get();
        BetterAdvancementsScreen.showDebugCoordinates = showDebugCoordinates.get();
        BetterAdvancementsScreen.orderTabsAlphabetically = orderTabsAlphabetically.get();
        BetterAdvancementsScreen.uiScaling = uiScaling.get();

        CriterionGrid.detailLevel = CriteriaDetail.fromName(detailLevel.get());
        CriterionGrid.requiresShift = requiresShift.get();
        BetterAdvancementsScreenButton.addToInventory = addToInventory.get();

        BetterDisplayInfo.defaultDrawDirectLines = defaultDrawDirectLines.get();
        BetterDisplayInfo.defaultHideLines = defaultHideLines.get();
        BetterDisplayInfo.defaultCompletedLineColor = ColorHelper.RGB(defaultCompletedLineColor.get());
        BetterDisplayInfo.defaultUncompletedLineColor = ColorHelper.RGB(defaultUncompletedLineColor.get());

        BetterAdvancementTabType.onlyUseAbove = onlyUseAboveAdvancementTabs.get();
        BetterAdvancementTab.backgroundProgressEnabled = backgroundProgressEnabled.get();
        BetterAdvancementTab.backgroundProgressMode =
            BetterAdvancementTab.BackgroundProgressMode.fromName(backgroundProgressMode.get());
        BetterAdvancementTab.backgroundProgressDirection =
            BetterAdvancementTab.BackgroundProgressDirection.fromName(backgroundProgressDirection.get());
        BetterAdvancementTab.backgroundProgressColor = ColorHelper.ARGB(backgroundProgressColor.get());
        BetterAdvancementTab.backgroundCompletedColor = ColorHelper.ARGB(backgroundCompletedColor.get());
    }

    private static boolean isValidArgb(Object value) {
        return value instanceof String colour && ColorHelper.isARGB(colour);
    }
}
