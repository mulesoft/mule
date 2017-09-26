/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.transformer;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.core.api.transformer.Transformer;
import org.mule.runtime.core.api.transformer.TransformerException;
import org.mule.runtime.core.internal.context.MuleContextWithRegistries;
import org.mule.runtime.core.internal.registry.MuleRegistry;
import org.mule.runtime.core.internal.transformer.builder.MockConverterBuilder;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import org.junit.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;

@SmallTest
public class DynamicDataTypeConverterResolverTestCase extends AbstractMuleTestCase {

  private MuleContextWithRegistries muleContext = mock(MuleContextWithRegistries.class);
  private MuleRegistry muleRegistry = mock(MuleRegistry.class);

  @Test
  public void doesNotFailWhenThereIsNoDataTypeResolution() throws TransformerException {
    when(muleContext.getRegistry()).thenReturn(muleRegistry);
    when(muleRegistry.lookupTransformer(Mockito.any(DataType.class), Mockito.any(DataType.class))).thenReturn(null);

    DynamicDataTypeConversionResolver resolver = new DynamicDataTypeConversionResolver(muleContext);

    List<DataType> targetValues = new ArrayList<>();
    targetValues.add(DataType.STRING);
    Transformer resolvedConverter = resolver.resolve(DataType.INPUT_STREAM, targetValues);

    assertEquals(null, resolvedConverter);
  }

  @Test
  public void findsExpectedConverter() throws TransformerException {
    Transformer expectedConverter = new MockConverterBuilder().from(DataType.BYTE_ARRAY).to(DataType.STRING).build();

    when(muleContext.getRegistry()).thenReturn(muleRegistry);
    when(muleRegistry.lookupTransformer(DataType.BYTE_ARRAY, DataType.STRING)).thenReturn(expectedConverter);

    DynamicDataTypeConversionResolver resolver = new DynamicDataTypeConversionResolver(muleContext);

    List<DataType> targetValues = new ArrayList<>();
    targetValues.add(DataType.INPUT_STREAM);
    targetValues.add(DataType.STRING);
    Transformer resolvedConverter = resolver.resolve(DataType.BYTE_ARRAY, targetValues);

    assertEquals(expectedConverter, resolvedConverter);
  }
}
