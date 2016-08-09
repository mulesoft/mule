/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.source;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import org.mule.runtime.api.execution.CompletionHandler;
import org.mule.runtime.api.execution.ExceptionCallback;
import org.mule.runtime.core.DefaultMuleEvent;
import org.mule.runtime.core.api.MessagingException;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.processor.MessageProcessor;
import org.mule.runtime.core.execution.ResponseCompletionCallback;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class ExtensionFlowProcessingTemplateTestCase extends AbstractMuleTestCase {

  @Mock
  private DefaultMuleEvent event;

  @Mock
  private MessageProcessor messageProcessor;

  @Mock
  private CompletionHandler completionHandler;

  @Mock
  private ResponseCompletionCallback responseCompletionCallback;

  @Mock
  private ExceptionCallback<org.mule.runtime.api.message.MuleEvent, Exception> exceptionCallback;

  @Mock
  private MessagingException messagingException;

  private RuntimeException runtimeException = new RuntimeException();

  private ExtensionFlowProcessingTemplate template;

  @Before
  public void before() {
    template = new ExtensionFlowProcessingTemplate(event, messageProcessor, completionHandler);
  }

  @Test
  public void getMuleEvent() throws Exception {
    assertThat(template.getMuleEvent(), is(sameInstance(event)));
  }

  @Test
  public void routeEvent() throws Exception {
    template.routeEvent(event);
    verify(messageProcessor).process(event);
  }

  @Test
  public void sendResponseToClient() throws MuleException {
    template.sendResponseToClient(event, responseCompletionCallback);
    verify(completionHandler).onCompletion(same(event), any(ExtensionSourceExceptionCallback.class));
    verify(responseCompletionCallback).responseSentSuccessfully();
  }

  @Test
  public void failedToSendResponseToClient() throws MuleException {
    doThrow(runtimeException).when(completionHandler).onCompletion(same(event), any(ExtensionSourceExceptionCallback.class));
    template.sendResponseToClient(event, responseCompletionCallback);

    verify(completionHandler, never()).onFailure(any(Exception.class));
    verify(responseCompletionCallback).responseSentWithFailure(runtimeException, event);
  }

  @Test
  public void sendFailureResponseToClient() throws Exception {
    template.sendFailureResponseToClient(messagingException, responseCompletionCallback);
    verify(completionHandler).onFailure(messagingException);
    verify(responseCompletionCallback).responseSentSuccessfully();
  }

  @Test
  public void failedToSendFailureResponseToClient() throws Exception {
    doThrow(runtimeException).when(completionHandler).onFailure(messagingException);
    template.sendFailureResponseToClient(messagingException, responseCompletionCallback);
    verify(responseCompletionCallback).responseSentWithFailure(runtimeException, event);
  }
}
