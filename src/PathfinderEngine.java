import java.util.ArrayList;
import java.util.List;

/**
 * Implements Queue-based Breadth-First Search (BFS) and Stack-based Depth-First Search (DFS)
 * using custom node-based data structures, recording frame-by-frame steps for visualization
 * and pure CPU execution metrics.
 * Assigned Team Member: Mujtuba Zia (F25CSC041)
 */
public class PathfinderEngine {

    private static final int[] DR = {-1, 0, 1, 0}; // Up, Right, Down, Left
    private static final int[] DC = {0, 1, 0, -1};

    public static class SearchStep {
        public final Cell visitedCell;
        public final Cell parentCell;
        public final int distance;
        public final int visitOrder;
        public final List<Cell> newlyEnqueuedFrontier;
        public final boolean isPathStep;
        public final List<String> dsSnapshot;
        public final int currentFrontierSize;
        public final int peakFrontierSize;
        public final int visitedCountSoFar;

        public SearchStep(
                Cell visitedCell,
                Cell parentCell,
                int distance,
                int visitOrder,
                List<Cell> newlyEnqueuedFrontier,
                boolean isPathStep,
                List<String> dsSnapshot,
                int currentFrontierSize,
                int peakFrontierSize,
                int visitedCountSoFar) {
            this.visitedCell = visitedCell;
            this.parentCell = parentCell;
            this.distance = distance;
            this.visitOrder = visitOrder;
            this.newlyEnqueuedFrontier = newlyEnqueuedFrontier;
            this.isPathStep = isPathStep;
            this.dsSnapshot = dsSnapshot;
            this.currentFrontierSize = currentFrontierSize;
            this.peakFrontierSize = peakFrontierSize;
            this.visitedCountSoFar = visitedCountSoFar;
        }
    }

    public static class SearchResult {
        public final AlgorithmMetrics metrics;
        public final List<SearchStep> steps;
        public final List<Cell> finalPath;
        public final int maxDistance;

        public SearchResult(
                AlgorithmMetrics metrics,
                List<SearchStep> steps,
                List<Cell> finalPath,
                int maxDistance) {
            this.metrics = metrics;
            this.steps = steps;
            this.finalPath = finalPath;
            this.maxDistance = Math.max(1, maxDistance);
        }
    }

    /**
     * Executes Queue-based Breadth-First Search (BFS) on the given MazeGrid.
     * Guarantees the shortest path in unweighted grids.
     */
    public SearchResult runBFS(MazeGrid maze) {
        maze.clearPathKeepMaze();
        int rows = maze.getRows();
        int cols = maze.getCols();
        Cell start = maze.getStartCell();
        Cell end = maze.getEndCell();

        boolean[][] discovered = new boolean[rows][cols];
        CustomQueue<Cell> queue = new CustomQueue<Cell>();
        List<SearchStep> steps = new ArrayList<SearchStep>();

        long t0 = System.nanoTime();

        start.setDistance(0);
        start.setParent(null);
        discovered[start.row][start.col] = true;
        queue.enqueue(start);

        int visitedCount = 0;
        int maxDist = 0;
        boolean found = false;

        while (!queue.isEmpty()) {
            Cell current = queue.dequeue();
            visitedCount++;
            current.setVisitOrder(visitedCount);
            if (current.getDistance() > maxDist) {
                maxDist = current.getDistance();
            }

            if (current == end) {
                found = true;
                steps.add(new SearchStep(
                        current,
                        current.getParent(),
                        current.getDistance(),
                        visitedCount,
                        new ArrayList<Cell>(),
                        false,
                        toCoordsList(queue.getSnapshot(8)),
                        queue.size(),
                        queue.getPeakSize(),
                        visitedCount
                ));
                break;
            }

            List<Cell> frontierAdded = new ArrayList<Cell>();
            for (int i = 0; i < 4; i++) {
                int nr = current.row + DR[i];
                int nc = current.col + DC[i];
                if (nr >= 0 && nr < rows && nc >= 0 && nc < cols && !discovered[nr][nc]) {
                    Cell neighbor = maze.getCell(nr, nc);
                    if (neighbor.isWalkable()) {
                        discovered[nr][nc] = true;
                        neighbor.setParent(current);
                        neighbor.setDistance(current.getDistance() + 1);
                        queue.enqueue(neighbor);
                        frontierAdded.add(neighbor);
                    }
                }
            }

            steps.add(new SearchStep(
                    current,
                    current.getParent(),
                    current.getDistance(),
                    visitedCount,
                    frontierAdded,
                    false,
                    toCoordsList(queue.getSnapshot(8)),
                    queue.size(),
                    queue.getPeakSize(),
                    visitedCount
            ));
        }

        List<Cell> finalPath = new ArrayList<Cell>();
        if (found) {
            finalPath = reconstructPathWithStack(end);
            for (Cell p : finalPath) {
                steps.add(new SearchStep(
                        p,
                        p.getParent(),
                        p.getDistance(),
                        p.getVisitOrder(),
                        new ArrayList<Cell>(),
                        true,
                        new ArrayList<String>(),
                        0,
                        queue.getPeakSize(),
                        visitedCount
                ));
            }
        }

        long elapsedMicros = Math.max(1L, (System.nanoTime() - t0) / 1000L);
        int pathLen = found ? Math.max(0, finalPath.size() - 1) : 0;

        AlgorithmMetrics metrics = new AlgorithmMetrics(
                "BFS (Queue)",
                found,
                visitedCount,
                maze.getTotalOpenCells(),
                pathLen,
                elapsedMicros,
                queue.getPeakSize(),
                "O(V + E)",
                "FIFO Queue",
                true
        );

        // Leave grid clean for step-by-step playback
        maze.clearPathKeepMaze();
        return new SearchResult(metrics, steps, finalPath, maxDist);
    }

    /**
     * Executes Stack-based Depth-First Search (DFS) on the given MazeGrid.
     * Explores deeply before backtracking; does not guarantee shortest path when loops exist.
     */
    public SearchResult runDFS(MazeGrid maze) {
        maze.clearPathKeepMaze();
        int rows = maze.getRows();
        int cols = maze.getCols();
        Cell start = maze.getStartCell();
        Cell end = maze.getEndCell();

        boolean[][] visited = new boolean[rows][cols];
        CustomStack<Cell> stack = new CustomStack<Cell>();
        List<SearchStep> steps = new ArrayList<SearchStep>();

        long t0 = System.nanoTime();

        start.setDistance(0);
        start.setParent(null);
        stack.push(start);

        int visitedCount = 0;
        int maxDist = 0;
        boolean found = false;

        while (!stack.isEmpty()) {
            Cell current = stack.pop();
            if (visited[current.row][current.col]) {
                continue;
            }

            visited[current.row][current.col] = true;
            visitedCount++;
            current.setVisitOrder(visitedCount);
            if (current.getDistance() > maxDist) {
                maxDist = current.getDistance();
            }

            if (current == end) {
                found = true;
                steps.add(new SearchStep(
                        current,
                        current.getParent(),
                        current.getDistance(),
                        visitedCount,
                        new ArrayList<Cell>(),
                        false,
                        toCoordsList(stack.getSnapshot(8)),
                        stack.size(),
                        stack.getPeakSize(),
                        visitedCount
                ));
                break;
            }

            List<Cell> frontierAdded = new ArrayList<Cell>();
            for (int i = 0; i < 4; i++) {
                int nr = current.row + DR[i];
                int nc = current.col + DC[i];
                if (nr >= 0 && nr < rows && nc >= 0 && nc < cols && !visited[nr][nc]) {
                    Cell neighbor = maze.getCell(nr, nc);
                    if (neighbor.isWalkable()) {
                        neighbor.setParent(current);
                        neighbor.setDistance(current.getDistance() + 1);
                        stack.push(neighbor);
                        frontierAdded.add(neighbor);
                    }
                }
            }

            steps.add(new SearchStep(
                    current,
                    current.getParent(),
                    current.getDistance(),
                    visitedCount,
                    frontierAdded,
                    false,
                    toCoordsList(stack.getSnapshot(8)),
                    stack.size(),
                    stack.getPeakSize(),
                    visitedCount
            ));
        }

        List<Cell> finalPath = new ArrayList<Cell>();
        if (found) {
            finalPath = reconstructPathWithStack(end);
            for (Cell p : finalPath) {
                steps.add(new SearchStep(
                        p,
                        p.getParent(),
                        p.getDistance(),
                        p.getVisitOrder(),
                        new ArrayList<Cell>(),
                        true,
                        new ArrayList<String>(),
                        0,
                        stack.getPeakSize(),
                        visitedCount
                ));
            }
        }

        long elapsedMicros = Math.max(1L, (System.nanoTime() - t0) / 1000L);
        int pathLen = found ? Math.max(0, finalPath.size() - 1) : 0;

        AlgorithmMetrics metrics = new AlgorithmMetrics(
                "DFS (Stack)",
                found,
                visitedCount,
                maze.getTotalOpenCells(),
                pathLen,
                elapsedMicros,
                stack.getPeakSize(),
                "O(V + E)",
                "LIFO Stack",
                false
        );

        maze.clearPathKeepMaze();
        return new SearchResult(metrics, steps, finalPath, maxDist);
    }

    /**
     * Reconstructs the path from START to END by pushing parent pointers onto a CustomStack
     * and popping them in forward order.
     */
    private List<Cell> reconstructPathWithStack(Cell endCell) {
        CustomStack<Cell> pathStack = new CustomStack<Cell>();
        Cell curr = endCell;
        while (curr != null) {
            pathStack.push(curr);
            curr = curr.getParent();
        }
        List<Cell> path = new ArrayList<Cell>(pathStack.size());
        while (!pathStack.isEmpty()) {
            path.add(pathStack.pop());
        }
        return path;
    }

    private List<String> toCoordsList(List<Cell> cells) {
        List<String> out = new ArrayList<String>(cells.size());
        for (Cell c : cells) {
            out.add(c.coords());
        }
        return out;
    }
}
