/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transformer;

import org.mule.api.registry.ResolverException;
import org.mule.api.registry.TransformerResolver;
import org.mule.api.transformer.DataType;
import org.mule.api.transformer.DiscoverableTransformer;
import org.mule.api.transformer.Transformer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.commons.collections.map.LRUMap;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jgrapht.Graphs;
import org.jgrapht.graph.DirectedWeightedMultigraph;
import org.jgrapht.traverse.ClosestFirstIterator;

/**
 * A transformer resolver that will find direct and non direct transformations
 * using a graph.
 */
public class GraphTransformerResolver implements TransformerResolver
{

    protected final Log logger = LogFactory.getLog(getClass());

    /**
     * Graph representing all the available transformation. Each
     * vertex represents a {@link org.mule.api.transformer.DataType} and each edge a {@link org.mule.api.transformer.Transformer}.
     */
    protected TransformationGraph graph;

    /**
     * Lock graph
     */
    private ReentrantReadWriteLock readWriteLock;

    /**
     * Least-recently used map for caching
     */
    private LRUMap lruMap;

    private Set<Transformer> registeredTransformers = new HashSet<Transformer>();

    /**
     * Creates a transformer resolver with an empty graph.
     */
    public GraphTransformerResolver()
    {
        this.graph = new TransformationGraph();
        this.readWriteLock = new ReentrantReadWriteLock();
        this.lruMap = new LRUMap();
    }

    /**
     * Find a transformer chain suitable for the specified criteria. The chain will
     * be wrapper inside a {@link CompositeTransformer}
     *
     * @param source information about the source object including the object itself
     * @param result information about the result object to transform to
     * @return A {@link CompositeTransformer}
     * @throws ResolverException
     */
    @Override
    public Transformer resolve(DataType<?> source, DataType<?> result) throws ResolverException
    {
        readWriteLock.readLock().lock();

        String cacheKey = getDataTypeSourceResultPairHash(source, result);
        if (lruMap.containsKey(cacheKey))
        {
            return (Transformer) lruMap.get(cacheKey);
        }

        try
        {
            // Checks if there is a transformer with the specified input data type
            if (!this.graph.containsVertex(source))
            {
                return null;
            }

            // Checks if there is a transformer with the specified output data type
            if (!this.graph.containsVertex(result))
            {
                return null;
            }

            Transformer transformer = findTransformer(source, result);
            lruMap.put(cacheKey, transformer);

            return transformer;
        }
        finally
        {
            readWriteLock.readLock().unlock();
        }
    }

    private Transformer findTransformer(DataType<?> source, DataType<?> result)
    {
        ClosestFirstIterator<DataType<?>, TransformationEdge> iter = new ClosestFirstIterator<DataType<?>, TransformationEdge>(graph, source);

        while (iter.hasNext())
        {
            DataType<?> currentDataType = iter.next();

            if (currentDataType.isCompatibleWith(result))
            {
                return createTransformer(iter, result);
            }
        }

        return null;
    }

    private Transformer createTransformer(ClosestFirstIterator<DataType<?>, TransformationEdge> iter, DataType<?> result)
    {
        List<TransformationEdge> edgeList = new ArrayList<TransformationEdge>();

        DataType<?> currentDataType = result;
        while (true)
        {
            TransformationEdge edge = iter.getSpanningTreeEdge(currentDataType);

            if (edge == null)
            {
                break;
            }

            edgeList.add(edge);
            currentDataType = Graphs.getOppositeVertex(graph, edge, currentDataType);
        }

        if (edgeList.size() == 1)
        {
            return edgeList.get(0).transformer;
        }
        else
        {
            Collections.reverse(edgeList);

            Transformer[] transformers = new Transformer[edgeList.size()];

            int index = 0;
            for (TransformationEdge edge : edgeList)
            {
                transformers[index++] = edge.transformer;
            }

            return new CompositeTransformer(transformers);
        }
    }

    /**
     * Callback called when a transformer is added or removed form the registry
     *
     * @param transformer    the transformer that has changed
     * @param registryAction whether the transformer was added or removed
     */
    @Override
    public void transformerChange(Transformer transformer, RegistryAction registryAction)
    {
        if (!(transformer instanceof DiscoverableTransformer))
        {
            return;
        }

        try
        {
            readWriteLock.writeLock().lock();

            if (registryAction == RegistryAction.ADDED)
            {
                addTransformer(transformer);
            }
            else if (registryAction == RegistryAction.REMOVED)
            {
                removeTransformer(transformer);

            }
        }
        finally
        {
            readWriteLock.writeLock().unlock();
        }
    }

    private void removeTransformer(Transformer transformer)
    {
        if (!registeredTransformers.contains(transformer))
        {
            if (logger.isDebugEnabled())
            {
                logger.debug("Attempt to remove an unregistered transformer: " + transformer);
            }

            return;
        }

        DataType<?> returnDataType = transformer.getReturnDataType();

        for (DataType<?> sourceDataType : transformer.getSourceDataTypes())
        {
            Set<TransformationEdge> allEdges = graph.getAllEdges(sourceDataType, returnDataType);

            for (TransformationEdge edge : allEdges)
            {

                if (edge.transformer == transformer)
                {
                    DataType<?> source = graph.getEdgeSource(edge);
                    DataType<?> target = graph.getEdgeTarget(edge);

                    graph.removeEdge(edge);

                    if (graph.inDegreeOf(source) == 0 && graph.outDegreeOf(source) == 0)
                    {
                        graph.removeVertex(source);
                    }

                    if (graph.inDegreeOf(target) == 0 && graph.outDegreeOf(target) == 0)
                    {
                        graph.removeVertex(target);
                    }
                }
            }
        }

        registeredTransformers.remove(transformer);

        // Clears the cache
        this.lruMap.clear();
    }

    private void addTransformer(Transformer transformer)
    {
        if (registeredTransformers.contains(transformer))
        {
            if (logger.isDebugEnabled())
            {
                logger.debug("Attempting to register an already registered transformer: " + transformer);
            }

            return;
        }

        DataType<?> returnDataType = transformer.getReturnDataType();
        if (!this.graph.containsVertex(returnDataType))
        {
            this.graph.addVertex(returnDataType);
        }

        for (DataType<?> sourceDataType : transformer.getSourceDataTypes())
        {
            if (!this.graph.containsVertex(sourceDataType))
            {
                this.graph.addVertex(sourceDataType);
            }

            graph.addEdge(sourceDataType, returnDataType, new TransformationEdge(transformer));
        }

        registeredTransformers.add(transformer);

        // Clears the cache
        this.lruMap.clear();
    }

    protected class TransformationEdge
    {

        private final Transformer transformer;

        public TransformationEdge(Transformer transformer)
        {
            this.transformer = transformer;
        }

        public Transformer getTransformer()
        {
            return transformer;
        }
    }

    protected class TransformationGraph extends DirectedWeightedMultigraph<DataType<?>, TransformationEdge>
    {

        public TransformationGraph()
        {
            super(TransformationEdge.class);
        }

        @Override
        public double getEdgeWeight(TransformationEdge transformationEdge)
        {
            DiscoverableTransformer transformer = (DiscoverableTransformer) transformationEdge.transformer;

            int negativeWeight = DiscoverableTransformer.MAX_PRIORITY_WEIGHTING - transformer.getPriorityWeighting();
            return negativeWeight < 0 ? 0 : negativeWeight;
        }
    }

    private String getDataTypeSourceResultPairHash(DataType<?> source, DataType<?> result)
    {
        StringBuilder builder = new StringBuilder();
        builder.append(source.getClass().getName());
        builder.append(source.hashCode());
        builder.append(":");
        builder.append(result.getClass().getName());
        builder.append(result.hashCode());

        return builder.toString();
    }
}
