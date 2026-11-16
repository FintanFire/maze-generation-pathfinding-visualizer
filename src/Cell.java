/**
 * Represents a single cell (row, col) in the 2D maze grid.
 * Assigned Team Member: Syed Inshal Ali Kazmi (F25CSC039)
 */
public class Cell {

    public enum CellState {
        WALL,
        OPEN,
        START,
        END,
        FRONTIER,
        VISITED,
        PATH,
        CARVING_HEAD
    }

    public final int row;
    public final int col;

    private CellState state;
    private Cell parent;
    private int distance;
    private int visitOrder;

    public Cell(int row, int col, CellState initialState) {
        this.row = row;
        this.col = col;
        this.state = initialState;
        this.parent = null;
        this.distance = -1;
        this.visitOrder = 0;
    }

    public CellState getState() {
        return state;
    }

    public void setState(CellState state) {
        this.state = state;
    }

    public Cell getParent() {
        return parent;
    }

    public void setParent(Cell parent) {
        this.parent = parent;
    }

    public int getDistance() {
        return distance;
    }

    public void setDistance(int distance) {
        this.distance = distance;
    }

    public int getVisitOrder() {
        return visitOrder;
    }

    public void setVisitOrder(int visitOrder) {
        this.visitOrder = visitOrder;
    }

    /**
     * Resets traversal/search states while preserving structural WALL, START, and END cells.
     */
    public void resetSearchState() {
        if (state == CellState.FRONTIER
                || state == CellState.VISITED
                || state == CellState.PATH
                || state == CellState.CARVING_HEAD) {
            state = CellState.OPEN;
        }
        parent = null;
        distance = -1;
        visitOrder = 0;
    }

    public boolean isWalkable() {
        return state != CellState.WALL;
    }

    public String coords() {
        return "(" + row + "," + col + ")";
    }

    @Override
    public String toString() {
        return coords();
    }
}
