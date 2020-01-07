/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.operation;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.mule.runtime.api.message.Message;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.tck.size.SmallTest;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class TargetReturnDelegateTestCase extends ValueReturnDelegateTestCase {

  protected static final String TARGET = "myFlowVar";

  @Override
  protected ReturnDelegate createReturnDelegate() {
    return new TargetReturnDelegate(TARGET, "#[message]", componentModel, muleContext.getExpressionManager(),
                                    getCursorProviderFactory(),
                                    muleContext);
  }

  @After
  public void after() {
    assertThat(event.getMessage().getPayload().getValue(), is(""));
  }

  @Override
  protected Message getOutputMessage(CoreEvent result) {
    Message message = (Message) result.getVariables().get(TARGET).getValue();

    assertThat(message, is(notNullValue()));
    return message;
  }

  @Override
  @Test
  public void evaluateEvent() {
    CoreEvent event = mock(CoreEvent.class);
    final String payload = "Hello there!";
    Message message = Message.of(payload);
    when(event.getMessage()).thenReturn(message);

    CoreEvent resultEvent = delegate.asReturnValue(event, operationContext);
    Message resultMessage = getOutputMessage(resultEvent);
    assertThat(resultMessage.getPayload().getValue(), is(payload));
  }
}
