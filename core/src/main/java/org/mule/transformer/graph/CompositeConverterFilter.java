/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
