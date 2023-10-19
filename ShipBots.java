import java.util.*;

public class ShipBots {
    ShipGenerator shipGenerator; //shipGenerator object is an instance of our ShipGenerator class
    private int[][] moves = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}}; //this defines up, right, left, down movements
    private int[][] ship; //our generated ship we will run the simulations on
    private int[] botPosition, buttonPosition, firePosition; //initial positions of the bot, button, fire
    private int rows, cols; //stores the dimensions of our generated ship
    private final int maxIterations = 20000; //maxIterations is a constant used to prevent some methods for looping continously and running out of heap space and memory
    private double q; //'q' parameter between 0 and 1 that we will pass in
    private Queue<int[]> firePositionQueue; //Queue to store all the coordinates of the positions of the fire that it will spread to

    //ShipBots constructor that takes in an instance of the ShipGenerator class and a q value
    public ShipBots(ShipGenerator shipGenerator, double q) {
        //retrieves all necessary info, including an instance of the ShipGenerator class, the ship, dimensions, and initial positions
        this.shipGenerator = shipGenerator;
        this.ship = shipGenerator.getShip();
        this.rows = shipGenerator.getRows();
        this.cols = shipGenerator.getCols();
        this.botPosition = shipGenerator.getKeyPosition(ship, 2); 
        this.buttonPosition = shipGenerator.getKeyPosition(ship, 3);
        this.firePosition = shipGenerator.getKeyPosition(ship, 4);
        this.q = q;
        this.firePositionQueue = new LinkedList<>();
        firePositionQueue.add(firePosition); //add the initial fire position to the queue
    }

    private static class Path { //private class 'Path' within ShipBots class to help represent our path for Uniform Cost Search (used in Bot 2 and Bot 3)


        private List<int[]> path; //list of array coordinates to represent our path
        private int cost;   //represents the cost associated with each path movement
        private int[] position; //represents the current position in the path
    
        public Path(List<int[]> path, int cost, int[] position) {
            //the three attributes are passed in the 'Path' constructor
            this.path = path;
            this.cost = cost;
            this.position = position;
        }
    
        public List<int[]> getPath() { //getter method to retrieve the path list
            return path;
        }
    
        public int getCost() { //getter method to retrieve the cost
            return cost;
        }
    
        public int[] getPosition() { //getter method to retrieve the current position
            return position;
        }
    }   

    private static class PathComparator implements Comparator<Path> { //the Comparator interface is implemeted so we can objects (paths) by their costs in the priority queue
        @Override
        public int compare(Path path1, Path path2) { //method compares two 'Path' objects based on their individual 'cost' values
            return Integer.compare(path1.getCost(), path2.getCost()); //this means that 'Path' objects added to a priority queue are ordered on ascending order (lower the cost = more in front in the priority queue)
        } 
    }

    private List<int[]> getAllNeighbors(int[] position) { //returns a list of neighboring cell positions/coordinates around the passed in position
        List<int[]> neighbors = new ArrayList<>(); //'neighbors' will store all neighboring positions

        for (int[] move : moves) { //loop through the neighbors (up/right/left/down)
            int currRow = position[0] + move[0]; //obtain row coordinate of curr neighbor
            int curCol = position[1] + move[1]; //obtain col coordinate of curr neighbor

            if (shipGenerator.inShip(currRow, curCol)) { //if this neighbor is in the ship, add it to our 'neighbors' list
                neighbors.add(new int[]{currRow, curCol});
            }
        }

        return neighbors; //return the neighbors list
    }

    private boolean isAdjacentToFire(int[] cell, Queue<int[]> firePositionQueue) { //this method helps determine whether the passed in 'cell' position is adjacent to a fire cell 
        for (int[] fireCell : firePositionQueue) { //firePositionQueue holds all fire positions, loop through each position

            //if both the row coordinate and column coordinate of 'cell' is within 1 absolute unit in any direction (u/r/l/d) of a fireCell (a position we know is on fire), then it is adjacent to a fire cell
            if (Math.abs(cell[0] - fireCell[0]) <= 1 && Math.abs(cell[1] - fireCell[1]) <= 1) { 
                return true; //returns true if the position is adjacent to a fire cell
            }
        }
        return false; //return false if no positions are found to be adjacent to a fire cell
    }

    private int getK(int row, int col) { //method returns the number of cells neighboring the (row,col) position that are on fire
        int count = 0;

        for (int[] move : moves) { //loop through all moves (up, left, right, down)
            int currRow = row + move[0]; //obtain current row coordinate of neighbor
            int curCol = col + move[1]; //obtain current column coordinate of neighbor


            //checks if the coordinates are 1. In the ship 2. Equal to 4 (a fire cell) 3. Not equal to 1 (A blocked cell)
            if (shipGenerator.inShip(currRow, curCol) && ship[currRow][curCol] == 4 && ship[currRow][curCol] != 1) {
                count++; //add to count if conditions are met
            }
        }

        return count; //return count (num of neighboring cells on fire)
    }
    
    private boolean containsFireOrBlockedCells(List<int[]> path) { //method checks whether any positions in the 'path' list are fire cells are blocked cells 
        for (int[] step : path) { //loop through each 'step' in the path
            if (ship[step[0]][step[1]] == 1 || ship[step[0]][step[1]] == 4) { //checks if path contains fire/blocked cells
                return true; //return true if it does
            }
        }
        return false; // return false if the path is 'clean' (containing no fire/blocked cells)
    }

    private List<int[]> planPathToButton(int[] start, int[] goal, boolean[][] visited) {//method plans a path from the start to goal position that avoids current fire cells
        
        //priority queue to prioritize paths with the lowest cost
        PriorityQueue<Path> allPathsQueues = new PriorityQueue<>(new PathComparator());
    
        //initialize starting position with a cost of 0 and add it to the 'allPathsQueues'
        Path initialPath = new Path(new ArrayList<>(), 0, start);
        allPathsQueues.add(initialPath);
        

        //while loop continues until either allPathsQueues is empty or iterations exceed our constant 'maxIterations'
        //as mentioned before, maxIterations is set as 20,000 to prevent heap/memory issues
        int iterations = 0;
        while (!allPathsQueues.isEmpty() && iterations < maxIterations) {
            Path currentPath = allPathsQueues.poll(); //retrieve the 'Path' with the lowest cost
            int[] currentCell = currentPath.getPosition(); //set our currentCell equal to the current position in the 'currentPath'
    
            if (Arrays.equals(currentCell, goal)) { //if our currentCell is equal to the goal, this means a path to the goal has been found
                return currentPath.getPath(); // return the path that leads to the goal
            }

            //if its not the goal, we explore the neighbors of 'currentCell'
    
            List<int[]> neighbors = getAllNeighbors(currentCell); //this retrieves the neighbors of 'currentCell' and stores it in the 'neighbors' list
    
            for (int[] neighbor : neighbors) { //loop through all the neighbors in the 'neighbors' list
                if (!visited[neighbor[0]][neighbor[1]] && ship[neighbor[0]][neighbor[1]] != 1) { //check if the neighbor is unvisited and not a blocked/wall cell
                    
                    List<int[]> newPath = new ArrayList<>(currentPath.getPath()); //create a new path starting from the 'currentPath' position and onwards
                    newPath.add(neighbor); // Add the neighbor of 'currentPath' to the new path 'newPath'
                    int newCost = currentPath.getCost() + 1; //update the cost by 1 additional value
    
                    //Add the new path 'newPath' with the updated cost to the priority queue
                    allPathsQueues.add(new Path(newPath, newCost, neighbor));
                }
            }
            iterations++; 
        }
    
        return null; //return null if there was no valid path that could be found from the bot to the button (start to goal)
    }

    private List<int[]> planPathToButtonWithFireAvoidance(int[] start, int[] goal, boolean[][] visited, Queue<int[]> firePositionQueue) { //method returns a path from bot to button (start to goal) that avoids cells adjacent to fire cells

        /* THIS METHOD IS JUST LIKE THE 'planPathToButton' METHOD, HOWEVER THIS ALSO AVOIDS CELLS THAT ARE ADJACENT TO FIRE CELLS */

        PriorityQueue<Path> allPathsQueues = new PriorityQueue<>(new PathComparator());
    
        Path initialPath = new Path(new ArrayList<>(), 0, start);
        allPathsQueues.add(initialPath);
    
        int iterations = 0;
        while (!allPathsQueues.isEmpty() && iterations < maxIterations) {
            Path currentPath = allPathsQueues.poll();
            int[] currentCell = currentPath.getPosition();
    
            if (Arrays.equals(currentCell, goal)) {
                return currentPath.getPath(); 
            }
    
            List<int[]> neighbors = getAllNeighbors(currentCell);
    
            for (int[] neighbor : neighbors) {
                if (!visited[neighbor[0]][neighbor[1]] && ship[neighbor[0]][neighbor[1]] != 1) {
                    List<int[]> newPath = new ArrayList<>(currentPath.getPath()); 
                    newPath.add(neighbor); 
                    int newCost = currentPath.getCost() + 1; 
    
                    if (!isAdjacentToFire(neighbor, firePositionQueue)) {
                        allPathsQueues.add(new Path(newPath, newCost, neighbor));
                    }
                }
            }
            iterations++;
        }
    
        return null;
    }

    private int heuristic(int[] start, int[] goal) { //this will be the heuristic value used for the A star algorithm, which is the distance, or "Manhattan" distance from the start position to goal position
        return Math.abs(start[0] - goal[0]) + Math.abs(start[1] - goal[1]);
    }

    private List<int[]> pathPlanAStar(int[] start, int[] goal, boolean[][] visited, Queue<int[]> fireQueue) {
    
          /* THIS METHOD IS JUST LIKE THE 'planPathToButton' and 'planPathToButtonWithFireAvoidance' METHOD, HOWEVER THE ONLY DIFFERENCE IS THE COST IS CALCULATED USING THE HEURISTIC DISTANCE VALUE INSTEAD OF JUST INCREMENTING IT BY +1*/

        PriorityQueue<Path> allPathsQueues = new PriorityQueue<>(new PathComparator());
    
  
        Path initialPath = new Path(new ArrayList<>(), 0, start);
        allPathsQueues.add(initialPath);
    
        int iterations = 0;
        while (!allPathsQueues.isEmpty() && iterations < maxIterations) {
            Path currentPath = allPathsQueues.poll();
            int[] currentCell = currentPath.getPosition();
    
            if (Arrays.equals(currentCell, goal)) {
                return currentPath.getPath(); //if the current path leads to the goal, just return it
            }
    
            List<int[]> neighbors = getAllNeighbors(currentCell);
            for (int[] neighbor : neighbors) {
                if (!visited[neighbor[0]][neighbor[1]] && ship[neighbor[0]][neighbor[1]] != 1) {
                    List<int[]> newPath = new ArrayList<>(currentPath.getPath());
                    newPath.add(neighbor);
                    int newCost = currentPath.getCost() + 1;
    
                    //calculate the heuristic (the current distance left to get to the goal)
                    int heuristicCost = heuristic(neighbor, goal);
                    
                    //calculate the total 'cost' as the sum of the new cost plus the distance that the 'heuristic' function returns
                    int cost = newCost + heuristicCost;
    
                    if (!isAdjacentToFire(neighbor, fireQueue)) {
                        //add the new path with the right 'cost' value to the priority queue
                        allPathsQueues.add(new Path(newPath, cost, neighbor));
                    }
                }
            }
            iterations++;
        }
    
        return null; // Return null if there was no valid path to the button
    }

    private void spreadFire(Queue<int[]> firePositionQueue) { //this method simulates the spreading of the fire, takes in the firePositionQueue (which currently stores the initial fire position)
        
        //only included this so that I could make sure that the K value for the first iteration was set to 1
        boolean isFirstIteration = true; 

        Queue<int[]> newfirePositionQueue = new LinkedList<>(); //create a new firePositionQueue to store the cells that are to be ignited in this current iteration
    
        //iterate through each cell in the firePositionQueue (each cell here should be on fire already)
        for (int i = 0; i < firePositionQueue.size(); i++) {
            int[] currentFire = firePositionQueue.poll(); //retrieve the the current fire cells position
    
            //iterate through the neighboring cells of the current fire cell
            for (int[] move : moves) {
                int currRow = currentFire[0] + move[0]; //retrieve the row coordinate
                int curCol = currentFire[1] + move[1]; //retrieve the neighbor column coordinate
    
                if (shipGenerator.inShip(currRow, curCol)) { //check if this coordinate is within our ship bounds
                    //check if the neighboring cell is not already on fire/blocked
                    if (ship[currRow][curCol] != 4 && ship[currRow][curCol] != 1 ) {
                        //Calculate 'K' value based on the number of neighboring cells that are on fire

                        //this just makes sure that if we are in first iteration K = 1, otherwise we can call the 'getK' helper method to retrieve K
                        int K = isFirstIteration ? 1 : getK(currRow, curCol); 
    
                        //calculate the probability of this neighbor cell catching fire
                        double rand = Math.random();//generate a random number between 0 and 1

                        //if the formula (1-(1-q)^K returns a number greater than the randomly generated number, we can say the cell is now on fire
                        if (rand < (1 - Math.pow((1 - q), K))) { 
                            //set the cell value equal to 4 to represent it being on fire and add it to the 'newfirePositionQueue'
                            ship[currRow][curCol] = 4;
                            newfirePositionQueue.add(new int[]{currRow, curCol});
                        }
                    }
                }
            }
            isFirstIteration = false; //now the 'K' value will be retrieved from the 'getK' method
        }
    
        //add newly ignited cells that were added to 'newfirePositionQueue' to the 'firePositionQueue' for the next iteration
        firePositionQueue.addAll(newfirePositionQueue);
    }

    public void botOneSimulation() { //method simulates BFS exploring of Bot One to find the shortest path to the button, the bot ignores the spread of the fire
        
        //create a 'botPositionQueue' and 'firePositionQueue' to store the bot and fire positions
        Queue<int[]> botPositionQueue = new LinkedList<>(); 
        Queue<int[]> firePositionQueue = new LinkedList<>();
        //create 2D boolean array to track the visited positions of the bot (true = visited, and false = non-visited)
        boolean[][] visited = new boolean[rows][cols];
    
        botPositionQueue.add(botPosition); //add bot's initial position to 'botPositionQueue'
        visited[botPosition[0]][botPosition[1]] = true; //set the initial position 
        firePositionQueue.add(firePosition); //add fire's initial position to the firePositionQueue

        //continue iterating until there are no more positions for the bot to explore or if iterations exceed our constant maxIterations
        int iterations = 0;
        while (!botPositionQueue.isEmpty() && iterations < maxIterations) {
            int[] currentBot = botPositionQueue.poll(); //retrieve the earliest cell explored by the bot in the queue
    
            //check if the current bot position equals the current button position and is not a fire cell, if so, simulation is successful
            if (Arrays.equals(currentBot, buttonPosition) && ship[currentBot[0]][currentBot[1]] != 4) {
                System.out.println("Success! The bot has reached the button and put out the fire in the ship!.");
                return;
            }
    
            List<int[]> botNeighbors = getAllNeighbors(currentBot); //retrieve all neighbor positions of the current bot position
    
            for (int[] botNeighbor : botNeighbors) { //loop through each of the neighbors of the current bot position
                if (!visited[botNeighbor[0]][botNeighbor[1]] && ship[botNeighbor[0]][botNeighbor[1]] == 1) { 
                    //if the neighbor is a wall or if it is already visited, we can continue onwards
                    continue;
                }
                botPositionQueue.add(botNeighbor); //add the neighbor position to the queue if it is not a wall or already visited
                visited[botNeighbor[0]][botNeighbor[1]] = true; //set that position to true to represent it being a visited cell in 'visited'
            }
    
            //call the spreadFire() method, passing in the firePositionQueue that stores the current fire positions, to simulate the spreading of the fire
            spreadFire(firePositionQueue);
    
            //check if the current bot's position is on fire, if so, simulate is a failure
            if (ship[currentBot[0]][currentBot[1]] == 4) {
                System.out.println("Failure! The bot has caught on fire!");
                return;
            }
            iterations++;
        }
    }

    public void botTwoSimulation() { //method simulates the bot re-planning (Using uniform cost search) the shortest path from itself to the button at each iteration whilst avoiding current fire cells

        //create a 'botPositionQueue' and 'firePositionQueue' to store the bot and fire positions
        Queue<int[]> botPositionQueue = new LinkedList<>();
        Queue<int[]> firePositionQueue = new LinkedList<>();

        //create a 2D boolean array 'visited' to store true for visited and false for unvisited
        boolean[][] visited = new boolean[rows][cols];
    
        botPositionQueue.add(botPosition); //add the initial bot position to the 'botPositionQueue'
        visited[botPosition[0]][botPosition[1]] = true; //mark this position as visited
        firePositionQueue.add(firePosition); //add the initial fire position to the 'firePositionQueue'
    
        int iterations = 0;
        List<int[]> currentPath = null; //create and set our 'currentPath' list to null/empty, which will store the current generated path the bot is exploring
        

        //continue the while loop until the 'botPositionQueue' is empty meaning no more paths/positions to explore or if the maxIterations constant is exceeded
        while (!botPositionQueue.isEmpty() && iterations < maxIterations) {
            //set currentPath equal to a new re-planned path that is generated from the 'planPathToButton' method which takes in the botPosition and buttonPosotion as the start/gpal values respectively
            //this re-planned path should avoid blocked/fire cells
            currentPath = planPathToButton(botPosition, buttonPosition, visited);
    
            //checks if there exists a valid/clean path from the current position to the button, if so, the simulation is succesful since we know the bot can take this path with no issues
            if (currentPath != null && !containsFireOrBlockedCells(currentPath)) {
                System.out.println("Success! The bot has reached the button and put out the fire in the ship!");
                return;
            }
    
            //iterate through the current re-planned path as long as its not empty
            if (currentPath != null) {
                for (int[] step : currentPath) { //loop through each step/position in this pth
                    if (visited[step[0]][step[1]]) { //if the cell is visited already, just continue
                        continue; 
                    }
                    visited[step[0]][step[1]] = true; //set the position to visited and add that step to the 'botPositionQueue'
                    botPositionQueue.add(step);
                }
            }
    
            //call the spreadFire() method to simualte the spreading at each iteration
            spreadFire(firePositionQueue);
    
            //check if the bot's current position is on fire (equal to 4), if so, simulation is a failure
            if (ship[botPosition[0]][botPosition[1]] == 4) {
                System.out.println("Failure! The bot has caught on fire!");
                return;
            }
    
            botPosition = botPositionQueue.poll(); //update the bot's position to be the least-recent added element in the queue
            iterations++;
        }
    }

    public void botThreeSimulation() { //method simulates the bot re-planning (Using uniform cost search) the shortest path from itself to the button at each iteration whilst avoiding cells adjacent to fire cells
        
        /*SAME PROCESS AS 'botTwoSimulation', THE ONLY DIFFERENCE IS WE SET 
        'currentPath' VALUE USING THE 'planPathToButtonWithFireAvoidance' method 
        instead of the 'planPathToButton' method */

        Queue<int[]> botPositionQueue = new LinkedList<>();
        Queue<int[]> firePositionQueue = new LinkedList<>();
        boolean[][] visited = new boolean[rows][cols];
    
        botPositionQueue.add(botPosition);
        visited[botPosition[0]][botPosition[1]] = true;
        firePositionQueue.add(firePosition);
    
        int iterations = 0;
        List<int[]> currentPath = null; 
    
        while (!botPositionQueue.isEmpty() && iterations < maxIterations) {
            currentPath = planPathToButtonWithFireAvoidance(botPosition, buttonPosition, visited, firePositionQueue);
    
            if (currentPath != null && !containsFireOrBlockedCells(currentPath)) {
                System.out.println("Success! The bot has reached the button and put out the fire in the ship!");
                return;
            }
    
            currentPath = planPathToButton(botPosition, buttonPosition, visited);
    
            if (currentPath != null && !containsFireOrBlockedCells(currentPath)) {
                System.out.println("Success! The bot has reached the button and put out the fire in the ship!");
                return;
            }
    
            List<int[]> botNeighbors = getAllNeighbors(botPosition);
    
            for (int[] botNeighbor : botNeighbors) {
                if (!visited[botNeighbor[0]][botNeighbor[1]] && ship[botNeighbor[0]][botNeighbor[1]] != 1) {
                 
                    botPositionQueue.add(botNeighbor);
                    visited[botNeighbor[0]][botNeighbor[1]] = true;
                }
            }
    
            spreadFire(firePositionQueue);
    
            if (ship[botPosition[0]][botPosition[1]] == 4) {
                System.out.println("Failure! The bot has caught on fire!");
                return;
            }
    
            botPosition = botPositionQueue.poll(); 
            iterations++;
        }
    }

    public void botFourSimulation() { //method simulates the bot re-planning the shortest path from itself to the button using A star algorithm
        
         /*SAME PROCESS AS 'botTwoSimulation' and 'botThreeSimulation', 
         THE ONLY DIFFERENCE IS WE SET 'currentPath' VALUE USING THE 
         'pathPlanAStar' method instead of the 'planPathToButton' or 'planPathToButtonWIthFireAvoidance' method */

        Queue<int[]> botPositionQueue = new LinkedList<>();
        Queue<int[]> firePositionQueue = new LinkedList<>();
    
        boolean[][] visited = new boolean[rows][cols];
    
        botPositionQueue.add(botPosition); 
        visited[botPosition[0]][botPosition[1]] = true; 
        firePositionQueue.add(firePosition);
    
        int iterations = 0;
        List<int[]> currentPath = null; 
    
        while (!botPositionQueue.isEmpty() && iterations < maxIterations) {
            
            currentPath = pathPlanAStar(botPosition, buttonPosition, visited, firePositionQueue);
    
            if (currentPath != null && !containsFireOrBlockedCells(currentPath)) {
                System.out.println("Success! The bot has reached the button and put out the fire in the ship!");
                return;
            }
    
            if (currentPath != null) {
                for (int[] step : currentPath) {
                    if (visited[step[0]][step[1]]) {
                        continue; 
                    }
                    visited[step[0]][step[1]] = true; 
                    botPositionQueue.add(step);
                }
            }
    

            spreadFire(firePositionQueue);
    
            if (ship[botPosition[0]][botPosition[1]] == 4) {
                System.out.println("Failure! The bot has caught on fire!");
                return;
            }
    
            botPosition = botPositionQueue.poll();
            iterations++;
        }
    }

    public static void main(String[] args) {

        //run 250 simulations for botOne, botTwo, botThree, and botFour
        //can adjust the q values myself

        /* DISCLAIMER: THE NUMBER OF OUTPUTS MIGHT NOT BE EQUAL TO 500 FOR SOME BOT SIMULATIONS....
         * THIS IS BECAUSE IN SOME SIMULATIONS, THE WHILE LOOPS CONTINUE INDEFINITELY. TO WORK AROUND THIS, 
         * I JUST RAN THE LOOP MULTIPLE TIMES UNTIL THE 500 "Success"/"Failure" STATEMENTS WERE DISPLAYED
         */

        /* BOT ONE SIMULATIONS */
        
        // for(int i = 0; i < 500; i++){
        //     new ShipBots(new ShipGenerator(100, 100),0.7).botOneSimulation();
        // }

        // for(int i = 0; i < 500; i++){
        //     new ShipBots(new ShipGenerator(100, 100),0.55).botOneSimulation();
        // }

        // for(int i = 0; i < 500; i++){
        //     new ShipBots(new ShipGenerator(100, 100),0.85).botOneSimulation();
        // }


        /* BOT TWO SIMULATIONS */
        
        for(int i = 0; i < 500; i++){
            new ShipBots(new ShipGenerator(100, 100),0.20).botTwoSimulation();
        }

        // for(int i = 0; i < 500; i++){
        //     new ShipBots(new ShipGenerator(100, 100),0.55).botTwoSimulation();
        // }

        // for(int i = 0; i < 500; i++){
        //     new ShipBots(new ShipGenerator(100, 100),0.85).botTwoSimulation();
        // }

        /* BOT THREE SIMULATIONS */
        
        // for(int i = 0; i < 500; i++){
        //     new ShipBots(new ShipGenerator(100, 100),0.20).botThreeSimulation();
        // }

        // for(int i = 0; i < 500; i++){
        //     new ShipBots(new ShipGenerator(100, 100),0.55).botThreeSimulation();
        // }

        // for(int i = 0; i < 500; i++){
        //     new ShipBots(new ShipGenerator(100, 100),0.85).botThreeSimulation();
        // }


         /* BOT FOUR SIMULATIONS */

        // for(int i = 0; i < 500; i++){
        //     new ShipBots(new ShipGenerator(100, 100),0.20).botFourSimulation();
        // }

        // for(int i = 0; i < 500; i++){
        //     new ShipBots(new ShipGenerator(100, 100),0.55).botFourSimulation();
        // }

        // for(int i = 0; i < 500; i++){
        //     new ShipBots(new ShipGenerator(100, 100),0.85).botFourSimulation();
        // }
    }
}
