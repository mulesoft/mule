/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.transformer.DataType;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;
import org.mule.transformer.TestConverter;
import org.mule.transformer.TestTransformer;
import org.mule.transformer.TransformerBuilder;
import org.mule.transformer.types.DataTypeFactory;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

@SmallTest
public class DefaultMuleMessageImplicitConversionTestCase extends AbstractMuleTestCase
{

    private MuleContext muleContext;
    private DataTypeConversionResolver conversionResolver;

    @Before
    public void setUp() throws Exception
    {
        // Configures a converter resolver that does nothing
        conversionResolver = mock(DataTypeConversionResolver.class);
        muleContext = mock(MuleContext.class);
        when(muleContext.getDataTypeConverterResolver()).thenReturn(conversionResolver);
    }

    @Test
    public void failsWhenNoImplicitConversionAvailable() throws MuleException
    {
        TestTransformer transformer = new TransformerBuilder().from(DataTypeFactory.BYTE_ARRAY).to(DataTypeFactory.STRING).returning("bar").boundTo(muleContext).build();

        when(conversionResolver.resolve(Mockito.any(DataType.class), Mockito.anyList())).thenReturn(null);

        DefaultMuleMessage message = new DefaultMuleMessage("TEST", muleContext);

        try
        {
            message.applyTransformers(null, transformer);
            fail("Transformation is supposed to fail");
        }
        catch (IllegalArgumentException expected)
        {
        }
        assertFalse(transformer.wasExecuted());
    }

    @Test
    public void appliesImplicitConversionWhenAvailable() throws MuleException
    {
        TestTransformer transformer = new TransformerBuilder().from(DataTypeFactory.BYTE_ARRAY).to(DataTypeFactory.STRING).returning("bar").boundTo(muleContext).build();
        TestConverter converter = new TransformerBuilder().from(DataTypeFactory.STRING).to(DataTypeFactory.BYTE_ARRAY).returning("bar".getBytes()).boundTo(muleContext).buildConverter(1);

        when(conversionResolver.resolve(Mockito.any(DataType.class), Mockito.anyList())).thenReturn(converter);

        DefaultMuleMessage message = new DefaultMuleMessage("TEST", muleContext);

        message.applyTransformers(null, transformer);

        assertEquals("bar", message.getPayload());
        assertTrue(transformer.wasExecuted());
        assertTrue(converter.wasExecuted());
    }
}
