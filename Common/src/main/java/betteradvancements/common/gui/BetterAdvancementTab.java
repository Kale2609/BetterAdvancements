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
    private static final float MIN_AUTO_FIT_SCALE = 0.50F;
    private static final int AUTO_FIT_MARGIN = 12;

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
    private final AdvancementNode rootNode;
    private final DisplayInfo display;
    private final ItemStack icon;
    private final Component title;
    private final BetterAdvancementWidget root;
    protected final Map<AdvancementHolder, BetterAdvancementWidget> widgets = Maps.newLinkedHashMap();
    private final BetterDisplayInfoRegistry betterDisplayInfos;

    protected int scrollX, scrollY;
    // Raw positions come from Minecraft/BetterAdvancements. Layout positions are derived from
    // those coordinates and may be rotated by 90 degrees for portrait-shaped trees.
    private int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE;
    private int minY = Integer.MAX_VALUE, maxY = Integer.MIN_VALUE;
    private int layoutMinX = Integer.MAX_VALUE, layoutMaxX = Integer.MIN_VALUE;
    private int layoutMinY = Integer.MAX_VALUE, layoutMaxY = Integer.MIN_VALUE;
    private float fade;
    private boolean centered;
    private boolean layoutDirty = true;
    private boolean orientationInitialized;
    private boolean treeRotated;
    private BetterAdvancementWidget hoveredWidget;
    private float treeScale = 1.0F;

    public BetterAdvancementTab(Minecraft mc, BetterAdvancementsScreen betterAdvancementsScreen, AdvancementNode advancementNode, DisplayInfo displayInfo) {
        this.minecraft = mc;
        this.screen = betterAdvancementsScreen;
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

    public void drawTab(GuiGraphics guiGraphics, int left, int top, int width, int height, boolean selected, int displayIndex) {
        BetterAdvancementTabType type = BetterAdvancementTabType.getTabType(width, height, displayIndex);
        if (type == null) {
            return;
        }

        type.draw(guiGraphics, left, top, width, height, selected, displayIndex);
        this.drawTabProgress(guiGraphics, left, top, width, height, selected, displayIndex, type);
    }

    public void drawIcon(GuiGraphics guiGraphics, int left, int top, int width, int height, int displayIndex) {
        BetterAdvancementTabType type = BetterAdvancementTabType.getTabType(width, height, displayIndex);
        if (type != null) {
            type.drawIcon(guiGraphics, left, top, width, height, displayIndex, this.icon);
        }
    }

    public void drawContents(GuiGraphics guiGraphics, int left, int top, int width, int height) {
        this.updateTreeScale(width, height);
        if (!this.centered) {
            this.scrollX = (width - (this.layoutMaxX + this.layoutMinX)) / 2;
            this.scrollY = (height - (this.layoutMaxY + this.layoutMinY)) / 2;
            this.centered = true;
        }
        this.clampScroll(0.0D, 0.0D, width, height);

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


        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(width / 2.0F, height / 2.0F, 0.0F);
        guiGraphics.pose().scale(this.treeScale, this.treeScale, 1.0F);
        guiGraphics.pose().translate(-width / 2.0F, -height / 2.0F, 0.0F);
        this.root.drawConnectivity(guiGraphics, this.scrollX, this.scrollY, true);
        this.root.drawConnectivity(guiGraphics, this.scrollX, this.scrollY, false);
        this.root.draw(guiGraphics, this.scrollX, this.scrollY);
        guiGraphics.pose().popPose();
        guiGraphics.pose().popPose();
        guiGraphics.disableScissor();
    }

    private void drawTabProgress(
        GuiGraphics guiGraphics,
        int left,
        int top,
        int width,
        int height,
        boolean selected,
        int displayIndex,
        BetterAdvancementTabType type
    ) {
        if (!backgroundProgressEnabled || backgroundProgressMode == BackgroundProgressMode.OFF) {
            return;
        }

        float progress = this.getCompletionProgress();
        if (backgroundProgressMode == BackgroundProgressMode.COMPLETION_COLOR) {
            if (progress >= 1.0F) {
                type.drawProgressOverlay(
                    guiGraphics, left, top, width, height, selected, displayIndex, 1.0F,
                    backgroundCompletedColor, backgroundProgressDirection);
            }
            return;
        }

        if (progress <= 0.0F) {
            return;
        }

        int colour = progress >= 1.0F ? backgroundCompletedColor : backgroundProgressColor;
        type.drawProgressOverlay(
            guiGraphics, left, top, width, height, selected, displayIndex, progress, colour,
            backgroundProgressDirection);
    }

    public float getCompletionProgress() {
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
        this.updateTreeScale(width, height);
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0.0D, 0.0D, -200.0D);
        guiGraphics.fill(0, 0, width, height, Mth.floor(this.fade * 255.0F) << 24);

        double treeMouseX = this.viewToTreeX(mouseX, width);
        double treeMouseY = this.viewToTreeY(mouseY, height);
        BetterAdvancementWidget activeWidget = null;
        if (this.hoveredWidget != null && this.hoveredWidget.isMouseOverDetachedPanel(mouseX, mouseY)) {
            activeWidget = this.hoveredWidget;
        } else if (mouseX > 0 && mouseX < width && mouseY > 0 && mouseY < height) {
            for (BetterAdvancementWidget widget : this.widgets.values()) {
                if (widget.isMouseOver(this.scrollX, this.scrollY, treeMouseX, treeMouseY)) {
                    activeWidget = widget;
                    break;
                }
            }
        }

        if (this.hoveredWidget != null && this.hoveredWidget != activeWidget) {
            this.hoveredWidget.clearDetachedPanelHitBox();
        }
        this.hoveredWidget = activeWidget;

        boolean flag = activeWidget != null;
        if (activeWidget != null) {
            int nodeX = this.treeToViewX(this.scrollX + this.getLayoutX(activeWidget), width);
            int nodeY = this.treeToViewY(this.scrollY + this.getLayoutY(activeWidget), height);
            activeWidget.drawHover(guiGraphics, nodeX, nodeY, this.fade, width, height, left, top);
        }

        guiGraphics.pose().popPose();

        if (doFade && flag) {
            this.fade = Mth.clamp(this.fade + 0.02F, 0.0F, 0.3F);
        } else {
            this.fade = Mth.clamp(this.fade - 0.04F, 0.0F, 1.0F);
        }
    }

    public boolean scrollHoveredCriteria(double mouseX, double mouseY, double scrollDelta, int width, int height) {
        this.updateTreeScale(width, height);
        double treeMouseX = this.viewToTreeX(mouseX, width);
        double treeMouseY = this.viewToTreeY(mouseY, height);

        if (this.hoveredWidget != null
            && (this.hoveredWidget.isMouseOverDetachedPanel(mouseX, mouseY)
                || this.hoveredWidget.isMouseOver(this.scrollX, this.scrollY, treeMouseX, treeMouseY))) {
            return this.hoveredWidget.changeCriterionPage(scrollDelta);
        }

        for (BetterAdvancementWidget widget : this.widgets.values()) {
            if (widget.isMouseOver(this.scrollX, this.scrollY, treeMouseX, treeMouseY)) {
                return widget.changeCriterionPage(scrollDelta);
            }
        }
        return false;
    }

    public boolean isMouseOver(
        int left,
        int top,
        int width,
        int height,
        int displayIndex,
        double mouseX,
        double mouseY
    ) {
        BetterAdvancementTabType type = BetterAdvancementTabType.getTabType(width, height, displayIndex);
        return type != null && type.isMouseOver(left, top, width, height, displayIndex, mouseX, mouseY);
    }

    public static BetterAdvancementTab create(
        Minecraft mc,
        BetterAdvancementsScreen betterAdvancementsScreen,
        AdvancementNode advancementNode
    ) {
        Optional<DisplayInfo> optional = advancementNode.advancement().display();
        return optional
            .map(displayInfo -> new BetterAdvancementTab(mc, betterAdvancementsScreen, advancementNode, displayInfo))
            .orElse(null);
    }

    public void scroll(double scrollX, double scrollY, int width, int height) {
        this.updateTreeScale(width, height);
        this.clampScroll(scrollX / this.treeScale, scrollY / this.treeScale, width, height);
    }

    public boolean isMouseOverWidget(BetterAdvancementWidget widget, double mouseX, double mouseY, int width, int height) {
        this.updateTreeScale(width, height);
        return widget.isMouseOver(
            this.scrollX,
            this.scrollY,
            this.viewToTreeX(mouseX, width),
            this.viewToTreeY(mouseY, height)
        );
    }

    public double screenDeltaToRawX(double screenDeltaX, double screenDeltaY, int width, int height) {
        this.updateTreeScale(width, height);
        double layoutDeltaX = screenDeltaX / this.treeScale;
        double layoutDeltaY = screenDeltaY / this.treeScale;
        return this.treeRotated ? layoutDeltaY : layoutDeltaX;
    }

    public double screenDeltaToRawY(double screenDeltaX, double screenDeltaY, int width, int height) {
        this.updateTreeScale(width, height);
        double layoutDeltaX = screenDeltaX / this.treeScale;
        double layoutDeltaY = screenDeltaY / this.treeScale;
        return this.treeRotated ? -layoutDeltaX : layoutDeltaY;
    }

    public float getTreeScale(int width, int height) {
        this.updateTreeScale(width, height);
        return this.treeScale;
    }

    public int treeToViewX(double treeX, int width) {
        return (int)Math.round(width / 2.0D + this.treeScale * (treeX - width / 2.0D));
    }

    public int treeToViewY(double treeY, int height) {
        return (int)Math.round(height / 2.0D + this.treeScale * (treeY - height / 2.0D));
    }

    private double viewToTreeX(double viewX, int width) {
        return width / 2.0D + (viewX - width / 2.0D) / this.treeScale;
    }

    private double viewToTreeY(double viewY, int height) {
        return height / 2.0D + (viewY - height / 2.0D) / this.treeScale;
    }

    private void updateTreeScale(int width, int height) {
        this.updateTreeLayout();

        // Legacy direct-line rendering bypasses GuiGraphics' pose stack. Rotation is safe because
        // its endpoints are transformed explicitly, but scaling would separate the lines from nodes.
        for (BetterAdvancementWidget widget : this.widgets.values()) {
            if (Boolean.TRUE.equals(widget.betterDisplayInfo.drawDirectLines())) {
                this.treeScale = 1.0F;
                return;
            }
        }

        int treeWidth = Math.max(1, this.layoutMaxX - this.layoutMinX);
        int treeHeight = Math.max(1, this.layoutMaxY - this.layoutMinY);
        int fitWidth = Math.max(1, width - 2 * AUTO_FIT_MARGIN);
        int fitHeight = Math.max(1, height - 2 * AUTO_FIT_MARGIN);
        float fitScale = Math.min((float)fitWidth / treeWidth, (float)fitHeight / treeHeight);
        this.treeScale = Mth.clamp(Math.min(1.0F, fitScale), MIN_AUTO_FIT_SCALE, 1.0F);
    }

    private void updateTreeLayout() {
        if (!this.layoutDirty) {
            return;
        }

        if (!this.orientationInitialized) {
            int rawWidth = Math.max(1, this.maxX - this.minX);
            int rawHeight = Math.max(1, this.maxY - this.minY);
            this.treeRotated = rawHeight > rawWidth;
            this.orientationInitialized = true;
        }

        this.layoutMinX = Integer.MAX_VALUE;
        this.layoutMaxX = Integer.MIN_VALUE;
        this.layoutMinY = Integer.MAX_VALUE;
        this.layoutMaxY = Integer.MIN_VALUE;
        for (BetterAdvancementWidget widget : this.widgets.values()) {
            int layoutX = this.getLayoutX(widget);
            int layoutY = this.getLayoutY(widget);
            this.layoutMinX = Math.min(this.layoutMinX, layoutX);
            this.layoutMaxX = Math.max(this.layoutMaxX, layoutX + 28);
            this.layoutMinY = Math.min(this.layoutMinY, layoutY);
            this.layoutMaxY = Math.max(this.layoutMaxY, layoutY + 27);
        }

        if (this.widgets.isEmpty()) {
            this.layoutMinX = this.layoutMinY = 0;
            this.layoutMaxX = this.layoutMaxY = 1;
        }
        this.layoutDirty = false;
    }

    public boolean isTreeRotated() {
        this.updateTreeLayout();
        return this.treeRotated;
    }

    public int getLayoutX(BetterAdvancementWidget widget) {
        return this.getLayoutX(widget.getX(), widget.getY());
    }

    public int getLayoutY(BetterAdvancementWidget widget) {
        return this.getLayoutY(widget.getX(), widget.getY());
    }

    public int getLayoutX(int rawX, int rawY) {
        return this.treeRotated ? -rawY : rawX;
    }

    public int getLayoutY(int rawX, int rawY) {
        return this.treeRotated ? rawX : rawY;
    }

    public boolean shouldScrollWheelHorizontally(int width, int height) {
        this.updateTreeScale(width, height);
        return this.treeRotated && (this.layoutMaxX - this.layoutMinX) * this.treeScale > width;
    }

    public int viewToRawX(double viewX, double viewY, int width, int height) {
        this.updateTreeScale(width, height);
        double layoutX = this.viewToTreeX(viewX, width) - this.scrollX;
        double layoutY = this.viewToTreeY(viewY, height) - this.scrollY;
        return (int)Math.round(this.treeRotated ? layoutY : layoutX);
    }

    public int viewToRawY(double viewX, double viewY, int width, int height) {
        this.updateTreeScale(width, height);
        double layoutX = this.viewToTreeX(viewX, width) - this.scrollX;
        double layoutY = this.viewToTreeY(viewY, height) - this.scrollY;
        return (int)Math.round(this.treeRotated ? -layoutX : layoutY);
    }

    private void clampScroll(double deltaX, double deltaY, int width, int height) {
        this.updateTreeLayout();
        double visibleMinX = width / 2.0D - width / (2.0D * this.treeScale);
        double visibleMaxX = width / 2.0D + width / (2.0D * this.treeScale);
        double visibleMinY = height / 2.0D - height / (2.0D * this.treeScale);
        double visibleMaxY = height / 2.0D + height / (2.0D * this.treeScale);

        if ((this.layoutMaxX - this.layoutMinX) * this.treeScale > width) {
            this.scrollX = (int)Math.round(Mth.clamp(
                this.scrollX + deltaX,
                visibleMaxX - this.layoutMaxX,
                visibleMinX - this.layoutMinX
            ));
        } else {
            this.scrollX = (width - (this.layoutMaxX + this.layoutMinX)) / 2;
        }

        if ((this.layoutMaxY - this.layoutMinY) * this.treeScale > height) {
            this.scrollY = (int)Math.round(Mth.clamp(
                this.scrollY + deltaY,
                visibleMaxY - this.layoutMaxY,
                visibleMinY - this.layoutMinY
            ));
        } else {
            this.scrollY = (height - (this.layoutMaxY + this.layoutMinY)) / 2;
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
        this.layoutDirty = true;

        for (BetterAdvancementWidget gui : this.widgets.values()) {
            gui.attachToParent();
        }
    }

    public void recalculateBounds() {
        this.minX = Integer.MAX_VALUE;
        this.maxX = Integer.MIN_VALUE;
        this.minY = Integer.MAX_VALUE;
        this.maxY = Integer.MIN_VALUE;
        for (BetterAdvancementWidget widget : this.widgets.values()) {
            this.minX = Math.min(this.minX, widget.getX());
            this.maxX = Math.max(this.maxX, widget.getX() + 28);
            this.minY = Math.min(this.minY, widget.getY());
            this.maxY = Math.max(this.maxY, widget.getY() + 27);
        }
        this.layoutDirty = true;
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
