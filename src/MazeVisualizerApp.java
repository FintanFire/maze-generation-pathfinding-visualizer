import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Main Swing Application Frame for the Maze Generation & Pathfinding Visualizer.
 * Integrates Left Controls, Center MazeCanvasPanel, Right Comparison & Live DS Inspector,
 * and Timer-driven step-by-step playback.
 * Assigned Team Member: Syed Hani Ali Kazmi (F25CSC038)
 */
public class MazeVisualizerApp extends JFrame {

    // Modern Dark Slate Palette
    private static final Color BG_APP = new Color(0x0F, 0x11, 0x17);
    private static final Color BG_SURFACE = new Color(0x16, 0x19, 0x22);
    private static final Color BG_ELEVATED = new Color(0x1E, 0x22, 0x2D);
    private static final Color BORDER_SUBTLE = new Color(0x26, 0x2B, 0x38);
    private static final Color TEXT_PRIMARY = new Color(0xF3, 0xF4, 0xF6);
    private static final Color TEXT_MUTED = new Color(0x9C, 0xA3, 0xAF);

    private static final Color ACCENT_INDIGO = new Color(0x63, 0x66, 0xF1);
    private static final Color ACCENT_CYAN = new Color(0x0E, 0xA5, 0xE9);
    private static final Color ACCENT_VIOLET = new Color(0x8B, 0x5C, 0xF6);
    private static final Color ACCENT_EMERALD = new Color(0x10, 0xB9, 0x81);
    private static final Color ACCENT_ROSE = new Color(0xF4, 0x3F, 0x5E);
    private static final Color ACCENT_AMBER = new Color(0xF5, 0x9E, 0x0B);

    private static final Font FONT_TITLE = new Font("Segoe UI", Font.BOLD, 14);
    private static final Font FONT_SECTION = new Font("Segoe UI", Font.BOLD, 11);
    private static final Font FONT_BODY = new Font("Segoe UI", Font.PLAIN, 12);
    private static final Font FONT_BODY_BOLD = new Font("Segoe UI", Font.BOLD, 12);
    private static final Font FONT_MONO = new Font("Consolas", Font.PLAIN, 11);
    private static final Font FONT_MONO_BOLD = new Font("Consolas", Font.BOLD, 11);

    private final MazeGrid mazeGrid;
    private final PathfinderEngine engine;
    private final MazeCanvasPanel canvasPanel;

    // Animation & Playback State
    private Timer animTimer;
    private boolean isAnimatingGeneration = false;
    private List<MazeGrid.GenerationStep> genSteps = new ArrayList<MazeGrid.GenerationStep>();
    private int genStepIndex = 0;

    private PathfinderEngine.SearchResult currentSearchResult = null;
    private int searchStepIndex = 0;
    private boolean isPlaying = false;

    private AlgorithmMetrics lastBfsMetrics = null;
    private AlgorithmMetrics lastDfsMetrics = null;

    // UI Controls
    private JComboBox<String> sizeCombo;
    private JTextField seedField;
    private JCheckBox animateGenCheck;
    private JSlider speedSlider;
    private JLabel speedValueLabel;
    private ModernButton playPauseBtn;

    private JLabel statusLabel;
    private RoundedBadge mazeTypeBadge;
    private RoundedBadge complexityBadge;

    // Comparison Table Labels
    private JLabel bfsStatusVal, dfsStatusVal;
    private JLabel bfsVisitedVal, dfsVisitedVal;
    private JLabel bfsPathVal, dfsPathVal;
    private JLabel bfsPeakVal, dfsPeakVal;
    private JLabel bfsTimeVal, dfsTimeVal;
    private JLabel bfsOptVal, dfsOptVal;
    private JLabel verdictBanner;

    // Live DS Inspector Labels
    private JLabel dsTitleLabel;
    private JLabel dsSizeLabel;
    private JLabel dsPeakLabel;
    private JPanel dsNodesContainer;

    public MazeVisualizerApp() {
        super("Maze Generation & Pathfinding Visualizer — SHU DSA Project");
        this.mazeGrid = new MazeGrid(31, 31);
        this.engine = new PathfinderEngine();
        this.canvasPanel = new MazeCanvasPanel(mazeGrid);

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1100, 720));
        setSize(1340, 840);
        setLocationRelativeTo(null);
        getContentPane().setBackground(BG_APP);
        setLayout(new BorderLayout(12, 12));
        ((JPanel) getContentPane()).setBorder(new EmptyBorder(12, 12, 12, 12));

        initUI();

        canvasPanel.setInteractionListener(new MazeCanvasPanel.CanvasInteractionListener() {
            @Override
            public void onMazeEditedByUser() {
                stopAnimationTimer();
                currentSearchResult = null;
                mazeGrid.clearPathKeepMaze();
                canvasPanel.setActivePathChain(null);
                updateMazeTypeBadge();
                setStatus("Maze edited manually — ready to run BFS or DFS", ACCENT_CYAN);
            }
        });

        // Generate initial perfect maze immediately
        long initialSeed = 2026038L;
        seedField.setText(String.valueOf(initialSeed));
        mazeGrid.generatePerfectMaze(initialSeed, false);
        updateMazeTypeBadge();
        setStatus("Ready • Perfect Maze (31×31) generated via Stack DFS", ACCENT_EMERALD);
    }

    private void initUI() {
        add(buildLeftSidebar(), BorderLayout.WEST);
        add(buildCenterStage(), BorderLayout.CENTER);
        add(buildRightAnalyticsDrawer(), BorderLayout.EAST);
    }

    private JComponent buildLeftSidebar() {
        ScrollablePanel content = new ScrollablePanel();
        content.setBackground(BG_APP);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(new EmptyBorder(0, 0, 0, 4));

        // Header Card
        RoundedCardPanel headerCard = new RoundedCardPanel();
        headerCard.setLayout(new BoxLayout(headerCard, BoxLayout.Y_AXIS));
        headerCard.setBorder(new EmptyBorder(12, 14, 12, 14));
        JLabel title = new JLabel("MAZE & PATHFINDING");
        title.setFont(FONT_TITLE);
        title.setForeground(TEXT_PRIMARY);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel subtitle = new JLabel("DSA Visualizer • SHU Fall 2026");
        subtitle.setFont(FONT_BODY);
        subtitle.setForeground(TEXT_MUTED);
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        headerCard.add(title);
        headerCard.add(Box.createVerticalStrut(2));
        headerCard.add(subtitle);
        headerCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 64));
        content.add(headerCard);
        content.add(Box.createVerticalStrut(10));

        // 1. Maze Controls Card
        RoundedCardPanel mazeCard = new RoundedCardPanel();
        mazeCard.setLayout(new BoxLayout(mazeCard, BoxLayout.Y_AXIS));
        mazeCard.setBorder(new EmptyBorder(12, 14, 12, 14));
        mazeCard.add(createSectionHeader("1. MAZE GENERATION"));
        mazeCard.add(Box.createVerticalStrut(8));

        JPanel sizeRow = new JPanel(new BorderLayout(8, 0));
        sizeRow.setOpaque(false);
        sizeRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        sizeRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        JLabel sizeLbl = new JLabel("Grid Size:");
        sizeLbl.setFont(FONT_BODY);
        sizeLbl.setForeground(TEXT_MUTED);
        sizeCombo = new JComboBox<String>(new String[]{"15 × 15", "21 × 21", "31 × 31", "41 × 41", "51 × 51"});
        sizeCombo.setSelectedIndex(2);
        styleComboBox(sizeCombo);
        sizeCombo.addActionListener(e -> handleResizeMaze());
        sizeRow.add(sizeLbl, BorderLayout.WEST);
        sizeRow.add(sizeCombo, BorderLayout.CENTER);
        mazeCard.add(sizeRow);
        mazeCard.add(Box.createVerticalStrut(8));

        JPanel seedRow = new JPanel(new BorderLayout(6, 0));
        seedRow.setOpaque(false);
        seedRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        seedRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        JLabel seedLbl = new JLabel("Seed:");
        seedLbl.setFont(FONT_BODY);
        seedLbl.setForeground(TEXT_MUTED);
        seedField = new JTextField("2026038");
        styleTextField(seedField);
        ModernButton randSeedBtn = new ModernButton("Rand", BG_ELEVATED, TEXT_PRIMARY);
        randSeedBtn.setPreferredSize(new Dimension(54, 26));
        randSeedBtn.addActionListener(e -> {
            long r = Math.abs(new Random().nextInt(9999999));
            seedField.setText(String.valueOf(r));
            generateMazeAction();
        });
        seedRow.add(seedLbl, BorderLayout.WEST);
        seedRow.add(seedField, BorderLayout.CENTER);
        seedRow.add(randSeedBtn, BorderLayout.EAST);
        mazeCard.add(seedRow);
        mazeCard.add(Box.createVerticalStrut(8));

        animateGenCheck = createStyledCheckBox("Animate Carving (Stack)", false);
        mazeCard.add(animateGenCheck);
        mazeCard.add(Box.createVerticalStrut(8));

        ModernButton genBtn = new ModernButton("Generate Perfect Maze", ACCENT_INDIGO, Color.WHITE);
        genBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        genBtn.setPreferredSize(new Dimension(260, 34));
        genBtn.addActionListener(e -> generateMazeAction());
        mazeCard.add(genBtn);
        mazeCard.add(Box.createVerticalStrut(6));

        ModernButton loopsBtn = new ModernButton("+ Carve 15% Loops", BG_ELEVATED, TEXT_PRIMARY);
        loopsBtn.setToolTipText("Removes 15% of internal dividing walls to create cycles so BFS & DFS find different path lengths.");
        loopsBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        loopsBtn.setPreferredSize(new Dimension(260, 30));
        loopsBtn.addActionListener(e -> carveLoopsAction());
        mazeCard.add(loopsBtn);

        content.add(mazeCard);
        content.add(Box.createVerticalStrut(10));

        // 2. Pathfinding Controls Card
        RoundedCardPanel algoCard = new RoundedCardPanel();
        algoCard.setLayout(new BoxLayout(algoCard, BoxLayout.Y_AXIS));
        algoCard.setBorder(new EmptyBorder(12, 14, 12, 14));
        algoCard.add(createSectionHeader("2. PATHFINDING"));
        algoCard.add(Box.createVerticalStrut(8));

        ModernButton bfsBtn = new ModernButton("Run BFS (Queue)", ACCENT_CYAN, Color.WHITE);
        bfsBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        bfsBtn.setPreferredSize(new Dimension(260, 32));
        bfsBtn.addActionListener(e -> startPathfinding(true));
        algoCard.add(bfsBtn);
        algoCard.add(Box.createVerticalStrut(6));

        ModernButton dfsBtn = new ModernButton("Run DFS (Stack)", ACCENT_VIOLET, Color.WHITE);
        dfsBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        dfsBtn.setPreferredSize(new Dimension(260, 32));
        dfsBtn.addActionListener(e -> startPathfinding(false));
        algoCard.add(dfsBtn);
        algoCard.add(Box.createVerticalStrut(6));

        ModernButton compareBtn = new ModernButton("Compare Both (BFS vs DFS)", ACCENT_EMERALD, Color.WHITE);
        compareBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        compareBtn.setPreferredSize(new Dimension(260, 32));
        compareBtn.addActionListener(e -> compareBothAction());
        algoCard.add(compareBtn);

        content.add(algoCard);
        content.add(Box.createVerticalStrut(10));

        // 3. Playback & Speed Card
        RoundedCardPanel playbackCard = new RoundedCardPanel();
        playbackCard.setLayout(new BoxLayout(playbackCard, BoxLayout.Y_AXIS));
        playbackCard.setBorder(new EmptyBorder(12, 14, 12, 14));
        playbackCard.add(createSectionHeader("3. PLAYBACK & SPEED"));
        playbackCard.add(Box.createVerticalStrut(8));

        JPanel transportRow = new JPanel(new GridLayout(1, 4, 5, 0));
        transportRow.setOpaque(false);
        transportRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        transportRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        playPauseBtn = new ModernButton("Pause", BG_ELEVATED, TEXT_PRIMARY);
        playPauseBtn.addActionListener(e -> togglePlayPause());
        ModernButton stepBackBtn = new ModernButton("-1", BG_ELEVATED, TEXT_PRIMARY);
        stepBackBtn.setToolTipText("Step Backward");
        stepBackBtn.addActionListener(e -> stepBackward());
        ModernButton stepFwdBtn = new ModernButton("+1", BG_ELEVATED, TEXT_PRIMARY);
        stepFwdBtn.setToolTipText("Step Forward");
        stepFwdBtn.addActionListener(e -> stepForward());
        ModernButton instantBtn = new ModernButton("Instant", BG_ELEVATED, TEXT_PRIMARY);
        instantBtn.addActionListener(e -> finishInstant());

        transportRow.add(playPauseBtn);
        transportRow.add(stepBackBtn);
        transportRow.add(stepFwdBtn);
        transportRow.add(instantBtn);
        playbackCard.add(transportRow);
        playbackCard.add(Box.createVerticalStrut(8));

        JPanel speedHeader = new JPanel(new BorderLayout());
        speedHeader.setOpaque(false);
        speedHeader.setAlignmentX(Component.LEFT_ALIGNMENT);
        speedHeader.setMaximumSize(new Dimension(Integer.MAX_VALUE, 18));
        JLabel spdTitle = new JLabel("Step Delay:");
        spdTitle.setFont(FONT_BODY);
        spdTitle.setForeground(TEXT_MUTED);
        speedValueLabel = new JLabel("15 ms");
        speedValueLabel.setFont(FONT_MONO_BOLD);
        speedValueLabel.setForeground(ACCENT_CYAN);
        speedHeader.add(spdTitle, BorderLayout.WEST);
        speedHeader.add(speedValueLabel, BorderLayout.EAST);
        playbackCard.add(speedHeader);

        speedSlider = new JSlider(1, 120, 15);
        speedSlider.setOpaque(false);
        speedSlider.setAlignmentX(Component.LEFT_ALIGNMENT);
        speedSlider.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
        speedSlider.addChangeListener(e -> {
            int val = speedSlider.getValue();
            speedValueLabel.setText(val + " ms");
            if (animTimer != null) {
                animTimer.setDelay(val);
            }
        });
        playbackCard.add(speedSlider);
        playbackCard.add(Box.createVerticalStrut(6));

        JPanel resetRow = new JPanel(new GridLayout(1, 2, 6, 0));
        resetRow.setOpaque(false);
        resetRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        resetRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        ModernButton clearPathBtn = new ModernButton("Clear Path", BG_ELEVATED, TEXT_PRIMARY);
        clearPathBtn.addActionListener(e -> clearPathAction());
        ModernButton resetAllBtn = new ModernButton("Reset Grid", new Color(0x3B, 0x1D, 0x28), ACCENT_ROSE);
        resetAllBtn.addActionListener(e -> resetAllAction());
        resetRow.add(clearPathBtn);
        resetRow.add(resetAllBtn);
        playbackCard.add(resetRow);

        content.add(playbackCard);
        content.add(Box.createVerticalStrut(10));

        // 4. Cursor Mode & Overlays Card
        RoundedCardPanel toolsCard = new RoundedCardPanel();
        toolsCard.setLayout(new BoxLayout(toolsCard, BoxLayout.Y_AXIS));
        toolsCard.setBorder(new EmptyBorder(12, 14, 12, 14));
        toolsCard.add(createSectionHeader("4. CURSOR & OVERLAYS"));
        toolsCard.add(Box.createVerticalStrut(6));

        JPanel radioRow = new JPanel(new GridLayout(1, 3, 2, 0));
        radioRow.setOpaque(false);
        radioRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        radioRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
        ButtonGroup bg = new ButtonGroup();
        JRadioButton rbStart = createStyledRadio("Start", false);
        JRadioButton rbEnd = createStyledRadio("End", false);
        JRadioButton rbWall = createStyledRadio("Walls", true);
        bg.add(rbStart);
        bg.add(rbEnd);
        bg.add(rbWall);
        rbStart.addActionListener(e -> canvasPanel.setCursorMode(MazeCanvasPanel.CursorMode.PLACE_START));
        rbEnd.addActionListener(e -> canvasPanel.setCursorMode(MazeCanvasPanel.CursorMode.PLACE_END));
        rbWall.addActionListener(e -> canvasPanel.setCursorMode(MazeCanvasPanel.CursorMode.PAINT_WALL));
        radioRow.add(rbStart);
        radioRow.add(rbEnd);
        radioRow.add(rbWall);
        toolsCard.add(radioRow);
        toolsCard.add(Box.createVerticalStrut(6));

        JCheckBox arrowsCheck = createStyledCheckBox("Show Parent Pointers", false);
        arrowsCheck.addActionListener(e -> canvasPanel.setShowParentArrows(arrowsCheck.isSelected()));
        JCheckBox distCheck = createStyledCheckBox("Show Distances", false);
        distCheck.addActionListener(e -> canvasPanel.setShowDistanceNumbers(distCheck.isSelected()));
        toolsCard.add(arrowsCheck);
        toolsCard.add(distCheck);
        toolsCard.add(Box.createVerticalStrut(8));

        JPanel utilRow = new JPanel(new GridLayout(1, 2, 6, 0));
        utilRow.setOpaque(false);
        utilRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        utilRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        ModernButton exportBtn = new ModernButton("Export PNG", BG_ELEVATED, TEXT_PRIMARY);
        exportBtn.addActionListener(e -> exportPngAction());
        ModernButton infoBtn = new ModernButton("DSA & Team", BG_ELEVATED, TEXT_PRIMARY);
        infoBtn.addActionListener(e -> showInfoDialog());
        utilRow.add(exportBtn);
        utilRow.add(infoBtn);
        toolsCard.add(utilRow);

        content.add(toolsCard);
        content.add(Box.createVerticalGlue());

        JScrollPane scroll = new JScrollPane(content);
        scroll.setBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.setPreferredSize(new Dimension(296, 0));
        return scroll;
    }

    private JComponent buildCenterStage() {
        JPanel center = new JPanel(new BorderLayout(0, 10));
        center.setOpaque(false);

        // Top Minimalist Status Pill
        RoundedCardPanel topBar = new RoundedCardPanel();
        topBar.setLayout(new BorderLayout(10, 0));
        topBar.setBorder(new EmptyBorder(10, 14, 10, 14));

        statusLabel = new JLabel("Ready");
        statusLabel.setFont(FONT_BODY_BOLD);
        statusLabel.setForeground(ACCENT_EMERALD);

        JPanel badges = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        badges.setOpaque(false);
        mazeTypeBadge = new RoundedBadge("Perfect Maze (Tree)", new Color(0x31, 0x2E, 0x81), TEXT_PRIMARY);
        complexityBadge = new RoundedBadge("O(V + E)", new Color(0x0C, 0x4A, 0x6E), new Color(0x7D, 0xD3, 0xFC));
        badges.add(mazeTypeBadge);
        badges.add(complexityBadge);

        topBar.add(statusLabel, BorderLayout.CENTER);
        topBar.add(badges, BorderLayout.EAST);
        center.add(topBar, BorderLayout.NORTH);

        // Canvas
        center.add(canvasPanel, BorderLayout.CENTER);

        // Bottom Legend Bar
        RoundedCardPanel legendBar = new RoundedCardPanel();
        legendBar.setLayout(new FlowLayout(FlowLayout.CENTER, 14, 6));
        legendBar.setBorder(new EmptyBorder(4, 10, 4, 10));
        legendBar.add(createLegendItem(ACCENT_EMERALD, "Start (S)"));
        legendBar.add(createLegendItem(ACCENT_ROSE, "End (E)"));
        legendBar.add(createLegendItem(BG_ELEVATED, "Wall"));
        legendBar.add(createLegendItem(new Color(0x38, 0xBD, 0xF8), "Frontier"));
        legendBar.add(createLegendItem(new Color(0x4F, 0x46, 0xE5), "Visited"));
        legendBar.add(createLegendItem(ACCENT_AMBER, "Final Path"));
        center.add(legendBar, BorderLayout.SOUTH);

        return center;
    }

    private JComponent buildRightAnalyticsDrawer() {
        ScrollablePanel right = new ScrollablePanel();
        right.setBackground(BG_APP);
        right.setLayout(new BoxLayout(right, BoxLayout.Y_AXIS));
        right.setBorder(new EmptyBorder(0, 4, 0, 0));

        // Card 1: BFS vs DFS Comparison Table
        RoundedCardPanel compCard = new RoundedCardPanel();
        compCard.setLayout(new BoxLayout(compCard, BoxLayout.Y_AXIS));
        compCard.setBorder(new EmptyBorder(12, 14, 12, 14));
        compCard.add(createSectionHeader("BFS VS. DFS COMPARISON"));
        compCard.add(Box.createVerticalStrut(8));

        JPanel tableGrid = new JPanel(new GridLayout(7, 3, 6, 6));
        tableGrid.setOpaque(false);
        tableGrid.setAlignmentX(Component.LEFT_ALIGNMENT);
        tableGrid.setMaximumSize(new Dimension(Integer.MAX_VALUE, 175));

        tableGrid.add(createTableHeader("Metric"));
        tableGrid.add(createTableHeader("BFS (Queue)"));
        tableGrid.add(createTableHeader("DFS (Stack)"));

        bfsStatusVal = createTableCell("—");
        dfsStatusVal = createTableCell("—");
        addTableRow(tableGrid, "Status", bfsStatusVal, dfsStatusVal);

        bfsVisitedVal = createTableCell("—");
        dfsVisitedVal = createTableCell("—");
        addTableRow(tableGrid, "Visited", bfsVisitedVal, dfsVisitedVal);

        bfsPathVal = createTableCell("—");
        dfsPathVal = createTableCell("—");
        addTableRow(tableGrid, "Path Len", bfsPathVal, dfsPathVal);

        bfsPeakVal = createTableCell("—");
        dfsPeakVal = createTableCell("—");
        addTableRow(tableGrid, "Peak DS", bfsPeakVal, dfsPeakVal);

        bfsTimeVal = createTableCell("—");
        dfsTimeVal = createTableCell("—");
        addTableRow(tableGrid, "CPU Time", bfsTimeVal, dfsTimeVal);

        bfsOptVal = createTableCell("Yes");
        dfsOptVal = createTableCell("No");
        addTableRow(tableGrid, "Optimal?", bfsOptVal, dfsOptVal);

        compCard.add(tableGrid);
        compCard.add(Box.createVerticalStrut(10));

        verdictBanner = new JLabel("<html><i>Run BFS, DFS, or 'Compare Both' to benchmark traversal on the active maze.</i></html>");
        verdictBanner.setFont(FONT_BODY);
        verdictBanner.setForeground(TEXT_MUTED);
        verdictBanner.setAlignmentX(Component.LEFT_ALIGNMENT);
        verdictBanner.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_SUBTLE, 1),
                new EmptyBorder(8, 10, 8, 10)
        ));
        verdictBanner.setMaximumSize(new Dimension(Integer.MAX_VALUE, 82));
        compCard.add(verdictBanner);

        right.add(compCard);
        right.add(Box.createVerticalStrut(10));

        // Card 2: Live Data Structure Inspector
        RoundedCardPanel dsCard = new RoundedCardPanel();
        dsCard.setLayout(new BoxLayout(dsCard, BoxLayout.Y_AXIS));
        dsCard.setBorder(new EmptyBorder(12, 14, 12, 14));
        dsCard.add(createSectionHeader("LIVE DATA STRUCTURE INSPECTOR"));
        dsCard.add(Box.createVerticalStrut(6));

        JPanel dsStatsRow = new JPanel(new GridLayout(1, 3, 4, 0));
        dsStatsRow.setOpaque(false);
        dsStatsRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        dsStatsRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));
        dsTitleLabel = new JLabel("Idle");
        dsTitleLabel.setFont(FONT_MONO_BOLD);
        dsTitleLabel.setForeground(ACCENT_CYAN);
        dsSizeLabel = new JLabel("Size: 0");
        dsSizeLabel.setFont(FONT_MONO);
        dsSizeLabel.setForeground(TEXT_PRIMARY);
        dsPeakLabel = new JLabel("Peak: 0");
        dsPeakLabel.setFont(FONT_MONO);
        dsPeakLabel.setForeground(TEXT_MUTED);
        dsStatsRow.add(dsTitleLabel);
        dsStatsRow.add(dsSizeLabel);
        dsStatsRow.add(dsPeakLabel);
        dsCard.add(dsStatsRow);
        dsCard.add(Box.createVerticalStrut(8));

        dsNodesContainer = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        dsNodesContainer.setBackground(BG_APP);
        dsNodesContainer.setAlignmentX(Component.LEFT_ALIGNMENT);
        dsNodesContainer.setBorder(BorderFactory.createLineBorder(BORDER_SUBTLE, 1));
        dsNodesContainer.setPreferredSize(new Dimension(280, 110));
        dsNodesContainer.setMaximumSize(new Dimension(Integer.MAX_VALUE, 125));
        updateInspectorChips(new ArrayList<String>(), "Idle", 0, 0);
        dsCard.add(dsNodesContainer);

        right.add(dsCard);
        right.add(Box.createVerticalStrut(10));

        // Card 3: SHU Team Attribution Card
        RoundedCardPanel teamCard = new RoundedCardPanel();
        teamCard.setLayout(new BoxLayout(teamCard, BoxLayout.Y_AXIS));
        teamCard.setBorder(new EmptyBorder(12, 14, 12, 14));
        teamCard.add(createSectionHeader("SHU TEAM • FALL 2026"));
        teamCard.add(Box.createVerticalStrut(8));
        teamCard.add(createTeamLine("Syed Hani Ali Kazmi (F25CSC038)", "Swing UI & Module Integration"));
        teamCard.add(Box.createVerticalStrut(6));
        teamCard.add(createTeamLine("Syed Inshal Ali Kazmi (F25CSC039)", "2D Grid & DFS Maze Generation"));
        teamCard.add(Box.createVerticalStrut(6));
        teamCard.add(createTeamLine("Mujtuba Zia (F25CSC041)", "BFS/DFS Pathfinding & Metrics"));

        right.add(teamCard);
        right.add(Box.createVerticalGlue());

        JScrollPane scroll = new JScrollPane(right);
        scroll.setBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.setPreferredSize(new Dimension(326, 0));
        return scroll;
    }

    // --- Actions & Playback Engine ---

    private long parseSeed() {
        try {
            return Long.parseLong(seedField.getText().trim());
        } catch (NumberFormatException ex) {
            long fallback = 2026038L;
            seedField.setText(String.valueOf(fallback));
            return fallback;
        }
    }

    private void handleResizeMaze() {
        stopAnimationTimer();
        String sel = (String) sizeCombo.getSelectedItem();
        int dim = 31;
        if (sel != null && sel.length() >= 2) {
            dim = Integer.parseInt(sel.substring(0, 2));
        }
        mazeGrid.resize(dim, dim);
        generateMazeAction();
    }

    private void generateMazeAction() {
        stopAnimationTimer();
        currentSearchResult = null;
        lastBfsMetrics = null;
        lastDfsMetrics = null;
        resetComparisonTable();
        canvasPanel.setActivePathChain(null);

        long seed = parseSeed();
        boolean animate = animateGenCheck.isSelected();
        genSteps = mazeGrid.generatePerfectMaze(seed, animate);
        updateMazeTypeBadge();

        if (!animate) {
            canvasPanel.repaint();
            updateInspectorChips(new ArrayList<String>(), "Maze Done", 0, 0);
            setStatus("Generated Perfect Maze (" + mazeGrid.getRows() + "×" + mazeGrid.getCols() + ")", ACCENT_EMERALD);
            return;
        }

        isAnimatingGeneration = true;
        isPlaying = true;
        playPauseBtn.setText("Pause");
        genStepIndex = 0;
        setStatus("Carving Perfect Maze with CustomStack...", ACCENT_AMBER);

        animTimer = new Timer(speedSlider.getValue(), e -> {
            if (genStepIndex >= genSteps.size()) {
                stopAnimationTimer();
                isAnimatingGeneration = false;
                mazeGrid.placeDefaultEndpoints();
                canvasPanel.repaint();
                updateInspectorChips(new ArrayList<String>(), "Maze Done", 0, 0);
                setStatus("Perfect Maze carving complete!", ACCENT_EMERALD);
                return;
            }
            MazeGrid.GenerationStep step = genSteps.get(genStepIndex);
            for (int r = 0; r < mazeGrid.getRows(); r++) {
                for (int c = 0; c < mazeGrid.getCols(); c++) {
                    if (mazeGrid.getCell(r, c).getState() == Cell.CellState.CARVING_HEAD) {
                        mazeGrid.getCell(r, c).setState(Cell.CellState.OPEN);
                    }
                }
            }
            if (step.carvedMid != null) {
                step.carvedMid.setState(Cell.CellState.OPEN);
            }
            if (step.activeCell != null) {
                step.activeCell.setState(step.isBacktrack ? Cell.CellState.OPEN : Cell.CellState.CARVING_HEAD);
            }
            updateInspectorChips(step.stackSnapshot, "Maze Stack", step.stackSize, step.peakStackSize);
            canvasPanel.repaint();
            genStepIndex++;
        });
        animTimer.start();
    }

    private void carveLoopsAction() {
        stopAnimationTimer();
        currentSearchResult = null;
        lastBfsMetrics = null;
        lastDfsMetrics = null;
        resetComparisonTable();
        canvasPanel.setActivePathChain(null);

        int removed = mazeGrid.carveLoops(0.15, parseSeed());
        updateMazeTypeBadge();
        canvasPanel.repaint();
        setStatus("Carved " + removed + " loop bridges — Maze now has multiple paths!", ACCENT_AMBER);
    }

    private void startPathfinding(boolean useBfs) {
        stopAnimationTimer();
        isAnimatingGeneration = false;
        mazeGrid.clearPathKeepMaze();
        canvasPanel.setActivePathChain(null);

        currentSearchResult = useBfs ? engine.runBFS(mazeGrid) : engine.runDFS(mazeGrid);
        canvasPanel.setMaxDistanceForHeatmap(currentSearchResult.maxDistance);
        recordMetrics(currentSearchResult.metrics, useBfs);

        searchStepIndex = 0;
        isPlaying = true;
        playPauseBtn.setText("Pause");
        setStatus("Visualizing " + currentSearchResult.metrics.algorithmName + "...", useBfs ? ACCENT_CYAN : ACCENT_VIOLET);

        animTimer = new Timer(speedSlider.getValue(), e -> {
            if (searchStepIndex >= currentSearchResult.steps.size()) {
                finishCurrentSearchPlayback();
                return;
            }
            applySearchStep(currentSearchResult.steps.get(searchStepIndex));
            searchStepIndex++;
            canvasPanel.repaint();
        });
        animTimer.start();
    }

    private void compareBothAction() {
        stopAnimationTimer();
        isAnimatingGeneration = false;

        PathfinderEngine.SearchResult bfsRes = engine.runBFS(mazeGrid);
        lastBfsMetrics = bfsRes.metrics;
        updateMetricsColumn(lastBfsMetrics, true);

        mazeGrid.clearPathKeepMaze();
        PathfinderEngine.SearchResult dfsRes = engine.runDFS(mazeGrid);
        lastDfsMetrics = dfsRes.metrics;
        updateMetricsColumn(lastDfsMetrics, false);
        updateVerdictBanner();

        currentSearchResult = bfsRes;
        canvasPanel.setMaxDistanceForHeatmap(bfsRes.maxDistance);
        searchStepIndex = bfsRes.steps.size();
        replaySearchUpTo(bfsRes.steps.size() - 1);
        setStatus("Comparison complete! Showing BFS shortest route on canvas.", ACCENT_EMERALD);
    }

    private void applySearchStep(PathfinderEngine.SearchStep step) {
        if (!step.isPathStep) {
            if (step.visitedCell != mazeGrid.getStartCell() && step.visitedCell != mazeGrid.getEndCell()) {
                step.visitedCell.setState(Cell.CellState.VISITED);
            }
            step.visitedCell.setParent(step.parentCell);
            step.visitedCell.setDistance(step.distance);
            step.visitedCell.setVisitOrder(step.visitOrder);

            for (Cell f : step.newlyEnqueuedFrontier) {
                if (f != mazeGrid.getStartCell() && f != mazeGrid.getEndCell() && f.getState() == Cell.CellState.OPEN) {
                    f.setState(Cell.CellState.FRONTIER);
                }
            }
            String dsName = currentSearchResult.metrics.dsLabel;
            updateInspectorChips(step.dsSnapshot, dsName, step.currentFrontierSize, step.peakFrontierSize);
        } else {
            if (step.visitedCell != mazeGrid.getStartCell() && step.visitedCell != mazeGrid.getEndCell()) {
                step.visitedCell.setState(Cell.CellState.PATH);
            }
            List<Cell> partialChain = new ArrayList<Cell>();
            for (int i = 0; i <= searchStepIndex && i < currentSearchResult.steps.size(); i++) {
                PathfinderEngine.SearchStep s = currentSearchResult.steps.get(i);
                if (s.isPathStep) {
                    partialChain.add(s.visitedCell);
                }
            }
            canvasPanel.setActivePathChain(partialChain);
        }
    }

    private void finishCurrentSearchPlayback() {
        stopAnimationTimer();
        if (currentSearchResult == null) return;
        AlgorithmMetrics m = currentSearchResult.metrics;
        if (m.pathFound) {
            canvasPanel.setActivePathChain(currentSearchResult.finalPath);
            setStatus(m.algorithmName + " found path in " + m.pathLength + " steps (" + m.cellsVisited + " visited)", ACCENT_EMERALD);
        } else {
            setStatus(m.algorithmName + ": No Valid Path Found! (Target Unreachable)", ACCENT_ROSE);
        }
        updateInspectorChips(new ArrayList<String>(), m.dsLabel + " (Done)", 0, m.peakFrontierSize);
        canvasPanel.repaint();
    }

    private void togglePlayPause() {
        if (animTimer == null) return;
        if (isPlaying) {
            animTimer.stop();
            isPlaying = false;
            playPauseBtn.setText("Play");
            setStatus("Paused at step " + searchStepIndex, ACCENT_AMBER);
        } else {
            animTimer.start();
            isPlaying = true;
            playPauseBtn.setText("Pause");
        }
    }

    private void stepForward() {
        if (currentSearchResult == null || isAnimatingGeneration) return;
        if (animTimer != null && animTimer.isRunning()) {
            animTimer.stop();
            isPlaying = false;
            playPauseBtn.setText("Play");
        }
        if (searchStepIndex < currentSearchResult.steps.size()) {
            applySearchStep(currentSearchResult.steps.get(searchStepIndex));
            searchStepIndex++;
            canvasPanel.repaint();
            if (searchStepIndex >= currentSearchResult.steps.size()) {
                finishCurrentSearchPlayback();
            }
        }
    }

    private void stepBackward() {
        if (currentSearchResult == null || isAnimatingGeneration) return;
        if (animTimer != null && animTimer.isRunning()) {
            animTimer.stop();
            isPlaying = false;
            playPauseBtn.setText("Play");
        }
        if (searchStepIndex > 0) {
            searchStepIndex--;
            replaySearchUpTo(searchStepIndex - 1);
        }
    }

    private void finishInstant() {
        if (isAnimatingGeneration) {
            stopAnimationTimer();
            isAnimatingGeneration = false;
            for (int i = genStepIndex; i < genSteps.size(); i++) {
                MazeGrid.GenerationStep s = genSteps.get(i);
                if (s.carvedMid != null) s.carvedMid.setState(Cell.CellState.OPEN);
                if (s.activeCell != null) s.activeCell.setState(Cell.CellState.OPEN);
            }
            mazeGrid.placeDefaultEndpoints();
            canvasPanel.repaint();
            setStatus("Perfect Maze carving complete!", ACCENT_EMERALD);
            return;
        }
        if (currentSearchResult == null) return;
        stopAnimationTimer();
        replaySearchUpTo(currentSearchResult.steps.size() - 1);
        searchStepIndex = currentSearchResult.steps.size();
        finishCurrentSearchPlayback();
    }

    private void replaySearchUpTo(int targetIndex) {
        mazeGrid.clearPathKeepMaze();
        canvasPanel.setActivePathChain(null);
        if (currentSearchResult == null || targetIndex < 0) {
            updateInspectorChips(new ArrayList<String>(), "Idle", 0, 0);
            canvasPanel.repaint();
            return;
        }
        int limit = Math.min(targetIndex, currentSearchResult.steps.size() - 1);
        List<Cell> partialChain = new ArrayList<Cell>();
        for (int i = 0; i <= limit; i++) {
            PathfinderEngine.SearchStep step = currentSearchResult.steps.get(i);
            if (!step.isPathStep) {
                if (step.visitedCell != mazeGrid.getStartCell() && step.visitedCell != mazeGrid.getEndCell()) {
                    step.visitedCell.setState(Cell.CellState.VISITED);
                }
                step.visitedCell.setParent(step.parentCell);
                step.visitedCell.setDistance(step.distance);
                step.visitedCell.setVisitOrder(step.visitOrder);
                for (Cell f : step.newlyEnqueuedFrontier) {
                    if (f != mazeGrid.getStartCell() && f != mazeGrid.getEndCell() && f.getState() == Cell.CellState.OPEN) {
                        f.setState(Cell.CellState.FRONTIER);
                    }
                }
            } else {
                if (step.visitedCell != mazeGrid.getStartCell() && step.visitedCell != mazeGrid.getEndCell()) {
                    step.visitedCell.setState(Cell.CellState.PATH);
                }
                partialChain.add(step.visitedCell);
            }
            if (i == limit) {
                updateInspectorChips(step.dsSnapshot, currentSearchResult.metrics.dsLabel, step.currentFrontierSize, step.peakFrontierSize);
            }
        }
        if (!partialChain.isEmpty()) {
            canvasPanel.setActivePathChain(partialChain);
        }
        canvasPanel.repaint();
    }

    private void clearPathAction() {
        stopAnimationTimer();
        currentSearchResult = null;
        mazeGrid.clearPathKeepMaze();
        canvasPanel.setActivePathChain(null);
        canvasPanel.repaint();
        updateInspectorChips(new ArrayList<String>(), "Idle", 0, 0);
        setStatus("Cleared traversal path — maze walls preserved", ACCENT_CYAN);
    }

    private void resetAllAction() {
        stopAnimationTimer();
        currentSearchResult = null;
        lastBfsMetrics = null;
        lastDfsMetrics = null;
        resetComparisonTable();
        mazeGrid.fillAllWalls();
        for (int r = 1; r < mazeGrid.getRows() - 1; r++) {
            for (int c = 1; c < mazeGrid.getCols() - 1; c++) {
                mazeGrid.getCell(r, c).setState(Cell.CellState.OPEN);
            }
        }
        mazeGrid.placeDefaultEndpoints();
        canvasPanel.setActivePathChain(null);
        updateMazeTypeBadge();
        canvasPanel.repaint();
        setStatus("Grid cleared to open sandbox — paint custom walls or generate a maze", ACCENT_CYAN);
    }

    private void stopAnimationTimer() {
        if (animTimer != null && animTimer.isRunning()) {
            animTimer.stop();
        }
        animTimer = null;
        isPlaying = false;
        if (playPauseBtn != null) {
            playPauseBtn.setText("Pause");
        }
    }

    // --- Metrics & UI Updaters ---

    private void recordMetrics(AlgorithmMetrics m, boolean isBfs) {
        if (isBfs) {
            lastBfsMetrics = m;
            updateMetricsColumn(m, true);
        } else {
            lastDfsMetrics = m;
            updateMetricsColumn(m, false);
        }
        updateVerdictBanner();
    }

    private void updateMetricsColumn(AlgorithmMetrics m, boolean isBfs) {
        String statusStr = m.pathFound ? "Found" : "Unreachable";
        String visitedStr = m.cellsVisited + " (" + String.format("%.0f%%", m.getExploredPercentage()) + ")";
        String pathStr = m.pathFound ? (m.pathLength + " steps") : "N/A";
        String peakStr = String.valueOf(m.peakFrontierSize);
        String timeStr = m.executionTimeMicros + " μs";
        String optStr = m.guaranteedOptimal ? "Yes" : "No";

        if (isBfs) {
            bfsStatusVal.setText(statusStr);
            bfsStatusVal.setForeground(m.pathFound ? ACCENT_EMERALD : ACCENT_ROSE);
            bfsVisitedVal.setText(visitedStr);
            bfsPathVal.setText(pathStr);
            bfsPeakVal.setText(peakStr);
            bfsTimeVal.setText(timeStr);
            bfsOptVal.setText(optStr);
        } else {
            dfsStatusVal.setText(statusStr);
            dfsStatusVal.setForeground(m.pathFound ? ACCENT_EMERALD : ACCENT_ROSE);
            dfsVisitedVal.setText(visitedStr);
            dfsPathVal.setText(pathStr);
            dfsPeakVal.setText(peakStr);
            dfsTimeVal.setText(timeStr);
            dfsOptVal.setText(optStr);
        }
    }

    private void resetComparisonTable() {
        JLabel[] cols = {bfsStatusVal, dfsStatusVal, bfsVisitedVal, dfsVisitedVal,
                bfsPathVal, dfsPathVal, bfsPeakVal, dfsPeakVal, bfsTimeVal, dfsTimeVal};
        for (JLabel lbl : cols) {
            if (lbl != null) {
                lbl.setText("—");
                lbl.setForeground(TEXT_PRIMARY);
            }
        }
        if (verdictBanner != null) {
            verdictBanner.setText("<html><i>Run BFS, DFS, or 'Compare Both' to benchmark traversal on the active maze.</i></html>");
        }
    }

    private void updateVerdictBanner() {
        if (lastBfsMetrics == null || lastDfsMetrics == null) return;
        if (!lastBfsMetrics.pathFound || !lastDfsMetrics.pathFound) {
            verdictBanner.setText("<html><font color='#F43F5E'><b>Unreachable Target:</b></font> Both algorithms exhausted all reachable cells without finding End (E).</html>");
            return;
        }
        int pathDiff = lastDfsMetrics.pathLength - lastBfsMetrics.pathLength;
        int visitDiff = lastBfsMetrics.cellsVisited - lastDfsMetrics.cellsVisited;
        if (pathDiff == 0 && mazeGrid.isPerfectMaze()) {
            verdictBanner.setText("<html><font color='#10B981'><b>Perfect Maze (Tree):</b></font> Only 1 unique path exists (" + lastBfsMetrics.pathLength + " steps). BFS & DFS differed by " + Math.abs(visitDiff) + " visited cells.</html>");
        } else if (pathDiff > 0) {
            verdictBanner.setText("<html><font color='#0EA5E9'><b>BFS Optimal:</b></font> BFS found a path <b>" + pathDiff + " steps shorter</b> than DFS! Try carving loops to see this gap grow.</html>");
        } else {
            verdictBanner.setText("<html><font color='#8B5CF6'><b>Tie on Path Length:</b></font> Both found " + lastBfsMetrics.pathLength + " steps. DFS visited " + lastDfsMetrics.cellsVisited + " vs BFS " + lastBfsMetrics.cellsVisited + ".</html>");
        }
    }

    private void updateInspectorChips(List<String> items, String dsName, int size, int peak) {
        dsTitleLabel.setText(dsName);
        dsSizeLabel.setText("Size: " + size);
        dsPeakLabel.setText("Peak: " + peak);

        dsNodesContainer.removeAll();
        if (items == null || items.isEmpty()) {
            JLabel emptyLbl = new JLabel("Structure empty");
            emptyLbl.setFont(FONT_MONO);
            emptyLbl.setForeground(TEXT_MUTED);
            dsNodesContainer.add(emptyLbl);
        } else {
            for (int i = 0; i < items.size(); i++) {
                JLabel chip = new JLabel(items.get(i));
                chip.setFont(FONT_MONO_BOLD);
                chip.setForeground(i == 0 ? Color.WHITE : TEXT_PRIMARY);
                chip.setOpaque(true);
                chip.setBackground(i == 0 ? ACCENT_INDIGO : BG_ELEVATED);
                chip.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(BORDER_SUBTLE, 1),
                        new EmptyBorder(3, 6, 3, 6)
                ));
                dsNodesContainer.add(chip);
            }
        }
        dsNodesContainer.revalidate();
        dsNodesContainer.repaint();
    }

    private void updateMazeTypeBadge() {
        if (mazeGrid.isPerfectMaze()) {
            mazeTypeBadge.setBadge("Perfect Maze (Tree)", new Color(0x31, 0x2E, 0x81), TEXT_PRIMARY);
        } else {
            mazeTypeBadge.setBadge("Braided Graph (Loops)", new Color(0x78, 0x35, 0x0F), new Color(0xFD, 0xE6, 0x8A));
        }
    }

    private void setStatus(String text, Color color) {
        statusLabel.setText(text);
        statusLabel.setForeground(color);
    }

    private void exportPngAction() {
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File("maze_snapshot.png"));
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            BufferedImage img = new BufferedImage(canvasPanel.getWidth(), canvasPanel.getHeight(), BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = img.createGraphics();
            canvasPanel.paint(g2);
            g2.dispose();
            try {
                ImageIO.write(img, "png", file);
                setStatus("Saved PNG snapshot: " + file.getName(), ACCENT_EMERALD);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Failed to export PNG: " + ex.getMessage(), "Export Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void showInfoDialog() {
        String msg = "<html><body style='width:340px; font-family:Segoe UI;'>"
                + "<h3>Maze Generation & Pathfinding Visualizer</h3>"
                + "<p><b>Salim Habib University — Data Structures & Algorithms (Fall 2026)</b></p>"
                + "<ul>"
                + "<li><b>2D Array Grid:</b> <code>Cell[][]</code> state matrix</li>"
                + "<li><b>CustomStack&lt;T&gt;:</b> Node-based LIFO Stack for Randomized DFS Maze Generation & DFS Pathfinding</li>"
                + "<li><b>CustomQueue&lt;T&gt;:</b> Node-based FIFO Queue with head/tail O(1) pointers for BFS Shortest-Path</li>"
                + "</ul>"
                + "<p><b>Team Members:</b><br>"
                + "• Syed Hani Ali Kazmi (F25CSC038)<br>"
                + "• Syed Inshal Ali Kazmi (F25CSC039)<br>"
                + "• Mujtuba Zia (F25CSC041)</p>"
                + "</body></html>";
        JOptionPane.showMessageDialog(this, msg, "DSA & Team Info", JOptionPane.INFORMATION_MESSAGE);
    }

    // --- Helper UI Builders ---

    private JLabel createSectionHeader(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(FONT_SECTION);
        lbl.setForeground(TEXT_MUTED);
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        return lbl;
    }

    private JPanel createLegendItem(Color swatch, String label) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        p.setOpaque(false);
        JPanel box = new JPanel();
        box.setPreferredSize(new Dimension(12, 12));
        box.setBackground(swatch);
        box.setBorder(BorderFactory.createLineBorder(BORDER_SUBTLE, 1));
        JLabel lbl = new JLabel(label);
        lbl.setFont(FONT_BODY);
        lbl.setForeground(TEXT_MUTED);
        p.add(box);
        p.add(lbl);
        return p;
    }

    private JLabel createTableHeader(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(FONT_SECTION);
        lbl.setForeground(TEXT_MUTED);
        return lbl;
    }

    private JLabel createTableCell(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(FONT_MONO_BOLD);
        lbl.setForeground(TEXT_PRIMARY);
        return lbl;
    }

    private void addTableRow(JPanel grid, String metric, JLabel bfsLbl, JLabel dfsLbl) {
        JLabel m = new JLabel(metric);
        m.setFont(FONT_BODY);
        m.setForeground(TEXT_MUTED);
        grid.add(m);
        grid.add(bfsLbl);
        grid.add(dfsLbl);
    }

    private JPanel createTeamLine(String name, String role) {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setOpaque(false);
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel n = new JLabel(name);
        n.setFont(new Font("Segoe UI", Font.BOLD, 11));
        n.setForeground(TEXT_PRIMARY);
        n.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel r = new JLabel(role);
        r.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        r.setForeground(TEXT_MUTED);
        r.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(n);
        p.add(r);
        return p;
    }

    private JCheckBox createStyledCheckBox(String text, boolean selected) {
        JCheckBox cb = new JCheckBox(text, selected);
        cb.setOpaque(false);
        cb.setForeground(TEXT_PRIMARY);
        cb.setFont(FONT_BODY);
        cb.setFocusPainted(false);
        cb.setAlignmentX(Component.LEFT_ALIGNMENT);
        return cb;
    }

    private JRadioButton createStyledRadio(String text, boolean selected) {
        JRadioButton rb = new JRadioButton(text, selected);
        rb.setOpaque(false);
        rb.setForeground(TEXT_PRIMARY);
        rb.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        rb.setFocusPainted(false);
        return rb;
    }

    private void styleComboBox(JComboBox<String> combo) {
        combo.setBackground(BG_ELEVATED);
        combo.setForeground(TEXT_PRIMARY);
        combo.setFont(FONT_BODY);
        combo.setFocusable(false);
    }

    private void styleTextField(JTextField tf) {
        tf.setBackground(BG_ELEVATED);
        tf.setForeground(TEXT_PRIMARY);
        tf.setCaretColor(TEXT_PRIMARY);
        tf.setFont(FONT_MONO);
        tf.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_SUBTLE, 1),
                new EmptyBorder(3, 6, 3, 6)
        ));
    }

    // --- Custom Minimalist Swing Components ---

    /**
     * Scrollable JPanel that tracks the viewport width so BoxLayout cards never overflow or clip horizontally.
     */
    private static class ScrollablePanel extends JPanel implements Scrollable {
        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
            return 16;
        }

        @Override
        public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
            return 48;
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return false;
        }
    }

    private static class RoundedCardPanel extends JPanel {
        RoundedCardPanel() {
            setOpaque(false);
            setAlignmentX(Component.LEFT_ALIGNMENT);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(BG_SURFACE);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
            g2.setColor(BORDER_SUBTLE);
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    private static class RoundedBadge extends JLabel {
        private Color bg;

        RoundedBadge(String text, Color bg, Color fg) {
            super(text);
            this.bg = bg;
            setForeground(fg);
            setFont(FONT_MONO_BOLD);
            setOpaque(false);
            setBorder(new EmptyBorder(4, 10, 4, 10));
        }

        void setBadge(String text, Color bg, Color fg) {
            setText(text);
            this.bg = bg;
            setForeground(fg);
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(bg);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    private static class ModernButton extends javax.swing.JButton {
        private final Color baseBg;
        private final Color hoverBg;
        private boolean hovered = false;

        ModernButton(String text, Color bg, Color fg) {
            super(text);
            this.baseBg = bg;
            this.hoverBg = bg.brighter();
            setForeground(fg);
            setFont(FONT_BODY_BOLD);
            setFocusPainted(false);
            setBorderPainted(false);
            setContentAreaFilled(false);
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setHorizontalAlignment(SwingConstants.CENTER);
            setAlignmentX(Component.LEFT_ALIGNMENT);

            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    hovered = true;
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    hovered = false;
                    repaint();
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(hovered ? hoverBg : baseBg);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
            g2.setColor(getForeground());
            g2.setFont(getFont());
            FontMetrics fm = g2.getFontMetrics();
            int tx = (getWidth() - fm.stringWidth(getText())) / 2;
            int ty = ((getHeight() - fm.getHeight()) / 2) + fm.getAscent();
            g2.drawString(getText(), tx, ty);
            g2.dispose();
        }
    }

    // --- Entry Point & Headless Self-Test ---

    public static void main(String[] args) {
        if (args.length > 0 && "--selftest".equals(args[0])) {
            runSelfTest();
            return;
        }
        SwingUtilities.invokeLater(() -> {
            MazeVisualizerApp app = new MazeVisualizerApp();
            app.setVisible(true);
        });
    }

    private static void runSelfTest() {
        System.out.println("=== Maze Generation & Pathfinding Visualizer Self-Test ===");
        MazeGrid grid = new MazeGrid(31, 31);
        grid.generatePerfectMaze(2026038L, false);

        PathfinderEngine eng = new PathfinderEngine();
        PathfinderEngine.SearchResult bfsTree = eng.runBFS(grid);
        PathfinderEngine.SearchResult dfsTree = eng.runDFS(grid);

        System.out.println("[Perfect Maze 31x31] BFS: found=" + bfsTree.metrics.pathFound
                + ", visited=" + bfsTree.metrics.cellsVisited
                + ", pathLen=" + bfsTree.metrics.pathLength
                + ", peakQueue=" + bfsTree.metrics.peakFrontierSize);
        System.out.println("[Perfect Maze 31x31] DFS: found=" + dfsTree.metrics.pathFound
                + ", visited=" + dfsTree.metrics.cellsVisited
                + ", pathLen=" + dfsTree.metrics.pathLength
                + ", peakStack=" + dfsTree.metrics.peakFrontierSize);

        if (!bfsTree.metrics.pathFound || !dfsTree.metrics.pathFound) {
            System.err.println("FAIL: Perfect maze should be connected!");
            System.exit(1);
        }
        if (bfsTree.metrics.pathLength != dfsTree.metrics.pathLength) {
            System.err.println("FAIL: Perfect maze (Spanning Tree) must have unique path length!");
            System.exit(1);
        }

        // Test Braided Maze with 15% loops
        grid.carveLoops(0.15, 2026038L);
        PathfinderEngine.SearchResult bfsLoop = eng.runBFS(grid);
        PathfinderEngine.SearchResult dfsLoop = eng.runDFS(grid);
        System.out.println("[Braided Maze 31x31] BFS: pathLen=" + bfsLoop.metrics.pathLength
                + ", visited=" + bfsLoop.metrics.cellsVisited);
        System.out.println("[Braided Maze 31x31] DFS: pathLen=" + dfsLoop.metrics.pathLength
                + ", visited=" + dfsLoop.metrics.cellsVisited);

        if (bfsLoop.metrics.pathLength > dfsLoop.metrics.pathLength) {
            System.err.println("FAIL: BFS must never return a longer path than DFS!");
            System.exit(1);
        }

        // Test Unreachable Target
        Cell end = grid.getEndCell();
        grid.setWallState(end.row - 1, end.col, true);
        grid.setWallState(end.row, end.col - 1, true);
        PathfinderEngine.SearchResult unreachBfs = eng.runBFS(grid);
        PathfinderEngine.SearchResult unreachDfs = eng.runDFS(grid);
        if (unreachBfs.metrics.pathFound || unreachDfs.metrics.pathFound) {
            System.err.println("FAIL: Blocked target should be unreachable!");
            System.exit(1);
        }
        System.out.println("[Unreachable Target] Handled gracefully by both BFS & DFS.");
        System.out.println("ALL SELF-TESTS PASSED.");
        System.exit(0);
    }
}
