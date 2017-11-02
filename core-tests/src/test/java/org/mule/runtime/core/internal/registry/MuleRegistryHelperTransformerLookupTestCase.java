/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.internal.registry;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.transformer.Converter;
import org.mule.runtime.core.api.transformer.Transformer;
import org.mule.runtime.core.internal.transformer.builder.MockConverterBuilder;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;
import org.mule.tck.testmodels.fruit.Orange;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

@SmallTest
public class MuleRegistryHelperTransformerLookupTestCase extends AbstractMuleTestCase {

  private static final DataType ORANGE_DATA_TYPE = DataType.fromType(Orange.class);

  private final DefaultRegistryBroker registry = mock(DefaultRegistryBroker.class);
  private final MuleContext muleContext = mock(MuleContext.class);
  private final MuleRegistryHelper muleRegistryHelper = new MuleRegistryHelper(registry, muleContext);
  private final Converter stringToOrange = new MockConverterBuilder().from(DataType.STRING).to(ORANGE_DATA_TYPE).build();
  private final Converter orangeToString = new MockConverterBuilder().from(ORANGE_DATA_TYPE).to(DataType.STRING).build();

  @Before
  public void setUp() throws Exception {
    TransformerResolver transformerResolver = mock(TransformerResolver.class);
    when(transformerResolver.resolve(DataType.STRING, ORANGE_DATA_TYPE)).thenReturn(stringToOrange);
    when(transformerResolver.resolve(ORANGE_DATA_TYPE, DataType.STRING)).thenReturn(orangeToString);

    muleRegistryHelper.registerObject("mockTransformerResolver", transformerResolver);

    muleRegistryHelper.registerTransformer(orangeToString);
    muleRegistryHelper.registerTransformer(stringToOrange);
  }

  @Test
  public void cachesTransformerResolvers() throws Exception {
    Transformer transformer1 = muleRegistryHelper.lookupTransformer(DataType.STRING, ORANGE_DATA_TYPE);
    Transformer transformer2 = muleRegistryHelper.lookupTransformer(ORANGE_DATA_TYPE, DataType.STRING);

    Mockito.verify(registry, times(0)).lookupObjects(TransformerResolver.class);
    assertEquals(stringToOrange, transformer1);
    assertEquals(orangeToString, transformer2);
  }

  @Test
  public void cachesTransformers() throws Exception {
    List<Transformer> transformers = muleRegistryHelper.lookupTransformers(DataType.STRING, ORANGE_DATA_TYPE);

    Mockito.verify(registry, times(0)).lookupObjects(Transformer.class);
    assertEquals(1, transformers.size());
    assertEquals(stringToOrange, transformers.get(0));
  }
}
