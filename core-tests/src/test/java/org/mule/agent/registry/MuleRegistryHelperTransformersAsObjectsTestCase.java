/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.agent.registry;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.mule.api.MuleContext;
import org.mule.api.registry.TransformerResolver;
import org.mule.api.transformer.Converter;
import org.mule.api.transformer.DataType;
import org.mule.api.transformer.Transformer;
import org.mule.registry.DefaultRegistryBroker;
import org.mule.registry.MuleRegistryHelper;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;
import org.mule.tck.testmodels.fruit.Apple;
import org.mule.transformer.builder.MockConverterBuilder;
import org.mule.transformer.types.DataTypeFactory;

import org.junit.Before;
import org.junit.Test;

@SmallTest
public class MuleRegistryHelperTransformersAsObjectsTestCase extends AbstractMuleTestCase
{

    private static final DataType<Apple> APPLE_DATA_TYPE = DataTypeFactory.create(Apple.class);

    private final DefaultRegistryBroker registry = mock(DefaultRegistryBroker.class);
    private final MuleContext muleContext = mock(MuleContext.class);
    private final MuleRegistryHelper muleRegistryHelper = new MuleRegistryHelper(registry, muleContext);
    private final Converter stringToApple = new MockConverterBuilder().from(DataTypeFactory.STRING).to(APPLE_DATA_TYPE).build();
    private final Converter appleToString = new MockConverterBuilder().from(APPLE_DATA_TYPE).to(DataTypeFactory.STRING).build();

    @Before
    public void setUp() throws Exception
    {
        TransformerResolver transformerResolver = mock(TransformerResolver.class);
        when(transformerResolver.resolve(DataTypeFactory.STRING, APPLE_DATA_TYPE)).thenReturn(stringToApple);
        when(transformerResolver.resolve(APPLE_DATA_TYPE, DataTypeFactory.STRING)).thenReturn(appleToString);

        muleRegistryHelper.registerObject("mockTransformerResolver", transformerResolver);

        muleRegistryHelper.registerObject("StringToAppleConverter", stringToApple);
        muleRegistryHelper.registerObject("AppleToStringConverter", appleToString, appleToString.getClass());
    }

    @Test
    public void testRegisterTransformersAsNamedObjects() throws Exception
    {
        Transformer transformer1 = muleRegistryHelper.lookupTransformer(DataTypeFactory.STRING, APPLE_DATA_TYPE);
        Transformer transformer2 = muleRegistryHelper.lookupTransformer(APPLE_DATA_TYPE, DataTypeFactory.STRING);

        assertEquals(stringToApple, transformer1);
        assertEquals(appleToString, transformer2);
    }
}
