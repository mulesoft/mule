/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.processor.interceptor;

import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.concurrent.CompletableFuture.supplyAsync;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.collection.IsMapContaining.hasEntry;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsSame.sameInstance;
import static org.junit.Assert.assertThat;
import static org.junit.rules.ExpectedException.none;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static org.mule.runtime.api.component.ComponentIdentifier.buildFromStringRepresentation;
import static org.mule.runtime.api.component.TypedComponentIdentifier.builder;
import static org.mule.runtime.api.component.TypedComponentIdentifier.ComponentType.PROCESSOR;
import static org.mule.runtime.core.api.construct.Flow.builder;
import static org.mule.runtime.core.component.ComponentAnnotations.ANNOTATION_PARAMETERS;

import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.interception.InterceptionAction;
import org.mule.runtime.api.interception.InterceptionEvent;
import org.mule.runtime.api.interception.ProcessorInterceptor;
import org.mule.runtime.api.interception.ProcessorInterceptorFactory;
import org.mule.runtime.api.message.ErrorType;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.meta.AbstractAnnotatedObject;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.construct.Flow;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.dsl.api.component.config.DefaultComponentLocation;
import org.mule.runtime.dsl.api.component.config.DefaultComponentLocation.DefaultLocationPart;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import javax.xml.namespace.QName;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.InOrder;
import org.mockito.Mockito;
import com.google.common.collect.ImmutableMap;

/*
 * The test methods with 'noMock' suffix exercise the optimized interception path when 'around' is not implemented.
 * This is needed because Mockito mocks override the methods.
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
    ProcessorInterceptor interceptor = spy(new ProcessorInterceptor() {});
    startFlowWithInterceptors(interceptor);

    Event result = process(flow, eventBuilder().message(Message.of("")).build());
    assertThat(result.getMessage().getPayload().getValue(), is(""));
    assertThat(result.getError().isPresent(), is(false));

    InOrder inOrder = inOrder(processor, interceptor);

    inOrder.verify(interceptor).before(mapArgWithEntry("param", ""), any());
    inOrder.verify(interceptor).around(mapArgWithEntry("param", ""), any(), any());
    inOrder.verify(processor).process(argThat(hasPayloadValue("")));
    inOrder.verify(interceptor).after(any(), eq(empty()));
  }

  @Test
  public void interceptorAppliedNoMock() throws Exception {
    ProcessorInterceptor interceptor = new ProcessorInterceptor() {};
    startFlowWithInterceptors(interceptor);

    Event result = process(flow, eventBuilder().message(Message.of("")).build());
    assertThat(result.getMessage().getPayload().getValue(), is(""));
    assertThat(result.getError().isPresent(), is(false));
  }

  @Test
  public void interceptorMutatesEventBefore() throws Exception {
    ProcessorInterceptor interceptor = spy(new ProcessorInterceptor() {

      @Override
      public void before(Map<String, Object> parameters, InterceptionEvent event) {
        event.message(Message.of(TEST_PAYLOAD));
      }
    });
    startFlowWithInterceptors(interceptor);

    Event result = process(flow, eventBuilder().message(Message.of("")).build());
    assertThat(result.getMessage().getPayload().getValue(), is(TEST_PAYLOAD));
    assertThat(result.getError().isPresent(), is(false));

    InOrder inOrder = inOrder(processor, interceptor);

    inOrder.verify(interceptor).before(mapArgWithEntry("param", ""), any());
    inOrder.verify(interceptor).around(mapArgWithEntry("param", TEST_PAYLOAD), argThat(interceptionHasPayloadValue(TEST_PAYLOAD)),
                                       any());
    inOrder.verify(processor).process(argThat(hasPayloadValue(TEST_PAYLOAD)));
    inOrder.verify(interceptor).after(argThat(interceptionHasPayloadValue(TEST_PAYLOAD)), eq(empty()));
  }

  @Test
  public void interceptorMutatesEventBeforeNoMock() throws Exception {
    ProcessorInterceptor interceptor = new ProcessorInterceptor() {

      @Override
      public void before(Map<String, Object> parameters, InterceptionEvent event) {
        event.message(Message.of(TEST_PAYLOAD));
      }
    };
    startFlowWithInterceptors(interceptor);

    Event result = process(flow, eventBuilder().message(Message.of("")).build());
    assertThat(result.getMessage().getPayload().getValue(), is(TEST_PAYLOAD));
    assertThat(result.getError().isPresent(), is(false));
  }

  @Test
  public void interceptorMutatesEventAfter() throws Exception {
    ProcessorInterceptor interceptor = spy(new ProcessorInterceptor() {

      @Override
      public void after(InterceptionEvent event, Optional<Throwable> thrown) {
        event.message(Message.of(TEST_PAYLOAD));
      }
    });
    startFlowWithInterceptors(interceptor);

    Event result = process(flow, eventBuilder().message(Message.of("")).build());
    assertThat(result.getMessage().getPayload().getValue(), is(TEST_PAYLOAD));
    assertThat(result.getError().isPresent(), is(false));

    InOrder inOrder = inOrder(processor, interceptor);

    inOrder.verify(interceptor).before(mapArgWithEntry("param", ""), any());
    inOrder.verify(interceptor).around(mapArgWithEntry("param", ""), any(), any());
    inOrder.verify(processor).process(argThat(hasPayloadValue("")));
    inOrder.verify(interceptor).after(any(), eq(empty()));
  }

  @Test
  public void interceptorMutatesEventAfterNoMock() throws Exception {
    ProcessorInterceptor interceptor = new ProcessorInterceptor() {

      @Override
      public void after(InterceptionEvent event, Optional<Throwable> thrown) {
        event.message(Message.of(TEST_PAYLOAD));
      }
    };
    startFlowWithInterceptors(interceptor);

    Event result = process(flow, eventBuilder().message(Message.of("")).build());
    assertThat(result.getMessage().getPayload().getValue(), is(TEST_PAYLOAD));
    assertThat(result.getError().isPresent(), is(false));
  }

  @Test
  public void interceptorMutatesEventAroundBeforeProceed() throws Exception {
    ProcessorInterceptor interceptor = spy(new ProcessorInterceptor() {

      @Override
      public CompletableFuture<InterceptionEvent> around(Map<String, Object> parameters, InterceptionEvent event,
                                                         InterceptionAction action) {
        event.message(Message.of(TEST_PAYLOAD));
        return action.proceed();
      }
    });
    startFlowWithInterceptors(interceptor);

    Event result = process(flow, eventBuilder().message(Message.of("")).build());
    assertThat(result.getMessage().getPayload().getValue(), is(TEST_PAYLOAD));
    assertThat(result.getError().isPresent(), is(false));

    InOrder inOrder = inOrder(processor, interceptor);

    inOrder.verify(interceptor).before(mapArgWithEntry("param", ""), any());
    inOrder.verify(interceptor).around(mapArgWithEntry("param", ""), any(), any());
    inOrder.verify(processor).process(argThat(hasPayloadValue(TEST_PAYLOAD)));
    inOrder.verify(interceptor).after(argThat(interceptionHasPayloadValue(TEST_PAYLOAD)), eq(empty()));
  }

  @Test
  public void interceptorMutatesEventAroundAfterProceed() throws Exception {
    ProcessorInterceptor interceptor = spy(new ProcessorInterceptor() {

      @Override
      public CompletableFuture<InterceptionEvent> around(Map<String, Object> parameters, InterceptionEvent event,
                                                         InterceptionAction action) {
        action.proceed();
        return supplyAsync(() -> {
          event.message(Message.of(TEST_PAYLOAD));
          return event;
        });
      }
    });
    startFlowWithInterceptors(interceptor);

    Event result = process(flow, eventBuilder().message(Message.of("")).build());
    assertThat(result.getMessage().getPayload().getValue(), is(TEST_PAYLOAD));
    assertThat(result.getError().isPresent(), is(false));

    InOrder inOrder = inOrder(processor, interceptor);

    inOrder.verify(interceptor).before(mapArgWithEntry("param", ""), any());
    inOrder.verify(interceptor).around(mapArgWithEntry("param", ""), any(), any());
    inOrder.verify(processor).process(argThat(hasPayloadValue("")));
    inOrder.verify(interceptor).after(argThat(interceptionHasPayloadValue(TEST_PAYLOAD)), eq(empty()));
  }

  @Test
  public void interceptorMutatesEventAroundBeforeSkip() throws Exception {
    ProcessorInterceptor interceptor = spy(new ProcessorInterceptor() {

      @Override
      public CompletableFuture<InterceptionEvent> around(Map<String, Object> parameters, InterceptionEvent event,
                                                         InterceptionAction action) {
        event.message(Message.of(TEST_PAYLOAD));
        return action.skip();
      }
    });
    startFlowWithInterceptors(interceptor);

    Event result = process(flow, eventBuilder().message(Message.of("")).build());
    assertThat(result.getMessage().getPayload().getValue(), is(TEST_PAYLOAD));
    assertThat(result.getError().isPresent(), is(false));

    InOrder inOrder = inOrder(processor, interceptor);

    inOrder.verify(interceptor).before(mapArgWithEntry("param", ""), any());
    inOrder.verify(interceptor).around(mapArgWithEntry("param", ""), any(), any());
    inOrder.verify(processor, never()).process(any());
    inOrder.verify(interceptor).after(argThat(interceptionHasPayloadValue(TEST_PAYLOAD)), eq(empty()));
  }

  @Test
  public void interceptorMutatesEventAroundAfterSkip() throws Exception {
    ProcessorInterceptor interceptor = spy(new ProcessorInterceptor() {

      @Override
      public CompletableFuture<InterceptionEvent> around(Map<String, Object> parameters, InterceptionEvent event,
                                                         InterceptionAction action) {
        action.skip();
        return supplyAsync(() -> {
          event.message(Message.of(TEST_PAYLOAD));
          return event;
        });
      }
    });
    startFlowWithInterceptors(interceptor);

    Event result = process(flow, eventBuilder().message(Message.of("")).build());
    assertThat(result.getMessage().getPayload().getValue(), is(TEST_PAYLOAD));
    assertThat(result.getError().isPresent(), is(false));

    InOrder inOrder = inOrder(processor, interceptor);

    inOrder.verify(interceptor).before(mapArgWithEntry("param", ""), any());
    inOrder.verify(interceptor).around(mapArgWithEntry("param", ""), any(), any());
    inOrder.verify(processor, never()).process(any());
    inOrder.verify(interceptor).after(argThat(interceptionHasPayloadValue(TEST_PAYLOAD)), eq(empty()));
  }

  @Test
  public void interceptorMutatesEventAroundAfterFailWithErrorType() throws Exception {
    ErrorType errorTypeMock = Mockito.mock(ErrorType.class);
    ProcessorInterceptor interceptor = spy(new ProcessorInterceptor() {

      @Override
      public CompletableFuture<InterceptionEvent> around(Map<String, Object> parameters, InterceptionEvent event,
                                                         InterceptionAction action) {
        action.fail(errorTypeMock);
        return supplyAsync(() -> {
          event.message(Message.of(TEST_PAYLOAD));
          return event;
        });
      }
    });
    startFlowWithInterceptors(interceptor);

    Event result = process(flow, eventBuilder().message(Message.of("")).build());
    assertThat(result.getMessage().getPayload().getValue(), is(TEST_PAYLOAD));
    assertThat(result.getError().isPresent(), is(true));
    assertThat(result.getError().get().getCause(), is(instanceOf(InterceptionException.class)));
    assertThat(result.getError().get().getErrorType(), is(sameInstance(errorTypeMock)));

    InOrder inOrder = inOrder(processor, interceptor);

    inOrder.verify(interceptor).before(mapArgWithEntry("param", ""), any());
    inOrder.verify(interceptor).around(mapArgWithEntry("param", ""), any(), any());
    inOrder.verify(processor, never()).process(any());
    inOrder.verify(interceptor).after(argThat(interceptionHasPayloadValue(TEST_PAYLOAD)), eq(empty()));
  }

  @Test
  public void interceptorMutatesEventAroundAfterFailWithCause() throws Exception {
    Throwable cause = new RuntimeException("");
    ProcessorInterceptor interceptor = spy(new ProcessorInterceptor() {

      @Override
      public CompletableFuture<InterceptionEvent> around(Map<String, Object> parameters, InterceptionEvent event,
                                                         InterceptionAction action) {
        action.fail(cause);
        return supplyAsync(() -> {
          event.message(Message.of(TEST_PAYLOAD));
          return event;
        });
      }
    });
    startFlowWithInterceptors(interceptor);

    Event result = process(flow, eventBuilder().message(Message.of("")).build());
    assertThat(result.getMessage().getPayload().getValue(), is(TEST_PAYLOAD));
    assertThat(result.getError().isPresent(), is(true));
    assertThat(result.getError().get().getCause(), is(instanceOf(RuntimeException.class)));
    assertThat(result.getError().get().getErrorType().getNamespace(), is(equalTo("MULE")));
    assertThat(result.getError().get().getErrorType().getIdentifier(), is(equalTo("UNKNOWN")));

    InOrder inOrder = inOrder(processor, interceptor);

    inOrder.verify(interceptor).before(mapArgWithEntry("param", ""), any());
    inOrder.verify(interceptor).around(mapArgWithEntry("param", ""), any(), any());
    inOrder.verify(processor, never()).process(any());
    inOrder.verify(interceptor).after(argThat(interceptionHasPayloadValue(TEST_PAYLOAD)), eq(empty()));
  }

  @Test
  public void interceptorThrowsExceptionBefore() throws Exception {
    RuntimeException expectedException = new RuntimeException("Some Error");
    ProcessorInterceptor interceptor = spy(new ProcessorInterceptor() {

      @Override
      public void before(Map<String, Object> parameters, InterceptionEvent event) {
        throw expectedException;
      }
    });
    startFlowWithInterceptors(interceptor);

    expected.expectCause(sameInstance(expectedException));
    try {
      process(flow, eventBuilder().message(Message.of("")).build());
    } finally {
      InOrder inOrder = inOrder(processor, interceptor);

      inOrder.verify(interceptor).before(any(), any());
      inOrder.verify(interceptor, never()).around(any(), any(), any());
      inOrder.verify(processor, never()).process(any());
      inOrder.verify(interceptor).after(any(), eq(of(expectedException)));
    }
  }

  @Test
  public void interceptorThrowsExceptionBeforeNoMock() throws Exception {
    RuntimeException expectedException = new RuntimeException("Some Error");
    ProcessorInterceptor interceptor = spy(new ProcessorInterceptor() {

      @Override
      public void before(Map<String, Object> parameters, InterceptionEvent event) {
        throw expectedException;
      }
    });
    startFlowWithInterceptors(interceptor);

    expected.expectCause(sameInstance(expectedException));
    process(flow, eventBuilder().message(Message.of("")).build());
  }

  @Test
  public void interceptorThrowsExceptionAfter() throws Exception {
    RuntimeException expectedException = new RuntimeException("Some Error");
    ProcessorInterceptor interceptor = spy(new ProcessorInterceptor() {

      @Override
      public void after(InterceptionEvent event, Optional<Throwable> thrown) {
        throw expectedException;
      }
    });
    startFlowWithInterceptors(interceptor);

    expected.expectCause(sameInstance(expectedException));
    try {
      process(flow, eventBuilder().message(Message.of("")).build());
    } finally {
      InOrder inOrder = inOrder(processor, interceptor);

      inOrder.verify(interceptor).before(any(), any());
      inOrder.verify(interceptor).around(any(), any(), any());
      inOrder.verify(processor).process(argThat(hasPayloadValue("")));
      inOrder.verify(interceptor).after(any(), eq(empty()));
    }
  }

  @Test
  public void interceptorThrowsExceptionAfterNoMock() throws Exception {
    RuntimeException expectedException = new RuntimeException("Some Error");
    ProcessorInterceptor interceptor = spy(new ProcessorInterceptor() {

      @Override
      public void after(InterceptionEvent event, Optional<Throwable> thrown) {
        throw expectedException;
      }
    });
    startFlowWithInterceptors(interceptor);

    expected.expectCause(sameInstance(expectedException));
    process(flow, eventBuilder().message(Message.of("")).build());
  }

  @Test
  public void interceptorThrowsExceptionAround() throws Exception {
    RuntimeException expectedException = new RuntimeException("Some Error");
    ProcessorInterceptor interceptor = spy(new ProcessorInterceptor() {

      @Override
      public CompletableFuture<InterceptionEvent> around(Map<String, Object> parameters, InterceptionEvent event,
                                                         InterceptionAction action) {
        throw expectedException;
      }
    });
    startFlowWithInterceptors(interceptor);

    expected.expectCause(sameInstance(expectedException));
    try {
      process(flow, eventBuilder().message(Message.of("")).build());
    } finally {
      InOrder inOrder = inOrder(processor, interceptor);

      inOrder.verify(interceptor).before(any(), any());
      inOrder.verify(interceptor).around(any(), any(), any());
      inOrder.verify(processor, never()).process(any());
      inOrder.verify(interceptor).after(any(), eq(of(expectedException)));
    }
  }

  @Test
  public void interceptorThrowsExceptionAroundAfterProceed() throws Exception {
    RuntimeException expectedException = new RuntimeException("Some Error");
    ProcessorInterceptor interceptor = spy(new ProcessorInterceptor() {

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
      process(flow, eventBuilder().message(Message.of("")).build());
    } finally {
      InOrder inOrder = inOrder(processor, interceptor);

      inOrder.verify(interceptor).before(any(), any());
      inOrder.verify(interceptor).around(any(), any(), any());
      inOrder.verify(processor).process(any());
      inOrder.verify(interceptor).after(any(), eq(of(expectedException)));
    }
  }

  @Test
  public void interceptorThrowsExceptionAroundAfterProceedInCallback() throws Exception {
    RuntimeException expectedException = new RuntimeException("Some Error");
    ProcessorInterceptor interceptor = spy(new ProcessorInterceptor() {

      @Override
      public CompletableFuture<InterceptionEvent> around(Map<String, Object> parameters, InterceptionEvent event,
                                                         InterceptionAction action) {
        action.proceed();
        return supplyAsync(() -> {
          throw expectedException;
        });
      }
    });
    startFlowWithInterceptors(interceptor);

    expected.expectCause(sameInstance(expectedException));
    try {
      process(flow, eventBuilder().message(Message.of("")).build());
    } finally {
      InOrder inOrder = inOrder(processor, interceptor);

      inOrder.verify(interceptor).before(any(), any());
      inOrder.verify(interceptor).around(any(), any(), any());
      inOrder.verify(processor).process(any());
      inOrder.verify(interceptor).after(any(), eq(of(expectedException)));
    }
  }

  @Test
  public void interceptorThrowsExceptionAroundAfterSkipInCallback() throws Exception {
    RuntimeException expectedException = new RuntimeException("Some Error");
    ProcessorInterceptor interceptor = spy(new ProcessorInterceptor() {

      @Override
      public CompletableFuture<InterceptionEvent> around(Map<String, Object> parameters, InterceptionEvent event,
                                                         InterceptionAction action) {
        action.skip();
        return supplyAsync(() -> {
          throw expectedException;
        });
      }
    });
    startFlowWithInterceptors(interceptor);

    expected.expectCause(sameInstance(expectedException));
    try {
      process(flow, eventBuilder().message(Message.of("")).build());
    } finally {
      InOrder inOrder = inOrder(processor, interceptor);

      inOrder.verify(interceptor).before(any(), any());
      inOrder.verify(interceptor).around(any(), any(), any());
      inOrder.verify(processor, never()).process(any());
      inOrder.verify(interceptor).after(any(), eq(of(expectedException)));
    }
  }

  @Test
  public void interceptedThrowsException() throws Exception {
    RuntimeException expectedException = new RuntimeException("Some Error");
    ProcessorInterceptor interceptor = spy(new ProcessorInterceptor() {});
    startFlowWithInterceptors(interceptor);

    when(processor.process(any())).thenThrow(expectedException);

    expected.expectCause(sameInstance(expectedException));
    try {
      process(flow, eventBuilder().message(Message.of("")).build());
    } finally {
      InOrder inOrder = inOrder(processor, interceptor);

      inOrder.verify(interceptor).before(any(), any());
      inOrder.verify(interceptor).around(any(), any(), any());
      inOrder.verify(processor).process(any());
      inOrder.verify(interceptor).after(any(), eq(of(expectedException)));
    }
  }

  @Test
  public void interceptorSkipsProcessor() throws Exception {
    ProcessorInterceptor interceptor = spy(new ProcessorInterceptor() {

      @Override
      public CompletableFuture<InterceptionEvent> around(Map<String, Object> parameters, InterceptionEvent event,
                                                         InterceptionAction action) {
        return action.skip();
      }
    });
    startFlowWithInterceptors(interceptor);

    Event result = process(flow, eventBuilder().message(Message.of("")).build());
    assertThat(result.getMessage().getPayload().getValue(), is(""));
    assertThat(result.getError().isPresent(), is(false));

    InOrder inOrder = inOrder(processor, interceptor);

    inOrder.verify(interceptor).before(any(), any());
    inOrder.verify(interceptor).around(any(), any(), any());
    inOrder.verify(processor, never()).process(any());
    inOrder.verify(interceptor).after(any(), eq(empty()));
  }

  @Test
  public void firstInterceptorMutatesEventBefore() throws Exception {
    ProcessorInterceptor interceptor1 = spy(new ProcessorInterceptor() {

      @Override
      public void before(Map<String, Object> parameters, InterceptionEvent event) {
        event.message(Message.of(TEST_PAYLOAD));
      }
    });
    ProcessorInterceptor interceptor2 = spy(new ProcessorInterceptor() {});
    startFlowWithInterceptors(interceptor1, interceptor2);

    Event result = process(flow, eventBuilder().message(Message.of("")).build());
    assertThat(result.getMessage().getPayload().getValue(), is(TEST_PAYLOAD));
    assertThat(result.getError().isPresent(), is(false));

    InOrder inOrder = inOrder(processor, interceptor1, interceptor2);

    inOrder.verify(interceptor1).before(mapArgWithEntry("param", ""), any());
    inOrder.verify(interceptor1).around(mapArgWithEntry("param", TEST_PAYLOAD),
                                        argThat(interceptionHasPayloadValue(TEST_PAYLOAD)), any());
    inOrder.verify(interceptor2).before(mapArgWithEntry("param", TEST_PAYLOAD),
                                        argThat(interceptionHasPayloadValue(TEST_PAYLOAD)));
    inOrder.verify(interceptor2).around(mapArgWithEntry("param", TEST_PAYLOAD),
                                        argThat(interceptionHasPayloadValue(TEST_PAYLOAD)), any());
    inOrder.verify(processor).process(argThat(hasPayloadValue(TEST_PAYLOAD)));
    inOrder.verify(interceptor2).after(argThat(interceptionHasPayloadValue(TEST_PAYLOAD)), eq(empty()));
    inOrder.verify(interceptor1).after(argThat(interceptionHasPayloadValue(TEST_PAYLOAD)), eq(empty()));
  }

  @Test
  public void secondInterceptorMutatesEventBefore() throws Exception {
    ProcessorInterceptor interceptor1 = spy(new ProcessorInterceptor() {});
    ProcessorInterceptor interceptor2 = spy(new ProcessorInterceptor() {

      @Override
      public void before(Map<String, Object> parameters, InterceptionEvent event) {
        event.message(Message.of(TEST_PAYLOAD));
      }
    });
    startFlowWithInterceptors(interceptor1, interceptor2);

    Event result = process(flow, eventBuilder().message(Message.of("")).build());
    assertThat(result.getMessage().getPayload().getValue(), is(TEST_PAYLOAD));
    assertThat(result.getError().isPresent(), is(false));

    InOrder inOrder = inOrder(processor, interceptor1, interceptor2);

    inOrder.verify(interceptor1).before(mapArgWithEntry("param", ""), argThat(interceptionHasPayloadValue("")));
    inOrder.verify(interceptor1).around(mapArgWithEntry("param", ""), argThat(interceptionHasPayloadValue("")), any());
    inOrder.verify(interceptor2).before(mapArgWithEntry("param", ""), any());
    inOrder.verify(interceptor2).around(mapArgWithEntry("param", TEST_PAYLOAD),
                                        argThat(interceptionHasPayloadValue(TEST_PAYLOAD)), any());
    inOrder.verify(processor).process(argThat(hasPayloadValue(TEST_PAYLOAD)));
    inOrder.verify(interceptor2).after(argThat(interceptionHasPayloadValue(TEST_PAYLOAD)), eq(empty()));
    inOrder.verify(interceptor1).after(argThat(interceptionHasPayloadValue(TEST_PAYLOAD)), eq(empty()));
  }

  @Test
  public void firstInterceptorMutatesEventAfter() throws Exception {
    ProcessorInterceptor interceptor1 = spy(new ProcessorInterceptor() {

      @Override
      public void after(InterceptionEvent event, Optional<Throwable> thrown) {
        event.message(Message.of(TEST_PAYLOAD));
      }
    });
    ProcessorInterceptor interceptor2 = spy(new ProcessorInterceptor() {});
    startFlowWithInterceptors(interceptor1, interceptor2);

    Event result = process(flow, eventBuilder().message(Message.of("")).build());
    assertThat(result.getMessage().getPayload().getValue(), is(TEST_PAYLOAD));
    assertThat(result.getError().isPresent(), is(false));

    InOrder inOrder = inOrder(processor, interceptor1, interceptor2);

    inOrder.verify(interceptor1).before(any(), argThat(interceptionHasPayloadValue("")));
    inOrder.verify(interceptor1).around(any(), argThat(interceptionHasPayloadValue("")), any());
    inOrder.verify(interceptor2).before(any(), argThat(interceptionHasPayloadValue("")));
    inOrder.verify(interceptor2).around(any(), argThat(interceptionHasPayloadValue("")), any());
    inOrder.verify(processor).process(argThat(hasPayloadValue("")));
    inOrder.verify(interceptor2).after(argThat(interceptionHasPayloadValue("")), eq(empty()));
    inOrder.verify(interceptor1).after(any(), eq(empty()));
  }

  @Test
  public void secondInterceptorMutatesEventAfter() throws Exception {
    ProcessorInterceptor interceptor1 = spy(new ProcessorInterceptor() {});
    ProcessorInterceptor interceptor2 = spy(new ProcessorInterceptor() {

      @Override
      public void after(InterceptionEvent event, Optional<Throwable> thrown) {
        event.message(Message.of(TEST_PAYLOAD));
      }
    });
    startFlowWithInterceptors(interceptor1, interceptor2);

    Event result = process(flow, eventBuilder().message(Message.of("")).build());
    assertThat(result.getMessage().getPayload().getValue(), is(TEST_PAYLOAD));
    assertThat(result.getError().isPresent(), is(false));

    InOrder inOrder = inOrder(processor, interceptor1, interceptor2);

    inOrder.verify(interceptor1).before(any(), argThat(interceptionHasPayloadValue("")));
    inOrder.verify(interceptor1).around(any(), argThat(interceptionHasPayloadValue("")), any());
    inOrder.verify(interceptor2).before(any(), argThat(interceptionHasPayloadValue("")));
    inOrder.verify(interceptor2).around(any(), argThat(interceptionHasPayloadValue("")), any());
    inOrder.verify(processor).process(argThat(hasPayloadValue("")));
    inOrder.verify(interceptor2).after(any(), eq(empty()));
    inOrder.verify(interceptor1).after(argThat(interceptionHasPayloadValue(TEST_PAYLOAD)), eq(empty()));
  }

  @Test
  public void firstInterceptorMutatesEventAroundBeforeProceed() throws Exception {
    ProcessorInterceptor interceptor1 = spy(new ProcessorInterceptor() {

      @Override
      public CompletableFuture<InterceptionEvent> around(Map<String, Object> parameters, InterceptionEvent event,
                                                         InterceptionAction action) {
        event.message(Message.of(TEST_PAYLOAD));
        return action.proceed();
      }
    });
    ProcessorInterceptor interceptor2 = spy(new ProcessorInterceptor() {});
    startFlowWithInterceptors(interceptor1, interceptor2);

    Event result = process(flow, eventBuilder().message(Message.of("")).build());
    assertThat(result.getMessage().getPayload().getValue(), is(TEST_PAYLOAD));
    assertThat(result.getError().isPresent(), is(false));

    InOrder inOrder = inOrder(processor, interceptor1, interceptor2);

    inOrder.verify(interceptor1).before(mapArgWithEntry("param", ""), argThat(interceptionHasPayloadValue("")));
    inOrder.verify(interceptor1).around(mapArgWithEntry("param", ""), any(), any());
    inOrder.verify(interceptor2).before(mapArgWithEntry("param", TEST_PAYLOAD),
                                        argThat(interceptionHasPayloadValue(TEST_PAYLOAD)));
    inOrder.verify(interceptor2).around(mapArgWithEntry("param", TEST_PAYLOAD),
                                        argThat(interceptionHasPayloadValue(TEST_PAYLOAD)), any());
    inOrder.verify(processor).process(argThat(hasPayloadValue(TEST_PAYLOAD)));
    inOrder.verify(interceptor2).after(argThat(interceptionHasPayloadValue(TEST_PAYLOAD)), eq(empty()));
    inOrder.verify(interceptor1).after(argThat(interceptionHasPayloadValue(TEST_PAYLOAD)), eq(empty()));
  }

  @Test
  public void secondInterceptorMutatesEventAroundBeforeProceed() throws Exception {
    ProcessorInterceptor interceptor1 = spy(new ProcessorInterceptor() {});
    ProcessorInterceptor interceptor2 = spy(new ProcessorInterceptor() {

      @Override
      public CompletableFuture<InterceptionEvent> around(Map<String, Object> parameters, InterceptionEvent event,
                                                         InterceptionAction action) {
        event.message(Message.of(TEST_PAYLOAD));
        return action.proceed();
      }
    });
    startFlowWithInterceptors(interceptor1, interceptor2);

    Event result = process(flow, eventBuilder().message(Message.of("")).build());
    assertThat(result.getMessage().getPayload().getValue(), is(TEST_PAYLOAD));
    assertThat(result.getError().isPresent(), is(false));

    InOrder inOrder = inOrder(processor, interceptor1, interceptor2);

    inOrder.verify(interceptor1).before(mapArgWithEntry("param", ""), argThat(interceptionHasPayloadValue("")));
    inOrder.verify(interceptor1).around(mapArgWithEntry("param", ""), argThat(interceptionHasPayloadValue("")), any());
    inOrder.verify(interceptor2).before(mapArgWithEntry("param", ""), argThat(interceptionHasPayloadValue("")));
    inOrder.verify(interceptor2).around(mapArgWithEntry("param", ""), any(), any());
    inOrder.verify(processor).process(argThat(hasPayloadValue(TEST_PAYLOAD)));
    inOrder.verify(interceptor2).after(argThat(interceptionHasPayloadValue(TEST_PAYLOAD)), eq(empty()));
    inOrder.verify(interceptor1).after(argThat(interceptionHasPayloadValue(TEST_PAYLOAD)), eq(empty()));
  }

  @Test
  public void firstInterceptorMutatesEventAroundAfterProceed() throws Exception {
    ProcessorInterceptor interceptor1 = spy(new ProcessorInterceptor() {

      @Override
      public CompletableFuture<InterceptionEvent> around(Map<String, Object> parameters, InterceptionEvent event,
                                                         InterceptionAction action) {
        action.proceed();
        return supplyAsync(() -> {
          event.message(Message.of(TEST_PAYLOAD));
          return event;
        });
      }
    });
    ProcessorInterceptor interceptor2 = spy(new ProcessorInterceptor() {});
    startFlowWithInterceptors(interceptor1, interceptor2);

    Event result = process(flow, eventBuilder().message(Message.of("")).build());
    assertThat(result.getMessage().getPayload().getValue(), is(TEST_PAYLOAD));
    assertThat(result.getError().isPresent(), is(false));

    InOrder inOrder = inOrder(processor, interceptor1, interceptor2);

    inOrder.verify(interceptor1).before(mapArgWithEntry("param", ""), argThat(interceptionHasPayloadValue("")));
    inOrder.verify(interceptor1).around(mapArgWithEntry("param", ""), any(), any());
    inOrder.verify(interceptor2).before(mapArgWithEntry("param", ""), argThat(interceptionHasPayloadValue("")));
    inOrder.verify(interceptor2).around(mapArgWithEntry("param", ""), argThat(interceptionHasPayloadValue("")), any());
    inOrder.verify(processor).process(argThat(hasPayloadValue("")));
    inOrder.verify(interceptor2).after(argThat(interceptionHasPayloadValue("")), eq(empty()));
    inOrder.verify(interceptor1).after(argThat(interceptionHasPayloadValue(TEST_PAYLOAD)), eq(empty()));
  }

  @Test
  public void secondInterceptorMutatesEventAroundAfterProceed() throws Exception {
    ProcessorInterceptor interceptor1 = spy(new ProcessorInterceptor() {});
    ProcessorInterceptor interceptor2 = spy(new ProcessorInterceptor() {

      @Override
      public CompletableFuture<InterceptionEvent> around(Map<String, Object> parameters, InterceptionEvent event,
                                                         InterceptionAction action) {
        action.proceed();
        return supplyAsync(() -> {
          event.message(Message.of(TEST_PAYLOAD));
          return event;
        });
      }
    });
    startFlowWithInterceptors(interceptor1, interceptor2);

    Event result = process(flow, eventBuilder().message(Message.of("")).build());
    assertThat(result.getMessage().getPayload().getValue(), is(TEST_PAYLOAD));
    assertThat(result.getError().isPresent(), is(false));

    InOrder inOrder = inOrder(processor, interceptor1, interceptor2);

    inOrder.verify(interceptor1).before(mapArgWithEntry("param", ""), argThat(interceptionHasPayloadValue("")));
    inOrder.verify(interceptor1).around(mapArgWithEntry("param", ""), argThat(interceptionHasPayloadValue("")), any());
    inOrder.verify(interceptor2).before(mapArgWithEntry("param", ""), argThat(interceptionHasPayloadValue("")));
    inOrder.verify(interceptor2).around(mapArgWithEntry("param", ""), any(), any());
    inOrder.verify(processor).process(argThat(hasPayloadValue("")));
    inOrder.verify(interceptor2).after(argThat(interceptionHasPayloadValue(TEST_PAYLOAD)), eq(empty()));
    inOrder.verify(interceptor1).after(argThat(interceptionHasPayloadValue(TEST_PAYLOAD)), eq(empty()));
  }

  @Test
  public void firstInterceptorThrowsExceptionBefore() throws Exception {
    RuntimeException expectedException = new RuntimeException("Some Error");
    ProcessorInterceptor interceptor1 = spy(new ProcessorInterceptor() {

      @Override
      public void before(Map<String, Object> parameters, InterceptionEvent event) {
        throw expectedException;
      }
    });
    ProcessorInterceptor interceptor2 = spy(new ProcessorInterceptor() {});
    startFlowWithInterceptors(interceptor1, interceptor2);

    expected.expectCause(sameInstance(expectedException));
    try {
      process(flow, eventBuilder().message(Message.of("")).build());
    } finally {
      InOrder inOrder = inOrder(processor, interceptor1, interceptor2);

      inOrder.verify(interceptor1).before(any(), any());
      inOrder.verify(interceptor1, never()).around(any(), any(), any());
      inOrder.verify(interceptor2, never()).before(any(), any());
      inOrder.verify(interceptor2, never()).around(any(), any(), any());
      inOrder.verify(processor, never()).process(any());
      inOrder.verify(interceptor2, never()).after(any(), eq(of(expectedException)));
      inOrder.verify(interceptor1).after(any(), eq(of(expectedException)));
    }
  }

  @Test
  public void secondInterceptorThrowsExceptionBefore() throws Exception {
    RuntimeException expectedException = new RuntimeException("Some Error");
    ProcessorInterceptor interceptor1 = spy(new ProcessorInterceptor() {});
    ProcessorInterceptor interceptor2 = spy(new ProcessorInterceptor() {

      @Override
      public void before(Map<String, Object> parameters, InterceptionEvent event) {
        throw expectedException;
      }
    });
    startFlowWithInterceptors(interceptor1, interceptor2);

    expected.expectCause(sameInstance(expectedException));
    try {
      process(flow, eventBuilder().message(Message.of("")).build());
    } finally {
      InOrder inOrder = inOrder(processor, interceptor1, interceptor2);

      inOrder.verify(interceptor1).before(any(), any());
      inOrder.verify(interceptor1).around(any(), any(), any());
      inOrder.verify(interceptor2).before(any(), any());
      inOrder.verify(interceptor2, never()).around(any(), any(), any());
      inOrder.verify(processor, never()).process(any());
      inOrder.verify(interceptor2).after(any(), eq(of(expectedException)));
      inOrder.verify(interceptor1).after(any(), eq(of(expectedException)));
    }
  }

  @Test
  public void firstInterceptorThrowsExceptionAfter() throws Exception {
    RuntimeException expectedException = new RuntimeException("Some Error");
    ProcessorInterceptor interceptor1 = spy(new ProcessorInterceptor() {

      @Override
      public void after(InterceptionEvent event, Optional<Throwable> thrown) {
        throw expectedException;
      }
    });
    ProcessorInterceptor interceptor2 = spy(new ProcessorInterceptor() {});
    startFlowWithInterceptors(interceptor1, interceptor2);

    expected.expectCause(sameInstance(expectedException));
    try {
      process(flow, eventBuilder().message(Message.of("")).build());
    } finally {
      InOrder inOrder = inOrder(processor, interceptor1, interceptor2);

      inOrder.verify(interceptor1).before(any(), any());
      inOrder.verify(interceptor1).around(any(), any(), any());
      inOrder.verify(interceptor2).before(any(), any());
      inOrder.verify(interceptor2).around(any(), any(), any());
      inOrder.verify(processor).process(any());
      inOrder.verify(interceptor2).after(any(), eq(empty()));
      inOrder.verify(interceptor1).after(any(), eq(empty()));
    }
  }

  @Test
  public void secondInterceptorThrowsExceptionAfter() throws Exception {
    RuntimeException expectedException = new RuntimeException("Some Error");
    ProcessorInterceptor interceptor1 = spy(new ProcessorInterceptor() {});
    ProcessorInterceptor interceptor2 = spy(new ProcessorInterceptor() {

      @Override
      public void after(InterceptionEvent event, Optional<Throwable> thrown) {
        throw expectedException;
      }
    });
    startFlowWithInterceptors(interceptor1, interceptor2);

    expected.expectCause(sameInstance(expectedException));
    try {
      process(flow, eventBuilder().message(Message.of("")).build());
    } finally {
      InOrder inOrder = inOrder(processor, interceptor1, interceptor2);

      inOrder.verify(interceptor1).before(any(), any());
      inOrder.verify(interceptor1).around(any(), any(), any());
      inOrder.verify(interceptor2).before(any(), any());
      inOrder.verify(interceptor2).around(any(), any(), any());
      inOrder.verify(processor).process(any());
      inOrder.verify(interceptor2).after(any(), eq(empty()));
      inOrder.verify(interceptor1).after(any(), eq(of(expectedException)));
    }
  }

  @Test
  public void firstInterceptorThrowsExceptionAround() throws Exception {
    RuntimeException expectedException = new RuntimeException("Some Error");
    ProcessorInterceptor interceptor1 = spy(new ProcessorInterceptor() {

      @Override
      public CompletableFuture<InterceptionEvent> around(Map<String, Object> parameters, InterceptionEvent event,
                                                         InterceptionAction action) {
        throw expectedException;
      }
    });
    ProcessorInterceptor interceptor2 = spy(new ProcessorInterceptor() {});
    startFlowWithInterceptors(interceptor1, interceptor2);

    expected.expectCause(sameInstance(expectedException));
    try {
      process(flow, eventBuilder().message(Message.of("")).build());
    } finally {
      InOrder inOrder = inOrder(processor, interceptor1, interceptor2);

      inOrder.verify(interceptor1).before(any(), any());
      inOrder.verify(interceptor1).around(any(), any(), any());
      inOrder.verify(interceptor2, never()).before(any(), any());
      inOrder.verify(interceptor2, never()).around(any(), any(), any());
      inOrder.verify(processor, never()).process(any());
      inOrder.verify(interceptor2, never()).after(any(), eq(of(expectedException)));
      inOrder.verify(interceptor1).after(any(), eq(of(expectedException)));
    }
  }

  @Test
  public void secondInterceptorThrowsExceptionAround() throws Exception {
    RuntimeException expectedException = new RuntimeException("Some Error");
    ProcessorInterceptor interceptor1 = spy(new ProcessorInterceptor() {});
    ProcessorInterceptor interceptor2 = spy(new ProcessorInterceptor() {

      @Override
      public CompletableFuture<InterceptionEvent> around(Map<String, Object> parameters, InterceptionEvent event,
                                                         InterceptionAction action) {
        throw expectedException;
      }
    });
    startFlowWithInterceptors(interceptor1, interceptor2);

    expected.expectCause(sameInstance(expectedException));
    try {
      process(flow, eventBuilder().message(Message.of("")).build());
    } finally {
      InOrder inOrder = inOrder(processor, interceptor1, interceptor2);

      inOrder.verify(interceptor1).before(any(), any());
      inOrder.verify(interceptor1).around(any(), any(), any());
      inOrder.verify(interceptor2).before(any(), any());
      inOrder.verify(interceptor2).around(any(), any(), any());
      inOrder.verify(processor, never()).process(any());
      inOrder.verify(interceptor2).after(any(), eq(of(expectedException)));
      inOrder.verify(interceptor1).after(any(), eq(of(expectedException)));
    }
  }

  @Test
  public void firstInterceptorThrowsExceptionAroundAfterProceed() throws Exception {
    RuntimeException expectedException = new RuntimeException("Some Error");
    ProcessorInterceptor interceptor1 = spy(new ProcessorInterceptor() {

      @Override
      public CompletableFuture<InterceptionEvent> around(Map<String, Object> parameters, InterceptionEvent event,
                                                         InterceptionAction action) {
        action.proceed();
        throw expectedException;
      }
    });
    ProcessorInterceptor interceptor2 = spy(new ProcessorInterceptor() {});
    startFlowWithInterceptors(interceptor1, interceptor2);

    expected.expectCause(sameInstance(expectedException));
    try {
      process(flow, eventBuilder().message(Message.of("")).build());
    } finally {
      InOrder inOrder = inOrder(processor, interceptor1, interceptor2);

      inOrder.verify(interceptor1).before(any(), any());
      inOrder.verify(interceptor1).around(any(), any(), any());
      inOrder.verify(interceptor2).before(any(), any());
      inOrder.verify(interceptor2).around(any(), any(), any());
      inOrder.verify(processor).process(any());
      inOrder.verify(interceptor2).after(any(), eq(empty()));
      inOrder.verify(interceptor1).after(any(), eq(of(expectedException)));
    }
  }

  @Test
  public void secondInterceptorThrowsExceptionAroundAfterProceed() throws Exception {
    RuntimeException expectedException = new RuntimeException("Some Error");
    ProcessorInterceptor interceptor1 = spy(new ProcessorInterceptor() {});
    ProcessorInterceptor interceptor2 = spy(new ProcessorInterceptor() {

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
      process(flow, eventBuilder().message(Message.of("")).build());
    } finally {
      InOrder inOrder = inOrder(processor, interceptor1, interceptor2);

      inOrder.verify(interceptor1).before(any(), any());
      inOrder.verify(interceptor1).around(any(), any(), any());
      inOrder.verify(interceptor2).before(any(), any());
      inOrder.verify(interceptor2).around(any(), any(), any());
      inOrder.verify(processor).process(any());
      inOrder.verify(interceptor2).after(any(), eq(of(expectedException)));
      inOrder.verify(interceptor1).after(any(), eq(of(expectedException)));
    }
  }

  @Test
  public void firstInterceptorSkipsProcessor() throws Exception {
    ProcessorInterceptor interceptor1 = spy(new ProcessorInterceptor() {

      @Override
      public CompletableFuture<InterceptionEvent> around(Map<String, Object> parameters, InterceptionEvent event,
                                                         InterceptionAction action) {
        return action.skip();
      }
    });
    ProcessorInterceptor interceptor2 = spy(new ProcessorInterceptor() {});
    startFlowWithInterceptors(interceptor1, interceptor2);

    Event result = process(flow, eventBuilder().message(Message.of("")).build());
    assertThat(result.getMessage().getPayload().getValue(), is(""));

    InOrder inOrder = inOrder(processor, interceptor1, interceptor2);

    inOrder.verify(interceptor1).before(any(), any());
    inOrder.verify(interceptor1).around(any(), any(), any());
    inOrder.verify(interceptor2, never()).before(any(), any());
    inOrder.verify(interceptor2, never()).around(any(), any(), any());
    inOrder.verify(processor, never()).process(any());
    inOrder.verify(interceptor2, never()).after(any(), eq(empty()));
    inOrder.verify(interceptor1).after(argThat(interceptionHasPayloadValue("")), eq(empty()));
  }

  @Test
  public void secondInterceptorSkipsProcessor() throws Exception {
    ProcessorInterceptor interceptor1 = spy(new ProcessorInterceptor() {});
    ProcessorInterceptor interceptor2 = spy(new ProcessorInterceptor() {

      @Override
      public CompletableFuture<InterceptionEvent> around(Map<String, Object> parameters, InterceptionEvent event,
                                                         InterceptionAction action) {
        return action.skip();
      }
    });
    startFlowWithInterceptors(interceptor1, interceptor2);

    Event result = process(flow, eventBuilder().message(Message.of("")).build());
    assertThat(result.getMessage().getPayload().getValue(), is(""));

    InOrder inOrder = inOrder(processor, interceptor1, interceptor2);

    inOrder.verify(interceptor1).before(any(), any());
    inOrder.verify(interceptor1).around(any(), any(), any());
    inOrder.verify(interceptor2).before(any(), any());
    inOrder.verify(interceptor2).around(any(), any(), any());
    inOrder.verify(processor, never()).process(any());
    inOrder.verify(interceptor2).after(any(), eq(empty()));
    inOrder.verify(interceptor1).after(any(), eq(empty()));
  }

  @Test
  public void firstInterceptorDoesntApply() throws Exception {
    ProcessorInterceptor interceptor1 = spy(new ProcessorInterceptor() {});
    ProcessorInterceptor interceptor2 = spy(new ProcessorInterceptor() {});
    startFlowWithInterceptorFactories(new ProcessorInterceptorFactory() {

      @Override
      public boolean intercept(ComponentLocation location) {
        return false;
      }

      @Override
      public ProcessorInterceptor get() {
        return interceptor1;
      };
    }, () -> interceptor2);

    Event result = process(flow, eventBuilder().message(Message.of("")).build());
    assertThat(result.getMessage().getPayload().getValue(), is(""));
    assertThat(result.getError().isPresent(), is(false));

    InOrder inOrder = inOrder(processor, interceptor1, interceptor2);

    inOrder.verify(interceptor1, never()).before(any(), any());
    inOrder.verify(interceptor1, never()).around(any(), any(), any());
    inOrder.verify(interceptor2).before(any(), any());
    inOrder.verify(interceptor2).around(any(), any(), any());
    inOrder.verify(processor).process(any());
    inOrder.verify(interceptor2).after(any(), eq(empty()));
    inOrder.verify(interceptor1, never()).after(any(), eq(empty()));
  }

  @Test
  public void secondInterceptorDoesntApply() throws Exception {
    ProcessorInterceptor interceptor1 = spy(new ProcessorInterceptor() {});
    ProcessorInterceptor interceptor2 = spy(new ProcessorInterceptor() {});
    startFlowWithInterceptorFactories(() -> interceptor1, new ProcessorInterceptorFactory() {

      @Override
      public boolean intercept(ComponentLocation location) {
        return false;
      }

      @Override
      public ProcessorInterceptor get() {
        return interceptor2;
      };
    });

    Event result = process(flow, eventBuilder().message(Message.of("")).build());
    assertThat(result.getMessage().getPayload().getValue(), is(""));
    assertThat(result.getError().isPresent(), is(false));

    InOrder inOrder = inOrder(processor, interceptor1, interceptor2);

    inOrder.verify(interceptor1).before(any(), any());
    inOrder.verify(interceptor1).around(any(), any(), any());
    inOrder.verify(interceptor2, never()).before(any(), any());
    inOrder.verify(interceptor2, never()).around(any(), any(), any());
    inOrder.verify(processor).process(any());
    inOrder.verify(interceptor2, never()).after(any(), eq(empty()));
    inOrder.verify(interceptor1).after(any(), eq(empty()));
  }

  private void startFlowWithInterceptors(ProcessorInterceptor... interceptors) throws Exception {
    for (ProcessorInterceptor interceptionHandler : interceptors) {
      muleContext.getProcessorInterceptorManager().addInterceptorFactory(() -> interceptionHandler);
    }

    flow.initialise();
    flow.start();
  }

  private void startFlowWithInterceptorFactories(ProcessorInterceptorFactory... interceptorFactories) throws Exception {
    for (ProcessorInterceptorFactory interceptionHandlerFactory : interceptorFactories) {
      muleContext.getProcessorInterceptorManager().addInterceptorFactory(interceptionHandlerFactory);
    }

    flow.initialise();
    flow.start();
  }

  private static class ProcessorInApp extends AbstractAnnotatedObject implements Processor {

    public ProcessorInApp() {
      setAnnotations(ImmutableMap.<QName, Object>builder()
          .put(ANNOTATION_PARAMETERS, singletonMap("param", "#[payload]"))
          .put(LOCATION_KEY,
               new DefaultComponentLocation(of("flowName"),
                                            singletonList(new DefaultLocationPart("0",
                                                                                  of(
                                                                                     builder()
                                                                                         .withIdentifier(
                                                                                                         buildFromStringRepresentation("test:processor"))
                                                                                         .withType(PROCESSOR)
                                                                                         .build()),
                                                                                  empty(), empty()))))
          .build());
    }

    @Override
    public Event process(Event event) throws MuleException {
      return event;
    }
  }

  private static Map<String, Object> mapArgWithEntry(String key, Object value) {
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
