/*
 * (C) Copyright 2004-2017, by Marden Neubert and Contributors.
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
package org.jgrapht.traverse;

import java.io.Serializable;
import java.util.*;

import org.jgrapht.*;
import org.jgrapht.util.*;

import SCM_TA_V1.Bug;

/**
 * A topological ordering iterator for a directed acyclic graph.
 * 
 * <p>
 * A topological order is a permutation <tt>p</tt> of the vertices of a graph such that an edge
 * <tt>(i,j)</tt> implies that <tt>i</tt> appears before <tt>j</tt> in <tt>p</tt>. For more
 * information see <a href="https://en.wikipedia.org/wiki/Topological_sorting">wikipedia</a> or
 * <a href="http://mathworld.wolfram.com/TopologicalSort.html">wolfram</a>.
 *
 * <p>
 * The iterator crosses components but does not track them, it only tracks visited vertices. The
 * iterator will detect (at some point) if the graph is not a directed acyclic graph and throw a
 * {@link IllegalArgumentException}.
 * 
 * <p>
 * For this iterator to work correctly the graph must not be modified during iteration. Currently
 * there are no means to ensure that, nor to fail-fast. The results of such modifications are
 * undefined.
 *
 * @param <V> the graph vertex type
 * @param <E> the graph edge type
 *
 * @author Marden Neubert
 * @author Dimitrios Michail
 * @since December 2004
 */
public class TopologicalOrderIterator<V, E>
    extends AbstractGraphIterator<V, E> implements Serializable, Cloneable
{
    private static final String GRAPH_IS_NOT_A_DAG = "Graph is not a DAG";

    private Queue<V> queue;
    private Map<V, ModifiableInteger> inDegreeMap;
    private int remainingVertices;
    private V cur;
    private Bug b;

    /**
     * Construct a topological order iterator.
     * 
     * <p>
     * Traversal will start at one of the graph's <i>sources</i>. See the definition of source at
     * <a href="http://mathworld.wolfram.com/Source.html">
     * http://mathworld.wolfram.com/Source.html</a>. In case of partial order, tie-breaking is
     * arbitrary.
     *
     * @param graph the directed graph to be iterated
     */
    public TopologicalOrderIterator(Graph<V, E> graph)
    {
        this(graph, (Comparator<V>) null);
    }

    /**
     * Construct a topological order iterator.
     * 
     * Creates a new topological order iterator over the directed graph specified, with a
     * user-supplied queue implementation to allow customized control over tie-breaking in case of
     * partial order. Traversal will start at one of the graph's <i>sources</i>. See the definition
     * of source at <a href="http://mathworld.wolfram.com/Source.html">
     * http://mathworld.wolfram.com/Source.html</a>.
     *
     * @param graph the directed graph to be iterated.
     * @param queue queue to use for tie-break in case of partial order (e.g. a PriorityQueue can be
     *        used to break ties according to vertex priority); must be initially empty
     * @deprecated in favor of {@link #TopologicalOrderIterator(Graph, Comparator)}
     */
    @Deprecated
    public TopologicalOrderIterator(Graph<V, E> graph, Queue<V> queue)
    {
        super(graph);
        GraphTests.requireDirected(graph);

        this.queue = Objects.requireNonNull(queue, "Queue must not be null");
        if (!queue.isEmpty()) {
            throw new IllegalArgumentException("Queue must be empty");
        }

        // count in-degrees
        this.inDegreeMap = new HashMap<>();
        for (V v : graph.vertexSet()) {
            int d = 0;
            for (E e : graph.incomingEdgesOf(v)) {
                V u = Graphs.getOppositeVertex(graph, e, v);
                if (v.equals(u)) {
                    throw new IllegalArgumentException(GRAPH_IS_NOT_A_DAG);
                }
                d++;
            }
            inDegreeMap.put(v, new ModifiableInteger(d));
            if (d == 0) {
                queue.offer(v);
            }
        }

        // record vertices count
        this.remainingVertices = graph.vertexSet().size();
    }

    /**
     * Construct a topological order iterator.
     * 
     * <p>
     * Traversal will start at one of the graph's <i>sources</i>. See the definition of source at
     * <a href="http://mathworld.wolfram.com/Source.html">
     * http://mathworld.wolfram.com/Source.html</a>. In case of partial order, a comparator is used
     * to break ties.
     *
     * @param graph the directed graph to be iterated
     * @param comparator comparator in order to break ties in case of partial order
     */
    public TopologicalOrderIterator(Graph<V, E> graph, Comparator<V> comparator)
    {
        super(graph);
        GraphTests.requireDirected(graph);

        // create queue
        if (comparator == null) {
            this.queue = new LinkedList<>();
        } else {
            this.queue = new PriorityQueue<>(comparator);
        }

        // count in-degrees
        this.inDegreeMap = new HashMap<>();
        
        // shuffle vertex list each time to create the permutation, made by Vahid-- no devs in common included
        List<V> setOfVertices = new ArrayList<V>(graph.vertexSet());
        Collections.shuffle(setOfVertices);
        
        for (V v : setOfVertices) {
            int d = 0;
            for (E e : graph.incomingEdgesOf(v)) {
                V u = Graphs.getOppositeVertex(graph, e, v);
                if (v.equals(u)) {
                    throw new IllegalArgumentException(GRAPH_IS_NOT_A_DAG);
                }
                d++;
            }
            inDegreeMap.put(v, new ModifiableInteger(d));
            if (d == 0) {
                queue.offer(v);
            }
        }

        // record vertices count
        this.remainingVertices = graph.vertexSet().size();
    }

    
    public TopologicalOrderIterator(Graph<V, E> graph, Comparator<V> comparator, String type)
    {
        super(graph);
        GraphTests.requireDirected(graph);

        // create queue
        if (comparator == null) {
            this.queue = new LinkedList<>();
        } else {
            this.queue = new PriorityQueue<>(comparator);
        }

        // count in-degrees
        this.inDegreeMap = new HashMap<>();
        
        // shuffle vertex list each time to create the permutation, made by Vahid-- no devs in common included
        /*List<V> setOfVertices = new ArrayList<V>(graph.vertexSet());
        Collections.shuffle(setOfVertices);*/
        
        //shuffle vertex list each time to create the permutation, made by Vahid-- devs in common included
        List<V> setOfVertices = new ArrayList<V>(graph.vertexSet());
        List<V> requiredToBeShuffeld = new ArrayList<V>();
        List<V> noNeedToBeShuffled = new ArrayList<V>();
        for (V v : setOfVertices) {
        	b = (Bug) v;
        	if (b.flag_devInCommon == 1)
        		requiredToBeShuffeld.add(v);
        	else 
        		noNeedToBeShuffled.add(v);
        }
        Collections.shuffle(requiredToBeShuffeld);
        setOfVertices.clear();
        for (V v : requiredToBeShuffeld)
			setOfVertices.add(v);
        
        for (V v : noNeedToBeShuffled)
			setOfVertices.add(v);
        
        for (V v : setOfVertices) {
            int d = 0;
            for (E e : graph.incomingEdgesOf(v)) {
                V u = Graphs.getOppositeVertex(graph, e, v);
                if (v.equals(u)) {
                    throw new IllegalArgumentException(GRAPH_IS_NOT_A_DAG);
                }
                d++;
            }
            inDegreeMap.put(v, new ModifiableInteger(d));
            if (d == 0) {
                queue.offer(v);
            }
        }

        // record vertices count
        this.remainingVertices = graph.vertexSet().size();
    }

    
    /**
     * {@inheritDoc}
     * 
     * Always returns true since the iterator does not care about components.
     */
    @Override
    public boolean isCrossComponentTraversal()
    {
        return true;
    }

    /**
     * {@inheritDoc}
     * 
     * Trying to disable the cross components nature of this iterator will result into throwing a
     * {@link IllegalArgumentException}.
     */
    @Override
    public void setCrossComponentTraversal(boolean crossComponentTraversal)
    {
        if (!crossComponentTraversal) {
            throw new IllegalArgumentException("Iterator is always cross-component");
        }
    }

    @Override
    public boolean hasNext()
    {
        if (cur != null) {
            return true;
        }
        cur = advance();
        if (cur != null && nListeners != 0) {
            fireVertexTraversed(createVertexTraversalEvent(cur));
        }
        return cur != null;
    }

    @Override
    public V next()
    {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }

        V result = cur;
        cur = null;
        if (nListeners != 0) {
            fireVertexFinished(createVertexTraversalEvent(result));
        }
        return result;
    }

    private V advance()
    {
        V result = queue.poll();

        if (result != null) {
            for (E e : graph.outgoingEdgesOf(result)) {
                V other = Graphs.getOppositeVertex(graph, e, result);

                ModifiableInteger inDegree = inDegreeMap.get(other);
                if (inDegree.value > 0) {
                    inDegree.value--;

                    if (inDegree.value == 0) {
                        queue.offer(other);
                    }
                }
            }

            --remainingVertices;
        } else {
            /*
             * Still expecting some vertices, but no vertex has zero degree.
             */
            if (remainingVertices > 0) {
                throw new IllegalArgumentException(GRAPH_IS_NOT_A_DAG);
            }
        }

        return result;
    }
    
    //override clone method form super class
	public Object clone() throws CloneNotSupportedException{
			
			TopologicalOrderIterator<V, E> t=(TopologicalOrderIterator<V, E>) super.clone();
			return t;
			
		}

}

// End TopologicalOrderIterator.java
