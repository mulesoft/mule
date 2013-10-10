/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transformer.graph;

import org.mule.api.transformer.Converter;
import org.mule.api.transformer.DataType;

import java.util.LinkedList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class CompositeConverterFilter implements ConverterFilter
{

    protected final Log logger = LogFactory.getLog(getClass());
    private final ConverterFilter[] filters;

    public CompositeConverterFilter(ConverterFilter... filters)
    {
        this.filters = filters;
    }

    @Override
    public List<Converter> filter(List<Converter> converters, DataType<?> source, DataType<?> result)
    {
        List<Converter> filteredTransformers = new LinkedList<Converter>(converters);

        for (ConverterFilter filter : filters)
        {
            if (filteredTransformers.size() <= 1)
            {
                break;
            }

            filteredTransformers = filter.filter(filteredTransformers, source, result);
        }

        return filteredTransformers;
    }
}
