/*
 * (C) Copyright 2013-2018, by Nikolay Ognyanov and Contributors.
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
package org.jgrapht.alg.cycle;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jgrapht.Graph;
import org.jgrapht.GraphTests;
import org.jgrapht.Graphs;
import org.jgrapht.alg.interfaces.CycleBasisAlgorithm;

/**
 * Find a cycle basis of an undirected graph using a variant of Paton's algorithm.
 * 
 * <p>
 * See:<br>
 * K. Paton, An algorithm for finding a fundamental set of cycles for an undirected linear graph,
 * Comm. ACM 12 (1969), pp. 514-518.
 * 
 * <p>
 * Note that Paton's algorithm produces a fundamental cycle basis while this implementation produces
 * a <a href=
 * "https://en.wikipedia.org/wiki/Cycle_space#Fundamental_and_weakly_fundamental_bases">weakly
 * fundamental cycle basis</a>. A cycle basis is called weakly fundamental if there exists a linear
 * ordering of the cycles in a cycle basis such that each cycle includes at least one edge that is
 * not part of any previous cycle. Every fundamental cycle basis is weakly fundamental (for all
 * linear orderings) but not necessarily vice versa.
 *
 * @param <V> the graph vertex type
 * @param <E> the graph edge type
 *
 * @author Nikolay Ognyanov
 */
public class PatonCycleBase<V, E>
    implements UndirectedCycleBase<V, E>, CycleBasisAlgorithm<V, E>
{
    private Graph<V, E> graph;

    /**
     * Construct a new instance of the cycle basis algorithm.
     * 
     * @deprecated Use {@link #PatonCycleBase(Graph)} instead.
     */
    @Deprecated
    public PatonCycleBase()
    {
    }

    /**
     * Create a cycle base finder for the specified graph.
     *
     * @param graph the input graph
     * @throws IllegalArgumentException if the graph argument is <code>null</code> or the graph is
     *         not undirected
     */
    public PatonCycleBase(Graph<V, E> graph)
    {
        this.graph = GraphTests.requireUndirected(graph);
    }

    /**
     * {@inheritDoc}
     * 
     * @deprecated in favor of {@link CycleBasisAlgorithm}
     */
    @Override
    @Deprecated
    public Graph<V, E> getGraph()
    {
        return graph;
    }

    /**
     * {@inheritDoc}
     * 
     * @deprecated in favor of {@link CycleBasisAlgorithm}
     */
    @Override
    @Deprecated
    public void setGraph(Graph<V, E> graph)
    {
        this.graph = GraphTests.requireUndirected(graph);
    }

    /**
     * {@inheritDoc}
     * 
     * @throws IllegalArgumentException if the graph contains multiple edges between two vertices
     * @deprecated in favor of {@link #getCycleBasis()}
     */
    @Override
    @Deprecated
    public List<List<V>> findCycleBase()
    {
        if (graph == null) {
            throw new IllegalArgumentException("Null graph.");
        }
        if (GraphTests.hasMultipleEdges(graph)) {
            throw new IllegalArgumentException("Graphs with multiple edges not supported");
        }

        Map<V, Set<V>> used = new HashMap<>();
        Map<V, V> parent = new HashMap<>();
        ArrayDeque<V> stack = new ArrayDeque<>();
        List<List<V>> cycles = new ArrayList<>();

        for (V root : graph.vertexSet()) {
            // Loop over the connected
            // components of the graph.
            if (parent.containsKey(root)) {
                continue;
            }

            // Free some memory in case of
            // multiple connected components.
            used.clear();

            // Prepare to walk the spanning tree.
            parent.put(root, root);
            used.put(root, new HashSet<>());
            stack.push(root);

            // Do the walk. It is a BFS with
            // a LIFO instead of the usual
            // FIFO. Thus it is easier to
            // find the cycles in the tree.
            while (!stack.isEmpty()) {
                V current = stack.pop();
                Set<V> currentUsed = used.get(current);
                for (E e : graph.edgesOf(current)) {
                    V neighbor = graph.getEdgeTarget(e);
                    if (neighbor.equals(current)) {
                        neighbor = graph.getEdgeSource(e);
                    }
                    if (!used.containsKey(neighbor)) {
                        // found a new node
                        parent.put(neighbor, current);
                        Set<V> neighbourUsed = new HashSet<>();
                        neighbourUsed.add(current);
                        used.put(neighbor, neighbourUsed);
                        stack.push(neighbor);
                    } else if (neighbor.equals(current)) {
                        // found a self loop
                        List<V> cycle = new ArrayList<>();
                        cycle.add(current);
                        cycles.add(cycle);
                    } else if (!currentUsed.contains(neighbor)) {
                        // found a cycle
                        Set<V> neighbourUsed = used.get(neighbor);
                        List<V> cycle = new ArrayList<>();
                        cycle.add(neighbor);
                        cycle.add(current);
                        V p = parent.get(current);
                        while (!neighbourUsed.contains(p)) {
                            cycle.add(p);
                            p = parent.get(p);
                        }
                        cycle.add(p);
                        cycles.add(cycle);
                        neighbourUsed.add(current);
                    }
                }
            }
        }
        return cycles;
    }

    /**
     * Return an undirected cycle basis of a graph. Works only for undirected graphs which do not
     * have multiple (parallel) edges.
     * 
     * @return an undirected cycle basis
     * @throws IllegalArgumentException if the graph is not undirected
     * @throws IllegalArgumentException if the graph contains multiple edges between two vertices
     */
    @Override
    public CycleBasis<V, E> getCycleBasis()
    {
        GraphTests.requireUndirected(graph);

        if (GraphTests.hasMultipleEdges(graph)) {
            throw new IllegalArgumentException("Graphs with multiple edges not supported");
        }

        Map<V, Map<V, E>> used = new HashMap<>();
        Map<V, E> parent = new HashMap<>();
        ArrayDeque<V> stack = new ArrayDeque<>();

        Set<List<E>> cycles = new LinkedHashSet<>();
        int totalLength = 0;
        double totalWeight = 0d;

        for (V root : graph.vertexSet()) {
            // Loop over the connected
            // components of the graph.
            if (parent.containsKey(root)) {
                continue;
            }

            // Free some memory in case of
            // multiple connected components.
            used.clear();

            // Prepare to walk the spanning tree.
            parent.put(root, null);
            used.put(root, new HashMap<>());
            stack.push(root);

            // Do the walk. It is a BFS with
            // a LIFO instead of the usual
            // FIFO. Thus it is easier to
            // find the cycles in the tree.
            while (!stack.isEmpty()) {
                V current = stack.pop();
                Map<V, E> currentUsed = used.get(current);
                for (E e : graph.edgesOf(current)) {
                    V neighbor = Graphs.getOppositeVertex(graph, e, current);
                    if (!used.containsKey(neighbor)) {
                        // found a new node
                        parent.put(neighbor, e);
                        Map<V, E> neighbourUsed = new HashMap<>();
                        neighbourUsed.put(current, e);
                        used.put(neighbor, neighbourUsed);
                        stack.push(neighbor);
                    } else if (neighbor.equals(current)) {
                        // found a self loop
                        List<E> cycle = new ArrayList<>();
                        cycle.add(e);
                        totalWeight += graph.getEdgeWeight(e);
                        totalLength += 1;
                        cycles.add(cycle);
                    } else if (!currentUsed.containsKey(neighbor)) {
                        // found a cycle
                        Map<V, E> neighbourUsed = used.get(neighbor);

                        double weight = 0d;
                        List<E> cycle = new ArrayList<>();

                        cycle.add(e);
                        weight += graph.getEdgeWeight(e);

                        V v = current;
                        while (!neighbourUsed.containsKey(v)) {
                            E p = parent.get(v);
                            cycle.add(p);
                            weight += graph.getEdgeWeight(p);
                            v = Graphs.getOppositeVertex(graph, p, v);
                        }
                        E a = neighbourUsed.get(v);
                        cycle.add(a);
                        weight += graph.getEdgeWeight(a);

                        neighbourUsed.put(current, e);

                        cycles.add(cycle);
                        totalLength += cycle.size();
                        totalWeight += weight;
                    }
                }
            }
        }

        return new CycleBasisImpl<V, E>(graph, cycles, totalLength, totalWeight);
    }
}

// End PatonCycleBase.java
