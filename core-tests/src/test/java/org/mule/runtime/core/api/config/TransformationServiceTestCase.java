/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.config;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mule.runtime.api.message.Message.of;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.transformer.DataTypeConversionResolver;
import org.mule.runtime.core.api.transformer.MessageTransformerException;
import org.mule.runtime.core.api.transformer.Transformer;
import org.mule.runtime.core.api.transformer.TransformerException;
import org.mule.runtime.core.internal.transformer.builder.MockConverterBuilder;
import org.mule.runtime.core.internal.transformer.builder.MockTransformerBuilder;
import org.mule.runtime.core.privileged.transformer.ExtendedTransformationService;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.io.ByteArrayInputStream;

import org.junit.Before;
import org.junit.Test;

@SmallTest
public class TransformationServiceTestCase extends AbstractMuleTestCase {

  private class A {

  }

  private class B {

  }

  private class C {

  }

  private class D {

  }

  private MuleContext muleContext = mock(MuleContext.class, RETURNS_DEEP_STUBS);
  private DataTypeConversionResolver conversionResolver = mock(DataTypeConversionResolver.class);
  private ExtendedTransformationService transformationService;

  @Before
  public void setUp() throws Exception {
    when(muleContext.getDataTypeConverterResolver()).thenReturn(conversionResolver);
    this.transformationService = new ExtendedTransformationService(muleContext);
  }

  private static final DataType dataTypeB = DataType.fromType(B.class);
  private static final DataType dataTypeC = DataType.fromType(C.class);
  private static final DataType dataTypeD = DataType.fromType(D.class);

  @Test
  public void failsOnConverterWhenSourceAndReturnTypeDoesNotMatchAndThereIsNoImplicitConversion() throws MuleException {
    // Converter(B->C), payload A: FAIL
    Transformer converter1 = new MockConverterBuilder().from(dataTypeB).to(dataTypeC).build();

    Message message = of(new A());

    try {
      transformationService.applyTransformers(message, null, converter1);
      fail("Transformation is supposed to fail");
    } catch (IllegalArgumentException expected) {
    }
    verifyTransformerNotExecuted(converter1);
  }

  @Test
  public void failsOnConverterWhenSourceAndReturnTypeDoesNotMatchAndThereIsNoImplicitConversion2() throws MuleException {
    // Converter(B->C), payload A: FAIL
    ByteArrayInputStream payload = new ByteArrayInputStream(TEST_PAYLOAD.getBytes());
    DataType originalSourceType = DataType.fromType(payload.getClass());

    Transformer converter1 = new MockConverterBuilder().from(originalSourceType).to(dataTypeC).build();
    A transformedPayload = new A();
    when(converter1.transform(any())).thenReturn(transformedPayload);

    Message message = of(payload);

    try {
      transformationService.applyTransformers(message, null, converter1);
      fail("Transformation is supposed to fail");
    } catch (MessageTransformerException expected) {
      assertThat(expected.getErrorMessage().getPayload().getValue(), is(transformedPayload));
    }
  }

  @Test
  public void appliesImplicitConversionOnConverterWhenSourceAndReturnTypeDoesNotMatch() throws MuleException {
    // Converter(C->D), payload B: uses implicit conversion B->C
    Transformer converter1 = new MockConverterBuilder().from(dataTypeC).to(dataTypeD).returning(new D()).build();
    Transformer converter2 = new MockConverterBuilder().from(dataTypeB).to(dataTypeC).returning(new C()).build();

    when(conversionResolver.resolve(any(DataType.class), anyList())).thenReturn(converter2);

    Message message = of(new B());

    message = transformationService.applyTransformers(message, null, converter1);

    assertTrue(message.getPayload().getValue() instanceof D);
    verifyTransformerExecuted(converter1);
    verifyTransformerExecuted(converter2);
  }

  @Test
  public void appliesConverter() throws MuleException {
    // Converter(B->C), payload B: OK
    Transformer converter1 = new MockConverterBuilder().from(dataTypeB).to(dataTypeC).returning(new C()).build();

    Message message = of(new B());
    message = transformationService.applyTransformers(message, null, converter1);

    assertTrue(message.getPayload().getValue() instanceof C);
    verifyTransformerExecuted(converter1);
  }

  @Test
  public void skipsConverterThatDoesNotMatchWhenOriginalPayloadMatchesExpectedOutputType() throws MuleException {
    // Converter(B->C), payload C: OK - skips transformer but C is the expected output type -> OK
    Transformer converter1 = new MockConverterBuilder().from(dataTypeB).to(dataTypeC).build();

    Message message = of(new C());
    message = transformationService.applyTransformers(message, null, converter1);

    assertTrue(message.getPayload().getValue() instanceof C);
    verifyTransformerNotExecuted(converter1);
  }

  @Test
  public void failsTransformationUsingConverterWhenSourceAndReturnTypeDoesNotMatch2() throws MuleException {
    // Converter(B -> C) Converter(C->D), payload A: FAIL
    Transformer converter1 = new MockConverterBuilder().from(dataTypeB).to(dataTypeC).build();
    Transformer converter2 = new MockConverterBuilder().from(dataTypeC).to(dataTypeD).build();

    Message message = of(new A());
    try {
      transformationService.applyTransformers(message, null, converter1, converter2);
      fail("Transformation is supposed to fail");
    } catch (IllegalArgumentException expected) {
    }
    verifyTransformerNotExecuted(converter1);
    verifyTransformerNotExecuted(converter2);
  }

  @Test
  public void appliesBothConverters() throws MuleException {
    // Converter(B -> C) Converter(C->D), payload B: converts B->C, C->D
    Transformer converter1 = new MockConverterBuilder().from(dataTypeB).to(dataTypeC).returning(new C()).build();
    Transformer converter2 = new MockConverterBuilder().from(dataTypeC).to(dataTypeD).returning(new D()).build();

    Message message = of(new B());
    message = transformationService.applyTransformers(message, null, converter1, converter2);

    assertTrue(message.getPayload().getValue() instanceof D);
    verifyTransformerExecuted(converter1);
    verifyTransformerExecuted(converter2);
  }

  @Test
  public void skipsFirstConverterAppliesSecond() throws MuleException {
    // Converter(B -> C) Converter(C->D), payload C: skips converter(B->C), applies Converter(C->D)
    Transformer converter1 = new MockConverterBuilder().from(dataTypeB).to(dataTypeC).build();
    Transformer converter2 = new MockConverterBuilder().from(dataTypeC).to(dataTypeD).returning(new D()).build();

    Message message = of(new C());
    message = transformationService.applyTransformers(message, null, converter1, converter2);

    assertTrue(message.getPayload().getValue() instanceof D);
    verifyTransformerNotExecuted(converter1);
    verifyTransformerExecuted(converter2);
  }

  @Test
  public void skipBothConvertersButPayloadMatchesExpectedOutputType() throws MuleException {
    // Converter(B -> C) Converter(C->D), payload D: skips converter(B-C), skips converter(C->D), but D is the expected output
    // type -> OK
    Transformer converter1 = new MockConverterBuilder().from(dataTypeB).to(dataTypeC).build();
    Transformer converter2 = new MockConverterBuilder().from(dataTypeC).to(dataTypeD).build();

    Message message = of(new D());
    message = transformationService.applyTransformers(message, null, converter1, converter2);

    assertTrue(message.getPayload().getValue() instanceof D);
    verifyTransformerNotExecuted(converter1);
    verifyTransformerNotExecuted(converter2);
  }

  @Test
  public void failsTransformerIgnoringNonMatchingConverter() throws MuleException {
    // Transformer(B -> D) Converter(C->D), payload A: FAIL
    Transformer transformer1 = new MockTransformerBuilder().from(dataTypeB).to(dataTypeD).build();
    Transformer converter2 = new MockConverterBuilder().from(dataTypeC).to(dataTypeD).build();

    Message message = of(new A());
    try {
      transformationService.applyTransformers(message, null, transformer1, converter2);
      fail("Transformation is supposed to fail");
    } catch (IllegalArgumentException expected) {
    }
    verifyTransformerNotExecuted(transformer1);
    verifyTransformerNotExecuted(converter2);
  }

  @Test
  public void appliesTransformerSkipsConverter() throws MuleException {
    // Transformer(B -> D) Converter(C->D), payload B: converts B->D, skips converter C->D, resulting output is of the expected
    // type -> OK
    Transformer transformer1 = new MockTransformerBuilder().from(dataTypeB).to(dataTypeD).returning(new D()).build();
    Transformer converter2 = new MockConverterBuilder().from(dataTypeC).to(dataTypeD).build();

    Message message = of(new B());
    message = transformationService.applyTransformers(message, null, transformer1, converter2);

    assertTrue(message.getPayload().getValue() instanceof D);
    verifyTransformerExecuted(transformer1);
    verifyTransformerNotExecuted(converter2);
  }

  @Test
  public void failsTransformerIgnoringMatchingConverter() throws MuleException {
    // Transformer(B -> D) Converter(C->D), payload C: FAIL
    Transformer transformer1 = new MockTransformerBuilder().from(dataTypeB).to(dataTypeD).build();
    Transformer converter2 = new MockConverterBuilder().from(dataTypeC).to(dataTypeD).build();

    Message message = of(new C());
    try {
      transformationService.applyTransformers(message, null, transformer1, converter2);
      fail("Transformation is supposed to fail");
    } catch (IllegalArgumentException expected) {
    }
    verifyTransformerNotExecuted(transformer1);
    verifyTransformerNotExecuted(converter2);
  }

  @Test
  public void failsTransformerIgnoringMatchingConverterWhenOriginalPayloadMatchesExpectedOutputType() throws MuleException {
    // Transformer(B -> D) Converter(C->D), payload D: FAIL
    Transformer transformer1 = new MockTransformerBuilder().from(dataTypeB).to(dataTypeD).build();
    Transformer converter2 = new MockConverterBuilder().from(dataTypeC).to(dataTypeD).build();

    Message message = of(new D());
    try {
      transformationService.applyTransformers(message, null, transformer1, converter2);
      fail("Transformation is supposed to fail");
    } catch (IllegalArgumentException expected) {
    }
    verifyTransformerNotExecuted(transformer1);
    verifyTransformerNotExecuted(converter2);
  }

  @Test
  public void skipsConverterFailsOnTransformer() throws MuleException {
    // Converter(B -> D) Transformer(C->D), payload A: FAIL
    Transformer converter1 = new MockConverterBuilder().from(dataTypeB).to(dataTypeD).build();
    Transformer transformer2 = new MockTransformerBuilder().from(dataTypeC).to(dataTypeD).build();

    Message message = of(new A());

    try {
      transformationService.applyTransformers(message, null, converter1, transformer2);
      fail("Transformation is supposed to fail");
    } catch (IllegalArgumentException expected) {
    }
    verifyTransformerNotExecuted(converter1);
    verifyTransformerNotExecuted(transformer2);
  }

  @Test
  public void appliesConverterFailsOnTransformer() throws MuleException {
    // Converter(B -> D) Transformer(C->D), payload B: converts B-> D, cannot apply transformer -> FAIL
    Transformer converter1 = new MockConverterBuilder().from(dataTypeB).to(dataTypeD).returning(new D()).build();
    Transformer transformer2 = new MockTransformerBuilder().from(dataTypeC).to(dataTypeD).build();

    Message message = of(new B());
    try {
      transformationService.applyTransformers(message, null, converter1, transformer2);
      fail("Transformation is supposed to fail");
    } catch (IllegalArgumentException expected) {
    }
    verifyTransformerExecuted(converter1);
    verifyTransformerNotExecuted(transformer2);
  }

  @Test
  public void skipsConverterAppliesTransformer() throws MuleException {
    // Converter(B -> D) Transformer(C->D), payload C: skips converter, transforms C to D -> OK
    Transformer converter1 = new MockConverterBuilder().from(dataTypeB).to(dataTypeD).build();
    Transformer transformer2 = new MockTransformerBuilder().from(dataTypeC).to(dataTypeD).returning(new D()).build();

    Message message = of(new C());
    message = transformationService.applyTransformers(message, null, converter1, transformer2);

    assertTrue(message.getPayload().getValue() instanceof D);
    verifyTransformerNotExecuted(converter1);
    verifyTransformerExecuted(transformer2);
  }

  @Test
  public void skipsConverterFailsOnTransformerWhenOriginalPayloadMatchesExpectedOutputType() throws MuleException {
    // Converter(B -> D) Transformer(C->D), payload D: FAIL
    Transformer converter1 = new MockConverterBuilder().from(dataTypeB).to(dataTypeD).build();
    Transformer transformer2 = new MockTransformerBuilder().from(dataTypeC).to(dataTypeD).build();

    Message message = of(new D());
    try {
      transformationService.applyTransformers(message, null, converter1, transformer2);
      fail("Transformation is supposed to fail");
    } catch (IllegalArgumentException expected) {
    }
    verifyTransformerNotExecuted(converter1);
    verifyTransformerNotExecuted(transformer2);
  }

  @Test
  public void failsOnFirstTransformer() throws MuleException {
    // Transformer(B ->D) Transformer(C->D), payload A: FAIL
    Transformer transformer1 = new MockTransformerBuilder().from(dataTypeB).to(dataTypeD).build();
    Transformer transformer2 = new MockTransformerBuilder().from(dataTypeC).to(dataTypeD).build();

    Message message = of(new A());

    try {
      transformationService.applyTransformers(message, null, transformer1, transformer2);
      fail("Transformation is supposed to fail");
    } catch (IllegalArgumentException expected) {
    }
    verifyTransformerNotExecuted(transformer1);
    verifyTransformerNotExecuted(transformer2);
  }

  @Test
  public void appliesFirstTransformerFailsOnSecondTransformer() throws MuleException {
    // Transformer(B ->D) Transformer(C->D), payload B: applies first transformer, cannot apply second transformer -> FAIL
    Transformer transformer1 = new MockTransformerBuilder().from(dataTypeB).to(dataTypeD).returning(new D()).build();
    Transformer transformer2 = new MockTransformerBuilder().from(dataTypeC).to(dataTypeD).build();

    Message message = of(new B());

    try {
      transformationService.applyTransformers(message, null, transformer1, transformer2);
      fail("Transformation is supposed to fail");
    } catch (IllegalArgumentException expected) {
    }
    verifyTransformerExecuted(transformer1);
    verifyTransformerNotExecuted(transformer2);
  }

  @Test
  public void failsOnFirstTransformerIgnoresSecondTransformer() throws MuleException {
    // Transformer(B ->D) Transformer(C->D), payload C: FAIL
    Transformer transformer1 = new MockTransformerBuilder().from(dataTypeB).to(dataTypeD).build();
    Transformer transformer2 = new MockTransformerBuilder().from(dataTypeC).to(dataTypeD).build();

    Message message = of(new C());

    try {
      transformationService.applyTransformers(message, null, transformer1, transformer2);
      fail("Transformation is supposed to fail");
    } catch (IllegalArgumentException expected) {
    }
    verifyTransformerNotExecuted(transformer1);
    verifyTransformerNotExecuted(transformer2);
  }

  @Test
  public void failsOnFirstTransformerIgnoresSecondTransformerWhenOriginalPayloadMatchesExpectedOutputType() throws MuleException {
    // Transformer(B ->D) Transformer(C->D), payload D: FAIL
    Transformer transformer1 = new MockTransformerBuilder().from(dataTypeB).to(dataTypeD).build();
    Transformer transformer2 = new MockTransformerBuilder().from(dataTypeC).to(dataTypeD).build();

    Message message = of(new D());

    try {
      transformationService.applyTransformers(message, null, transformer1, transformer2);
      fail("Transformation is supposed to fail");
    } catch (IllegalArgumentException expected) {
    }
    verifyTransformerNotExecuted(transformer1);
    verifyTransformerNotExecuted(transformer2);
  }

  @Test
  public void failsOnTransformerWhenSourceAndReturnTypeDoesNotMatch() throws MuleException {
    // Transformer(B->C), payload A: FAIL
    Transformer transformer1 = new MockTransformerBuilder().from(dataTypeB).to(dataTypeC).build();

    Message message = of(new A());

    try {
      transformationService.applyTransformers(message, null, transformer1);
      fail("Transformation is supposed to fail");
    } catch (IllegalArgumentException expected) {
    }
    verifyTransformerNotExecuted(transformer1);
  }

  @Test
  public void appliesTransformer() throws MuleException {
    // Transformer(B->C), payload B: OK
    Transformer transformer1 = new MockTransformerBuilder().from(dataTypeB).to(dataTypeC).returning(new C()).build();

    Message message = of(new B());
    message = transformationService.applyTransformers(message, null, transformer1);

    assertTrue(message.getPayload().getValue() instanceof C);
    verifyTransformerExecuted(transformer1);
  }

  @Test
  public void failsOnTransformerWhenOriginalPayloadMatchesExpectedOutputType() throws MuleException {
    // Transformer(B->C), payload C: FAIL
    Transformer transformer1 = new MockTransformerBuilder().from(dataTypeB).to(dataTypeC).build();

    Message message = of(new C());
    try {
      transformationService.applyTransformers(message, null, transformer1);
      fail("Transformation is supposed to fail");
    } catch (IllegalArgumentException expected) {
    }
    verifyTransformerNotExecuted(transformer1);
  }

  @Test
  public void failsWhenNoImplicitConversionAvailable() throws MuleException {
    Transformer transformer = new MockTransformerBuilder().from(DataType.BYTE_ARRAY).to(DataType.STRING).build();

    when(conversionResolver.resolve(any(DataType.class), anyList())).thenReturn(null);

    Message message = of("TEST");

    try {
      transformationService.applyTransformers(message, null, transformer);
      fail("Transformation is supposed to fail");
    } catch (IllegalArgumentException expected) {
    }
    verifyTransformerNotExecuted(transformer);
  }

  @Test
  public void appliesImplicitConversionWhenAvailable() throws MuleException {
    Transformer transformer = new MockTransformerBuilder().from(DataType.BYTE_ARRAY).to(DataType.STRING).returning("bar").build();
    Transformer converter =
        new MockConverterBuilder().from(DataType.STRING).to(DataType.BYTE_ARRAY).returning("bar".getBytes()).build();

    when(conversionResolver.resolve(any(DataType.class), anyList())).thenReturn(converter);

    Message message = of("TEST");
    message = transformationService.applyTransformers(message, null, transformer);

    assertEquals("bar", message.getPayload().getValue());
    verifyTransformerExecuted(transformer);
    verifyTransformerExecuted(converter);
  }

  private void verifyTransformerNotExecuted(Transformer converter1) throws TransformerException {
    verify(converter1, times(0)).transform(any(Object.class));
  }

  private void verifyTransformerExecuted(Transformer converter1) throws TransformerException {
    verify(converter1, times(1)).transform(any(Object.class));
  }
}
