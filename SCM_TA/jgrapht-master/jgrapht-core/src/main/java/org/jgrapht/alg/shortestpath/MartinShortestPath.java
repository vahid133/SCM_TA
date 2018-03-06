/*
 * (C) Copyright 2017-2018, by Dimitrios Michail and Contributors.
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
package org.jgrapht.alg.shortestpath;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.jgrapht.Graph;
import org.jgrapht.GraphPath;
import org.jgrapht.Graphs;
import org.jgrapht.graph.GraphWalk;
import org.jgrapht.util.GenericFibonacciHeap;

/**
 * Martin's algorithm for the multi-objective shortest paths problem.
 * 
 * <p>
 * Martin's label setting algorithm is a multiple objective extension of Dijkstra's algorithm, where
 * the minimum operator is replaced by a dominance test. It computes a maximal complete set of
 * efficient paths when all the cost values are non-negative.
 * 
 * <p>
 * Note that the multi-objective shortest path problem is a well-known NP-hard problem.
 * 
 * @author Dimitrios Michail
 *
 * @param <V> the vertex type
 * @param <E> the edge type
 */
public class MartinShortestPath<V, E>
    extends BaseMultiObjectiveShortestPathAlgorithm<V, E>
{
    // the edge weight function
    private final Function<E, double[]> edgeWeightFunction;
    // the number of objectives
    private final int objectives;
    // final labels for each node
    private final Map<V, LinkedList<Label>> nodeLabels;
    // temporary labels ordered lexicographically
    private final GenericFibonacciHeap<Label, Void> heap;

    /**
     * Create a new shortest path algorithm
     * 
     * @param graph the input graph
     * @param edgeWeightFunction the edge weight function
     */
    public MartinShortestPath(Graph<V, E> graph, Function<E, double[]> edgeWeightFunction)
    {
        super(graph);
        this.edgeWeightFunction =
            Objects.requireNonNull(edgeWeightFunction, "Function cannot be null");
        this.objectives = validateEdgeWeightFunction(edgeWeightFunction);
        this.nodeLabels = new HashMap<>();
        this.heap = new GenericFibonacciHeap<>(new LabelComparator());
    }

    @Override
    public List<GraphPath<V, E>> getPaths(V source, V sink)
    {
        return this.getPaths(source).getPaths(sink);
    }

    @Override
    public MultiObjectiveSingleSourcePaths<V, E> getPaths(V source)
    {
        if (!graph.containsVertex(source)) {
            throw new IllegalArgumentException(
                BaseMultiObjectiveShortestPathAlgorithm.GRAPH_MUST_CONTAIN_THE_SOURCE_VERTEX);
        }

        if (graph.vertexSet().isEmpty() || graph.edgeSet().isEmpty()) {
            return new ListMultiObjectiveSingleSourcePathsImpl<>(
                graph, source, Collections.emptyMap());
        }

        if (nodeLabels.isEmpty()) {
            runAlgorithm(source);
        }

        Map<V, List<GraphPath<V, E>>> paths = buildPaths(source);
        return new ListMultiObjectiveSingleSourcePathsImpl<>(graph, source, paths);
    }

    /**
     * Execute the main algorithm
     */
    private void runAlgorithm(V source)
    {
        Label sourceLabel = new Label(source, new double[objectives], null, null);
        for (V v : graph.vertexSet()) {
            nodeLabels.put(v, new LinkedList<>());
        }
        nodeLabels.get(source).add(sourceLabel);
        heap.insert(sourceLabel, null);

        while (!heap.isEmpty()) {
            Label curLabel = heap.removeMin().getKey();
            V v = curLabel.node;
            for (E e : graph.outgoingEdgesOf(v)) {
                V u = Graphs.getOppositeVertex(graph, e, v);
                Label newLabel =
                    new Label(u, sum(curLabel.value, edgeWeightFunction.apply(e)), curLabel, e);

                boolean isDominated = false;
                LinkedList<Label> uLabels = nodeLabels.get(u);
                ListIterator<Label> it = uLabels.listIterator();
                while (it.hasNext()) {
                    Label oldLabel = it.next();
                    if (dominates(oldLabel.value, newLabel.value)) {
                        isDominated = true;
                        break;
                    }
                    if (dominates(newLabel.value, oldLabel.value)) {
                        it.remove();
                    }
                }
                if (!isDominated) {
                    uLabels.add(newLabel);
                    heap.insert(newLabel, null);
                }
            }
        }
    }

    /**
     * Build the actual paths from the final labels of each node.
     * 
     * @param source the source vertex
     * @return the paths
     */
    private Map<V, List<GraphPath<V, E>>> buildPaths(V source)
    {
        Map<V, List<GraphPath<V, E>>> paths = new HashMap<>();
        for (V sink : graph.vertexSet()) {
            if (sink.equals(source)) {
                paths.put(sink, Arrays.asList(createEmptyPath(source, sink)));
            } else {
                paths.put(sink, nodeLabels.get(sink).stream().map(l -> {
                    double weight = 0d;
                    LinkedList<E> edgeList = new LinkedList<>();
                    Label cur = l;
                    while (cur != null && cur.fromPrevious != null) {
                        weight += graph.getEdgeWeight(cur.fromPrevious);
                        edgeList.push(cur.fromPrevious);
                        cur = cur.previous;
                    }
                    return new GraphWalk<>(graph, source, sink, edgeList, weight);
                }).collect(Collectors.toList()));
            }
        }
        return paths;
    }

    /**
     * Compute the sum of two vectors
     * 
     * @param a the first vector
     * @param b the second vector
     * @return the sum
     */
    private static double[] sum(double[] a, double b[])
    {
        int d = a.length;
        double[] res = new double[d];
        for (int i = 0; i < d; i++) {
            res[i] = a[i] + b[i];
        }
        return res;
    }

    /**
     * Return whether a vector dominates another.
     * 
     * @param a the first vector
     * @param b the second vector
     * @return true if the first vector dominates the second
     */
    private static boolean dominates(double[] a, double[] b)
    {
        boolean strict = false;
        int d = a.length;
        for (int i = 0; i < d; i++) {
            if (a[i] > b[i]) {
                return false;
            }
            if (a[i] < b[i]) {
                strict = true;
            }
        }
        return strict;
    }

    /**
     * Check the validity of the edge weight function
     * 
     * @param edgeWeightFunction the edge weight function
     * @return the number of dimensions
     */
    private int validateEdgeWeightFunction(Function<E, double[]> edgeWeightFunction)
    {
        int dim = 0;
        for (E e : graph.edgeSet()) {
            double[] f = edgeWeightFunction.apply(e);
            if (f == null) {
                throw new IllegalArgumentException("Invalid edge weight function");
            }
            if (dim == 0) {
                dim = f.length;
            } else {
                if (dim != f.length) {
                    throw new IllegalArgumentException("Invalid edge weight function");
                }
            }
            for (int i = 0; i < dim; i++) {
                if (Double.compare(f[i], 0d) < 0) {
                    throw new IllegalArgumentException("Edge weight must be non-negative");
                }
            }
        }
        return dim;
    }

    /**
     * A node label.
     */
    private class Label
    {
        public V node;
        public double[] value;
        public Label previous;
        public E fromPrevious;

        public Label(V node, double[] value, Label previous, E fromPrevious)
        {
            this.node = node;
            this.value = value;
            this.previous = previous;
            this.fromPrevious = fromPrevious;
        }

        @Override
        public String toString()
        {
            return "Label [node=" + node + ", value=" + Arrays.toString(value) + ", fromPrevious="
                + fromPrevious + "]";
        }

    }

    /**
     * Lexicographic comparator of two node labels.
     */
    private class LabelComparator
        implements Comparator<Label>
    {

        @Override
        public int compare(Label o1, Label o2)
        {
            for (int i = 0; i < objectives; i++) {
                if (o1.value[i] < o2.value[i]) {
                    return -1;
                } else if (o1.value[i] > o2.value[i]) {
                    return 1;
                }
            }
            return 0;
        }

    }

}
