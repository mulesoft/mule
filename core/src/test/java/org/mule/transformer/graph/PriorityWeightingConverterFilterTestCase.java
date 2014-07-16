/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transformer.graph;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import org.mule.api.registry.ResolverException;
import org.mule.api.transformer.Converter;
import org.mule.api.transformer.DataType;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;
import org.mule.transformer.builder.MockConverterBuilder;

import java.util.ArrayList;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;

@SmallTest
public class PriorityWeightingConverterFilterTestCase extends AbstractMuleTestCase
{
    
    private static final DataType XML_DATA_TYPE = mock(DataType.class, "XML_DATA_TYPE");
    private static final DataType JSON_DATA_TYPE = mock(DataType.class, "JSON_DATA_TYPE");
    private static final DataType INPUT_STREAM_DATA_TYPE = mock(DataType.class, "INPUT_STREAM_DATA_TYPE");
    private static final DataType STRING_DATA_TYPE = mock(DataType.class, "STRING_DATA_TYPE");

    private static class XML_CLASS
    {

    }

    private static class JSON_CLASS
    {

    }

    private static class INPUT_STREAM_CLASS
    {

    }

    private static class STRING_CLASS
    {

    }

    private PriorityWeightingConverterFilter filter = new PriorityWeightingConverterFilter();

    @BeforeClass
    public static void setupDataTypes()
    {
        doReturn(true).when(XML_DATA_TYPE).isCompatibleWith(XML_DATA_TYPE);
        doReturn(XML_CLASS.class).when(XML_DATA_TYPE).getType();
        doReturn(true).when(JSON_DATA_TYPE).isCompatibleWith(JSON_DATA_TYPE);
        doReturn(JSON_CLASS.class).when(JSON_DATA_TYPE).getType();
        doReturn(true).when(INPUT_STREAM_DATA_TYPE).isCompatibleWith(INPUT_STREAM_DATA_TYPE);
        doReturn(INPUT_STREAM_CLASS.class).when(INPUT_STREAM_DATA_TYPE).getType();
        doReturn(true).when(STRING_DATA_TYPE).isCompatibleWith(STRING_DATA_TYPE);
        doReturn(STRING_CLASS.class).when(STRING_DATA_TYPE).getType();
    }

    @Test
    public void filtersEmptyList() throws ResolverException
    {
        List<Converter> availableConverters = new ArrayList<Converter>();

        List<Converter> converters = filter.filter(availableConverters, XML_DATA_TYPE, JSON_DATA_TYPE);

        assertEquals(0, converters.size());
    }

    @Test
    public void filtersSameWeight() throws ResolverException
    {
        Converter xmlToInputStream = new MockConverterBuilder().named("xmlToInputStream").from(XML_DATA_TYPE).to(INPUT_STREAM_DATA_TYPE).weighting(1).build();
        Converter xmlToString = new MockConverterBuilder().named("xmlToString").from(XML_DATA_TYPE).to(STRING_DATA_TYPE).weighting(1).build();

        List<Converter> availableConverters = new ArrayList<Converter>();
        availableConverters.add(xmlToInputStream);
        availableConverters.add(xmlToString);

        List<Converter> converters = filter.filter(availableConverters, XML_DATA_TYPE, JSON_DATA_TYPE);

        assertEquals(2, converters.size());
        assertTrue(converters.contains(xmlToInputStream));
        assertTrue(converters.contains(xmlToString));
    }

    @Test
    public void filtersSameLengthDifferentWeightsAddingBetterTransformerFirst() throws ResolverException
    {
        Converter xmlToInputStream = new MockConverterBuilder().named("xmlToInputStream").from(XML_DATA_TYPE).to(INPUT_STREAM_DATA_TYPE).weighting(2).build();
        Converter xmlToString = new MockConverterBuilder().named("xmlToString").from(XML_DATA_TYPE).to(STRING_DATA_TYPE).weighting(1).build();

        List<Converter> availableConverters = new ArrayList<Converter>();
        availableConverters.add(xmlToInputStream);
        availableConverters.add(xmlToString);

        List<Converter> converters = filter.filter(availableConverters, XML_DATA_TYPE, JSON_DATA_TYPE);

        assertEquals(1, converters.size());
        assertEquals(xmlToInputStream, converters.get(0));
    }

    @Test
    public void filtersSameLengthDifferentWeightsAddingBetterTransformerLast() throws ResolverException
    {
        Converter xmlToInputStream = new MockConverterBuilder().named("xmlToInputStream").from(XML_DATA_TYPE).to(INPUT_STREAM_DATA_TYPE).weighting(2).build();
        Converter xmlToString = new MockConverterBuilder().named("xmlToString").from(XML_DATA_TYPE).to(STRING_DATA_TYPE).weighting(1).build();

        List<Converter> availableConverters = new ArrayList<Converter>();
        availableConverters.add(xmlToString);
        availableConverters.add(xmlToInputStream);

        List<Converter> converters = filter.filter(availableConverters, XML_DATA_TYPE, JSON_DATA_TYPE);

        assertEquals(1, converters.size());
        assertEquals(xmlToInputStream, converters.get(0));
    }
}
