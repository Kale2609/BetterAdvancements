package betteradvancements.common.gui;

import betteradvancements.common.advancements.BetterDisplayInfo;
import betteradvancements.common.api.IBetterAdvancementEntryGui;
import betteradvancements.common.api.event.IAdvancementDrawConnectionsEvent;
import betteradvancements.common.platform.Services;
import betteradvancements.common.reference.Resources;
import betteradvancements.common.util.CriterionGrid;
import betteradvancements.common.util.RenderUtil;
import com.google.common.collect.Lists;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.advancements.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.StringSplitter;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.advancements.AdvancementWidgetType;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;

import java.util.Collections;
import java.util.List;

public class BetterAdvancementWidget implements IBetterAdvancementEntryGui {
    protected static final int ADVANCEMENT_SIZE = 26;
    private static final int CORNER_SIZE = 10;
    private static final int WIDGET_WIDTH = 256, WIDGET_HEIGHT = 26, TITLE_SIZE = 32, ICON_OFFSET = 128, ICON_SIZE = 26;

    private final BetterAdvancementTab betterAdvancementTabGui;
    private final AdvancementNode advancementNode;
    protected final BetterDisplayInfo betterDisplayInfo;
    private final DisplayInfo displayInfo;
    private final String title;
    private int width;
    private List<FormattedCharSequence> description;
    private CriterionGrid criterionGrid;
    private int criterionPage;
    private int criterionPageCount = 1;
    private boolean criterionPanelDetached;
    private boolean detachedPanelHitBoxValid;
    private int detachedPanelX;
    private int detachedPanelY;
    private int detachedPanelWidth;
    private int detachedPanelHeight;
    private final Minecraft minecraft;
    private BetterAdvancementWidget parent;
    private final List<BetterAdvancementWidget> children = Lists.newArrayList();
    private AdvancementProgress advancementProgress;
    protected int x, y;
    private final int screenScale;

    public BetterAdvancementWidget(BetterAdvancementTab betterAdvancementTabGui, Minecraft mc, AdvancementNode advancementNode, DisplayInfo displayInfo) {
        this.betterAdvancementTabGui = betterAdvancementTabGui;
        this.advancementNode = advancementNode;
        this.betterDisplayInfo = betterAdvancementTabGui.getBetterDisplayInfo(this.advancementNode);
        this.displayInfo = displayInfo;
        this.minecraft = mc;
        this.title = displayInfo.getTitle().getString(163);
        this.x = this.betterDisplayInfo.getPosX() != null ? this.betterDisplayInfo.getPosX() : Mth.floor(displayInfo.getX() * 32.0F);
        this.y = this.betterDisplayInfo.getPosY() != null ? this.betterDisplayInfo.getPosY() : Mth.floor(displayInfo.getY() * 27.0F);
        this.refreshHover();
        this.screenScale = mc.getWindow().calculateScale(0, false);
    }

    private void refreshHover() {
        int fallbackWidth = Math.max(64, this.betterAdvancementTabGui.getScreen().width - 32);
        this.refreshHover(fallbackWidth);
    }

    private void refreshHover(int availableWidth) {
        Minecraft mc = this.minecraft;
        int maxPanelWidth = Math.max(1, availableWidth);
        int maxContentWidth = Math.max(1, maxPanelWidth - 8);

        int progressWidth = 0;
        if (this.advancementProgress != null && this.advancementProgress.getProgressText() != null) {
            progressWidth = mc.font.width(this.advancementProgress.getProgressText());
        }

        // 32 px for the icon/title offset, 5 px right padding and, when present,
        // another 8 px gap before the progress counter.
        int requiredHeaderWidth = 32 + mc.font.width(this.title) + 5;
        if (progressWidth > 0) {
            requiredHeaderWidth += progressWidth + 8;
        }
        int titleWidth = Math.min(maxContentWidth, requiredHeaderWidth);
        this.criterionGrid = CriterionGrid.findOptimalCriterionGrid(
            this.advancementNode.holder(),
            this.advancementNode.advancement(),
            this.advancementProgress,
            maxContentWidth,
            mc.font
        );

        int maxWidth;
        if (!CriterionGrid.requiresShift || Screen.hasShiftDown()) {
            maxWidth = Math.max(titleWidth, Math.min(maxContentWidth, this.criterionGrid.width));
        } else {
            maxWidth = titleWidth;
        }

        this.description = Language.getInstance().getVisualOrder(
            this.findOptimalLines(
                ComponentUtils.mergeStyles(
                    displayInfo.getDescription().copy(),
                    Style.EMPTY.withColor(displayInfo.getType().getChatColor())
                ),
                maxWidth,
                maxContentWidth
            )
        );

        for (FormattedCharSequence line : this.description) {
            maxWidth = Math.max(maxWidth, Math.min(maxContentWidth, mc.font.width(line)));
        }

        this.width = Math.min(maxPanelWidth, maxWidth + 8);
    }

    private List<FormattedText> findOptimalLines(Component line, int width, int maxWidth) {
        if (line.getString().isEmpty()) {
            return Collections.emptyList();
        }

        StringSplitter stringsplitter = this.minecraft.font.getSplitter();
        int safeWidth = Math.max(1, Math.min(width, maxWidth));
        List<FormattedText> list = stringsplitter.splitLines(line, safeWidth, Style.EMPTY);

        if (list.size() > 1) {
            int preferredWidth = Math.max(safeWidth, this.betterAdvancementTabGui.getScreen().internalWidth / 4);
            safeWidth = Math.min(maxWidth, preferredWidth);
            list = stringsplitter.splitLines(line, safeWidth, Style.EMPTY);
        }

        while (list.size() > 5 && safeWidth < maxWidth) {
            int nextWidth = Math.min(maxWidth, safeWidth + Math.max(1, safeWidth / 4));
            if (nextWidth == safeWidth) {
                break;
            }
            safeWidth = nextWidth;
            list = stringsplitter.splitLines(line, safeWidth, Style.EMPTY);
        }

        return list;
    }

    private BetterAdvancementWidget getFirstVisibleParent(AdvancementNode advancement) {
        do {
            advancement = advancement.parent();
        } while(advancement != null && advancement.advancement().display().isEmpty());

        if (advancement != null && !advancement.advancement().display().isEmpty()) {
            return this.betterAdvancementTabGui.getWidget(advancement.holder());
        } else {
            return null;
        }
    }

    public void drawConnectivity(GuiGraphics guiGraphics, int scrollX, int scrollY, boolean drawInside) {
        //Check if connections should be drawn at all
        if (!this.betterDisplayInfo.hideLines()) {
            //Draw connection to parent
            if (this.parent != null) {
                
                this.drawConnection(guiGraphics, this.parent, scrollX, scrollY, drawInside);
            }
            
            //Create and post event to get extra connections
            IAdvancementDrawConnectionsEvent event = Services.PLATFORM.getEventHelper().postAdvancementDrawConnectionsEvent(this.advancementNode);

            //Draw extra connections from event
            for (AdvancementHolder parent : event.getExtraConnections()) {
                final BetterAdvancementWidget parentGui = this.betterAdvancementTabGui.getWidget(parent);
                
                if (parentGui != null) {
                    this.drawConnection(guiGraphics, parentGui, scrollX, scrollY, drawInside);
                }
            }
        }
        //Draw child connections
        for (BetterAdvancementWidget betterAdvancementWidget : this.children) {
            betterAdvancementWidget.drawConnectivity(guiGraphics, scrollX, scrollY, drawInside);
        }
    }
    
    /**
     * Draws connection line between this advancement and the advancement supplied in parent.
     */
    public void drawConnection(GuiGraphics guiGraphics, BetterAdvancementWidget parent, int scrollX, int scrollY, boolean drawInside) {
        int innerLineColor = this.advancementProgress != null && this.advancementProgress.isDone() ? betterDisplayInfo.getCompletedLineColor() : betterDisplayInfo.getUnCompletedLineColor();
        int borderLineColor = 0xFF000000;
        
        int thisLayoutX = this.betterAdvancementTabGui.getLayoutX(this);
        int thisLayoutY = this.betterAdvancementTabGui.getLayoutY(this);
        int parentLayoutX = this.betterAdvancementTabGui.getLayoutX(parent);
        int parentLayoutY = this.betterAdvancementTabGui.getLayoutY(parent);

        if (this.betterDisplayInfo.drawDirectLines()) {
            float x1 = scrollX + thisLayoutX + ADVANCEMENT_SIZE / 2 + 3;
            float y1 = scrollY + thisLayoutY + ADVANCEMENT_SIZE / 2;
            float x2 = scrollX + parentLayoutX + ADVANCEMENT_SIZE / 2 + 3;
            float y2 = scrollY + parentLayoutY + ADVANCEMENT_SIZE / 2;

            float width;
            boolean perpendicular = x1 == x2 || y1 == y2;
            
            if (!perpendicular) {
                switch (this.screenScale) {
                    case 1 -> width = drawInside ? 1.5F : 0.5F;
                    case 2 -> width = drawInside ? 2.25F : 0.75F;
                    case 3 -> width = drawInside ? 2F : 0.6666666666666667F;
                    case 4 -> width = drawInside ? 2.125F : 0.625F;
                    default -> width = drawInside ? 3 : 1;
                }
                if (drawInside) {
                    RenderUtil.drawRect(x1 - .75F, y1 - .75F, x2 - .75F, y2 - .75F, width, borderLineColor);
                }
                else {
                    RenderUtil.drawRect(x1, y1, x2, y2, width, innerLineColor);
                }
            }
            else {
                width = drawInside ? 3 : 1;
                
                if (drawInside) {
                    RenderUtil.drawRect(x1 - 1, y1 - 1, x2 - 1, y2 - 1, width, borderLineColor);
                }
                else {
                    RenderUtil.drawRect(x1, y1, x2, y2, width, innerLineColor);
                }
            }
        }
        else if (this.betterAdvancementTabGui.isTreeRotated()) {
            // The normal Minecraft layout grows left-to-right. After rotating a portrait tree,
            // preserve the same elbow shape, rotated 90 degrees, so descendants grow downward.
            int startX = scrollX + parentLayoutX + ADVANCEMENT_SIZE / 2;
            int startY = scrollY + parentLayoutY + ADVANCEMENT_SIZE / 2;
            int endYHalf = scrollY + parentLayoutY + ADVANCEMENT_SIZE + 6; // rotated 32 px step
            int endX = scrollX + thisLayoutX + ADVANCEMENT_SIZE / 2;
            int endY = scrollY + thisLayoutY + ADVANCEMENT_SIZE / 2;

            if (drawInside) {
                guiGraphics.vLine(startX - 1, endYHalf, startY, borderLineColor);
                guiGraphics.vLine(startX, endYHalf + 1, startY, borderLineColor);
                guiGraphics.vLine(startX + 1, endYHalf, startY, borderLineColor);
                guiGraphics.vLine(endX - 1, endY, endYHalf - 1, borderLineColor);
                guiGraphics.vLine(endX, endY, endYHalf - 1, borderLineColor);
                guiGraphics.vLine(endX + 1, endY, endYHalf - 1, borderLineColor);
                guiGraphics.hLine(endX, startX, endYHalf - 1, borderLineColor);
                guiGraphics.hLine(endX, startX, endYHalf + 1, borderLineColor);
            } else {
                guiGraphics.vLine(startX, endYHalf, startY, innerLineColor);
                guiGraphics.vLine(endX, endY, endYHalf, innerLineColor);
                guiGraphics.hLine(endX, startX, endYHalf, innerLineColor);
            }
        }
        else {
            int startX = scrollX + parentLayoutX + ADVANCEMENT_SIZE / 2;
            int endXHalf = scrollX + parentLayoutX + ADVANCEMENT_SIZE + 6; // 6 = 32 - 26
            int startY = scrollY + parentLayoutY + ADVANCEMENT_SIZE / 2;
            int endX = scrollX + thisLayoutX + ADVANCEMENT_SIZE / 2;
            int endY = scrollY + thisLayoutY + ADVANCEMENT_SIZE / 2;
            
            if (drawInside) {
                guiGraphics.hLine(endXHalf, startX, startY - 1, borderLineColor);
                guiGraphics.hLine(endXHalf + 1, startX, startY, borderLineColor);
                guiGraphics.hLine(endXHalf, startX, startY + 1, borderLineColor);
                guiGraphics.hLine(endX, endXHalf - 1, endY - 1, borderLineColor);
                guiGraphics.hLine(endX, endXHalf - 1, endY, borderLineColor);
                guiGraphics.hLine(endX, endXHalf - 1, endY + 1, borderLineColor);
                guiGraphics.vLine(endXHalf - 1, endY, startY, borderLineColor);
                guiGraphics.vLine(endXHalf + 1, endY, startY, borderLineColor);
            } else {
                guiGraphics.hLine(endXHalf, startX, startY, innerLineColor);
                guiGraphics.hLine(endX, endXHalf, endY, innerLineColor);
                guiGraphics.vLine(endXHalf, endY, startY, innerLineColor);
            }
        }
    }

    public void draw(GuiGraphics guiGraphics, int scrollX, int scrollY) {
        int layoutX = this.betterAdvancementTabGui.getLayoutX(this);
        int layoutY = this.betterAdvancementTabGui.getLayoutY(this);
        if (!this.displayInfo.isHidden() || this.advancementProgress != null && this.advancementProgress.isDone()) {
            float f = this.advancementProgress == null ? 0.0F : this.advancementProgress.getPercent();
            AdvancementWidgetType advancementState;

            if (f >= 1.0F) {
                advancementState = AdvancementWidgetType.OBTAINED;
            } else {
                advancementState = AdvancementWidgetType.UNOBTAINED;
            }

            RenderUtil.setColor(betterDisplayInfo.getIconColor(advancementState));
            RenderSystem.enableBlend();
            guiGraphics.blitSprite(advancementState.frameSprite(this.displayInfo.getType()), scrollX + layoutX + 3, scrollY + layoutY, ICON_SIZE, ICON_SIZE);
            RenderUtil.setColor(betterDisplayInfo.defaultIconColor());
            guiGraphics.renderFakeItem(this.displayInfo.getIcon(), scrollX + layoutX + 8, scrollY + layoutY + 5);
        }

        for (BetterAdvancementWidget betterAdvancementWidget : this.children) {
            betterAdvancementWidget.draw(guiGraphics, scrollX, scrollY);
        }
    }

    public boolean isCompleted() {
        return this.advancementProgress != null && this.advancementProgress.isDone();
    }

    public void getAdvancementProgress(AdvancementProgress advancementProgressIn) {
        this.advancementProgress = advancementProgressIn;
        this.criterionPage = 0;
        this.criterionPageCount = 1;
        this.criterionPanelDetached = false;
        this.detachedPanelHitBoxValid = false;
        this.refreshHover();
    }

    public void addGuiAdvancement(BetterAdvancementWidget betterAdvancementEntryScreen) {
        this.children.add(betterAdvancementEntryScreen);
    }

    public void drawHover(
        GuiGraphics guiGraphics,
        int nodeX,
        int nodeY,
        float fade,
        int contentWidth,
        int contentHeight,
        int contentScreenX,
        int contentScreenY
    ) {
        this.refreshHover(contentWidth);
        this.detachedPanelHitBoxValid = false;

        // A very small custom UI scale can leave less room than the tooltip sprites themselves.
        // In that case, do not attempt negative-sized 9-slice rendering.
        if (contentWidth < CORNER_SIZE * 2 || contentHeight < WIDGET_HEIGHT) {
            this.criterionPage = 0;
            this.criterionPageCount = 1;
            this.criterionPanelDetached = false;
            return;
        }

        String progressText = this.advancementProgress == null || this.advancementProgress.getProgressText() == null
            ? null
            : this.advancementProgress.getProgressText().getString();
        int progressTextWidth = progressText == null ? 0 : this.minecraft.font.width(progressText);
        int renderWidth = this.width;

        boolean showCriteria = this.criterionGrid != null
            && !this.criterionGrid.isEmpty()
            && (!CriterionGrid.requiresShift || Screen.hasShiftDown());

        int descriptionHeight = this.description.size() * this.minecraft.font.lineHeight;
        int fullCriteriaHeight = showCriteria ? this.criterionGrid.height : 0;
        int fullBoxHeight = TITLE_SIZE + descriptionHeight + fullCriteriaHeight;

        // Detach only when the criteria make the complete hover body too tall for both
        // the space below and the space above the hovered node. Horizontal pressure alone
        // is handled by clamping the normal tooltip inside the content area.
        int spaceBelow = Math.max(0, contentHeight - nodeY);
        int spaceAbove = Math.max(0, nodeY + ADVANCEMENT_SIZE);
        boolean verticalFits = fullBoxHeight <= Math.max(spaceBelow, spaceAbove);
        boolean detached = showCriteria && !verticalFits;
        this.criterionPanelDetached = detached;

        CriterionGrid pageGrid;
        int pageIndicatorHeight = 0;
        int boxHeight;
        int drawX;
        int titleY;
        int backgroundY;
        int descriptionY;
        boolean drawLeft = false;
        boolean drawAbove = false;

        if (detached) {
            int availableCriteriaHeight = contentHeight - TITLE_SIZE - descriptionHeight;
            boolean criteriaFit = availableCriteriaHeight >= this.minecraft.font.lineHeight;
            List<CriterionGrid> pages = List.of();

            if (criteriaFit) {
                int rowsPerPage = Math.max(1, availableCriteriaHeight / this.minecraft.font.lineHeight);
                int criterionContentWidth = Math.max(1, contentWidth - 8);
                pages = this.criterionGrid.getPages(rowsPerPage, criterionContentWidth);

                // If pagination is necessary, always reserve one visible line for its indicator
                // when the window can hold at least one criterion row plus the indicator.
                if (pages.size() > 1 && availableCriteriaHeight >= this.minecraft.font.lineHeight * 2) {
                    pageIndicatorHeight = this.minecraft.font.lineHeight;
                    rowsPerPage = Math.max(
                        1,
                        (availableCriteriaHeight - pageIndicatorHeight) / this.minecraft.font.lineHeight
                    );
                    pages = this.criterionGrid.getPages(rowsPerPage, criterionContentWidth);
                }

                this.criterionPageCount = Math.max(1, pages.size());
                this.criterionPage = Mth.clamp(this.criterionPage, 0, this.criterionPageCount - 1);
                pageGrid = pages.get(this.criterionPage);
            } else {
                this.criterionPage = 0;
                this.criterionPageCount = 1;
                pageGrid = null;
            }

            // Size a detached panel to its actual content instead of retaining the width of
            // the full pre-pagination grid. This removes the large empty block visible when a
            // narrow page was rendered inside a very wide original grid.
            int headerContentWidth = 32 + this.minecraft.font.width(this.title) + 5;
            if (progressTextWidth > 0) {
                headerContentWidth += progressTextWidth + 8;
            }
            int descriptionContentWidth = 0;
            for (FormattedCharSequence line : this.description) {
                descriptionContentWidth = Math.max(descriptionContentWidth, this.minecraft.font.width(line));
            }
            int criteriaContentWidth = 0;
            for (CriterionGrid page : pages) {
                criteriaContentWidth = Math.max(criteriaContentWidth, page.width);
            }
            int pageIndicatorContentWidth = 0;
            if (this.criterionPageCount > 1) {
                pageIndicatorContentWidth = this.minecraft.font.width(
                    Component.translatable("betteradvancements.criteria_page", this.criterionPage + 1, this.criterionPageCount)
                );
            }
            int detachedWidth = Math.max(
                CORNER_SIZE * 2,
                Math.min(
                    contentWidth,
                    Math.max(
                        headerContentWidth,
                        Math.max(descriptionContentWidth, Math.max(criteriaContentWidth, pageIndicatorContentWidth))
                    ) + 8
                )
            );
            renderWidth = detachedWidth;

            int criteriaHeight = pageGrid == null ? 0 : pageGrid.height;
            boxHeight = TITLE_SIZE + descriptionHeight + criteriaHeight + pageIndicatorHeight;
            boxHeight = Math.min(contentHeight, Math.max(TITLE_SIZE, boxHeight));

            drawX = Mth.clamp(nodeX, 0, Math.max(0, contentWidth - renderWidth));
            titleY = 0;
            backgroundY = 0;
            descriptionY = WIDGET_HEIGHT;

            this.detachedPanelX = drawX;
            this.detachedPanelY = backgroundY;
            this.detachedPanelWidth = renderWidth;
            this.detachedPanelHeight = boxHeight;
            this.detachedPanelHitBoxValid = this.criterionPageCount > 1;

            guiGraphics.enableScissor(
                contentScreenX + drawX,
                contentScreenY + backgroundY,
                contentScreenX + drawX + renderWidth,
                contentScreenY + backgroundY + boxHeight
            );
        } else {
            // Normal-sized hover windows retain the original BetterAdvancements behaviour:
            // title/icon remain attached to the hovered node and the body opens below it when
            // possible, otherwise above it. No pagination is necessary in this mode.
            this.criterionPage = 0;
            this.criterionPageCount = 1;
            pageGrid = showCriteria ? this.criterionGrid : null;
            boxHeight = fullBoxHeight;

            int rightDrawX = nodeX;
            int leftDrawX = nodeX - renderWidth + ADVANCEMENT_SIZE + 6;
            boolean rightFits = rightDrawX + renderWidth <= contentWidth;
            boolean leftFits = leftDrawX >= 0;
            drawLeft = rightFits ? false : leftFits || nodeX > contentWidth / 2;
            int preferredDrawX = drawLeft ? leftDrawX : rightDrawX;
            drawX = Mth.clamp(preferredDrawX, 0, Math.max(0, contentWidth - renderWidth));

            boolean belowFits = fullBoxHeight <= spaceBelow;
            boolean aboveFits = fullBoxHeight <= spaceAbove;
            drawAbove = !belowFits && aboveFits;
            titleY = nodeY;
            backgroundY = drawAbove ? nodeY + ADVANCEMENT_SIZE - boxHeight : nodeY;
            descriptionY = drawAbove ? backgroundY + 7 : nodeY + WIDGET_HEIGHT;
        }

        float percentageObtained = this.advancementProgress == null ? 0.0F : this.advancementProgress.getPercent();
        int obtainedWidth = Mth.floor(percentageObtained * (float) renderWidth);
        AdvancementWidgetType stateTitleLeft;
        AdvancementWidgetType stateTitleRight;
        AdvancementWidgetType stateIcon;

        if (percentageObtained >= 1.0F) {
            obtainedWidth = renderWidth / 2;
            stateTitleLeft = AdvancementWidgetType.OBTAINED;
            stateTitleRight = AdvancementWidgetType.OBTAINED;
            stateIcon = AdvancementWidgetType.OBTAINED;
        } else if (obtainedWidth < 2) {
            obtainedWidth = renderWidth / 2;
            stateTitleLeft = AdvancementWidgetType.UNOBTAINED;
            stateTitleRight = AdvancementWidgetType.UNOBTAINED;
            stateIcon = AdvancementWidgetType.UNOBTAINED;
        } else if (obtainedWidth > renderWidth - 2) {
            obtainedWidth = renderWidth / 2;
            stateTitleLeft = AdvancementWidgetType.OBTAINED;
            stateTitleRight = AdvancementWidgetType.OBTAINED;
            stateIcon = AdvancementWidgetType.UNOBTAINED;
        } else {
            stateTitleLeft = AdvancementWidgetType.OBTAINED;
            stateTitleRight = AdvancementWidgetType.UNOBTAINED;
            stateIcon = AdvancementWidgetType.UNOBTAINED;
        }

        RenderSystem.enableBlend();

        if (!this.description.isEmpty() || pageGrid != null) {
            this.render9Sprite(
                guiGraphics,
                drawX,
                backgroundY,
                renderWidth,
                boxHeight,
                CORNER_SIZE,
                WIDGET_WIDTH,
                WIDGET_HEIGHT,
                0,
                52
            );
        }

        // Title left side
        RenderUtil.setColor(betterDisplayInfo.getTitleColor(stateTitleLeft));
        int leftSide = Math.min(obtainedWidth, WIDGET_WIDTH - 16);
        guiGraphics.blit(
            Resources.Gui.WIDGETS,
            drawX,
            titleY,
            0,
            betterDisplayInfo.getTitleYMultiplier(stateTitleLeft) * WIDGET_HEIGHT,
            leftSide,
            WIDGET_HEIGHT
        );
        if (leftSide < obtainedWidth) {
            guiGraphics.blit(
                Resources.Gui.WIDGETS,
                drawX + leftSide,
                titleY,
                16,
                betterDisplayInfo.getTitleYMultiplier(stateTitleLeft) * WIDGET_HEIGHT,
                obtainedWidth - leftSide,
                WIDGET_HEIGHT
            );
        }

        // Title right side
        int unobtainedWidth = renderWidth - obtainedWidth;
        RenderUtil.setColor(betterDisplayInfo.getTitleColor(stateTitleRight));
        int rightSide = Math.min(unobtainedWidth, WIDGET_WIDTH - 16);
        guiGraphics.blit(
            Resources.Gui.WIDGETS,
            drawX + obtainedWidth,
            titleY,
            WIDGET_WIDTH - rightSide,
            betterDisplayInfo.getTitleYMultiplier(stateTitleRight) * WIDGET_HEIGHT,
            rightSide,
            WIDGET_HEIGHT
        );
        if (rightSide < unobtainedWidth) {
            guiGraphics.blit(
                Resources.Gui.WIDGETS,
                drawX + obtainedWidth + rightSide - 2,
                titleY,
                WIDGET_WIDTH - unobtainedWidth + rightSide - 2,
                betterDisplayInfo.getTitleYMultiplier(stateTitleRight) * WIDGET_HEIGHT,
                unobtainedWidth - rightSide + 2,
                WIDGET_HEIGHT
            );
        }

        int iconX = detached ? drawX + 3 : nodeX + 3;
        int iconY = detached ? titleY : nodeY;
        RenderUtil.setColor(betterDisplayInfo.getIconColor(stateIcon));
        guiGraphics.blitSprite(stateIcon.frameSprite(this.displayInfo.getType()), iconX, iconY, ICON_SIZE, ICON_SIZE);
        RenderUtil.setColor(betterDisplayInfo.defaultIconColor());
        guiGraphics.renderFakeItem(this.displayInfo.getIcon(), iconX + 5, iconY + 5);

        if (detached) {
            int titleRight = progressText == null
                ? drawX + renderWidth - 5
                : drawX + renderWidth - progressTextWidth - 13;
            int titleAvailableWidth = Math.max(0, titleRight - (drawX + 32));
            String visibleTitle = this.fitTitleToWidth(this.title, titleAvailableWidth);
            guiGraphics.drawString(this.minecraft.font, visibleTitle, drawX + 32, titleY + 9, -1);
            if (progressText != null) {
                guiGraphics.drawString(
                    this.minecraft.font,
                    progressText,
                    drawX + renderWidth - progressTextWidth - 5,
                    titleY + 9,
                    -1
                );
            }
        } else if (drawLeft) {
            int titleStart = drawX + 5;
            int progressX = progressText == null
                ? drawX + renderWidth - 5
                : Math.min(nodeX - progressTextWidth, drawX + renderWidth - progressTextWidth - 5);
            int titleRight = progressText == null ? Math.min(nodeX, drawX + renderWidth - 5) : progressX - 5;
            String visibleTitle = this.fitTitleToWidth(this.title, Math.max(0, titleRight - titleStart));
            guiGraphics.drawString(this.minecraft.font, visibleTitle, titleStart, titleY + 9, -1);
            if (progressText != null) {
                guiGraphics.drawString(this.minecraft.font, progressText, progressX, titleY + 9, -1);
            }
        } else {
            int titleStart = Math.max(drawX + 5, nodeX + 32);
            int progressX = drawX + renderWidth - progressTextWidth - 5;
            int titleRight = progressText == null ? drawX + renderWidth - 5 : progressX - 5;
            String visibleTitle = this.fitTitleToWidth(this.title, Math.max(0, titleRight - titleStart));
            guiGraphics.drawString(this.minecraft.font, visibleTitle, titleStart, titleY + 9, -1);
            if (progressText != null) {
                guiGraphics.drawString(this.minecraft.font, progressText, progressX, titleY + 9, -1);
            }
        }

        int yOffset = descriptionY;
        for (int lineIndex = 0; lineIndex < this.description.size(); ++lineIndex) {
            guiGraphics.drawString(
                this.minecraft.font,
                this.description.get(lineIndex),
                drawX + 5,
                yOffset + lineIndex * this.minecraft.font.lineHeight,
                -5592406,
                false
            );
        }

        yOffset += descriptionHeight;
        if (pageGrid != null) {
            int xOffset = drawX + 5;
            for (CriterionGrid.Column column : pageGrid.columns) {
                for (int rowIndex = 0; rowIndex < column.cells().size(); rowIndex++) {
                    guiGraphics.drawString(
                        this.minecraft.font,
                        column.cells().get(rowIndex),
                        xOffset,
                        yOffset + rowIndex * this.minecraft.font.lineHeight,
                        -5592406,
                        false
                    );
                }
                xOffset += column.width();
            }

            if (detached && this.criterionPageCount > 1 && pageIndicatorHeight > 0) {
                Component pageText = Component.translatable("betteradvancements.criteria_page", this.criterionPage + 1, this.criterionPageCount);
                int pageTextWidth = this.minecraft.font.width(pageText);
                guiGraphics.drawString(
                    this.minecraft.font,
                    pageText,
                    drawX + renderWidth - pageTextWidth - 5,
                    yOffset + pageGrid.height,
                    -5592406,
                    false
                );
            }
        }

        if (detached) {
            guiGraphics.disableScissor();
        }
    }

    private String fitTitleToWidth(String value, int availableWidth) {
        if (availableWidth <= 0 || value.isEmpty()) {
            return "";
        }
        if (this.minecraft.font.width(value) <= availableWidth) {
            return value;
        }

        String ellipsis = "…";
        int ellipsisWidth = this.minecraft.font.width(ellipsis);
        if (ellipsisWidth > availableWidth) {
            return "";
        }

        FormattedText head = this.minecraft.font.getSplitter().headByWidth(
            FormattedText.of(value),
            availableWidth - ellipsisWidth,
            Style.EMPTY
        );
        return head.getString() + ellipsis;
    }

    public boolean isMouseOverDetachedPanel(double mouseX, double mouseY) {
        return this.detachedPanelHitBoxValid
            && mouseX >= this.detachedPanelX
            && mouseX < this.detachedPanelX + this.detachedPanelWidth
            && mouseY >= this.detachedPanelY
            && mouseY < this.detachedPanelY + this.detachedPanelHeight;
    }

    public void clearDetachedPanelHitBox() {
        this.detachedPanelHitBoxValid = false;
    }

    public boolean changeCriterionPage(double scrollDelta) {
        if (!this.criterionPanelDetached
            || (CriterionGrid.requiresShift && !Screen.hasShiftDown())
            || this.criterionPageCount <= 1
            || scrollDelta == 0.0D) {
            return false;
        }

        if (scrollDelta < 0.0D) {
            this.criterionPage = Math.min(this.criterionPage + 1, this.criterionPageCount - 1);
        } else {
            this.criterionPage = Math.max(this.criterionPage - 1, 0);
        }

        // Consume the wheel while a paged criterion panel is active, even at the first/last page.
        return true;
    }

    protected void render9Sprite(GuiGraphics guiGraphics, int x, int y, int width, int height, int textureHeight, int textureWidth, int textureDistance, int textureX, int textureY) {
        // Top left corner
        guiGraphics.blit(Resources.Gui.WIDGETS, x, y, textureX, textureY, textureHeight, textureHeight);
        // Top side
        RenderUtil.renderRepeating(Resources.Gui.WIDGETS, guiGraphics, x + textureHeight, y, width - textureHeight - textureHeight, textureHeight, textureX + textureHeight, textureY, textureWidth - textureHeight - textureHeight, textureDistance);
        // Top right corner
        guiGraphics.blit(Resources.Gui.WIDGETS, x + width - textureHeight, y, textureX + textureWidth - textureHeight, textureY, textureHeight, textureHeight);
        // Bottom left corner
        guiGraphics.blit(Resources.Gui.WIDGETS, x, y + height - textureHeight, textureX, textureY + textureDistance - textureHeight, textureHeight, textureHeight);
        // Bottom side
        RenderUtil.renderRepeating(Resources.Gui.WIDGETS, guiGraphics, x + textureHeight, y + height - textureHeight, width - textureHeight - textureHeight, textureHeight, textureX + textureHeight, textureY + textureDistance - textureHeight, textureWidth - textureHeight - textureHeight, textureDistance);
        // Bottom right corner
        guiGraphics.blit(Resources.Gui.WIDGETS, x + width - textureHeight, y + height - textureHeight, textureX + textureWidth - textureHeight, textureY + textureDistance - textureHeight, textureHeight, textureHeight);
        // Left side
        RenderUtil.renderRepeating(Resources.Gui.WIDGETS, guiGraphics, x, y + textureHeight, textureHeight, height - textureHeight - textureHeight, textureX, textureY + textureHeight, textureWidth, textureDistance - textureHeight - textureHeight);
        // Center
        RenderUtil.renderRepeating(Resources.Gui.WIDGETS, guiGraphics, x + textureHeight, y + textureHeight, width - textureHeight - textureHeight, height - textureHeight - textureHeight, textureX + textureHeight, textureY + textureHeight, textureWidth - textureHeight - textureHeight, textureDistance - textureHeight - textureHeight);
        // Right side
        RenderUtil.renderRepeating(Resources.Gui.WIDGETS, guiGraphics, x + width - textureHeight, y + textureHeight, textureHeight, height - textureHeight - textureHeight, textureX + textureWidth - textureHeight, textureY + textureHeight, textureWidth, textureDistance - textureHeight - textureHeight);
    }

    public boolean isMouseOver(double scrollX, double scrollY, double mouseX, double mouseY) {
        if (!this.displayInfo.isHidden() || this.advancementProgress != null && this.advancementProgress.isDone()) {
            double left = scrollX + this.betterAdvancementTabGui.getLayoutX(this);
            double right = left + ADVANCEMENT_SIZE;
            double top = scrollY + this.betterAdvancementTabGui.getLayoutY(this);
            double bottom = top + ADVANCEMENT_SIZE;
            return mouseX >= left && mouseX <= right && mouseY >= top && mouseY <= bottom;
        } else {
            return false;
        }
    }

    public void attachToParent() {
        if (this.parent == null && advancementNode.advancement().parent().isPresent()) {
            this.parent = this.getFirstVisibleParent(advancementNode);

            if (this.parent != null) {
                this.parent.addGuiAdvancement(this);
            }
        }
    }

    @Override
    public int getY() {
        return this.y;
    }

    @Override
    public int getX() {
        return this.x;
    }

    @Override
    public AdvancementNode getAdvancement() {
        return this.advancementNode;
    }
}
