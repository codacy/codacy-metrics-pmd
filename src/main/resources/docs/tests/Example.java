// #Metrics: {"complexity": 5}
package com.ai.astar;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

/**
 * A Star Algorithm
 *
 */
public class AStar {
    private static int DEFAULT_HV_COST = 10; // Horizontal - Vertical Cost
    private static int DEFAULT_DIAGONAL_COST = 14;
    private int hvCost;
    private int diagonalCost;
    private Node[][] searchArea;
    private PriorityQueue<Node> openList;
    private List<Node> closedList;
    private Node initialNode;
    private Node finalNode;
    // #LineComplexity: 1
    public AStar(int rows, int cols, Node initialNode, Node finalNode, int hvCost, int diagonalCost) {
        this.hvCost = hvCost;
        this.diagonalCost = diagonalCost;
        setInitialNode(initialNode);
        setFinalNode(finalNode);
        this.searchArea = new Node[rows][cols];
        this.openList = new PriorityQueue<Node>(new Comparator<Node>() {
            @Override
            public int compare(Node node0, Node node1) {
                return node0.getF() < node1.getF() ? -1 : node0.getF() > node1.getF() ? 1 : 0;
            }
        });
        setNodes();
        this.closedList = new ArrayList<Node>();
    }
    // #LineComplexity: 1
    public AStar(int rows, int cols, Node initialNode, Node finalNode) {
        this(rows, cols, initialNode, finalNode, DEFAULT_HV_COST, DEFAULT_DIAGONAL_COST);
    }
    // #LineComplexity: 3
    private void setNodes() {
        for (int i = 0; i < searchArea.length; i++) {
            for (int j = 0; j < searchArea[0].length; j++) {
                Node node = new Node(i, j);
                node.calculateHeuristic(getFinalNode());
                this.searchArea[i][j] = node;
            }
        }
    }
    // #LineComplexity: 2
    public void setBlocks(int[][] blocksArray) {
        for (int i = 0; i < blocksArray.length; i++) {
            int row = blocksArray[i][0];
            int col = blocksArray[i][1];
            setBlock(row, col);
        }
    }
    // #LineComplexity: 3
    public List<Node> findPath() {
        openList.add(initialNode);
        while (!isEmpty(openList)) {
            Node currentNode = openList.poll();
            closedList.add(currentNode);
            if (isFinalNode(currentNode)) {
                return getPath(currentNode);
            } else {
                addAdjacentNodes(currentNode);
            }
        }
        return new ArrayList<Node>();
    }
    // #LineComplexity: 2
    private List<Node> getPath(Node currentNode) {
        List<Node> path = new ArrayList<Node>();
        path.add(currentNode);
        Node parent;
        while ((parent = currentNode.getParent()) != null) {
            path.add(0, parent);
            currentNode = parent;
        }
        return path;
    }
    // #LineComplexity: 1
    private void addAdjacentNodes(Node currentNode) {
        addAdjacentUpperRow(currentNode);
        addAdjacentMiddleRow(currentNode);
        addAdjacentLowerRow(currentNode);
    }
    // #LineComplexity: 4
    private void addAdjacentLowerRow(Node currentNode) {
        int row = currentNode.getRow();
        int col = currentNode.getCol();
        int lowerRow = row + 1;
        if (lowerRow < getSearchArea().length) {
            if (col - 1 >= 0) {
                checkNode(currentNode, col - 1, lowerRow, getDiagonalCost()); // Comment this line if diagonal movements are not allowed
            }
            if (col + 1 < getSearchArea()[0].length) {
                checkNode(currentNode, col + 1, lowerRow, getDiagonalCost()); // Comment this line if diagonal movements are not allowed
            }
            checkNode(currentNode, col, lowerRow, getHvCost());
        }
    }
    // #LineComplexity: 3
    private void addAdjacentMiddleRow(Node currentNode) {
        int row = currentNode.getRow();
        int col = currentNode.getCol();
        int middleRow = row;
        if (col - 1 >= 0) {
            checkNode(currentNode, col - 1, middleRow, getHvCost());
        }
        if (col + 1 < getSearchArea()[0].length) {
            checkNode(currentNode, col + 1, middleRow, getHvCost());
        }
    }
    // #LineComplexity: 4
    private void addAdjacentUpperRow(Node currentNode) {
        int row = currentNode.getRow();
        int col = currentNode.getCol();
        int upperRow = row - 1;
        if (upperRow >= 0) {
            if (col - 1 >= 0) {
                checkNode(currentNode, col - 1, upperRow, getDiagonalCost()); // Comment this if diagonal movements are not allowed
            }
            if (col + 1 < getSearchArea()[0].length) {
                checkNode(currentNode, col + 1, upperRow, getDiagonalCost()); // Comment this if diagonal movements are not allowed
            }
            checkNode(currentNode, col, upperRow, getHvCost());
        }
    }
    // #LineComplexity: 5
    private void checkNode(Node currentNode, int col, int row, int cost) {
        Node adjacentNode = getSearchArea()[row][col];
        if (!adjacentNode.isBlock() && !getClosedList().contains(adjacentNode)) {
            if (!getOpenList().contains(adjacentNode)) {
                adjacentNode.setNodeData(currentNode, cost);
                getOpenList().add(adjacentNode);
            } else {
                boolean changed = adjacentNode.checkBetterPath(currentNode, cost);
                if (changed) {
                    // Remove and Add the changed node, so that the PriorityQueue can sort again its
                    // contents with the modified "finalCost" value of the modified node
                    getOpenList().remove(adjacentNode);
                    getOpenList().add(adjacentNode);
                }
            }
        }
    }
    // #LineComplexity: 1
    private boolean isFinalNode(Node currentNode) {
        return currentNode.equals(finalNode);
    }
    // #LineComplexity: 1
    private boolean isEmpty(PriorityQueue<Node> openList) {
        return openList.size() == 0;
    }
    // #LineComplexity: 1
    private void setBlock(int row, int col) {
        this.searchArea[row][col].setBlock(true);
    }
    // #LineComplexity: 1
    public Node getInitialNode() {
        return initialNode;
    }
    // #LineComplexity: 1
    public void setInitialNode(Node initialNode) {
        this.initialNode = initialNode;
    }
    // #LineComplexity: 1
    public Node getFinalNode() {
        return finalNode;
    }
    // #LineComplexity: 1
    public void setFinalNode(Node finalNode) {
        this.finalNode = finalNode;
    }
    // #LineComplexity: 1
    public Node[][] getSearchArea() {
        return searchArea;
    }
    // #LineComplexity: 1
    public void setSearchArea(Node[][] searchArea) {
        this.searchArea = searchArea;
    }
    // #LineComplexity: 1
    public PriorityQueue<Node> getOpenList() {
        return openList;
    }
    // #LineComplexity: 1
    public void setOpenList(PriorityQueue<Node> openList) {
        this.openList = openList;
    }
    // #LineComplexity: 1
    public List<Node> getClosedList() {
        return closedList;
    }
    // #LineComplexity: 1
    public void setClosedList(List<Node> closedList) {
        this.closedList = closedList;
    }
    // #LineComplexity: 1
    public int getHvCost() {
        return hvCost;
    }
    // #LineComplexity: 1
    public void setHvCost(int hvCost) {
        this.hvCost = hvCost;
    }
    // #LineComplexity: 1
    private int getDiagonalCost() {
        return diagonalCost;
    }
    // #LineComplexity: 1
    private void setDiagonalCost(int diagonalCost) {
        this.diagonalCost = diagonalCost;
    }
}
