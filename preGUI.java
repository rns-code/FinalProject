package org.example;

// Import everything inside the java.util package
import java.util.*;

/*
 * Title: Wandering In The Woods
 * File Name: wanderingInTheWoods2
 * Programmers: Ryan Sheehan, Magdalena Massaro & Dylan Cuatecontzi
 * Emails: ryansheehan@lewisu.edu, magdalenamassaro@lewisu.edu, dylancuatecontzi@lewisu.edu
 * Course: Software Engineering CPSC 44000
 * Date Completed:
 */

public class WanderingInTheWoods {

    // Random object for movement
    static Random rand = new Random();

    // Scanner for user input
    static Scanner scan = new Scanner(System.in);

    public static void main(String[] args) {

        // Title of simulation
        System.out.println("Wandering in the Woods Educational Simulation");

        // Grade level selection
        System.out.println("Select Grade Level:");
        System.out.println("1 - K-2");
        System.out.println("2 - Grades 3-5");
        System.out.println("3 - Grades 6-8");

        int level = scan.nextInt();

        // Run simulation based on grade level
        runByLevel(level);
    }

    // Determines settings based on selected grade level
    public static void runByLevel(int level) {

        int rows = 5;
        int cols = 5;
        int players = 2;
        int simulations = 1;
        boolean systematic = false;

        switch (level) {

            // K-2 level uses simple diagonal start and random movement
            case 1:
                rows = 5;
                cols = 5;
                runK2(rows, cols);
                return;

            // Grades 3-5 allow user to choose grid and players
            case 2:
                System.out.print("Enter grid size (square): ");
                rows = cols = scan.nextInt();

                System.out.print("Enter number of players (2-4): ");
                players = scan.nextInt();

                simulations = 5;
                break;

            // Grades 6-8 allow full customization and strategy selection
            case 3:
                System.out.print("Enter number of rows: ");
                rows = scan.nextInt();

                System.out.print("Enter number of columns: ");
                cols = scan.nextInt();

                System.out.print("Enter number of players (2-4): ");
                players = scan.nextInt();

                System.out.print("Enter number of simulations: ");
                simulations = scan.nextInt();

                System.out.print("Movement type (1 = Random, 2 = Systematic): ");
                int choice = scan.nextInt();

                systematic = (choice == 2);
                break;

            default:
                System.out.println("Invalid level.");
                return;
        }

        // Run analysis for Grades 3-8
        analyzeSimulations(rows, cols, players, simulations, systematic);
    }

    // Simple K-2 simulation with two players starting in opposite corners
    public static void runK2(int rows, int cols) {

        int[][] positions = new int[2][2];

        // Player 1 starts top-left
        positions[0][0] = 0;
        positions[0][1] = 0;

        // Player 2 starts bottom-right
        positions[1][0] = rows - 1;
        positions[1][1] = cols - 1;

        int steps = 0;

        // Continue until both players land on same position
        while (!(positions[0][0] == positions[1][0] &&
                positions[0][1] == positions[1][1])) {

            moveRandom(positions[0], rows, cols);
            moveRandom(positions[1], rows, cols);
            steps++;
        }

        System.out.println("They found each other in " + steps + " steps!");
    }

    // Runs multiple simulations and calculates statistics
    public static void analyzeSimulations(int rows, int cols, int players,
                                          int simulations, boolean systematic) {

        int totalSteps = 0;
        int shortest = Integer.MAX_VALUE;
        int longest = 0;

        for (int i = 1; i <= simulations; i++) {

            int steps = runSimulation(rows, cols, players, systematic);

            totalSteps += steps;
            shortest = Math.min(shortest, steps);
            longest = Math.max(longest, steps);

            System.out.println("Simulation " + i + " finished in " + steps + " steps.");
        }

        double average = (double) totalSteps / simulations;

        System.out.println("\nSimulation Results");
        System.out.println("Average Steps: " + average);
        System.out.println("Shortest Run: " + shortest);
        System.out.println("Longest Run: " + longest);

    }

    // Core simulation logic
    public static int runSimulation(int rows, int cols,
                                    int players, boolean systematic) {

        int[][] positions = new int[players][2];
        int[] group = new int[players];

        // Random starting positions
        for (int i = 0; i < players; i++) {
            positions[i][0] = rand.nextInt(rows);
            positions[i][1] = rand.nextInt(cols);
            group[i] = i;
        }

        int steps = 0;

        // Continue until all players merge into one group
        while (!allInOneGroup(group)) {

            Set<Integer> movedGroups = new HashSet<>();

            for (int i = 0; i < players; i++) {

                if (!movedGroups.contains(group[i])) {

                    if (systematic)
                        moveSystematic(group[i], positions, group, rows, cols);
                    else
                        moveGroupRandom(group[i], positions, group, rows, cols);

                    movedGroups.add(group[i]);
                }
            }

            checkForMeetings(positions, group);
            steps++;
        }

        return steps;
    }

    // Random movement for a group
    public static void moveGroupRandom(int groupID,
                                       int[][] positions,
                                       int[] group,
                                       int rows, int cols) {

        int move = rand.nextInt(4);

        for (int i = 0; i < group.length; i++) {
            if (group[i] == groupID) {
                moveByDirection(positions[i], move, rows, cols);
            }
        }
    }

    // Systematic movement pattern (row sweep)
    public static void moveSystematic(int groupID,
                                      int[][] positions,
                                      int[] group,
                                      int rows, int cols) {

        for (int i = 0; i < group.length; i++) {

            if (group[i] == groupID) {

                if (positions[i][1] < cols - 1) {
                    positions[i][1]++;
                }
                else if (positions[i][0] < rows - 1) {
                    positions[i][0]++;
                    positions[i][1] = 0;
                }
            }
        }
    }

    // Moves a single player randomly
    public static void moveRandom(int[] pos, int rows, int cols) {
        moveByDirection(pos, rand.nextInt(4), rows, cols);
    }

    // Moves based on direction (0 up, 1 down, 2 left, 3 right)
    public static void moveByDirection(int[] pos, int move,
                                       int rows, int cols) {

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
        }
    }

    // Checks if players land on same space and merges groups
    public static void checkForMeetings(int[][] positions, int[] group) {

        for (int i = 0; i < positions.length; i++) {

            for (int j = i + 1; j < positions.length; j++) {

                if (positions[i][0] == positions[j][0] &&
                        positions[i][1] == positions[j][1]) {

                    mergeGroups(group, group[i], group[j]);
                }
            }
        }
    }

    // Merges two groups into one
    public static void mergeGroups(int[] group, int g1, int g2) {

        int newGroup = Math.min(g1, g2);
        int oldGroup = Math.max(g1, g2);

        for (int i = 0; i < group.length; i++) {
            if (group[i] == oldGroup) {
                group[i] = newGroup;
            }
        }
    }

    // Checks if all players belong to same group
    public static boolean allInOneGroup(int[] group) {

        int first = group[0];

        for (int g : group) {
            if (g != first) {
                return false;
            }
        }

        return true;
    }
}
