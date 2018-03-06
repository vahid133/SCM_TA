/*
 * (C) Copyright 2017-2018, by Alexandru Valeanu and Contributors.
 *
 * JGraphT : a free Java graph-theory library
 *
 * This program and the accompanying materials are dual-licensed under
 * either
 *
 * (a) the terms of the GNU Lesser General Public License version 2.1
 * as published by the Free Software Foundation, or (at your option) any
 * later version.
 *
 * or (per the licensee's choosing)
 *
 * (b) the terms of the Eclipse Public License v1.0 as published by
 * the Eclipse Foundation.
 */
package org.jgrapht.alg.tour;

import org.jgrapht.Graph;
import org.jgrapht.GraphPath;
import org.jgrapht.alg.interfaces.TSPAlgorithm;
import org.jgrapht.graph.GraphWalk;

import java.util.*;

/**
 * A dynamic programming algorithm for the TSP problem.
 *
 * <p>
 * The travelling salesman problem (TSP) asks the following question: "Given a list of cities and
 * the distances between each pair of cities, what is the shortest possible route that visits each
 * city exactly once and returns to the origin city?".
 *
 * <p>
 * This is an implementation of the Held-Karp algorithm which returns a optimal, minimum-cost Hamiltonian tour.
 * The implementation requires the input graph to contain at least one vertex.
 * The running time is $O(2^{|V|} \times |V|^2)$ and it takes $O(2^{|V|} \times |V|)$ extra memory.
 *
 * <p>
 * See <a href="https://en.wikipedia.org/wiki/Travelling_salesman_problem">wikipedia</a> for more
 * details about TSP.
 *
 * <p>
 * See <a href="https://en.wikipedia.org/wiki/Held%E2%80%93Karp_algorithm">wikipedia</a> for more
 * details about the dynamic programming algorithm.
 *
 * @param <V> the graph vertex type
 * @param <E> the graph edge type
 *
 * @author Alexandru Valeanu
 */
public class HeldKarpTSP<V, E>
        implements TSPAlgorithm<V, E> {

    /**
     * Construct a new instance
     */
    public HeldKarpTSP() {
    }

    private double memo(int previousNode, int state, double[][] C, double[][] W){
        // have we seen this state before?
        if (C[previousNode][state] != Double.MIN_VALUE)
            return C[previousNode][state];

        // no cycle has been found yet
        double totalCost = Double.MAX_VALUE;

        // check if all nodes have been visited (i.e. state + 1 == 2^n)
        if (state == (1 << W.length) - 1){
            // check if there is a return edge we can use
            if (W[previousNode][0] != Double.MAX_VALUE)
                totalCost = W[previousNode][0];
            else
                totalCost = Double.MAX_VALUE;
        }
        else{
            // try to find the 'best' next (i.e. unvisited and adjacent to previousNode) node in the tour
            for (int i = 0; i < W.length; i++) {
                if (((state >> i) & 1) == 0 && W[previousNode][i] != Double.MAX_VALUE){
                    totalCost = Math.min(totalCost, W[previousNode][i] + memo(i, state ^ (1 << i), C, W));
                }
            }
        }

        return C[previousNode][state] = totalCost;
    }

    /**
     * Computes a minimum-cost Hamiltonian tour.
     *
     * @param graph the input graph
     * @return a minimum-cost tour if one exists, null otherwise
     * @throws IllegalArgumentException if the graph contains no vertices
     * @throws IllegalArgumentException if the graph contains more than 31 vertices
     */
    @Override
    public GraphPath<V, E> getTour(Graph<V, E> graph) {
        final int n = graph.vertexSet().size(); // number of nodes

        if (n == 0) {
            throw new IllegalArgumentException("Graph contains no vertices");
        }

        if (n > 31){
            throw new IllegalArgumentException("The internal representation of the dynamic programming state " +
                    "space cannot represent graphs containing more than 31 vertices. " +
                    "The runtime complexity of this implementation, O(2^|V| x |V|^2),  makes it unsuitable " +
                    "for graphs with more than 31 vertices.");
        }

        if (n == 1){
            V startNode = graph.vertexSet().iterator().next();
            return new GraphWalk<>(graph, startNode, startNode, Collections.singletonList(startNode), null, 0);
        }

        // W[u, v] = the cost of the minimum weight between u and v
        double[][] W = new double[n][n];
        for (int i = 0; i < n; i++) {
            Arrays.fill(W[i], Double.MAX_VALUE);
        }

        /*
         * Normalize the graph
         *   map each vertex to an integer (using a HashMap)
         *   keep the reverse mapping  (using an ArrayList)
         */
        Map<V, Integer> vertexMap = new HashMap<>();
        List<V> indexList = new ArrayList<>();
        for (E e: graph.edgeSet()){
            V source = graph.getEdgeSource(e);
            V target = graph.getEdgeTarget(e);

            // map 'source' if no mapping exists
            if (!vertexMap.containsKey(source)){
                vertexMap.put(source, vertexMap.size());
                indexList.add(source);
            }

            // map 'target' if no mapping exists
            if (!vertexMap.containsKey(target)){
                vertexMap.put(target, vertexMap.size());
                indexList.add(target);
            }

            int u = vertexMap.get(source);
            int v = vertexMap.get(target);

            // use Math.min in case we deal with a multigraph
            W[u][v] = Math.min(W[u][v], graph.getEdgeWeight(e));

            // If the graph is undirected we need to also consider the reverse edge
            if (graph.getType().isUndirected())
                W[v][u] = Math.min(W[v][u], graph.getEdgeWeight(e));
        }

        // C[prevNode, state] = the minimum cost of a tour that ends in prevNode and contains all
        //                      nodes in the bitmask state
        double[][] C = new double[n][1 << n];
        for (int i = 0; i < n; i++) {
            Arrays.fill(C[i], Double.MIN_VALUE);
        }

        // start the tour from node 0 (because the tour is a cycle the start vertex does not matter)
        double tourWeight = memo(0, 1, C, W);

        // check if there is no tour
        if (tourWeight == Double.MAX_VALUE)
            return null;

        /*
         * Reconstruct the tour
         */
        List<V> vertexList = new ArrayList<>(n);
        List<E> edgeList = new ArrayList<>(n);

        int lastNode = 0;
        int lastState = 1;

        vertexList.add(indexList.get(lastNode));

        for (int step = 1; step < n; step++) {
            int nextNode = -1;
            for (int node = 0; node < n; node++) {
                if (C[node][lastState ^ (1 << node)] + W[lastNode][node] == C[lastNode][lastState]){
                    nextNode = node;
                    break;
                }
            }

            assert nextNode != -1;
            vertexList.add(indexList.get(nextNode));
            edgeList.add(graph.getEdge(indexList.get(lastNode), indexList.get(nextNode)));
            lastState ^= 1 << nextNode;
            lastNode = nextNode;
        }

        // add start vertex
        vertexList.add(indexList.get(0));
        edgeList.add(graph.getEdge(indexList.get(lastNode), indexList.get(0)));

        return new GraphWalk<>(graph, indexList.get(0), indexList.get(0), vertexList, edgeList, tourWeight);
    }
}
