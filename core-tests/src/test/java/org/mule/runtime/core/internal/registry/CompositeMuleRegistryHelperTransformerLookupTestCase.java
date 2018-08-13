/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.internal.registry;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.transformer.Converter;
import org.mule.runtime.core.api.transformer.Transformer;
import org.mule.runtime.core.api.transformer.TransformerException;
import org.mule.runtime.core.internal.transformer.builder.MockConverterBuilder;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.testmodels.fruit.Apple;
import org.mule.tck.testmodels.fruit.Banana;
import org.mule.tck.testmodels.fruit.Orange;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CompositeMuleRegistryHelperTransformerLookupTestCase extends AbstractMuleTestCase {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private static final DataType ORANGE_DATA_TYPE = DataType.fromType(Orange.class);
  private static final DataType APPLE_DATA_TYPE = DataType.fromType(Apple.class);
  private static final DataType BANANA_DATA_TYPE = DataType.fromType(Banana.class);

  private final SimpleRegistry parentRegistry = mock(SimpleRegistry.class);
  private final SimpleRegistry registry = mock(SimpleRegistry.class);

  private final MuleContext parentMuleContext = mock(MuleContext.class);
  private final MuleContext muleContext = mock(MuleContext.class);

  private final Converter stringToOrange = new MockConverterBuilder().from(DataType.STRING).to(ORANGE_DATA_TYPE).build();
  private final Converter stringToApple = new MockConverterBuilder().from(DataType.STRING).to(APPLE_DATA_TYPE).build();

  private final MuleRegistryHelper parentMuleRegistryHelper = new MuleRegistryHelper(parentRegistry, parentMuleContext);
  private final MuleRegistryHelper muleRegistryHelper =
      new CompositeMuleRegistryHelper(registry, muleContext, parentMuleRegistryHelper);

  @Before
  public void setUp() throws Exception {
    TransformerResolver childTransformerResolver = mock(TransformerResolver.class);
    when(childTransformerResolver.resolve(DataType.STRING, ORANGE_DATA_TYPE)).thenReturn(stringToOrange);

    muleRegistryHelper.registerObject("mockChildTransformerResolver", childTransformerResolver);
    muleRegistryHelper.registerTransformer(stringToOrange);

    TransformerResolver parentTransformerResolver = mock(TransformerResolver.class);
    when(parentTransformerResolver.resolve(DataType.STRING, APPLE_DATA_TYPE)).thenReturn(stringToApple);

    parentMuleRegistryHelper.registerObject("mockParentTransformerResolver", parentTransformerResolver);
    parentMuleRegistryHelper.registerTransformer(stringToApple);
  }

  @Test
  public void lookupTransformerOnMuleRegistryHelperOnly() throws Exception {
    Transformer transformer = muleRegistryHelper.lookupTransformer(DataType.STRING, ORANGE_DATA_TYPE);

    assertEquals(stringToOrange, transformer);
  }


  @Test
  public void lookupTransformerOnParentMuleRegistryHelper() throws Exception {
    Transformer transformer = muleRegistryHelper.lookupTransformer(DataType.STRING, APPLE_DATA_TYPE);

    assertEquals(stringToApple, transformer);
  }

  @Test
  public void lookupTransformerNotFound() throws Exception {
    expectedException.expect(TransformerException.class);

    muleRegistryHelper.lookupTransformer(DataType.STRING, BANANA_DATA_TYPE);
  }

}
