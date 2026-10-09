package betteradvancements.common.gui;

import betteradvancements.common.platform.Services;
import betteradvancements.common.reference.Resources;
import betteradvancements.common.util.RenderUtil;
import betteradvancements.common.util.TabSortMode;
import com.google.common.collect.Maps;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementNode;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.client.GameNarrator;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientAdvancements;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundSeenAdvancementsPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class BetterAdvancementsScreen extends Screen implements ClientAdvancements.Listener {
    private static final Component VERY_SAD_LABEL = Component.translatable("advancements.sad_label");
    private static final Component NO_ADVANCEMENTS_LABEL = Component.translatable("advancements.empty");
    private static final Component TITLE =Component.translatable("gui.advancements");
    private static final int WIDTH = 252, HEIGHT = 140, CORNER_SIZE = 30;
    private static final int SIDE = 30, TOP = 40, BOTTOM = 30, PADDING = 9;
    private static final float MIN_ZOOM = 1, MAX_ZOOM = 2, ZOOM_STEP = 0.2F;
    private final ClientAdvancements clientAdvancements;
    private final Map<AdvancementHolder, BetterAdvancementTab> tabs = Maps.newLinkedHashMap();
    private BetterAdvancementTab selectedTab;
    private static int tabPage, maxPages;
    private float zoom = MIN_ZOOM;
    private boolean isScrolling;
    protected int internalWidth, internalHeight;
    public static int uiScaling = 100;
    public static boolean showDebugCoordinates = false;
    /**
     * Legacy compatibility flag. New code should use {@link #tabSortMode}.
     */
    public static boolean orderTabsAlphabetically = false;
    public static TabSortMode tabSortMode = TabSortMode.ORIGINAL;
    private static final List<String> customTabOrder = new ArrayList<>();

    private BetterAdvancementWidget advConnectedToMouse = null;
    private BetterAdvancementTab draggedTab;
    private boolean customTabOrderChanged;

    public BetterAdvancementsScreen(ClientAdvancements clientAdvancements) {
        super(GameNarrator.NO_TITLE);
        this.clientAdvancements = clientAdvancements;
    }

    public static TabSortMode getTabSortMode() {
        if (tabSortMode == TabSortMode.ORIGINAL && orderTabsAlphabetically) {
            return TabSortMode.ALPHABETICAL;
        }
        return tabSortMode;
    }

    public static void setTabSortMode(TabSortMode mode) {
        tabSortMode = mode == null ? TabSortMode.ORIGINAL : mode;
        orderTabsAlphabetically = tabSortMode == TabSortMode.ALPHABETICAL;
    }

    public static List<String> getCustomTabOrder() {
        return List.copyOf(customTabOrder);
    }

    public static void setCustomTabOrder(Iterable<? extends String> order) {
        LinkedHashSet<String> validated = new LinkedHashSet<>();
        if (order != null) {
            for (String id : order) {
                if (id == null || id.length() > 256 || validated.size() >= 4096) {
                    continue;
                }
                String trimmed = id.trim();
                if (ResourceLocation.tryParse(trimmed) != null) {
                    validated.add(trimmed);
                }
            }
        }

        customTabOrder.clear();
        customTabOrder.addAll(validated);
    }

    /**
     * Adds the buttons (and other controls) to the screen in question. Called when the GUI is displayed and when the
     * window resizes, the buttonList is cleared beforehand.
     */
    @Override
    protected void init() {
        this.internalHeight = this.height * uiScaling / 100;
        this.internalWidth = this.width * uiScaling / 100;

        this.tabs.clear();
        this.selectedTab = null;
        this.clientAdvancements.setListener(this);

        if (this.selectedTab == null && !this.tabs.isEmpty()) {
            BetterAdvancementTab advancementTab = this.getOrderedTabs().getFirst();
            this.clientAdvancements.setSelectedTab(advancementTab.getRootNode().holder(), true);
        } else {
            this.clientAdvancements.setSelectedTab(this.selectedTab == null ? null : this.selectedTab.getRootNode().holder(), true);
        }

        int left = SIDE + (width - internalWidth) / 2;
        int top = TOP + (height - internalHeight) / 2;

        int right = internalWidth - SIDE + (width - internalWidth) / 2;
        int bottom = internalHeight - SIDE + (height - internalHeight) / 2;

        int width = right - left;
        int height = bottom - top;

        int maxTabs = Math.max(1, BetterAdvancementTabType.getMaxTabs(width, height));

        if (this.tabs.size() > maxTabs) {

            addRenderableWidget(Button.builder(Component.literal("<"), b -> tabPage = Math.max(tabPage - 1, 0)).pos(left, bottom + 4).size(20, 20).build());
            addRenderableWidget(Button.builder(Component.literal(">"), b -> tabPage = Math.min(tabPage + 1, maxPages)).pos(right - 20, bottom + 4).size(20, 20).build());
            maxPages = (this.tabs.size() - 1) / maxTabs;
            tabPage = Math.min(tabPage, maxPages);
        } else {
            maxPages = 0;
            tabPage = 0;
        }
    }

    /**
     * Called when the screen is unloaded. Used to disable keyboard repeat events
     */
    @Override
    public void onClose() {
        if (this.customTabOrderChanged) {
            Services.PLATFORM.saveCustomTabOrder(getCustomTabOrder());
            this.customTabOrderChanged = false;
        }
        this.draggedTab = null;
        this.clientAdvancements.setListener(null);
        ClientPacketListener clientpacketlistener = this.minecraft.getConnection();
        if (clientpacketlistener != null) {
            clientpacketlistener.send(ServerboundSeenAdvancementsPacket.closedScreen());
        }
        super.onClose();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int left = SIDE + (width - internalWidth) / 2;
            int top = TOP + (height - internalHeight) / 2;
            int right = internalWidth - SIDE + (width - internalWidth) / 2;
            int bottom = internalHeight - SIDE + (height - internalHeight) / 2;
            int tabAreaWidth = right - left;
            int tabAreaHeight = bottom - top;

            int maxTabs = Math.max(1, BetterAdvancementTabType.getMaxTabs(tabAreaWidth, tabAreaHeight));
            TabHit hit = this.findTabAt(mouseX, mouseY, left, top, tabAreaWidth, tabAreaHeight, maxTabs);
            if (hit != null) {
                this.clientAdvancements.setSelectedTab(hit.tab().getRootNode().holder(), true);

                if (getTabSortMode() == TabSortMode.CUSTOM) {
                    this.seedCustomTabOrder();
                    this.draggedTab = hit.tab();
                    this.customTabOrderChanged = false;
                }
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (this.draggedTab != null && getTabSortMode() == TabSortMode.CUSTOM && maxPages > 0 && scrollY != 0.0D) {
            tabPage = Mth.clamp(tabPage + (scrollY < 0.0D ? 1 : -1), 0, maxPages);
            return true;
        }

        if (this.selectedTab == null) {
            return false;
        }

        int left = SIDE + (width - internalWidth) / 2;
        int top = TOP + (height - internalHeight) / 2;
        int right = internalWidth - SIDE + (width - internalWidth) / 2;
        int bottom = internalHeight - SIDE + (height - internalHeight) / 2;

        int boxLeft = left + PADDING;
        int boxTop = top + 2 * PADDING;
        int boxRight = right - PADDING;
        int boxBottom = bottom - PADDING;
        int contentWidth = boxRight - boxLeft;
        int contentHeight = boxBottom - boxTop;

        double localMouseX = mouseX - boxLeft;
        double localMouseY = mouseY - boxTop;
        if (localMouseX >= 0 && localMouseX < contentWidth && localMouseY >= 0 && localMouseY < contentHeight) {
            if (this.selectedTab.scrollHoveredCriteria(localMouseX, localMouseY, scrollY, contentWidth, contentHeight)) {
                return true;
            }
        }

        if (this.selectedTab.shouldScrollWheelHorizontally(contentWidth, contentHeight)) {
            // Rotated portrait trees deliberately overflow along the horizontal axis at the
            // 50% minimum scale. A normal vertical wheel therefore pans them horizontally.
            double horizontalWheel = scrollY != 0.0D ? scrollY : scrollX;
            this.selectedTab.scroll(horizontalWheel * 16.0D, 0.0D, contentWidth, contentHeight);
        } else {
            this.selectedTab.scroll(scrollX * 16.0D, scrollY * 16.0D, contentWidth, contentHeight);
        }
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.minecraft.options.keyAdvancements.matches(keyCode, scanCode)) {
            this.minecraft.setScreen(null);
            this.minecraft.mouseHandler.grabMouse();
            return true;
        } else {
            return super.keyPressed(keyCode, scanCode, modifiers);
        }
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double mouseDeltaX, double mouseDeltaY) {
        int left = SIDE + (width - internalWidth) / 2;
        int top = TOP + (height - internalHeight) / 2;
        int contentWidth = internalWidth - 2 * SIDE - 2 * PADDING;
        int contentHeight = internalHeight - TOP - BOTTOM - 3 * PADDING;

        if (button == 0 && this.draggedTab != null && getTabSortMode() == TabSortMode.CUSTOM) {
            int right = internalWidth - SIDE + (width - internalWidth) / 2;
            int bottom = internalHeight - SIDE + (height - internalHeight) / 2;
            int tabAreaWidth = right - left;
            int tabAreaHeight = bottom - top;
            int maxTabs = Math.max(1, BetterAdvancementTabType.getMaxTabs(tabAreaWidth, tabAreaHeight));
            TabHit hit = this.findTabAt(mouseX, mouseY, left, top, tabAreaWidth, tabAreaHeight, maxTabs);

            if (hit != null && hit.tab() != this.draggedTab) {
                if (this.moveCustomTab(this.draggedTab, hit.tab())) {
                    this.customTabOrderChanged = true;
                }
            }
            return true;
        }

        if (button != 0) {
            this.isScrolling = false;
            return false;
        }

        if (!this.isScrolling) {
            if (this.advConnectedToMouse == null) {
                boolean inGui = mouseX < left + internalWidth - 2*SIDE - PADDING && mouseX > left + PADDING && mouseY < top + internalHeight - TOP + 1 && mouseY > top + 2*PADDING;
                if (this.selectedTab != null && inGui) {
                    for (BetterAdvancementWidget betterAdvancementEntryScreen : this.selectedTab.widgets.values()) {
                        if (this.selectedTab.isMouseOverWidget(
                            betterAdvancementEntryScreen,
                            mouseX - left - PADDING,
                            mouseY - top - 2 * PADDING,
                            contentWidth,
                            contentHeight
                        )) {

                            if (betterAdvancementEntryScreen.betterDisplayInfo.allowDragging())
                            {
                                this.advConnectedToMouse = betterAdvancementEntryScreen;
                                break;
                            }
                        }
                    }
                }
            }
            else {
                double rawDeltaX = this.selectedTab.screenDeltaToRawX(
                    mouseDeltaX, mouseDeltaY, contentWidth, contentHeight);
                double rawDeltaY = this.selectedTab.screenDeltaToRawY(
                    mouseDeltaX, mouseDeltaY, contentWidth, contentHeight);
                this.advConnectedToMouse.x = (int)Math.round(this.advConnectedToMouse.x + rawDeltaX);
                this.advConnectedToMouse.y = (int)Math.round(this.advConnectedToMouse.y + rawDeltaY);
                this.selectedTab.recalculateBounds();
            }
        }
        else {
            if (this.advConnectedToMouse != null) {
                Services.PLATFORM.getEventHelper().postAdvancementMovementEvent(advConnectedToMouse);
            }
            this.advConnectedToMouse = null;
        }

        if (this.advConnectedToMouse == null) {
            if (!this.isScrolling) {
                this.isScrolling = true;
            } else if (this.selectedTab != null) {
                this.selectedTab.scroll(mouseDeltaX, mouseDeltaY, contentWidth, contentHeight);
            }
        }

        return true;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && this.draggedTab != null) {
            this.draggedTab = null;
            if (this.customTabOrderChanged) {
                Services.PLATFORM.saveCustomTabOrder(getCustomTabOrder());
                this.customTabOrderChanged = false;
            }
            return true;
        }

        return super.mouseReleased(mouseX, mouseY, button);
    }

    /**
     * Draws the screen and all the components in it.
     */
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {

        int left = SIDE + (width - internalWidth) / 2;
        int top = TOP + (height - internalHeight) / 2;

        int right = internalWidth - SIDE + (width - internalWidth) / 2;
        int bottom = internalHeight - SIDE + (height - internalHeight) / 2;

        int width = right - left;
        int height = bottom - top;

        int maxTabs = Math.max(1, BetterAdvancementTabType.getMaxTabs(width, height));
        int skip = tabPage * maxTabs;

        this.renderBackground(guiGraphics, mouseX, mouseY, partialTicks);
        if (maxPages != 0) {
            Component page = Component.literal(String.format("%d / %d", tabPage + 1, maxPages + 1));
            int textWidth = this.font.width(page);
            guiGraphics.drawString(this.font, page.getVisualOrderText(), left + (internalWidth - textWidth) / 2 - textWidth, bottom + 8, -1);
            super.render(guiGraphics, mouseX, mouseY, partialTicks);
        }
        this.renderInside(guiGraphics, mouseX, mouseY, left, top, right, bottom, maxTabs, skip);
        this.renderWindow(guiGraphics, left, top, right, bottom, maxTabs, skip);
        //Don't draw tool tips if dragging an advancement
        if (this.advConnectedToMouse == null) {
            this.renderToolTips(guiGraphics, mouseX, mouseY, left, top, right, bottom, maxTabs, skip);
        }
        
        //Draw guide lines to all advancements at 45 or 90 degree angles.
        if (this.advConnectedToMouse != null)
        {
            for (BetterAdvancementWidget betterAdvancementEntryScreen : this.selectedTab.widgets.values()) {
                if (betterAdvancementEntryScreen != this.advConnectedToMouse)
                {
                    int contentWidth = right - left - 2 * PADDING;
                    int contentHeight = bottom - top - 3 * PADDING;
                    float treeScale = this.selectedTab.getTreeScale(contentWidth, contentHeight);
                    int advancementSize = Math.max(1, Math.round(BetterAdvancementWidget.ADVANCEMENT_SIZE * treeScale));
                    int widgetLayoutX = this.selectedTab.getLayoutX(betterAdvancementEntryScreen);
                    int widgetLayoutY = this.selectedTab.getLayoutY(betterAdvancementEntryScreen);
                    int draggedLayoutX = this.selectedTab.getLayoutX(this.advConnectedToMouse);
                    int draggedLayoutY = this.selectedTab.getLayoutY(this.advConnectedToMouse);
                    int x1 = left + PADDING + this.selectedTab.treeToViewX(this.selectedTab.scrollX + widgetLayoutX + 3, contentWidth);
                    int x2 = left + PADDING + this.selectedTab.treeToViewX(this.selectedTab.scrollX + draggedLayoutX + 3, contentWidth);
                    int y1 = top + 2 * PADDING + this.selectedTab.treeToViewY(this.selectedTab.scrollY + widgetLayoutY, contentHeight);
                    int y2 = top + 2 * PADDING + this.selectedTab.treeToViewY(this.selectedTab.scrollY + draggedLayoutY, contentHeight);
                    int centerX1 = x1 + advancementSize / 2;
                    int centerX2 = x2 + advancementSize / 2;
                    int centerY1 = y1 + advancementSize / 2;
                    int centerY2 = y2 + advancementSize / 2;
                    double degrees = Math.toDegrees(Math.atan2(centerX1 - centerX2, centerY1 - centerY2));
                    if (degrees < 0)
                    {
                        degrees += 360;
                    }
                    
                    if (widgetLayoutX == draggedLayoutX)
                    {
                        if (y1 > y2)
                        {
                            //Draw right
                            RenderUtil.drawRect(x1, y1 + advancementSize - 1, x2, y2, 1, 0x00FF00);
                            //Draw bottom for bottom
                            RenderUtil.drawRect(x1 + advancementSize - 1, y1 + advancementSize - 1, x2, y1 + advancementSize - 1, 1, 0x00FF00);
                            //Draw top for bottom
                            RenderUtil.drawRect(x1 + advancementSize - 1, y1, x2, y1, 1, 0x00FF00);
                            //Draw bottom for top
                            RenderUtil.drawRect(x1 + advancementSize - 1, y2 + advancementSize - 1, x2, y2 + advancementSize - 1, 1, 0x00FF00);
                            //Draw top for top
                            RenderUtil.drawRect(x1 + advancementSize - 1, y2, x2, y2, 1, 0x00FF00);
                            //Draw left
                            RenderUtil.drawRect(x1 + advancementSize - 1, y1 + advancementSize - 1, x2 + advancementSize - 1, y2, 1, 0x00FF00);
                        }
                        else
                        {
                            //Draw right
                            RenderUtil.drawRect(x1, y2 + advancementSize - 1, x2, y1, 1, 0x00FF00);
                            //Draw bottom for bottom
                            RenderUtil.drawRect(x1 + advancementSize - 1, y2 + advancementSize - 1, x2, y2 + advancementSize - 1, 1, 0x00FF00);
                            //Draw top for bottom
                            RenderUtil.drawRect(x1 + advancementSize - 1, y2, x2, y2, 1, 0x00FF00);
                            //Draw bottom for top
                            RenderUtil.drawRect(x1 + advancementSize - 1, y1 + advancementSize - 1, x2, y1 + advancementSize - 1, 1, 0x00FF00);
                            //Draw top for top
                            RenderUtil.drawRect(x1 + advancementSize - 1, y1, x2, y1, 1, 0x00FF00);
                            //Draw left
                            RenderUtil.drawRect(x1 + advancementSize - 1, y2 + advancementSize - 1, x2 + advancementSize - 1, y1, 1, 0x00FF00);
                        }
                    }
                    if (widgetLayoutY == draggedLayoutY)
                    {
                        if (x1 > x2)
                        {
                            //Draw top
                            RenderUtil.drawRect(x2, y1, x1 + advancementSize - 1, y2, 1, 0x00FF00);
                            //Draw left for right
                            RenderUtil.drawRect(x1, y1, x1, y2 + advancementSize - 1, 1, 0x00FF00);
                            //Draw right for right
                            RenderUtil.drawRect(x1 + advancementSize - 1, y1, x1 + advancementSize - 1, y2 + advancementSize - 1, 1, 0x00FF00);
                            //Draw left for left
                            RenderUtil.drawRect(x2, y1, x2, y2 + advancementSize - 1, 1, 0x00FF00);
                            //Draw right for left
                            RenderUtil.drawRect(x2 + advancementSize - 1, y1, x2 + advancementSize - 1, y2 + advancementSize - 1, 1, 0x00FF00);
                            //Draw bottom
                            RenderUtil.drawRect(x2, y1 + advancementSize - 1, x1 + advancementSize - 1, y2 + advancementSize - 1, 1, 0x00FF00);
                        }
                        else
                        {
                            //Draw left
                            RenderUtil.drawRect(x2 + advancementSize - 1, y1, x1, y2, 1, 0x00FF00);
                            //Draw left for right
                            RenderUtil.drawRect(x2, y1, x2, y2 + advancementSize - 1, 1, 0x00FF00);
                            //Draw right for right
                            RenderUtil.drawRect(x2 + advancementSize - 1, y1, x2 + advancementSize - 1, y2 + advancementSize - 1, 1, 0x00FF00);
                            //Draw left for left
                            RenderUtil.drawRect(x1, y1, x1, y2 + advancementSize - 1, 1, 0x00FF00);
                            //Draw right for left
                            RenderUtil.drawRect(x1 + advancementSize - 1, y1, x1 + advancementSize - 1, y2 + advancementSize - 1, 1, 0x00FF00);
                            //Draw right
                            RenderUtil.drawRect(x2 + advancementSize - 1, y1 + advancementSize - 1, x1, y2 + advancementSize - 1, 1, 0x00FF00);
                        }
                    }
                    if (degrees == 45 || degrees == 135 || degrees == 225 || degrees == 315)
                    {
                        //Draw lines around each advancement
                        //First
                        //Top
                        RenderUtil.drawRect(x1, y1, x1 + advancementSize - 1, y1, 1, 0x00FF00);
                        //Bottom
                        RenderUtil.drawRect(x1, y1 + advancementSize - 1, x1 + advancementSize - 1, y1 + advancementSize - 1, 1, 0x00FF00);
                        //Left
                        RenderUtil.drawRect(x1, y1, x1, y1 + advancementSize - 1, 1, 0x00FF00);
                        //Right
                        RenderUtil.drawRect(x1 + advancementSize - 1, y1, x1 + advancementSize - 1, y1 + advancementSize - 1, 1, 0x00FF00);
                        //Second
                        //Top
                        RenderUtil.drawRect(x2, y2, x2 + advancementSize - 1, y2, 1, 0x00FF00);
                        //Bottom
                        RenderUtil.drawRect(x2, y2 + advancementSize - 1, x2 + advancementSize - 1, y2 + advancementSize - 1, 1, 0x00FF00);
                        //Left
                        RenderUtil.drawRect(x2, y2, x2, y2 + advancementSize - 1, 1, 0x00FF00);
                        //Right
                        RenderUtil.drawRect(x2 + advancementSize - 1, y2, x2 + advancementSize - 1, y2 + advancementSize - 1, 1, 0x00FF00);
                        
                        if (degrees == 45 || degrees == 225)
                        {
                            RenderUtil.drawRect(x1, y1 + advancementSize - 1, x2, y2 + advancementSize - 1, 1, 0x00FF00);
                            RenderUtil.drawRect(x1 + advancementSize - 1, y1, x2 + advancementSize - 1, y2, 1, 0x00FF00);
                        }
                        else if (degrees == 135 || degrees == 315)
                        {
                            RenderUtil.drawRect(x1, y1, x2, y2, 1, 0x00FF00);
                            RenderUtil.drawRect(x1 + advancementSize - 1, y1 + advancementSize - 1, x2 + advancementSize - 1, y2 + advancementSize - 1, 1, 0x00FF00);
                        }
                    }
                }
            }
        }

        if (BetterAdvancementsScreen.showDebugCoordinates && this.selectedTab != null && mouseX < internalWidth - SIDE - PADDING && mouseX > SIDE + PADDING && mouseY < internalHeight - top + 1 && mouseY > top + PADDING * 2) {
            //If dragging an advancement, draw coordinates of advancement being moved instead of mouse coordinates
            int contentWidth = right - left - 2 * PADDING;
            int contentHeight = bottom - top - 3 * PADDING;
            if (this.advConnectedToMouse != null) {
                int layoutX = this.selectedTab.getLayoutX(this.advConnectedToMouse);
                int layoutY = this.selectedTab.getLayoutY(this.advConnectedToMouse);
                int currentX = left + PADDING + this.selectedTab.treeToViewX(
                    this.selectedTab.scrollX + layoutX + 3, contentWidth);
                int currentY = top + 2 * PADDING + this.selectedTab.treeToViewY(
                    this.selectedTab.scrollY + layoutY, contentHeight) - font.lineHeight;

                guiGraphics.drawString(font, this.advConnectedToMouse.x + "," + this.advConnectedToMouse.y, currentX, currentY, 0x000000);
            } else {
                // Display the original, unrotated advancement coordinates under the mouse.
                int xMouse = mouseX - left - PADDING;
                int yMouse = mouseY - top - 2 * PADDING;
                int currentX = this.selectedTab.viewToRawX(xMouse, yMouse, contentWidth, contentHeight);
                int currentY = this.selectedTab.viewToRawY(xMouse, yMouse, contentWidth, contentHeight);

                guiGraphics.drawString(font, currentX + "," + currentY, mouseX, mouseY - font.lineHeight, 0x000000);
            }
        }
    }

    private List<BetterAdvancementTab> getOrderedTabs() {
        List<BetterAdvancementTab> ordered = new ArrayList<>(this.tabs.values());
        TabSortMode mode = getTabSortMode();

        Comparator<BetterAdvancementTab> alphabetical = Comparator
            .comparing((BetterAdvancementTab tab) -> tab.getTitle().getString().toLowerCase(Locale.ROOT))
            .thenComparing(tab -> tab.getRootNode().holder().id().toString());

        switch (mode) {
            case ALPHABETICAL -> ordered.sort(alphabetical);
            case COMPLETION -> {
                // Completion is derived from every displayed advancement in a tab. Cache it once per
                // ordering pass so Comparator calls do not repeatedly walk every widget each frame.
                Map<BetterAdvancementTab, Float> completion = new IdentityHashMap<>();
                for (BetterAdvancementTab tab : ordered) {
                    completion.put(tab, tab.getCompletionProgress());
                }
                ordered.sort(
                    Comparator.comparingDouble((BetterAdvancementTab tab) -> completion.get(tab))
                        .reversed()
                        .thenComparing(alphabetical)
                );
            }
            case CUSTOM -> {
                Map<String, Integer> positions = new java.util.HashMap<>();
                for (int i = 0; i < customTabOrder.size(); i++) {
                    positions.putIfAbsent(customTabOrder.get(i), i);
                }
                ordered.sort(Comparator.comparingInt(tab ->
                    positions.getOrDefault(tab.getRootNode().holder().id().toString(), Integer.MAX_VALUE)
                ));
            }
            case ORIGINAL -> {
                // LinkedHashMap insertion order is already the original root order.
            }
        }

        return ordered;
    }

    private TabHit findTabAt(
        double mouseX,
        double mouseY,
        int left,
        int top,
        int tabAreaWidth,
        int tabAreaHeight,
        int maxTabs
    ) {
        if (maxTabs <= 0) {
            return null;
        }

        List<BetterAdvancementTab> orderedTabs = this.getOrderedTabs();
        int skip = tabPage * maxTabs;
        int end = Math.min(skip + maxTabs, orderedTabs.size());

        for (int displayIndex = skip; displayIndex < end; displayIndex++) {
            BetterAdvancementTab tab = orderedTabs.get(displayIndex);
            if (tab.isMouseOver(left, top, tabAreaWidth, tabAreaHeight, displayIndex, mouseX, mouseY)) {
                return new TabHit(tab, displayIndex);
            }
        }

        return null;
    }

    private void seedCustomTabOrder() {
        LinkedHashSet<String> merged = new LinkedHashSet<>();
        for (BetterAdvancementTab tab : this.getOrderedTabs()) {
            merged.add(tab.getRootNode().holder().id().toString());
        }
        merged.addAll(customTabOrder);
        setCustomTabOrder(merged);
    }

    private boolean moveCustomTab(BetterAdvancementTab dragged, BetterAdvancementTab target) {
        String draggedId = dragged.getRootNode().holder().id().toString();
        String targetId = target.getRootNode().holder().id().toString();
        int draggedIndex = customTabOrder.indexOf(draggedId);
        int targetIndex = customTabOrder.indexOf(targetId);

        if (draggedIndex < 0 || targetIndex < 0 || draggedIndex == targetIndex) {
            return false;
        }

        // Move instead of swapping. A fast drag can skip intermediate tabs; swapping in that
        // case would unexpectedly move the target all the way back to the dragged tab's old
        // position. Removing and inserting keeps every intervening tab in relative order.
        customTabOrder.remove(draggedIndex);
        customTabOrder.add(Math.min(targetIndex, customTabOrder.size()), draggedId);
        return true;
    }

    private record TabHit(BetterAdvancementTab tab, int displayIndex) {}

    private void renderInside(GuiGraphics guiGraphics, int mouseX, int mouseY, int left, int top, int right, int bottom, int maxTabs, int skip) {
        BetterAdvancementTab betterAdvancementTab = this.selectedTab;
        int boxLeft = left + PADDING;
        int boxTop = top + 2*PADDING;
        int boxRight = right - PADDING;
        int boxBottom = bottom - PADDING;

        int width = boxRight - boxLeft;
        int height = boxBottom - boxTop;

        if (betterAdvancementTab == null) {
            guiGraphics.fill(boxLeft, boxTop, boxRight, boxBottom, -16777216);
            guiGraphics.drawString(this.font, NO_ADVANCEMENTS_LABEL, boxLeft + (width - this.font.width(NO_ADVANCEMENTS_LABEL)) / 2, boxTop + height / 2 - this.font.lineHeight, -1);
            guiGraphics.drawString(this.font, VERY_SAD_LABEL, boxLeft + (width - this.font.width(VERY_SAD_LABEL)) / 2, boxTop + height / 2 + this.font.lineHeight, -1);
        } else {
            betterAdvancementTab.drawContents(guiGraphics, boxLeft, boxTop, width, height);
        }
    }

    public void renderWindow(GuiGraphics guiGraphics, int left, int top, int right, int bottom, int maxTabs, int skip) {
        RenderSystem.enableBlend();
        // Top left corner
        guiGraphics.blit(Resources.Gui.WINDOW, left, top, 0, 0, CORNER_SIZE, CORNER_SIZE);
        // Top side
        RenderUtil.renderRepeating(Resources.Gui.WINDOW, guiGraphics, left + CORNER_SIZE, top, internalWidth - CORNER_SIZE - 2*SIDE - CORNER_SIZE, CORNER_SIZE, CORNER_SIZE, 0, WIDTH - CORNER_SIZE - CORNER_SIZE, CORNER_SIZE);
        // Top right corner
        guiGraphics.blit(Resources.Gui.WINDOW, right - CORNER_SIZE, top, WIDTH - CORNER_SIZE, 0, CORNER_SIZE, CORNER_SIZE);
        // Left side
        RenderUtil.renderRepeating(Resources.Gui.WINDOW, guiGraphics, left, top + CORNER_SIZE, CORNER_SIZE, bottom - top - 2 * CORNER_SIZE, 0, CORNER_SIZE, CORNER_SIZE, HEIGHT - CORNER_SIZE - CORNER_SIZE);
        // Right side
        RenderUtil.renderRepeating(Resources.Gui.WINDOW, guiGraphics, right - CORNER_SIZE, top + CORNER_SIZE, CORNER_SIZE, bottom - top - 2 * CORNER_SIZE, WIDTH - CORNER_SIZE, CORNER_SIZE, CORNER_SIZE, HEIGHT - CORNER_SIZE - CORNER_SIZE);
        // Bottom left corner
        guiGraphics.blit(Resources.Gui.WINDOW, left, bottom - CORNER_SIZE, 0, HEIGHT - CORNER_SIZE, CORNER_SIZE, CORNER_SIZE);
        // Bottom side
        RenderUtil.renderRepeating(Resources.Gui.WINDOW, guiGraphics, left + CORNER_SIZE, bottom - CORNER_SIZE, internalWidth - CORNER_SIZE - 2*SIDE - CORNER_SIZE, CORNER_SIZE, CORNER_SIZE, HEIGHT - CORNER_SIZE, WIDTH - CORNER_SIZE - CORNER_SIZE, CORNER_SIZE);
        // Bottom right corner
        guiGraphics.blit(Resources.Gui.WINDOW, right - CORNER_SIZE, bottom - CORNER_SIZE, WIDTH - CORNER_SIZE, HEIGHT - CORNER_SIZE, CORNER_SIZE, CORNER_SIZE);

        int width = right - left;
        int height = bottom - top;

        if (this.tabs.size() > 1) {
            List<BetterAdvancementTab> orderedTabs = this.getOrderedTabs();
            int end = Math.min(skip + maxTabs, orderedTabs.size());

            for (int displayIndex = skip; displayIndex < end; displayIndex++) {
                BetterAdvancementTab tab = orderedTabs.get(displayIndex);
                tab.drawTab(guiGraphics, left, top, width, height, tab == this.selectedTab, displayIndex);
            }

            RenderSystem.defaultBlendFunc();

            for (int displayIndex = skip; displayIndex < end; displayIndex++) {
                orderedTabs.get(displayIndex).drawIcon(guiGraphics, left, top, width, height, displayIndex);
            }

            RenderSystem.disableBlend();
        }

        FormattedCharSequence windowTitle = TITLE.getVisualOrderText();
        if (selectedTab != null) {
            windowTitle = FormattedCharSequence.composite(
                windowTitle,
                  Component.literal(" - ").getVisualOrderText(),
                selectedTab.getTitle().getVisualOrderText()
            );
        }
        guiGraphics.drawString(this.font, windowTitle, left + 8, top + 6, 4210752, false);
    }

    private void renderToolTips(GuiGraphics guiGraphics, int mouseX, int mouseY, int left, int top, int right, int bottom, int maxTabs, int skip) {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        if (this.selectedTab != null) {
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(left + PADDING, top + 2*PADDING, 400.0D);
            RenderSystem.enableDepthTest();
            this.selectedTab.drawToolTips(
                guiGraphics,
                mouseX - left - PADDING,
                mouseY - top - 2 * PADDING,
                left + PADDING,
                top + 2 * PADDING,
                right - left - 2 * PADDING,
                bottom - top - 3 * PADDING);
            RenderSystem.disableDepthTest();
            guiGraphics.pose().popPose();
        }

        int width = right - left;
        int height = bottom - top;

        if (this.tabs.size() > 1) {
            List<BetterAdvancementTab> orderedTabs = this.getOrderedTabs();
            int end = Math.min(skip + maxTabs, orderedTabs.size());
            for (int displayIndex = skip; displayIndex < end; displayIndex++) {
                BetterAdvancementTab tab = orderedTabs.get(displayIndex);
                if (tab.isMouseOver(left, top, width, height, displayIndex, mouseX, mouseY)) {
                    guiGraphics.renderTooltip(this.font, tab.getTitle(), mouseX, mouseY);
                }
            }
        }
    }

    @Override
    public void onAddAdvancementRoot(AdvancementNode advancement) {
        BetterAdvancementTab betterAdvancementTabGui = BetterAdvancementTab.create(this.minecraft, this, advancement);

        if (betterAdvancementTabGui != null) {
            this.tabs.put(advancement.holder(), betterAdvancementTabGui);
        }
    }

    @Override
    public void onRemoveAdvancementRoot(AdvancementNode advancement) {
    }

    @Override
    public void onAddAdvancementTask(AdvancementNode advancement) {
        BetterAdvancementTab betterAdvancementTabGui = this.getTab(advancement);

        if (betterAdvancementTabGui != null) {
            betterAdvancementTabGui.addAdvancement(advancement);
        }
    }

    @Override
    public void onRemoveAdvancementTask(AdvancementNode advancement) {
    }

    @Override
    public void onUpdateAdvancementProgress(AdvancementNode advancement, AdvancementProgress advancementProgress) {
        BetterAdvancementWidget betterAdvancementEntryScreen = this.getAdvancementWidget(advancement);

        if (betterAdvancementEntryScreen != null) {
            betterAdvancementEntryScreen.getAdvancementProgress(advancementProgress);
        }
    }

    @Override
    public void onSelectedTabChanged(AdvancementHolder advancement) {
        if (this.selectedTab != null) {
            this.selectedTab.storeScroll();
        }
        this.selectedTab = this.tabs.get(advancement);
        if (this.selectedTab != null) {
            this.selectedTab.loadScroll();
        }
    }

    @Override
    public void onAdvancementsCleared() {
        this.tabs.clear();
        this.selectedTab = null;
        this.draggedTab = null;
        this.customTabOrderChanged = false;
    }

    public BetterAdvancementWidget getAdvancementWidget(AdvancementNode advancement) {
        BetterAdvancementTab betterAdvancementTab = this.getTab(advancement);
        return betterAdvancementTab == null ? null : betterAdvancementTab.getWidget(advancement.holder());
    }

    private BetterAdvancementTab getTab(AdvancementNode advancement) {
        AdvancementNode advancementNode = advancement.root();
        return this.tabs.get(advancementNode.holder());
    }
}