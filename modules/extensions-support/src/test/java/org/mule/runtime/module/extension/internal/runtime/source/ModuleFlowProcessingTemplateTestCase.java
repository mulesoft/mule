/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.source;

import static java.util.Collections.emptyList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mule.runtime.core.internal.execution.SourcePolicyTestUtils.onCallback;
import static reactor.core.publisher.Mono.just;

import org.mule.runtime.api.component.execution.CompletableCallback;
import org.mule.runtime.api.util.Reference;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.internal.exception.MessagingException;
import org.mule.runtime.core.internal.execution.SourceResultAdapter;
import org.mule.runtime.core.internal.execution.SourcePolicyTestUtils;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.reactivestreams.Publisher;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class ModuleFlowProcessingTemplateTestCase extends AbstractMuleTestCase {

  @Mock
  private SourceResultAdapter message;

  @Mock
  private CoreEvent event;

  @Mock
  private Processor messageProcessor;

  @Mock
  private SourceCompletionHandler completionHandler;

  @Mock(lenient = true)
  private MessagingException messagingException;

  @Mock
  private Map<String, Object> mockParameters;

  private final RuntimeException runtimeException = new RuntimeException();

  private FlowProcessingTemplate template;

  @Before
  public void before() throws Exception {
    template = new FlowProcessingTemplate(message, messageProcessor, emptyList(), completionHandler);
    doAnswer(onCallback(callback -> callback.complete(null))).when(completionHandler).onCompletion(any(), any(), any());
    doAnswer(onCallback(callback -> callback.complete(null))).when(completionHandler).onFailure(any(), any(), any());
  }

  @Test
  public void getMuleEvent() throws Exception {
    assertThat(template.getSourceMessage(), is(sameInstance(message)));
  }

  @Test
  public void routeEvent() throws Exception {
    template.routeEvent(event);
    verify(messageProcessor).process(event);
  }

  @Test
  public void routeEventAsync() throws Exception {
    when(messageProcessor.apply(any(Publisher.class))).thenReturn(just(event));
    template.routeEventAsync(event);
    verify(messageProcessor).apply(any(Publisher.class));
  }

  @Test
  public void sendResponseToClient() throws Throwable {
    Reference<CompletableCallback<Void>> callbackReference = new Reference<>();
    SourcePolicyTestUtils.<Void>block(callback -> {
      callbackReference.set(callback);
      template.sendResponseToClient(event, mockParameters, callback);
    });

    assertThat(callbackReference, is(notNullValue()));
    verify(completionHandler).onCompletion(same(event), same(mockParameters), same(callbackReference.get()));
  }

  @Test
  public void failedToSendResponseToClient() throws Throwable {
    Reference<Throwable> exceptionReference = new Reference<>();
    Reference<CompletableCallback<Void>> callbackReference = new Reference<>();
    doAnswer(SourcePolicyTestUtils.<Void>onCallback(callback -> {
      callbackReference.set(callback);
      callback.error(runtimeException);
    })).when(completionHandler).onCompletion(same(event), same(mockParameters), any());

    try {
      SourcePolicyTestUtils.<Void>block(callback -> {
        callback = callback.before(new CompletableCallback<Void>() {

          @Override
          public void complete(Void value) {

          }

          @Override
          public void error(Throwable e) {
            exceptionReference.set(e);
          }
        });

        template.sendResponseToClient(event, mockParameters, callback);
      });
      fail("This should have failed");
    } catch (Exception e) {
      assertThat(e, is(sameInstance(runtimeException)));
    }

    verify(completionHandler, never()).onFailure(any(MessagingException.class), same(mockParameters), any());
    assertThat(exceptionReference.get(), equalTo(runtimeException));
  }

  @Test
  public void sendFailureResponseToClient() throws Throwable {
    Reference<CompletableCallback<Void>> callbackReference = new Reference<>();
    SourcePolicyTestUtils.<Void>block(callback -> {
      callbackReference.set(callback);
      template.sendFailureResponseToClient(messagingException, mockParameters, callback);
    });

    assertThat(callbackReference.get(), is(notNullValue()));
    verify(completionHandler).onFailure(messagingException, mockParameters, callbackReference.get());
  }

  @Test
  public void failedToSendFailureResponseToClient() throws Throwable {
    Reference<Throwable> exceptionReference = new Reference<>();
    when(messagingException.getEvent()).thenReturn(event);
    doAnswer(onCallback(callback -> callback.error(runtimeException))).when(completionHandler)
        .onFailure(same(messagingException), same(mockParameters), any());

    try {
      SourcePolicyTestUtils.<Void>block(callback -> {
        callback = callback.before(new CompletableCallback<Void>() {

          @Override
          public void complete(Void value) {

          }

          @Override
          public void error(Throwable e) {
            exceptionReference.set(e);
          }
        });

        template.sendFailureResponseToClient(messagingException, mockParameters, callback);
      });
      fail("This should have failed");
    } catch (Exception e) {
      assertThat(e, is(sameInstance(runtimeException)));
    }

    assertThat(exceptionReference.get(), equalTo(runtimeException));
  }
}
