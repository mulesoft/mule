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
import org.mule.api.transformer.Converter;
import org.mule.api.transformer.DataType;
import org.mule.api.transformer.Transformer;
import org.mule.tck.size.SmallTest;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

@SmallTest
public class GraphTransformerResolverTest
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
        Transformer xmlToJson = createMockTransformer(JSON_DATA_TYPE, "", XML_DATA_TYPE);

        GraphTransformerResolver graphResolver = new GraphTransformerResolver();
        graphResolver.transformerChange(xmlToJson, TransformerResolver.RegistryAction.ADDED);

        assertEquals(0, graphResolver.graph.vertexSet().size());
        assertEquals(0, graphResolver.graph.edgeSet().size());
    }

    @Test
    public void ignoresTransformerRemoved()
    {
        Transformer xmlToJson = createMockTransformer(JSON_DATA_TYPE, "", XML_DATA_TYPE);

        GraphTransformerResolver graphResolver = new GraphTransformerResolver();
        graphResolver.transformerChange(xmlToJson, TransformerResolver.RegistryAction.ADDED);
        graphResolver.transformerChange(xmlToJson, TransformerResolver.RegistryAction.REMOVED);

        assertEquals(0, graphResolver.graph.vertexSet().size());
        assertEquals(0, graphResolver.graph.edgeSet().size());
    }

    @Test
    public void processesConverterAdded()
    {
        Transformer xmlToJson = createMockConverter(JSON_DATA_TYPE, "", XML_DATA_TYPE);

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
        Transformer xmlToJson = createMockConverter(JSON_DATA_TYPE, "", XML_DATA_TYPE);

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
        Transformer xmlToJson = createMockConverter(JSON_DATA_TYPE, "", XML_DATA_TYPE, INPUT_STREAM_DATA_TYPE);

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
        Transformer xmlToJson = createMockConverter(JSON_DATA_TYPE, "", XML_DATA_TYPE);

        GraphTransformerResolver graphResolver = new GraphTransformerResolver();
        graphResolver.transformerChange(xmlToJson, TransformerResolver.RegistryAction.ADDED);
        graphResolver.transformerChange(xmlToJson, TransformerResolver.RegistryAction.REMOVED);

        assertEquals(0, graphResolver.graph.vertexSet().size());
        assertEquals(0, graphResolver.graph.edgeSet().size());
    }

    @Test
    public void ignoresRemovingConverterThatWasNeverAdded()
    {
        Transformer xmlToJson = createMockConverter(1, JSON_DATA_TYPE, "", XML_DATA_TYPE);
        Transformer betterXmlToJson = createMockConverter(2, JSON_DATA_TYPE, "", XML_DATA_TYPE);

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
        Transformer xmlToJson = createMockConverter(JSON_DATA_TYPE, "", XML_DATA_TYPE, INPUT_STREAM_DATA_TYPE);

        GraphTransformerResolver graphResolver = new GraphTransformerResolver();
        graphResolver.transformerChange(xmlToJson, TransformerResolver.RegistryAction.ADDED);
        graphResolver.transformerChange(xmlToJson, TransformerResolver.RegistryAction.REMOVED);

        assertEquals(0, graphResolver.graph.vertexSet().size());
        assertEquals(0, graphResolver.graph.edgeSet().size());
    }

    @Test
    public void multipleConvertersFromSameSourceToResultTypes()
    {
        Transformer xmlToJson = createMockConverter(JSON_DATA_TYPE, "", XML_DATA_TYPE);
        Transformer betterXmlToJson = createMockConverter(JSON_DATA_TYPE, "", XML_DATA_TYPE);

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
        Transformer xmlToJson = createMockConverter(JSON_DATA_TYPE, "", XML_DATA_TYPE);
        Transformer betterXmlToJson = createMockConverter(JSON_DATA_TYPE, "", XML_DATA_TYPE);

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
        Transformer xmlToJson = createMockConverter(JSON_DATA_TYPE, "", XML_DATA_TYPE);
        Transformer betterXmlToJson = createMockConverter(JSON_DATA_TYPE, "", XML_DATA_TYPE);

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
        Transformer xmlToJson = createMockConverter(JSON_DATA_TYPE, "", XML_DATA_TYPE);
        Transformer objectToJson = createMockConverter(JSON_DATA_TYPE, "", INPUT_STREAM_DATA_TYPE);

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
        Transformer xmlToJson = createMockConverter(JSON_DATA_TYPE, "", XML_DATA_TYPE);
        Transformer objectToJson = createMockConverter(JSON_DATA_TYPE, "", INPUT_STREAM_DATA_TYPE);

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
        Transformer xmlToJson = createMockConverter(JSON_DATA_TYPE, "", XML_DATA_TYPE);
        Transformer objectToJson = createMockConverter(JSON_DATA_TYPE, "", INPUT_STREAM_DATA_TYPE);

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
        Transformer xmlToJson = createMockTransformer(JSON_DATA_TYPE, "", XML_DATA_TYPE);

        GraphTransformerResolver graphResolver = new GraphTransformerResolver();
        graphResolver.transformerChange(xmlToJson, TransformerResolver.RegistryAction.ADDED);

        Transformer transformer = graphResolver.resolve(XML_DATA_TYPE, INPUT_STREAM_DATA_TYPE);

        assertNull(transformer);
    }

    @Test
    public void noTransformerFound() throws ResolverException
    {
        DataType mockStringDataType = mock(DataType.class);

        Transformer xmlToJson = createMockConverter(JSON_DATA_TYPE, "", XML_DATA_TYPE);
        Transformer inputStreamToXml = createMockConverter(XML_DATA_TYPE, "", INPUT_STREAM_DATA_TYPE);

        GraphTransformerResolver graphResolver = new GraphTransformerResolver();
        graphResolver.transformerChange(inputStreamToXml, TransformerResolver.RegistryAction.ADDED);
        graphResolver.transformerChange(xmlToJson, TransformerResolver.RegistryAction.ADDED);

        Transformer transformer = graphResolver.resolve(INPUT_STREAM_DATA_TYPE, mockStringDataType);

        assertNull(transformer);
    }

    @Test
    public void resolvesDirectTransformation() throws ResolverException
    {
        Transformer xmlToJson = createMockConverter(JSON_DATA_TYPE, "xmlToJson", XML_DATA_TYPE);

        GraphTransformerResolver graphResolver = new GraphTransformerResolver();
        graphResolver.transformerChange(xmlToJson, TransformerResolver.RegistryAction.ADDED);

        Transformer transformer = graphResolver.resolve(XML_DATA_TYPE, JSON_DATA_TYPE);

        assertEquals(xmlToJson, transformer);
    }

    @Test
    public void resolvesMultipleDirectTransformationAddingBetterTransformerFirst() throws ResolverException
    {
        Transformer betterXmlToJson = createMockConverter(2, JSON_DATA_TYPE, "betterXmlToJson", XML_DATA_TYPE);
        Transformer xmlToJson = createMockConverter(1, JSON_DATA_TYPE, "xmlToJson", XML_DATA_TYPE);

        GraphTransformerResolver graphResolver = new GraphTransformerResolver();
        graphResolver.transformerChange(betterXmlToJson, TransformerResolver.RegistryAction.ADDED);
        graphResolver.transformerChange(xmlToJson, TransformerResolver.RegistryAction.ADDED);

        Transformer transformer = graphResolver.resolve(XML_DATA_TYPE, JSON_DATA_TYPE);

        assertEquals(betterXmlToJson, transformer);
    }

    @Test
    public void resolvesMultipleDirectTransformationAddingBetterTransformerLast() throws ResolverException
    {
        Transformer betterXmlToJson = createMockConverter(2, JSON_DATA_TYPE, "betterXmlToJson", XML_DATA_TYPE);
        Transformer xmlToJson = createMockConverter(1, JSON_DATA_TYPE, "xmlToJson", XML_DATA_TYPE);


        GraphTransformerResolver graphResolver = new GraphTransformerResolver();
        graphResolver.transformerChange(xmlToJson, TransformerResolver.RegistryAction.ADDED);
        graphResolver.transformerChange(betterXmlToJson, TransformerResolver.RegistryAction.ADDED);

        Transformer transformer = graphResolver.resolve(XML_DATA_TYPE, JSON_DATA_TYPE);

        assertEquals(betterXmlToJson, transformer);
    }

    @Test
    public void resolvesTransformationChain() throws ResolverException
    {
        Transformer xmlToJson = createMockConverter(JSON_DATA_TYPE, "", XML_DATA_TYPE);
        Transformer inputStreamToXml = createMockConverter(XML_DATA_TYPE, "", INPUT_STREAM_DATA_TYPE);

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
        Transformer xmlToJson = createMockConverter(JSON_DATA_TYPE, "", XML_DATA_TYPE);
        Transformer inputStreamToXml = createMockConverter(XML_DATA_TYPE, "", INPUT_STREAM_DATA_TYPE);

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
        Transformer xmlToJson = createMockConverter(1, JSON_DATA_TYPE, "xmlToJson", XML_DATA_TYPE);
        Transformer betterXmlToJson = createMockConverter(2, JSON_DATA_TYPE, "betterXmlToJson", XML_DATA_TYPE);
        Transformer inputStreamToXml = createMockConverter(1, XML_DATA_TYPE, "inputStreamToXml", INPUT_STREAM_DATA_TYPE);

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
        Transformer xmlToJson = createMockConverter(1, JSON_DATA_TYPE, "", XML_DATA_TYPE);
        Transformer inputStreamToXml = createMockConverter(1, XML_DATA_TYPE, "", INPUT_STREAM_DATA_TYPE);
        Transformer mockBetterTransformerinputStreamToXml = createMockConverter(2, XML_DATA_TYPE, "", INPUT_STREAM_DATA_TYPE);

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
        Transformer xmlToJson = createMockConverter(1, JSON_DATA_TYPE, "xmlToJson", XML_DATA_TYPE);
        Transformer betterXmlToJson = createMockConverter(2, JSON_DATA_TYPE, "betterXmlToJson", XML_DATA_TYPE);
        Transformer inputStreamToXml = createMockConverter(1, XML_DATA_TYPE, "inputStreamToXml", INPUT_STREAM_DATA_TYPE);
        Transformer mockBetterTransformerInputStreamToXml = createMockConverter(2, XML_DATA_TYPE, "mockBetterTransformerInputStreamToXml", INPUT_STREAM_DATA_TYPE);

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
        Transformer xmlToJson = createMockConverter(JSON_DATA_TYPE, "xmlToJson", XML_DATA_TYPE);
        Transformer inputStreamToXml = createMockConverter(XML_DATA_TYPE, "inputStreamToXml", INPUT_STREAM_DATA_TYPE);

        GraphTransformerResolver graphResolver = new GraphTransformerResolver();
        graphResolver.transformerChange(inputStreamToXml, TransformerResolver.RegistryAction.ADDED);
        graphResolver.transformerChange(xmlToJson, TransformerResolver.RegistryAction.ADDED);

        Transformer transformer1 = graphResolver.resolve(INPUT_STREAM_DATA_TYPE, JSON_DATA_TYPE);
        assertNotNull(transformer1);

        Transformer xmlToString = createMockConverter(STRING_DATA_TYPE, "xmlToString", XML_DATA_TYPE);
        graphResolver.transformerChange(xmlToString, TransformerResolver.RegistryAction.ADDED);

        Transformer transformer2 = graphResolver.resolve(INPUT_STREAM_DATA_TYPE, JSON_DATA_TYPE);
        assertNotSame(transformer1, transformer2);
    }

    @Test
    public void clearsCacheWhenRemovesTransformer() throws ResolverException
    {
        Transformer xmlToJson = createMockConverter(JSON_DATA_TYPE, "", XML_DATA_TYPE);

        Transformer inputStreamToXml = createMockConverter(XML_DATA_TYPE, "", INPUT_STREAM_DATA_TYPE);

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
        Transformer inputStreamToXml = createMockConverter(XML_DATA_TYPE, "", INPUT_STREAM_DATA_TYPE);

        GraphTransformerResolver graphResolver = new GraphTransformerResolver();
        graphResolver.transformerChange(inputStreamToXml, TransformerResolver.RegistryAction.ADDED);

        List<Transformer> transformers = graphResolver.lookupTransformers(JSON_DATA_TYPE, INPUT_STREAM_DATA_TYPE);

        assertEquals(0, transformers.size());
    }

    @Test
    public void lookupTransformersNoTargetInGraph() throws Exception
    {
        Transformer inputStreamToXml = createMockConverter(XML_DATA_TYPE, "", INPUT_STREAM_DATA_TYPE);

        GraphTransformerResolver graphResolver = new GraphTransformerResolver();
        graphResolver.transformerChange(inputStreamToXml, TransformerResolver.RegistryAction.ADDED);

        List<Transformer> transformers = graphResolver.lookupTransformers(INPUT_STREAM_DATA_TYPE, JSON_DATA_TYPE);

        assertEquals(0, transformers.size());
    }

    @Test
    public void lookupTransformersOneMatchingDirectTransformer() throws Exception
    {
        Transformer inputStreamToXml = createMockConverter(XML_DATA_TYPE, "", INPUT_STREAM_DATA_TYPE);

        GraphTransformerResolver graphResolver = new GraphTransformerResolver();
        graphResolver.transformerChange(inputStreamToXml, TransformerResolver.RegistryAction.ADDED);

        List<Transformer> transformers = graphResolver.lookupTransformers(INPUT_STREAM_DATA_TYPE, XML_DATA_TYPE);

        assertEquals(1, transformers.size());
        assertEquals(inputStreamToXml, transformers.get(0));
    }

    @Test
    public void lookupTransformersTwoMatchingDirectTransformers() throws Exception
    {
        Transformer inputStreamToXml = createMockConverter(XML_DATA_TYPE, "", INPUT_STREAM_DATA_TYPE);
        Transformer betterInputStreamToXml = createMockConverter(XML_DATA_TYPE, "", INPUT_STREAM_DATA_TYPE);

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
        Transformer inputStreamToString = createMockConverter(STRING_DATA_TYPE, "inputStreamToString", INPUT_STREAM_DATA_TYPE);
        Transformer inputStreamToJson = createMockConverter(JSON_DATA_TYPE, "inputStreamToJson", INPUT_STREAM_DATA_TYPE);
        Transformer jsonToXml = createMockConverter(XML_DATA_TYPE, "jsonToXml", JSON_DATA_TYPE);
        Transformer stringToXml = createMockConverter(XML_DATA_TYPE, "stringToXml", STRING_DATA_TYPE);

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
        Transformer inputStreamToString = createMockConverter(STRING_DATA_TYPE, "inputStreamToString", INPUT_STREAM_DATA_TYPE);
        Transformer inputStreamToJson = createMockConverter(JSON_DATA_TYPE, "inputStreamToJson", INPUT_STREAM_DATA_TYPE);
        Transformer jsonToXml = createMockConverter(XML_DATA_TYPE, "jsonToXml", JSON_DATA_TYPE);
        Transformer stringToXml = createMockConverter(XML_DATA_TYPE, "stringToXml", STRING_DATA_TYPE);
        Transformer stringToJson = createMockConverter(JSON_DATA_TYPE, "stringToJson", STRING_DATA_TYPE);

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
        Transformer inputStreamToString = createMockConverter(STRING_DATA_TYPE, "inputStreamToString", INPUT_STREAM_DATA_TYPE);
        Transformer inputStreamToJson = createMockConverter(JSON_DATA_TYPE, "inputStreamToJson", INPUT_STREAM_DATA_TYPE);
        Transformer jsonToXml = createMockConverter(XML_DATA_TYPE, "jsonToXml", JSON_DATA_TYPE);
        Transformer jsonToString = createMockConverter(STRING_DATA_TYPE, "jsonToXml", JSON_DATA_TYPE);
        Transformer stringToXml = createMockConverter(XML_DATA_TYPE, "stringToXml", STRING_DATA_TYPE);
        Transformer stringToJson = createMockConverter(JSON_DATA_TYPE, "stringToJson", STRING_DATA_TYPE);

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

    private Transformer createMockTransformer(DataType returnType, String name, DataType... sourceTypes)
    {

        Transformer transformer;
        if (name == null || name.isEmpty())
        {

            transformer = mock(Transformer.class);
        }
        else {
            transformer = mock(Transformer.class, name);
        }

        doReturn(returnType).when(transformer).getReturnDataType();
        doReturn(Arrays.asList(sourceTypes)).when(transformer).getSourceDataTypes();

        return transformer;
    }

    private Transformer createMockConverter(DataType returnType, String name, DataType... sourceTypes)
    {
        MockConverter converter;
        if (name == null || name.isEmpty())
        {

            converter = mock(MockConverter.class);
        }
        else {
            converter = mock(MockConverter.class, name);
        }

        doReturn(returnType).when(converter).getReturnDataType();
        doReturn(Arrays.asList(sourceTypes)).when(converter).getSourceDataTypes();

        return converter;
    }

    private Transformer createMockConverter(int weight, DataType returnType, String name, DataType... sourceTypes)
    {
        MockConverter converter;
        if (name == null || name.isEmpty())
        {

            converter = mock(MockConverter.class);
        }
        else {
            converter = mock(MockConverter.class, name);
        }

        doReturn(returnType).when(converter).getReturnDataType();
        doReturn(Arrays.asList(sourceTypes)).when(converter).getSourceDataTypes();
        doReturn(weight).when(converter).getPriorityWeighting();

        return converter;
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

    private interface MockConverter extends Transformer, Converter
    {

    }
}
