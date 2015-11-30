/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transformer.graph;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.mule.api.registry.ResolverException;
import org.mule.api.transformer.Converter;
import org.mule.api.transformer.DataType;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;
import org.mule.transformer.builder.MockConverterBuilder;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

@SmallTest
public class NameTransformerFilterTestCase extends AbstractMuleTestCase
{
    private NameConverterFilter filter = new NameConverterFilter();
    private static final DataType<?> UNUSED_DATA_TYPE = null;

    @Test
    public void filtersEmptyList() throws ResolverException
    {
        List<Converter> availableConverters = new ArrayList<Converter>();

        List<Converter> converters = filter.filter(availableConverters, UNUSED_DATA_TYPE, UNUSED_DATA_TYPE);

        assertEquals(0, converters.size());
    }

    @Test
    public void filtersEqualName() throws ResolverException
    {
        Converter xmlToString = new MockConverterBuilder().named("xmlToString").build();

        List<Converter> availableConverters = new ArrayList<Converter>();
        availableConverters.add(xmlToString);
        availableConverters.add(xmlToString);

        List<Converter> transformers = filter.filter(availableConverters, UNUSED_DATA_TYPE, UNUSED_DATA_TYPE);

        assertEquals(2, transformers.size());
        assertEquals(xmlToString, transformers.get(0));
        assertEquals(xmlToString, transformers.get(1));
    }

    @Test
    public void filtersDifferentNameWithBetterTransformerFist() throws ResolverException
    {
        Converter xmlToString = new MockConverterBuilder().named("xmlToString").build();
        Converter stringToJson = new MockConverterBuilder().named("stringToJson").build();

        List<Converter> availableConverters = new ArrayList<Converter>();
        availableConverters.add(stringToJson);
        availableConverters.add(xmlToString);

        List<Converter> transformers = filter.filter(availableConverters, UNUSED_DATA_TYPE, UNUSED_DATA_TYPE);

        assertEquals(1, transformers.size());
        assertTrue(transformers.contains(stringToJson));
    }

    @Test
    public void filtersDifferentNameWithBetterTransformerLast() throws ResolverException
    {
        Converter xmlToString = new MockConverterBuilder().named("xmlToString").build();
        Converter stringToJson = new MockConverterBuilder().named("stringToJson").build();

        List<Converter> availableConverters = new ArrayList<Converter>();
        availableConverters.add(xmlToString);
        availableConverters.add(stringToJson);

        List<Converter> transformers = filter.filter(availableConverters, UNUSED_DATA_TYPE, UNUSED_DATA_TYPE);

        assertEquals(1, transformers.size());
        assertTrue(transformers.contains(stringToJson));
    }
}
