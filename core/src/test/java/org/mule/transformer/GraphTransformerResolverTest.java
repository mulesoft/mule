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
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import org.mule.api.registry.ResolverException;
import org.mule.api.registry.TransformerResolver;
import org.mule.api.transformer.DataType;
import org.mule.api.transformer.DiscoverableTransformer;
import org.mule.api.transformer.Transformer;
import org.mule.tck.size.SmallTest;

import java.util.Arrays;
import java.util.Set;

import org.junit.BeforeClass;
import org.junit.Test;

@SmallTest
public class GraphTransformerResolverTest
{

    private static final DataType XML_DATA_TYPE = mock(DataType.class, "XML_DATA_TYPE");
    private static final DataType JSON_DATA_TYPE = mock(DataType.class, "JSON_DATA_TYPE");
    private static final DataType OBJECT_DATA_TYPE = mock(DataType.class, "OBJECT_DATA_TYPE");

    @BeforeClass
    public static void setupDataTypes()
    {
        doReturn(true).when(XML_DATA_TYPE).isCompatibleWith(XML_DATA_TYPE);
        doReturn(true).when(JSON_DATA_TYPE).isCompatibleWith(JSON_DATA_TYPE);
        doReturn(true).when(OBJECT_DATA_TYPE).isCompatibleWith(OBJECT_DATA_TYPE);
    }

    @Test
    public void ignoresTransformerAdded()
    {
        Transformer xmlToJson = createMockTransformer(JSON_DATA_TYPE, XML_DATA_TYPE);

        GraphTransformerResolver graphResolver = new GraphTransformerResolver();
        graphResolver.transformerChange(xmlToJson, TransformerResolver.RegistryAction.ADDED);

        assertEquals(0, graphResolver.graph.vertexSet().size());
        assertEquals(0, graphResolver.graph.edgeSet().size());
    }

    @Test
    public void ignoresTransformerRemoved()
    {
        Transformer xmlToJson = createMockTransformer(JSON_DATA_TYPE, XML_DATA_TYPE);

        GraphTransformerResolver graphResolver = new GraphTransformerResolver();
        graphResolver.transformerChange(xmlToJson, TransformerResolver.RegistryAction.ADDED);
        graphResolver.transformerChange(xmlToJson, TransformerResolver.RegistryAction.REMOVED);

        assertEquals(0, graphResolver.graph.vertexSet().size());
        assertEquals(0, graphResolver.graph.edgeSet().size());
    }

    @Test
    public void processesConverterAdded()
    {
        Transformer xmlToJson = createMockConverter(JSON_DATA_TYPE, XML_DATA_TYPE);

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
        Transformer xmlToJson = createMockConverter(JSON_DATA_TYPE, XML_DATA_TYPE);

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
        Transformer xmlToJson = createMockConverter(JSON_DATA_TYPE, XML_DATA_TYPE, OBJECT_DATA_TYPE);

        GraphTransformerResolver graphResolver = new GraphTransformerResolver();
        graphResolver.transformerChange(xmlToJson, TransformerResolver.RegistryAction.ADDED);

        assertEquals(3, graphResolver.graph.vertexSet().size());
        assertTrue(graphResolver.graph.containsVertex(XML_DATA_TYPE));
        assertTrue(graphResolver.graph.containsVertex(JSON_DATA_TYPE));
        assertTrue(graphResolver.graph.containsVertex(OBJECT_DATA_TYPE));

        assertEquals(2, graphResolver.graph.edgeSet().size());
        assertTrue(graphResolver.graph.containsEdge(XML_DATA_TYPE, JSON_DATA_TYPE));
        assertFalse(graphResolver.graph.containsEdge(JSON_DATA_TYPE, XML_DATA_TYPE));

        assertTrue(graphResolver.graph.containsEdge(OBJECT_DATA_TYPE, JSON_DATA_TYPE));
        assertFalse(graphResolver.graph.containsEdge(JSON_DATA_TYPE, OBJECT_DATA_TYPE));
    }

    @Test
    public void processesConverterRemoved()
    {
        Transformer xmlToJson = createMockConverter(JSON_DATA_TYPE, XML_DATA_TYPE);

        GraphTransformerResolver graphResolver = new GraphTransformerResolver();
        graphResolver.transformerChange(xmlToJson, TransformerResolver.RegistryAction.ADDED);
        graphResolver.transformerChange(xmlToJson, TransformerResolver.RegistryAction.REMOVED);

        assertEquals(0, graphResolver.graph.vertexSet().size());
        assertEquals(0, graphResolver.graph.edgeSet().size());
    }

    @Test
    public void ignoresRemovingConverterThatWasNeverAdded()
    {
        Transformer xmlToJson = createMockConverter(1, JSON_DATA_TYPE, XML_DATA_TYPE);
        Transformer betterXmlToJson = createMockConverter(2, JSON_DATA_TYPE, XML_DATA_TYPE);

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
        Transformer xmlToJson = createMockConverter(JSON_DATA_TYPE, XML_DATA_TYPE, OBJECT_DATA_TYPE);

        GraphTransformerResolver graphResolver = new GraphTransformerResolver();
        graphResolver.transformerChange(xmlToJson, TransformerResolver.RegistryAction.ADDED);
        graphResolver.transformerChange(xmlToJson, TransformerResolver.RegistryAction.REMOVED);

        assertEquals(0, graphResolver.graph.vertexSet().size());
        assertEquals(0, graphResolver.graph.edgeSet().size());
    }

    @Test
    public void multipleConvertersFromSameSourceToResultTypes()
    {
        Transformer xmlToJson = createMockConverter(JSON_DATA_TYPE, XML_DATA_TYPE);
        Transformer betterXmlToJson = createMockConverter(JSON_DATA_TYPE, XML_DATA_TYPE);

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
        Transformer xmlToJson = createMockConverter(JSON_DATA_TYPE, XML_DATA_TYPE);
        Transformer betterXmlToJson = createMockConverter(JSON_DATA_TYPE, XML_DATA_TYPE);

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
        Transformer xmlToJson = createMockConverter(JSON_DATA_TYPE, XML_DATA_TYPE);
        Transformer betterXmlToJson = createMockConverter(JSON_DATA_TYPE, XML_DATA_TYPE);

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
        Transformer xmlToJson = createMockConverter(JSON_DATA_TYPE, XML_DATA_TYPE);
        Transformer objectToJson = createMockConverter(JSON_DATA_TYPE, OBJECT_DATA_TYPE);

        GraphTransformerResolver graphResolver = new GraphTransformerResolver();
        graphResolver.transformerChange(xmlToJson, TransformerResolver.RegistryAction.ADDED);
        graphResolver.transformerChange(objectToJson, TransformerResolver.RegistryAction.ADDED);

        assertEquals(3, graphResolver.graph.vertexSet().size());
        assertTrue(graphResolver.graph.containsVertex(XML_DATA_TYPE));
        assertTrue(graphResolver.graph.containsVertex(JSON_DATA_TYPE));
        assertTrue(graphResolver.graph.containsVertex(OBJECT_DATA_TYPE));

        assertEquals(2, graphResolver.graph.edgeSet().size());
        assertTrue(graphResolver.graph.containsEdge(XML_DATA_TYPE, JSON_DATA_TYPE));
        assertFalse(graphResolver.graph.containsEdge(JSON_DATA_TYPE, XML_DATA_TYPE));
        assertTrue(graphResolver.graph.containsEdge(OBJECT_DATA_TYPE, JSON_DATA_TYPE));
        assertFalse(graphResolver.graph.containsEdge(JSON_DATA_TYPE, OBJECT_DATA_TYPE));
    }

    @Test
    public void removeFirstAddedConverterWithDifferentSourceToSameResultTypes()
    {
        Transformer xmlToJson = createMockConverter(JSON_DATA_TYPE, XML_DATA_TYPE);
        Transformer objectToJson = createMockConverter(JSON_DATA_TYPE, OBJECT_DATA_TYPE);

        GraphTransformerResolver graphResolver = new GraphTransformerResolver();
        graphResolver.transformerChange(xmlToJson, TransformerResolver.RegistryAction.ADDED);
        graphResolver.transformerChange(objectToJson, TransformerResolver.RegistryAction.ADDED);

        graphResolver.transformerChange(xmlToJson, TransformerResolver.RegistryAction.REMOVED);

        assertEquals(2, graphResolver.graph.vertexSet().size());
        assertTrue(graphResolver.graph.containsVertex(JSON_DATA_TYPE));
        assertTrue(graphResolver.graph.containsVertex(OBJECT_DATA_TYPE));

        assertEquals(1, graphResolver.graph.edgeSet().size());
        assertFalse(graphResolver.graph.containsEdge(XML_DATA_TYPE, JSON_DATA_TYPE));
        assertFalse(graphResolver.graph.containsEdge(JSON_DATA_TYPE, XML_DATA_TYPE));
        assertTrue(graphResolver.graph.containsEdge(OBJECT_DATA_TYPE, JSON_DATA_TYPE));
        assertFalse(graphResolver.graph.containsEdge(JSON_DATA_TYPE, OBJECT_DATA_TYPE));
    }

    @Test
    public void removeSecondAddedConverterWithDifferentSourceToSameResultTypes()
    {
        Transformer xmlToJson = createMockConverter(JSON_DATA_TYPE, XML_DATA_TYPE);
        Transformer objectToJson = createMockConverter(JSON_DATA_TYPE, OBJECT_DATA_TYPE);

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
        assertFalse(graphResolver.graph.containsEdge(OBJECT_DATA_TYPE, JSON_DATA_TYPE));
        assertFalse(graphResolver.graph.containsEdge(JSON_DATA_TYPE, OBJECT_DATA_TYPE));
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
        Transformer xmlToJson = createMockTransformer(JSON_DATA_TYPE, XML_DATA_TYPE);

        GraphTransformerResolver graphResolver = new GraphTransformerResolver();
        graphResolver.transformerChange(xmlToJson, TransformerResolver.RegistryAction.ADDED);

        Transformer transformer = graphResolver.resolve(XML_DATA_TYPE, OBJECT_DATA_TYPE);

        assertNull(transformer);
    }

    @Test
    public void noTransformerFound() throws ResolverException
    {
        DataType mockStringDataType = mock(DataType.class);

        Transformer xmlToJson = createMockConverter(JSON_DATA_TYPE, XML_DATA_TYPE);
        Transformer objectToXml = createMockConverter(XML_DATA_TYPE, OBJECT_DATA_TYPE);

        GraphTransformerResolver graphResolver = new GraphTransformerResolver();
        graphResolver.transformerChange(objectToXml, TransformerResolver.RegistryAction.ADDED);
        graphResolver.transformerChange(xmlToJson, TransformerResolver.RegistryAction.ADDED);

        Transformer transformer = graphResolver.resolve(OBJECT_DATA_TYPE, mockStringDataType);

        assertNull(transformer);
    }

    @Test
    public void resolvesDirectTransformation() throws ResolverException
    {
        Transformer xmlToJson = createMockConverter(JSON_DATA_TYPE, XML_DATA_TYPE);

        GraphTransformerResolver graphResolver = new GraphTransformerResolver();
        graphResolver.transformerChange(xmlToJson, TransformerResolver.RegistryAction.ADDED);

        Transformer transformer = graphResolver.resolve(XML_DATA_TYPE, JSON_DATA_TYPE);

        assertEquals(xmlToJson, transformer);
    }

    @Test
    public void resolvesMultipleDirectTransformationAddingBetterTransformerFirst() throws ResolverException
    {
        Transformer betterXmlToJson = createMockConverter(2, JSON_DATA_TYPE, XML_DATA_TYPE);
        Transformer xmlToJson = createMockConverter(1, JSON_DATA_TYPE, XML_DATA_TYPE);

        GraphTransformerResolver graphResolver = new GraphTransformerResolver();
        graphResolver.transformerChange(betterXmlToJson, TransformerResolver.RegistryAction.ADDED);
        graphResolver.transformerChange(xmlToJson, TransformerResolver.RegistryAction.ADDED);

        Transformer transformer = graphResolver.resolve(XML_DATA_TYPE, JSON_DATA_TYPE);

        assertEquals(betterXmlToJson, transformer);
    }

    @Test
    public void resolvesMultipleDirectTransformationAddingBetterTransformerLast() throws ResolverException
    {
        Transformer betterXmlToJson = createMockConverter(2, JSON_DATA_TYPE, XML_DATA_TYPE);
        Transformer xmlToJson = createMockConverter(1, JSON_DATA_TYPE, XML_DATA_TYPE);

        GraphTransformerResolver graphResolver = new GraphTransformerResolver();
        graphResolver.transformerChange(xmlToJson, TransformerResolver.RegistryAction.ADDED);
        graphResolver.transformerChange(betterXmlToJson, TransformerResolver.RegistryAction.ADDED);

        Transformer transformer = graphResolver.resolve(XML_DATA_TYPE, JSON_DATA_TYPE);

        assertEquals(betterXmlToJson, transformer);
    }

    @Test
    public void resolvesTransformationChain() throws ResolverException
    {
        Transformer xmlToJson = createMockConverter(JSON_DATA_TYPE, XML_DATA_TYPE);
        Transformer objectToXml = createMockConverter(XML_DATA_TYPE, OBJECT_DATA_TYPE);

        GraphTransformerResolver graphResolver = new GraphTransformerResolver();
        graphResolver.transformerChange(objectToXml, TransformerResolver.RegistryAction.ADDED);
        graphResolver.transformerChange(xmlToJson, TransformerResolver.RegistryAction.ADDED);

        Transformer transformer = graphResolver.resolve(OBJECT_DATA_TYPE, JSON_DATA_TYPE);

        assertTrue(transformer instanceof CompositeTransformer);
        CompositeTransformer compositeTransformer = (CompositeTransformer) transformer;
        assertEquals(2, compositeTransformer.chain.size());
        assertEquals(objectToXml, compositeTransformer.chain.get(0));
        assertEquals(xmlToJson, compositeTransformer.chain.get(1));
    }

    @Test
    public void resolvesTransformationChainWithMultipleEdgesFromSource() throws ResolverException
    {
        setupDataTypes();

        Transformer xmlToJson = createMockConverter(1, JSON_DATA_TYPE, XML_DATA_TYPE);
        Transformer betterXmlToJson = createMockConverter(2, JSON_DATA_TYPE, XML_DATA_TYPE);
        Transformer objectToXml = createMockConverter(1, XML_DATA_TYPE, OBJECT_DATA_TYPE);

        GraphTransformerResolver graphResolver = new GraphTransformerResolver();
        graphResolver.transformerChange(betterXmlToJson, TransformerResolver.RegistryAction.ADDED);
        graphResolver.transformerChange(xmlToJson, TransformerResolver.RegistryAction.ADDED);
        graphResolver.transformerChange(objectToXml, TransformerResolver.RegistryAction.ADDED);

        Transformer transformer = graphResolver.resolve(OBJECT_DATA_TYPE, JSON_DATA_TYPE);

        assertTrue(transformer instanceof CompositeTransformer);
        CompositeTransformer chain = (CompositeTransformer) transformer;
        assertEquals(2, chain.chain.size());
        assertEquals(objectToXml, chain.chain.get(0));
        assertEquals(betterXmlToJson, chain.chain.get(1));
    }

    @Test
    public void resolvesTransformationChainWithMultipleEdgesToResult() throws ResolverException
    {
        Transformer xmlToJson = createMockConverter(1, JSON_DATA_TYPE, XML_DATA_TYPE);
        Transformer objectToXml = createMockConverter(1, XML_DATA_TYPE, OBJECT_DATA_TYPE);
        Transformer mockBetterTransformerObjectToXml = createMockConverter(2, XML_DATA_TYPE, OBJECT_DATA_TYPE);

        GraphTransformerResolver graphResolver = new GraphTransformerResolver();
        graphResolver.transformerChange(objectToXml, TransformerResolver.RegistryAction.ADDED);
        graphResolver.transformerChange(xmlToJson, TransformerResolver.RegistryAction.ADDED);
        graphResolver.transformerChange(mockBetterTransformerObjectToXml, TransformerResolver.RegistryAction.ADDED);

        Transformer transformer = graphResolver.resolve(OBJECT_DATA_TYPE, JSON_DATA_TYPE);

        assertTrue(transformer instanceof CompositeTransformer);
        CompositeTransformer chain = (CompositeTransformer) transformer;
        assertEquals(2, chain.chain.size());
        assertEquals(mockBetterTransformerObjectToXml, chain.chain.get(0));
        assertEquals(xmlToJson, chain.chain.get(1));
    }

    @Test
    public void resolvesTransformationChainWithMultipleEdges() throws ResolverException
    {
        Transformer xmlToJson = createMockConverter(1, JSON_DATA_TYPE, XML_DATA_TYPE);
        Transformer betterXmlToJson = createMockConverter(2, JSON_DATA_TYPE, XML_DATA_TYPE);
        Transformer objectToXml = createMockConverter(1, XML_DATA_TYPE, OBJECT_DATA_TYPE);
        Transformer mockBetterTransformerObjectToXml = createMockConverter(2, XML_DATA_TYPE, OBJECT_DATA_TYPE);

        GraphTransformerResolver graphResolver = new GraphTransformerResolver();
        graphResolver.transformerChange(mockBetterTransformerObjectToXml, TransformerResolver.RegistryAction.ADDED);
        graphResolver.transformerChange(objectToXml, TransformerResolver.RegistryAction.ADDED);
        graphResolver.transformerChange(xmlToJson, TransformerResolver.RegistryAction.ADDED);
        graphResolver.transformerChange(betterXmlToJson, TransformerResolver.RegistryAction.ADDED);

        Transformer transformer = graphResolver.resolve(OBJECT_DATA_TYPE, JSON_DATA_TYPE);

        assertTrue(transformer instanceof CompositeTransformer);
        CompositeTransformer chain = (CompositeTransformer) transformer;
        assertEquals(2, chain.chain.size());
        assertEquals(mockBetterTransformerObjectToXml, chain.chain.get(0));
        assertEquals(betterXmlToJson, chain.chain.get(1));
    }

    @Test
    public void clearsCacheWhenAddsTransformer() throws ResolverException
    {
        Transformer xmlToJson = createMockConverter(1, JSON_DATA_TYPE, XML_DATA_TYPE);
        Transformer objectToXml = createMockConverter(1, XML_DATA_TYPE, OBJECT_DATA_TYPE);

        GraphTransformerResolver graphResolver = new GraphTransformerResolver();
        graphResolver.transformerChange(objectToXml, TransformerResolver.RegistryAction.ADDED);
        graphResolver.transformerChange(xmlToJson, TransformerResolver.RegistryAction.ADDED);

        Transformer transformer = graphResolver.resolve(OBJECT_DATA_TYPE, JSON_DATA_TYPE);
        assertNotNull(transformer);

        Transformer betterXmlToJson = createMockConverter(2, JSON_DATA_TYPE, XML_DATA_TYPE);
        graphResolver.transformerChange(betterXmlToJson, TransformerResolver.RegistryAction.ADDED);

        transformer = graphResolver.resolve(OBJECT_DATA_TYPE, JSON_DATA_TYPE);
        assertTrue(transformer instanceof CompositeTransformer);
        CompositeTransformer chain = (CompositeTransformer) transformer;
        assertEquals(2, chain.chain.size());
        assertEquals(objectToXml, chain.chain.get(0));
        assertEquals(betterXmlToJson, chain.chain.get(1));
    }

    @Test
    public void clearsCacheWhenRemovesTransformer() throws ResolverException
    {
        Transformer xmlToJson = createMockConverter(JSON_DATA_TYPE, XML_DATA_TYPE);

        Transformer objectToXml = createMockConverter(XML_DATA_TYPE, OBJECT_DATA_TYPE);

        GraphTransformerResolver graphResolver = new GraphTransformerResolver();
        graphResolver.transformerChange(objectToXml, TransformerResolver.RegistryAction.ADDED);
        graphResolver.transformerChange(xmlToJson, TransformerResolver.RegistryAction.ADDED);

        Transformer transformer = graphResolver.resolve(OBJECT_DATA_TYPE, JSON_DATA_TYPE);
        assertNotNull(transformer);

        graphResolver.transformerChange(objectToXml, TransformerResolver.RegistryAction.REMOVED);
        graphResolver.transformerChange(xmlToJson, TransformerResolver.RegistryAction.REMOVED);

        transformer = graphResolver.resolve(OBJECT_DATA_TYPE, JSON_DATA_TYPE);

        assertNull(transformer);
    }

    private Transformer createMockTransformer(DataType returnType, DataType... sourceTypes)
    {
        Transformer transformer = mock(Transformer.class);
        doReturn(returnType).when(transformer).getReturnDataType();
        doReturn(Arrays.asList(sourceTypes)).when(transformer).getSourceDataTypes();

        return transformer;
    }

    private Transformer createMockConverter(DataType returnType, DataType... sourceTypes)
    {
        Transformer converter = mock(MockConverter.class);
        doReturn(returnType).when(converter).getReturnDataType();
        doReturn(Arrays.asList(sourceTypes)).when(converter).getSourceDataTypes();

        return converter;
    }

    private Transformer createMockConverter(int weight, DataType returnType, DataType... sourceTypes)
    {
        MockConverter converter = mock(MockConverter.class);
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

    private interface MockConverter extends Transformer, DiscoverableTransformer
    {

    }
}
