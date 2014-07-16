/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transformer.graph;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import org.mule.api.transformer.Converter;
import org.mule.api.transformer.DataType;
import org.mule.api.transformer.Transformer;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;
import org.mule.transformer.builder.MockConverterBuilder;

import java.util.Set;

import org.junit.BeforeClass;
import org.junit.Test;

@SmallTest
public class TransformationGraphTestCase extends AbstractMuleTestCase
{

    private static final DataType XML_DATA_TYPE = mock(DataType.class, "XML_DATA_TYPE");
    private static final DataType JSON_DATA_TYPE = mock(DataType.class, "JSON_DATA_TYPE");
    private static final DataType INPUT_STREAM_DATA_TYPE = mock(DataType.class, "INPUT_STREAM_DATA_TYPE");
    private static final DataType STRING_DATA_TYPE = mock(DataType.class, "STRING_DATA_TYPE");

    private static class XML_CLASS {}
    private static class JSON_CLASS {}
    private static class INPUT_STREAM_CLASS{}
    private static class STRING_CLASS{}

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

        assertEquals(2, graph.vertexSet().size());
        assertTrue(graph.containsVertex(XML_DATA_TYPE));
        assertTrue(graph.containsVertex(JSON_DATA_TYPE));

        assertEquals(1, graph.edgeSet().size());
        assertTrue(graph.containsEdge(XML_DATA_TYPE, JSON_DATA_TYPE));
        assertFalse(graph.containsEdge(JSON_DATA_TYPE, XML_DATA_TYPE));
    }

    @Test
    public void ignoresConverterAddedTwice()
    {
        Converter xmlToJson = new MockConverterBuilder().from(XML_DATA_TYPE).to(JSON_DATA_TYPE).build();

        TransformationGraph graph = new TransformationGraph();
        graph.addConverter(xmlToJson);
        graph.addConverter(xmlToJson);

        assertEquals(2, graph.vertexSet().size());
        assertTrue(graph.containsVertex(XML_DATA_TYPE));
        assertTrue(graph.containsVertex(JSON_DATA_TYPE));

        assertEquals(1, graph.edgeSet().size());
        assertTrue(graph.containsEdge(XML_DATA_TYPE, JSON_DATA_TYPE));
        assertFalse(graph.containsEdge(JSON_DATA_TYPE, XML_DATA_TYPE));
    }

    @Test
    public void processesConverterAddedWithMultipleSourceTypes()
    {
        Converter xmlToJson = new MockConverterBuilder().from(XML_DATA_TYPE, INPUT_STREAM_DATA_TYPE).to(JSON_DATA_TYPE).build();

        TransformationGraph graph = new TransformationGraph();
        graph.addConverter(xmlToJson);

        assertEquals(3, graph.vertexSet().size());
        assertTrue(graph.containsVertex(XML_DATA_TYPE));
        assertTrue(graph.containsVertex(JSON_DATA_TYPE));
        assertTrue(graph.containsVertex(INPUT_STREAM_DATA_TYPE));

        assertEquals(2, graph.edgeSet().size());
        assertTrue(graph.containsEdge(XML_DATA_TYPE, JSON_DATA_TYPE));
        assertFalse(graph.containsEdge(JSON_DATA_TYPE, XML_DATA_TYPE));

        assertTrue(graph.containsEdge(INPUT_STREAM_DATA_TYPE, JSON_DATA_TYPE));
        assertFalse(graph.containsEdge(JSON_DATA_TYPE, INPUT_STREAM_DATA_TYPE));
    }

    @Test
    public void processesConverterRemoved()
    {
        Converter xmlToJson = new MockConverterBuilder().from(XML_DATA_TYPE).to(JSON_DATA_TYPE).build();

        TransformationGraph graph = new TransformationGraph();
        graph.addConverter(xmlToJson);
        graph.removeConverter(xmlToJson);

        assertEquals(0, graph.vertexSet().size());
        assertEquals(0, graph.edgeSet().size());
    }

    @Test
    public void ignoresRemovingConverterThatWasNeverAdded()
    {
        Converter xmlToJson = new MockConverterBuilder().from(XML_DATA_TYPE).to(JSON_DATA_TYPE).weighting(1).build();
        Converter betterXmlToJson = new MockConverterBuilder().from(XML_DATA_TYPE).to(JSON_DATA_TYPE).weighting(2).build();

        TransformationGraph graph = new TransformationGraph();
        graph.addConverter(xmlToJson);
        graph.removeConverter(betterXmlToJson);

        assertEquals(2, graph.vertexSet().size());
        assertTrue(graph.containsVertex(XML_DATA_TYPE));
        assertTrue(graph.containsVertex(JSON_DATA_TYPE));

        assertEquals(1, graph.edgeSet().size());
        assertTrue(graph.containsEdge(XML_DATA_TYPE, JSON_DATA_TYPE));
    }

    @Test
    public void processesConverterRemovedWithMultipleSourceTypes()
    {
        Converter xmlToJson = new MockConverterBuilder().from(XML_DATA_TYPE, INPUT_STREAM_DATA_TYPE).to(JSON_DATA_TYPE).build();

        TransformationGraph graph = new TransformationGraph();
        graph.addConverter(xmlToJson);
        graph.removeConverter(xmlToJson);

        assertEquals(0, graph.vertexSet().size());
        assertEquals(0, graph.edgeSet().size());
    }

    @Test
    public void multipleConvertersFromSameSourceToResultTypes()
    {
        Converter xmlToJson = new MockConverterBuilder().from(XML_DATA_TYPE).to(JSON_DATA_TYPE).build();
        Converter betterXmlToJson = new MockConverterBuilder().from(XML_DATA_TYPE).to(JSON_DATA_TYPE).build();

        TransformationGraph graph = new TransformationGraph();
        graph.addConverter(xmlToJson);
        graph.addConverter(betterXmlToJson);

        assertEquals(2, graph.vertexSet().size());
        assertTrue(graph.containsVertex(XML_DATA_TYPE));
        assertTrue(graph.containsVertex(JSON_DATA_TYPE));

        assertEquals(2, graph.edgeSet().size());
        assertTrue(graph.containsEdge(XML_DATA_TYPE, JSON_DATA_TYPE));
        assertFalse(graph.containsEdge(JSON_DATA_TYPE, XML_DATA_TYPE));

        assertContainsTransformer(graph.edgesOf(JSON_DATA_TYPE), xmlToJson);
        assertContainsTransformer(graph.edgesOf(JSON_DATA_TYPE), betterXmlToJson);
    }

    @Test
    public void removesFirstDuplicateConverterAdded()
    {
        Converter xmlToJson = new MockConverterBuilder().from(XML_DATA_TYPE).to(JSON_DATA_TYPE).build();
        Converter betterXmlToJson = new MockConverterBuilder().from(XML_DATA_TYPE).to(JSON_DATA_TYPE).build();

        TransformationGraph graph = new TransformationGraph();
        graph.addConverter(xmlToJson);
        graph.addConverter(betterXmlToJson);
        graph.removeConverter(xmlToJson);

        assertEquals(2, graph.vertexSet().size());
        assertTrue(graph.containsVertex(JSON_DATA_TYPE));
        assertTrue(graph.containsVertex(XML_DATA_TYPE));

        Set<TransformationEdge> transformationEdges = graph.edgesOf(JSON_DATA_TYPE);
        assertEquals(1, transformationEdges.size());
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

        assertEquals(2, graph.vertexSet().size());
        assertTrue(graph.containsVertex(JSON_DATA_TYPE));
        assertTrue(graph.containsVertex(XML_DATA_TYPE));

        Set<TransformationEdge> transformationEdges = graph.edgesOf(JSON_DATA_TYPE);
        assertEquals(1, transformationEdges.size());
        assertContainsTransformer(transformationEdges, xmlToJson);
    }

    @Test
    public void multipleConvertersFromDifferentSourceToSameResultTypes()
    {
        Converter xmlToJson = new MockConverterBuilder().from(XML_DATA_TYPE).to(JSON_DATA_TYPE).build();
        Converter objectToJson = new MockConverterBuilder().from(INPUT_STREAM_DATA_TYPE).to(JSON_DATA_TYPE).build();

        TransformationGraph graph = new TransformationGraph();
        graph.addConverter(xmlToJson);
        graph.addConverter(objectToJson);

        assertEquals(3, graph.vertexSet().size());
        assertTrue(graph.containsVertex(XML_DATA_TYPE));
        assertTrue(graph.containsVertex(JSON_DATA_TYPE));
        assertTrue(graph.containsVertex(INPUT_STREAM_DATA_TYPE));

        assertEquals(2, graph.edgeSet().size());
        assertTrue(graph.containsEdge(XML_DATA_TYPE, JSON_DATA_TYPE));
        assertFalse(graph.containsEdge(JSON_DATA_TYPE, XML_DATA_TYPE));
        assertTrue(graph.containsEdge(INPUT_STREAM_DATA_TYPE, JSON_DATA_TYPE));
        assertFalse(graph.containsEdge(JSON_DATA_TYPE, INPUT_STREAM_DATA_TYPE));
    }

    @Test
    public void removeFirstAddedConverterWithDifferentSourceToSameResultTypes()
    {
        Converter xmlToJson = new MockConverterBuilder().from(XML_DATA_TYPE).to(JSON_DATA_TYPE).build();
        Converter objectToJson = new MockConverterBuilder().from(INPUT_STREAM_DATA_TYPE).to(JSON_DATA_TYPE).build();

        TransformationGraph graph = new TransformationGraph();
        graph.addConverter(xmlToJson);
        graph.addConverter(objectToJson);
        graph.removeConverter(xmlToJson);


        assertEquals(2, graph.vertexSet().size());
        assertTrue(graph.containsVertex(JSON_DATA_TYPE));
        assertTrue(graph.containsVertex(INPUT_STREAM_DATA_TYPE));

        assertEquals(1, graph.edgeSet().size());
        assertFalse(graph.containsEdge(XML_DATA_TYPE, JSON_DATA_TYPE));
        assertFalse(graph.containsEdge(JSON_DATA_TYPE, XML_DATA_TYPE));
        assertTrue(graph.containsEdge(INPUT_STREAM_DATA_TYPE, JSON_DATA_TYPE));
        assertFalse(graph.containsEdge(JSON_DATA_TYPE, INPUT_STREAM_DATA_TYPE));
    }

    @Test
    public void removeSecondAddedConverterWithDifferentSourceToSameResultTypes()
    {
        Converter xmlToJson = new MockConverterBuilder().from(XML_DATA_TYPE).to(JSON_DATA_TYPE).build();
        Converter objectToJson = new MockConverterBuilder().from(INPUT_STREAM_DATA_TYPE).to(JSON_DATA_TYPE).build();

        TransformationGraph graph = new TransformationGraph();
        graph.addConverter(xmlToJson);
        graph.addConverter(objectToJson);
        graph.removeConverter(objectToJson);

        assertEquals(2, graph.vertexSet().size());
        assertTrue(graph.containsVertex(JSON_DATA_TYPE));
        assertTrue(graph.containsVertex(XML_DATA_TYPE));

        assertEquals(1, graph.edgeSet().size());
        assertTrue(graph.containsEdge(XML_DATA_TYPE, JSON_DATA_TYPE));
        assertFalse(graph.containsEdge(JSON_DATA_TYPE, XML_DATA_TYPE));
        assertFalse(graph.containsEdge(INPUT_STREAM_DATA_TYPE, JSON_DATA_TYPE));
        assertFalse(graph.containsEdge(JSON_DATA_TYPE, INPUT_STREAM_DATA_TYPE));
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
