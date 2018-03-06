/*
 * (C) Copyright 2003-2018, by Barak Naveh and Contributors.
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
package org.jgrapht.graph;

import org.jgrapht.*;
import org.jgrapht.graph.builder.*;

/**
 * The default implementation of a directed weighted graph. A default directed weighted graph is a
 * non-simple directed graph in which multiple (parallel) edges between any two vertices are
 * <i>not</i> permitted, but loops are. The graph has weights on its edges.
 *
 * @param <V> the graph vertex type
 * @param <E> the graph edge type
 *
 * @see DefaultDirectedGraph
 */
public class DefaultDirectedWeightedGraph<V, E>
    extends DefaultDirectedGraph<V, E>
{
    private static final long serialVersionUID = -4867672646995721544L;

    /**
     * Creates a new directed weighted graph.
     *
     * @param edgeClass class on which to base factory for edges
     */
    public DefaultDirectedWeightedGraph(Class<? extends E> edgeClass)
    {
        this(new ClassBasedEdgeFactory<>(edgeClass));
    }

    /**
     * Creates a new directed weighted graph with the specified edge factory.
     *
     * @param ef the edge factory of the new graph.
     */
    public DefaultDirectedWeightedGraph(EdgeFactory<V, E> ef)
    {
        super(ef, true);
    }

    /**
     * Create a builder for this kind of graph.
     * 
     * @param edgeClass class on which to base factory for edges
     * @param <V> the graph vertex type
     * @param <E> the graph edge type
     * @return a builder for this kind of graph
     */
    public static <V,
        E> GraphBuilder<V, E, ? extends DefaultDirectedWeightedGraph<V, E>> createBuilder(
            Class<? extends E> edgeClass)
    {
        return new GraphBuilder<>(new DefaultDirectedWeightedGraph<>(edgeClass));
    }

    /**
     * Create a builder for this kind of graph.
     * 
     * @param ef the edge factory of the new graph
     * @param <V> the graph vertex type
     * @param <E> the graph edge type
     * @return a builder for this kind of graph
     */
    public static <V,
        E> GraphBuilder<V, E, ? extends DefaultDirectedWeightedGraph<V, E>> createBuilder(
            EdgeFactory<V, E> ef)
    {
        return new GraphBuilder<>(new DefaultDirectedWeightedGraph<>(ef));
    }
}

// End DefaultDirectedWeightedGraph.java
