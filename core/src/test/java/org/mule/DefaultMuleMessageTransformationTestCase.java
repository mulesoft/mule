/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.transformer.DataType;
import org.mule.api.transformer.Transformer;
import org.mule.api.transformer.TransformerException;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;
import org.mule.transformer.builder.MockConverterBuilder;
import org.mule.transformer.builder.MockTransformerBuilder;
import org.mule.transformer.types.DataTypeFactory;
import org.mule.transformer.types.SimpleDataType;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

@SmallTest
public class DefaultMuleMessageTransformationTestCase extends AbstractMuleTestCase
{

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

    private MuleContext muleContext = mock(MuleContext.class, RETURNS_DEEP_STUBS);
    private DataTypeConversionResolver conversionResolver = mock(DataTypeConversionResolver.class);

    @Before
    public void setUp() throws Exception
    {
        when(muleContext.getDataTypeConverterResolver()).thenReturn(conversionResolver);
        when(muleContext.getConfiguration().useExtendedTransformations()).thenReturn(true);
    }

    private static final DataType<Object> dataTypeB = new SimpleDataType<Object>(B.class);
    private static final DataType<Object> dataTypeC = new SimpleDataType<Object>(C.class);
    private static final DataType<Object> dataTypeD = new SimpleDataType<Object>(D.class);

    @Test
    public void failsOnConverterWhenSourceAndReturnTypeDoesNotMatchAndThereIsNoImplicitConversion() throws MuleException
    {
        // Converter(B->C), payload A: FAIL
        Transformer converter1 = new MockConverterBuilder().from(dataTypeB).to(dataTypeC).build();

        DefaultMuleMessage message = new DefaultMuleMessage(new A(), muleContext);

        try
        {
            message.applyTransformers(null, converter1);
            fail("Transformation is supposed to fail");
        }
        catch (IllegalArgumentException expected)
        {
        }
        verifyTransformerNotExecuted(converter1);
    }

    @Test
    public void appliesImplicitConversionOnConverterWhenSourceAndReturnTypeDoesNotMatch() throws MuleException
    {
        // Converter(C->D), payload B: uses implicit conversion B->C
        Transformer converter1 = new MockConverterBuilder().from(dataTypeC).to(dataTypeD).returning(new D()).build();
        Transformer converter2 = new MockConverterBuilder().from(dataTypeB).to(dataTypeC).returning(new C()).build();

        when(conversionResolver.resolve(Mockito.any(DataType.class), Mockito.anyList())).thenReturn(converter2);

        DefaultMuleMessage message = new DefaultMuleMessage(new B(), muleContext);

        message.applyTransformers(null, converter1);

        assertTrue(message.getPayload() instanceof D);
        verifyTransformerExecuted(converter1);
        verifyTransformerExecuted(converter2);
    }

    @Test
    public void appliesConverter() throws MuleException
    {
        // Converter(B->C), payload B: OK
        Transformer converter1 = new MockConverterBuilder().from(dataTypeB).to(dataTypeC).returning(new C()).build();

        DefaultMuleMessage message = new DefaultMuleMessage(new B(), muleContext);
        message.applyTransformers(null, converter1);

        assertTrue(message.getPayload() instanceof C);
        verifyTransformerExecuted(converter1);
    }

    @Test
    public void skipsConverterThatDoesNotMatchWhenOriginalPayloadMatchesExpectedOutputType() throws MuleException
    {
        // Converter(B->C), payload C: OK - skips transformer but C is the expected output type -> OK
        Transformer converter1 = new MockConverterBuilder().from(dataTypeB).to(dataTypeC).build();

        DefaultMuleMessage message = new DefaultMuleMessage(new C(), muleContext);
        message.applyTransformers(null, converter1);

        assertTrue(message.getPayload() instanceof C);
        verifyTransformerNotExecuted(converter1);
    }

    @Test
    public void failsTransformationUsingConverterWhenSourceAndReturnTypeDoesNotMatch2() throws MuleException
    {
        // Converter(B -> C) Converter(C->D), payload A: FAIL
        Transformer converter1 = new MockConverterBuilder().from(dataTypeB).to(dataTypeC).build();
        Transformer converter2 = new MockConverterBuilder().from(dataTypeC).to(dataTypeD).build();

        DefaultMuleMessage message = new DefaultMuleMessage(new A(), muleContext);
        try
        {
            message.applyTransformers(null, converter1, converter2);
            fail("Transformation is supposed to fail");
        }
        catch (IllegalArgumentException expected)
        {
        }
        verifyTransformerNotExecuted(converter1);
        verifyTransformerNotExecuted(converter2);
    }

    @Test
    public void appliesBothConverters() throws MuleException
    {
        // Converter(B -> C) Converter(C->D), payload B: converts B->C, C->D
        Transformer converter1 = new MockConverterBuilder().from(dataTypeB).to(dataTypeC).returning(new C()).build();
        Transformer converter2 = new MockConverterBuilder().from(dataTypeC).to(dataTypeD).returning(new D()).build();

        DefaultMuleMessage message = new DefaultMuleMessage(new B(), muleContext);
        message.applyTransformers(null, converter1, converter2);

        assertTrue(message.getPayload() instanceof D);
        verifyTransformerExecuted(converter1);
        verifyTransformerExecuted(converter2);
    }

    @Test
    public void skipsFirstConverterAppliesSecond() throws MuleException
    {
        // Converter(B -> C) Converter(C->D), payload C: skips converter(B->C), applies Converter(C->D)
        Transformer converter1 = new MockConverterBuilder().from(dataTypeB).to(dataTypeC).build();
        Transformer converter2 = new MockConverterBuilder().from(dataTypeC).to(dataTypeD).returning(new D()).build();

        DefaultMuleMessage message = new DefaultMuleMessage(new C(), muleContext);
        message.applyTransformers(null, converter1, converter2);

        assertTrue(message.getPayload() instanceof D);
        verifyTransformerNotExecuted(converter1);
        verifyTransformerExecuted(converter2);
    }

    @Test
    public void skipBothConvertersButPayloadMatchesExpectedOutputType() throws MuleException
    {
        // Converter(B -> C) Converter(C->D), payload D: skips converter(B-C), skips converter(C->D), but D is the expected output type -> OK
        Transformer converter1 = new MockConverterBuilder().from(dataTypeB).to(dataTypeC).build();
        Transformer converter2 = new MockConverterBuilder().from(dataTypeC).to(dataTypeD).build();

        DefaultMuleMessage message = new DefaultMuleMessage(new D(), muleContext);
        message.applyTransformers(null, converter1, converter2);

        assertTrue(message.getPayload() instanceof D);
        verifyTransformerNotExecuted(converter1);
        verifyTransformerNotExecuted(converter2);
    }

    @Test
    public void failsTransformerIgnoringNonMatchingConverter() throws MuleException
    {
        // Transformer(B -> D) Converter(C->D), payload A: FAIL
        Transformer transformer1 = new MockTransformerBuilder().from(dataTypeB).to(dataTypeD).build();
        Transformer converter2 = new MockConverterBuilder().from(dataTypeC).to(dataTypeD).build();

        DefaultMuleMessage message = new DefaultMuleMessage(new A(), muleContext);
        try
        {
            message.applyTransformers(null, transformer1, converter2);
            fail("Transformation is supposed to fail");
        }
        catch (IllegalArgumentException expected)
        {
        }
        verifyTransformerNotExecuted(transformer1);
        verifyTransformerNotExecuted(converter2);
    }

    @Test
    public void appliesTransformerSkipsConverter() throws MuleException
    {
        // Transformer(B -> D) Converter(C->D), payload B: converts B->D, skips converter C->D, resulting output is of the expected type -> OK
        Transformer transformer1 = new MockTransformerBuilder().from(dataTypeB).to(dataTypeD).returning(new D()).build();
        Transformer converter2 = new MockConverterBuilder().from(dataTypeC).to(dataTypeD).build();

        DefaultMuleMessage message = new DefaultMuleMessage(new B(), muleContext);
        message.applyTransformers(null, transformer1, converter2);

        assertTrue(message.getPayload() instanceof D);
        verifyTransformerExecuted(transformer1);
        verifyTransformerNotExecuted(converter2);
    }

    @Test
    public void failsTransformerIgnoringMatchingConverter() throws MuleException
    {
        // Transformer(B -> D) Converter(C->D), payload C: FAIL
        Transformer transformer1 = new MockTransformerBuilder().from(dataTypeB).to(dataTypeD).build();
        Transformer converter2 = new MockConverterBuilder().from(dataTypeC).to(dataTypeD).build();

        DefaultMuleMessage message = new DefaultMuleMessage(new C(), muleContext);
        try
        {
            message.applyTransformers(null, transformer1, converter2);
            fail("Transformation is supposed to fail");
        }
        catch (IllegalArgumentException expected)
        {
        }
        verifyTransformerNotExecuted(transformer1);
        verifyTransformerNotExecuted(converter2);
    }

    @Test
    public void failsTransformerIgnoringMatchingConverterWhenOriginalPayloadMatchesExpectedOutputType() throws MuleException
    {
        // Transformer(B -> D) Converter(C->D), payload D: FAIL
        Transformer transformer1 = new MockTransformerBuilder().from(dataTypeB).to(dataTypeD).build();
        Transformer converter2 = new MockConverterBuilder().from(dataTypeC).to(dataTypeD).build();

        DefaultMuleMessage message = new DefaultMuleMessage(new D(), muleContext);
        try
        {
            message.applyTransformers(null, transformer1, converter2);
            fail("Transformation is supposed to fail");
        }
        catch (IllegalArgumentException expected)
        {
        }
        verifyTransformerNotExecuted(transformer1);
        verifyTransformerNotExecuted(converter2);
    }

    @Test
    public void skipsConverterFailsOnTransformer() throws MuleException
    {
        // Converter(B -> D) Transformer(C->D), payload A: FAIL
        Transformer converter1 = new MockConverterBuilder().from(dataTypeB).to(dataTypeD).build();
        Transformer transformer2 = new MockTransformerBuilder().from(dataTypeC).to(dataTypeD).build();

        DefaultMuleMessage message = new DefaultMuleMessage(new A(), muleContext);

        try
        {
            message.applyTransformers(null, converter1, transformer2);
            fail("Transformation is supposed to fail");
        }
        catch (IllegalArgumentException expected)
        {
        }
        verifyTransformerNotExecuted(converter1);
        verifyTransformerNotExecuted(transformer2);
    }

    @Test
    public void appliesConverterFailsOnTransformer() throws MuleException
    {
        // Converter(B -> D) Transformer(C->D), payload B: converts B-> D, cannot apply transformer -> FAIL
        Transformer converter1 = new MockConverterBuilder().from(dataTypeB).to(dataTypeD).returning(new D()).build();
        Transformer transformer2 = new MockTransformerBuilder().from(dataTypeC).to(dataTypeD).build();

        DefaultMuleMessage message = new DefaultMuleMessage(new B(), muleContext);
        try
        {
            message.applyTransformers(null, converter1, transformer2);
            fail("Transformation is supposed to fail");
        }
        catch (IllegalArgumentException expected)
        {
        }
        verifyTransformerExecuted(converter1);
        verifyTransformerNotExecuted(transformer2);
    }

    @Test
    public void skipsConverterAppliesTransformer() throws MuleException
    {
        // Converter(B -> D) Transformer(C->D), payload C: skips converter, transforms C to D -> OK
        Transformer converter1 = new MockConverterBuilder().from(dataTypeB).to(dataTypeD).build();
        Transformer transformer2 = new MockTransformerBuilder().from(dataTypeC).to(dataTypeD).returning(new D()).build();

        DefaultMuleMessage message = new DefaultMuleMessage(new C(), muleContext);
        message.applyTransformers(null, converter1, transformer2);

        assertTrue(message.getPayload() instanceof D);
        verifyTransformerNotExecuted(converter1);
        verifyTransformerExecuted(transformer2);
    }

    @Test
    public void skipsConverterFailsOnTransformerWhenOriginalPayloadMatchesExpectedOutputType() throws MuleException
    {
        // Converter(B -> D) Transformer(C->D), payload D: FAIL
        Transformer converter1 = new MockConverterBuilder().from(dataTypeB).to(dataTypeD).build();
        Transformer transformer2 = new MockTransformerBuilder().from(dataTypeC).to(dataTypeD).build();

        DefaultMuleMessage message = new DefaultMuleMessage(new D(), muleContext);
        try
        {
            message.applyTransformers(null, converter1, transformer2);
            fail("Transformation is supposed to fail");
        }
        catch (IllegalArgumentException expected)
        {
        }
        verifyTransformerNotExecuted(converter1);
        verifyTransformerNotExecuted(transformer2);
    }

    @Test
    public void failsOnFirstTransformer() throws MuleException
    {
        // Transformer(B ->D) Transformer(C->D), payload A: FAIL
        Transformer transformer1 = new MockTransformerBuilder().from(dataTypeB).to(dataTypeD).build();
        Transformer transformer2 = new MockTransformerBuilder().from(dataTypeC).to(dataTypeD).build();

        DefaultMuleMessage message = new DefaultMuleMessage(new A(), muleContext);

        try
        {
            message.applyTransformers(null, transformer1, transformer2);
            fail("Transformation is supposed to fail");
        }
        catch (IllegalArgumentException expected)
        {
        }
        verifyTransformerNotExecuted(transformer1);
        verifyTransformerNotExecuted(transformer2);
    }

    @Test
    public void appliesFirstTransformerFailsOnSecondTransformer() throws MuleException
    {
        // Transformer(B ->D) Transformer(C->D), payload B: applies first transformer, cannot apply second transformer -> FAIL
        Transformer transformer1 = new MockTransformerBuilder().from(dataTypeB).to(dataTypeD).returning(new D()).build();
        Transformer transformer2 = new MockTransformerBuilder().from(dataTypeC).to(dataTypeD).build();

        DefaultMuleMessage message = new DefaultMuleMessage(new B(), muleContext);

        try
        {
            message.applyTransformers(null, transformer1, transformer2);
            fail("Transformation is supposed to fail");
        }
        catch (IllegalArgumentException expected)
        {
        }
        verifyTransformerExecuted(transformer1);
        verifyTransformerNotExecuted(transformer2);
    }

    @Test
    public void failsOnFirstTransformerIgnoresSecondTransformer() throws MuleException
    {
        // Transformer(B ->D) Transformer(C->D), payload C: FAIL
        Transformer transformer1 = new MockTransformerBuilder().from(dataTypeB).to(dataTypeD).build();
        Transformer transformer2 = new MockTransformerBuilder().from(dataTypeC).to(dataTypeD).build();

        DefaultMuleMessage message = new DefaultMuleMessage(new C(), muleContext);

        try
        {
            message.applyTransformers(null, transformer1, transformer2);
            fail("Transformation is supposed to fail");
        }
        catch (IllegalArgumentException expected)
        {
        }
        verifyTransformerNotExecuted(transformer1);
        verifyTransformerNotExecuted(transformer2);
    }

    @Test
    public void failsOnFirstTransformerIgnoresSecondTransformerWhenOriginalPayloadMatchesExpectedOutputType() throws MuleException
    {
        // Transformer(B ->D) Transformer(C->D), payload D: FAIL
        Transformer transformer1 = new MockTransformerBuilder().from(dataTypeB).to(dataTypeD).build();
        Transformer transformer2 = new MockTransformerBuilder().from(dataTypeC).to(dataTypeD).build();

        DefaultMuleMessage message = new DefaultMuleMessage(new D(), muleContext);

        try
        {
            message.applyTransformers(null, transformer1, transformer2);
            fail("Transformation is supposed to fail");
        }
        catch (IllegalArgumentException expected)
        {
        }
        verifyTransformerNotExecuted(transformer1);
        verifyTransformerNotExecuted(transformer2);
    }

    @Test
    public void failsOnTransformerWhenSourceAndReturnTypeDoesNotMatch() throws MuleException
    {
        // Transformer(B->C), payload A: FAIL
        Transformer transformer1 = new MockTransformerBuilder().from(dataTypeB).to(dataTypeC).build();

        DefaultMuleMessage message = new DefaultMuleMessage(new A(), muleContext);

        try
        {
            message.applyTransformers(null, transformer1);
            fail("Transformation is supposed to fail");
        }
        catch (IllegalArgumentException expected)
        {
        }
        verifyTransformerNotExecuted(transformer1);
    }

    @Test
    public void appliesTransformer() throws MuleException
    {
        // Transformer(B->C), payload B: OK
        Transformer transformer1 = new MockTransformerBuilder().from(dataTypeB).to(dataTypeC).returning(new C()).build();

        DefaultMuleMessage message = new DefaultMuleMessage(new B(), muleContext);
        message.applyTransformers(null, transformer1);

        assertTrue(message.getPayload() instanceof C);
        verifyTransformerExecuted(transformer1);
    }

    @Test
    public void failsOnTransformerWhenOriginalPayloadMatchesExpectedOutputType() throws MuleException
    {
        // Transformer(B->C), payload C: FAIL
        Transformer transformer1 = new MockTransformerBuilder().from(dataTypeB).to(dataTypeC).build();

        DefaultMuleMessage message = new DefaultMuleMessage(new C(), muleContext);
        try
        {
            message.applyTransformers(null, transformer1);
            fail("Transformation is supposed to fail");
        }
        catch (IllegalArgumentException expected)
        {
        }
        verifyTransformerNotExecuted(transformer1);
    }

    @Test
    public void failsWhenNoImplicitConversionAvailable() throws MuleException
    {
        Transformer transformer = new MockTransformerBuilder().from(DataTypeFactory.BYTE_ARRAY).to(DataTypeFactory.STRING).build();

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
        verifyTransformerNotExecuted(transformer);
    }

    @Test
    public void appliesImplicitConversionWhenAvailable() throws MuleException
    {
        Transformer transformer = new MockTransformerBuilder().from(DataTypeFactory.BYTE_ARRAY).to(DataTypeFactory.STRING).returning("bar").build();
        Transformer converter = new MockConverterBuilder().from(DataTypeFactory.STRING).to(DataTypeFactory.BYTE_ARRAY).returning("bar".getBytes()).build();

        when(conversionResolver.resolve(Mockito.any(DataType.class), Mockito.anyList())).thenReturn(converter);

        DefaultMuleMessage message = new DefaultMuleMessage("TEST", muleContext);

        message.applyTransformers(null, transformer);

        assertEquals("bar", message.getPayload());
        verifyTransformerExecuted(transformer);
        verifyTransformerExecuted(converter);
    }

    private void verifyTransformerNotExecuted(Transformer converter1) throws TransformerException
    {
        Mockito.verify(converter1, Mockito.times(0)).transform(Mockito.any(Object.class));
    }

    private void verifyTransformerExecuted(Transformer converter1) throws TransformerException
    {
        Mockito.verify(converter1, Mockito.times(1)).transform(Mockito.any(Object.class));
    }
}
