/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transformer.graph;

import static org.mule.transformer.TransformerUtils.getConverterKey;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jgrapht.graph.DirectedMultigraph;
import org.mule.api.transformer.Converter;
import org.mule.api.transformer.DataType;

/**
 * Represents the set of transformations between {@link DataType} based on the
 * available {@link Converter}.
 */
public class TransformationGraph extends DirectedMultigraph<DataType<?>, TransformationEdge>
{

    protected final Log logger = LogFactory.getLog(getClass());

    protected Map<String, Converter> registeredConverters = new HashMap<String, Converter>();

    public TransformationGraph()
    {
        super(TransformationEdge.class);
    }

    public void addConverter(Converter converter)
    {
        if (registeredConverters.containsKey(getConverterKey(converter)))
        {
            if (logger.isDebugEnabled())
            {
                logger.debug("Attempting to register an already registered converter: " + converter);
            }

            return;
        }

        DataType<?> returnDataType = converter.getReturnDataType();
        if (!containsVertex(returnDataType))
        {
            addVertex(returnDataType);
        }

        for (DataType<?> sourceDataType : converter.getSourceDataTypes())
        {
            if (!containsVertex(sourceDataType))
            {
                addVertex(sourceDataType);
            }

            addEdge(sourceDataType, returnDataType, new TransformationEdge(converter));
        }

        registeredConverters.put(getConverterKey(converter), converter);
    }

    public void removeConverter(Converter converter)
    {
        if (!registeredConverters.containsKey(getConverterKey(converter)))
        {
            if (logger.isDebugEnabled())
            {
                logger.debug("Attempt to remove an unregistered converter: " + converter);
            }

            return;
        }

        DataType<?> returnDataType = converter.getReturnDataType();

        for (DataType<?> sourceDataType : converter.getSourceDataTypes())
        {
            Set<TransformationEdge> allEdges = getAllEdges(sourceDataType, returnDataType);

            for (TransformationEdge edge : allEdges)
            {

                if (edge.getConverter() == converter)
                {
                    DataType<?> source = getEdgeSource(edge);
                    DataType<?> target = getEdgeTarget(edge);

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

        registeredConverters.remove(getConverterKey(converter));
    }
}
