/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transformer;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import org.mule.api.registry.ResolverException;
import org.mule.api.registry.TransformerResolver;
import org.mule.api.transformer.DataType;
import org.mule.api.transformer.Transformer;
import org.mule.tck.size.SmallTest;
import org.mule.transformer.builder.MockConverterBuilder;
import org.mule.transformer.builder.MockTransformerBuilder;

import java.util.List;
import java.util.Set;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

@SmallTest
public class GraphTransformerResolverTestCase
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
    public void ignoresTransformerAdded()
    {
        Transformer xmlToJson = new MockTransformerBuilder().from(XML_DATA_TYPE).to(JSON_DATA_TYPE).build();

        GraphTransformerResolver graphResolver = new GraphTransformerResolver();
        graphResolver.transformerChange(xmlToJson, TransformerResolver.RegistryAction.ADDED);

        assertEquals(0, graphResolver.graph.vertexSet().size());
        assertEquals(0, graphResolver.graph.edgeSet().size());
    }

    @Test
    public void ignoresTransformerRemoved()
    {
        Transformer xmlToJson = new MockTransformerBuilder().from(XML_DATA_TYPE).to(JSON_DATA_TYPE).build();

        GraphTransformerResolver graphResolver = new GraphTransformerResolver();
        graphResolver.transformerChange(xmlToJson, TransformerResolver.RegistryAction.ADDED);
        graphResolver.transformerChange(xmlToJson, TransformerResolver.RegistryAction.REMOVED);

        assertEquals(0, graphResolver.graph.vertexSet().size());
        assertEquals(0, graphResolver.graph.edgeSet().size());
    }

    @Test
    public void processesConverterAdded()
    {
        Transformer xmlToJson = new MockConverterBuilder().from(XML_DATA_TYPE).to(JSON_DATA_TYPE).build();

        GraphTransformerResolver graphResolver = new GraphTransformerResolver();
        graphResolver.transformerChange(xmlToJson, TransformerResolver.RegistryAction.ADDED);

        assertEquals(2, graphResolver.graph.vertexSet().size());
        assertTrue(graphResolver.graph.containsVertex(XML_DATA_TYPE));
        assertTrue(graphResolver.graph.containsVertex(JSON_DATA_TYPE));

        assertEquals(1, graphResolver.graph.edgeSet().size());
        assertTrue(graphResolver.graph.containsEdge(XML_DATA_TYPE, JSON_DATA_TYPE));
        assertFalse(graphResolver.graph.containsEdge(JSON_DATA_TYPE, XML_DATA_TYPE));
    }

    @Test
    public void ignoresConverterAddedTwice()
    {
        Transformer xmlToJson = new MockConverterBuilder().from(XML_DATA_TYPE).to(JSON_DATA_TYPE).build();

        GraphTransformerResolver graphResolver = new GraphTransformerResolver();
        graphResolver.transformerChange(xmlToJson, TransformerResolver.RegistryAction.ADDED);
        graphResolver.transformerChange(xmlToJson, TransformerResolver.RegistryAction.ADDED);

        assertEquals(2, graphResolver.graph.vertexSet().size());
        assertTrue(graphResolver.graph.containsVertex(XML_DATA_TYPE));
        assertTrue(graphResolver.graph.containsVertex(JSON_DATA_TYPE));

        assertEquals(1, graphResolver.graph.edgeSet().size());
        assertTrue(graphResolver.graph.containsEdge(XML_DATA_TYPE, JSON_DATA_TYPE));
        assertFalse(graphResolver.graph.containsEdge(JSON_DATA_TYPE, XML_DATA_TYPE));
    }

    @Test
    public void processesConverterAddedWithMultipleSourceTypes()
    {
        Transformer xmlToJson = new MockConverterBuilder().from(XML_DATA_TYPE, INPUT_STREAM_DATA_TYPE).to(JSON_DATA_TYPE).build();

        GraphTransformerResolver graphResolver = new GraphTransformerResolver();
        graphResolver.transformerChange(xmlToJson, TransformerResolver.RegistryAction.ADDED);

        assertEquals(3, graphResolver.graph.vertexSet().size());
        assertTrue(graphResolver.graph.containsVertex(XML_DATA_TYPE));
        assertTrue(graphResolver.graph.containsVertex(JSON_DATA_TYPE));
        assertTrue(graphResolver.graph.containsVertex(INPUT_STREAM_DATA_TYPE));

        assertEquals(2, graphResolver.graph.edgeSet().size());
        assertTrue(graphResolver.graph.containsEdge(XML_DATA_TYPE, JSON_DATA_TYPE));
        assertFalse(graphResolver.graph.containsEdge(JSON_DATA_TYPE, XML_DATA_TYPE));

        assertTrue(graphResolver.graph.containsEdge(INPUT_STREAM_DATA_TYPE, JSON_DATA_TYPE));
        assertFalse(graphResolver.graph.containsEdge(JSON_DATA_TYPE, INPUT_STREAM_DATA_TYPE));
    }

    @Test
    public void processesConverterRemoved()
    {
        Transformer xmlToJson = new MockConverterBuilder().from(XML_DATA_TYPE).to(JSON_DATA_TYPE).build();

        GraphTransformerResolver graphResolver = new GraphTransformerResolver();
        graphResolver.transformerChange(xmlToJson, TransformerResolver.RegistryAction.ADDED);
        graphResolver.transformerChange(xmlToJson, TransformerResolver.RegistryAction.REMOVED);

        assertEquals(0, graphResolver.graph.vertexSet().size());
        assertEquals(0, graphResolver.graph.edgeSet().size());
    }

    @Test
    public void ignoresRemovingConverterThatWasNeverAdded()
    {
        Transformer xmlToJson = new MockConverterBuilder().from(XML_DATA_TYPE).to(JSON_DATA_TYPE).weighting(1).build();
        Transformer betterXmlToJson = new MockConverterBuilder().from(XML_DATA_TYPE).to(JSON_DATA_TYPE).weighting(2).build();

        GraphTransformerResolver graphResolver = new GraphTransformerResolver();
        graphResolver.transformerChange(xmlToJson, TransformerResolver.RegistryAction.ADDED);
        graphResolver.transformerChange(betterXmlToJson, TransformerResolver.RegistryAction.REMOVED);

        assertEquals(2, graphResolver.graph.vertexSet().size());
        assertTrue(graphResolver.graph.containsVertex(XML_DATA_TYPE));
        assertTrue(graphResolver.graph.containsVertex(JSON_DATA_TYPE));

        assertEquals(1, graphResolver.graph.edgeSet().size());
        assertTrue(graphResolver.graph.containsEdge(XML_DATA_TYPE, JSON_DATA_TYPE));
    }

    @Test
    public void processesConverterRemovedWithMultipleSourceTypes()
    {
        Transformer xmlToJson = new MockConverterBuilder().from(XML_DATA_TYPE, INPUT_STREAM_DATA_TYPE).to(JSON_DATA_TYPE).build();

        GraphTransformerResolver graphResolver = new GraphTransformerResolver();
        graphResolver.transformerChange(xmlToJson, TransformerResolver.RegistryAction.ADDED);
        graphResolver.transformerChange(xmlToJson, TransformerResolver.RegistryAction.REMOVED);

        assertEquals(0, graphResolver.graph.vertexSet().size());
        assertEquals(0, graphResolver.graph.edgeSet().size());
    }

    @Test
    public void multipleConvertersFromSameSourceToResultTypes()
    {
        Transformer xmlToJson = new MockConverterBuilder().from(XML_DATA_TYPE).to(JSON_DATA_TYPE).build();
        Transformer betterXmlToJson = new MockConverterBuilder().from(XML_DATA_TYPE).to(JSON_DATA_TYPE).build();

        GraphTransformerResolver graphResolver = new GraphTransformerResolver();
        graphResolver.transformerChange(xmlToJson, TransformerResolver.RegistryAction.ADDED);
        graphResolver.transformerChange(betterXmlToJson, TransformerResolver.RegistryAction.ADDED);

        assertEquals(2, graphResolver.graph.vertexSet().size());
        assertTrue(graphResolver.graph.containsVertex(XML_DATA_TYPE));
        assertTrue(graphResolver.graph.containsVertex(JSON_DATA_TYPE));

        assertEquals(2, graphResolver.graph.edgeSet().size());
        assertTrue(graphResolver.graph.containsEdge(XML_DATA_TYPE, JSON_DATA_TYPE));
        assertFalse(graphResolver.graph.containsEdge(JSON_DATA_TYPE, XML_DATA_TYPE));

        assertContainsTransformer(graphResolver.graph.edgesOf(JSON_DATA_TYPE), xmlToJson);
        assertContainsTransformer(graphResolver.graph.edgesOf(JSON_DATA_TYPE), betterXmlToJson);
    }

    @Test
    public void removesFirstDuplicateConverterAdded()
    {
        Transformer xmlToJson = new MockConverterBuilder().from(XML_DATA_TYPE).to(JSON_DATA_TYPE).build();
        Transformer betterXmlToJson = new MockConverterBuilder().from(XML_DATA_TYPE).to(JSON_DATA_TYPE).build();

        GraphTransformerResolver graphResolver = new GraphTransformerResolver();
        graphResolver.transformerChange(xmlToJson, TransformerResolver.RegistryAction.ADDED);
        graphResolver.transformerChange(betterXmlToJson, TransformerResolver.RegistryAction.ADDED);

        graphResolver.transformerChange(xmlToJson, TransformerResolver.RegistryAction.REMOVED);

        assertEquals(2, graphResolver.graph.vertexSet().size());
        assertTrue(graphResolver.graph.containsVertex(JSON_DATA_TYPE));
        assertTrue(graphResolver.graph.containsVertex(XML_DATA_TYPE));

        Set<GraphTransformerResolver.TransformationEdge> transformationEdges = graphResolver.graph.edgesOf(JSON_DATA_TYPE);
        assertEquals(1, transformationEdges.size());
        assertContainsTransformer(transformationEdges, betterXmlToJson);
    }

    @Test
    public void removesSecondDuplicateConverterAdded()
    {
        Transformer xmlToJson = new MockConverterBuilder().from(XML_DATA_TYPE).to(JSON_DATA_TYPE).build();
        Transformer betterXmlToJson = new MockConverterBuilder().from(XML_DATA_TYPE).to(JSON_DATA_TYPE).build();

        GraphTransformerResolver graphResolver = new GraphTransformerResolver();
        graphResolver.transformerChange(xmlToJson, TransformerResolver.RegistryAction.ADDED);
        graphResolver.transformerChange(betterXmlToJson, TransformerResolver.RegistryAction.ADDED);

        graphResolver.transformerChange(betterXmlToJson, TransformerResolver.RegistryAction.REMOVED);

        assertEquals(2, graphResolver.graph.vertexSet().size());
        assertTrue(graphResolver.graph.containsVertex(JSON_DATA_TYPE));
        assertTrue(graphResolver.graph.containsVertex(XML_DATA_TYPE));

        Set<GraphTransformerResolver.TransformationEdge> transformationEdges = graphResolver.graph.edgesOf(JSON_DATA_TYPE);
        assertEquals(1, transformationEdges.size());
        assertContainsTransformer(transformationEdges, xmlToJson);
    }

    @Test
    public void multipleConvertersFromDifferentSourceToSameResultTypes()
    {
        Transformer xmlToJson = new MockConverterBuilder().from(XML_DATA_TYPE).to(JSON_DATA_TYPE).build();
        Transformer objectToJson = new MockConverterBuilder().from(INPUT_STREAM_DATA_TYPE).to(JSON_DATA_TYPE).build();

        GraphTransformerResolver graphResolver = new GraphTransformerResolver();
        graphResolver.transformerChange(xmlToJson, TransformerResolver.RegistryAction.ADDED);
        graphResolver.transformerChange(objectToJson, TransformerResolver.RegistryAction.ADDED);

        assertEquals(3, graphResolver.graph.vertexSet().size());
        assertTrue(graphResolver.graph.containsVertex(XML_DATA_TYPE));
        assertTrue(graphResolver.graph.containsVertex(JSON_DATA_TYPE));
        assertTrue(graphResolver.graph.containsVertex(INPUT_STREAM_DATA_TYPE));

        assertEquals(2, graphResolver.graph.edgeSet().size());
        assertTrue(graphResolver.graph.containsEdge(XML_DATA_TYPE, JSON_DATA_TYPE));
        assertFalse(graphResolver.graph.containsEdge(JSON_DATA_TYPE, XML_DATA_TYPE));
        assertTrue(graphResolver.graph.containsEdge(INPUT_STREAM_DATA_TYPE, JSON_DATA_TYPE));
        assertFalse(graphResolver.graph.containsEdge(JSON_DATA_TYPE, INPUT_STREAM_DATA_TYPE));
    }

    @Test
    public void removeFirstAddedConverterWithDifferentSourceToSameResultTypes()
    {
        Transformer xmlToJson = new MockConverterBuilder().from(XML_DATA_TYPE).to(JSON_DATA_TYPE).build();
        Transformer objectToJson = new MockConverterBuilder().from(INPUT_STREAM_DATA_TYPE).to(JSON_DATA_TYPE).build();

        GraphTransformerResolver graphResolver = new GraphTransformerResolver();
        graphResolver.transformerChange(xmlToJson, TransformerResolver.RegistryAction.ADDED);
        graphResolver.transformerChange(objectToJson, TransformerResolver.RegistryAction.ADDED);

        graphResolver.transformerChange(xmlToJson, TransformerResolver.RegistryAction.REMOVED);

        assertEquals(2, graphResolver.graph.vertexSet().size());
        assertTrue(graphResolver.graph.containsVertex(JSON_DATA_TYPE));
        assertTrue(graphResolver.graph.containsVertex(INPUT_STREAM_DATA_TYPE));

        assertEquals(1, graphResolver.graph.edgeSet().size());
        assertFalse(graphResolver.graph.containsEdge(XML_DATA_TYPE, JSON_DATA_TYPE));
        assertFalse(graphResolver.graph.containsEdge(JSON_DATA_TYPE, XML_DATA_TYPE));
        assertTrue(graphResolver.graph.containsEdge(INPUT_STREAM_DATA_TYPE, JSON_DATA_TYPE));
        assertFalse(graphResolver.graph.containsEdge(JSON_DATA_TYPE, INPUT_STREAM_DATA_TYPE));
    }

    @Test
    public void removeSecondAddedConverterWithDifferentSourceToSameResultTypes()
    {
        Transformer xmlToJson = new MockConverterBuilder().from(XML_DATA_TYPE).to(JSON_DATA_TYPE).build();
        Transformer objectToJson = new MockConverterBuilder().from(INPUT_STREAM_DATA_TYPE).to(JSON_DATA_TYPE).build();

        GraphTransformerResolver graphResolver = new GraphTransformerResolver();
        graphResolver.transformerChange(xmlToJson, TransformerResolver.RegistryAction.ADDED);
        graphResolver.transformerChange(objectToJson, TransformerResolver.RegistryAction.ADDED);

        graphResolver.transformerChange(objectToJson, TransformerResolver.RegistryAction.REMOVED);

        assertEquals(2, graphResolver.graph.vertexSet().size());
        assertTrue(graphResolver.graph.containsVertex(JSON_DATA_TYPE));
        assertTrue(graphResolver.graph.containsVertex(XML_DATA_TYPE));

        assertEquals(1, graphResolver.graph.edgeSet().size());
        assertTrue(graphResolver.graph.containsEdge(XML_DATA_TYPE, JSON_DATA_TYPE));
        assertFalse(graphResolver.graph.containsEdge(JSON_DATA_TYPE, XML_DATA_TYPE));
        assertFalse(graphResolver.graph.containsEdge(INPUT_STREAM_DATA_TYPE, JSON_DATA_TYPE));
        assertFalse(graphResolver.graph.containsEdge(JSON_DATA_TYPE, INPUT_STREAM_DATA_TYPE));
    }

    @Test
    public void noTransformerFromSourceDataTypeFound() throws ResolverException
    {
        GraphTransformerResolver graphResolver = new GraphTransformerResolver();

        Transformer transformer = graphResolver.resolve(XML_DATA_TYPE, JSON_DATA_TYPE);

        assertNull(transformer);
    }

    @Test
    public void noTransformerToReturnDataTypeFound() throws ResolverException
    {
        Transformer xmlToJson = new MockTransformerBuilder().from(XML_DATA_TYPE).to(JSON_DATA_TYPE).build();

        GraphTransformerResolver graphResolver = new GraphTransformerResolver();
        graphResolver.transformerChange(xmlToJson, TransformerResolver.RegistryAction.ADDED);

        Transformer transformer = graphResolver.resolve(XML_DATA_TYPE, INPUT_STREAM_DATA_TYPE);

        assertNull(transformer);
    }

    @Test
    public void noTransformerFound() throws ResolverException
    {
        DataType mockStringDataType = mock(DataType.class);

        Transformer xmlToJson = new MockConverterBuilder().from(XML_DATA_TYPE).to(JSON_DATA_TYPE).build();
        Transformer inputStreamToXml = new MockConverterBuilder().from(INPUT_STREAM_DATA_TYPE).to(XML_DATA_TYPE).build();

        GraphTransformerResolver graphResolver = new GraphTransformerResolver();
        graphResolver.transformerChange(inputStreamToXml, TransformerResolver.RegistryAction.ADDED);
        graphResolver.transformerChange(xmlToJson, TransformerResolver.RegistryAction.ADDED);

        Transformer transformer = graphResolver.resolve(INPUT_STREAM_DATA_TYPE, mockStringDataType);

        assertNull(transformer);
    }

    @Test
    public void resolvesDirectTransformation() throws ResolverException
    {
        Transformer xmlToJson = new MockConverterBuilder().named("xmlToJson").from(XML_DATA_TYPE).to(JSON_DATA_TYPE).build();

        GraphTransformerResolver graphResolver = new GraphTransformerResolver();
        graphResolver.transformerChange(xmlToJson, TransformerResolver.RegistryAction.ADDED);

        Transformer transformer = graphResolver.resolve(XML_DATA_TYPE, JSON_DATA_TYPE);

        assertEquals(xmlToJson, transformer);
    }

    @Test
    public void resolvesMultipleDirectTransformationAddingBetterTransformerFirst() throws ResolverException
    {
        Transformer betterXmlToJson = new MockConverterBuilder().named("betterXmlToJson").from(XML_DATA_TYPE).to(JSON_DATA_TYPE).weighting(2).build();
        Transformer xmlToJson = new MockConverterBuilder().named("xmlToJson").from(XML_DATA_TYPE).to(JSON_DATA_TYPE).weighting(1).build();

        GraphTransformerResolver graphResolver = new GraphTransformerResolver();
        graphResolver.transformerChange(betterXmlToJson, TransformerResolver.RegistryAction.ADDED);
        graphResolver.transformerChange(xmlToJson, TransformerResolver.RegistryAction.ADDED);

        Transformer transformer = graphResolver.resolve(XML_DATA_TYPE, JSON_DATA_TYPE);

        assertEquals(betterXmlToJson, transformer);
    }

    @Test
    public void resolvesMultipleDirectTransformationAddingBetterTransformerLast() throws ResolverException
    {
        Transformer betterXmlToJson = new MockConverterBuilder().named("betterXmlToJson").from(XML_DATA_TYPE).to(JSON_DATA_TYPE).weighting(2).build();
        Transformer xmlToJson = new MockConverterBuilder().named("xmlToJson").from(XML_DATA_TYPE).to(JSON_DATA_TYPE).weighting(1).build();

        GraphTransformerResolver graphResolver = new GraphTransformerResolver();
        graphResolver.transformerChange(xmlToJson, TransformerResolver.RegistryAction.ADDED);
        graphResolver.transformerChange(betterXmlToJson, TransformerResolver.RegistryAction.ADDED);

        Transformer transformer = graphResolver.resolve(XML_DATA_TYPE, JSON_DATA_TYPE);

        assertEquals(betterXmlToJson, transformer);
    }

    @Test
    public void resolvesTransformationChain() throws ResolverException
    {
        Transformer xmlToJson = new MockConverterBuilder().from(XML_DATA_TYPE).to(JSON_DATA_TYPE).build();
        Transformer inputStreamToXml = new MockConverterBuilder().from(INPUT_STREAM_DATA_TYPE).to(XML_DATA_TYPE).build();

        GraphTransformerResolver graphResolver = new GraphTransformerResolver();
        graphResolver.transformerChange(inputStreamToXml, TransformerResolver.RegistryAction.ADDED);
        graphResolver.transformerChange(xmlToJson, TransformerResolver.RegistryAction.ADDED);

        Transformer transformer = graphResolver.resolve(INPUT_STREAM_DATA_TYPE, JSON_DATA_TYPE);

        assertTrue(transformer instanceof CompositeConverter);
        CompositeConverter compositeConverter = (CompositeConverter) transformer;
        assertEquals(2, compositeConverter.chain.size());
        assertEquals(inputStreamToXml, compositeConverter.chain.get(0));
        assertEquals(xmlToJson, compositeConverter.chain.get(1));
    }

    @Test
    public void cachesResolvedTransformer() throws ResolverException
    {
        Transformer xmlToJson = new MockConverterBuilder().from(XML_DATA_TYPE).to(JSON_DATA_TYPE).build();
        Transformer inputStreamToXml = new MockConverterBuilder().from(INPUT_STREAM_DATA_TYPE).to(XML_DATA_TYPE).build();

        GraphTransformerResolver graphResolver = new GraphTransformerResolver();
        graphResolver.transformerChange(inputStreamToXml, TransformerResolver.RegistryAction.ADDED);
        graphResolver.transformerChange(xmlToJson, TransformerResolver.RegistryAction.ADDED);

        Transformer transformer1 = graphResolver.resolve(INPUT_STREAM_DATA_TYPE, JSON_DATA_TYPE);

        Transformer transformer2 = graphResolver.resolve(INPUT_STREAM_DATA_TYPE, JSON_DATA_TYPE);
        assertSame(transformer1, transformer2);
    }

    @Test
    public void resolvesTransformationChainWithMultipleEdgesFromSource() throws ResolverException
    {
        Transformer xmlToJson = new MockConverterBuilder().named("xmlToJson").from(XML_DATA_TYPE).to(JSON_DATA_TYPE).weighting(1).build();
        Transformer betterXmlToJson = new MockConverterBuilder().named("betterXmlToJson").from(XML_DATA_TYPE).to(JSON_DATA_TYPE).weighting(2).build();
        Transformer inputStreamToXml = new MockConverterBuilder().named("inputStreamToXml").from(INPUT_STREAM_DATA_TYPE).to(XML_DATA_TYPE).weighting(1).build();

        GraphTransformerResolver graphResolver = new GraphTransformerResolver();
        graphResolver.transformerChange(betterXmlToJson, TransformerResolver.RegistryAction.ADDED);
        graphResolver.transformerChange(xmlToJson, TransformerResolver.RegistryAction.ADDED);
        graphResolver.transformerChange(inputStreamToXml, TransformerResolver.RegistryAction.ADDED);

        Transformer transformer = graphResolver.resolve(INPUT_STREAM_DATA_TYPE, JSON_DATA_TYPE);

        assertTrue(transformer instanceof CompositeConverter);
        CompositeConverter chain = (CompositeConverter) transformer;
        assertEquals(2, chain.chain.size());
        assertEquals(inputStreamToXml, chain.chain.get(0));
        assertEquals(betterXmlToJson, chain.chain.get(1));
    }

    @Test
    public void resolvesTransformationChainWithMultipleEdgesToResult() throws ResolverException
    {
        Transformer xmlToJson = new MockConverterBuilder().from(XML_DATA_TYPE).to(JSON_DATA_TYPE).weighting(1).build();
        Transformer inputStreamToXml = new MockConverterBuilder().from(INPUT_STREAM_DATA_TYPE).to(XML_DATA_TYPE).weighting(1).build();
        Transformer mockBetterTransformerinputStreamToXml = new MockConverterBuilder().from(INPUT_STREAM_DATA_TYPE).to(XML_DATA_TYPE).weighting(2).build();

        GraphTransformerResolver graphResolver = new GraphTransformerResolver();
        graphResolver.transformerChange(inputStreamToXml, TransformerResolver.RegistryAction.ADDED);
        graphResolver.transformerChange(xmlToJson, TransformerResolver.RegistryAction.ADDED);
        graphResolver.transformerChange(mockBetterTransformerinputStreamToXml, TransformerResolver.RegistryAction.ADDED);

        Transformer transformer = graphResolver.resolve(INPUT_STREAM_DATA_TYPE, JSON_DATA_TYPE);

        assertTrue(transformer instanceof CompositeConverter);
        CompositeConverter chain = (CompositeConverter) transformer;
        assertEquals(2, chain.chain.size());
        assertEquals(mockBetterTransformerinputStreamToXml, chain.chain.get(0));
        assertEquals(xmlToJson, chain.chain.get(1));
    }

    @Test
    public void resolvesTransformationChainWithMultipleEdges() throws ResolverException
    {
        Transformer xmlToJson = new MockConverterBuilder().named("xmlToJson").from(XML_DATA_TYPE).to(JSON_DATA_TYPE).weighting(1).build();
        Transformer betterXmlToJson = new MockConverterBuilder().named("betterXmlToJson").from(XML_DATA_TYPE).to(JSON_DATA_TYPE).weighting(2).build();
        Transformer inputStreamToXml = new MockConverterBuilder().named("inputStreamToXml").from(INPUT_STREAM_DATA_TYPE).to(XML_DATA_TYPE).weighting(1).build();
        Transformer mockBetterTransformerInputStreamToXml = new MockConverterBuilder().named("mockBetterTransformerInputStreamToXml").from(INPUT_STREAM_DATA_TYPE).to(XML_DATA_TYPE).weighting(2).build();

        GraphTransformerResolver graphResolver = new GraphTransformerResolver();
        graphResolver.transformerChange(mockBetterTransformerInputStreamToXml, TransformerResolver.RegistryAction.ADDED);
        graphResolver.transformerChange(inputStreamToXml, TransformerResolver.RegistryAction.ADDED);
        graphResolver.transformerChange(xmlToJson, TransformerResolver.RegistryAction.ADDED);
        graphResolver.transformerChange(betterXmlToJson, TransformerResolver.RegistryAction.ADDED);

        Transformer transformer = graphResolver.resolve(INPUT_STREAM_DATA_TYPE, JSON_DATA_TYPE);

        assertTrue(transformer instanceof CompositeConverter);
        CompositeConverter chain = (CompositeConverter) transformer;
        assertEquals(2, chain.chain.size());
        assertEquals(mockBetterTransformerInputStreamToXml, chain.chain.get(0));
        assertEquals(betterXmlToJson, chain.chain.get(1));
    }

    @Test
    public void clearsCacheWhenAddsTransformer() throws ResolverException
    {
        Transformer xmlToJson = new MockConverterBuilder().named("xmlToJson").from(XML_DATA_TYPE).to(JSON_DATA_TYPE).build();
        Transformer inputStreamToXml = new MockConverterBuilder().named("inputStreamToXml").from(INPUT_STREAM_DATA_TYPE).to(XML_DATA_TYPE).build();

        GraphTransformerResolver graphResolver = new GraphTransformerResolver();
        graphResolver.transformerChange(inputStreamToXml, TransformerResolver.RegistryAction.ADDED);
        graphResolver.transformerChange(xmlToJson, TransformerResolver.RegistryAction.ADDED);

        Transformer transformer1 = graphResolver.resolve(INPUT_STREAM_DATA_TYPE, JSON_DATA_TYPE);
        assertNotNull(transformer1);

        Transformer xmlToString = new MockConverterBuilder().named("xmlToString").from(XML_DATA_TYPE).to(STRING_DATA_TYPE).build();
        graphResolver.transformerChange(xmlToString, TransformerResolver.RegistryAction.ADDED);

        Transformer transformer2 = graphResolver.resolve(INPUT_STREAM_DATA_TYPE, JSON_DATA_TYPE);
        assertNotSame(transformer1, transformer2);
    }

    @Test
    public void clearsCacheWhenRemovesTransformer() throws ResolverException
    {
        Transformer xmlToJson = new MockConverterBuilder().from(XML_DATA_TYPE).to(JSON_DATA_TYPE).build();

        Transformer inputStreamToXml = new MockConverterBuilder().from(INPUT_STREAM_DATA_TYPE).to(XML_DATA_TYPE).build();

        GraphTransformerResolver graphResolver = new GraphTransformerResolver();
        graphResolver.transformerChange(inputStreamToXml, TransformerResolver.RegistryAction.ADDED);
        graphResolver.transformerChange(xmlToJson, TransformerResolver.RegistryAction.ADDED);

        Transformer transformer = graphResolver.resolve(INPUT_STREAM_DATA_TYPE, JSON_DATA_TYPE);
        assertNotNull(transformer);

        graphResolver.transformerChange(inputStreamToXml, TransformerResolver.RegistryAction.REMOVED);
        graphResolver.transformerChange(xmlToJson, TransformerResolver.RegistryAction.REMOVED);

        transformer = graphResolver.resolve(INPUT_STREAM_DATA_TYPE, JSON_DATA_TYPE);

        assertNull(transformer);
    }

    @Test
    public void lookupTransformersNoSourceInGraph() throws Exception
    {
        Transformer inputStreamToXml = new MockConverterBuilder().from(INPUT_STREAM_DATA_TYPE).to(XML_DATA_TYPE).build();

        GraphTransformerResolver graphResolver = new GraphTransformerResolver();
        graphResolver.transformerChange(inputStreamToXml, TransformerResolver.RegistryAction.ADDED);

        List<Transformer> transformers = graphResolver.lookupTransformers(JSON_DATA_TYPE, INPUT_STREAM_DATA_TYPE);

        assertEquals(0, transformers.size());
    }

    @Test
    public void lookupTransformersNoTargetInGraph() throws Exception
    {
        Transformer inputStreamToXml = new MockConverterBuilder().from(INPUT_STREAM_DATA_TYPE).to(XML_DATA_TYPE).build();

        GraphTransformerResolver graphResolver = new GraphTransformerResolver();
        graphResolver.transformerChange(inputStreamToXml, TransformerResolver.RegistryAction.ADDED);

        List<Transformer> transformers = graphResolver.lookupTransformers(INPUT_STREAM_DATA_TYPE, JSON_DATA_TYPE);

        assertEquals(0, transformers.size());
    }

    @Test
    public void lookupTransformersOneMatchingDirectTransformer() throws Exception
    {
        Transformer inputStreamToXml = new MockConverterBuilder().from(INPUT_STREAM_DATA_TYPE).to(XML_DATA_TYPE).build();

        GraphTransformerResolver graphResolver = new GraphTransformerResolver();
        graphResolver.transformerChange(inputStreamToXml, TransformerResolver.RegistryAction.ADDED);

        List<Transformer> transformers = graphResolver.lookupTransformers(INPUT_STREAM_DATA_TYPE, XML_DATA_TYPE);

        assertEquals(1, transformers.size());
        assertEquals(inputStreamToXml, transformers.get(0));
    }

    @Test
    public void lookupTransformersTwoMatchingDirectTransformers() throws Exception
    {
        Transformer inputStreamToXml = new MockConverterBuilder().from(INPUT_STREAM_DATA_TYPE).to(XML_DATA_TYPE).build();
        Transformer betterInputStreamToXml = new MockConverterBuilder().from(INPUT_STREAM_DATA_TYPE).to(XML_DATA_TYPE).build();

        GraphTransformerResolver graphResolver = new GraphTransformerResolver();
        graphResolver.transformerChange(inputStreamToXml, TransformerResolver.RegistryAction.ADDED);
        graphResolver.transformerChange(betterInputStreamToXml, TransformerResolver.RegistryAction.ADDED);

        List<Transformer> transformers = graphResolver.lookupTransformers(INPUT_STREAM_DATA_TYPE, XML_DATA_TYPE);

        assertEquals(2, transformers.size());
        Assert.assertTrue(transformers.contains(inputStreamToXml));
        Assert.assertTrue(transformers.contains(betterInputStreamToXml));
    }

    @Test
    public void lookupTransformersMatchingNonDirectTransformers1() throws Exception
    {
        /*
        A= INPUT_STREAM_DATA_TYPE
        B= STRING_DATA_TYPE
        C= JSON_DATA_TYPE
        D= XML_DATA_TYPE

        A->B
        A->C
        B->D
        C->D
         */
        Transformer inputStreamToString = new MockConverterBuilder().named("inputStreamToString").from(INPUT_STREAM_DATA_TYPE).to(STRING_DATA_TYPE).build();
        Transformer inputStreamToJson = new MockConverterBuilder().named("inputStreamToJson").from(INPUT_STREAM_DATA_TYPE).to(JSON_DATA_TYPE).build();
        Transformer jsonToXml = new MockConverterBuilder().named("jsonToXml").from(JSON_DATA_TYPE).to(XML_DATA_TYPE).build();
        Transformer stringToXml = new MockConverterBuilder().named("stringToXml").from(STRING_DATA_TYPE).to(XML_DATA_TYPE).build();

        GraphTransformerResolver graphResolver = new GraphTransformerResolver();
        graphResolver.transformerChange(inputStreamToString, TransformerResolver.RegistryAction.ADDED);
        graphResolver.transformerChange(inputStreamToJson, TransformerResolver.RegistryAction.ADDED);
        graphResolver.transformerChange(jsonToXml, TransformerResolver.RegistryAction.ADDED);
        graphResolver.transformerChange(stringToXml, TransformerResolver.RegistryAction.ADDED);

        List<Transformer> transformers = graphResolver.lookupTransformers(INPUT_STREAM_DATA_TYPE, XML_DATA_TYPE);

        assertEquals(2, transformers.size());
        assertContainsCompositeTransformer(transformers, inputStreamToString, stringToXml);
        assertContainsCompositeTransformer(transformers, inputStreamToJson, jsonToXml);
    }

    @Test
    public void lookupTransformersMatchingNonDirectTransformers2() throws Exception
    {
        /*
        A= INPUT_STREAM_DATA_TYPE
        B= STRING_DATA_TYPE
        C= JSON_DATA_TYPE
        D= XML_DATA_TYPE

        A->B
        A->C
        B->C
        B->D
        C->D
         */
        Transformer inputStreamToString = new MockConverterBuilder().named("inputStreamToString").from(INPUT_STREAM_DATA_TYPE).to(STRING_DATA_TYPE).build();
        Transformer inputStreamToJson = new MockConverterBuilder().named("inputStreamToJson").from(INPUT_STREAM_DATA_TYPE).to(JSON_DATA_TYPE).build();
        Transformer jsonToXml = new MockConverterBuilder().named("jsonToXml").from(JSON_DATA_TYPE).to(XML_DATA_TYPE).build();
        Transformer stringToXml = new MockConverterBuilder().named("stringToXml").from(STRING_DATA_TYPE).to(XML_DATA_TYPE).build();
        Transformer stringToJson = new MockConverterBuilder().named("stringToJson").from(STRING_DATA_TYPE).to(JSON_DATA_TYPE).build();

        GraphTransformerResolver graphResolver = new GraphTransformerResolver();
        graphResolver.transformerChange(inputStreamToString, TransformerResolver.RegistryAction.ADDED);
        graphResolver.transformerChange(inputStreamToJson, TransformerResolver.RegistryAction.ADDED);
        graphResolver.transformerChange(jsonToXml, TransformerResolver.RegistryAction.ADDED);
        graphResolver.transformerChange(stringToXml, TransformerResolver.RegistryAction.ADDED);
        graphResolver.transformerChange(stringToJson, TransformerResolver.RegistryAction.ADDED);

        List<Transformer> transformers = graphResolver.lookupTransformers(INPUT_STREAM_DATA_TYPE, XML_DATA_TYPE);

        assertEquals(3, transformers.size());
        assertContainsCompositeTransformer(transformers, inputStreamToString, stringToXml);
        assertContainsCompositeTransformer(transformers, inputStreamToJson, jsonToXml);
        assertContainsCompositeTransformer(transformers, inputStreamToString, stringToJson, jsonToXml);
    }

    @Test
    public void lookupTransformersMatchingNonDirectTransformers3() throws Exception
    {
        /*
        A= INPUT_STREAM_DATA_TYPE
        B= STRING_DATA_TYPE
        C= JSON_DATA_TYPE
        D= XML_DATA_TYPE

        A->B
        A->C
        B->C
        B->D
        C->D
        C->D
         */
        Transformer inputStreamToString = new MockConverterBuilder().named("inputStreamToString").from(INPUT_STREAM_DATA_TYPE).to(STRING_DATA_TYPE).build();
        Transformer inputStreamToJson = new MockConverterBuilder().named("inputStreamToJson").from(INPUT_STREAM_DATA_TYPE).to(JSON_DATA_TYPE).build();
        Transformer jsonToXml = new MockConverterBuilder().named("jsonToXml").from(JSON_DATA_TYPE).to(XML_DATA_TYPE).build();
        Transformer jsonToString = new MockConverterBuilder().named("jsonToXml").from(JSON_DATA_TYPE).to(STRING_DATA_TYPE).build();
        Transformer stringToXml = new MockConverterBuilder().named("stringToXml").from(STRING_DATA_TYPE).to(XML_DATA_TYPE).build();
        Transformer stringToJson = new MockConverterBuilder().named("stringToJson").from(STRING_DATA_TYPE).to(JSON_DATA_TYPE).build();

        GraphTransformerResolver graphResolver = new GraphTransformerResolver();
        graphResolver.transformerChange(inputStreamToString, TransformerResolver.RegistryAction.ADDED);
        graphResolver.transformerChange(inputStreamToJson, TransformerResolver.RegistryAction.ADDED);
        graphResolver.transformerChange(jsonToXml, TransformerResolver.RegistryAction.ADDED);
        graphResolver.transformerChange(jsonToString, TransformerResolver.RegistryAction.ADDED);
        graphResolver.transformerChange(stringToXml, TransformerResolver.RegistryAction.ADDED);
        graphResolver.transformerChange(stringToJson, TransformerResolver.RegistryAction.ADDED);

        List<Transformer> transformers = graphResolver.lookupTransformers(INPUT_STREAM_DATA_TYPE, XML_DATA_TYPE);

        assertEquals(4, transformers.size());
        assertContainsCompositeTransformer(transformers, inputStreamToString, stringToXml);
        assertContainsCompositeTransformer(transformers, inputStreamToJson, jsonToXml);
        assertContainsCompositeTransformer(transformers, inputStreamToString, stringToJson, jsonToXml);
        assertContainsCompositeTransformer(transformers, inputStreamToJson, jsonToString, stringToXml);
    }

    private void assertContainsCompositeTransformer(List<Transformer> transformers, Transformer... composedTransformers)
    {
        //To change body of created methods use File | Settings | File Templates.
        for (Transformer transformer :transformers)
        {
            if (transformer instanceof CompositeConverter)
            {
                CompositeConverter compositeConverter = (CompositeConverter) transformer;
                if (compositeConverter.chain.size() != composedTransformers.length)
                {
                    continue;
                }

                boolean matches = true;
                for (int i =0; i< composedTransformers.length -1; i++)
                {
                    if (composedTransformers[i] != compositeConverter.chain.get(i))
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

        fail("Transformer list does not contain a composite transformer with: " + composedTransformers);
    }

    private void assertContainsTransformer(Set<GraphTransformerResolver.TransformationEdge> transformationEdges, Transformer transformer)
    {
        for (GraphTransformerResolver.TransformationEdge edge : transformationEdges)
        {
            if (edge.getTransformer() == transformer)
            {
                return;
            }
        }

        fail(String.format("Transformation edges %s do not contain expected transformer %s", transformationEdges, transformer));
    }

}
