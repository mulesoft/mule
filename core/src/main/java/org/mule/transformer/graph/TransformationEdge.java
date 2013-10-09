/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transformer.graph;

import org.mule.api.transformer.Converter;

/**
 * Represents an available transformation inside a transformation graph.
 */
class TransformationEdge
{

    private final Converter converter;

    public TransformationEdge(Converter converter)
    {
        this.converter = converter;
    }

    public Converter getConverter()
    {
        return converter;
    }

    @Override
    public String toString()
    {
        return converter.getName();
    }
}
