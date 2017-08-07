/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.operation;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.when;
import static org.mule.runtime.api.meta.TargetType.MESSAGE;
import static org.mule.runtime.api.meta.TargetType.PAYLOAD;
import static org.mule.tck.util.MuleContextUtils.eventBuilder;
import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.getDefaultCursorStreamProviderFactory;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.meta.TargetType;
import org.mule.runtime.api.meta.model.ComponentModel;
import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.mule.runtime.module.extension.internal.runtime.ExecutionContextAdapter;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.nio.charset.Charset;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class TargetOutputMessageReturnDelegateTestCase extends AbstractMuleTestCase {

  private static final String TARGET = "myFlowVar";

  @Mock(answer = RETURNS_DEEP_STUBS)
  protected MuleContext muleContext;

  @Mock
  protected ExecutionContextAdapter operationContext;

  @Mock(answer = RETURNS_DEEP_STUBS)
  protected ComponentModel componentModel;

  protected Event event;

  @Mock
  protected Object attributes;

  protected ReturnDelegate delegate;

  private final Object payload = "hello world!";

  @Before
  public void before() throws MuleException {
    event = eventBuilder().message(Message.builder().value("").attributesValue(attributes).build()).build();
    when(operationContext.getEvent()).thenReturn(event);
  }

  private TargetReturnDelegate createDelegate(TargetType output) {
    return new TargetReturnDelegate(TARGET, output, componentModel, getDefaultCursorStreamProviderFactory(), muleContext);
  }

  @Test
  public void operationTargetMessage() {
    delegate = createDelegate(MESSAGE);

    Event result = delegate.asReturnValue(payload, operationContext);
    assertMessage(result.getMessage());
    assertThat(result.getVariables().get(TARGET).getValue(), is(instanceOf(Message.class)));
    Message message = (Message) result.getVariables().get(TARGET).getValue();
    assertThat(message.getPayload().getValue(), is(payload));
  }

  @Test
  public void operationTargetPayload() {
    delegate = createDelegate(PAYLOAD);
    Event result = delegate.asReturnValue(payload, operationContext);
    assertMessage(result.getMessage());
    assertThat(result.getVariables().get(TARGET).getValue(), is(payload));
  }

  @Test
  public void operationTargetPayloadWithResult() {
    delegate = createDelegate(PAYLOAD);
    MediaType mediaType = MediaType.APPLICATION_JSON.withCharset(Charset.defaultCharset());
    Event result = delegate.asReturnValue(Result.builder().output(payload).mediaType(mediaType).build(), operationContext);
    assertMessage(result.getMessage());
    assertThat(result.getVariables().get(TARGET).getValue(), is(payload));
    assertThat(result.getVariables().get(TARGET).getDataType().getMediaType(), is(mediaType));
  }

  private void assertMessage(Message message) {
    assertThat(message.getPayload().getValue(), is(""));
    assertThat(message.getAttributes().getValue(), is(attributes));
    assertThat(message.getPayload().getDataType().getType().equals(String.class), is(true));
  }
}
