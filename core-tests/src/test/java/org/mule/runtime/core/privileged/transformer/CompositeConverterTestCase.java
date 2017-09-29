/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.privileged.transformer;

import static java.nio.charset.StandardCharsets.UTF_8;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.transformer.Converter;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.util.Arrays;

import org.junit.Test;

@SmallTest
public class CompositeConverterTestCase extends AbstractMuleTestCase {

  private Converter mockConverterA = mock(Converter.class);
  private Converter mockConverterB = mock(Converter.class);

  @Test(expected = IllegalArgumentException.class)
  public void rejectsEmptyCompositeTransformer() throws Exception {
    new CompositeConverter();
  }

  @Test
  public void isSourceDataTypeSupported() {
    Converter converter = mock(Converter.class);
    when(converter.isSourceDataTypeSupported(DataType.STRING)).thenReturn(true);
    CompositeConverter chain = new CompositeConverter(converter);

    assertTrue(chain.isSourceDataTypeSupported(DataType.STRING));
  }

  @Test
  public void getSourceDataTypes() {
    DataType[] dataTypes = new DataType[] {DataType.STRING};
    Converter converter = mock(Converter.class);
    when(converter.getSourceDataTypes()).thenReturn(Arrays.asList(dataTypes));
    CompositeConverter chain = new CompositeConverter(converter);

    assertEquals(DataType.STRING, chain.getSourceDataTypes().get(0));
  }

  @Test
  public void isAcceptNull() {
    Converter converter = mock(Converter.class);
    when(converter.isAcceptNull()).thenReturn(true);
    CompositeConverter chain = new CompositeConverter(converter);

    assertTrue(chain.isAcceptNull());
  }

  @Test
  public void isIgnoreBadInput() {
    Converter converter = mock(Converter.class);
    when(converter.isIgnoreBadInput()).thenReturn(true);
    CompositeConverter chain = new CompositeConverter(converter);

    assertTrue(chain.isIgnoreBadInput());
  }

  @Test
  public void setReturnDataType() {
    Converter converter = mock(Converter.class);
    CompositeConverter chain = new CompositeConverter(converter);
    chain.setReturnDataType(DataType.STRING);

    verify(converter, atLeastOnce()).setReturnDataType(DataType.STRING);
  }

  @Test
  public void getReturnDataType() {
    doReturn(DataType.STRING).when(mockConverterB).getReturnDataType();
    CompositeConverter compositeConverter = new CompositeConverter(mockConverterA, mockConverterB);

    assertEquals(DataType.STRING, compositeConverter.getReturnDataType());
  }

  @Test
  public void priorityWeighting() throws Exception {
    CompositeConverter compositeConverter = new CompositeConverter(mockConverterA, mockConverterB);
    when(mockConverterA.getPriorityWeighting()).thenReturn(1);
    when(mockConverterB.getPriorityWeighting()).thenReturn(2);

    int priorityWeighting = compositeConverter.getPriorityWeighting();

    assertEquals(3, priorityWeighting);
  }

  @Test
  public void initialise() throws Exception {
    CompositeConverter compositeConverter = new CompositeConverter(mockConverterA, mockConverterB);

    compositeConverter.initialise();

    verify(mockConverterA, atLeastOnce()).initialise();
    verify(mockConverterB, atLeastOnce()).initialise();
  }

  @Test
  public void dispose() throws Exception {
    CompositeConverter compositeConverter = new CompositeConverter(mockConverterA, mockConverterB);

    compositeConverter.dispose();

    verify(mockConverterA, atLeastOnce()).dispose();
    verify(mockConverterB, atLeastOnce()).dispose();
  }

  @Test
  public void setMuleContext() {
    MuleContext mockMuleContext = mock(MuleContext.class);
    CompositeConverter compositeConverter = new CompositeConverter(mockConverterA, mockConverterB);

    compositeConverter.setMuleContext(mockMuleContext);

    verify(mockConverterA, atLeastOnce()).setMuleContext(mockMuleContext);
    verify(mockConverterB, atLeastOnce()).setMuleContext(mockMuleContext);
  }

  @Test
  public void transform() throws Exception {
    doReturn("MyOutput1").when(mockConverterA).transform(any());
    doReturn(DataType.builder().charset(UTF_8).build()).when(mockConverterA).getReturnDataType();
    doReturn("MyOutput2").when(mockConverterB).transform(eq("MyOutput1"), eq(UTF_8));
    doReturn(DataType.builder().charset(UTF_8).build()).when(mockConverterB).getReturnDataType();
    CompositeConverter compositeConverter = new CompositeConverter(mockConverterA, mockConverterB);

    Object output = compositeConverter.transform("MyInput");

    verify(mockConverterA, times(1)).transform("MyInput");
    verify(mockConverterB, times(1)).transform("MyOutput1", UTF_8);
    assertEquals("MyOutput2", output);
  }

  @Test
  public void appliesTransformerChainOnMessage() throws Exception {
    CompositeConverter compositeConverter = new CompositeConverter(mockConverterA, mockConverterB);
    MuleContext muleContext = mock(MuleContext.class, RETURNS_DEEP_STUBS);
    compositeConverter.setMuleContext(muleContext);
    final CoreEvent event = testEvent();
    ExtendedTransformationService transformationService = mock(ExtendedTransformationService.class);
    doReturn(mock(Message.class)).when(transformationService).applyTransformers(any(), eq(event), eq(compositeConverter));
    doReturn(transformationService).when(muleContext).getTransformationService();

    compositeConverter.process(event);

    verify(transformationService, times(1)).applyTransformers(eq(testEvent().getMessage()), eq(testEvent()),
                                                              eq(compositeConverter));
  }
}
