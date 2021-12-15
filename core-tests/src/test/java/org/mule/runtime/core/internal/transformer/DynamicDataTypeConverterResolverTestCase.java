/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.transformer;

import static org.mule.runtime.api.metadata.DataType.BYTE_ARRAY;
import static org.mule.runtime.api.metadata.DataType.INPUT_STREAM;
import static org.mule.runtime.api.metadata.DataType.STRING;
import static org.mule.test.allure.AllureConstants.RegistryFeature.REGISTRY;
import static org.mule.test.allure.AllureConstants.RegistryFeature.TransfromersStory.TRANSFORMERS;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.core.api.transformer.Transformer;
import org.mule.runtime.core.api.transformer.TransformerException;
import org.mule.runtime.core.internal.context.MuleContextWithRegistry;
import org.mule.runtime.core.internal.registry.MuleRegistry;
import org.mule.runtime.core.internal.transformer.builder.MockConverterBuilder;
import org.mule.runtime.core.privileged.transformer.TransformersRegistry;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;

@SmallTest
@Feature(REGISTRY)
@Story(TRANSFORMERS)
public class DynamicDataTypeConverterResolverTestCase extends AbstractMuleTestCase {

  private final MuleContextWithRegistry muleContext = mock(MuleContextWithRegistry.class);
  private final MuleRegistry muleRegistry = mock(MuleRegistry.class);
  private final TransformersRegistry transformersRegistry = mock(TransformersRegistry.class);

  @Test
  public void doesNotFailWhenThereIsNoDataTypeResolution() throws TransformerException {
    when(muleContext.getRegistry()).thenReturn(muleRegistry);
    when(transformersRegistry.lookupTransformer(any(DataType.class), any(DataType.class))).thenReturn(null);

    DynamicDataTypeConversionResolver resolver = new DynamicDataTypeConversionResolver(transformersRegistry);

    List<DataType> targetValues = new ArrayList<>();
    targetValues.add(DataType.STRING);
    Transformer resolvedConverter = resolver.resolve(INPUT_STREAM, targetValues);

    assertEquals(null, resolvedConverter);
  }

  @Test
  public void findsExpectedConverter() throws TransformerException {
    Transformer expectedConverter = new MockConverterBuilder().from(BYTE_ARRAY).to(STRING).build();

    when(muleContext.getRegistry()).thenReturn(muleRegistry);
    when(transformersRegistry.lookupTransformer(BYTE_ARRAY, STRING)).thenReturn(expectedConverter);

    DynamicDataTypeConversionResolver resolver = new DynamicDataTypeConversionResolver(transformersRegistry);

    List<DataType> targetValues = new ArrayList<>();
    targetValues.add(INPUT_STREAM);
    targetValues.add(STRING);
    Transformer resolvedConverter = resolver.resolve(BYTE_ARRAY, targetValues);

    assertEquals(expectedConverter, resolvedConverter);
  }
}
