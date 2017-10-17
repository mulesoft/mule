/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.transformer.graph;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

import org.mule.runtime.core.api.registry.ResolverException;
import org.mule.runtime.core.api.transformer.Converter;
import org.mule.tck.size.SmallTest;
import org.mule.runtime.core.privileged.transformer.CompositeConverter;
import org.mule.runtime.core.internal.transformer.builder.MockConverterBuilder;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

@SmallTest
public class ShortestTransformationLengthTransformerFilterTestCase extends AbstractTransformationGraphTestCase {

  private TransformationLengthConverterFilter filter = new TransformationLengthConverterFilter();

  @Test
  public void filtersEmptyList() throws ResolverException {

    List<Converter> availableConverters = new ArrayList<Converter>();

    List<Converter> converters = filter.filter(availableConverters, UNUSED_DATA_TYPE, UNUSED_DATA_TYPE);

    assertEquals(0, converters.size());
  }

  @Test
  public void filtersEqualLength() throws ResolverException {
    Converter xmlToString = new MockConverterBuilder().named("xmlToString").from(XML_DATA_TYPE).to(STRING_DATA_TYPE).build();
    Converter xmlToJson = new MockConverterBuilder().named("xmlToJson").from(XML_DATA_TYPE).to(JSON_DATA_TYPE).build();

    List<Converter> availableConverters = new ArrayList<Converter>();
    availableConverters.add(xmlToString);
    availableConverters.add(xmlToJson);

    List<Converter> transformers = filter.filter(availableConverters, UNUSED_DATA_TYPE, UNUSED_DATA_TYPE);

    assertEquals(2, transformers.size());
    assertTrue(transformers.contains(xmlToString));
    assertTrue(transformers.contains(xmlToJson));
  }

  @Test
  public void filtersDifferentLengthWithBetterTrasnformerFist() throws ResolverException {
    Converter xmlToString = new MockConverterBuilder().named("xmlToString").from(XML_DATA_TYPE).to(STRING_DATA_TYPE).build();
    Converter stringToJson = new MockConverterBuilder().named("stringToJson").from(STRING_DATA_TYPE).to(JSON_DATA_TYPE).build();

    List<Converter> availableConverters = new ArrayList<Converter>();
    availableConverters.add(xmlToString);
    CompositeConverter compositeConverter = new CompositeConverter(xmlToString, stringToJson);
    availableConverters.add(compositeConverter);

    List<Converter> transformers = filter.filter(availableConverters, UNUSED_DATA_TYPE, UNUSED_DATA_TYPE);

    assertEquals(1, transformers.size());
    assertEquals(xmlToString, transformers.get(0));
  }

  @Test
  public void filtersDifferentLengthWithBetterTransformerLast() throws ResolverException {
    Converter xmlToString = new MockConverterBuilder().named("xmlToString").from(XML_DATA_TYPE).to(STRING_DATA_TYPE).build();
    Converter stringToJson = new MockConverterBuilder().named("stringToJson").from(STRING_DATA_TYPE).to(JSON_DATA_TYPE).build();

    List<Converter> availableConverters = new ArrayList<Converter>();
    CompositeConverter compositeConverter = new CompositeConverter(xmlToString, stringToJson);
    availableConverters.add(compositeConverter);
    availableConverters.add(xmlToString);

    List<Converter> transformers = filter.filter(availableConverters, UNUSED_DATA_TYPE, UNUSED_DATA_TYPE);

    assertEquals(1, transformers.size());
    assertEquals(xmlToString, transformers.get(0));
  }
}
