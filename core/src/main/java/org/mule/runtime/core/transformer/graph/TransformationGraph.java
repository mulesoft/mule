/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.transformer.graph;

import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.core.api.transformer.Converter;

import java.util.HashSet;
import java.util.Set;

import org.jgrapht.graph.DirectedMultigraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents the set of transformations between {@link DataType} based on the
 * available {@link Converter}.
 */
public class TransformationGraph extends DirectedMultigraph<DataType, TransformationEdge>
{

    protected final Logger logger = LoggerFactory.getLogger(getClass());
    
    private Set<Converter> registeredConverters = new HashSet<>();
    
    public TransformationGraph()
    {
        super(TransformationEdge.class);
    }

    public void addConverter(Converter converter)
    {
        if (registeredConverters.contains(converter))
        {
            if (logger.isDebugEnabled())
            {
                logger.debug("Attempting to register an already registered converter: " + converter);
            }

            return;
        }

        DataType returnDataType = converter.getReturnDataType();
        if (!containsVertex(returnDataType))
        {
            addVertex(returnDataType);
        }

        for (DataType sourceDataType : converter.getSourceDataTypes())
        {
            if (!containsVertex(sourceDataType))
            {
                addVertex(sourceDataType);
            }

            addEdge(sourceDataType, returnDataType, new TransformationEdge(converter));
        }

        registeredConverters.add(converter);
    }
    
    public void removeConverter(Converter converter)
    {
        if (!registeredConverters.contains(converter))
        {
            if (logger.isDebugEnabled())
            {
                logger.debug("Attempt to remove an unregistered converter: " + converter);
            }

            return;
        }

        DataType returnDataType = converter.getReturnDataType();

        for (DataType sourceDataType : converter.getSourceDataTypes())
        {
            Set<TransformationEdge> allEdges = getAllEdges(sourceDataType, returnDataType);

            for (TransformationEdge edge : allEdges)
            {

                if (edge.getConverter() == converter)
                {
                    DataType source = getEdgeSource(edge);
                    DataType target = getEdgeTarget(edge);

                    removeEdge(edge);

                    if (inDegreeOf(source) == 0 && outDegreeOf(source) == 0)
                    {
                        removeVertex(source);
                    }

                    if (inDegreeOf(target) == 0 && outDegreeOf(target) == 0)
                    {
                        removeVertex(target);
                    }
                }
            }
        }

        registeredConverters.remove(converter);
    }
}
