package searchCustom;

import java.util.ArrayList;
import java.util.HashSet;

import searchShared.NodeQueue;
import searchShared.Problem;
import searchShared.SearchObject;
import searchShared.SearchNode;

import world.GridPos;

public class CustomGraphSearch implements SearchObject {

	private HashSet<SearchNode> explored;
	private NodeQueue frontier;
	protected ArrayList<SearchNode> path;
	private boolean insertFront;

	/**
	 * The constructor tells graph search whether it should insert nodes to front or
	 * back of the frontier
	 */
	public CustomGraphSearch(boolean bInsertFront) {
		insertFront = bInsertFront;
	}

	/**
	 * Implements "graph search", which is the foundation of many search algorithms
	 */
	public ArrayList<SearchNode> search(Problem p) {
		// The frontier is a queue of expanded SearchNodes not processed yet
		frontier = new NodeQueue();
		/// The explored set is a set of nodes that have been processed
		explored = new HashSet<SearchNode>();
		// The start state is given
		GridPos startState = (GridPos) p.getInitialState();
		// Initialize the frontier with the start state
		frontier.addNodeToFront(new SearchNode(startState));

		// Path will be empty until we find the goal.
		path = new ArrayList<SearchNode>();

		// Implement this!
		// System.out.println("Implement CustomGraphSearch.java!");

		// Main graph search loop
		while (!frontier.isEmpty()) {
			// Choose a node from frontier
			SearchNode currentNode = frontier.removeFirst();
			GridPos currentState = (GridPos) currentNode.getState();

			// Check if we've reached the goal
			if (p.isGoalState(currentState)) {
				path = currentNode.getPathFromRoot();
				return path;
			}

			// Add current node to explored set
			explored.add(currentNode);

			// Get all reachable child states from current state
			ArrayList<GridPos> childStates = p.getReachableStatesFrom(currentState);

			// Process each child state
			for (GridPos childState : childStates) {
				SearchNode childNode = new SearchNode(childState, currentNode);

				// Only add child to frontier if not already explored or in frontier
				if (!explored.contains(childNode) && !frontier.contains(childNode)) {
					if (insertFront) {
						frontier.addNodeToFront(childNode);
					} else {
						frontier.addNodeToBack(childNode);
					}
				}
			}
		}

		/* Note: Returning an empty path signals that no path exists */
		return path;
	}

	/*
	 * Functions below are just getters used externally by the program
	 */
	public ArrayList<SearchNode> getPath() {
		return path;
	}

	public ArrayList<SearchNode> getFrontierNodes() {
		return new ArrayList<SearchNode>(frontier.toList());
	}

	public ArrayList<SearchNode> getExploredNodes() {
		return new ArrayList<SearchNode>(explored);
	}

	public ArrayList<SearchNode> getAllExpandedNodes() {
		ArrayList<SearchNode> allNodes = new ArrayList<SearchNode>();
		allNodes.addAll(getFrontierNodes());
		allNodes.addAll(getExploredNodes());
		return allNodes;
	}

}
