/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transformer.graph;

import org.mule.api.transformer.Converter;
import org.mule.api.transformer.DataType;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jgrapht.graph.DirectedMultigraph;

/**
 * Represents the set of transformations between {@link DataType} based on the
 * available {@link Converter}.
 */
public class TransformationGraph extends DirectedMultigraph<DataType<?>, TransformationEdge>
{

    protected final Log logger = LogFactory.getLog(getClass());
    
    private Set<Converter> registeredConverters = new HashSet<Converter>();
    
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

        registeredConverters.remove(converter);
    }
}
