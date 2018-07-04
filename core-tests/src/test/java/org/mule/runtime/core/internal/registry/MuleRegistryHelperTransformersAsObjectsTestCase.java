/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.internal.registry;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.transformer.Converter;
import org.mule.runtime.core.api.transformer.Transformer;
import org.mule.runtime.core.internal.transformer.builder.MockConverterBuilder;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;
import org.mule.tck.testmodels.fruit.Apple;

import org.junit.Before;
import org.junit.Test;

@SmallTest
public class MuleRegistryHelperTransformersAsObjectsTestCase extends AbstractMuleTestCase {

  private static final DataType APPLE_DATA_TYPE = DataType.fromType(Apple.class);

  private final Registry registry = mock(Registry.class);
  private final MuleContext muleContext = mock(MuleContext.class);
  private final MuleRegistryHelper muleRegistryHelper = new MuleRegistryHelper(registry, muleContext);
  private final Converter stringToApple = new MockConverterBuilder().from(DataType.STRING).to(APPLE_DATA_TYPE).build();
  private final Converter appleToString = new MockConverterBuilder().from(APPLE_DATA_TYPE).to(DataType.STRING).build();

  @Before
  public void setUp() throws Exception {
    TransformerResolver transformerResolver = mock(TransformerResolver.class);
    when(transformerResolver.resolve(DataType.STRING, APPLE_DATA_TYPE)).thenReturn(stringToApple);
    when(transformerResolver.resolve(APPLE_DATA_TYPE, DataType.STRING)).thenReturn(appleToString);

    muleRegistryHelper.registerObject("mockTransformerResolver", transformerResolver);

    muleRegistryHelper.registerObject("StringToAppleConverter", stringToApple);
    muleRegistryHelper.registerObject("AppleToStringConverter", appleToString, appleToString.getClass());
  }

  @Test
  public void testRegisterTransformersAsNamedObjects() throws Exception {
    Transformer transformer1 = muleRegistryHelper.lookupTransformer(DataType.STRING, APPLE_DATA_TYPE);
    Transformer transformer2 = muleRegistryHelper.lookupTransformer(APPLE_DATA_TYPE, DataType.STRING);

    assertEquals(stringToApple, transformer1);
    assertEquals(appleToString, transformer2);
  }
}
