/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.agent.registry;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mule.transformer.types.DataTypeFactory.STRING;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mule.api.MuleContext;
import org.mule.api.registry.TransformerResolver;
import org.mule.api.transformer.Converter;
import org.mule.api.transformer.DataType;
import org.mule.api.transformer.Transformer;
import org.mule.registry.DefaultRegistryBroker;
import org.mule.registry.MuleRegistryHelper;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;
import org.mule.tck.testmodels.fruit.Banana;
import org.mule.tck.testmodels.fruit.Orange;
import org.mule.transformer.builder.MockConverterBuilder;
import org.mule.transformer.types.DataTypeFactory;

@SmallTest
public class MuleRegistryHelperTransformerLookupTestCase extends AbstractMuleTestCase
{

    private static final DataType<Orange> ORANGE_DATA_TYPE = DataTypeFactory.create(Orange.class);
    private static final DataType<Banana> BANANA_DATA_TYPE = DataTypeFactory.create(Banana.class);

    private final DefaultRegistryBroker registry = mock(DefaultRegistryBroker.class);
    private final MuleContext muleContext = mock(MuleContext.class);
    private final MuleRegistryHelper muleRegistryHelper = new MuleRegistryHelper(registry, muleContext);
    private final Converter stringToOrange = new MockConverterBuilder().from(STRING).to(ORANGE_DATA_TYPE).build();
    private final Converter orangeToString = new MockConverterBuilder().from(ORANGE_DATA_TYPE).to(STRING).build();

    @Before
    public void setUp() throws Exception
    {
        TransformerResolver transformerResolver = mock(TransformerResolver.class);
        when(transformerResolver.resolve(DataTypeFactory.STRING, ORANGE_DATA_TYPE)).thenReturn(stringToOrange);
        when(transformerResolver.resolve(ORANGE_DATA_TYPE, STRING)).thenReturn(orangeToString);

        muleRegistryHelper.registerObject("mockTransformerResolver", transformerResolver);

        muleRegistryHelper.registerTransformer(orangeToString);
        muleRegistryHelper.registerTransformer(stringToOrange);
    }

    @Test
    public void cachesTransformerResolvers() throws Exception
    {
        Transformer transformer1 = muleRegistryHelper.lookupTransformer(STRING, ORANGE_DATA_TYPE);
        Transformer transformer2 = muleRegistryHelper.lookupTransformer(ORANGE_DATA_TYPE, STRING);

        verify(registry, times(0)).lookupObjects(TransformerResolver.class);
        assertThat((Converter) transformer1, equalTo(stringToOrange));
        assertThat((Converter) transformer2, equalTo(orangeToString));
    }

    @Test
    public void cachesTransformers() throws Exception
    {
        List<Transformer> transformers = muleRegistryHelper.lookupTransformers(STRING, ORANGE_DATA_TYPE);

        Mockito.verify(registry, times(0)).lookupObjects(Transformer.class);
        assertThat(transformers, hasSize(1));
        assertThat((Converter) transformers.get(0), equalTo(stringToOrange));
    }
    
    @Test
    public void reregisterDoesNotLeakTransformer() throws Exception
    {
        Converter stringToBanana = new MockConverterBuilder().from(STRING).to(BANANA_DATA_TYPE).build();
        stringToBanana.setName("StringToBanana");

        Converter copyStringToBanana = new MockConverterBuilder().from(STRING).to(BANANA_DATA_TYPE).build();
        copyStringToBanana.setName("StringToBanana");

        muleRegistryHelper.registerTransformer(stringToBanana);
        muleRegistryHelper.registerTransformer(copyStringToBanana);
        muleRegistryHelper.lookupTransformers(DataTypeFactory.STRING, BANANA_DATA_TYPE);

        List<Transformer> transformers = muleRegistryHelper.lookupTransformers(STRING, BANANA_DATA_TYPE);

        assertThat(transformers, hasSize(1));
    }

}
