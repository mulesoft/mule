/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transformer.graph;

import static junit.framework.Assert.fail;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Set;

import org.junit.BeforeClass;
import org.junit.Test;
import org.mule.api.transformer.Converter;
import org.mule.api.transformer.DataType;
import org.mule.api.transformer.Transformer;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;
import org.mule.transformer.builder.MockConverterBuilder;

@SmallTest
public class TransformationGraphTestCase extends AbstractMuleTestCase
{

    protected static final DataType XML_DATA_TYPE = mock(DataType.class, "XML_DATA_TYPE");
    protected static final DataType JSON_DATA_TYPE = mock(DataType.class, "JSON_DATA_TYPE");
    protected static final DataType INPUT_STREAM_DATA_TYPE = mock(DataType.class, "INPUT_STREAM_DATA_TYPE");
    protected static final DataType STRING_DATA_TYPE = mock(DataType.class, "STRING_DATA_TYPE");

    protected static class XML_CLASS {}
    protected static class JSON_CLASS {}
    protected static class INPUT_STREAM_CLASS{}
    protected static class STRING_CLASS{}

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
    public void processesConverterAdded()
    {
        Converter xmlToJson = new MockConverterBuilder().from(XML_DATA_TYPE).to(JSON_DATA_TYPE).build();

        TransformationGraph graph = new TransformationGraph();
        graph.addConverter(xmlToJson);

        assertThat(graph.vertexSet(), hasSize(2));
        assertThat(graph.containsVertex(XML_DATA_TYPE), is(true));
        assertThat(graph.containsVertex(JSON_DATA_TYPE), is(true));

        assertThat(graph.edgeSet(), hasSize(1));
        assertThat(graph.containsEdge(XML_DATA_TYPE, JSON_DATA_TYPE), is(true));
        assertThat(graph.containsEdge(JSON_DATA_TYPE, XML_DATA_TYPE), is(false));
    }

    @Test
    public void ignoresConverterAddedTwice()
    {
        Converter xmlToJson = new MockConverterBuilder().from(XML_DATA_TYPE).to(JSON_DATA_TYPE).build();

        TransformationGraph graph = new TransformationGraph();
        graph.addConverter(xmlToJson);
        graph.addConverter(xmlToJson);

        assertThat(graph.vertexSet(), hasSize(2));
        assertThat(graph.containsVertex(XML_DATA_TYPE), is(true));
        assertThat(graph.containsVertex(JSON_DATA_TYPE), is(true));

        assertThat(graph.edgeSet(), hasSize(1));
        assertThat(graph.containsEdge(XML_DATA_TYPE, JSON_DATA_TYPE), is(true));
        assertThat(graph.containsEdge(JSON_DATA_TYPE, XML_DATA_TYPE), is(false));
    }

    @Test
    public void processesConverterAddedWithMultipleSourceTypes()
    {
        Converter xmlToJson = new MockConverterBuilder().from(XML_DATA_TYPE, INPUT_STREAM_DATA_TYPE).to(JSON_DATA_TYPE).build();

        TransformationGraph graph = new TransformationGraph();
        graph.addConverter(xmlToJson);

        assertThat(graph.vertexSet(), hasSize(3));
        assertThat(graph.containsVertex(XML_DATA_TYPE), is(true));
        assertThat(graph.containsVertex(JSON_DATA_TYPE), is(true));
        assertThat(graph.containsVertex(INPUT_STREAM_DATA_TYPE), is(true));

        assertThat(graph.edgeSet(), hasSize(2));
        assertThat(graph.containsEdge(XML_DATA_TYPE, JSON_DATA_TYPE), is(true));
        assertThat(graph.containsEdge(JSON_DATA_TYPE, XML_DATA_TYPE), is(false));

        assertThat(graph.containsEdge(INPUT_STREAM_DATA_TYPE, JSON_DATA_TYPE), is(true));
        assertThat(graph.containsEdge(JSON_DATA_TYPE, INPUT_STREAM_DATA_TYPE), is(false));
    }

    @Test
    public void processesConverterRemoved()
    {
        Converter xmlToJson = new MockConverterBuilder().from(XML_DATA_TYPE).to(JSON_DATA_TYPE).build();

        TransformationGraph graph = new TransformationGraph();
        graph.addConverter(xmlToJson);
        graph.removeConverter(xmlToJson);

        assertThat(graph.vertexSet(), is(empty()));
        assertThat(graph.edgeSet(), is(empty()));
    }

    @Test
    public void ignoresRemovingConverterThatWasNeverAdded()
    {
        Converter xmlToJson = new MockConverterBuilder().from(XML_DATA_TYPE).to(JSON_DATA_TYPE).weighting(1).build();
        Converter betterXmlToJson = new MockConverterBuilder().from(XML_DATA_TYPE).to(JSON_DATA_TYPE).weighting(2).build();

        TransformationGraph graph = new TransformationGraph();
        graph.addConverter(xmlToJson);
        graph.removeConverter(betterXmlToJson);

        assertThat(graph.vertexSet(), hasSize(2));
        assertThat(graph.containsVertex(XML_DATA_TYPE), is(true));
        assertThat(graph.containsVertex(JSON_DATA_TYPE), is(true));

        assertThat(graph.edgeSet(), hasSize(1));
        assertThat(graph.containsEdge(XML_DATA_TYPE, JSON_DATA_TYPE), is(true));
    }

    @Test
    public void processesConverterRemovedWithMultipleSourceTypes()
    {
        Converter xmlToJson = new MockConverterBuilder().from(XML_DATA_TYPE, INPUT_STREAM_DATA_TYPE).to(JSON_DATA_TYPE).build();

        TransformationGraph graph = new TransformationGraph();
        graph.addConverter(xmlToJson);
        graph.removeConverter(xmlToJson);

        assertThat(graph.vertexSet(), is(empty()));
        assertThat(graph.edgeSet(), is(empty()));
    }

    @Test
    public void multipleConvertersFromSameSourceToResultTypes()
    {
        Converter xmlToJson = new MockConverterBuilder().from(XML_DATA_TYPE).to(JSON_DATA_TYPE).build();
        when(xmlToJson.getName()).thenReturn("xmlToJson");
        Converter betterXmlToJson = new MockConverterBuilder().from(XML_DATA_TYPE).to(JSON_DATA_TYPE).build();
        when(betterXmlToJson.getName()).thenReturn("betterXmlToJson");

        TransformationGraph graph = new TransformationGraph();
        graph.addConverter(xmlToJson);
        graph.addConverter(betterXmlToJson);

        assertThat(graph.vertexSet(), hasSize(2));
        assertThat(graph.containsVertex(XML_DATA_TYPE), is(true));
        assertThat(graph.containsVertex(JSON_DATA_TYPE), is(true));

        assertThat(graph.edgeSet(), hasSize(2));
        assertThat(graph.containsEdge(XML_DATA_TYPE, JSON_DATA_TYPE), is(true));
        assertThat(graph.containsEdge(JSON_DATA_TYPE, XML_DATA_TYPE), is(false));

        assertContainsTransformer(graph.edgesOf(JSON_DATA_TYPE), xmlToJson);
        assertContainsTransformer(graph.edgesOf(JSON_DATA_TYPE), betterXmlToJson);
    }

    @Test
    public void removesFirstDuplicateConverterAdded()
    {
        Converter xmlToJson = new MockConverterBuilder().from(XML_DATA_TYPE).to(JSON_DATA_TYPE).build();
        when(xmlToJson.getName()).thenReturn("xmlToJson");
        Converter betterXmlToJson = new MockConverterBuilder().from(XML_DATA_TYPE).to(JSON_DATA_TYPE).build();
        when(betterXmlToJson.getName()).thenReturn("betterXmlToJson");

        TransformationGraph graph = new TransformationGraph();
        graph.addConverter(xmlToJson);
        graph.addConverter(betterXmlToJson);
        graph.removeConverter(xmlToJson);

        assertThat(graph.vertexSet(), hasSize(2));
        assertThat(graph.containsVertex(JSON_DATA_TYPE), is(true));
        assertThat(graph.containsVertex(XML_DATA_TYPE), is(true));

        Set<TransformationEdge> transformationEdges = graph.edgesOf(JSON_DATA_TYPE);
        assertThat(transformationEdges, hasSize(1));
        assertContainsTransformer(transformationEdges, betterXmlToJson);
    }

    @Test
    public void removesSecondDuplicateConverterAdded()
    {
        Converter xmlToJson = new MockConverterBuilder().from(XML_DATA_TYPE).to(JSON_DATA_TYPE).build();
        Converter betterXmlToJson = new MockConverterBuilder().from(XML_DATA_TYPE).to(JSON_DATA_TYPE).build();

        TransformationGraph graph = new TransformationGraph();
        graph.addConverter(xmlToJson);
        graph.addConverter(betterXmlToJson);
        graph.removeConverter(betterXmlToJson);

        assertThat(graph.vertexSet(), hasSize(2));
        assertThat(graph.containsVertex(JSON_DATA_TYPE), is(true));
        assertThat(graph.containsVertex(XML_DATA_TYPE), is(true));

        Set<TransformationEdge> transformationEdges = graph.edgesOf(JSON_DATA_TYPE);
        assertThat(transformationEdges, hasSize(1));
        assertContainsTransformer(transformationEdges, xmlToJson);
    }

    @Test
    public void multipleConvertersFromDifferentSourceToSameResultTypes()
    {
        Converter xmlToJson = new MockConverterBuilder().from(XML_DATA_TYPE).to(JSON_DATA_TYPE).build();
        when(xmlToJson.getName()).thenReturn("xmlToJson");
        Converter objectToJson = new MockConverterBuilder().from(INPUT_STREAM_DATA_TYPE).to(JSON_DATA_TYPE).build();
        when(objectToJson.getName()).thenReturn("objectToJson");

        TransformationGraph graph = new TransformationGraph();
        graph.addConverter(xmlToJson);
        graph.addConverter(objectToJson);

        assertThat(graph.vertexSet(), hasSize(3));
        assertThat(graph.containsVertex(XML_DATA_TYPE), is(true));
        assertThat(graph.containsVertex(JSON_DATA_TYPE), is(true));
        assertThat(graph.containsVertex(INPUT_STREAM_DATA_TYPE), is(true));

        assertThat(graph.edgeSet(), hasSize(2));
        assertThat(graph.containsEdge(XML_DATA_TYPE, JSON_DATA_TYPE), is(true));
        assertThat(graph.containsEdge(JSON_DATA_TYPE, XML_DATA_TYPE), is(false));
        assertThat(graph.containsEdge(INPUT_STREAM_DATA_TYPE, JSON_DATA_TYPE), is(true));
        assertThat(graph.containsEdge(JSON_DATA_TYPE, INPUT_STREAM_DATA_TYPE), is(false));
    }

    @Test
    public void removeFirstAddedConverterWithDifferentSourceToSameResultTypes()
    {
        Converter xmlToJson = new MockConverterBuilder().from(XML_DATA_TYPE).to(JSON_DATA_TYPE).build();
        when(xmlToJson.getName()).thenReturn("xmlToJson");
        Converter objectToJson = new MockConverterBuilder().from(INPUT_STREAM_DATA_TYPE).to(JSON_DATA_TYPE).build();
        when(objectToJson.getName()).thenReturn("objectToJson");

        TransformationGraph graph = new TransformationGraph();
        graph.addConverter(xmlToJson);
        graph.addConverter(objectToJson);
        graph.removeConverter(xmlToJson);


        assertThat(graph.vertexSet(), hasSize(2));
        assertThat(graph.containsVertex(JSON_DATA_TYPE), is(true));
        assertThat(graph.containsVertex(INPUT_STREAM_DATA_TYPE), is(true));

        assertThat(graph.edgeSet(), hasSize(1));
        assertThat(graph.containsEdge(XML_DATA_TYPE, JSON_DATA_TYPE), is(false));
        assertThat(graph.containsEdge(JSON_DATA_TYPE, XML_DATA_TYPE), is(false));
        assertThat(graph.containsEdge(INPUT_STREAM_DATA_TYPE, JSON_DATA_TYPE), is(true));
        assertThat(graph.containsEdge(JSON_DATA_TYPE, INPUT_STREAM_DATA_TYPE), is(false));
    }
    
    @Test
    public void reregisterDoesNotLeak()
    {
        Converter xmlToJson = new MockConverterBuilder().from(XML_DATA_TYPE).to(JSON_DATA_TYPE).build();
        when(xmlToJson.getName()).thenReturn("xmlToJson");
        Converter xmlToJsonCopy = new MockConverterBuilder().from(XML_DATA_TYPE).to(JSON_DATA_TYPE).build();
        when(xmlToJsonCopy.getName()).thenReturn("xmlToJson");
        
        TransformationGraph graph = new TransformationGraph();
        graph.addConverter(xmlToJson);
        graph.addConverter(xmlToJsonCopy);
        
        assertThat(graph.registeredConverters.keySet(), hasSize(1));
    }

    @Test
    public void removeSecondAddedConverterWithDifferentSourceToSameResultTypes()
    {
        Converter xmlToJson = new MockConverterBuilder().from(XML_DATA_TYPE).to(JSON_DATA_TYPE).build();
        when(xmlToJson.getName()).thenReturn("xmlToJson");
        Converter objectToJson = new MockConverterBuilder().from(INPUT_STREAM_DATA_TYPE).to(JSON_DATA_TYPE).build();
        when(objectToJson.getName()).thenReturn("objectToJson");

        TransformationGraph graph = new TransformationGraph();
        graph.addConverter(xmlToJson);
        graph.addConverter(objectToJson);
        graph.removeConverter(objectToJson);

        assertThat(graph.vertexSet(), hasSize(2));
        assertThat(graph.containsVertex(JSON_DATA_TYPE), is(true));
        assertThat(graph.containsVertex(XML_DATA_TYPE), is(true));

        assertThat(graph.edgeSet(), hasSize(1));
        assertThat(graph.containsEdge(XML_DATA_TYPE, JSON_DATA_TYPE), is(true));
        assertThat(graph.containsEdge(JSON_DATA_TYPE, XML_DATA_TYPE), is(false));
        assertThat(graph.containsEdge(INPUT_STREAM_DATA_TYPE, JSON_DATA_TYPE), is(false));
        assertThat(graph.containsEdge(JSON_DATA_TYPE, INPUT_STREAM_DATA_TYPE), is(false));
    }

    private void assertContainsTransformer(Set<TransformationEdge> transformationEdges, Transformer transformer)
    {
        for (TransformationEdge edge : transformationEdges)
        {
            if (edge.getConverter() == transformer)
            {
                return;
            }
        }

        fail(String.format("Transformation edges %s do not contain expected transformer %s", transformationEdges, transformer));
    }

}
