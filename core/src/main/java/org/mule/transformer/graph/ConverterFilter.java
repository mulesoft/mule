/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transformer.graph;

import org.mule.api.transformer.Converter;
import org.mule.api.transformer.DataType;

import java.util.List;

/**
 * Provides a way to filter a converters during the process of transformer
 * resolution for a given source and result data types.
 */
public interface ConverterFilter
{

    /**
     * Filters a list of converters
     *
     * @param converters converters to filter
     * @param source source data type being resolved
     * @param result result data type being resolved
     * @return a non null list of converters that match the specified criteria
     */
    public List<Converter> filter(List<Converter> converters, DataType<?> source, DataType<?> result);

}
