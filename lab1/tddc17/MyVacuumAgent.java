package tddc17;

import aima.core.environment.liuvacuum.*;
import aima.core.agent.Action;
import aima.core.agent.AgentProgram;
import aima.core.agent.Percept;
import aima.core.agent.impl.*;

import java.util.Random;

import java.util.*;

class MyAgentState {
	public int[][] world = new int[30][30];
	final int UNKNOWN = 0;
	final int WALL = 1;
	final int CLEAR = 2;
	final int DIRT = 3;
	final int HOME = 4;
	final int ACTION_NONE = 0;
	final int ACTION_MOVE_FORWARD = 1;
	final int ACTION_TURN_RIGHT = 2;
	final int ACTION_TURN_LEFT = 3;
	final int ACTION_SUCK = 4;

	public int agent_x_position = 1;
	public int agent_y_position = 1;
	public int agent_last_action = ACTION_NONE;

	public static final int NORTH = 0;
	public static final int EAST = 1;
	public static final int SOUTH = 2;
	public static final int WEST = 3;
	public int agent_direction = EAST;

	// added
	public int home_x = -1;
	public int home_y = -1;
	public Set<Point> dirtLocations = new HashSet<>();
	public boolean allDirtCleaned = false;
	public boolean returningHome = false;

	MyAgentState() {
		for (int i = 0; i < world.length; i++)
			for (int j = 0; j < world[i].length; j++)
				world[i][j] = UNKNOWN;
		world[1][1] = HOME;
		agent_last_action = ACTION_NONE;

	}

	// Based on the last action and the received percept updates the x & y agent
	// position
	public void updatePosition(DynamicPercept p) {
		Boolean bump = (Boolean) p.getAttribute("bump");

		if (agent_last_action == ACTION_MOVE_FORWARD && !bump) {
			int newX = agent_x_position;
			int newY = agent_y_position;

			switch (agent_direction) {
				case MyAgentState.NORTH:
					newY--;
					break;
				case MyAgentState.EAST:
					newX++;
					break;
				case MyAgentState.SOUTH:
					newY++;
					break;
				case MyAgentState.WEST:
					newX--;
					break;
			}

			// CRITICAL: Validate bounds before updating position
			if (isWithinBounds(newX, newY)) {
				agent_x_position = newX;
				agent_y_position = newY;
			} else {
				System.out.println("WARNING: Attempted to move to invalid position (" +
						newX + "," + newY + "). Staying at (" +
						agent_x_position + "," + agent_y_position + ")");
			}
		}
	}

	public void updateWorld(int x_position, int y_position, int info) {
		world[x_position][y_position] = info;

		// *** ADDED: Track dirt locations ***
		Point pos = new Point(x_position, y_position);
		if (info == DIRT) {
			dirtLocations.add(pos);
		} else {
			dirtLocations.remove(pos);
		}
	}

	public void printWorldDebug() {
		for (int i = 0; i < world.length; i++) {
			for (int j = 0; j < world[i].length; j++) {
				if (world[j][i] == UNKNOWN)
					System.out.print(" ? ");
				if (world[j][i] == WALL)
					System.out.print(" # ");
				if (world[j][i] == CLEAR)
					System.out.print(" . ");
				if (world[j][i] == DIRT)
					System.out.print(" D ");
				if (world[j][i] == HOME)
					System.out.print(" H ");
			}
			System.out.println("");
		}
	}

	// ADDED: // Point class for BFS
	public static class Point {
		int x, y;

		Point(int x, int y) {
			this.x = x;
			this.y = y;
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof Point) {
				Point p = (Point) obj;
				return x == p.x && y == p.y;
			}
			return false;
		}

		@Override
		public int hashCode() {
			return Objects.hash(x, y);
		}

		@Override
		public String toString() {
			return "(" + x + "," + y + ")";
		}
	}

	public List<Point> findPathToClosestUnknown() {
		Queue<Point> frontier = new LinkedList<>();
		Set<Point> visited = new HashSet<>();
		Map<Point, Point> cameFrom = new HashMap<>();

		Point start = new Point(agent_x_position, agent_y_position);
		frontier.add(start);
		visited.add(start);

		int[] dx = { 0, 1, 0, -1 };
		int[] dy = { -1, 0, 1, 0 };

		while (!frontier.isEmpty()) {
			Point current = frontier.poll();

			// Found an unknown cell! (This is the goal)
			if (world[current.x][current.y] == UNKNOWN) {
				return reconstructPath(cameFrom, current, start);
			}

			// Explore neighbors,Can traverse through unknown now, could not beforre
			for (int i = 0; i < 4; i++) {
				int newX = current.x + dx[i];
				int newY = current.y + dy[i];

				if (newX >= 0 && newX < world.length &&
						newY >= 0 && newY < world[0].length) {

					Point neighbor = new Point(newX, newY);

					if (!visited.contains(neighbor) &&
							world[newX][newY] != WALL) { // Only block WALLS, not unknown

						visited.add(neighbor);
						cameFrom.put(neighbor, current);
						frontier.add(neighbor);
					}
				}
			}
		}

		return null; // No unknown cells reachable
	}

	// Reconstruct path from BFS
	private List<Point> reconstructPath(Map<Point, Point> cameFrom, Point goal, Point start) {
		List<Point> path = new ArrayList<>();
		Point current = goal;

		while (current != null && !current.equals(start)) {
			path.add(0, current); // Add to front
			current = cameFrom.get(current);
		}

		return path;
	}

	// Simple move towards position (like working code's moveTowards)
	public Action moveTowardsPosition(Point target) {
		Point forward = getForwardPosition();
		Point left = getLeftPosition();
		Point right = getRightPosition();

		if (target.equals(left)) {
			return LIUVacuumEnvironment.ACTION_TURN_LEFT;
		} else if (target.equals(right)) {
			return LIUVacuumEnvironment.ACTION_TURN_RIGHT;
		} else if (target.equals(forward)) {
			return LIUVacuumEnvironment.ACTION_MOVE_FORWARD;
		} else {
			// Default: turn left if target is behind
			return LIUVacuumEnvironment.ACTION_TURN_LEFT;
		}
	}

	// Helper methods for directional positions
	private Point getForwardPosition() {
		switch (agent_direction) {
			case NORTH:
				return new Point(agent_x_position, agent_y_position - 1);
			case EAST:
				return new Point(agent_x_position + 1, agent_y_position);
			case SOUTH:
				return new Point(agent_x_position, agent_y_position + 1);
			case WEST:
				return new Point(agent_x_position - 1, agent_y_position);
			default:
				return new Point(agent_x_position, agent_y_position);
		}
	}

	private Point getLeftPosition() {
		switch (agent_direction) {
			case NORTH:
				return new Point(agent_x_position - 1, agent_y_position);
			case EAST:
				return new Point(agent_x_position, agent_y_position - 1);
			case SOUTH:
				return new Point(agent_x_position + 1, agent_y_position);
			case WEST:
				return new Point(agent_x_position, agent_y_position + 1);
			default:
				return new Point(agent_x_position, agent_y_position);
		}
	}

	private Point getRightPosition() {
		switch (agent_direction) {
			case NORTH:
				return new Point(agent_x_position + 1, agent_y_position);
			case EAST:
				return new Point(agent_x_position, agent_y_position + 1);
			case SOUTH:
				return new Point(agent_x_position - 1, agent_y_position);
			case WEST:
				return new Point(agent_x_position, agent_y_position - 1);
			default:
				return new Point(agent_x_position, agent_y_position);
		}
	}

	public boolean isWithinBounds(int x, int y) {
		return x >= 0 && x < world.length && y >= 0 && y < world[0].length;
	}
}

class MyAgentProgram implements AgentProgram {

	private int initnialRandomActions = 10;
	private Random random_generator = new Random();

	// Here you can define your variables!
	public int iterationCounter = 1000; // increased for BFS
	public MyAgentState state = new MyAgentState();

	// moves the Agent to a random start position
	// uses percepts to update the Agent position - only the position, other
	// percepts are ignored
	// returns a random action
	private Action moveToRandomStartPosition(DynamicPercept percept) {
		int action = random_generator.nextInt(6);
		initnialRandomActions--;
		state.updatePosition(percept);
		if (action == 0) {
			state.agent_direction = ((state.agent_direction - 1) % 4);
			if (state.agent_direction < 0)
				state.agent_direction += 4;
			state.agent_last_action = state.ACTION_TURN_LEFT;
			return LIUVacuumEnvironment.ACTION_TURN_LEFT;
		} else if (action == 1) {
			state.agent_direction = ((state.agent_direction + 1) % 4);
			state.agent_last_action = state.ACTION_TURN_RIGHT;
			return LIUVacuumEnvironment.ACTION_TURN_RIGHT;
		}
		state.agent_last_action = state.ACTION_MOVE_FORWARD;
		return LIUVacuumEnvironment.ACTION_MOVE_FORWARD;
	}

	@Override
	public Action execute(Percept percept) {

		// DO NOT REMOVE this if condition!!!
		if (initnialRandomActions > 0) {
			return moveToRandomStartPosition((DynamicPercept) percept);
		} else if (initnialRandomActions == 0) {
			// process percept for the last step of the initial random actions
			initnialRandomActions--;
			state.updatePosition((DynamicPercept) percept);
			System.out.println("Processing percepts after the last execution of moveToRandomStartPosition()");
			state.agent_last_action = state.ACTION_SUCK;
			return LIUVacuumEnvironment.ACTION_SUCK;
		}

		// This example agent program will update the internal agent state while only
		// moving forward.
		// START HERE - code below should be modified!

		// code below modified for BFS-based intelligent behavior

		System.out.println("x=" + state.agent_x_position);
		System.out.println("y=" + state.agent_y_position);
		System.out.println("dir=" + state.agent_direction);

		iterationCounter--;
		if (iterationCounter == 0)
			return NoOpAction.NO_OP;

		DynamicPercept p = (DynamicPercept) percept;
		Boolean bump = (Boolean) p.getAttribute("bump");
		Boolean dirt = (Boolean) p.getAttribute("dirt");
		Boolean home = (Boolean) p.getAttribute("home");

		System.out.println("percept: " + p);

		// State update based on the percept value and the last action
		state.updatePosition((DynamicPercept) percept);

		// *** ADDED: Record home position when detected ***
		if (home && state.home_x == -1) {
			state.home_x = state.agent_x_position;
			state.home_y = state.agent_y_position;
			System.out.println("Home position recorded: (" + state.home_x + "," + state.home_y + ")");
		}

		if (bump) {
			switch (state.agent_direction) {
				case MyAgentState.NORTH:
					state.updateWorld(state.agent_x_position, state.agent_y_position - 1, state.WALL);
					break;
				case MyAgentState.EAST:
					state.updateWorld(state.agent_x_position + 1, state.agent_y_position, state.WALL);
					break;
				case MyAgentState.SOUTH:
					state.updateWorld(state.agent_x_position, state.agent_y_position + 1, state.WALL);
					break;
				case MyAgentState.WEST:
					state.updateWorld(state.agent_x_position - 1, state.agent_y_position, state.WALL);
					break;
			}
		}

		if (dirt)
			state.updateWorld(state.agent_x_position, state.agent_y_position, state.DIRT);
		else {
			int cellType = home ? state.HOME : state.CLEAR;
			state.updateWorld(state.agent_x_position, state.agent_y_position, cellType);
		}

		state.printWorldDebug();

		// *** MODIFIED: Intelligent action selection using BFS ***

		// Priority 1: Clean dirt in current cell
		if (dirt) {
			System.out.println("DIRT -> choosing SUCK action!");
			state.agent_last_action = state.ACTION_SUCK;
			return LIUVacuumEnvironment.ACTION_SUCK;
		}

		// Priority 2: Check if all dirt cleaned AND exploration complete, then return
		// home
		if (!state.allDirtCleaned && state.dirtLocations.isEmpty()) {
			// Only declare cleaning complete if no unexplored areas remain
			List<MyAgentState.Point> pathToUnknown = state.findPathToClosestUnknown();
			if (pathToUnknown == null) {
				state.allDirtCleaned = true;
				state.returningHome = true;
				System.out.println("All dirt cleaned AND exploration complete! Returning to home.");
			} else {
				System.out.println("No known dirt, but unexplored areas remain.");
			}
		}

		if (state.returningHome && state.home_x != -1) {
			if (state.agent_x_position == state.home_x && state.agent_y_position == state.home_y) {
				System.out.println("Reached home! Mission complete.");
				return NoOpAction.NO_OP;
			} else {
				// Simple pathfinding home - move toward home coordinates
				int dx = state.home_x - state.agent_x_position;
				int dy = state.home_y - state.agent_y_position;

				if (Math.abs(dx) > Math.abs(dy)) {
					// Move horizontally
					if (dx > 0 && state.agent_direction != MyAgentState.EAST) {
						state.agent_last_action = state.ACTION_TURN_RIGHT;
						state.agent_direction = (state.agent_direction + 1) % 4;
						return LIUVacuumEnvironment.ACTION_TURN_RIGHT;
					} else if (dx < 0 && state.agent_direction != MyAgentState.WEST) {
						state.agent_last_action = state.ACTION_TURN_LEFT;
						state.agent_direction = (state.agent_direction - 1 + 4) % 4;
						return LIUVacuumEnvironment.ACTION_TURN_LEFT;
					}
				} else {
					// Move vertically
					if (dy > 0 && state.agent_direction != MyAgentState.SOUTH) {
						state.agent_last_action = state.ACTION_TURN_RIGHT;
						state.agent_direction = (state.agent_direction + 1) % 4;
						return LIUVacuumEnvironment.ACTION_TURN_RIGHT;
					} else if (dy < 0 && state.agent_direction != MyAgentState.NORTH) {
						state.agent_last_action = state.ACTION_TURN_LEFT;
						state.agent_direction = (state.agent_direction - 1 + 4) % 4;
						return LIUVacuumEnvironment.ACTION_TURN_LEFT;
					}
				}

				System.out.println("Moving toward home...");
				state.agent_last_action = state.ACTION_MOVE_FORWARD;
				return LIUVacuumEnvironment.ACTION_MOVE_FORWARD;
			}
		}

		// Priority 3: Go to known dirt locations
		if (!state.dirtLocations.isEmpty()) {
			MyAgentState.Point nearestDirt = null;
			int minDistance = Integer.MAX_VALUE;

			for (MyAgentState.Point dirt_pos : state.dirtLocations) {
				int distance = Math.abs(dirt_pos.x - state.agent_x_position) +
						Math.abs(dirt_pos.y - state.agent_y_position);
				if (distance < minDistance) {
					minDistance = distance;
					nearestDirt = dirt_pos;
				}
			}

			if (nearestDirt != null) {
				// Simple movement toward dirt
				int dx = nearestDirt.x - state.agent_x_position;
				int dy = nearestDirt.y - state.agent_y_position;

				if (Math.abs(dx) > Math.abs(dy)) {
					if (dx > 0 && state.agent_direction != MyAgentState.EAST) {
						state.agent_last_action = state.ACTION_TURN_RIGHT;
						state.agent_direction = (state.agent_direction + 1) % 4;
						return LIUVacuumEnvironment.ACTION_TURN_RIGHT;
					} else if (dx < 0 && state.agent_direction != MyAgentState.WEST) {
						state.agent_last_action = state.ACTION_TURN_LEFT;
						state.agent_direction = (state.agent_direction - 1 + 4) % 4;
						return LIUVacuumEnvironment.ACTION_TURN_LEFT;
					}
				} else {
					if (dy > 0 && state.agent_direction != MyAgentState.SOUTH) {
						state.agent_last_action = state.ACTION_TURN_RIGHT;
						state.agent_direction = (state.agent_direction + 1) % 4;
						return LIUVacuumEnvironment.ACTION_TURN_RIGHT;
					} else if (dy < 0 && state.agent_direction != MyAgentState.NORTH) {
						state.agent_last_action = state.ACTION_TURN_LEFT;
						state.agent_direction = (state.agent_direction - 1 + 4) % 4;
						return LIUVacuumEnvironment.ACTION_TURN_LEFT;
					}
				}

				System.out.println("Moving toward dirt at " + nearestDirt);
				state.agent_last_action = state.ACTION_MOVE_FORWARD;
				return LIUVacuumEnvironment.ACTION_MOVE_FORWARD;
			}
		}

		// Priority 4: Explore unknown areas
		// Find path to ANY unknown cell using BFS
		List<MyAgentState.Point> pathToUnknown = state.findPathToClosestUnknown();

		if (pathToUnknown != null && !pathToUnknown.isEmpty()) {
			// Move towards first step in path
			MyAgentState.Point nextStep = pathToUnknown.get(0);
			Action moveAction = state.moveTowardsPosition(nextStep);

			if (moveAction != null) {
				// Update direction for turns
				if (moveAction == LIUVacuumEnvironment.ACTION_TURN_LEFT) {
					state.agent_last_action = state.ACTION_TURN_LEFT;
					state.agent_direction = (state.agent_direction - 1 + 4) % 4;
				} else if (moveAction == LIUVacuumEnvironment.ACTION_TURN_RIGHT) {
					state.agent_last_action = state.ACTION_TURN_RIGHT;
					state.agent_direction = (state.agent_direction + 1) % 4;
				} else if (moveAction == LIUVacuumEnvironment.ACTION_MOVE_FORWARD) {
					state.agent_last_action = state.ACTION_MOVE_FORWARD;
				}

				System.out.println("Moving towards unknown area at " + nextStep);
				return moveAction;
			}
		} else {
			// No unknown areas found - exploration complete
			if (!state.returningHome) {
				state.allDirtCleaned = true;
				state.returningHome = true;
				System.out.println("Exploration complete, returning home!");
			}
		}

		// Fallback behavior (similar to original but improved)
		if (bump) {
			state.agent_last_action = state.ACTION_TURN_RIGHT;
			state.agent_direction = (state.agent_direction + 1) % 4;
			return LIUVacuumEnvironment.ACTION_TURN_RIGHT;
		} else {
			state.agent_last_action = state.ACTION_MOVE_FORWARD;
			return LIUVacuumEnvironment.ACTION_MOVE_FORWARD;
		}
	}
}

public class MyVacuumAgent extends AbstractAgent {
	public MyVacuumAgent() {
		super(new MyAgentProgram());
	}
}