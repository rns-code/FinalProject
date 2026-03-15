package org.example;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.media.AudioClip;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Random;
import java.util.Set;

/*
 * Title: Wandering In The Woods
 * File Name: WanderingInTheWoods
 * Programmers: Ryan Sheehan, Magdalena Massaro & Dylan Cuatecontzi
 * Emails: ryansheehan@lewisu.edu, magdalenamassaro@lewisu.edu, dylanscuatecontzi@lewisu.edu
 * Course: Software Engineering CPSC 44000
 * Date Completed: March 12th, 2026
 */

public class WanderingInTheWoods extends Application {

    // Random movement generator
    private static final Random rand = new Random();

    // Default grid and player settings
    int rows = 5;
    int cols = 5;
    int players = 2;

    // Stores player row/column positions
    int[][] positions;

    // Stores player groups after meetings
    int[] group;

    // Visual player circles
    Circle[] playerIcons;

    // Main game display area
    Pane gamePane = new Pane();

    // Timer that drives simulation steps
    Timeline timeline;

    // Sound played when all players meet
    AudioClip meetSound;

    // Size of each grid square
    int cellSize = 60;

    // Number of movement steps taken
    int steps = 0;

    // Save file for interrupted games
    File saveFile = new File("save.dat");

    // Main window reference
    Stage primaryStage;

    @Override
    public void start(Stage stage) {
        primaryStage = stage;

        // Attempt to load saved game first
        boolean loaded = loadGame();

        // If no save exists, ask user for a new game setup
        if (!loaded) {
            if (!showGradeSelection()) {
                Platform.exit();
                return;
            }
            initializeNewGame();
        }

        // Draw board and players
        drawGrid();
        initializePlayers();
        loadSound();

        // Main simulation timer
        timeline = new Timeline(
                new KeyFrame(Duration.seconds(1), e -> simulationStep())
        );
        timeline.setCycleCount(Timeline.INDEFINITE);

        // Buttons for control
        Button startBtn = new Button("Start");
        Button stopBtn = new Button("Stop");
        Button resetBtn = new Button("Reset");

        startBtn.setOnAction(e -> timeline.play());
        stopBtn.setOnAction(e -> timeline.pause());
        resetBtn.setOnAction(e -> resetGame());

        // Bottom controls
        HBox controls = new HBox(10, startBtn, stopBtn, resetBtn);
        controls.setAlignment(Pos.CENTER);

        BorderPane root = new BorderPane();
        root.setCenter(gamePane);
        root.setBottom(controls);

        Scene scene = new Scene(root, cols * cellSize, rows * cellSize + 60);

        stage.setTitle("Wandering in the Woods");
        stage.setScene(scene);
        stage.show();

        // Save game if window closes before completion
        stage.setOnCloseRequest(e -> saveGame());
    }

    // Shows grade selection and applies chosen settings
    public boolean showGradeSelection() {
        ChoiceDialog<String> dialog = new ChoiceDialog<>(
                "1 - K-2",
                Arrays.asList("1 - K-2", "2 - Grades 3-5", "3 - Grades 6-8")
        );

        dialog.setTitle("Grade Level");
        dialog.setHeaderText("Choose Grade Level");
        dialog.setContentText("Select one:");

        Optional<String> result = dialog.showAndWait();
        if (result.isEmpty()) {
            return false;
        }

        int level;
        if (result.get().startsWith("1")) {
            level = 1;
        } else if (result.get().startsWith("2")) {
            level = 2;
        } else {
            level = 3;
        }

        return runByLevel(level);
    }

    // Applies settings based on grade level
    public boolean runByLevel(int level) {
        switch (level) {
            case 1:
                rows = 5;
                cols = 5;
                players = 2;
                return true;

            case 2:
                Integer gridSize = askForInteger(
                        "Grid Size",
                        "Grades 3-5 Settings",
                        "Enter grid size:",
                        5
                );
                if (gridSize == null) return false;

                Integer playerCount2 = askForInteger(
                        "Players",
                        "Grades 3-5 Settings",
                        "Enter number of players (2-4):",
                        2
                );
                if (playerCount2 == null) return false;

                rows = gridSize;
                cols = gridSize;
                players = clampPlayers(playerCount2);
                return true;

            case 3:
                Integer rowCount = askForInteger(
                        "Rows",
                        "Grades 6-8 Settings",
                        "Enter number of rows:",
                        5
                );
                if (rowCount == null) return false;

                Integer colCount = askForInteger(
                        "Columns",
                        "Grades 6-8 Settings",
                        "Enter number of columns:",
                        5
                );
                if (colCount == null) return false;

                Integer playerCount3 = askForInteger(
                        "Players",
                        "Grades 6-8 Settings",
                        "Enter number of players (2-4):",
                        2
                );
                if (playerCount3 == null) return false;

                rows = Math.max(2, rowCount);
                cols = Math.max(2, colCount);
                players = clampPlayers(playerCount3);
                return true;

            default:
                rows = 5;
                cols = 5;
                players = 2;
                return true;
        }
    }

    // Reusable number input dialog
    public Integer askForInteger(String title, String header, String prompt, int defaultValue) {
        while (true) {
            TextInputDialog dialog = new TextInputDialog(String.valueOf(defaultValue));
            dialog.setTitle(title);
            dialog.setHeaderText(header);
            dialog.setContentText(prompt);

            Optional<String> result = dialog.showAndWait();
            if (result.isEmpty()) {
                return null;
            }

            try {
                return Integer.parseInt(result.get().trim());
            } catch (NumberFormatException e) {
                // Loop again until valid input is entered
            }
        }
    }

    // Keeps player count between 2 and 4
    public int clampPlayers(int value) {
        if (value < 2) return 2;
        if (value > 4) return 4;
        return value;
    }

    // Creates a new random game state
    public void initializeNewGame() {
        positions = new int[players][2];
        group = new int[players];

        for (int i = 0; i < players; i++) {
            positions[i][0] = rand.nextInt(rows);
            positions[i][1] = rand.nextInt(cols);
            group[i] = i;
        }

        steps = 0;
    }

    // Draws the grid background
    public void drawGrid() {
        gamePane.getChildren().clear();
        gamePane.setPrefSize(cols * cellSize, rows * cellSize);

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                Rectangle cell = new Rectangle(cellSize, cellSize);
                cell.setFill(Color.LIGHTGREEN);
                cell.setStroke(Color.BLACK);
                cell.setX(c * cellSize);
                cell.setY(r * cellSize);
                gamePane.getChildren().add(cell);
            }
        }
    }

    // Places visual circles on the board
    public void initializePlayers() {
        playerIcons = new Circle[players];
        Color[] colors = {Color.RED, Color.BLUE, Color.YELLOW, Color.PURPLE};

        for (int i = 0; i < players; i++) {
            Circle player = new Circle(cellSize / 3.0);
            player.setFill(colors[i % colors.length]);

            double x = positions[i][1] * cellSize + cellSize / 2.0;
            double y = positions[i][0] * cellSize + cellSize / 2.0;

            player.setCenterX(x);
            player.setCenterY(y);

            playerIcons[i] = player;
            gamePane.getChildren().add(player);
        }
    }

    // Loads sound safely
    public void loadSound() {
        try {
            var soundUrl = getClass().getResource("/meet.wav");
            if (soundUrl != null) {
                meetSound = new AudioClip(soundUrl.toExternalForm());
            }
        } catch (Exception e) {
            meetSound = null;
        }
    }

    // Runs one simulation step
    public void simulationStep() {
        if (allInOneGroup()) {
            timeline.stop();
            if (meetSound != null) {
                meetSound.play();
            }
            deleteSave();
            resetGame();
            return;
        }

        Set<Integer> movedGroups = new HashSet<>();

        for (int i = 0; i < players; i++) {
            if (!movedGroups.contains(group[i])) {
                moveGroupRandom(group[i]);
                movedGroups.add(group[i]);
            }
        }

        checkForMeetings();
        updateGraphics();
        steps++;
    }

    // Moves all players in the same group together
    public void moveGroupRandom(int groupID) {
        int move = rand.nextInt(4);

        for (int i = 0; i < players; i++) {
            if (group[i] == groupID) {
                moveByDirection(positions[i], move);
            }
        }
    }

    // Moves one player by one grid square
    public void moveByDirection(int[] pos, int move) {
        switch (move) {
            case 0:
                if (pos[0] > 0) pos[0]--;
                break;
            case 1:
                if (pos[0] < rows - 1) pos[0]++;
                break;
            case 2:
                if (pos[1] > 0) pos[1]--;
                break;
            case 3:
                if (pos[1] < cols - 1) pos[1]++;
                break;
            default:
                break;
        }
    }

    // Detects meetings between players
    public void checkForMeetings() {
        for (int i = 0; i < players; i++) {
            for (int j = i + 1; j < players; j++) {
                if (positions[i][0] == positions[j][0]
                        && positions[i][1] == positions[j][1]) {
                    mergeGroups(group[i], group[j]);
                }
            }
        }
    }

    // Merges two player groups
    public void mergeGroups(int g1, int g2) {
        int newGroup = Math.min(g1, g2);
        int oldGroup = Math.max(g1, g2);

        for (int i = 0; i < group.length; i++) {
            if (group[i] == oldGroup) {
                group[i] = newGroup;
            }
        }
    }

    // Checks if everyone is in the same group
    public boolean allInOneGroup() {
        int first = group[0];

        for (int g : group) {
            if (g != first) {
                return false;
            }
        }
        return true;
    }

    // Animates players to their new grid squares
    public void updateGraphics() {
        for (int i = 0; i < players; i++) {
            double newX = positions[i][1] * cellSize + cellSize / 2.0;
            double newY = positions[i][0] * cellSize + cellSize / 2.0;

            Circle player = playerIcons[i];

            TranslateTransition move = new TranslateTransition(Duration.millis(300), player);
            move.setFromX(player.getTranslateX());
            move.setFromY(player.getTranslateY());
            move.setToX(newX - player.getCenterX());
            move.setToY(newY - player.getCenterY());
            move.play();
        }
    }

    // Saves current game state
    public void saveGame() {
        try {
            ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(saveFile));
            out.writeObject(positions);
            out.writeObject(group);
            out.writeInt(rows);
            out.writeInt(cols);
            out.writeInt(players);
            out.writeInt(steps);
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Loads saved game state
    public boolean loadGame() {
        if (!saveFile.exists()) {
            return false;
        }

        try {
            ObjectInputStream in = new ObjectInputStream(new FileInputStream(saveFile));
            positions = (int[][]) in.readObject();
            group = (int[]) in.readObject();
            rows = in.readInt();
            cols = in.readInt();
            players = in.readInt();
            steps = in.readInt();
            in.close();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // Deletes save file
    public void deleteSave() {
        if (saveFile.exists()) {
            saveFile.delete();
        }
    }

    // Resets the game back to grade selection
    public void resetGame() {
        if (timeline != null) {
            timeline.stop();
        }

        deleteSave();

        Platform.runLater(() -> {
            try {
                new WanderingInTheWoods().start(new Stage());
                primaryStage.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public static void main(String[] args) {
        launch();
    }
<<<<<<< HEAD
}
=======
}
>>>>>>> bc2fc8c35aabbaa255219ddc383cb199eeb90df7
