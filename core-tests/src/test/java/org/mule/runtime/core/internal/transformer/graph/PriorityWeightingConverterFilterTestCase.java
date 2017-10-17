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
import org.mule.runtime.core.internal.transformer.builder.MockConverterBuilder;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

@SmallTest
public class PriorityWeightingConverterFilterTestCase extends AbstractTransformationGraphTestCase {

  private PriorityWeightingConverterFilter filter = new PriorityWeightingConverterFilter();

  @Test
  public void filtersEmptyList() throws ResolverException {
    List<Converter> availableConverters = new ArrayList<Converter>();

    List<Converter> converters = filter.filter(availableConverters, XML_DATA_TYPE, JSON_DATA_TYPE);

    assertEquals(0, converters.size());
  }

  @Test
  public void filtersSameWeight() throws ResolverException {
    Converter xmlToInputStream =
        new MockConverterBuilder().named("xmlToInputStream").from(XML_DATA_TYPE).to(INPUT_STREAM_DATA_TYPE).weighting(1).build();
    Converter xmlToString =
        new MockConverterBuilder().named("xmlToString").from(XML_DATA_TYPE).to(STRING_DATA_TYPE).weighting(1).build();

    List<Converter> availableConverters = new ArrayList<Converter>();
    availableConverters.add(xmlToInputStream);
    availableConverters.add(xmlToString);

    List<Converter> converters = filter.filter(availableConverters, XML_DATA_TYPE, JSON_DATA_TYPE);

    assertEquals(2, converters.size());
    assertTrue(converters.contains(xmlToInputStream));
    assertTrue(converters.contains(xmlToString));
  }

  @Test
  public void filtersSameLengthDifferentWeightsAddingBetterTransformerFirst() throws ResolverException {
    Converter xmlToInputStream =
        new MockConverterBuilder().named("xmlToInputStream").from(XML_DATA_TYPE).to(INPUT_STREAM_DATA_TYPE).weighting(2).build();
    Converter xmlToString =
        new MockConverterBuilder().named("xmlToString").from(XML_DATA_TYPE).to(STRING_DATA_TYPE).weighting(1).build();

    List<Converter> availableConverters = new ArrayList<Converter>();
    availableConverters.add(xmlToInputStream);
    availableConverters.add(xmlToString);

    List<Converter> converters = filter.filter(availableConverters, XML_DATA_TYPE, JSON_DATA_TYPE);

    assertEquals(1, converters.size());
    assertEquals(xmlToInputStream, converters.get(0));
  }

  @Test
  public void filtersSameLengthDifferentWeightsAddingBetterTransformerLast() throws ResolverException {
    Converter xmlToInputStream =
        new MockConverterBuilder().named("xmlToInputStream").from(XML_DATA_TYPE).to(INPUT_STREAM_DATA_TYPE).weighting(2).build();
    Converter xmlToString =
        new MockConverterBuilder().named("xmlToString").from(XML_DATA_TYPE).to(STRING_DATA_TYPE).weighting(1).build();

    List<Converter> availableConverters = new ArrayList<Converter>();
    availableConverters.add(xmlToString);
    availableConverters.add(xmlToInputStream);

    List<Converter> converters = filter.filter(availableConverters, XML_DATA_TYPE, JSON_DATA_TYPE);

    assertEquals(1, converters.size());
    assertEquals(xmlToInputStream, converters.get(0));
  }
}
