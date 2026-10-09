package betteradvancements.common.util;

import net.minecraft.ChatFormatting;
import net.minecraft.advancements.*;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

// An arrangement of criteria into rows and columns
public class CriterionGrid {
    public static CriteriaDetail detailLevel = CriteriaDetail.DEFAULT;
    public static boolean requiresShift = false;
    public static boolean sortAlphabetically = true;
    private static final CriterionGrid empty = new CriterionGrid();

    private final List<Component> cellContents;
    private final int[] cellWidths;
    private final int fontHeight;
    private final int numColumns;
    public final int numRows;
    public List<Column> columns;
    public int width;
    public int height;

    private CriterionGrid() {
        this.cellContents = Collections.emptyList();
        this.cellWidths = new int[0];
        this.fontHeight = 0;
        this.numColumns = 0;
        this.numRows = 0;
        this.columns = Collections.emptyList();
        this.width = 0;
        this.height = 0;
    }

    public CriterionGrid(List<Component> cellContents, int[] cellWidths, int fontHeight, int numColumns) {
        this.cellContents = cellContents;
        this.cellWidths = cellWidths;
        this.fontHeight = fontHeight;
        this.numColumns = numColumns;
        this.numRows = (int)Math.ceil((double)cellContents.size() / numColumns);
    }

    public void init() {
        this.columns = new ArrayList<>();
        this.width = 0;
        for (int c = 0; c < this.numColumns; c++) {
            List<Component> column = new ArrayList<>();
            int columnWidth = 0;
            for (int r = 0; r < this.numRows; r++) {
                int cellIndex = c * this.numRows + r;
                if (cellIndex >= this.cellContents.size()) {
                    break;
                }
                column.add(this.cellContents.get(cellIndex));
                columnWidth = Math.max(columnWidth, this.cellWidths[cellIndex]);
            }
            this.columns.add(new Column(column, columnWidth));
            this.width += columnWidth;
        }
        this.height = this.numRows * this.fontHeight;
    }

    private record CriterionEntry(boolean obtained, String sortKey, Component component) {}

    public record Column(List<Component> cells, int width) {}

    // Of all the possible grids whose aspect ratio is less than the maximum, this method returns the one with the smallest number of rows.
    // If there is no such grid, this method returns a single-column grid.
    public static CriterionGrid findOptimalCriterionGrid(AdvancementHolder holder, Advancement advancement, AdvancementProgress progress, int maxWidth, Font font) {
        if (progress == null || progress.isDone() || detailLevel.equals(CriteriaDetail.OFF)) {
            return CriterionGrid.empty;
        }
        AdvancementRequirements requirements = advancement.requirements();
        if (requirements.size() <= 1) {
            return CriterionGrid.empty;
        }
        int numUnobtained = 0;
        List<CriterionEntry> criterionEntries = new ArrayList<>();

        for (String criterion : requirements.names()) {
            CriterionProgress criterionProgress = progress.getCriterion(criterion);
            boolean obtained = criterionProgress != null && criterionProgress.isDone();
            String criterionKey = "betteradvancements.criterion." + holder.id() + "." + criterion;
            MutableComponent label = Component.translatableWithFallback(criterionKey, criterion).withStyle(ChatFormatting.WHITE);

            if (obtained) {
                if (detailLevel.showObtained()) {
                    MutableComponent text = Component.literal(" + ").withStyle(ChatFormatting.GREEN);
                    text.append(label);
                    criterionEntries.add(new CriterionEntry(true, label.getString(), text));
                }
            } else {
                if (detailLevel.showUnobtained()) {
                    MutableComponent text = Component.literal(" x ").withStyle(ChatFormatting.DARK_RED);
                    text.append(label);
                    criterionEntries.add(new CriterionEntry(false, label.getString(), text));
                }
                numUnobtained++;
            }
        }

        if (sortAlphabetically) {
            Comparator<CriterionEntry> alphabetical = Comparator
                .comparing((CriterionEntry entry) -> entry.sortKey().toLowerCase(Locale.ROOT))
                .thenComparing(CriterionEntry::sortKey);

            if (detailLevel.showObtained() && detailLevel.showUnobtained()) {
                criterionEntries.sort(
                    Comparator.comparing((CriterionEntry entry) -> !entry.obtained())
                        .thenComparing(alphabetical)
                );
            } else {
                criterionEntries.sort(alphabetical);
            }
        }

        List<Component> cellContents = new ArrayList<>(criterionEntries.size() + 1);
        for (CriterionEntry entry : criterionEntries) {
            cellContents.add(entry.component());
        }

        if (!detailLevel.showUnobtained()) {
            MutableComponent text = Component.literal(" x ").withStyle(ChatFormatting.DARK_RED);
            MutableComponent text2 = Component.translatable("betteradvancements.remaining", numUnobtained).withStyle(ChatFormatting.WHITE, ChatFormatting.ITALIC);
            text.append(text2);
            cellContents.add(text);
        }

        int[] cellWidths = new int[cellContents.size()];
        for (int i = 0; i < cellWidths.length; i++) {
            cellWidths[i] = font.width(cellContents.get(i));
        }

        int numCols = 0;
        CriterionGrid prevGrid = null;
        CriterionGrid currGrid = null;
        do {
            numCols++;
            CriterionGrid newGrid = new CriterionGrid(cellContents, cellWidths, font.lineHeight, numCols);
            if (prevGrid != null && newGrid.numRows == prevGrid.numRows) {
                // We increased the width without decreasing the height, which is pointless.
                continue;
            }
            newGrid.init();
            prevGrid = currGrid;
            currGrid = newGrid;
        } while(numCols <= cellContents.size() && currGrid.width <= maxWidth);
        return prevGrid != null ? prevGrid : currGrid;
    }
    public boolean isEmpty() {
        return this.cellContents.isEmpty();
    }

    /**
     * Splits this grid into contiguous pages that fit both the requested row count and width.
     *
     * <p>The entries stay in their already-sorted order.  A page may use fewer columns than
     * the full grid.  This is important because simply slicing the flattened entry list and
     * reusing {@link #numColumns} can make a page wider than the original grid when several
     * wide entries happen to land on the same page.</p>
     */
    public List<CriterionGrid> getPages(int maxRows, int maxWidth) {
        if (this.cellContents.isEmpty()) {
            return List.of(CriterionGrid.empty);
        }

        int rowsPerPage = Math.max(1, maxRows);
        int safeMaxWidth = Math.max(1, maxWidth);
        int maxColumns = Math.max(1, this.cellContents.size());
        List<CriterionGrid> pages = new ArrayList<>();

        int from = 0;
        while (from < this.cellContents.size()) {
            CriterionGrid bestPage = null;
            int bestCount = 0;
            int remaining = this.cellContents.size() - from;

            for (int columns = 1; columns <= maxColumns && columns <= remaining; columns++) {
                int count = Math.min(remaining, rowsPerPage * columns);
                CriterionGrid candidate = this.createSlice(from, count, columns);

                if (candidate.numRows <= rowsPerPage && candidate.width <= safeMaxWidth) {
                    if (count > bestCount || count == bestCount && (bestPage == null || candidate.width < bestPage.width)) {
                        bestPage = candidate;
                        bestCount = count;
                    }
                }
            }

            if (bestPage == null) {
                // A single translated criterion can itself be wider than the available panel.
                // Keep forward progress and let the caller's scissor clip that unavoidable case.
                bestPage = this.createSlice(from, 1, 1);
                bestCount = 1;
            }

            pages.add(bestPage);
            from += bestCount;
        }

        return pages;
    }

    private CriterionGrid createSlice(int from, int count, int columns) {
        int to = Math.min(this.cellContents.size(), from + count);
        List<Component> pageContents = new ArrayList<>(this.cellContents.subList(from, to));
        int[] pageWidths = new int[pageContents.size()];
        System.arraycopy(this.cellWidths, from, pageWidths, 0, pageContents.size());

        CriterionGrid page = new CriterionGrid(
            pageContents,
            pageWidths,
            this.fontHeight,
            Math.min(Math.max(1, columns), pageContents.size())
        );
        page.init();
        return page;
    }

}
