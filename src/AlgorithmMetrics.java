/**
 * Immutable benchmark metrics captured for a single pathfinding execution.
 * Assigned Team Member: Mujtuba Zia (F25CSC041)
 */
public class AlgorithmMetrics {

    public final String algorithmName;
    public final boolean pathFound;
    public final int cellsVisited;
    public final int totalOpenCells;
    public final int pathLength;
    public final long executionTimeMicros;
    public final int peakFrontierSize;
    public final String timeComplexity;
    public final String dsLabel;
    public final boolean guaranteedOptimal;

    public AlgorithmMetrics(
            String algorithmName,
            boolean pathFound,
            int cellsVisited,
            int totalOpenCells,
            int pathLength,
            long executionTimeMicros,
            int peakFrontierSize,
            String timeComplexity,
            String dsLabel,
            boolean guaranteedOptimal) {
        this.algorithmName = algorithmName;
        this.pathFound = pathFound;
        this.cellsVisited = cellsVisited;
        this.totalOpenCells = totalOpenCells;
        this.pathLength = pathLength;
        this.executionTimeMicros = executionTimeMicros;
        this.peakFrontierSize = peakFrontierSize;
        this.timeComplexity = timeComplexity;
        this.dsLabel = dsLabel;
        this.guaranteedOptimal = guaranteedOptimal;
    }

    public double getExploredPercentage() {
        if (totalOpenCells <= 0) {
            return 0.0;
        }
        return (cellsVisited * 100.0) / totalOpenCells;
    }
}
