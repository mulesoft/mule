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
import org.mule.api.transformer.Converter;
import org.mule.api.transformer.DataType;
import org.mule.api.transformer.Transformer;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.commons.collections.map.LRUMap;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jgrapht.graph.DirectedMultigraph;

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
     * be wrapper inside a {@link CompositeConverter}
     *
     * @param source information about the source object including the object itself
     * @param result information about the result object to transform to
     * @return A {@link CompositeConverter}
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
            Transformer nearestTransformerMatch = null;

            List<List<TransformationEdge>> transformationPaths = findTransformationPaths(source, result);

            if (transformationPaths.size() != 0)
            {
                sortTransformationPathsByLength(transformationPaths);

                List<Transformer> transformers = createTransformers(getShortestTransformationPaths(transformationPaths));

                nearestTransformerMatch = getNearestTransformerMatch(transformers, source.getType(), result.getType());
            }

            // Caches the obtained value
            lruMap.put(cacheKey, nearestTransformerMatch);

            return nearestTransformerMatch;
        }
        finally
        {
            readWriteLock.readLock().unlock();
        }
    }

    private List<List<TransformationEdge>> getShortestTransformationPaths(List<List<TransformationEdge>> transformationPaths)
    {
        int shortestPathLength = transformationPaths.get(0).size();
        int index = 1;
        for (; index < transformationPaths.size(); index++)
        {
            if (transformationPaths.get(index).size() > shortestPathLength)
            {
                break;
            }
        }

        return transformationPaths.subList(0, index);
    }

    private void sortTransformationPathsByLength(List<List<TransformationEdge>> transformationPaths)
    {
        Collections.sort(transformationPaths, new Comparator<List<TransformationEdge>>()
        {
            @Override
            public int compare(List<TransformationEdge> transformationEdges, List<TransformationEdge> transformationEdges1)
            {
                return transformationEdges.size() - transformationEdges1.size();
            }
        });
    }

    public List<Transformer> lookupTransformers(DataType<?> source, DataType<?> target)
    {
        List<Transformer> transformers = new LinkedList<Transformer>();
        if (!graph.containsVertex(source))
        {
            return transformers;
        }

        // Checks if there is a transformer with the specified output data type
        if (!graph.containsVertex(target))
        {
            return transformers;
        }

        Set<DataType<?>> visited = new HashSet<DataType<?>>();

        List<List<TransformationEdge>> transformationPaths = findTransformationPaths(source, target, visited);

        transformers = createTransformers(transformationPaths);

        return transformers;
    }

    private List<Transformer> createTransformers(List<List<TransformationEdge>> transformationPaths)
    {
        List<Transformer> transformers = new LinkedList<Transformer>();

        for (List<TransformationEdge> transformationPath : transformationPaths)
        {
            Transformer[] pathTransformers = new Transformer[transformationPath.size()];

            int index = 0;
            for (TransformationEdge edge : transformationPath)
            {
                pathTransformers[index++] = edge.transformer;
            }

            Transformer transformer;
            if (transformationPath.size() == 1)
            {
                transformer = transformationPath.get(0).getTransformer();
            }
            else
            {
                transformer = new CompositeConverter(pathTransformers);
            }
            transformers.add(transformer);
        }

        return transformers;
    }

    private List<List<TransformationEdge>> findTransformationPaths(DataType<?> source, DataType<?> target)
    {
        List<List<TransformationEdge>> transformationPaths = new LinkedList<List<TransformationEdge>>();

        if (!graph.containsVertex(source))
        {
            return transformationPaths;
        }

        if (!graph.containsVertex(target))
        {
            return transformationPaths;
        }

        Set<DataType<?>> visited = new HashSet<DataType<?>>();


        transformationPaths = findTransformationPaths(source, target, visited);
        if (logger.isDebugEnabled())
        {
            logger.debug("TransformationPaths: " + transformationPaths);
        }

        return transformationPaths;
    }

    private List<List<TransformationEdge>> findTransformationPaths(DataType<?> source, DataType<?> target, Set<DataType<?>> visited)
    {
        List<List<TransformationEdge>> transformers = new LinkedList<List<TransformationEdge>>();

        if (visited.contains(source))
        {
            return transformers;
        }
        else
        {
            visited.add(source);

            Set<TransformationEdge> transformationEdges = graph.outgoingEdgesOf(source);
            for (TransformationEdge transformationEdge : transformationEdges)
            {
                DataType<?> edgeTarget = graph.getEdgeTarget(transformationEdge);

                if (edgeTarget.equals(target))
                {
                    LinkedList<TransformationEdge> transformationEdges1 = new LinkedList<TransformationEdge>();
                    transformationEdges1.add(transformationEdge);
                    transformers.add(transformationEdges1);
                }
                else
                {
                    List<List<TransformationEdge>> newTransformations = findTransformationPaths(edgeTarget, target, visited);

                    for (List<TransformationEdge> transformationEdgeList : newTransformations)
                    {
                        transformationEdgeList.add(0, transformationEdge);
                        transformers.add(transformationEdgeList);
                    }
                }
            }

            visited.remove(source);
        }


        return transformers;
    }

    protected Transformer getNearestTransformerMatch(List<Transformer> trans, Class input, Class output) throws ResolverException
    {
        if (trans.size() == 0)
        {
            return null;
        }
        else if (trans.size() == 1)
        {
            return trans.get(0);
        }
        else
        {
            if (logger.isDebugEnabled())
            {
                logger.debug("Comparing transformers for best match: input = " + input + " output = " + output + " Possible transformers = " + trans);
            }

            List<TransformerWeighting> weightings = new LinkedList<TransformerWeighting>();
            for (Transformer transformer : trans)
            {
                TransformerWeighting current = new TransformerWeighting(input, output, transformer);
                weightings.add(current);
            }
            Collections.sort(weightings);

            return weightings.get(weightings.size() - 1).getTransformer();
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
        if (!(transformer instanceof Converter))
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

        @Override
        public String toString()
        {
            return transformer.getClass().getName();
        }
    }

    protected class TransformationGraph extends DirectedMultigraph<DataType<?>, TransformationEdge>
    {

        public TransformationGraph()
        {
            super(TransformationEdge.class);
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
