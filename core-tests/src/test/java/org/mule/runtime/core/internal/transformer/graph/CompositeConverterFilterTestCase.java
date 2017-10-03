/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.transformer.graph;

import static junit.framework.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.mule.runtime.core.api.registry.ResolverException;
import org.mule.runtime.core.api.transformer.Converter;
import org.mule.runtime.api.metadata.DataType;
import org.mule.tck.size.SmallTest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Test;
import org.mockito.Mockito;

@SmallTest
public class CompositeConverterFilterTestCase extends AbstractTransformationGraphTestCase {

  @Test
  public void filtersEmptyList() throws ResolverException {
    List<Converter> transformers = Collections.<Converter>emptyList();
    ConverterFilter filter1 = Mockito.mock(ConverterFilter.class);
    ConverterFilter filter2 = Mockito.mock(ConverterFilter.class);

    CompositeConverterFilter compositeConverterFilter = new CompositeConverterFilter(filter1, filter2);
    List<Converter> filteredTransformers = compositeConverterFilter.filter(transformers, XML_DATA_TYPE, JSON_DATA_TYPE);

    assertEquals(transformers, filteredTransformers);
    verify(filter1, times(0)).filter(any(List.class), any(DataType.class), any(DataType.class));
    verify(filter2, times(0)).filter(any(List.class), any(DataType.class), any(DataType.class));
  }

  @Test
  public void filtersSingletonList() throws Exception {
    Converter transformer1 = mock(Converter.class);
    List<Converter> transformers = Collections.singletonList(transformer1);

    ConverterFilter filter1 = Mockito.mock(ConverterFilter.class);
    ConverterFilter filter2 = Mockito.mock(ConverterFilter.class);

    CompositeConverterFilter compositeConverterFilter = new CompositeConverterFilter(filter1, filter2);
    List<Converter> filteredTransformers = compositeConverterFilter.filter(transformers, XML_DATA_TYPE, JSON_DATA_TYPE);

    assertEquals(transformers, filteredTransformers);
    verify(filter1, times(0)).filter(any(List.class), any(DataType.class), any(DataType.class));
    verify(filter2, times(0)).filter(any(List.class), any(DataType.class), any(DataType.class));
  }

  @Test
  public void stopsFilteringWhenGetsOneResult() throws Exception {
    Converter transformer1 = mock(Converter.class);
    Converter transformer2 = mock(Converter.class);

    List<Converter> transformers = new ArrayList<Converter>();
    transformers.add(transformer1);
    transformers.add(transformer2);

    ConverterFilter filter1 = Mockito.mock(ConverterFilter.class);
    List<Converter> expectedFilteredTransformers = Collections.singletonList(transformer1);
    when(filter1.filter(transformers, XML_DATA_TYPE, JSON_DATA_TYPE)).thenReturn(expectedFilteredTransformers);
    ConverterFilter filter2 = Mockito.mock(ConverterFilter.class);

    CompositeConverterFilter compositeConverterFilter = new CompositeConverterFilter(filter1, filter2);
    List<Converter> filteredTransformers = compositeConverterFilter.filter(transformers, XML_DATA_TYPE, JSON_DATA_TYPE);

    assertEquals(expectedFilteredTransformers, filteredTransformers);
    verify(filter2, times(0)).filter(any(List.class), any(DataType.class), any(DataType.class));
  }

  @Test
  public void failsWhenFiltersDoNotReduceListToASingleElement() throws Exception {
    Converter transformer1 = mock(Converter.class);
    Converter transformer2 = mock(Converter.class);

    List<Converter> transformers = new ArrayList<Converter>();
    transformers.add(transformer1);
    transformers.add(transformer2);

    ConverterFilter filter1 = Mockito.mock(ConverterFilter.class);
    when(filter1.filter(transformers, XML_DATA_TYPE, JSON_DATA_TYPE)).thenReturn(transformers);
    ConverterFilter filter2 = Mockito.mock(ConverterFilter.class);
    when(filter2.filter(transformers, XML_DATA_TYPE, JSON_DATA_TYPE)).thenReturn(transformers);

    CompositeConverterFilter compositeConverterFilter = new CompositeConverterFilter(filter1, filter2);
    List<Converter> filteredTransformers = compositeConverterFilter.filter(transformers, XML_DATA_TYPE, JSON_DATA_TYPE);

    assertEquals(transformers, filteredTransformers);
    verify(filter1, times(1)).filter(any(List.class), any(DataType.class), any(DataType.class));
    verify(filter2, times(1)).filter(any(List.class), any(DataType.class), any(DataType.class));
  }
}
