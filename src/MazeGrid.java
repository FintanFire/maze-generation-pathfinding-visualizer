import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * 2D array maze grid with Stack-based Randomized DFS Perfect Maze generation,
 * step recording for animated carving, and optional loop carving.
 * Assigned Team Member: Syed Inshal Ali Kazmi (F25CSC039)
 */
public class MazeGrid {

    public static class GenerationStep {
        public final Cell carvedMid;
        public final Cell activeCell;
        public final boolean isBacktrack;
        public final List<String> stackSnapshot;
        public final int stackSize;
        public final int peakStackSize;

        public GenerationStep(
                Cell carvedMid,
                Cell activeCell,
                boolean isBacktrack,
                List<String> stackSnapshot,
                int stackSize,
                int peakStackSize) {
            this.carvedMid = carvedMid;
            this.activeCell = activeCell;
            this.isBacktrack = isBacktrack;
            this.stackSnapshot = stackSnapshot;
            this.stackSize = stackSize;
            this.peakStackSize = peakStackSize;
        }
    }

    private int rows;
    private int cols;
    private Cell[][] grid;
    private Cell startCell;
    private Cell endCell;
    private boolean perfectMaze;
    private long lastSeed;

    public MazeGrid(int rows, int cols) {
        resize(rows, cols);
    }

    /**
     * Enforces odd dimensions between 11 and 81 for clean cell-carving coordinates.
     */
    public void resize(int requestedRows, int requestedCols) {
        int r = Math.max(11, Math.min(81, requestedRows));
        int c = Math.max(11, Math.min(81, requestedCols));
        if (r % 2 == 0) r++;
        if (c % 2 == 0) c++;
        if (r > 81) r = 81;
        if (c > 81) c = 81;

        this.rows = r;
        this.cols = c;
        this.grid = new Cell[rows][cols];
        this.perfectMaze = true;
        fillAllWalls();
    }

    public void fillAllWalls() {
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                grid[r][c] = new Cell(r, c, Cell.CellState.WALL);
            }
        }
        startCell = grid[1][1];
        endCell = grid[rows - 2][cols - 2];
    }

    /**
     * Generates a Perfect Maze (Spanning Tree) using CustomStack-based Randomized DFS.
     * Returns a list of GenerationSteps so the UI can animate carving or apply instantly.
     */
    public List<GenerationStep> generatePerfectMaze(long seed, boolean prepareForAnimation) {
        this.lastSeed = seed;
        this.perfectMaze = true;
        fillAllWalls();

        Random rng = new Random(seed);
        CustomStack<Cell> stack = new CustomStack<Cell>();
        List<GenerationStep> steps = new ArrayList<GenerationStep>();

        Cell origin = grid[1][1];
        origin.setState(Cell.CellState.OPEN);
        stack.push(origin);
        steps.add(new GenerationStep(
                null,
                origin,
                false,
                toStringList(stack.getSnapshot(8)),
                stack.size(),
                stack.getPeakSize()
        ));

        int[][] dirs = {{-2, 0}, {0, 2}, {2, 0}, {0, -2}};

        while (!stack.isEmpty()) {
            Cell current = stack.peek();
            List<int[]> unvisitedNeighbors = new ArrayList<int[]>();

            for (int[] d : dirs) {
                int nr = current.row + d[0];
                int nc = current.col + d[1];
                if (nr >= 1 && nr <= rows - 2 && nc >= 1 && nc <= cols - 2) {
                    if (grid[nr][nc].getState() == Cell.CellState.WALL) {
                        unvisitedNeighbors.add(new int[]{nr, nc});
                    }
                }
            }

            if (!unvisitedNeighbors.isEmpty()) {
                int[] chosen = unvisitedNeighbors.get(rng.nextInt(unvisitedNeighbors.size()));
                int nr = chosen[0];
                int nc = chosen[1];
                int midR = (current.row + nr) / 2;
                int midC = (current.col + nc) / 2;

                Cell midCell = grid[midR][midC];
                Cell nextCell = grid[nr][nc];
                midCell.setState(Cell.CellState.OPEN);
                nextCell.setState(Cell.CellState.OPEN);
                stack.push(nextCell);

                steps.add(new GenerationStep(
                        midCell,
                        nextCell,
                        false,
                        toStringList(stack.getSnapshot(8)),
                        stack.size(),
                        stack.getPeakSize()
                ));
            } else {
                Cell popped = stack.pop();
                steps.add(new GenerationStep(
                        null,
                        popped,
                        true,
                        toStringList(stack.getSnapshot(8)),
                        stack.size(),
                        stack.getPeakSize()
                ));
            }
        }

        if (prepareForAnimation) {
            // Reset all to WALL so the animation timer can carve them step-by-step
            for (int r = 0; r < rows; r++) {
                for (int c = 0; c < cols; c++) {
                    grid[r][c].setState(Cell.CellState.WALL);
                }
            }
        } else {
            placeDefaultEndpoints();
        }

        return steps;
    }

    public void placeDefaultEndpoints() {
        startCell = grid[1][1];
        endCell = grid[rows - 2][cols - 2];
        startCell.setState(Cell.CellState.START);
        endCell.setState(Cell.CellState.END);
    }

    /**
     * Carves loops into the existing maze by removing a percentage of internal dividing walls.
     * Converts a Perfect Maze (Tree) into a Braided Maze (Graph with cycles) so BFS and DFS
     * produce visibly different path lengths.
     */
    public int carveLoops(double ratio, long seed) {
        clearPathKeepMaze();
        Random rng = new Random(seed ^ 0x9E3779B97F4A7C15L);
        List<Cell> candidateWalls = new ArrayList<Cell>();

        for (int r = 1; r < rows - 1; r++) {
            for (int c = 1; c < cols - 1; c++) {
                if (grid[r][c].getState() != Cell.CellState.WALL) {
                    continue;
                }
                boolean horizontalBridge =
                        grid[r][c - 1].isWalkable() && grid[r][c + 1].isWalkable()
                        && !grid[r - 1][c].isWalkable() && !grid[r + 1][c].isWalkable();
                boolean verticalBridge =
                        grid[r - 1][c].isWalkable() && grid[r + 1][c].isWalkable()
                        && !grid[r][c - 1].isWalkable() && !grid[r][c + 1].isWalkable();

                if (horizontalBridge || verticalBridge) {
                    candidateWalls.add(grid[r][c]);
                }
            }
        }

        Collections.shuffle(candidateWalls, rng);
        int toRemove = Math.max(1, (int) Math.round(candidateWalls.size() * ratio));
        int removed = 0;
        for (int i = 0; i < toRemove && i < candidateWalls.size(); i++) {
            candidateWalls.get(i).setState(Cell.CellState.OPEN);
            removed++;
        }
        if (removed > 0) {
            this.perfectMaze = false;
        }
        return removed;
    }

    /**
     * Clears only traversal states (VISITED, FRONTIER, PATH) while preserving walls and endpoints.
     */
    public void clearPathKeepMaze() {
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                grid[r][c].resetSearchState();
            }
        }
        if (startCell != null) {
            startCell.setState(Cell.CellState.START);
        }
        if (endCell != null) {
            endCell.setState(Cell.CellState.END);
        }
    }

    public void setStartCell(int r, int c) {
        if (!isInsideInterior(r, c)) return;
        Cell target = grid[r][c];
        if (target == endCell) return;
        if (startCell != null) {
            startCell.setState(Cell.CellState.OPEN);
        }
        target.setState(Cell.CellState.START);
        startCell = target;
    }

    public void setEndCell(int r, int c) {
        if (!isInsideInterior(r, c)) return;
        Cell target = grid[r][c];
        if (target == startCell) return;
        if (endCell != null) {
            endCell.setState(Cell.CellState.OPEN);
        }
        target.setState(Cell.CellState.END);
        endCell = target;
    }

    public void setWallState(int r, int c, boolean makeWall) {
        if (!isInsideInterior(r, c)) return;
        Cell target = grid[r][c];
        if (target == startCell || target == endCell) return;
        target.setState(makeWall ? Cell.CellState.WALL : Cell.CellState.OPEN);
        this.perfectMaze = false;
    }

    public boolean isInsideInterior(int r, int c) {
        return r >= 1 && r < rows - 1 && c >= 1 && c < cols - 1;
    }

    public int getTotalOpenCells() {
        int count = 0;
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (grid[r][c].isWalkable()) {
                    count++;
                }
            }
        }
        return count;
    }

    private List<String> toStringList(List<Cell> cells) {
        List<String> list = new ArrayList<String>(cells.size());
        for (Cell c : cells) {
            list.add(c.coords());
        }
        return list;
    }

    public int getRows() {
        return rows;
    }

    public int getCols() {
        return cols;
    }

    public Cell getCell(int r, int c) {
        return grid[r][c];
    }

    public Cell getStartCell() {
        return startCell;
    }

    public Cell getEndCell() {
        return endCell;
    }

    public boolean isPerfectMaze() {
        return perfectMaze;
    }

    public long getLastSeed() {
        return lastSeed;
    }
}
