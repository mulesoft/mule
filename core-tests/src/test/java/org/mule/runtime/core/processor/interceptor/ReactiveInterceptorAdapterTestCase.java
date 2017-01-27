/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.processor.interceptor;

import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static java.util.concurrent.CompletableFuture.supplyAsync;
import static org.hamcrest.Matchers.sameInstance;
import static org.hamcrest.collection.IsMapContaining.hasEntry;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.rules.ExpectedException.none;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static org.mule.runtime.core.api.construct.Flow.builder;
import static org.mule.runtime.dsl.api.component.config.ComponentIdentifier.ANNOTATION_PARAMETERS;

import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.component.ComponentLocation;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.interception.InterceptionAction;
import org.mule.runtime.api.interception.InterceptionEvent;
import org.mule.runtime.api.interception.InterceptionHandler;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.core.AbstractAnnotatedObject;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.construct.Flow;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.InOrder;

/**
 * Failures due to:
 * 
 * * around, mutate event after proceed, some callback/future stuff missing in api to support that
 *
 */
public class ReactiveInterceptorAdapterTestCase extends AbstractMuleContextTestCase {

  private Flow flow;
  private Processor processor;

  @Rule
  public ExpectedException expected = none();

  @Before
  public void before() throws MuleException {
    processor = spy(new ProcessorInApp());
    flow = builder("flow", muleContext).messageProcessors(singletonList(processor)).build();
  }

  @After
  public void after() throws MuleException {
    flow.stop();
    flow.dispose();
  }

  @Test
  public void interceptorApplied() throws Exception {
    InterceptionHandler interceptor = spy(new InterceptionHandler() {});
    startFlowWithInterceptors(interceptor);

    Event result = process(flow, eventBuilder().message(Message.builder().payload("").build()).build());
    assertThat(result.getMessage().getPayload().getValue(), is(""));

    InOrder inOrder = inOrder(processor, interceptor);

    inOrder.verify(interceptor).before(mapArgWithEntry("param", ""), any());
    inOrder.verify(interceptor).around(mapArgWithEntry("param", ""), any(), any());
    inOrder.verify(processor).process(argThat(hasPayloadValue("")));
    inOrder.verify(interceptor).after(any());
  }

  @Test
  public void interceptorMutatesEventBefore() throws Exception {
    InterceptionHandler interceptor = spy(new InterceptionHandler() {

      @Override
      public void before(Map<String, Object> parameters, InterceptionEvent event) {
        event.message(Message.builder().payload(TEST_PAYLOAD).build());
      }
    });
    startFlowWithInterceptors(interceptor);

    Event result = process(flow, eventBuilder().message(Message.builder().payload("").build()).build());
    assertThat(result.getMessage().getPayload().getValue(), is(TEST_PAYLOAD));

    InOrder inOrder = inOrder(processor, interceptor);

    inOrder.verify(interceptor).before(mapArgWithEntry("param", ""), any());
    inOrder.verify(interceptor).around(mapArgWithEntry("param", TEST_PAYLOAD), argThat(interceptionHasPayloadValue(TEST_PAYLOAD)),
                                       any());
    inOrder.verify(processor).process(argThat(hasPayloadValue(TEST_PAYLOAD)));
    inOrder.verify(interceptor).after(argThat(interceptionHasPayloadValue(TEST_PAYLOAD)));
  }

  @Test
  public void interceptorMutatesEventAfter() throws Exception {
    InterceptionHandler interceptor = spy(new InterceptionHandler() {

      @Override
      public void after(InterceptionEvent event) {
        event.message(Message.builder().payload(TEST_PAYLOAD).build());
      }
    });
    startFlowWithInterceptors(interceptor);

    Event result = process(flow, eventBuilder().message(Message.builder().payload("").build()).build());
    assertThat(result.getMessage().getPayload().getValue(), is(TEST_PAYLOAD));

    InOrder inOrder = inOrder(processor, interceptor);

    inOrder.verify(interceptor).before(mapArgWithEntry("param", ""), any());
    inOrder.verify(interceptor).around(mapArgWithEntry("param", ""), any(), any());
    inOrder.verify(processor).process(argThat(hasPayloadValue("")));
    inOrder.verify(interceptor).after(any());
  }

  @Test
  public void interceptorMutatesEventAroundBeforeProceed() throws Exception {
    InterceptionHandler interceptor = spy(new InterceptionHandler() {

      @Override
      public CompletableFuture<InterceptionEvent> around(Map<String, Object> parameters, InterceptionEvent event,
                                                         InterceptionAction action) {
        event.message(Message.builder().payload(TEST_PAYLOAD).build());
        return action.proceed();
      }
    });
    startFlowWithInterceptors(interceptor);

    Event result = process(flow, eventBuilder().message(Message.builder().payload("").build()).build());
    assertThat(result.getMessage().getPayload().getValue(), is(TEST_PAYLOAD));

    InOrder inOrder = inOrder(processor, interceptor);

    inOrder.verify(interceptor).before(mapArgWithEntry("param", ""), any());
    inOrder.verify(interceptor).around(mapArgWithEntry("param", ""), any(), any());
    inOrder.verify(processor).process(argThat(hasPayloadValue(TEST_PAYLOAD)));
    inOrder.verify(interceptor).after(argThat(interceptionHasPayloadValue(TEST_PAYLOAD)));
  }

  @Test
  public void interceptorMutatesEventAroundAfterProceed() throws Exception {
    InterceptionHandler interceptor = spy(new InterceptionHandler() {

      @Override
      public CompletableFuture<InterceptionEvent> around(Map<String, Object> parameters, InterceptionEvent event,
                                                         InterceptionAction action) {
        action.proceed();
        return supplyAsync(() -> {
          event.message(Message.builder().payload(TEST_PAYLOAD).build());
          return event;
        });
      }
    });
    startFlowWithInterceptors(interceptor);

    Event result = process(flow, eventBuilder().message(Message.builder().payload("").build()).build());
    assertThat(result.getMessage().getPayload().getValue(), is(TEST_PAYLOAD));

    InOrder inOrder = inOrder(processor, interceptor);

    inOrder.verify(interceptor).before(mapArgWithEntry("param", ""), any());
    inOrder.verify(interceptor).around(mapArgWithEntry("param", ""), any(), any());
    inOrder.verify(processor).process(argThat(hasPayloadValue("")));
    inOrder.verify(interceptor).after(argThat(interceptionHasPayloadValue(TEST_PAYLOAD)));
  }

  @Test
  public void interceptorThrowsExceptionBefore() throws Exception {
    RuntimeException expectedException = new RuntimeException("Some Error");
    InterceptionHandler interceptor = spy(new InterceptionHandler() {

      @Override
      public void before(Map<String, Object> parameters, InterceptionEvent event) {
        throw expectedException;
      }
    });
    startFlowWithInterceptors(interceptor);

    expected.expectCause(sameInstance(expectedException));
    try {
      process(flow, eventBuilder().message(Message.builder().payload("").build()).build());
    } finally {
      InOrder inOrder = inOrder(processor, interceptor);

      inOrder.verify(interceptor).before(any(), any());
      inOrder.verify(interceptor, never()).around(any(), any(), any());
      inOrder.verify(processor, never()).process(any());
      inOrder.verify(interceptor).after(any());
    }
  }

  @Test
  public void interceptorThrowsExceptionAfter() throws Exception {
    RuntimeException expectedException = new RuntimeException("Some Error");
    InterceptionHandler interceptor = spy(new InterceptionHandler() {

      @Override
      public void after(InterceptionEvent event) {
        throw expectedException;
      }
    });
    startFlowWithInterceptors(interceptor);

    expected.expectCause(sameInstance(expectedException));
    try {
      process(flow, eventBuilder().message(Message.builder().payload("").build()).build());
    } finally {
      InOrder inOrder = inOrder(processor, interceptor);

      inOrder.verify(interceptor).before(any(), any());
      inOrder.verify(interceptor).around(any(), any(), any());
      inOrder.verify(processor).process(argThat(hasPayloadValue("")));
      inOrder.verify(interceptor).after(any());
    }
  }

  @Test
  public void interceptorThrowsExceptionAround() throws Exception {
    RuntimeException expectedException = new RuntimeException("Some Error");
    InterceptionHandler interceptor = spy(new InterceptionHandler() {

      @Override
      public CompletableFuture<InterceptionEvent> around(Map<String, Object> parameters, InterceptionEvent event,
                                                         InterceptionAction action) {
        throw expectedException;
      }
    });
    startFlowWithInterceptors(interceptor);

    expected.expectCause(sameInstance(expectedException));
    try {
      process(flow, eventBuilder().message(Message.builder().payload("").build()).build());
    } finally {
      InOrder inOrder = inOrder(processor, interceptor);

      inOrder.verify(interceptor).before(any(), any());
      inOrder.verify(interceptor).around(any(), any(), any());
      inOrder.verify(processor, never()).process(any());
      inOrder.verify(interceptor).after(any());
    }
  }

  @Test
  public void interceptorThrowsExceptionAroundAfterProceed() throws Exception {
    RuntimeException expectedException = new RuntimeException("Some Error");
    InterceptionHandler interceptor = spy(new InterceptionHandler() {

      @Override
      public CompletableFuture<InterceptionEvent> around(Map<String, Object> parameters, InterceptionEvent event,
                                                         InterceptionAction action) {
        action.proceed();
        throw expectedException;
      }
    });
    startFlowWithInterceptors(interceptor);

    expected.expectCause(sameInstance(expectedException));
    try {
      process(flow, eventBuilder().message(Message.builder().payload("").build()).build());
    } finally {
      InOrder inOrder = inOrder(processor, interceptor);

      inOrder.verify(interceptor).before(any(), any());
      inOrder.verify(interceptor).around(any(), any(), any());
      inOrder.verify(processor).process(any());
      inOrder.verify(interceptor).after(any());
    }
  }

  @Test
  public void interceptedThrowsException() throws Exception {
    RuntimeException expectedException = new RuntimeException("Some Error");
    InterceptionHandler interceptor = spy(new InterceptionHandler() {});
    startFlowWithInterceptors(interceptor);

    when(processor.process(any())).thenThrow(expectedException);

    expected.expectCause(sameInstance(expectedException));
    try {
      process(flow, eventBuilder().message(Message.builder().payload("").build()).build());
    } finally {
      InOrder inOrder = inOrder(processor, interceptor);

      inOrder.verify(interceptor).before(any(), any());
      inOrder.verify(interceptor).around(any(), any(), any());
      inOrder.verify(processor).process(any());
      inOrder.verify(interceptor).after(any());
    }
  }

  @Test
  public void interceptorSkipsProcessor() throws Exception {
    InterceptionHandler interceptor = spy(new InterceptionHandler() {

      @Override
      public CompletableFuture<InterceptionEvent> around(Map<String, Object> parameters, InterceptionEvent event,
                                                         InterceptionAction action) {
        return action.skip();
      }
    });
    startFlowWithInterceptors(interceptor);

    Event result = process(flow, eventBuilder().message(Message.builder().payload("").build()).build());
    assertThat(result.getMessage().getPayload().getValue(), is(""));

    InOrder inOrder = inOrder(processor, interceptor);

    inOrder.verify(interceptor).before(any(), any());
    inOrder.verify(interceptor).around(any(), any(), any());
    inOrder.verify(processor, never()).process(any());
    inOrder.verify(interceptor).after(any());
  }

  @Test
  public void firstInterceptorMutatesEventBefore() throws Exception {
    InterceptionHandler interceptor1 = spy(new InterceptionHandler() {

      @Override
      public void before(Map<String, Object> parameters, InterceptionEvent event) {
        event.message(Message.builder().payload(TEST_PAYLOAD).build());
      }
    });
    InterceptionHandler interceptor2 = spy(new InterceptionHandler() {});
    startFlowWithInterceptors(interceptor1, interceptor2);

    Event result = process(flow, eventBuilder().message(Message.builder().payload("").build()).build());
    assertThat(result.getMessage().getPayload().getValue(), is(TEST_PAYLOAD));

    InOrder inOrder = inOrder(processor, interceptor1, interceptor2);

    inOrder.verify(interceptor1).before(mapArgWithEntry("param", ""), any());
    inOrder.verify(interceptor1).around(mapArgWithEntry("param", TEST_PAYLOAD),
                                        argThat(interceptionHasPayloadValue(TEST_PAYLOAD)), any());
    inOrder.verify(interceptor2).before(mapArgWithEntry("param", TEST_PAYLOAD),
                                        argThat(interceptionHasPayloadValue(TEST_PAYLOAD)));
    inOrder.verify(interceptor2).around(mapArgWithEntry("param", TEST_PAYLOAD),
                                        argThat(interceptionHasPayloadValue(TEST_PAYLOAD)), any());
    inOrder.verify(processor).process(argThat(hasPayloadValue(TEST_PAYLOAD)));
    inOrder.verify(interceptor2).after(argThat(interceptionHasPayloadValue(TEST_PAYLOAD)));
    inOrder.verify(interceptor1).after(argThat(interceptionHasPayloadValue(TEST_PAYLOAD)));
  }

  @Test
  public void secondInterceptorMutatesEventBefore() throws Exception {
    InterceptionHandler interceptor1 = spy(new InterceptionHandler() {});
    InterceptionHandler interceptor2 = spy(new InterceptionHandler() {

      @Override
      public void before(Map<String, Object> parameters, InterceptionEvent event) {
        event.message(Message.builder().payload(TEST_PAYLOAD).build());
      }
    });
    startFlowWithInterceptors(interceptor1, interceptor2);

    Event result = process(flow, eventBuilder().message(Message.builder().payload("").build()).build());
    assertThat(result.getMessage().getPayload().getValue(), is(TEST_PAYLOAD));

    InOrder inOrder = inOrder(processor, interceptor1, interceptor2);

    inOrder.verify(interceptor1).before(mapArgWithEntry("param", ""), argThat(interceptionHasPayloadValue("")));
    inOrder.verify(interceptor1).around(mapArgWithEntry("param", ""), argThat(interceptionHasPayloadValue("")), any());
    inOrder.verify(interceptor2).before(mapArgWithEntry("param", ""), any());
    inOrder.verify(interceptor2).around(mapArgWithEntry("param", TEST_PAYLOAD),
                                        argThat(interceptionHasPayloadValue(TEST_PAYLOAD)), any());
    inOrder.verify(processor).process(argThat(hasPayloadValue(TEST_PAYLOAD)));
    inOrder.verify(interceptor2).after(argThat(interceptionHasPayloadValue(TEST_PAYLOAD)));
    inOrder.verify(interceptor1).after(argThat(interceptionHasPayloadValue(TEST_PAYLOAD)));
  }

  @Test
  public void firstInterceptorMutatesEventAfter() throws Exception {
    InterceptionHandler interceptor1 = spy(new InterceptionHandler() {

      @Override
      public void after(InterceptionEvent event) {
        event.message(Message.builder().payload(TEST_PAYLOAD).build());
      }
    });
    InterceptionHandler interceptor2 = spy(new InterceptionHandler() {});
    startFlowWithInterceptors(interceptor1, interceptor2);

    Event result = process(flow, eventBuilder().message(Message.builder().payload("").build()).build());
    assertThat(result.getMessage().getPayload().getValue(), is(TEST_PAYLOAD));

    InOrder inOrder = inOrder(processor, interceptor1, interceptor2);

    inOrder.verify(interceptor1).before(any(), argThat(interceptionHasPayloadValue("")));
    inOrder.verify(interceptor1).around(any(), argThat(interceptionHasPayloadValue("")), any());
    inOrder.verify(interceptor2).before(any(), argThat(interceptionHasPayloadValue("")));
    inOrder.verify(interceptor2).around(any(), argThat(interceptionHasPayloadValue("")), any());
    inOrder.verify(processor).process(argThat(hasPayloadValue("")));
    inOrder.verify(interceptor2).after(argThat(interceptionHasPayloadValue("")));
    inOrder.verify(interceptor1).after(any());
  }

  @Test
  public void secondInterceptorMutatesEventAfter() throws Exception {
    InterceptionHandler interceptor1 = spy(new InterceptionHandler() {});
    InterceptionHandler interceptor2 = spy(new InterceptionHandler() {

      @Override
      public void after(InterceptionEvent event) {
        event.message(Message.builder().payload(TEST_PAYLOAD).build());
      }
    });
    startFlowWithInterceptors(interceptor1, interceptor2);

    Event result = process(flow, eventBuilder().message(Message.builder().payload("").build()).build());
    assertThat(result.getMessage().getPayload().getValue(), is(TEST_PAYLOAD));

    InOrder inOrder = inOrder(processor, interceptor1, interceptor2);

    inOrder.verify(interceptor1).before(any(), argThat(interceptionHasPayloadValue("")));
    inOrder.verify(interceptor1).around(any(), argThat(interceptionHasPayloadValue("")), any());
    inOrder.verify(interceptor2).before(any(), argThat(interceptionHasPayloadValue("")));
    inOrder.verify(interceptor2).around(any(), argThat(interceptionHasPayloadValue("")), any());
    inOrder.verify(processor).process(argThat(hasPayloadValue("")));
    inOrder.verify(interceptor2).after(any());
    inOrder.verify(interceptor1).after(argThat(interceptionHasPayloadValue(TEST_PAYLOAD)));
  }

  @Test
  public void firstInterceptorMutatesEventAroundBeforeProceed() throws Exception {
    InterceptionHandler interceptor1 = spy(new InterceptionHandler() {

      @Override
      public CompletableFuture<InterceptionEvent> around(Map<String, Object> parameters, InterceptionEvent event,
                                                         InterceptionAction action) {
        event.message(Message.builder().payload(TEST_PAYLOAD).build());
        return action.proceed();
      }
    });
    InterceptionHandler interceptor2 = spy(new InterceptionHandler() {});
    startFlowWithInterceptors(interceptor1, interceptor2);

    Event result = process(flow, eventBuilder().message(Message.builder().payload("").build()).build());
    assertThat(result.getMessage().getPayload().getValue(), is(TEST_PAYLOAD));

    InOrder inOrder = inOrder(processor, interceptor1, interceptor2);

    inOrder.verify(interceptor1).before(mapArgWithEntry("param", ""), argThat(interceptionHasPayloadValue("")));
    inOrder.verify(interceptor1).around(mapArgWithEntry("param", ""), any(), any());
    inOrder.verify(interceptor2).before(mapArgWithEntry("param", TEST_PAYLOAD),
                                        argThat(interceptionHasPayloadValue(TEST_PAYLOAD)));
    inOrder.verify(interceptor2).around(mapArgWithEntry("param", TEST_PAYLOAD),
                                        argThat(interceptionHasPayloadValue(TEST_PAYLOAD)), any());
    inOrder.verify(processor).process(argThat(hasPayloadValue(TEST_PAYLOAD)));
    inOrder.verify(interceptor2).after(argThat(interceptionHasPayloadValue(TEST_PAYLOAD)));
    inOrder.verify(interceptor1).after(argThat(interceptionHasPayloadValue(TEST_PAYLOAD)));
  }

  @Test
  public void secondInterceptorMutatesEventAroundBeforeProceed() throws Exception {
    InterceptionHandler interceptor1 = spy(new InterceptionHandler() {});
    InterceptionHandler interceptor2 = spy(new InterceptionHandler() {

      @Override
      public CompletableFuture<InterceptionEvent> around(Map<String, Object> parameters, InterceptionEvent event,
                                                         InterceptionAction action) {
        event.message(Message.builder().payload(TEST_PAYLOAD).build());
        return action.proceed();
      }
    });
    startFlowWithInterceptors(interceptor1, interceptor2);

    Event result = process(flow, eventBuilder().message(Message.builder().payload("").build()).build());
    assertThat(result.getMessage().getPayload().getValue(), is(TEST_PAYLOAD));

    InOrder inOrder = inOrder(processor, interceptor1, interceptor2);

    inOrder.verify(interceptor1).before(mapArgWithEntry("param", ""), argThat(interceptionHasPayloadValue("")));
    inOrder.verify(interceptor1).around(mapArgWithEntry("param", ""), argThat(interceptionHasPayloadValue("")), any());
    inOrder.verify(interceptor2).before(mapArgWithEntry("param", ""), argThat(interceptionHasPayloadValue("")));
    inOrder.verify(interceptor2).around(mapArgWithEntry("param", ""), any(), any());
    inOrder.verify(processor).process(argThat(hasPayloadValue(TEST_PAYLOAD)));
    inOrder.verify(interceptor2).after(argThat(interceptionHasPayloadValue(TEST_PAYLOAD)));
    inOrder.verify(interceptor1).after(argThat(interceptionHasPayloadValue(TEST_PAYLOAD)));
  }

  @Test
  public void firstInterceptorMutatesEventAroundAfterProceed() throws Exception {
    InterceptionHandler interceptor1 = spy(new InterceptionHandler() {

      @Override
      public CompletableFuture<InterceptionEvent> around(Map<String, Object> parameters, InterceptionEvent event,
                                                         InterceptionAction action) {
        action.proceed();
        return supplyAsync(() -> {
          event.message(Message.builder().payload(TEST_PAYLOAD).build());
          return event;
        });
      }
    });
    InterceptionHandler interceptor2 = spy(new InterceptionHandler() {});
    startFlowWithInterceptors(interceptor1, interceptor2);

    Event result = process(flow, eventBuilder().message(Message.builder().payload("").build()).build());
    assertThat(result.getMessage().getPayload().getValue(), is(TEST_PAYLOAD));

    InOrder inOrder = inOrder(processor, interceptor1, interceptor2);

    inOrder.verify(interceptor1).before(mapArgWithEntry("param", ""), argThat(interceptionHasPayloadValue("")));
    inOrder.verify(interceptor1).around(mapArgWithEntry("param", ""), any(), any());
    inOrder.verify(interceptor2).before(mapArgWithEntry("param", ""), argThat(interceptionHasPayloadValue("")));
    inOrder.verify(interceptor2).around(mapArgWithEntry("param", ""), argThat(interceptionHasPayloadValue("")), any());
    inOrder.verify(processor).process(argThat(hasPayloadValue("")));
    inOrder.verify(interceptor2).after(argThat(interceptionHasPayloadValue("")));
    inOrder.verify(interceptor1).after(argThat(interceptionHasPayloadValue(TEST_PAYLOAD)));
  }

  @Test
  public void secondInterceptorMutatesEventAroundAfterProceed() throws Exception {
    InterceptionHandler interceptor1 = spy(new InterceptionHandler() {});
    InterceptionHandler interceptor2 = spy(new InterceptionHandler() {

      @Override
      public CompletableFuture<InterceptionEvent> around(Map<String, Object> parameters, InterceptionEvent event,
                                                         InterceptionAction action) {
        action.proceed();
        return supplyAsync(() -> {
          event.message(Message.builder().payload(TEST_PAYLOAD).build());
          return event;
        });
      }
    });
    startFlowWithInterceptors(interceptor1, interceptor2);

    Event result = process(flow, eventBuilder().message(Message.builder().payload("").build()).build());
    assertThat(result.getMessage().getPayload().getValue(), is(TEST_PAYLOAD));

    InOrder inOrder = inOrder(processor, interceptor1, interceptor2);

    inOrder.verify(interceptor1).before(mapArgWithEntry("param", ""), argThat(interceptionHasPayloadValue("")));
    inOrder.verify(interceptor1).around(mapArgWithEntry("param", ""), argThat(interceptionHasPayloadValue("")), any());
    inOrder.verify(interceptor2).before(mapArgWithEntry("param", ""), argThat(interceptionHasPayloadValue("")));
    inOrder.verify(interceptor2).around(mapArgWithEntry("param", ""), any(), any());
    inOrder.verify(processor).process(argThat(hasPayloadValue("")));
    inOrder.verify(interceptor2).after(argThat(interceptionHasPayloadValue(TEST_PAYLOAD)));
    inOrder.verify(interceptor1).after(argThat(interceptionHasPayloadValue(TEST_PAYLOAD)));
  }

  @Test
  public void firstInterceptorThrowsExceptionBefore() throws Exception {
    RuntimeException expectedException = new RuntimeException("Some Error");
    InterceptionHandler interceptor1 = spy(new InterceptionHandler() {

      @Override
      public void before(Map<String, Object> parameters, InterceptionEvent event) {
        throw expectedException;
      }
    });
    InterceptionHandler interceptor2 = spy(new InterceptionHandler() {});
    startFlowWithInterceptors(interceptor1, interceptor2);

    expected.expectCause(sameInstance(expectedException));
    try {
      process(flow, eventBuilder().message(Message.builder().payload("").build()).build());
    } finally {
      InOrder inOrder = inOrder(processor, interceptor1, interceptor2);

      inOrder.verify(interceptor1).before(any(), any());
      inOrder.verify(interceptor1, never()).around(any(), any(), any());
      inOrder.verify(interceptor2, never()).before(any(), any());
      inOrder.verify(interceptor2, never()).around(any(), any(), any());
      inOrder.verify(processor, never()).process(any());
      inOrder.verify(interceptor2, never()).after(any());
      inOrder.verify(interceptor1).after(any());
    }
  }

  @Test
  public void secondInterceptorThrowsExceptionBefore() throws Exception {
    RuntimeException expectedException = new RuntimeException("Some Error");
    InterceptionHandler interceptor1 = spy(new InterceptionHandler() {});
    InterceptionHandler interceptor2 = spy(new InterceptionHandler() {

      @Override
      public void before(Map<String, Object> parameters, InterceptionEvent event) {
        throw expectedException;
      }
    });
    startFlowWithInterceptors(interceptor1, interceptor2);

    expected.expectCause(sameInstance(expectedException));
    try {
      process(flow, eventBuilder().message(Message.builder().payload("").build()).build());
    } finally {
      InOrder inOrder = inOrder(processor, interceptor1, interceptor2);

      inOrder.verify(interceptor1).before(any(), any());
      inOrder.verify(interceptor1).around(any(), any(), any());
      inOrder.verify(interceptor2).before(any(), any());
      inOrder.verify(interceptor2, never()).around(any(), any(), any());
      inOrder.verify(processor, never()).process(any());
      inOrder.verify(interceptor2).after(any());
      inOrder.verify(interceptor1).after(any());
    }
  }

  @Test
  public void firstInterceptorThrowsExceptionAfter() throws Exception {
    RuntimeException expectedException = new RuntimeException("Some Error");
    InterceptionHandler interceptor1 = spy(new InterceptionHandler() {

      @Override
      public void after(InterceptionEvent event) {
        throw expectedException;
      }
    });
    InterceptionHandler interceptor2 = spy(new InterceptionHandler() {});
    startFlowWithInterceptors(interceptor1, interceptor2);

    expected.expectCause(sameInstance(expectedException));
    try {
      process(flow, eventBuilder().message(Message.builder().payload("").build()).build());
    } finally {
      InOrder inOrder = inOrder(processor, interceptor1, interceptor2);

      inOrder.verify(interceptor1).before(any(), any());
      inOrder.verify(interceptor1).around(any(), any(), any());
      inOrder.verify(interceptor2).before(any(), any());
      inOrder.verify(interceptor2).around(any(), any(), any());
      inOrder.verify(processor).process(any());
      inOrder.verify(interceptor2).after(any());
      inOrder.verify(interceptor1).after(any());
    }
  }

  @Test
  public void secondInterceptorThrowsExceptionAfter() throws Exception {
    RuntimeException expectedException = new RuntimeException("Some Error");
    InterceptionHandler interceptor1 = spy(new InterceptionHandler() {});
    InterceptionHandler interceptor2 = spy(new InterceptionHandler() {

      @Override
      public void after(InterceptionEvent event) {
        throw expectedException;
      }
    });
    startFlowWithInterceptors(interceptor1, interceptor2);

    expected.expectCause(sameInstance(expectedException));
    try {
      process(flow, eventBuilder().message(Message.builder().payload("").build()).build());
    } finally {
      InOrder inOrder = inOrder(processor, interceptor1, interceptor2);

      inOrder.verify(interceptor1).before(any(), any());
      inOrder.verify(interceptor1).around(any(), any(), any());
      inOrder.verify(interceptor2).before(any(), any());
      inOrder.verify(interceptor2).around(any(), any(), any());
      inOrder.verify(processor).process(any());
      inOrder.verify(interceptor2).after(any());
      inOrder.verify(interceptor1).after(any());
    }
  }

  @Test
  public void firstInterceptorThrowsExceptionAround() throws Exception {
    RuntimeException expectedException = new RuntimeException("Some Error");
    InterceptionHandler interceptor1 = spy(new InterceptionHandler() {

      @Override
      public CompletableFuture<InterceptionEvent> around(Map<String, Object> parameters, InterceptionEvent event,
                                                         InterceptionAction action) {
        throw expectedException;
      }
    });
    InterceptionHandler interceptor2 = spy(new InterceptionHandler() {});
    startFlowWithInterceptors(interceptor1, interceptor2);

    expected.expectCause(sameInstance(expectedException));
    try {
      process(flow, eventBuilder().message(Message.builder().payload("").build()).build());
    } finally {
      InOrder inOrder = inOrder(processor, interceptor1, interceptor2);

      inOrder.verify(interceptor1).before(any(), any());
      inOrder.verify(interceptor1).around(any(), any(), any());
      inOrder.verify(interceptor2, never()).before(any(), any());
      inOrder.verify(interceptor2, never()).around(any(), any(), any());
      inOrder.verify(processor, never()).process(any());
      inOrder.verify(interceptor2, never()).after(any());
      inOrder.verify(interceptor1).after(any());
    }
  }

  @Test
  public void secondInterceptorThrowsExceptionAround() throws Exception {
    RuntimeException expectedException = new RuntimeException("Some Error");
    InterceptionHandler interceptor1 = spy(new InterceptionHandler() {});
    InterceptionHandler interceptor2 = spy(new InterceptionHandler() {

      @Override
      public CompletableFuture<InterceptionEvent> around(Map<String, Object> parameters, InterceptionEvent event,
                                                         InterceptionAction action) {
        throw expectedException;
      }
    });
    startFlowWithInterceptors(interceptor1, interceptor2);

    expected.expectCause(sameInstance(expectedException));
    try {
      process(flow, eventBuilder().message(Message.builder().payload("").build()).build());
    } finally {
      InOrder inOrder = inOrder(processor, interceptor1, interceptor2);

      inOrder.verify(interceptor1).before(any(), any());
      inOrder.verify(interceptor1).around(any(), any(), any());
      inOrder.verify(interceptor2).before(any(), any());
      inOrder.verify(interceptor2).around(any(), any(), any());
      inOrder.verify(processor, never()).process(any());
      inOrder.verify(interceptor2).after(any());
      inOrder.verify(interceptor1).after(any());
    }
  }

  @Test
  public void firstInterceptorThrowsExceptionAroundAfterProceed() throws Exception {
    RuntimeException expectedException = new RuntimeException("Some Error");
    InterceptionHandler interceptor1 = spy(new InterceptionHandler() {

      @Override
      public CompletableFuture<InterceptionEvent> around(Map<String, Object> parameters, InterceptionEvent event,
                                                         InterceptionAction action) {
        action.proceed();
        throw expectedException;
      }
    });
    InterceptionHandler interceptor2 = spy(new InterceptionHandler() {});
    startFlowWithInterceptors(interceptor1, interceptor2);

    expected.expectCause(sameInstance(expectedException));
    try {
      process(flow, eventBuilder().message(Message.builder().payload("").build()).build());
    } finally {
      InOrder inOrder = inOrder(processor, interceptor1, interceptor2);

      inOrder.verify(interceptor1).before(any(), any());
      inOrder.verify(interceptor1).around(any(), any(), any());
      inOrder.verify(interceptor2).before(any(), any());
      inOrder.verify(interceptor2).around(any(), any(), any());
      inOrder.verify(processor).process(any());
      inOrder.verify(interceptor2).after(any());
      inOrder.verify(interceptor1).after(any());
    }
  }

  @Test
  public void secondInterceptorThrowsExceptionAroundAfterProceed() throws Exception {
    RuntimeException expectedException = new RuntimeException("Some Error");
    InterceptionHandler interceptor1 = spy(new InterceptionHandler() {});
    InterceptionHandler interceptor2 = spy(new InterceptionHandler() {

      @Override
      public CompletableFuture<InterceptionEvent> around(Map<String, Object> parameters, InterceptionEvent event,
                                                         InterceptionAction action) {
        action.proceed();
        throw expectedException;
      }
    });
    startFlowWithInterceptors(interceptor1, interceptor2);

    expected.expectCause(sameInstance(expectedException));
    try {
      process(flow, eventBuilder().message(Message.builder().payload("").build()).build());
    } finally {
      InOrder inOrder = inOrder(processor, interceptor1, interceptor2);

      inOrder.verify(interceptor1).before(any(), any());
      inOrder.verify(interceptor1).around(any(), any(), any());
      inOrder.verify(interceptor2).before(any(), any());
      inOrder.verify(interceptor2).around(any(), any(), any());
      inOrder.verify(processor).process(any());
      inOrder.verify(interceptor2).after(any());
      inOrder.verify(interceptor1).after(any());
    }
  }

  @Test
  public void firstInterceptorSkipsProcessor() throws Exception {
    InterceptionHandler interceptor1 = spy(new InterceptionHandler() {

      @Override
      public CompletableFuture<InterceptionEvent> around(Map<String, Object> parameters, InterceptionEvent event,
                                                         InterceptionAction action) {
        return action.skip();
      }
    });
    InterceptionHandler interceptor2 = spy(new InterceptionHandler() {});
    startFlowWithInterceptors(interceptor1, interceptor2);

    Event result = process(flow, eventBuilder().message(Message.builder().payload("").build()).build());
    assertThat(result.getMessage().getPayload().getValue(), is(""));

    InOrder inOrder = inOrder(processor, interceptor1, interceptor2);

    inOrder.verify(interceptor1).before(any(), any());
    inOrder.verify(interceptor1).around(any(), any(), any());
    inOrder.verify(interceptor2, never()).before(any(), any());
    inOrder.verify(interceptor2, never()).around(any(), any(), any());
    inOrder.verify(processor, never()).process(any());
    inOrder.verify(interceptor2, never()).after(any());
    inOrder.verify(interceptor1).after(argThat(interceptionHasPayloadValue("")));
  }

  @Test
  public void secondInterceptorSkipsProcessor() throws Exception {
    InterceptionHandler interceptor1 = spy(new InterceptionHandler() {});
    InterceptionHandler interceptor2 = spy(new InterceptionHandler() {

      @Override
      public CompletableFuture<InterceptionEvent> around(Map<String, Object> parameters, InterceptionEvent event,
                                                         InterceptionAction action) {
        return action.skip();
      }
    });
    startFlowWithInterceptors(interceptor1, interceptor2);

    Event result = process(flow, eventBuilder().message(Message.builder().payload("").build()).build());
    assertThat(result.getMessage().getPayload().getValue(), is(""));

    InOrder inOrder = inOrder(processor, interceptor1, interceptor2);

    inOrder.verify(interceptor1).before(any(), any());
    inOrder.verify(interceptor1).around(any(), any(), any());
    inOrder.verify(interceptor2).before(any(), any());
    inOrder.verify(interceptor2).around(any(), any(), any());
    inOrder.verify(processor, never()).process(any());
    inOrder.verify(interceptor2).after(any());
    inOrder.verify(interceptor1).after(any());
  }

  @Test
  public void firstInterceptorDoesntApply() throws Exception {
    InterceptionHandler interceptor1 = spy(new InterceptionHandler() {

      @Override
      public boolean intercept(ComponentIdentifier identifier, ComponentLocation location) {
        return false;
      }
    });
    InterceptionHandler interceptor2 = spy(new InterceptionHandler() {});
    startFlowWithInterceptors(interceptor1, interceptor2);

    Event result = process(flow, eventBuilder().message(Message.builder().payload("").build()).build());
    assertThat(result.getMessage().getPayload().getValue(), is(""));

    InOrder inOrder = inOrder(processor, interceptor1, interceptor2);

    inOrder.verify(interceptor1, never()).before(any(), any());
    inOrder.verify(interceptor1, never()).around(any(), any(), any());
    inOrder.verify(interceptor2).before(any(), any());
    inOrder.verify(interceptor2).around(any(), any(), any());
    inOrder.verify(processor).process(any());
    inOrder.verify(interceptor2).after(any());
    inOrder.verify(interceptor1, never()).after(any());
  }

  @Test
  public void secondInterceptorDoesntApply() throws Exception {
    InterceptionHandler interceptor1 = spy(new InterceptionHandler() {});
    InterceptionHandler interceptor2 = spy(new InterceptionHandler() {

      @Override
      public boolean intercept(ComponentIdentifier identifier, ComponentLocation location) {
        return false;
      }
    });
    startFlowWithInterceptors(interceptor1, interceptor2);

    Event result = process(flow, eventBuilder().message(Message.builder().payload("").build()).build());
    assertThat(result.getMessage().getPayload().getValue(), is(""));

    InOrder inOrder = inOrder(processor, interceptor1, interceptor2);

    inOrder.verify(interceptor1).before(any(), any());
    inOrder.verify(interceptor1).around(any(), any(), any());
    inOrder.verify(interceptor2, never()).before(any(), any());
    inOrder.verify(interceptor2, never()).around(any(), any(), any());
    inOrder.verify(processor).process(any());
    inOrder.verify(interceptor2, never()).after(any());
    inOrder.verify(interceptor1).after(any());
  }

  private void startFlowWithInterceptors(InterceptionHandler... interceptors) throws Exception {
    for (InterceptionHandler interceptionHandler : interceptors) {
      muleContext.getMessageProcessorInterceptorManager().addInterceptionHandler(interceptionHandler);
    }

    flow.initialise();
    flow.start();
  }

  private static class ProcessorInApp extends AbstractAnnotatedObject implements Processor {

    public ProcessorInApp() {
      setAnnotations(singletonMap(ANNOTATION_PARAMETERS, singletonMap("param", "#[payload]")));
    }

    @Override
    public Event process(Event event) throws MuleException {
      return event;
    }
  }

  public static Map<String, Object> mapArgWithEntry(String key, Object value) {
    return (Map<String, Object>) argThat(hasEntry(key, value));
  }

  private static final class EventPayloadMatcher extends TypeSafeMatcher<Event> {

    private Matcher<Object> payloadMatcher;

    public EventPayloadMatcher(Matcher<Object> payloadMatcher) {
      this.payloadMatcher = payloadMatcher;
    }

    @Override
    public void describeTo(Description description) {
      payloadMatcher.describeTo(description);
    }

    @Override
    protected boolean matchesSafely(Event item) {
      return payloadMatcher.matches(item.getMessage().getPayload().getValue());
    }

  }

  private static EventPayloadMatcher hasPayloadValue(Object expectedPayload) {
    return new EventPayloadMatcher(is(expectedPayload));
  }

  private static final class InterceptionPayloadMatcher extends TypeSafeMatcher<InterceptionEvent> {

    private Matcher<Object> payloadMatcher;

    public InterceptionPayloadMatcher(Matcher<Object> payloadMatcher) {
      this.payloadMatcher = payloadMatcher;
    }

    @Override
    public void describeTo(Description description) {
      payloadMatcher.describeTo(description);
    }

    @Override
    protected boolean matchesSafely(InterceptionEvent item) {
      return payloadMatcher.matches(item.getMessage().getPayload().getValue());
    }

  }

  private static InterceptionPayloadMatcher interceptionHasPayloadValue(Object expectedPayload) {
    return new InterceptionPayloadMatcher(is(expectedPayload));
  }
}
