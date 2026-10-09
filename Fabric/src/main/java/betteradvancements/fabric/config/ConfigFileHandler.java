package betteradvancements.fabric.config;

import betteradvancements.common.advancements.BetterDisplayInfo;
import betteradvancements.common.gui.BetterAdvancementTab;
import betteradvancements.common.gui.BetterAdvancementTabType;
import betteradvancements.common.gui.BetterAdvancementsScreen;
import betteradvancements.common.gui.BetterAdvancementsScreenButton;
import betteradvancements.common.reference.Constants;
import betteradvancements.common.util.ColorHelper;
import betteradvancements.common.util.CriteriaDetail;
import betteradvancements.common.util.CriterionGrid;
import betteradvancements.common.util.TabSortMode;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class ConfigFileHandler {
    public static void readFromConfig() {
        JsonObject root = new JsonObject();
        try {
            File configFile = getConfigFile();
            if (!configFile.exists()) {
                writeToConfig();
                configFile = getConfigFile();
            }
            root = JsonParser.parseReader(new FileReader(configFile)).getAsJsonObject();
        } catch (IOException e) {
            Constants.log.error(e);
        }

        if (root.has("defaultUncompletedIconColor")) {
            BetterDisplayInfo.defaultUncompletedIconColor = ColorHelper.RGB(root.get("defaultUncompletedIconColor").getAsString());
        }
        if (root.has("defaultUncompletedTitleColor")) {
            BetterDisplayInfo.defaultUncompletedTitleColor = ColorHelper.RGB(root.get("defaultUncompletedTitleColor").getAsString());
        }
        if (root.has("defaultCompletedIconColor")) {
            BetterDisplayInfo.defaultCompletedIconColor = ColorHelper.RGB(root.get("defaultCompletedIconColor").getAsString());
        }
        if (root.has("defaultCompletedTitleColor")) {
            BetterDisplayInfo.defaultCompletedTitleColor = ColorHelper.RGB(root.get("defaultCompletedTitleColor").getAsString());
        }
        if (root.has("doAdvancementsBackgroundFade")) {
            BetterAdvancementTab.doFade = root.get("doAdvancementsBackgroundFade").getAsBoolean();
        }
        if (root.has("showDebugCoordinates")) {
            BetterAdvancementsScreen.showDebugCoordinates = root.get("showDebugCoordinates").getAsBoolean();
        }
        if (root.has("tabSortMode")) {
            BetterAdvancementsScreen.setTabSortMode(readTabSortMode(root, "tabSortMode"));
        } else if (root.has("orderTabsAlphabetically")) {
            boolean legacyAlphabetical = readBoolean(root, "orderTabsAlphabetically", false);
            BetterAdvancementsScreen.setTabSortMode(
                legacyAlphabetical ? TabSortMode.ALPHABETICAL : TabSortMode.ORIGINAL
            );
        }
        if (root.has("customTabOrder")) {
            BetterAdvancementsScreen.setCustomTabOrder(readCustomTabOrder(root, "customTabOrder"));
        }
        if (root.has("uiScaling")) {
            BetterAdvancementsScreen.uiScaling = root.get("uiScaling").getAsInt();
        }
        if (root.has("criteriaDetail")) {
            CriterionGrid.detailLevel = CriteriaDetail.fromName(root.get("criteriaDetail").getAsString());
        }
        if (root.has("criteriaDetailRequiresShift")) {
            CriterionGrid.requiresShift = root.get("criteriaDetailRequiresShift").getAsBoolean();
        }
        if (root.has("sortCriteriaAlphabetically")) {
            CriterionGrid.sortAlphabetically = readBoolean(root, "sortCriteriaAlphabetically", true);
        }
        if (root.has("addInventoryButton")) {
            BetterAdvancementsScreenButton.addToInventory = root.get("addInventoryButton").getAsBoolean();
        }
        if (root.has("defaultDrawDirectLines")) {
            BetterDisplayInfo.defaultDrawDirectLines = root.get("defaultDrawDirectLines").getAsBoolean();
        }
        if (root.has("defaultHideLines")) {
            BetterDisplayInfo.defaultHideLines = root.get("defaultHideLines").getAsBoolean();
        }
        if (root.has("defaultCompletedLineColor")) {
            BetterDisplayInfo.defaultCompletedLineColor = ColorHelper.RGB(root.get("defaultCompletedLineColor").getAsString());
        }
        if (root.has("defaultUncompletedLineColor")) {
            BetterDisplayInfo.defaultUncompletedLineColor = ColorHelper.RGB(root.get("defaultUncompletedLineColor").getAsString());
        }
        if (root.has("onlyUseAboveAdvancementTabs")) {
            BetterAdvancementTabType.onlyUseAbove = root.get("onlyUseAboveAdvancementTabs").getAsBoolean();
        }
        if (root.has("backgroundProgressEnabled")) {
            BetterAdvancementTab.backgroundProgressEnabled = readBoolean(
                root, "backgroundProgressEnabled", BetterAdvancementTab.DEFAULT_BACKGROUND_PROGRESS_ENABLED);
        }
        if (root.has("backgroundProgressMode")) {
            BetterAdvancementTab.backgroundProgressMode = readBackgroundProgressMode(root, "backgroundProgressMode");
        }
        if (root.has("backgroundProgressDirection")) {
            BetterAdvancementTab.backgroundProgressDirection = readBackgroundProgressDirection(root, "backgroundProgressDirection");
        }
        if (root.has("backgroundProgressColor")) {
            BetterAdvancementTab.backgroundProgressColor = readArgb(
                root, "backgroundProgressColor", BetterAdvancementTab.DEFAULT_BACKGROUND_PROGRESS_COLOR);
        }
        if (root.has("backgroundCompletedColor")) {
            BetterAdvancementTab.backgroundCompletedColor = readArgb(
                root, "backgroundCompletedColor", BetterAdvancementTab.DEFAULT_BACKGROUND_COMPLETED_COLOR);
        }
    }

    public static void writeToConfig() {
        JsonObject root = new JsonObject();

        root.addProperty("defaultUncompletedIconColor", ColorHelper.asRGBString(BetterDisplayInfo.defaultUncompletedIconColor));
        root.addProperty("defaultUncompletedTitleColor", ColorHelper.asRGBString(BetterDisplayInfo.defaultUncompletedTitleColor));
        root.addProperty("defaultCompletedIconColor", ColorHelper.asRGBString(BetterDisplayInfo.defaultCompletedIconColor));
        root.addProperty("defaultCompletedTitleColor", ColorHelper.asRGBString(BetterDisplayInfo.defaultCompletedTitleColor));
        root.addProperty("doAdvancementsBackgroundFade", BetterAdvancementTab.doFade);
        root.addProperty("showDebugCoordinates", BetterAdvancementsScreen.showDebugCoordinates);
        root.addProperty("orderTabsAlphabetically", BetterAdvancementsScreen.getTabSortMode() == TabSortMode.ALPHABETICAL);
        root.addProperty("tabSortMode", BetterAdvancementsScreen.getTabSortMode().name());
        JsonArray customOrder = new JsonArray();
        for (String id : BetterAdvancementsScreen.getCustomTabOrder()) {
            customOrder.add(id);
        }
        root.add("customTabOrder", customOrder);
        root.addProperty("uiScaling", BetterAdvancementsScreen.uiScaling);
        root.addProperty("criteriaDetail", CriterionGrid.detailLevel.getName());
        root.addProperty("criteriaDetailRequiresShift", CriterionGrid.requiresShift);
        root.addProperty("sortCriteriaAlphabetically", CriterionGrid.sortAlphabetically);
        root.addProperty("addInventoryButton", BetterAdvancementsScreenButton.addToInventory);
        root.addProperty("defaultDrawDirectLines", BetterDisplayInfo.defaultDrawDirectLines);
        root.addProperty("defaultHideLines", BetterDisplayInfo.defaultHideLines);
        root.addProperty("defaultCompletedLineColor", ColorHelper.asRGBString(BetterDisplayInfo.defaultCompletedLineColor));
        root.addProperty("defaultUncompletedLineColor", ColorHelper.asRGBString(BetterDisplayInfo.defaultUncompletedLineColor));
        root.addProperty("onlyUseAboveAdvancementTabs", BetterAdvancementTabType.onlyUseAbove);
        root.addProperty("backgroundProgressEnabled", BetterAdvancementTab.backgroundProgressEnabled);
        root.addProperty("backgroundProgressMode", BetterAdvancementTab.backgroundProgressMode.name());
        root.addProperty("backgroundProgressDirection", BetterAdvancementTab.backgroundProgressDirection.name());
        root.addProperty("backgroundProgressColor", ColorHelper.asARGBString(BetterAdvancementTab.backgroundProgressColor));
        root.addProperty("backgroundCompletedColor", ColorHelper.asARGBString(BetterAdvancementTab.backgroundCompletedColor));

        try (FileWriter file = new FileWriter(getConfigFile())) {
            file.write(new GsonBuilder().setPrettyPrinting().create().toJson(root));
            file.flush();
        } catch (IOException e) {
            Constants.log.error(e);
        }
    }

    private static boolean readBoolean(JsonObject root, String key, boolean defaultValue) {
        try {
            if (!root.get(key).isJsonPrimitive() || !root.get(key).getAsJsonPrimitive().isBoolean()) {
                throw new IllegalStateException("Expected a JSON boolean");
            }
            return root.get(key).getAsBoolean();
        } catch (RuntimeException e) {
            Constants.log.warn("Invalid {} value; expected true or false. Using {}", key, defaultValue);
            return defaultValue;
        }
    }

    private static BetterAdvancementTab.BackgroundProgressMode readBackgroundProgressMode(JsonObject root, String key) {
        try {
            String value = root.get(key).getAsString();
            BetterAdvancementTab.BackgroundProgressMode parsed = BetterAdvancementTab.BackgroundProgressMode.fromName(value);
            if (!parsed.name().equalsIgnoreCase(value)) {
                Constants.log.warn("Invalid {} value '{}'; using {}", key, value, BetterAdvancementTab.DEFAULT_BACKGROUND_PROGRESS_MODE);
            }
            return parsed;
        } catch (RuntimeException e) {
            Constants.log.warn("Invalid {} value; using {}", key, BetterAdvancementTab.DEFAULT_BACKGROUND_PROGRESS_MODE);
            return BetterAdvancementTab.DEFAULT_BACKGROUND_PROGRESS_MODE;
        }
    }

    private static BetterAdvancementTab.BackgroundProgressDirection readBackgroundProgressDirection(JsonObject root, String key) {
        try {
            String value = root.get(key).getAsString();
            BetterAdvancementTab.BackgroundProgressDirection parsed = BetterAdvancementTab.BackgroundProgressDirection.fromName(value);
            if (!parsed.name().equalsIgnoreCase(value)) {
                Constants.log.warn("Invalid {} value '{}'; using {}", key, value, BetterAdvancementTab.DEFAULT_BACKGROUND_PROGRESS_DIRECTION);
            }
            return parsed;
        } catch (RuntimeException e) {
            Constants.log.warn("Invalid {} value; using {}", key, BetterAdvancementTab.DEFAULT_BACKGROUND_PROGRESS_DIRECTION);
            return BetterAdvancementTab.DEFAULT_BACKGROUND_PROGRESS_DIRECTION;
        }
    }

    private static TabSortMode readTabSortMode(JsonObject root, String key) {
        try {
            String value = root.get(key).getAsString();
            TabSortMode parsed = TabSortMode.fromName(value);
            if (!parsed.name().equalsIgnoreCase(value)) {
                Constants.log.warn("Invalid {} value '{}'; using {}", key, value, TabSortMode.ORIGINAL);
            }
            return parsed;
        } catch (RuntimeException e) {
            Constants.log.warn("Invalid {} value; using {}", key, TabSortMode.ORIGINAL);
            return TabSortMode.ORIGINAL;
        }
    }

    private static java.util.List<String> readCustomTabOrder(JsonObject root, String key) {
        java.util.List<String> order = new java.util.ArrayList<>();
        try {
            JsonArray array = root.getAsJsonArray(key);
            for (JsonElement element : array) {
                if (order.size() >= 4096) {
                    Constants.log.warn("{} contains more than 4096 entries; ignoring the remainder", key);
                    break;
                }
                if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()) {
                    order.add(element.getAsString());
                }
            }
        } catch (RuntimeException e) {
            Constants.log.warn("Invalid {} value; using an empty custom tab order", key);
        }
        return order;
    }

    private static int readArgb(JsonObject root, String key, int defaultValue) {
        try {
            String value = root.get(key).getAsString();
            return ColorHelper.ARGB(value);
        } catch (RuntimeException e) {
            Constants.log.warn("Invalid {} value; expected #AARRGGBB. Using {}", key, ColorHelper.asARGBString(defaultValue));
            return defaultValue;
        }
    }

    public static File getConfigFile() throws IOException {
        return FabricLoader.getInstance().getConfigDir().resolve("betteradvancements.json").toFile();
    }
}
