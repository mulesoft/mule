/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transformer.graph;

import com.google.common.collect.ImmutableSet;
import org.mule.api.transformer.Converter;
import org.mule.api.transformer.DataType;

import java.util.Set;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class SynchronizedTransformationGraph
{
    private ReentrantReadWriteLock readWriteLock;
    private TransformationGraph transformationGraph;

    public SynchronizedTransformationGraph()
    {
        this.transformationGraph = new TransformationGraph();
        this.readWriteLock = new ReentrantReadWriteLock();
    }

    public void addConverter(Converter converter)
    {
        readWriteLock.writeLock().lock();
        try
        {
            transformationGraph.addConverter(converter);
        }
        finally
        {
            readWriteLock.writeLock().unlock();
        }
    }

    public void removeConverter(Converter converter)
    {
        readWriteLock.writeLock().lock();
        try
        {
            transformationGraph.removeConverter(converter);
        }
        finally
        {
            readWriteLock.writeLock().unlock();
        }
    }

    public Set<TransformationEdge> outgoingEdgesOf(DataType<?> vertex)
    {
        readWriteLock.readLock().lock();
        try
        {
            return ImmutableSet.copyOf(transformationGraph.outgoingEdgesOf(vertex));
        }
        finally
        {
            readWriteLock.readLock().unlock();
        }
    }

    public DataType<?> getEdgeTarget(TransformationEdge transformationEdge)
    {
        readWriteLock.readLock().lock();
        try
        {
            return transformationGraph.getEdgeTarget(transformationEdge);
        }
        finally
        {
            readWriteLock.readLock().unlock();
        }
    }

    public boolean containsVertex(DataType<?> dataType)
    {
        readWriteLock.readLock().lock();
        try
        {
            return transformationGraph.containsVertex(dataType);
        }
        finally
        {
            readWriteLock.readLock().unlock();
        }
    }

}
