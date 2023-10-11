import java.util.*;

public class ShipGenerator {
    private int[][] ship;
    private int rows, cols;
    private Random random = new Random();

    public ShipGenerator(int rows, int cols){
        this.rows = rows;
        this.cols = cols;
        this.ship = new int[rows][cols];
        generateShipConfig();
        loosenShipConfigDeadEnds((findDeadEnds().size() / 2) - 1);
        initializeBotButtonFirePositions();
    }

    public int[][] getShip() {
        return ship;
    }
    public int getRows() {
        return rows;
    }
    public int getCols() {
        return cols;
    }

    private void initializeBotButtonFirePositions() {
        int[] buttonPosition;
        int[] initialFirePosition;
        int[] initialBotPosition;
        do {
            // Generate random positions for button, bot, and initial fire
            buttonPosition = randomOpenShipPosition();
            initialFirePosition = randomOpenShipPosition();
            initialBotPosition = randomOpenShipPosition();
        } while (Arrays.equals(buttonPosition, initialFirePosition) 
                || Arrays.equals(initialFirePosition, initialBotPosition) 
                || Arrays.equals(initialBotPosition, buttonPosition));

        // Set the values for the button, initial fire, and initial bot positions
        ship[buttonPosition[0]][buttonPosition[1]] = 3;  // Set button position to 3
        ship[initialFirePosition[0]][initialFirePosition[1]] = 4;  // Set initial fire position to 4
        ship[initialBotPosition[0]][initialBotPosition[1]] = 2;  // Set initial bot position to 2
    }

    private void initializeShip(){
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                ship[i][j] = 1;
            }
        }
    }

    private void generateShipConfig() {
        // Initialize all cells as blocked (1)
        initializeShip();

        // Choose a random starting point within the valid range and mark it as open (0)
        int startRow = random.nextInt(rows);
        int startCol = random.nextInt(cols);
        ship[startRow][startCol] = 0;

        // Create a list of cells with exactly one open neighbor
        List<int[]> candidates = new ArrayList<>();
        candidates.add(new int[]{startRow - 1, startCol});
        candidates.add(new int[]{startRow + 1, startCol});
        candidates.add(new int[]{startRow, startCol - 1});
        candidates.add(new int[]{startRow, startCol + 1});

        while (!candidates.isEmpty()) {
            // Choose a random cell from the candidates
            int randIndex = random.nextInt(candidates.size());
            int[] currentCell = candidates.remove(randIndex);
            int row = currentCell[0];
            int col = currentCell[1];

            // Check if the cell is valid
            if (!inShip(row, col)) {
                continue; // Skip this cell and choose another candidate
            }


            // Count the number of open neighbors
            int openNeighbors = 0;
            if (inShip(row - 1, col) && ship[row - 1][col] == 0) openNeighbors++;
            if (inShip(row + 1, col) && ship[row + 1][col] == 0) openNeighbors++;
            if (inShip(row, col - 1) && ship[row][col - 1] == 0) openNeighbors++;
            if (inShip(row, col + 1) && ship[row][col + 1] == 0) openNeighbors++;

            if (openNeighbors == 1) {
                ship[row][col] = 0;

            //Add neighbors of the current cell to the candidates list
                candidates.add(new int[]{row - 1, col});
                candidates.add(new int[]{row + 1, col});
                candidates.add(new int[]{row, col - 1});
                candidates.add(new int[]{row, col + 1});
            }
        }
    }

    public boolean inShip(int row, int col) {
        return row >= 0 && row < rows && col >= 0 && col < cols;
    }

    public void printship() {
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                System.out.print(ship[i][j] + "  ");
            }
            System.out.println();
        }
    }
    
    private int[] randomOpenShipPosition() {
        int row, col;
        do {
            row = random.nextInt(rows);
            col = random.nextInt(cols);
        } while (ship[row][col] != 0);
        return new int[]{row, col};
    }

    private boolean isShipCellInterior(int row, int col) {
        return inShip(row - 1, col) && inShip(row + 1, col) && inShip(row, col - 1) && inShip(row, col + 1);
    }

    private boolean hasExactlyOneOpenNeighbor(int row, int col) {
        int openNeighborCount = 0;
        if (inShip(row - 1, col) && ship[row - 1][col] == 0) openNeighborCount++;
        if (inShip(row + 1, col) && ship[row + 1][col] == 0) openNeighborCount++;
        if (inShip(row, col - 1) && ship[row][col - 1] == 0) openNeighborCount++;
        if (inShip(row, col + 1) && ship[row][col + 1] == 0) openNeighborCount++;
        return openNeighborCount == 1;
    }

    private boolean hasExactlyThreeBlockedNeighbors(int row, int col) {
        int blockedNeighborCount = 0;
        if (inShip(row - 1, col) && ship[row - 1][col] == 1) blockedNeighborCount++;
        if (inShip(row + 1, col) && ship[row + 1][col] == 1) blockedNeighborCount++;
        if (inShip(row, col - 1) && ship[row][col - 1] == 1) blockedNeighborCount++;
        if (inShip(row, col + 1) && ship[row][col + 1] == 1) blockedNeighborCount++;
        return blockedNeighborCount == 3;
    }

    private List<int[]> findDeadEnds() {
        List<int[]> deadEndPositions = new ArrayList<>();
    
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                if (ship[i][j] == 0 && isShipCellInterior(i, j) && hasExactlyThreeBlockedNeighbors(i, j) && hasExactlyOneOpenNeighbor(i, j)) {
                    // Check if the current cell meets the updated criteria for a dead-end cell.
                    // - It must be open (ship[i][j] == 0).
                    // - It must have exactly four valid neighbors (up, down, left, and right).
                    // - Out of these four valid neighbors, three must be blocked (ship[i][j] == 1).
                    // - Exactly one of the four valid neighbors must be open (ship[i][j] == 0).
                    deadEndPositions.add(new int[]{i, j});
                }
            }
        }
    
        return deadEndPositions;
    }

    private void loosenShipConfigDeadEnds(int numDeadEndsToModify) {
        List<int[]> deadEndPositions = findDeadEnds();
    
        int numModified = 0;
        while (numModified < numDeadEndsToModify && !deadEndPositions.isEmpty()) {
            // Randomly select a dead-end position from the list
            int randIndex = random.nextInt(deadEndPositions.size());
            int[] position = deadEndPositions.get(randIndex);
    
            // Check if the selected dead-end cell has exactly three blocked neighbors
            if (hasExactlyThreeBlockedNeighbors(position[0], position[1])) {
                // Initialize a list to store the blocked neighbors
                List<int[]> blockedNeighbors = new ArrayList<>();
    
                // Check each of the four possible neighbors
                if (inShip(position[0] - 1, position[1]) && ship[position[0] - 1][position[1]] == 1) {
                    blockedNeighbors.add(new int[]{position[0] - 1, position[1]});
                }
                if (inShip(position[0] + 1, position[1]) && ship[position[0] + 1][position[1]] == 1) {
                    blockedNeighbors.add(new int[]{position[0] + 1, position[1]});
                }
                if (inShip(position[0], position[1] - 1) && ship[position[0]][position[1] - 1] == 1) {
                    blockedNeighbors.add(new int[]{position[0], position[1] - 1});
                }
                if (inShip(position[0], position[1] + 1) && ship[position[0]][position[1] + 1] == 1) {
                    blockedNeighbors.add(new int[]{position[0], position[1] + 1});
                }
    
                // If there are blocked neighbors, choose one randomly to open
                if (!blockedNeighbors.isEmpty()) {
                    int randNeighborIndex = random.nextInt(blockedNeighbors.size());
                    int[] neighbor = blockedNeighbors.get(randNeighborIndex);
                    ship[neighbor[0]][neighbor[1]] = 0; // Open the selected blocked neighbor
                    numModified++; // Increment the count of modified dead-end cells
    
                    // Print the chosen dead-end cell position and the modified neighbor
                    //System.out.println("Chosen Dead-End Cell: (" + position[0] + ", " + position[1] + ")");
                    //System.out.println("Modified Neighbor: (" + neighbor[0] + ", " + neighbor[1] + ")");
                }
            }
            // Remove the processed dead-end position from the list
            deadEndPositions.remove(randIndex);
        } 
    }
    
    public static void main(String[] args) {
        new ShipGenerator(20, 20).printship();
    }
}