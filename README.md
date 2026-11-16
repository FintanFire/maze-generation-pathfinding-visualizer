# Maze Generation & Pathfinding Visualizer

**Salim Habib University (SHU) — Data Structures & Algorithms (Fall 2026)**

## Team Members
- **Syed Hani Ali Kazmi (`F25CSC038`)** — Java Swing UI, Canvas Renderer & Module Integration
- **Syed Inshal Ali Kazmi (`F25CSC039`)** — 2D Grid, Cell States & Stack-based Randomized DFS Maze Generation
- **Mujtuba Zia (`F25CSC041`)** — Custom Queue/Stack, BFS/DFS Pathfinding & Benchmark Metrics

## Data Structures Implemented From Scratch
- `CustomStack<T>` (`src/CustomStack.java`): Generic singly-linked LIFO stack (`O(1)` push/pop/peek, peak size tracking, live snapshot).
- `CustomQueue<T>` (`src/CustomQueue.java`): Generic linked FIFO queue with `head` & `tail` pointers (`O(1)` enqueue/dequeue, peak size tracking, live snapshot).
- `MazeGrid` (`src/MazeGrid.java`): 2D `Cell[][]` grid with Stack-based Randomized DFS Perfect Maze generator, animated carving support, and optional `+15% Loops` carver.
- `PathfinderEngine` (`src/PathfinderEngine.java`): Queue-based BFS (optimal shortest path) and Stack-based DFS with step-by-step playback and `System.nanoTime()` CPU benchmarking.

## How to Compile & Run
```powershell
mkdir bin -Force
javac -encoding UTF-8 -d bin src/*.java
java -cp bin MazeVisualizerApp
```
Or double-click `run.bat`.

To run the headless verification self-test:
```powershell
java -cp bin MazeVisualizerApp --selftest
```
