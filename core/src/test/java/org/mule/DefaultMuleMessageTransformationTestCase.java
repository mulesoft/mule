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
import org.mule.transformer.types.SimpleDataType;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

@SmallTest
public class DefaultMuleMessageTransformationTestCase extends AbstractMuleTestCase
{

    private MuleContext muleContext;
    private DataTypeConversionResolver conversionResolver;

    private class A
    {

    }

    private class B
    {

    }

    private class C
    {

    }

    private class D
    {

    }

    @Before
    public void setUp() throws Exception
    {
        // Configures a converter resolver that does nothing
        muleContext = mock(MuleContext.class);
        conversionResolver = mock(DataTypeConversionResolver.class);
        when(muleContext.getDataTypeConverterResolver()).thenReturn(conversionResolver);
    }

    private static final DataType<Object> dataTypeB = new SimpleDataType<Object>(B.class);
    private static final DataType<Object> dataTypeC = new SimpleDataType<Object>(C.class);
    private static final DataType<Object> dataTypeD = new SimpleDataType<Object>(D.class);

    @Test
    public void failsOnConverterWhenSourceAndReturnTypeDoesNotMatchAndThereIsNoImplicitConversion() throws MuleException
    {
        // Converter(B->C), payload A: FAIL
        TestConverter converter1 = new TransformerBuilder().from(dataTypeB).to(dataTypeC).returning(new C()).boundTo(muleContext).buildConverter(1);

        DefaultMuleMessage message = new DefaultMuleMessage(new A(), muleContext);

        try
        {
            message.applyTransformers(null, converter1);
            fail("Transformation is supposed to fail");
        }
        catch (IllegalArgumentException expected)
        {
        }
        assertFalse(converter1.wasExecuted());
    }

    @Test
    public void appliesImplicitConversionOnConverterWhenSourceAndReturnTypeDoesNotMatch() throws MuleException
    {
        // Converter(C->D), payload B: uses implicit conversion B->C
        TestConverter converter1 = new TransformerBuilder().from(dataTypeC).to(dataTypeD).returning(new D()).boundTo(muleContext).buildConverter(1);
        TestConverter converter2 = new TransformerBuilder().from(dataTypeB).to(dataTypeC).returning(new C()).boundTo(muleContext).buildConverter(1);

        when(conversionResolver.resolve(Mockito.any(DataType.class), Mockito.anyList())).thenReturn(converter2);

        DefaultMuleMessage message = new DefaultMuleMessage(new B(), muleContext);

        message.applyTransformers(null, converter1);

        assertTrue(message.getPayload() instanceof D);
        assertTrue(converter1.wasExecuted());
        assertTrue(converter2.wasExecuted());
    }

    @Test
    public void appliesConverter() throws MuleException
    {
        // Converter(B->C), payload B: OK
        TestConverter converter1 = new TransformerBuilder().from(dataTypeB).to(dataTypeC).returning(new C()).boundTo(muleContext).buildConverter(1);

        DefaultMuleMessage message = new DefaultMuleMessage(new B(), muleContext);
        message.applyTransformers(null, converter1);

        assertTrue(message.getPayload() instanceof C);
        assertTrue(converter1.wasExecuted());
    }

    @Test
    public void skipsConverterThatDoesNotMatchWhenOriginalPayloadMatchesExpectedOutputType() throws MuleException
    {
        // Converter(B->C), payload C: OK - skips transformer but C is the expected output type -> OK
        TestConverter converter1 = new TransformerBuilder().from(dataTypeB).to(dataTypeC).returning(new C()).boundTo(muleContext).buildConverter(1);

        DefaultMuleMessage message = new DefaultMuleMessage(new C(), muleContext);
        message.applyTransformers(null, converter1);

        assertTrue(message.getPayload() instanceof C);
        assertFalse(converter1.wasExecuted());
    }

    @Test
    public void failsTransformationUsingConverterWhenSourceAndReturnTypeDoesNotMatch2() throws MuleException
    {
        // Converter(B -> C) Converter(C->D), payload A: FAIL
        TestConverter converter1 = new TransformerBuilder().from(dataTypeB).to(dataTypeC).returning(new C()).boundTo(muleContext).buildConverter(1);
        TestConverter converter2 = new TransformerBuilder().from(dataTypeC).to(dataTypeD).returning(new D()).boundTo(muleContext).buildConverter(1);

        DefaultMuleMessage message = new DefaultMuleMessage(new A(), muleContext);
        try
        {
            message.applyTransformers(null, converter1, converter2);
            fail("Transformation is supposed to fail");
        }
        catch (IllegalArgumentException expected)
        {
        }
        assertFalse(converter1.wasExecuted());
        assertFalse(converter2.wasExecuted());
    }

    @Test
    public void appliesBothConverters() throws MuleException
    {
        // Converter(B -> C) Converter(C->D), payload B: converts B->C, C->D
        TestConverter converter1 = new TransformerBuilder().from(dataTypeB).to(dataTypeC).returning(new C()).boundTo(muleContext).buildConverter(1);
        TestConverter converter2 = new TransformerBuilder().from(dataTypeC).to(dataTypeD).returning(new D()).boundTo(muleContext).buildConverter(1);

        DefaultMuleMessage message = new DefaultMuleMessage(new B(), muleContext);
        message.applyTransformers(null, converter1, converter2);

        assertTrue(message.getPayload() instanceof D);
        assertTrue(converter1.wasExecuted());
        assertTrue(converter2.wasExecuted());
    }

    @Test
    public void skipsFirstConverterAppliesSecond() throws MuleException
    {
        // Converter(B -> C) Converter(C->D), payload C: skips converter(B->C), applies Converter(C->D)
        TestConverter converter1 = new TransformerBuilder().from(dataTypeB).to(dataTypeC).returning(new C()).boundTo(muleContext).buildConverter(1);
        TestConverter converter2 = new TransformerBuilder().from(dataTypeC).to(dataTypeD).returning(new D()).boundTo(muleContext).buildConverter(1);

        DefaultMuleMessage message = new DefaultMuleMessage(new C(), muleContext);
        message.applyTransformers(null, converter1, converter2);

        assertTrue(message.getPayload() instanceof D);
        assertFalse(converter1.wasExecuted());
        assertTrue(converter2.wasExecuted());
    }

    @Test
    public void skipBothConvertersButPayloadMatchesExpectedOutputType() throws MuleException
    {
        // Converter(B -> C) Converter(C->D), payload D: skips converter(B-C), skips converter(C->D), but D is the expected output type -> OK
        TestConverter converter1 = new TransformerBuilder().from(dataTypeB).to(dataTypeC).returning(new C()).boundTo(muleContext).buildConverter(1);
        TestConverter converter2 = new TransformerBuilder().from(dataTypeC).to(dataTypeD).returning(new D()).boundTo(muleContext).buildConverter(1);

        DefaultMuleMessage message = new DefaultMuleMessage(new D(), muleContext);
        message.applyTransformers(null, converter1, converter2);

        assertTrue(message.getPayload() instanceof D);
        assertFalse(converter1.wasExecuted());
        assertFalse(converter2.wasExecuted());
    }

    @Test
    public void failsTransformerIgnoringNonMatchingConverter() throws MuleException
    {
        // Transformer(B -> D) Converter(C->D), payload A: FAIL
        TestTransformer transformer1 = new TransformerBuilder().from(dataTypeB).to(dataTypeD).returning(new D()).boundTo(muleContext).build();
        TestConverter converter2 = new TransformerBuilder().from(dataTypeC).to(dataTypeD).returning(new D()).boundTo(muleContext).buildConverter(1);

        DefaultMuleMessage message = new DefaultMuleMessage(new A(), muleContext);
        try
        {
            message.applyTransformers(null, transformer1, converter2);
            fail("Transformation is supposed to fail");
        }
        catch (IllegalArgumentException expected)
        {
        }
        assertFalse(transformer1.wasExecuted());
        assertFalse(converter2.wasExecuted());
    }

    @Test
    public void appliesTransformerSkipsConverter() throws MuleException
    {
        // Transformer(B -> D) Converter(C->D), payload B: converts B->D, skips converter C->D, resulting output is of the expected type -> OK
        TestTransformer transformer1 = new TransformerBuilder().from(dataTypeB).to(dataTypeD).returning(new D()).boundTo(muleContext).build();
        TestConverter converter2 = new TransformerBuilder().from(dataTypeC).to(dataTypeD).returning(new D()).boundTo(muleContext).buildConverter(1);

        DefaultMuleMessage message = new DefaultMuleMessage(new B(), muleContext);
        message.applyTransformers(null, transformer1, converter2);

        assertTrue(message.getPayload() instanceof D);
        assertTrue(transformer1.wasExecuted());
        assertFalse(converter2.wasExecuted());
    }

    @Test
    public void failsTransformerIgnoringMatchingConverter() throws MuleException
    {
        // Transformer(B -> D) Converter(C->D), payload C: FAIL
        TestTransformer transformer1 = new TransformerBuilder().from(dataTypeB).to(dataTypeD).returning(new D()).boundTo(muleContext).build();
        TestConverter converter2 = new TransformerBuilder().from(dataTypeC).to(dataTypeD).returning(new D()).boundTo(muleContext).buildConverter(1);

        DefaultMuleMessage message = new DefaultMuleMessage(new C(), muleContext);
        try
        {
            message.applyTransformers(null, transformer1, converter2);
            fail("Transformation is supposed to fail");
        }
        catch (IllegalArgumentException expected)
        {
        }
        assertFalse(transformer1.wasExecuted());
        assertFalse(converter2.wasExecuted());
    }

    @Test
    public void failsTransformerIgnoringMatchingConverterWhenOriginalPayloadMatchesExpectedOutputType() throws MuleException
    {
        // Transformer(B -> D) Converter(C->D), payload D: FAIL
        TestTransformer transformer1 = new TransformerBuilder().from(dataTypeB).to(dataTypeD).returning(new D()).boundTo(muleContext).build();
        TestConverter converter2 = new TransformerBuilder().from(dataTypeC).to(dataTypeD).returning(new D()).boundTo(muleContext).buildConverter(1);

        DefaultMuleMessage message = new DefaultMuleMessage(new D(), muleContext);
        try
        {
            message.applyTransformers(null, transformer1, converter2);
            fail("Transformation is supposed to fail");
        }
        catch (IllegalArgumentException expected)
        {
        }
        assertFalse(transformer1.wasExecuted());
        assertFalse(converter2.wasExecuted());
    }

    @Test
    public void skipsConverterFailsOnTransformer() throws MuleException
    {
        // Converter(B -> D) Transformer(C->D), payload A: FAIL
        TestConverter converter1 = new TransformerBuilder().from(dataTypeB).to(dataTypeD).returning(new D()).boundTo(muleContext).buildConverter(1);
        TestTransformer transformer2 = new TransformerBuilder().from(dataTypeC).to(dataTypeD).returning(new D()).boundTo(muleContext).build();

        DefaultMuleMessage message = new DefaultMuleMessage(new A(), muleContext);

        try
        {
            message.applyTransformers(null, converter1, transformer2);
            fail("Transformation is supposed to fail");
        }
        catch (IllegalArgumentException expected)
        {
        }
        assertFalse(converter1.wasExecuted());
        assertFalse(transformer2.wasExecuted());
    }

    @Test
    public void appliesConverterFailsOnTransformer() throws MuleException
    {
        // Converter(B -> D) Transformer(C->D), payload B: converts B-> D, cannot apply transformer -> FAIL
        TestConverter converter1 = new TransformerBuilder().from(dataTypeB).to(dataTypeD).returning(new D()).boundTo(muleContext).buildConverter(1);
        TestTransformer transformer2 = new TransformerBuilder().from(dataTypeC).to(dataTypeD).returning(new D()).boundTo(muleContext).build();

        DefaultMuleMessage message = new DefaultMuleMessage(new B(), muleContext);
        try
        {
            message.applyTransformers(null, converter1, transformer2);
            fail("Transformation is supposed to fail");
        }
        catch (IllegalArgumentException expected)
        {
        }
        assertTrue(converter1.wasExecuted());
        assertFalse(transformer2.wasExecuted());
    }

    @Test
    public void skipsConverterAppliesTransformer() throws MuleException
    {
        // Converter(B -> D) Transformer(C->D), payload C: skips converter, transforms C to D -> OK
        TestConverter converter1 = new TransformerBuilder().from(dataTypeB).to(dataTypeD).returning(new D()).boundTo(muleContext).buildConverter(1);
        TestTransformer transformer2 = new TransformerBuilder().from(dataTypeC).to(dataTypeD).returning(new D()).boundTo(muleContext).build();

        DefaultMuleMessage message = new DefaultMuleMessage(new C(), muleContext);
        message.applyTransformers(null, converter1, transformer2);

        assertTrue(message.getPayload() instanceof D);
        assertFalse(converter1.wasExecuted());
        assertTrue(transformer2.wasExecuted());
    }

    @Test
    public void skipsConverterFailsOnTransformerWhenOriginalPayloadMatchesExpectedOutputType() throws MuleException
    {
        // Converter(B -> D) Transformer(C->D), payload D: FAIL
        TestConverter converter1 = new TransformerBuilder().from(dataTypeB).to(dataTypeD).returning(new D()).boundTo(muleContext).buildConverter(1);
        TestTransformer transformer2 = new TransformerBuilder().from(dataTypeC).to(dataTypeD).returning(new D()).boundTo(muleContext).build();

        DefaultMuleMessage message = new DefaultMuleMessage(new D(), muleContext);
        try
        {
            message.applyTransformers(null, converter1, transformer2);
            fail("Transformation is supposed to fail");
        }
        catch (IllegalArgumentException expected)
        {
        }
        assertFalse(converter1.wasExecuted());
        assertFalse(transformer2.wasExecuted());
    }

    @Test
    public void failsOnFirstTransformer() throws MuleException
    {
        // Transformer(B ->D) Transformer(C->D), payload A: FAIL
        TestTransformer transformer1 = new TransformerBuilder().from(dataTypeB).to(dataTypeD).returning(new D()).boundTo(muleContext).build();
        TestTransformer transformer2 = new TransformerBuilder().from(dataTypeC).to(dataTypeD).returning(new D()).boundTo(muleContext).build();

        DefaultMuleMessage message = new DefaultMuleMessage(new A(), muleContext);

        try
        {
            message.applyTransformers(null, transformer1, transformer2);
            fail("Transformation is supposed to fail");
        }
        catch (IllegalArgumentException expected)
        {
        }
        assertFalse(transformer1.wasExecuted());
        assertFalse(transformer2.wasExecuted());
    }

    @Test
    public void appliesFirstTransformerFailsOnSecondTransformer() throws MuleException
    {
        // Transformer(B ->D) Transformer(C->D), payload B: applies first transformer, cannot apply second transformer -> FAIL
        TestTransformer transformer1 = new TransformerBuilder().from(dataTypeB).to(dataTypeD).returning(new D()).boundTo(muleContext).build();
        TestTransformer transformer2 = new TransformerBuilder().from(dataTypeC).to(dataTypeD).returning(new D()).boundTo(muleContext).build();

        DefaultMuleMessage message = new DefaultMuleMessage(new B(), muleContext);

        try
        {
            message.applyTransformers(null, transformer1, transformer2);
            fail("Transformation is supposed to fail");
        }
        catch (IllegalArgumentException expected)
        {
        }
        assertTrue(transformer1.wasExecuted());
        assertFalse(transformer2.wasExecuted());
    }

    @Test
    public void failsOnFirstTransformerIgnoresSecondTransformer() throws MuleException
    {
        // Transformer(B ->D) Transformer(C->D), payload C: FAIL
        TestTransformer transformer1 = new TransformerBuilder().from(dataTypeB).to(dataTypeD).returning(new D()).boundTo(muleContext).build();
        TestTransformer transformer2 = new TransformerBuilder().from(dataTypeC).to(dataTypeD).returning(new D()).boundTo(muleContext).build();

        DefaultMuleMessage message = new DefaultMuleMessage(new C(), muleContext);

        try
        {
            message.applyTransformers(null, transformer1, transformer2);
            fail("Transformation is supposed to fail");
        }
        catch (IllegalArgumentException expected)
        {
        }
        assertFalse(transformer1.wasExecuted());
        assertFalse(transformer2.wasExecuted());
    }

    @Test
    public void failsOnFirstTransformerIgnoresSecondTransformerWhenOriginalPayloadMatchesExpectedOutputType() throws MuleException
    {
        // Transformer(B ->D) Transformer(C->D), payload D: FAIL
        TestTransformer transformer1 = new TransformerBuilder().from(dataTypeB).to(dataTypeD).returning(new D()).boundTo(muleContext).build();
        TestTransformer transformer2 = new TransformerBuilder().from(dataTypeC).to(dataTypeD).returning(new D()).boundTo(muleContext).build();

        DefaultMuleMessage message = new DefaultMuleMessage(new D(), muleContext);

        try
        {
            message.applyTransformers(null, transformer1, transformer2);
            fail("Transformation is supposed to fail");
        }
        catch (IllegalArgumentException expected)
        {
        }
        assertFalse(transformer1.wasExecuted());
        assertFalse(transformer2.wasExecuted());
    }

    @Test
    public void failsOnTransformerWhenSourceAndReturnTypeDoesNotMatch() throws MuleException
    {
        // Transformer(B->C), payload A: FAIL
        TestTransformer transformer1 = new TransformerBuilder().from(dataTypeB).to(dataTypeC).returning(new C()).boundTo(muleContext).build();

        DefaultMuleMessage message = new DefaultMuleMessage(new A(), muleContext);

        try
        {
            message.applyTransformers(null, transformer1);
            fail("Transformation is supposed to fail");
        }
        catch (IllegalArgumentException expected)
        {
        }
        assertFalse(transformer1.wasExecuted());
    }

    @Test
    public void appliesTransformer() throws MuleException
    {
        // Transformer(B->C), payload B: OK
        TestTransformer transformer1 = new TransformerBuilder().from(dataTypeB).to(dataTypeC).returning(new C()).boundTo(muleContext).build();

        DefaultMuleMessage message = new DefaultMuleMessage(new B(), muleContext);
        message.applyTransformers(null, transformer1);

        assertTrue(message.getPayload() instanceof C);
        assertTrue(transformer1.wasExecuted());
    }

    @Test
    public void failsOnTransformerWhenOriginalPayloadMatchesExpectedOutputType() throws MuleException
    {
        // Transformer(B->C), payload C: FAIL
        TestTransformer transformer1 = new TransformerBuilder().from(dataTypeB).to(dataTypeC).returning(new C()).boundTo(muleContext).build();

        DefaultMuleMessage message = new DefaultMuleMessage(new C(), muleContext);
        try
        {
            message.applyTransformers(null, transformer1);
            fail("Transformation is supposed to fail");
        }
        catch (IllegalArgumentException expected)
        {
        }
        assertFalse(transformer1.wasExecuted());
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
