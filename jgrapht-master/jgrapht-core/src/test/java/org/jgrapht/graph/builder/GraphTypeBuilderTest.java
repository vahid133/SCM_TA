/*
 * (C) Copyright 2018-2018, by Dimitrios Michail and Contributors.
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
package org.jgrapht.graph.builder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.jgrapht.Graph;
import org.jgrapht.graph.ClassBasedEdgeFactory;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.Pseudograph;
import org.junit.Test;

/**
 * Tests for the graph type builder.
 * 
 * @author Dimitrios Michail
 */
public class GraphTypeBuilderTest
{

    @Test
    public void testGraphTypeBuilder()
    {
        Graph<Integer,
            DefaultEdge> graph = GraphTypeBuilder
                .<Integer, DefaultEdge> directed().allowingMultipleEdges(true)
                .allowingSelfLoops(true).edgeClass(DefaultEdge.class).buildGraph();
        assertTrue(graph.getType().isDirected());
        assertTrue(graph.getType().isAllowingMultipleEdges());
        assertTrue(graph.getType().isAllowingSelfLoops());
        assertNotNull(graph.getEdgeFactory());
    }

    @Test
    public void testGraphTypeBuilderWithEdgeFactory()
    {
        Graph<Integer, DefaultWeightedEdge> graph = GraphTypeBuilder
            .directed().allowingMultipleEdges(true).allowingSelfLoops(true)
            .edgeFactory(
                new ClassBasedEdgeFactory<Integer, DefaultWeightedEdge>(DefaultWeightedEdge.class))
            .buildGraph();
        assertTrue(graph.getType().isDirected());
        assertTrue(graph.getType().isAllowingMultipleEdges());
        assertTrue(graph.getType().isAllowingSelfLoops());
        assertNotNull(graph.getEdgeFactory());
    }

    @Test
    public void testGraphTypeBuilderWithVertexClass()
    {
        Graph<Integer,
            DefaultEdge> graph = GraphTypeBuilder
                .directed().allowingMultipleEdges(true).allowingSelfLoops(true)
                .vertexClass(Integer.class).edgeClass(DefaultEdge.class).buildGraph();
        assertTrue(graph.getType().isDirected());
        assertTrue(graph.getType().isAllowingMultipleEdges());
        assertTrue(graph.getType().isAllowingSelfLoops());
        assertNotNull(graph.getEdgeFactory());
    }

    @Test
    public void testGraphTypeBuilderUndirected()
    {
        Graph<Integer,
            DefaultEdge> graph = GraphTypeBuilder
                .<Integer, DefaultEdge> undirected().allowingMultipleEdges(true)
                .allowingSelfLoops(false).edgeClass(DefaultEdge.class).buildGraph();
        assertTrue(graph.getType().isUndirected());
        assertTrue(graph.getType().isAllowingMultipleEdges());
        assertFalse(graph.getType().isAllowingSelfLoops());
        assertNotNull(graph.getEdgeFactory());
    }

    @Test
    public void testGraphTypeBuilderFromGraph()
    {
        Graph<Integer, DefaultEdge> graph = new Pseudograph<>(DefaultEdge.class);
        Graph<Integer, DefaultEdge> graph1 = GraphTypeBuilder.forGraph(graph).buildGraph();

        assertTrue(graph1.getType().isUndirected());
        assertTrue(graph1.getType().isAllowingMultipleEdges());
        assertTrue(graph1.getType().isAllowingSelfLoops());
        assertNotNull(graph1.getEdgeFactory());
        assertEquals(graph.getEdgeFactory(), graph1.getEdgeFactory());
    }

}
