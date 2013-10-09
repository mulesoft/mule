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

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

/**
 * Filters a list of {@link Converter} returning a new list containing only
 * the converters with the lower name using lexical order.
 */
public class NameConverterFilter implements ConverterFilter
{

    @Override
    public List<Converter> filter(List<Converter> converters, DataType<?> source, DataType<?> result)
    {
        if (converters.size() == 0)
        {
            return Collections.emptyList();
        }

        List<Converter> finteredConverters = new LinkedList<Converter>(converters);
        sortConvertersByName(finteredConverters);

        String firstConverterName= finteredConverters.get(0).getName();
        int index = 1;
        for (; index < finteredConverters.size(); index++)
        {
            if (!firstConverterName.equals(finteredConverters.get(index).getName()))
            {
                break;
            }
        }

        return finteredConverters.subList(0, index);
    }

    private void sortConvertersByName(List<Converter> converterss)
    {
        Collections.sort(converterss, new Comparator<Converter>()
        {
            public int compare(Converter converter, Converter converter1)
            {
                return converter.getName().compareTo(converter1.getName());
            }
        });
    }
}
