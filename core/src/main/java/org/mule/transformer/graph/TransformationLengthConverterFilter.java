/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transformer.graph;

import org.mule.api.transformer.Converter;
import org.mule.api.transformer.DataType;
import org.mule.api.transformer.Transformer;
import org.mule.transformer.CompositeConverter;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Filters a list of {@link Converter} returning a new list containing only the
 * converters with the lower transformation path length.
 */
public class TransformationLengthConverterFilter implements ConverterFilter
{

    @Override
    public List<Converter> filter(List<Converter> converters, DataType<?> source, DataType<?> result)
    {
        if (converters.size() ==0)
        {
            return Collections.emptyList();
        }

        sortByTransformationLength(converters);

        int shortestLength = getTransformationLength(converters.get(0));
        int index = 1;
        for (; index < converters.size(); index++)
        {
            if (getTransformationLength(converters.get(index)) > shortestLength)
            {
                break;
            }
        }

        return converters.subList(0, index);
    }

    private void sortByTransformationLength(List<Converter> converterss)
    {
        Collections.sort(converterss, new Comparator<Converter>()
        {
            public int compare(Converter converter, Converter converter1)
            {
                int length = getTransformationLength(converter);
                int length1 = getTransformationLength(converter1);

                return length - length1;
            }
        });
    }

    private int getTransformationLength(Converter converter)
    {
        if (converter instanceof CompositeConverter)
        {
            return ((CompositeConverter) converter).getConverters().size();
        }
        else
        {
            return 1;
        }
    }
}
