/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.transformer.graph;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.mule.runtime.core.api.registry.ResolverException;
import org.mule.runtime.core.api.transformer.Converter;
import org.mule.runtime.core.api.transformer.Transformer;
import org.mule.tck.size.SmallTest;
import org.mule.runtime.core.privileged.transformer.CompositeConverter;
import org.mule.runtime.core.internal.registry.TransformerResolver;
import org.mule.runtime.core.internal.transformer.builder.MockConverterBuilder;
import org.mule.runtime.core.internal.transformer.builder.MockTransformerBuilder;

import org.junit.Test;

@SmallTest
public class GraphTransformerResolverTestCase extends AbstractTransformationGraphTestCase {


  private GraphTransformerResolver graphResolver = new GraphTransformerResolver();

  @Test
  public void cachesResolvedTransformer() throws ResolverException {
    Converter xmlToJson = new MockConverterBuilder().from(XML_DATA_TYPE).to(JSON_DATA_TYPE).build();
    Converter inputStreamToXml = new MockConverterBuilder().from(INPUT_STREAM_DATA_TYPE).to(XML_DATA_TYPE).build();

    graphResolver.transformerChange(inputStreamToXml, TransformerResolver.RegistryAction.ADDED);
    graphResolver.transformerChange(xmlToJson, TransformerResolver.RegistryAction.ADDED);

    Transformer transformer1 = graphResolver.resolve(INPUT_STREAM_DATA_TYPE, JSON_DATA_TYPE);
    Transformer transformer2 = graphResolver.resolve(INPUT_STREAM_DATA_TYPE, JSON_DATA_TYPE);

    assertSame(transformer1, transformer2);
  }

  @Test
  public void clearsCacheWhenAddsConverter() throws ResolverException {
    Converter xmlToJson = new MockConverterBuilder().named("xmlToJson").from(XML_DATA_TYPE).to(JSON_DATA_TYPE).build();
    Converter inputStreamToXml =
        new MockConverterBuilder().named("inputStreamToXml").from(INPUT_STREAM_DATA_TYPE).to(XML_DATA_TYPE).build();

    graphResolver.transformerChange(inputStreamToXml, TransformerResolver.RegistryAction.ADDED);
    graphResolver.transformerChange(xmlToJson, TransformerResolver.RegistryAction.ADDED);

    Transformer transformer1 = graphResolver.resolve(INPUT_STREAM_DATA_TYPE, JSON_DATA_TYPE);
    assertNotNull(transformer1);

    Converter xmlToString = new MockConverterBuilder().named("xmlToString").from(XML_DATA_TYPE).to(STRING_DATA_TYPE).build();
    graphResolver.transformerChange(xmlToString, TransformerResolver.RegistryAction.ADDED);

    Transformer transformer2 = graphResolver.resolve(INPUT_STREAM_DATA_TYPE, JSON_DATA_TYPE);
    assertNotSame(transformer1, transformer2);
  }

  @Test
  public void ignoresAddedTransformer() throws ResolverException {
    Converter xmlToJson = new MockConverterBuilder().named("xmlToJson").from(XML_DATA_TYPE).to(JSON_DATA_TYPE).build();
    Converter inputStreamToXml =
        new MockConverterBuilder().named("inputStreamToXml").from(INPUT_STREAM_DATA_TYPE).to(XML_DATA_TYPE).build();

    graphResolver.transformerChange(inputStreamToXml, TransformerResolver.RegistryAction.ADDED);
    graphResolver.transformerChange(xmlToJson, TransformerResolver.RegistryAction.ADDED);

    Transformer transformer1 = graphResolver.resolve(INPUT_STREAM_DATA_TYPE, JSON_DATA_TYPE);
    assertNotNull(transformer1);

    Transformer xmlToString = new MockTransformerBuilder().named("xmlToString").from(XML_DATA_TYPE).to(STRING_DATA_TYPE).build();
    graphResolver.transformerChange(xmlToString, TransformerResolver.RegistryAction.ADDED);

    Transformer transformer2 = graphResolver.resolve(INPUT_STREAM_DATA_TYPE, JSON_DATA_TYPE);
    assertSame(transformer1, transformer2);
  }

  @Test
  public void ignoresRemovedTransformer() throws ResolverException {
    Converter xmlToJson = new MockConverterBuilder().named("xmlToJson").from(XML_DATA_TYPE).to(JSON_DATA_TYPE).build();
    Converter inputStreamToXml =
        new MockConverterBuilder().named("inputStreamToXml").from(INPUT_STREAM_DATA_TYPE).to(XML_DATA_TYPE).build();

    graphResolver.transformerChange(inputStreamToXml, TransformerResolver.RegistryAction.ADDED);
    graphResolver.transformerChange(xmlToJson, TransformerResolver.RegistryAction.ADDED);

    Transformer transformer1 = graphResolver.resolve(INPUT_STREAM_DATA_TYPE, JSON_DATA_TYPE);
    assertNotNull(transformer1);

    Transformer xmlToString = new MockTransformerBuilder().named("xmlToString").from(XML_DATA_TYPE).to(STRING_DATA_TYPE).build();
    graphResolver.transformerChange(xmlToString, TransformerResolver.RegistryAction.REMOVED);

    Transformer transformer2 = graphResolver.resolve(INPUT_STREAM_DATA_TYPE, JSON_DATA_TYPE);
    assertSame(transformer1, transformer2);
  }

  @Test
  public void clearsCacheWhenRemovesTransformer() throws ResolverException {
    Converter xmlToJson = new MockConverterBuilder().from(XML_DATA_TYPE).to(JSON_DATA_TYPE).build();
    Converter inputStreamToXml = new MockConverterBuilder().from(INPUT_STREAM_DATA_TYPE).to(XML_DATA_TYPE).build();

    graphResolver.transformerChange(inputStreamToXml, TransformerResolver.RegistryAction.ADDED);
    graphResolver.transformerChange(xmlToJson, TransformerResolver.RegistryAction.ADDED);

    Transformer transformer = graphResolver.resolve(INPUT_STREAM_DATA_TYPE, JSON_DATA_TYPE);
    assertNotNull(transformer);

    graphResolver.transformerChange(inputStreamToXml, TransformerResolver.RegistryAction.REMOVED);

    transformer = graphResolver.resolve(INPUT_STREAM_DATA_TYPE, JSON_DATA_TYPE);

    assertNull(transformer);
  }

  @Test
  public void resolvesTransformersWithDifferentLength() throws ResolverException {
    Converter xmlToInputStream =
        new MockConverterBuilder().named("xmlToInputStream").from(XML_DATA_TYPE).to(INPUT_STREAM_DATA_TYPE).weighting(1).build();
    Converter xmlToJson =
        new MockConverterBuilder().named("xmlToJson").from(XML_DATA_TYPE).to(JSON_DATA_TYPE).weighting(1).build();
    Converter inputStreamToJson = new MockConverterBuilder().named("inputStreamToJson").from(INPUT_STREAM_DATA_TYPE)
        .to(JSON_DATA_TYPE).weighting(1).build();

    graphResolver.transformerChange(xmlToInputStream, TransformerResolver.RegistryAction.ADDED);
    graphResolver.transformerChange(xmlToJson, TransformerResolver.RegistryAction.ADDED);
    graphResolver.transformerChange(inputStreamToJson, TransformerResolver.RegistryAction.ADDED);

    Transformer transformer = graphResolver.resolve(XML_DATA_TYPE, JSON_DATA_TYPE);

    assertEquals(xmlToJson, transformer);
  }

  @Test
  public void resolvesTransformersWithSameLengthAndDifferentWeight() throws ResolverException {
    Converter xmlToInputStream =
        new MockConverterBuilder().named("xmlToInputStream").from(XML_DATA_TYPE).to(INPUT_STREAM_DATA_TYPE).weighting(1).build();
    Converter xmlToString =
        new MockConverterBuilder().named("xmlToString").from(XML_DATA_TYPE).to(STRING_DATA_TYPE).weighting(1).build();
    Converter inputStreamToJson = new MockConverterBuilder().named("inputStreamToJson").from(INPUT_STREAM_DATA_TYPE)
        .to(JSON_DATA_TYPE).weighting(2).build();
    Converter stringToJson =
        new MockConverterBuilder().named("stringToJson").from(STRING_DATA_TYPE).to(JSON_DATA_TYPE).weighting(1).build();

    graphResolver.transformerChange(xmlToInputStream, TransformerResolver.RegistryAction.ADDED);
    graphResolver.transformerChange(xmlToString, TransformerResolver.RegistryAction.ADDED);
    graphResolver.transformerChange(inputStreamToJson, TransformerResolver.RegistryAction.ADDED);
    graphResolver.transformerChange(stringToJson, TransformerResolver.RegistryAction.ADDED);

    Transformer transformer = graphResolver.resolve(XML_DATA_TYPE, JSON_DATA_TYPE);

    assertTrue(transformer instanceof CompositeConverter);
    CompositeConverter compositeConverter = (CompositeConverter) transformer;
    assertEquals(2, compositeConverter.getConverters().size());
    assertEquals(xmlToInputStream, compositeConverter.getConverters().get(0));
    assertEquals(inputStreamToJson, compositeConverter.getConverters().get(1));
  }

  @Test
  public void resolvesTransformerWithSameLengthAndSameWeight() throws ResolverException {
    Converter xmlToInputStream =
        new MockConverterBuilder().named("xmlToInputStream").from(XML_DATA_TYPE).to(INPUT_STREAM_DATA_TYPE).weighting(1).build();
    Converter xmlToString =
        new MockConverterBuilder().named("xmlToString").from(XML_DATA_TYPE).to(STRING_DATA_TYPE).weighting(1).build();
    Converter inputStreamToJson = new MockConverterBuilder().named("inputStreamToJson").from(INPUT_STREAM_DATA_TYPE)
        .to(JSON_DATA_TYPE).weighting(1).build();
    Converter stringToJson =
        new MockConverterBuilder().named("stringToJson").from(STRING_DATA_TYPE).to(JSON_DATA_TYPE).weighting(1).build();

    graphResolver.transformerChange(xmlToInputStream, TransformerResolver.RegistryAction.ADDED);
    graphResolver.transformerChange(xmlToString, TransformerResolver.RegistryAction.ADDED);
    graphResolver.transformerChange(inputStreamToJson, TransformerResolver.RegistryAction.ADDED);
    graphResolver.transformerChange(stringToJson, TransformerResolver.RegistryAction.ADDED);

    Transformer transformer = graphResolver.resolve(XML_DATA_TYPE, JSON_DATA_TYPE);

    assertTrue(transformer instanceof CompositeConverter);
    CompositeConverter compositeConverter = (CompositeConverter) transformer;
    assertEquals(2, compositeConverter.getConverters().size());
    assertEquals(xmlToInputStream, compositeConverter.getConverters().get(0));
    assertEquals(inputStreamToJson, compositeConverter.getConverters().get(1));
  }

  @Test(expected = ResolverException.class)
  public void cannotResolveTransformerWithSameLengthAndSameWeightAndSameName() throws ResolverException {
    Converter xmlToInputStream1 =
        new MockConverterBuilder().named("xmlToInputStream").from(XML_DATA_TYPE).to(INPUT_STREAM_DATA_TYPE).weighting(1).build();
    Converter xmlToInputStream2 =
        new MockConverterBuilder().named("xmlToInputStream").from(XML_DATA_TYPE).to(INPUT_STREAM_DATA_TYPE).weighting(1).build();

    graphResolver.transformerChange(xmlToInputStream1, TransformerResolver.RegistryAction.ADDED);
    graphResolver.transformerChange(xmlToInputStream2, TransformerResolver.RegistryAction.ADDED);

    graphResolver.resolve(XML_DATA_TYPE, INPUT_STREAM_DATA_TYPE);
  }
}
