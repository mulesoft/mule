/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.operation;

import static java.util.Optional.empty;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.runtime.api.metadata.MediaType.ANY;
import static org.mule.runtime.api.metadata.MediaType.APPLICATION_JSON;
import static org.mule.runtime.core.api.util.SystemUtils.getDefaultEncoding;
import static org.mule.tck.util.MuleContextUtils.eventBuilder;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.meta.model.ComponentModel;
import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.mule.runtime.module.extension.api.runtime.privileged.ExecutionContextAdapter;
import org.mule.runtime.module.extension.internal.loader.java.property.MediaTypeModelProperty;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.tck.size.SmallTest;

import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public abstract class ValueReturnDelegateContractTestCase extends AbstractMuleContextTestCase {

  @Mock
  protected ExecutionContextAdapter operationContext;

  @Mock(answer = RETURNS_DEEP_STUBS)
  protected ComponentModel componentModel;

  protected CoreEvent event;

  @Mock
  protected Object attributes;

  protected ReturnDelegate delegate;

  @Before
  public void before() throws MuleException {
    event = eventBuilder(muleContext).message(Message.builder().value("").attributesValue(attributes).build()).build();
    when(componentModel.getModelProperty(MediaTypeModelProperty.class)).thenReturn(empty());
    delegate = createReturnDelegate();
    when(operationContext.getEvent()).thenReturn(event);
    when(operationContext.getMuleContext()).thenReturn(muleContext);
  }

  @Test
  public void returnsSingleValue() {
    byte[] value = new byte[] {};
    CoreEvent result = delegate.asReturnValue(value, operationContext);

    Message message = getOutputMessage(result);

    assertThat(message.getPayload().getValue(), is(sameInstance(value)));
    assertThat(message.getPayload().getDataType().getType().equals(byte[].class), is(true));
  }

  @Test
  public void operationReturnsOperationResultButKeepsAttributes() throws Exception {
    Object payload = new Object();
    MediaType mediaType = ANY.withCharset(getDefaultEncoding(muleContext));

    CoreEvent result =
        delegate.asReturnValue(Result.builder().output(payload).mediaType(mediaType).build(), operationContext);

    Message message = getOutputMessage(result);

    assertThat(message.getPayload().getValue(), is(sameInstance(payload)));
    assertThat(message.getAttributes().getValue(), is(nullValue()));
    assertThat(message.getPayload().getDataType().getMediaType(), equalTo(mediaType));
  }

  @Test
  public void operationReturnsOperationResultThatOnlySpecifiesPayload() throws Exception {
    Object payload = "hello world!";

    CoreEvent result = delegate.asReturnValue(Result.builder().output(payload).build(), operationContext);

    Message message = getOutputMessage(result);

    assertThat(message.getPayload().getValue(), is(sameInstance(payload)));
    assertThat(message.getAttributes().getValue(), is(nullValue()));
    assertThat(message.getPayload().getDataType().getType().equals(String.class), is(true));
  }

  @Test
  public void operationReturnsOperationResultThatOnlySpecifiesPayloadAndAttributes() throws Exception {
    Object payload = "hello world!";
    Object newAttributes = mock(Object.class);

    CoreEvent result =
        delegate.asReturnValue(Result.builder().output(payload).attributes(newAttributes).build(), operationContext);

    Message message = getOutputMessage(result);

    assertThat(message.getPayload().getValue(), is(sameInstance(payload)));
    assertThat(message.getAttributes().getValue(), is(sameInstance(newAttributes)));
    assertThat(message.getPayload().getDataType().getType().equals(String.class), is(true));
  }

  @Test
  public void operationWithDefaultMimeType() throws Exception {
    when(componentModel.getModelProperty(MediaTypeModelProperty.class)).thenReturn(Optional.of(
                                                                                               new MediaTypeModelProperty(APPLICATION_JSON
                                                                                                   .toRfcString(), true)));
    delegate = createReturnDelegate();

    Object value = "Hello world!";
    CoreEvent result = delegate.asReturnValue(value, operationContext);

    Message message = getOutputMessage(result);

    assertThat(message.getPayload().getValue(), is(sameInstance(value)));
    assertThat(message.getPayload().getDataType().getType().equals(String.class), is(true));
    assertThat(message.getPayload().getDataType().getMediaType().toRfcString(), containsString(APPLICATION_JSON.toRfcString()));
  }

  protected abstract ReturnDelegate createReturnDelegate();

  protected abstract Message getOutputMessage(CoreEvent result);
}
