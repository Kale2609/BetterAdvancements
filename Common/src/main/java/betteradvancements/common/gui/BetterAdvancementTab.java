package betteradvancements.common.gui;

import betteradvancements.common.advancements.BetterDisplayInfo;
import betteradvancements.common.advancements.BetterDisplayInfoRegistry;
import com.google.common.collect.Maps;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementNode;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.Tuple;
import net.minecraft.world.item.ItemStack;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class BetterAdvancementTab {
    public static boolean doFade = true;

    public static final boolean DEFAULT_BACKGROUND_PROGRESS_ENABLED = false;
    public static final BackgroundProgressMode DEFAULT_BACKGROUND_PROGRESS_MODE = BackgroundProgressMode.PROGRESS_FILL;
    public static final BackgroundProgressDirection DEFAULT_BACKGROUND_PROGRESS_DIRECTION = BackgroundProgressDirection.BOTTOM_TO_TOP;
    public static final int DEFAULT_BACKGROUND_PROGRESS_COLOR = 0x66D4A72C;
    public static final int DEFAULT_BACKGROUND_COMPLETED_COLOR = 0x99FFD700;

    public static boolean backgroundProgressEnabled = DEFAULT_BACKGROUND_PROGRESS_ENABLED;
    public static BackgroundProgressMode backgroundProgressMode = DEFAULT_BACKGROUND_PROGRESS_MODE;
    public static BackgroundProgressDirection backgroundProgressDirection = DEFAULT_BACKGROUND_PROGRESS_DIRECTION;
    public static int backgroundProgressColor = DEFAULT_BACKGROUND_PROGRESS_COLOR;
    public static int backgroundCompletedColor = DEFAULT_BACKGROUND_COMPLETED_COLOR;

    public enum BackgroundProgressMode {
        OFF,
        COMPLETION_COLOR,
        PROGRESS_FILL;

        public static BackgroundProgressMode fromName(String name) {
            if (name != null) {
                for (BackgroundProgressMode value : values()) {
                    if (value.name().equalsIgnoreCase(name)) {
                        return value;
                    }
                }
            }
            return DEFAULT_BACKGROUND_PROGRESS_MODE;
        }
    }

    public enum BackgroundProgressDirection {
        LEFT_TO_RIGHT,
        RIGHT_TO_LEFT,
        TOP_TO_BOTTOM,
        BOTTOM_TO_TOP;

        public static BackgroundProgressDirection fromName(String name) {
            if (name != null) {
                for (BackgroundProgressDirection value : values()) {
                    if (value.name().equalsIgnoreCase(name)) {
                        return value;
                    }
                }
            }
            return DEFAULT_BACKGROUND_PROGRESS_DIRECTION;
        }
    }
    public static final Map<AdvancementHolder, Tuple<Integer, Integer>> scrollHistory = Maps.newLinkedHashMap();

    private final Minecraft minecraft;
    private final BetterAdvancementsScreen screen;
    private final BetterAdvancementTabType type;
    private final int index;
    private final AdvancementNode rootNode;
    private final DisplayInfo display;
    private final ItemStack icon;
    private final Component title;
    private final BetterAdvancementWidget root;
    protected final Map<AdvancementHolder, BetterAdvancementWidget> widgets = Maps.newLinkedHashMap();
    private final BetterDisplayInfoRegistry betterDisplayInfos;

    protected int scrollX, scrollY;
    private int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE;
    private int minY = Integer.MAX_VALUE, maxY = Integer.MIN_VALUE;
    private float fade;
    private boolean centered;

    public BetterAdvancementTab(Minecraft mc, BetterAdvancementsScreen betterAdvancementsScreen, BetterAdvancementTabType type, int index, AdvancementNode advancementNode, DisplayInfo displayInfo) {
        this.minecraft = mc;
        this.screen = betterAdvancementsScreen;
        this.type = type;
        this.index = index;
        this.rootNode = advancementNode;
        this.display = displayInfo;
        this.icon = displayInfo.getIcon();
        this.title = displayInfo.getTitle();
        this.betterDisplayInfos = new BetterDisplayInfoRegistry(advancementNode);
        this.root = new BetterAdvancementWidget(this, mc, advancementNode, displayInfo);
        this.addWidget(this.root, advancementNode.holder());
    }

    public AdvancementNode getRootNode() {
        return this.rootNode;
    }

    public Component getTitle() {
        return this.title;
    }

    public void drawTab(GuiGraphics guiGraphics, int left, int top, int width, int height, boolean selected) {
        this.type.draw(guiGraphics, left, top, width, height, selected, this.index);
        this.drawTabProgress(guiGraphics, left, top, width, height, selected);
    }

    public void drawIcon(GuiGraphics guiGraphics, int left, int top,int width, int height) {
        this.type.drawIcon(guiGraphics, left, top, width, height, this.index, this.icon);
    }

    public void drawContents(GuiGraphics guiGraphics, int left, int top, int width, int height) {
        if (!this.centered) {
            this.scrollX = (width - (this.maxX + this.minX)) / 2;
            this.scrollY = (height - (this.maxY + this.minY)) / 2;
            this.centered = true;
        }

        guiGraphics.enableScissor(left, top, left + width, top + height);
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(left, top, 0);
        ResourceLocation resourcelocation = this.display.getBackground().orElse(TextureManager.INTENTIONAL_MISSING_TEXTURE);

        int i = this.scrollX % 16;
        int j = this.scrollY % 16;

        int k = -1;
        for (; k <= 1 + width / 16; k++) {
            int l = -1;
            for (;l <= height / 16; l++) {
                guiGraphics.blit(resourcelocation, i + 16 * k, j + 16 * l, 0.0F, 0.0F, 16, 16, 16, 16);
            }
            guiGraphics.blit(resourcelocation, i + 16 * k, j + 16 * l, 0.0F, 0.0F, 16, height % 16, 16, 16);
        }


        this.root.drawConnectivity(guiGraphics, this.scrollX, this.scrollY, true);
        this.root.drawConnectivity(guiGraphics, this.scrollX, this.scrollY, false);
        this.root.draw(guiGraphics, this.scrollX, this.scrollY);
        guiGraphics.pose().popPose();
        guiGraphics.disableScissor();
    }

    private void drawTabProgress(GuiGraphics guiGraphics, int left, int top, int width, int height, boolean selected) {
        if (!backgroundProgressEnabled || backgroundProgressMode == BackgroundProgressMode.OFF) {
            return;
        }

        float progress = this.getCompletionProgress();
        if (backgroundProgressMode == BackgroundProgressMode.COMPLETION_COLOR) {
            if (progress >= 1.0F) {
                this.type.drawProgressOverlay(
                    guiGraphics, left, top, width, height, selected, this.index, 1.0F,
                    backgroundCompletedColor, backgroundProgressDirection);
            }
            return;
        }

        if (progress <= 0.0F) {
            return;
        }

        int colour = progress >= 1.0F ? backgroundCompletedColor : backgroundProgressColor;
        this.type.drawProgressOverlay(
            guiGraphics, left, top, width, height, selected, this.index, progress, colour,
            backgroundProgressDirection);
    }

    private float getCompletionProgress() {
        if (this.widgets.isEmpty()) {
            return 0.0F;
        }

        int completed = 0;
        for (BetterAdvancementWidget widget : this.widgets.values()) {
            if (widget.isCompleted()) {
                completed++;
            }
        }

        return (float) completed / (float) this.widgets.size();
    }

    public void drawToolTips(GuiGraphics guiGraphics, int mouseX, int mouseY, int left, int top, int width, int height) {
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0.0D, 0.0D, -200.0D);
        guiGraphics.fill(0, 0, width, height, Mth.floor(this.fade * 255.0F) << 24);
        boolean flag = false;

        if (mouseX > 0 && mouseX < width && mouseY > 0 && mouseY < height) {
            for (BetterAdvancementWidget betterAdvancementWidget : this.widgets.values()) {
                if (betterAdvancementWidget.isMouseOver(this.scrollX, this.scrollY, mouseX, mouseY)) {
                    flag = true;
                    betterAdvancementWidget.drawHover(guiGraphics, this.scrollX, this.scrollY, this.fade, left, top);
                    break;
                }
            }
        }

        guiGraphics.pose().popPose();

        if (doFade && flag) {
            this.fade = Mth.clamp(this.fade + 0.02F, 0.0F, 0.3F);
        } else {
            this.fade = Mth.clamp(this.fade - 0.04F, 0.0F, 1.0F);
        }
    }

    public boolean isMouseOver(int left, int top, int width, int height, double mouseX, double mouseY) {
        return this.type.isMouseOver(left, top, width, height, this.index, mouseX, mouseY);
    }

    public static BetterAdvancementTab create(Minecraft mc, BetterAdvancementsScreen betterAdvancementsScreen, int index, AdvancementNode advancementNode, int width, int height) {
        Optional<DisplayInfo> optional = advancementNode.advancement().display();
        if (optional.isEmpty()) {
            return null;
        } else {
            BetterAdvancementTabType advancementTabType = BetterAdvancementTabType.getTabType(width, height, index);
            if (advancementTabType == null) {
                return null;
            } else {
                return new BetterAdvancementTab(mc, betterAdvancementsScreen, advancementTabType, index, advancementNode, optional.get());
            }

        }
    }

    public void scroll(double scrollX, double scrollY, int width, int height) {
        if (this.maxX - this.minX > width) {
            this.scrollX = (int)Math.round(Mth.clamp(this.scrollX + scrollX, -(this.maxX - width), -this.minX));
        }

        if (this.maxY - this.minY > height) {
            this.scrollY = (int)Math.round(Mth.clamp(this.scrollY + scrollY, -(this.maxY - height), -this.minY));
        }
    }

    public void addAdvancement(AdvancementNode advancementNode) {
        Optional<DisplayInfo> optional = advancementNode.advancement().display();
        if (optional.isPresent()) {
            BetterAdvancementWidget betterAdvancementEntryScreen = new BetterAdvancementWidget(this, this.minecraft, advancementNode, optional.get());
            this.addWidget(betterAdvancementEntryScreen, advancementNode.holder());
        }
    }

    private void addWidget(BetterAdvancementWidget betterAdvancementEntryScreen, AdvancementHolder advancementHolder) {
        this.widgets.put(advancementHolder, betterAdvancementEntryScreen);
        int left = betterAdvancementEntryScreen.getX();
        int right = left + 28;
        int top = betterAdvancementEntryScreen.getY();
        int bottom = top + 27;
        this.minX = Math.min(this.minX, left);
        this.maxX = Math.max(this.maxX, right);
        this.minY = Math.min(this.minY, top);
        this.maxY = Math.max(this.maxY, bottom);

        for (BetterAdvancementWidget gui : this.widgets.values()) {
            gui.attachToParent();
        }
    }

    public BetterAdvancementWidget getWidget(AdvancementHolder advancementHolder) {
        return this.widgets.get(advancementHolder);
    }

    public BetterAdvancementsScreen getScreen() {
        return this.screen;
    }

    public BetterDisplayInfo getBetterDisplayInfo(AdvancementNode advancementNode) {
        return betterDisplayInfos.get(advancementNode.holder());
    }

    public void storeScroll() {
        scrollHistory.put(this.rootNode.holder(), new Tuple<>(scrollX, scrollY));
    }

    public void loadScroll() {
        Tuple<Integer, Integer> scroll = scrollHistory.get(this.rootNode.holder());
        if (scroll != null) {
            this.centered = true;
            this.scrollX = scroll.getA();
            this.scrollY = scroll.getB();
        }
    }
}
