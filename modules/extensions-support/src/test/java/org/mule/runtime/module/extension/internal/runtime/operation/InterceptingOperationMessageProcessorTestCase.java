/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.operation;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.mule.runtime.core.api.message.InternalMessage;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.policy.DefaultPolicyManager;
import org.mule.runtime.core.policy.NullPolicyProvider;
import org.mule.runtime.core.policy.PolicyManager;
import org.mule.runtime.extension.api.runtime.operation.InterceptingCallback;
import org.mule.tck.size.SmallTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class InterceptingOperationMessageProcessorTestCase extends AbstractOperationMessageProcessorTestCase {

  @Mock(answer = RETURNS_DEEP_STUBS)
  private InterceptingCallback interceptingCallback;

  @Mock
  private Processor next;

  @Override
  @Before
  public void before() throws Exception {
    super.before();
    when(interceptingCallback.shouldProcessNext()).thenReturn(true);
    when(operationExecutor.execute(any())).thenReturn(interceptingCallback);
  }

  @Override
  protected OperationMessageProcessor createOperationMessageProcessor() {
    InterceptingOperationMessageProcessor messageProcessor =
        new InterceptingOperationMessageProcessor(extensionModel, operationModel, configurationProvider, target, resolverSet,
                                                  extensionManager, mockPolicyManager);

    messageProcessor.setListener(next);

    return messageProcessor;
  }

  @Test
  public void nextExecuted() throws Exception {
    org.mule.runtime.core.api.Event resultEvent = mock(org.mule.runtime.core.api.Event.class);
    InternalMessage resultMessage = mock(InternalMessage.class);
    when(resultEvent.getMessage()).thenReturn(resultMessage);
    when(next.process(any())).thenReturn(resultEvent);

    assertThat(messageProcessor.process(event), is(sameInstance(resultEvent)));
    verify(next).process(any());
    verify(interceptingCallback).onSuccess(resultMessage);
    verify(interceptingCallback, never()).onException(any());
    verify(interceptingCallback).onComplete();
  }

  @Test
  public void nextSkipped() throws Exception {
    when(interceptingCallback.shouldProcessNext()).thenReturn(false);

    messageProcessor.process(event);
    verify(next, never()).process(any());
    verify(interceptingCallback, never()).onSuccess(any());
    verify(interceptingCallback, never()).onException(any());
    verify(interceptingCallback).onComplete();
  }

  @Test
  public void nextFails() throws Exception {
    final Exception exception = new RuntimeException();
    when(next.process(any())).thenThrow(exception);

    try {
      messageProcessor.process(event);
      fail("Should have failed");
    } catch (Exception e) {
      verify(next).process(any());
      verify(interceptingCallback, never()).onSuccess(any());
      verify(interceptingCallback).onException(exception);
      verify(interceptingCallback).onComplete();
    }
  }

  @Test
  public void thereIsNoNext() throws Exception {
    ((InterceptingOperationMessageProcessor) messageProcessor).setListener(null);

    messageProcessor.process(event);

    verify(next, never()).process(any());
    verify(interceptingCallback).onSuccess(any());
    verify(interceptingCallback, never()).onException(any());
    verify(interceptingCallback).onComplete();
  }

}
