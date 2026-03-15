package org.example;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
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

    private static final Random rand = new Random();

    int rows = 5;
    int cols = 5;
    int players = 2;

    int[][] positions;
    int[] group;
    int[] groupDirections;

    // Snake scan state for systematic mode
    int[] scanDirections; // 1 = right, -1 = left
    boolean[] scanMovingDown; // true = down, false = up

    Circle[] playerIcons;

    Pane gamePane = new Pane();
    Timeline timeline;
    AudioClip meetSound;

    int cellSize = 60;
    int steps = 0;

    File saveFile = new File("save.dat");
    Stage primaryStage;

    boolean useRandomMovement = true;
    int systematicStep = 0;

    int totalSimulations = 1;
    int currentSimulation = 1;

    int[] simulationResults;
    int totalStepsAcrossRuns = 0;

    @Override
    public void start(Stage stage) {
        primaryStage = stage;

        boolean loaded = loadGame();

        if (!loaded) {
            if (!showGradeSelection()) {
                Platform.exit();
                return;
            }
            initializeNewGame();
        }

        drawGrid();
        initializePlayers();
        loadSound();

        timeline = new Timeline(
                new KeyFrame(Duration.seconds(1), e -> simulationStep())
        );
        timeline.setCycleCount(Timeline.INDEFINITE);

        Button startBtn = new Button("Start");
        Button stopBtn = new Button("Stop");
        Button resetBtn = new Button("Reset");

        startBtn.setOnAction(e -> timeline.play());
        stopBtn.setOnAction(e -> timeline.pause());
        resetBtn.setOnAction(e -> resetGame());

        HBox controls = new HBox(10, startBtn, stopBtn, resetBtn);
        controls.setAlignment(Pos.CENTER);

        BorderPane root = new BorderPane();
        root.setCenter(gamePane);
        root.setBottom(controls);

        Scene scene = new Scene(root, cols * cellSize, rows * cellSize + 60);

        stage.setTitle("Wandering in the Woods");
        stage.setScene(scene);
        stage.show();

        stage.setOnCloseRequest(e -> saveGame());
    }

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

    public boolean showMovementSelection() {
        ChoiceDialog<String> dialog = new ChoiceDialog<>(
                "Random",
                Arrays.asList("Random", "Systematic")
        );

        dialog.setTitle("Movement Type");
        dialog.setHeaderText("Choose Movement Style");
        dialog.setContentText("Select movement:");

        Optional<String> result = dialog.showAndWait();
        if (result.isEmpty()) {
            return false;
        }

        useRandomMovement = result.get().equalsIgnoreCase("Random");
        systematicStep = 0;
        return true;
    }

    public boolean askSimulationCount() {
        Integer simCount = askForInteger(
                "Simulations",
                "Grades 6-8 Settings",
                "How many simulations should run?",
                10
        );

        if (simCount == null) {
            return false;
        }

        totalSimulations = Math.max(1, simCount);
        currentSimulation = 1;
        simulationResults = new int[totalSimulations];
        totalStepsAcrossRuns = 0;
        return true;
    }

    public boolean runByLevel(int level) {
        switch (level) {
            case 1:
                rows = 5;
                cols = 5;
                players = 2;
                useRandomMovement = true;
                totalSimulations = 1;
                currentSimulation = 1;
                simulationResults = new int[1];
                totalStepsAcrossRuns = 0;
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
                useRandomMovement = true;
                totalSimulations = 1;
                currentSimulation = 1;
                simulationResults = new int[1];
                totalStepsAcrossRuns = 0;
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

                if (!showMovementSelection()) {
                    return false;
                }

                if (!askSimulationCount()) {
                    return false;
                }

                return true;

            default:
                rows = 5;
                cols = 5;
                players = 2;
                useRandomMovement = true;
                totalSimulations = 1;
                currentSimulation = 1;
                simulationResults = new int[1];
                totalStepsAcrossRuns = 0;
                return true;
        }
    }

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
            }
        }
    }

    public int clampPlayers(int value) {
        if (value < 2) return 2;
        if (value > 4) return 4;
        return value;
    }

    public void initializeNewGame() {
        positions = new int[players][2];
        group = new int[players];
        groupDirections = new int[players];
        scanDirections = new int[players];
        scanMovingDown = new boolean[players];

        for (int i = 0; i < players; i++) {
            positions[i][0] = rand.nextInt(rows);
            positions[i][1] = rand.nextInt(cols);
            group[i] = i;

            scanDirections[i] = (i % 2 == 0) ? 1 : -1;
            scanMovingDown[i] = true;
        }

        steps = 0;
        systematicStep = 0;

        if (totalSimulations < 1) {
            totalSimulations = 1;
        }

        if (simulationResults == null || simulationResults.length != totalSimulations) {
            simulationResults = new int[totalSimulations];
        }
    }

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

    public void simulationStep() {
        if (allInOneGroup()) {
            timeline.stop();

            if (meetSound != null) {
                meetSound.play();
            }

            if (simulationResults != null && currentSimulation - 1 < simulationResults.length) {
                simulationResults[currentSimulation - 1] = steps;
                totalStepsAcrossRuns += steps;
            }

            Platform.runLater(() -> {
                showCompletionMessage();

                if (currentSimulation < totalSimulations) {
                    currentSimulation++;
                    initializeNewGame();
                    drawGrid();
                    initializePlayers();
                    timeline.play();
                } else {
                    showFinalStatistics();
                    deleteSave();
                    resetGame();
                }
            });

            return;
        }

        Set<Integer> movedGroups = new HashSet<>();

        for (int i = 0; i < players; i++) {
            if (!movedGroups.contains(group[i])) {
                moveGroup(group[i]);
                movedGroups.add(group[i]);
            }
        }

        checkForMeetings();
        updateGraphics();
        steps++;
    }

    public void moveGroup(int groupID) {

        if (useRandomMovement) {

            int move = rand.nextInt(4);

            for (int i = 0; i < players; i++) {
                if (group[i] == groupID) {
                    moveByDirection(positions[i], move);
                }
            }

        } else {

            moveGroupSystematicScan(groupID);

        }
    }

    public void moveGroupSystematicScan(int groupID) {

        for (int i = 0; i < players; i++) {

            if (group[i] != groupID) continue;

            int r = positions[i][0];
            int c = positions[i][1];

            // EVEN rows move RIGHT
            if (r % 2 == 0) {

                if (c < cols - 1) {
                    positions[i][1]++; // move right
                }
                else if (r < rows - 1) {
                    positions[i][0]++; // move down
                }

            }

            // ODD rows move LEFT
            else {

                if (c > 0) {
                    positions[i][1]--; // move left
                }
                else if (r < rows - 1) {
                    positions[i][0]++; // move down
                }

            }
        }
    }

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

    public void mergeGroups(int g1, int g2) {
        int newGroup = Math.min(g1, g2);
        int oldGroup = Math.max(g1, g2);

        for (int i = 0; i < group.length; i++) {
            if (group[i] == oldGroup) {
                group[i] = newGroup;
            }
        }

        scanDirections[oldGroup] = scanDirections[newGroup];
        scanMovingDown[oldGroup] = scanMovingDown[newGroup];
    }

    public boolean allInOneGroup() {
        int first = group[0];

        for (int g : group) {
            if (g != first) {
                return false;
            }
        }
        return true;
    }

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

    public void showCompletionMessage() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Simulation Complete");
        alert.setHeaderText("Simulation " + currentSimulation + " finished");
        alert.setContentText("It took " + steps + " steps for all circles to meet.");
        alert.showAndWait();
    }

    public void showFinalStatistics() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Final Simulation Statistics");
        alert.setHeaderText("All simulations are complete.");

        StringBuilder stats = new StringBuilder();
        int minSteps = simulationResults[0];
        int maxSteps = simulationResults[0];

        for (int i = 0; i < simulationResults.length; i++) {
            stats.append("Simulation ")
                    .append(i + 1)
                    .append(": ")
                    .append(simulationResults[i])
                    .append(" steps\n");

            if (simulationResults[i] < minSteps) {
                minSteps = simulationResults[i];
            }

            if (simulationResults[i] > maxSteps) {
                maxSteps = simulationResults[i];
            }
        }

        double averageSteps = (double) totalStepsAcrossRuns / totalSimulations;

        stats.append("\nTotal simulations: ").append(totalSimulations);
        stats.append("\nAverage steps: ").append(String.format("%.2f", averageSteps));
        stats.append("\nMinimum steps: ").append(minSteps);
        stats.append("\nMaximum steps: ").append(maxSteps);

        alert.setContentText(stats.toString());
        alert.showAndWait();
    }

    public void saveGame() {
        try {
            ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(saveFile));
            out.writeObject(positions);
            out.writeObject(group);
            out.writeObject(groupDirections);
            out.writeObject(scanDirections);
            out.writeObject(scanMovingDown);
            out.writeInt(rows);
            out.writeInt(cols);
            out.writeInt(players);
            out.writeInt(steps);
            out.writeBoolean(useRandomMovement);
            out.writeInt(systematicStep);
            out.writeInt(totalSimulations);
            out.writeInt(currentSimulation);
            out.writeObject(simulationResults);
            out.writeInt(totalStepsAcrossRuns);
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean loadGame() {
        if (!saveFile.exists()) {
            return false;
        }

        try {
            ObjectInputStream in = new ObjectInputStream(new FileInputStream(saveFile));
            positions = (int[][]) in.readObject();
            group = (int[]) in.readObject();
            groupDirections = (int[]) in.readObject();
            scanDirections = (int[]) in.readObject();
            scanMovingDown = (boolean[]) in.readObject();
            rows = in.readInt();
            cols = in.readInt();
            players = in.readInt();
            steps = in.readInt();
            useRandomMovement = in.readBoolean();
            systematicStep = in.readInt();
            totalSimulations = in.readInt();
            currentSimulation = in.readInt();
            simulationResults = (int[]) in.readObject();
            totalStepsAcrossRuns = in.readInt();
            in.close();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public void deleteSave() {
        if (saveFile.exists()) {
            saveFile.delete();
        }
    }

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
}