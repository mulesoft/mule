/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transformer.graph;

import static junit.framework.Assert.fail;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;
import static org.mockito.Mockito.mock;

import java.util.List;

import org.junit.Test;
import org.mockito.Mockito;
import org.mule.api.transformer.Converter;
import org.mule.api.transformer.DataType;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;
import org.mule.transformer.CompositeConverter;
import org.mule.transformer.builder.MockConverterBuilder;

@SmallTest
public class TransformationGraphLookupStrategyTestCase extends AbstractMuleTestCase
{

    private static final DataType XML_DATA_TYPE = mock(DataType.class, "XML_DATA_TYPE");
    private static final DataType JSON_DATA_TYPE = mock(DataType.class, "JSON_DATA_TYPE");
    private static final DataType INPUT_STREAM_DATA_TYPE = mock(DataType.class, "INPUT_STREAM_DATA_TYPE");
    private static final DataType STRING_DATA_TYPE = mock(DataType.class, "STRING_DATA_TYPE");

    private SynchronizedTransformationGraph graph = new SynchronizedTransformationGraph();
    private TransformationGraphLookupStrategy lookupStrategyTransformation = new TransformationGraphLookupStrategy(graph);

    @Test
    public void lookupTransformersNoSourceInGraph() throws Exception
    {
        Converter inputStreamToXml = new MockConverterBuilder().from(INPUT_STREAM_DATA_TYPE).to(XML_DATA_TYPE).build();
        graph.addConverter(inputStreamToXml);

        List<Converter> converters = lookupStrategyTransformation.lookupConverters(JSON_DATA_TYPE, INPUT_STREAM_DATA_TYPE);

        assertThat(converters, is(empty()));
    }

    @Test
    public void lookupTransformersNoTargetInGraph() throws Exception
    {
        Converter inputStreamToXml = new MockConverterBuilder().from(INPUT_STREAM_DATA_TYPE).to(XML_DATA_TYPE).build();
        graph.addConverter(inputStreamToXml);

        List<Converter> converters = lookupStrategyTransformation.lookupConverters(INPUT_STREAM_DATA_TYPE, JSON_DATA_TYPE);

        assertThat(converters, is(empty()));
    }

    @Test
    public void findsDirectTransformation() throws Exception
    {
        Converter inputStreamToXml = new MockConverterBuilder().from(INPUT_STREAM_DATA_TYPE).to(XML_DATA_TYPE).build();
        graph.addConverter(inputStreamToXml);

        List<Converter> converters = lookupStrategyTransformation.lookupConverters(INPUT_STREAM_DATA_TYPE, XML_DATA_TYPE);

        assertThat(converters, hasSize(1));
        assertThat(converters.get(0), sameInstance(inputStreamToXml));
    }

    @Test
    public void findsDirectTransformationWhileChangingGraph() throws Exception
    {
        Converter inputStreamToXml = new MockConverterBuilder().from(INPUT_STREAM_DATA_TYPE).to(XML_DATA_TYPE).build();
        graph.addConverter(inputStreamToXml);

        List<Converter> converters = lookupStrategyTransformation.lookupConverters(INPUT_STREAM_DATA_TYPE, XML_DATA_TYPE);

        assertThat(converters, hasSize(1));
        assertThat(converters.get(0), is(inputStreamToXml));
    }

    @Test
    public void findsMultipleDirectTransformations() throws Exception
    {
        Converter inputStreamToXml = new MockConverterBuilder().from(INPUT_STREAM_DATA_TYPE).to(XML_DATA_TYPE).build();
        Mockito.when(inputStreamToXml.getName()).thenReturn("inputStreamToXml");
        graph.addConverter(inputStreamToXml);
        Converter betterInputStreamToXml = new MockConverterBuilder().from(INPUT_STREAM_DATA_TYPE).to(XML_DATA_TYPE).build();
        Mockito.when(betterInputStreamToXml.getName()).thenReturn("betterInputStreamToXml");
        graph.addConverter(betterInputStreamToXml);

        List<Converter> converters = lookupStrategyTransformation.lookupConverters(INPUT_STREAM_DATA_TYPE, XML_DATA_TYPE);

        assertThat(converters, hasSize(2));
        assertThat(converters, hasItem(inputStreamToXml));
        assertThat(converters, hasItem(betterInputStreamToXml));
    }

    @Test
    public void findsCompositeConverter() throws Exception
    {
        Converter inputStreamToString = new MockConverterBuilder().named("inputStreamToString").from(INPUT_STREAM_DATA_TYPE).to(STRING_DATA_TYPE).build();
        graph.addConverter(inputStreamToString);
        Converter stringToJson = new MockConverterBuilder().named("stringToJson").from(STRING_DATA_TYPE).to(JSON_DATA_TYPE).build();
        graph.addConverter(stringToJson);

        List<Converter> converters = lookupStrategyTransformation.lookupConverters(INPUT_STREAM_DATA_TYPE, JSON_DATA_TYPE);

        assertThat(converters, hasSize(1));
        assertContainsCompositeTransformer(converters, inputStreamToString, stringToJson);
    }

    @Test
    public void findsMultipleCompositeConvertersWithMultipleEdgesFromSource() throws Exception
    {
        Converter inputStreamToString = new MockConverterBuilder().named("inputStreamToString").from(INPUT_STREAM_DATA_TYPE).to(STRING_DATA_TYPE).build();
        graph.addConverter(inputStreamToString);
        Converter inputStreamToJson = new MockConverterBuilder().named("inputStreamToJson").from(INPUT_STREAM_DATA_TYPE).to(JSON_DATA_TYPE).build();
        graph.addConverter(inputStreamToJson);
        Converter jsonToXml = new MockConverterBuilder().named("jsonToXml").from(JSON_DATA_TYPE).to(XML_DATA_TYPE).build();
        graph.addConverter(jsonToXml);
        Converter jsonToString = new MockConverterBuilder().named("jsonToString").from(JSON_DATA_TYPE).to(STRING_DATA_TYPE).build();
        graph.addConverter(jsonToString);
        Converter stringToXml = new MockConverterBuilder().named("stringToXml").from(STRING_DATA_TYPE).to(XML_DATA_TYPE).build();
        graph.addConverter(stringToXml);
        Converter stringToJson = new MockConverterBuilder().named("stringToJson").from(STRING_DATA_TYPE).to(JSON_DATA_TYPE).build();
        graph.addConverter(stringToJson);

        List<Converter> converters = lookupStrategyTransformation.lookupConverters(INPUT_STREAM_DATA_TYPE, XML_DATA_TYPE);

        assertThat(converters, hasSize(4));
        assertContainsCompositeTransformer(converters, inputStreamToString, stringToXml);
        assertContainsCompositeTransformer(converters, inputStreamToJson, jsonToXml);
        assertContainsCompositeTransformer(converters, inputStreamToString, stringToJson, jsonToXml);
    }

    private void assertContainsCompositeTransformer(List<Converter> converters, Converter... composedConverters)
    {
        for (Converter converter : converters)
        {
            if (converter instanceof CompositeConverter)
            {
                CompositeConverter compositeConverter = (CompositeConverter) converter;
                if (compositeConverter.getConverters().size() != composedConverters.length)
                {
                    continue;
                }

                boolean matches = true;
                for (int i = 0; i < composedConverters.length - 1; i++)
                {
                    if (composedConverters[i] != compositeConverter.getConverters().get(i))
                    {
                        matches = false;
                        break;
                    }
                }

                if (matches)
                {
                    return;
                }
            }
        }

        fail("Converter list does not contain a composite converter with: " + composedConverters);
    }
}
