import javax.swing.JPanel;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.GeneralPath;
import java.util.ArrayList;
import java.util.List;

/**
 * Custom anti-aliased Graphics2D maze viewport with distance heatmap gradients,
 * continuous path polyline, parent-pointer arrows, and mouse interaction.
 * Assigned Team Member: Syed Hani Ali Kazmi (F25CSC038)
 */
public class MazeCanvasPanel extends JPanel {

    public enum CursorMode {
        PLACE_START,
        PLACE_END,
        PAINT_WALL
    }

    public interface CanvasInteractionListener {
        void onMazeEditedByUser();
    }

    private static final Color BG_CANVAS = new Color(0x0F, 0x11, 0x17);
    private static final Color WALL_FILL = new Color(0x1E, 0x22, 0x2D);
    private static final Color WALL_EDGE = new Color(0x26, 0x2B, 0x38);
    private static final Color GRID_HAIRLINE = new Color(0x16, 0x19, 0x22);
    private static final Color START_COLOR = new Color(0x10, 0xB9, 0x81);
    private static final Color END_COLOR = new Color(0xF4, 0x3F, 0x5E);
    private static final Color FRONTIER_COLOR = new Color(0x38, 0xBD, 0xF8);
    private static final Color CARVING_HEAD_COLOR = new Color(0xF5, 0x9E, 0x0B);
    private static final Color PATH_COLOR = new Color(0xF5, 0x9E, 0x0B);
    private static final Color PATH_CELL_TINT = new Color(0xF5, 0x9E, 0x0B, 55);

    private static final Color VISITED_NEAR = new Color(0x31, 0x2E, 0x81);
    private static final Color VISITED_MID = new Color(0x4F, 0x46, 0xE5);
    private static final Color VISITED_FAR = new Color(0x81, 0x8C, 0xF8);

    private MazeGrid mazeGrid;
    private CursorMode cursorMode = CursorMode.PAINT_WALL;
    private boolean showParentArrows = false;
    private boolean showDistanceNumbers = false;
    private int maxDistanceForHeatmap = 1;
    private List<Cell> activePathChain = new ArrayList<Cell>();
    private CanvasInteractionListener interactionListener;

    // Layout metrics cached during paint for accurate mouse hit-testing
    private int cachedCellSize = 20;
    private int cachedOffsetX = 0;
    private int cachedOffsetY = 0;
    private boolean dragPaintMakeWall = true;

    public MazeCanvasPanel(MazeGrid mazeGrid) {
        this.mazeGrid = mazeGrid;
        setBackground(BG_CANVAS);
        setOpaque(true);

        MouseAdapter mouseHandler = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                handleMousePress(e);
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                handleMouseDrag(e);
            }
        };
        addMouseListener(mouseHandler);
        addMouseMotionListener(mouseHandler);
    }

    public void setInteractionListener(CanvasInteractionListener listener) {
        this.interactionListener = listener;
    }

    public void setCursorMode(CursorMode mode) {
        this.cursorMode = mode;
    }

    public void setShowParentArrows(boolean show) {
        this.showParentArrows = show;
        repaint();
    }

    public void setShowDistanceNumbers(boolean show) {
        this.showDistanceNumbers = show;
        repaint();
    }

    public void setMaxDistanceForHeatmap(int maxDistance) {
        this.maxDistanceForHeatmap = Math.max(1, maxDistance);
    }

    public void setActivePathChain(List<Cell> pathChain) {
        this.activePathChain = (pathChain != null) ? pathChain : new ArrayList<Cell>();
        repaint();
    }

    private int[] screenToGrid(int mx, int my) {
        if (cachedCellSize <= 0) return null;
        int c = (mx - cachedOffsetX) / cachedCellSize;
        int r = (my - cachedOffsetY) / cachedCellSize;
        if (r >= 0 && r < mazeGrid.getRows() && c >= 0 && c < mazeGrid.getCols()) {
            return new int[]{r, c};
        }
        return null;
    }

    private void handleMousePress(MouseEvent e) {
        int[] rc = screenToGrid(e.getX(), e.getY());
        if (rc == null) return;
        int r = rc[0];
        int c = rc[1];
        if (!mazeGrid.isInsideInterior(r, c)) return;

        Cell target = mazeGrid.getCell(r, c);
        if (cursorMode == CursorMode.PLACE_START) {
            if (target != mazeGrid.getEndCell()) {
                if (interactionListener != null) interactionListener.onMazeEditedByUser();
                mazeGrid.setStartCell(r, c);
                repaint();
            }
        } else if (cursorMode == CursorMode.PLACE_END) {
            if (target != mazeGrid.getStartCell()) {
                if (interactionListener != null) interactionListener.onMazeEditedByUser();
                mazeGrid.setEndCell(r, c);
                repaint();
            }
        } else if (cursorMode == CursorMode.PAINT_WALL) {
            if (target == mazeGrid.getStartCell() || target == mazeGrid.getEndCell()) return;
            dragPaintMakeWall = (target.getState() != Cell.CellState.WALL);
            if (interactionListener != null) interactionListener.onMazeEditedByUser();
            mazeGrid.setWallState(r, c, dragPaintMakeWall);
            repaint();
        }
    }

    private void handleMouseDrag(MouseEvent e) {
        if (cursorMode != CursorMode.PAINT_WALL) return;
        int[] rc = screenToGrid(e.getX(), e.getY());
        if (rc == null) return;
        int r = rc[0];
        int c = rc[1];
        if (!mazeGrid.isInsideInterior(r, c)) return;
        Cell target = mazeGrid.getCell(r, c);
        if (target == mazeGrid.getStartCell() || target == mazeGrid.getEndCell()) return;

        boolean currentlyWall = (target.getState() == Cell.CellState.WALL);
        if (currentlyWall != dragPaintMakeWall) {
            mazeGrid.setWallState(r, c, dragPaintMakeWall);
            repaint();
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            int rows = mazeGrid.getRows();
            int cols = mazeGrid.getCols();
            int availW = Math.max(100, getWidth() - 28);
            int availH = Math.max(100, getHeight() - 28);
            int cellSize = Math.max(4, Math.min(availW / cols, availH / rows));

            int gridW = cellSize * cols;
            int gridH = cellSize * rows;
            int offsetX = (getWidth() - gridW) / 2;
            int offsetY = (getHeight() - gridH) / 2;

            this.cachedCellSize = cellSize;
            this.cachedOffsetX = offsetX;
            this.cachedOffsetY = offsetY;

            // Subtle outer frame around the maze viewport
            g2.setColor(new Color(0x16, 0x19, 0x22));
            g2.fillRoundRect(offsetX - 6, offsetY - 6, gridW + 12, gridH + 12, 12, 12);
            g2.setColor(WALL_EDGE);
            g2.drawRoundRect(offsetX - 6, offsetY - 6, gridW + 12, gridH + 12, 12, 12);

            g2.setColor(BG_CANVAS);
            g2.fillRect(offsetX, offsetY, gridW, gridH);

            int arc = Math.max(2, Math.min(8, cellSize / 3));
            int pad = (cellSize >= 14) ? 1 : 0;

            for (int r = 0; r < rows; r++) {
                for (int c = 0; c < cols; c++) {
                    Cell cell = mazeGrid.getCell(r, c);
                    int x = offsetX + c * cellSize;
                    int y = offsetY + r * cellSize;

                    switch (cell.getState()) {
                        case WALL:
                            g2.setColor(WALL_FILL);
                            g2.fillRect(x, y, cellSize, cellSize);
                            if (cellSize >= 12) {
                                g2.setColor(WALL_EDGE);
                                g2.drawRect(x, y, cellSize - 1, cellSize - 1);
                            }
                            break;

                        case OPEN:
                            if (cellSize >= 14) {
                                g2.setColor(GRID_HAIRLINE);
                                g2.drawRect(x, y, cellSize, cellSize);
                            }
                            break;

                        case VISITED:
                            g2.setColor(interpolateVisitedColor(cell.getDistance()));
                            g2.fillRoundRect(x + pad, y + pad, cellSize - 2 * pad, cellSize - 2 * pad, arc, arc);
                            break;

                        case FRONTIER:
                            g2.setColor(FRONTIER_COLOR);
                            g2.fillRoundRect(x + pad, y + pad, cellSize - 2 * pad, cellSize - 2 * pad, arc, arc);
                            break;

                        case CARVING_HEAD:
                            g2.setColor(CARVING_HEAD_COLOR);
                            g2.fillRoundRect(x + pad, y + pad, cellSize - 2 * pad, cellSize - 2 * pad, arc, arc);
                            break;

                        case PATH:
                            g2.setColor(interpolateVisitedColor(cell.getDistance()));
                            g2.fillRoundRect(x + pad, y + pad, cellSize - 2 * pad, cellSize - 2 * pad, arc, arc);
                            g2.setColor(PATH_CELL_TINT);
                            g2.fillRoundRect(x + pad, y + pad, cellSize - 2 * pad, cellSize - 2 * pad, arc, arc);
                            break;

                        case START:
                        case END:
                            // Drawn cleanly after path polyline so badges sit on top
                            break;
                    }

                    // Optional Parent Arrows
                    if (showParentArrows && cellSize >= 14 && cell.getParent() != null
                            && (cell.getState() == Cell.CellState.VISITED || cell.getState() == Cell.CellState.PATH)) {
                        drawParentArrow(g2, cell, x, y, cellSize);
                    }

                    // Optional Distance Numbers
                    if (showDistanceNumbers && cellSize >= 18 && cell.getDistance() >= 0
                            && (cell.getState() == Cell.CellState.VISITED || cell.getState() == Cell.CellState.PATH)) {
                        drawDistanceLabel(g2, cell.getDistance(), x, y, cellSize);
                    }
                }
            }

            // Draw smooth continuous golden polyline for reconstructed path
            if (activePathChain != null && activePathChain.size() >= 2) {
                drawPathPolyline(g2, activePathChain, offsetX, offsetY, cellSize);
            }

            // Draw START and END badges on top
            drawEndpointBadge(g2, mazeGrid.getStartCell(), offsetX, offsetY, cellSize, START_COLOR, "S");
            drawEndpointBadge(g2, mazeGrid.getEndCell(), offsetX, offsetY, cellSize, END_COLOR, "E");

        } finally {
            g2.dispose();
        }
    }

    private Color interpolateVisitedColor(int distance) {
        if (distance < 0) return VISITED_NEAR;
        float t = Math.min(1.0f, distance / (float) maxDistanceForHeatmap);
        if (t < 0.5f) {
            return lerpColor(VISITED_NEAR, VISITED_MID, t * 2.0f);
        } else {
            return lerpColor(VISITED_MID, VISITED_FAR, (t - 0.5f) * 2.0f);
        }
    }

    private Color lerpColor(Color a, Color b, float t) {
        int r = (int) (a.getRed() + (b.getRed() - a.getRed()) * t);
        int g = (int) (a.getGreen() + (b.getGreen() - a.getGreen()) * t);
        int bl = (int) (a.getBlue() + (b.getBlue() - a.getBlue()) * t);
        return new Color(
                Math.max(0, Math.min(255, r)),
                Math.max(0, Math.min(255, g)),
                Math.max(0, Math.min(255, bl))
        );
    }

    private void drawPathPolyline(Graphics2D g2, List<Cell> chain, int offsetX, int offsetY, int cellSize) {
        GeneralPath gp = new GeneralPath();
        for (int i = 0; i < chain.size(); i++) {
            Cell cell = chain.get(i);
            float cx = offsetX + cell.col * cellSize + cellSize / 2.0f;
            float cy = offsetY + cell.row * cellSize + cellSize / 2.0f;
            if (i == 0) {
                gp.moveTo(cx, cy);
            } else {
                gp.lineTo(cx, cy);
            }
        }
        float strokeWidth = Math.max(2.5f, cellSize * 0.34f);
        g2.setColor(new Color(0xF5, 0x9E, 0x0B, 90));
        g2.setStroke(new BasicStroke(strokeWidth + 3.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.draw(gp);

        g2.setColor(PATH_COLOR);
        g2.setStroke(new BasicStroke(strokeWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.draw(gp);
    }

    private void drawEndpointBadge(Graphics2D g2, Cell cell, int offsetX, int offsetY, int cellSize, Color color, String label) {
        if (cell == null) return;
        int x = offsetX + cell.col * cellSize;
        int y = offsetY + cell.row * cellSize;
        int pad = Math.max(1, cellSize / 10);
        int arc = Math.max(4, cellSize / 3);

        g2.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 70));
        g2.fillRoundRect(x, y, cellSize, cellSize, arc + 2, arc + 2);

        g2.setColor(color);
        g2.fillRoundRect(x + pad, y + pad, cellSize - 2 * pad, cellSize - 2 * pad, arc, arc);

        if (cellSize >= 12) {
            g2.setColor(Color.WHITE);
            int fontSize = Math.max(9, (int) (cellSize * 0.52));
            g2.setFont(new Font("Segoe UI", Font.BOLD, fontSize));
            FontMetrics fm = g2.getFontMetrics();
            int tx = x + (cellSize - fm.stringWidth(label)) / 2;
            int ty = y + ((cellSize - fm.getHeight()) / 2) + fm.getAscent();
            g2.drawString(label, tx, ty);
        }
    }

    private void drawParentArrow(Graphics2D g2, Cell cell, int x, int y, int cellSize) {
        Cell parent = cell.getParent();
        if (parent == null) return;
        int dr = parent.row - cell.row;
        int dc = parent.col - cell.col;

        int cx = x + cellSize / 2;
        int cy = y + cellSize / 2;
        int len = Math.max(3, cellSize / 4);

        g2.setColor(new Color(255, 255, 255, 130));
        g2.setStroke(new BasicStroke(1.3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        int ex = cx + dc * len;
        int ey = cy + dr * len;
        g2.drawLine(cx - dc * (len / 2), cy - dr * (len / 2), ex, ey);
    }

    private void drawDistanceLabel(Graphics2D g2, int dist, int x, int y, int cellSize) {
        String s = String.valueOf(dist);
        int fontSize = Math.max(8, Math.min(11, cellSize / 3));
        g2.setFont(new Font("Consolas", Font.PLAIN, fontSize));
        g2.setColor(new Color(255, 255, 255, 175));
        FontMetrics fm = g2.getFontMetrics();
        int tx = x + (cellSize - fm.stringWidth(s)) / 2;
        int ty = y + ((cellSize - fm.getHeight()) / 2) + fm.getAscent();
        g2.drawString(s, tx, ty);
    }
}
