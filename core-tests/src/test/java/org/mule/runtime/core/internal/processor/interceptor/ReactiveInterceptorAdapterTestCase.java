/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.processor.interceptor;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.concurrent.CompletableFuture.supplyAsync;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.collection.IsMapContaining.hasEntry;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsSame.sameInstance;
import static org.junit.Assert.assertThat;
import static org.junit.rules.ExpectedException.none;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mule.runtime.api.component.ComponentIdentifier.buildFromStringRepresentation;
import static org.mule.runtime.api.component.TypedComponentIdentifier.ComponentType.PROCESSOR;
import static org.mule.runtime.api.component.TypedComponentIdentifier.builder;
import static org.mule.runtime.core.api.construct.Flow.builder;
import static org.mule.runtime.core.api.exception.Errors.ComponentIdentifiers.UNKNOWN;
import static org.mule.runtime.core.internal.component.ComponentAnnotations.ANNOTATION_PARAMETERS;
import static org.mule.tck.junit4.matcher.EventMatcher.hasErrorType;
import static org.mule.tck.junit4.matcher.EventMatcher.hasErrorTypeThat;
import static org.mule.tck.junit4.matcher.MessagingExceptionMatcher.withEventThat;
import org.mule.runtime.api.component.TypedComponentIdentifier;
import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.interception.InterceptionAction;
import org.mule.runtime.api.interception.InterceptionEvent;
import org.mule.runtime.api.interception.ProcessorInterceptor;
import org.mule.runtime.api.interception.ProcessorInterceptorFactory;
import org.mule.runtime.api.message.ErrorType;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.meta.AbstractAnnotatedObject;
import org.mule.runtime.api.meta.AnnotatedObject;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.construct.Flow;
import org.mule.runtime.core.api.exception.MessagingException;
import org.mule.runtime.core.api.processor.ParametersResolverProcessor;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.dsl.api.component.config.DefaultComponentLocation;
import org.mule.runtime.dsl.api.component.config.DefaultComponentLocation.DefaultLocationPart;
import org.mule.runtime.extension.api.runtime.operation.ExecutionContext;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.tck.size.SmallTest;

import com.google.common.collect.ImmutableMap;

import java.util.Collection;
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
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.InOrder;
import org.mockito.verification.VerificationMode;

@SmallTest
@RunWith(Parameterized.class)
public class ReactiveInterceptorAdapterTestCase extends AbstractMuleContextTestCase {

  private final boolean useMockInterceptor;
  private final Processor processor;

  private Flow flow;

  @Rule
  public ExpectedException expected = none();

  public ReactiveInterceptorAdapterTestCase(boolean useMockInterceptor, Processor processor) {
    this.useMockInterceptor = useMockInterceptor;
    this.processor = spy(processor);
  }

  /*
   * The cases with no mocks exercise the optimized interception path when 'around' is not implemented. This is needed because
   * Mockito mocks override the methods.
   */
  @Parameters
  public static Collection<Object[]> data() {
    return asList(new Object[][] {
        {true, new ProcessorInApp()},
        {true, new OperationProcessorInApp()},
        {false, new ProcessorInApp()},
        {false, new OperationProcessorInApp()}
    });
  }

  @Before
  public void before() throws MuleException {
    flow = builder("flow", muleContext).processors(processor).build();
  }

  @After
  public void after() throws MuleException {
    try {
      flow.stop();
      flow.dispose();
    } catch (Exception e) {
      // TODO remove this catch if this gets fixed
    }
  }

  private ProcessorInterceptor prepareInterceptor(ProcessorInterceptor interceptor) {
    if (useMockInterceptor) {
      return spy(interceptor);
    } else {
      return interceptor;
    }
  }

  @Test
  public void interceptorApplied() throws Exception {
    ProcessorInterceptor interceptor = prepareInterceptor(new ProcessorInterceptor() {});
    startFlowWithInterceptors(interceptor);

    Event result = process(flow, eventBuilder().message(Message.of("")).build());
    assertThat(result.getMessage().getPayload().getValue(), is(""));
    assertThat(result.getError().isPresent(), is(false));

    if (useMockInterceptor) {
      InOrder inOrder = inOrder(processor, interceptor);

      inOrder.verify(interceptor).before(eq(((AnnotatedObject) processor).getLocation()), mapArgWithEntry("param", ""), any());
      inOrder.verify(interceptor).around(eq(((AnnotatedObject) processor).getLocation()), mapArgWithEntry("param", ""), any(),
                                         any());
      inOrder.verify(processor).process(argThat(hasPayloadValue("")));
      inOrder.verify(interceptor).after(eq(((AnnotatedObject) processor).getLocation()), any(), eq(empty()));

      assertThat(result.getInternalParameters().entrySet(), hasSize(0));
      verifyParametersResolvedAndDisposed(times(1));
    }
  }

  @Test
  public void interceptorMutatesEventBefore() throws Exception {
    ProcessorInterceptor interceptor = prepareInterceptor(new ProcessorInterceptor() {

      @Override
      public void before(ComponentLocation location, Map<String, Object> parameters, InterceptionEvent event) {
        event.message(Message.of(TEST_PAYLOAD));
      }
    });
    startFlowWithInterceptors(interceptor);

    Event result = process(flow, eventBuilder().message(Message.of("")).build());
    assertThat(result.getMessage().getPayload().getValue(), is(TEST_PAYLOAD));
    assertThat(result.getError().isPresent(), is(false));

    if (useMockInterceptor) {
      InOrder inOrder = inOrder(processor, interceptor);

      inOrder.verify(interceptor).before(any(), mapArgWithEntry("param", ""), any());
      inOrder.verify(interceptor).around(any(), mapArgWithEntry("param", TEST_PAYLOAD),
                                         argThat(interceptionHasPayloadValue(TEST_PAYLOAD)),
                                         any());
      inOrder.verify(processor).process(argThat(hasPayloadValue(TEST_PAYLOAD)));
      inOrder.verify(interceptor).after(any(), argThat(interceptionHasPayloadValue(TEST_PAYLOAD)), eq(empty()));

      assertThat(result.getInternalParameters().entrySet(), hasSize(0));
      verifyParametersResolvedAndDisposed(times(2));
    }
  }

  @Test
  public void interceptorMutatesEventAfter() throws Exception {
    ProcessorInterceptor interceptor = prepareInterceptor(new ProcessorInterceptor() {

      @Override
      public void after(ComponentLocation location, InterceptionEvent event, Optional<Throwable> thrown) {
        event.message(Message.of(TEST_PAYLOAD));
      }
    });
    startFlowWithInterceptors(interceptor);

    Event result = process(flow, eventBuilder().message(Message.of("")).build());
    assertThat(result.getMessage().getPayload().getValue(), is(TEST_PAYLOAD));
    assertThat(result.getError().isPresent(), is(false));

    if (useMockInterceptor) {
      InOrder inOrder = inOrder(processor, interceptor);

      inOrder.verify(interceptor).before(any(), mapArgWithEntry("param", ""), any());
      inOrder.verify(interceptor).around(any(), mapArgWithEntry("param", ""), any(), any());
      inOrder.verify(processor).process(argThat(hasPayloadValue("")));
      inOrder.verify(interceptor).after(any(), any(), eq(empty()));

      assertThat(result.getInternalParameters().entrySet(), hasSize(0));
      verifyParametersResolvedAndDisposed(times(1));
    }
  }

  @Test
  public void interceptorMutatesEventAroundBeforeProceed() throws Exception {
    ProcessorInterceptor interceptor = prepareInterceptor(new ProcessorInterceptor() {

      @Override
      public CompletableFuture<InterceptionEvent> around(ComponentLocation location, Map<String, Object> parameters,
                                                         InterceptionEvent event, InterceptionAction action) {
        event.message(Message.of(TEST_PAYLOAD));
        return action.proceed();
      }
    });
    startFlowWithInterceptors(interceptor);

    Event result = process(flow, eventBuilder().message(Message.of("")).build());
    assertThat(result.getMessage().getPayload().getValue(), is(TEST_PAYLOAD));
    assertThat(result.getError().isPresent(), is(false));

    if (useMockInterceptor) {
      InOrder inOrder = inOrder(processor, interceptor);

      inOrder.verify(interceptor).before(any(), mapArgWithEntry("param", ""), any());
      inOrder.verify(interceptor).around(any(), mapArgWithEntry("param", ""), any(), any());
      inOrder.verify(processor).process(argThat(hasPayloadValue(TEST_PAYLOAD)));
      inOrder.verify(interceptor).after(any(), argThat(interceptionHasPayloadValue(TEST_PAYLOAD)), eq(empty()));

      assertThat(result.getInternalParameters().entrySet(), hasSize(0));
      verifyParametersResolvedAndDisposed(times(1));
    }
  }

  @Test
  public void interceptorMutatesEventAroundAfterProceed() throws Exception {
    ProcessorInterceptor interceptor = prepareInterceptor(new ProcessorInterceptor() {

      @Override
      public CompletableFuture<InterceptionEvent> around(ComponentLocation location, Map<String, Object> parameters,
                                                         InterceptionEvent event, InterceptionAction action) {
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

    if (useMockInterceptor) {
      InOrder inOrder = inOrder(processor, interceptor);

      inOrder.verify(interceptor).before(any(), mapArgWithEntry("param", ""), any());
      inOrder.verify(interceptor).around(any(), mapArgWithEntry("param", ""), any(), any());
      inOrder.verify(processor).process(argThat(hasPayloadValue("")));
      inOrder.verify(interceptor).after(any(), argThat(interceptionHasPayloadValue(TEST_PAYLOAD)), eq(empty()));

      assertThat(result.getInternalParameters().entrySet(), hasSize(0));
      verifyParametersResolvedAndDisposed(times(1));
    }
  }

  @Test
  public void interceptorMutatesEventAroundBeforeSkip() throws Exception {
    ProcessorInterceptor interceptor = prepareInterceptor(new ProcessorInterceptor() {

      @Override
      public CompletableFuture<InterceptionEvent> around(ComponentLocation location, Map<String, Object> parameters,
                                                         InterceptionEvent event, InterceptionAction action) {
        event.message(Message.of(TEST_PAYLOAD));
        return action.skip();
      }
    });
    startFlowWithInterceptors(interceptor);

    Event result = process(flow, eventBuilder().message(Message.of("")).build());
    assertThat(result.getMessage().getPayload().getValue(), is(TEST_PAYLOAD));
    assertThat(result.getError().isPresent(), is(false));

    if (useMockInterceptor) {
      InOrder inOrder = inOrder(processor, interceptor);

      inOrder.verify(interceptor).before(any(), mapArgWithEntry("param", ""), any());
      inOrder.verify(interceptor).around(any(), mapArgWithEntry("param", ""), any(), any());
      inOrder.verify(processor, never()).process(any());
      inOrder.verify(interceptor).after(any(), argThat(interceptionHasPayloadValue(TEST_PAYLOAD)), eq(empty()));

      assertThat(result.getInternalParameters().entrySet(), hasSize(0));
      verifyParametersResolvedAndDisposed(times(1));
    }
  }

  @Test
  public void interceptorMutatesEventAroundAfterSkip() throws Exception {
    ProcessorInterceptor interceptor = prepareInterceptor(new ProcessorInterceptor() {

      @Override
      public CompletableFuture<InterceptionEvent> around(ComponentLocation location, Map<String, Object> parameters,
                                                         InterceptionEvent event, InterceptionAction action) {
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

    if (useMockInterceptor) {
      InOrder inOrder = inOrder(processor, interceptor);

      inOrder.verify(interceptor).before(any(), mapArgWithEntry("param", ""), any());
      inOrder.verify(interceptor).around(any(), mapArgWithEntry("param", ""), any(), any());
      inOrder.verify(processor, never()).process(any());
      inOrder.verify(interceptor).after(any(), argThat(interceptionHasPayloadValue(TEST_PAYLOAD)), eq(empty()));

      assertThat(result.getInternalParameters().entrySet(), hasSize(0));
      verifyParametersResolvedAndDisposed(times(1));
    }
  }

  @Test
  public void interceptorMutatesEventAroundAfterFailWithErrorType() throws Exception {
    ErrorType errorTypeMock = mock(ErrorType.class);
    ProcessorInterceptor interceptor = prepareInterceptor(new ProcessorInterceptor() {

      @Override
      public CompletableFuture<InterceptionEvent> around(ComponentLocation location, Map<String, Object> parameters,
                                                         InterceptionEvent event, InterceptionAction action) {
        event.message(Message.of(TEST_PAYLOAD));
        return action.fail(errorTypeMock);
      }
    });
    startFlowWithInterceptors(interceptor);

    expected.expect(MessagingException.class);
    expected.expect(withEventThat(hasErrorTypeThat(sameInstance(errorTypeMock))));
    expected.expectCause(instanceOf(InterceptionException.class));
    try {
      process(flow, eventBuilder().message(Message.of("")).build());
    } finally {
      if (useMockInterceptor) {
        InOrder inOrder = inOrder(processor, interceptor);

        inOrder.verify(interceptor).before(any(), mapArgWithEntry("param", ""), any());
        inOrder.verify(interceptor).around(any(), mapArgWithEntry("param", ""), any(), any());
        inOrder.verify(processor, never()).process(any());
        inOrder.verify(interceptor).after(any(), argThat(interceptionHasPayloadValue(TEST_PAYLOAD)),
                                          argThat(not(empty())));

        verifyParametersResolvedAndDisposed(times(1));
      }
    }
  }

  @Test
  public void interceptorMutatesEventAroundAfterFailWithCause() throws Exception {
    Throwable cause = new RuntimeException("");
    ProcessorInterceptor interceptor = prepareInterceptor(new ProcessorInterceptor() {

      @Override
      public CompletableFuture<InterceptionEvent> around(ComponentLocation location, Map<String, Object> parameters,
                                                         InterceptionEvent event, InterceptionAction action) {
        event.message(Message.of(TEST_PAYLOAD));
        return action.fail(cause);
      }
    });
    startFlowWithInterceptors(interceptor);

    expected.expectCause(sameInstance(cause));
    try {
      process(flow, eventBuilder().message(Message.of("")).build());
    } finally {
      if (useMockInterceptor) {
        InOrder inOrder = inOrder(processor, interceptor);

        inOrder.verify(interceptor).before(any(), mapArgWithEntry("param", ""), any());
        inOrder.verify(interceptor).around(any(), mapArgWithEntry("param", ""), any(), any());
        inOrder.verify(processor, never()).process(any());
        inOrder.verify(interceptor).after(any(), argThat(interceptionHasPayloadValue(TEST_PAYLOAD)), eq(of(cause)));

        verifyParametersResolvedAndDisposed(times(1));
      }
    }
  }

  @Test
  public void interceptorThrowsExceptionBefore() throws Exception {
    // TODO fix or create jira issue
    if (useMockInterceptor == true && processor instanceof OperationProcessorInApp) {
      return;
    }
    RuntimeException expectedException = new RuntimeException("Some Error");
    ProcessorInterceptor interceptor = prepareInterceptor(new ProcessorInterceptor() {

      @Override
      public void before(ComponentLocation location, Map<String, Object> parameters, InterceptionEvent event) {
        throw expectedException;
      }
    });
    startFlowWithInterceptors(interceptor);

    expected.expectCause(sameInstance(expectedException));
    try {
      process(flow, eventBuilder().message(Message.of("")).build());
    } finally {
      if (useMockInterceptor) {
        InOrder inOrder = inOrder(processor, interceptor);

        inOrder.verify(interceptor).before(any(), any(), any());
        inOrder.verify(interceptor, never()).around(any(), any(), any(), any());
        inOrder.verify(processor, never()).process(any());
        inOrder.verify(interceptor).after(any(), any(), eq(of(expectedException)));

        verifyParametersResolvedAndDisposed(times(1));
      }
    }
  }

  @Test
  public void interceptorThrowsExceptionAfter() throws Exception {
    RuntimeException expectedException = new RuntimeException("Some Error");
    ProcessorInterceptor interceptor = prepareInterceptor(new ProcessorInterceptor() {

      @Override
      public void after(ComponentLocation location, InterceptionEvent event, Optional<Throwable> thrown) {
        throw expectedException;
      }
    });
    startFlowWithInterceptors(interceptor);

    expected.expectCause(sameInstance(expectedException));
    try {
      process(flow, eventBuilder().message(Message.of("")).build());
    } finally {
      if (useMockInterceptor) {
        InOrder inOrder = inOrder(processor, interceptor);

        inOrder.verify(interceptor).before(any(), any(), any());
        inOrder.verify(interceptor).around(any(), any(), any(), any());
        inOrder.verify(processor).process(argThat(hasPayloadValue("")));
        inOrder.verify(interceptor).after(any(), any(), eq(empty()));

        verifyParametersResolvedAndDisposed(times(1));
      }
    }
  }

  @Test
  public void interceptorThrowsExceptionAround() throws Exception {
    RuntimeException expectedException = new RuntimeException("Some Error");
    ProcessorInterceptor interceptor = prepareInterceptor(new ProcessorInterceptor() {

      @Override
      public CompletableFuture<InterceptionEvent> around(ComponentLocation location, Map<String, Object> parameters,
                                                         InterceptionEvent event, InterceptionAction action) {
        throw expectedException;
      }
    });
    startFlowWithInterceptors(interceptor);

    expected.expectCause(sameInstance(expectedException));
    try {
      process(flow, eventBuilder().message(Message.of("")).build());
    } finally {
      if (useMockInterceptor) {
        InOrder inOrder = inOrder(processor, interceptor);

        inOrder.verify(interceptor).before(any(), any(), any());
        inOrder.verify(interceptor).around(any(), any(), any(), any());
        inOrder.verify(processor, never()).process(any());
        inOrder.verify(interceptor).after(any(), any(), eq(of(expectedException)));

        verifyParametersResolvedAndDisposed(times(1));
      }
    }
  }

  @Test
  public void interceptorThrowsExceptionAroundAfterProceed() throws Exception {
    RuntimeException expectedException = new RuntimeException("Some Error");
    ProcessorInterceptor interceptor = prepareInterceptor(new ProcessorInterceptor() {

      @Override
      public CompletableFuture<InterceptionEvent> around(ComponentLocation location, Map<String, Object> parameters,
                                                         InterceptionEvent event, InterceptionAction action) {
        action.proceed();
        throw expectedException;
      }
    });
    startFlowWithInterceptors(interceptor);

    expected.expectCause(sameInstance(expectedException));
    try {
      process(flow, eventBuilder().message(Message.of("")).build());
    } finally {
      if (useMockInterceptor) {
        InOrder inOrder = inOrder(processor, interceptor);

        inOrder.verify(interceptor).before(any(), any(), any());
        inOrder.verify(interceptor).around(any(), any(), any(), any());
        inOrder.verify(processor).process(any());
        inOrder.verify(interceptor).after(any(), any(), eq(of(expectedException)));

        verifyParametersResolvedAndDisposed(times(1));
      }
    }
  }

  @Test
  public void interceptorThrowsExceptionAroundAfterProceedInCallback() throws Exception {
    RuntimeException expectedException = new RuntimeException("Some Error");
    ProcessorInterceptor interceptor = prepareInterceptor(new ProcessorInterceptor() {

      @Override
      public CompletableFuture<InterceptionEvent> around(ComponentLocation location, Map<String, Object> parameters,
                                                         InterceptionEvent event, InterceptionAction action) {
        action.proceed();
        return supplyAsync(() -> {
          throw expectedException;
        });
      }
    });
    startFlowWithInterceptors(interceptor);

    expected.expect(MessagingException.class);
    expected.expect(withEventThat(hasErrorType(UNKNOWN.getNamespace(), UNKNOWN.getName())));
    expected.expectCause(sameInstance(expectedException));
    try {
      process(flow, eventBuilder().message(Message.of("")).build());
    } finally {
      if (useMockInterceptor) {
        InOrder inOrder = inOrder(processor, interceptor);

        inOrder.verify(interceptor).before(any(), any(), any());
        inOrder.verify(interceptor).around(any(), any(), any(), any());
        inOrder.verify(processor).process(any());
        inOrder.verify(interceptor).after(any(), any(), eq(of(expectedException)));

        verifyParametersResolvedAndDisposed(times(1));
      }
    }
  }

  @Test
  public void interceptorThrowsExceptionAroundAfterSkipInCallback() throws Exception {
    RuntimeException expectedException = new RuntimeException("Some Error");
    ProcessorInterceptor interceptor = prepareInterceptor(new ProcessorInterceptor() {

      @Override
      public CompletableFuture<InterceptionEvent> around(ComponentLocation location, Map<String, Object> parameters,
                                                         InterceptionEvent event, InterceptionAction action) {
        action.skip();
        return supplyAsync(() -> {
          throw expectedException;
        });
      }
    });
    startFlowWithInterceptors(interceptor);

    expected.expect(MessagingException.class);
    expected.expect(withEventThat(hasErrorType(UNKNOWN.getNamespace(), UNKNOWN.getName())));
    expected.expectCause(sameInstance(expectedException));
    try {
      process(flow, eventBuilder().message(Message.of("")).build());
    } finally {
      if (useMockInterceptor) {
        InOrder inOrder = inOrder(processor, interceptor);

        inOrder.verify(interceptor).before(any(), any(), any());
        inOrder.verify(interceptor).around(any(), any(), any(), any());
        inOrder.verify(processor, never()).process(any());
        inOrder.verify(interceptor).after(any(), any(), eq(of(expectedException)));

        verifyParametersResolvedAndDisposed(times(1));
      }
    }
  }

  @Test
  public void interceptedThrowsException() throws Exception {
    RuntimeException expectedException = new RuntimeException("Some Error");
    ProcessorInterceptor interceptor = prepareInterceptor(new ProcessorInterceptor() {});
    startFlowWithInterceptors(interceptor);

    when(processor.process(any())).thenThrow(expectedException);

    expected.expect(MessagingException.class);
    expected.expect(withEventThat(hasErrorType(UNKNOWN.getNamespace(), UNKNOWN.getName())));
    expected.expectCause(sameInstance(expectedException));
    try {
      process(flow, eventBuilder().message(Message.of("")).build());
    } finally {
      if (useMockInterceptor) {
        InOrder inOrder = inOrder(processor, interceptor);

        inOrder.verify(interceptor).before(any(), any(), any());
        inOrder.verify(interceptor).around(any(), any(), any(), any());
        inOrder.verify(processor).process(any());
        inOrder.verify(interceptor).after(any(), any(), eq(of(expectedException)));

        verifyParametersResolvedAndDisposed(times(1));
      }
    }
  }

  @Test
  public void interceptorSkipsProcessor() throws Exception {
    ProcessorInterceptor interceptor = prepareInterceptor(new ProcessorInterceptor() {

      @Override
      public CompletableFuture<InterceptionEvent> around(ComponentLocation location, Map<String, Object> parameters,
                                                         InterceptionEvent event, InterceptionAction action) {
        return action.skip();
      }
    });
    startFlowWithInterceptors(interceptor);

    Event result = process(flow, eventBuilder().message(Message.of("")).build());
    assertThat(result.getMessage().getPayload().getValue(), is(""));
    assertThat(result.getError().isPresent(), is(false));

    if (useMockInterceptor) {
      InOrder inOrder = inOrder(processor, interceptor);

      inOrder.verify(interceptor).before(any(), any(), any());
      inOrder.verify(interceptor).around(any(), any(), any(), any());
      inOrder.verify(processor, never()).process(any());
      inOrder.verify(interceptor).after(any(), any(), eq(empty()));

      assertThat(result.getInternalParameters().entrySet(), hasSize(0));
      verifyParametersResolvedAndDisposed(times(1));
    }
  }

  @Test
  public void firstInterceptorMutatesEventBefore() throws Exception {
    ProcessorInterceptor interceptor1 = prepareInterceptor(new ProcessorInterceptor() {

      @Override
      public void before(ComponentLocation location, Map<String, Object> parameters, InterceptionEvent event) {
        event.message(Message.of(TEST_PAYLOAD));
      }
    });
    ProcessorInterceptor interceptor2 = prepareInterceptor(new ProcessorInterceptor() {});
    startFlowWithInterceptors(interceptor1, interceptor2);

    Event result = process(flow, eventBuilder().message(Message.of("")).build());
    assertThat(result.getMessage().getPayload().getValue(), is(TEST_PAYLOAD));
    assertThat(result.getError().isPresent(), is(false));

    if (useMockInterceptor) {
      InOrder inOrder = inOrder(processor, interceptor1, interceptor2);

      inOrder.verify(interceptor1).before(any(), mapArgWithEntry("param", ""), any());
      inOrder.verify(interceptor1).around(any(), mapArgWithEntry("param", TEST_PAYLOAD),
                                          argThat(interceptionHasPayloadValue(TEST_PAYLOAD)), any());
      inOrder.verify(interceptor2).before(any(), mapArgWithEntry("param", TEST_PAYLOAD),
                                          argThat(interceptionHasPayloadValue(TEST_PAYLOAD)));
      inOrder.verify(interceptor2).around(any(), mapArgWithEntry("param", TEST_PAYLOAD),
                                          argThat(interceptionHasPayloadValue(TEST_PAYLOAD)), any());
      inOrder.verify(processor).process(argThat(hasPayloadValue(TEST_PAYLOAD)));
      inOrder.verify(interceptor2).after(any(), argThat(interceptionHasPayloadValue(TEST_PAYLOAD)), eq(empty()));
      inOrder.verify(interceptor1).after(any(), argThat(interceptionHasPayloadValue(TEST_PAYLOAD)), eq(empty()));

      assertThat(result.getInternalParameters().entrySet(), hasSize(0));
      verifyParametersResolvedAndDisposed(times(2));
    }
  }

  @Test
  public void secondInterceptorMutatesEventBefore() throws Exception {
    ProcessorInterceptor interceptor1 = prepareInterceptor(new ProcessorInterceptor() {});
    ProcessorInterceptor interceptor2 = prepareInterceptor(new ProcessorInterceptor() {

      @Override
      public void before(ComponentLocation location, Map<String, Object> parameters, InterceptionEvent event) {
        event.message(Message.of(TEST_PAYLOAD));
      }
    });
    startFlowWithInterceptors(interceptor1, interceptor2);

    Event result = process(flow, eventBuilder().message(Message.of("")).build());
    assertThat(result.getMessage().getPayload().getValue(), is(TEST_PAYLOAD));
    assertThat(result.getError().isPresent(), is(false));

    if (useMockInterceptor) {
      InOrder inOrder = inOrder(processor, interceptor1, interceptor2);

      inOrder.verify(interceptor1).before(any(), mapArgWithEntry("param", ""), argThat(interceptionHasPayloadValue("")));
      inOrder.verify(interceptor1).around(any(), mapArgWithEntry("param", ""), argThat(interceptionHasPayloadValue("")), any());
      inOrder.verify(interceptor2).before(any(), mapArgWithEntry("param", ""), any());
      inOrder.verify(interceptor2).around(any(), mapArgWithEntry("param", TEST_PAYLOAD),
                                          argThat(interceptionHasPayloadValue(TEST_PAYLOAD)), any());
      inOrder.verify(processor).process(argThat(hasPayloadValue(TEST_PAYLOAD)));
      inOrder.verify(interceptor2).after(any(), argThat(interceptionHasPayloadValue(TEST_PAYLOAD)), eq(empty()));
      inOrder.verify(interceptor1).after(any(), argThat(interceptionHasPayloadValue(TEST_PAYLOAD)), eq(empty()));

      assertThat(result.getInternalParameters().entrySet(), hasSize(0));
      verifyParametersResolvedAndDisposed(times(2));
    }
  }

  @Test
  public void firstInterceptorMutatesEventAfter() throws Exception {
    ProcessorInterceptor interceptor1 = prepareInterceptor(new ProcessorInterceptor() {

      @Override
      public void after(ComponentLocation location, InterceptionEvent event, Optional<Throwable> thrown) {
        event.message(Message.of(TEST_PAYLOAD));
      }
    });
    ProcessorInterceptor interceptor2 = prepareInterceptor(new ProcessorInterceptor() {});
    startFlowWithInterceptors(interceptor1, interceptor2);

    Event result = process(flow, eventBuilder().message(Message.of("")).build());
    assertThat(result.getMessage().getPayload().getValue(), is(TEST_PAYLOAD));
    assertThat(result.getError().isPresent(), is(false));

    if (useMockInterceptor) {
      InOrder inOrder = inOrder(processor, interceptor1, interceptor2);

      inOrder.verify(interceptor1).before(any(), any(), argThat(interceptionHasPayloadValue("")));
      inOrder.verify(interceptor1).around(any(), any(), argThat(interceptionHasPayloadValue("")), any());
      inOrder.verify(interceptor2).before(any(), any(), argThat(interceptionHasPayloadValue("")));
      inOrder.verify(interceptor2).around(any(), any(), argThat(interceptionHasPayloadValue("")), any());
      inOrder.verify(processor).process(argThat(hasPayloadValue("")));
      inOrder.verify(interceptor2).after(any(), argThat(interceptionHasPayloadValue("")), eq(empty()));
      inOrder.verify(interceptor1).after(any(), any(), eq(empty()));

      assertThat(result.getInternalParameters().entrySet(), hasSize(0));
      verifyParametersResolvedAndDisposed(times(1));
    }
  }

  @Test
  public void secondInterceptorMutatesEventAfter() throws Exception {
    ProcessorInterceptor interceptor1 = prepareInterceptor(new ProcessorInterceptor() {});
    ProcessorInterceptor interceptor2 = prepareInterceptor(new ProcessorInterceptor() {

      @Override
      public void after(ComponentLocation location, InterceptionEvent event, Optional<Throwable> thrown) {
        event.message(Message.of(TEST_PAYLOAD));
      }
    });
    startFlowWithInterceptors(interceptor1, interceptor2);

    Event result = process(flow, eventBuilder().message(Message.of("")).build());
    assertThat(result.getMessage().getPayload().getValue(), is(TEST_PAYLOAD));
    assertThat(result.getError().isPresent(), is(false));

    if (useMockInterceptor) {
      InOrder inOrder = inOrder(processor, interceptor1, interceptor2);

      inOrder.verify(interceptor1).before(any(), any(), argThat(interceptionHasPayloadValue("")));
      inOrder.verify(interceptor1).around(any(), any(), argThat(interceptionHasPayloadValue("")), any());
      inOrder.verify(interceptor2).before(any(), any(), argThat(interceptionHasPayloadValue("")));
      inOrder.verify(interceptor2).around(any(), any(), argThat(interceptionHasPayloadValue("")), any());
      inOrder.verify(processor).process(argThat(hasPayloadValue("")));
      inOrder.verify(interceptor2).after(any(), any(), eq(empty()));
      inOrder.verify(interceptor1).after(any(), argThat(interceptionHasPayloadValue(TEST_PAYLOAD)), eq(empty()));

      assertThat(result.getInternalParameters().entrySet(), hasSize(0));
      verifyParametersResolvedAndDisposed(times(1));
    }
  }

  @Test
  public void firstInterceptorMutatesEventAroundBeforeProceed() throws Exception {
    ProcessorInterceptor interceptor1 = prepareInterceptor(new ProcessorInterceptor() {

      @Override
      public CompletableFuture<InterceptionEvent> around(ComponentLocation location, Map<String, Object> parameters,
                                                         InterceptionEvent event, InterceptionAction action) {
        event.message(Message.of(TEST_PAYLOAD));
        return action.proceed();
      }
    });
    ProcessorInterceptor interceptor2 = prepareInterceptor(new ProcessorInterceptor() {});
    startFlowWithInterceptors(interceptor1, interceptor2);

    Event result = process(flow, eventBuilder().message(Message.of("")).build());
    assertThat(result.getMessage().getPayload().getValue(), is(TEST_PAYLOAD));
    assertThat(result.getError().isPresent(), is(false));

    if (useMockInterceptor) {
      InOrder inOrder = inOrder(processor, interceptor1, interceptor2);

      inOrder.verify(interceptor1).before(any(), mapArgWithEntry("param", ""), argThat(interceptionHasPayloadValue("")));
      inOrder.verify(interceptor1).around(any(), mapArgWithEntry("param", ""), any(), any());
      inOrder.verify(interceptor2).before(any(), mapArgWithEntry("param", TEST_PAYLOAD),
                                          argThat(interceptionHasPayloadValue(TEST_PAYLOAD)));
      inOrder.verify(interceptor2).around(any(), mapArgWithEntry("param", TEST_PAYLOAD),
                                          argThat(interceptionHasPayloadValue(TEST_PAYLOAD)), any());
      inOrder.verify(processor).process(argThat(hasPayloadValue(TEST_PAYLOAD)));
      inOrder.verify(interceptor2).after(any(), argThat(interceptionHasPayloadValue(TEST_PAYLOAD)), eq(empty()));
      inOrder.verify(interceptor1).after(any(), argThat(interceptionHasPayloadValue(TEST_PAYLOAD)), eq(empty()));

      assertThat(result.getInternalParameters().entrySet(), hasSize(0));
      verifyParametersResolvedAndDisposed(times(2));
    }
  }

  @Test
  public void secondInterceptorMutatesEventAroundBeforeProceed() throws Exception {
    ProcessorInterceptor interceptor1 = prepareInterceptor(new ProcessorInterceptor() {});
    ProcessorInterceptor interceptor2 = prepareInterceptor(new ProcessorInterceptor() {

      @Override
      public CompletableFuture<InterceptionEvent> around(ComponentLocation location, Map<String, Object> parameters,
                                                         InterceptionEvent event, InterceptionAction action) {
        event.message(Message.of(TEST_PAYLOAD));
        return action.proceed();
      }
    });
    startFlowWithInterceptors(interceptor1, interceptor2);

    Event result = process(flow, eventBuilder().message(Message.of("")).build());
    assertThat(result.getMessage().getPayload().getValue(), is(TEST_PAYLOAD));
    assertThat(result.getError().isPresent(), is(false));

    if (useMockInterceptor) {
      InOrder inOrder = inOrder(processor, interceptor1, interceptor2);

      inOrder.verify(interceptor1).before(any(), mapArgWithEntry("param", ""), argThat(interceptionHasPayloadValue("")));
      inOrder.verify(interceptor1).around(any(), mapArgWithEntry("param", ""), argThat(interceptionHasPayloadValue("")), any());
      inOrder.verify(interceptor2).before(any(), mapArgWithEntry("param", ""), argThat(interceptionHasPayloadValue("")));
      inOrder.verify(interceptor2).around(any(), mapArgWithEntry("param", ""), any(), any());
      inOrder.verify(processor).process(argThat(hasPayloadValue(TEST_PAYLOAD)));
      inOrder.verify(interceptor2).after(any(), argThat(interceptionHasPayloadValue(TEST_PAYLOAD)), eq(empty()));
      inOrder.verify(interceptor1).after(any(), argThat(interceptionHasPayloadValue(TEST_PAYLOAD)), eq(empty()));

      assertThat(result.getInternalParameters().entrySet(), hasSize(0));
      verifyParametersResolvedAndDisposed(times(1));
    }
  }

  @Test
  public void firstInterceptorMutatesEventAroundAfterProceed() throws Exception {
    // TODO fix or create jira issue
    if (useMockInterceptor == true && processor instanceof OperationProcessorInApp) {
      return;
    }
    ProcessorInterceptor interceptor1 = prepareInterceptor(new ProcessorInterceptor() {

      @Override
      public CompletableFuture<InterceptionEvent> around(ComponentLocation location, Map<String, Object> parameters,
                                                         InterceptionEvent event, InterceptionAction action) {
        action.proceed();
        return supplyAsync(() -> {
          event.message(Message.of(TEST_PAYLOAD));
          return event;
        });
      }
    });
    ProcessorInterceptor interceptor2 = prepareInterceptor(new ProcessorInterceptor() {});
    startFlowWithInterceptors(interceptor1, interceptor2);

    Event result = process(flow, eventBuilder().message(Message.of("")).build());
    assertThat(result.getMessage().getPayload().getValue(), is(TEST_PAYLOAD));
    assertThat(result.getError().isPresent(), is(false));

    if (useMockInterceptor) {
      InOrder inOrder = inOrder(processor, interceptor1, interceptor2);

      inOrder.verify(interceptor1).before(any(), mapArgWithEntry("param", ""), argThat(interceptionHasPayloadValue("")));
      inOrder.verify(interceptor1).around(any(), mapArgWithEntry("param", ""), any(), any());
      inOrder.verify(interceptor2).before(any(), mapArgWithEntry("param", ""), argThat(interceptionHasPayloadValue("")));
      inOrder.verify(interceptor2).around(any(), mapArgWithEntry("param", ""), argThat(interceptionHasPayloadValue("")), any());
      inOrder.verify(processor).process(argThat(hasPayloadValue("")));
      inOrder.verify(interceptor2).after(any(), argThat(interceptionHasPayloadValue("")), eq(empty()));
      inOrder.verify(interceptor1).after(any(), argThat(interceptionHasPayloadValue(TEST_PAYLOAD)), eq(empty()));

      assertThat(result.getInternalParameters().entrySet(), hasSize(0));
      verifyParametersResolvedAndDisposed(times(1));
    }
  }

  @Test
  public void secondInterceptorMutatesEventAroundAfterProceed() throws Exception {
    ProcessorInterceptor interceptor1 = prepareInterceptor(new ProcessorInterceptor() {});
    ProcessorInterceptor interceptor2 = prepareInterceptor(new ProcessorInterceptor() {

      @Override
      public CompletableFuture<InterceptionEvent> around(ComponentLocation location, Map<String, Object> parameters,
                                                         InterceptionEvent event, InterceptionAction action) {
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

    if (useMockInterceptor) {
      InOrder inOrder = inOrder(processor, interceptor1, interceptor2);

      inOrder.verify(interceptor1).before(any(), mapArgWithEntry("param", ""), argThat(interceptionHasPayloadValue("")));
      inOrder.verify(interceptor1).around(any(), mapArgWithEntry("param", ""), argThat(interceptionHasPayloadValue("")), any());
      inOrder.verify(interceptor2).before(any(), mapArgWithEntry("param", ""), argThat(interceptionHasPayloadValue("")));
      inOrder.verify(interceptor2).around(any(), mapArgWithEntry("param", ""), any(), any());
      inOrder.verify(processor).process(argThat(hasPayloadValue("")));
      inOrder.verify(interceptor2).after(any(), argThat(interceptionHasPayloadValue(TEST_PAYLOAD)), eq(empty()));
      inOrder.verify(interceptor1).after(any(), argThat(interceptionHasPayloadValue(TEST_PAYLOAD)), eq(empty()));

      assertThat(result.getInternalParameters().entrySet(), hasSize(0));
      verifyParametersResolvedAndDisposed(times(1));
    }
  }

  @Test
  public void firstInterceptorThrowsExceptionBefore() throws Exception {
    // TODO fix or create jira issue
    if (useMockInterceptor == true && processor instanceof OperationProcessorInApp) {
      return;
    }
    RuntimeException expectedException = new RuntimeException("Some Error");
    ProcessorInterceptor interceptor1 = prepareInterceptor(new ProcessorInterceptor() {

      @Override
      public void before(ComponentLocation location, Map<String, Object> parameters, InterceptionEvent event) {
        throw expectedException;
      }
    });
    ProcessorInterceptor interceptor2 = prepareInterceptor(new ProcessorInterceptor() {});
    startFlowWithInterceptors(interceptor1, interceptor2);

    expected.expectCause(sameInstance(expectedException));
    try {
      process(flow, eventBuilder().message(Message.of("")).build());
    } finally {
      if (useMockInterceptor) {
        InOrder inOrder = inOrder(processor, interceptor1, interceptor2);

        inOrder.verify(interceptor1).before(any(), any(), any());
        inOrder.verify(interceptor1, never()).around(any(), any(), any(), any());
        inOrder.verify(interceptor2, never()).before(any(), any(), any());
        inOrder.verify(interceptor2, never()).around(any(), any(), any(), any());
        inOrder.verify(processor, never()).process(any());
        inOrder.verify(interceptor2, never()).after(any(), any(), eq(of(expectedException)));
        inOrder.verify(interceptor1).after(any(), any(), eq(of(expectedException)));

        verifyParametersResolvedAndDisposed(times(1));
      }
    }
  }

  @Test
  public void secondInterceptorThrowsExceptionBefore() throws Exception {
    // TODO fix or create jira issue
    if (useMockInterceptor == true && processor instanceof OperationProcessorInApp) {
      return;
    }
    RuntimeException expectedException = new RuntimeException("Some Error");
    ProcessorInterceptor interceptor1 = prepareInterceptor(new ProcessorInterceptor() {});
    ProcessorInterceptor interceptor2 = prepareInterceptor(new ProcessorInterceptor() {

      @Override
      public void before(ComponentLocation location, Map<String, Object> parameters, InterceptionEvent event) {
        throw expectedException;
      }
    });
    startFlowWithInterceptors(interceptor1, interceptor2);

    expected.expectCause(sameInstance(expectedException));
    try {
      process(flow, eventBuilder().message(Message.of("")).build());
    } finally {
      if (useMockInterceptor) {
        InOrder inOrder = inOrder(processor, interceptor1, interceptor2);

        inOrder.verify(interceptor1).before(any(), any(), any());
        inOrder.verify(interceptor1).around(any(), any(), any(), any());
        inOrder.verify(interceptor2).before(any(), any(), any());
        inOrder.verify(interceptor2, never()).around(any(), any(), any(), any());
        inOrder.verify(processor, never()).process(any());
        inOrder.verify(interceptor2).after(any(), any(), eq(of(expectedException)));
        inOrder.verify(interceptor1).after(any(), any(), eq(of(expectedException)));

        verifyParametersResolvedAndDisposed(times(1));
      }
    }
  }

  @Test
  public void firstInterceptorThrowsExceptionAfter() throws Exception {
    RuntimeException expectedException = new RuntimeException("Some Error");
    ProcessorInterceptor interceptor1 = prepareInterceptor(new ProcessorInterceptor() {

      @Override
      public void after(ComponentLocation location, InterceptionEvent event, Optional<Throwable> thrown) {
        throw expectedException;
      }
    });
    ProcessorInterceptor interceptor2 = prepareInterceptor(new ProcessorInterceptor() {});
    startFlowWithInterceptors(interceptor1, interceptor2);

    expected.expectCause(sameInstance(expectedException));
    try {
      process(flow, eventBuilder().message(Message.of("")).build());
    } finally {
      if (useMockInterceptor) {
        InOrder inOrder = inOrder(processor, interceptor1, interceptor2);

        inOrder.verify(interceptor1).before(any(), any(), any());
        inOrder.verify(interceptor1).around(any(), any(), any(), any());
        inOrder.verify(interceptor2).before(any(), any(), any());
        inOrder.verify(interceptor2).around(any(), any(), any(), any());
        inOrder.verify(processor).process(any());
        inOrder.verify(interceptor2).after(any(), any(), eq(empty()));
        inOrder.verify(interceptor1).after(any(), any(), eq(empty()));

        verifyParametersResolvedAndDisposed(times(1));
      }
    }
  }

  @Test
  public void secondInterceptorThrowsExceptionAfter() throws Exception {
    // TODO fix or create jira issue
    if (useMockInterceptor == true && processor instanceof OperationProcessorInApp) {
      return;
    }
    RuntimeException expectedException = new RuntimeException("Some Error");
    ProcessorInterceptor interceptor1 = prepareInterceptor(new ProcessorInterceptor() {});
    ProcessorInterceptor interceptor2 = prepareInterceptor(new ProcessorInterceptor() {

      @Override
      public void after(ComponentLocation location, InterceptionEvent event, Optional<Throwable> thrown) {
        throw expectedException;
      }
    });
    startFlowWithInterceptors(interceptor1, interceptor2);

    expected.expectCause(sameInstance(expectedException));
    try {
      process(flow, eventBuilder().message(Message.of("")).build());
    } finally {
      if (useMockInterceptor) {
        InOrder inOrder = inOrder(processor, interceptor1, interceptor2);

        inOrder.verify(interceptor1).before(any(), any(), any());
        inOrder.verify(interceptor1).around(any(), any(), any(), any());
        inOrder.verify(interceptor2).before(any(), any(), any());
        inOrder.verify(interceptor2).around(any(), any(), any(), any());
        inOrder.verify(processor).process(any());
        inOrder.verify(interceptor2).after(any(), any(), eq(empty()));
        inOrder.verify(interceptor1).after(any(), any(), eq(of(expectedException)));

        verifyParametersResolvedAndDisposed(times(1));
      }
    }
  }

  @Test
  public void firstInterceptorThrowsExceptionAround() throws Exception {
    RuntimeException expectedException = new RuntimeException("Some Error");
    ProcessorInterceptor interceptor1 = prepareInterceptor(new ProcessorInterceptor() {

      @Override
      public CompletableFuture<InterceptionEvent> around(ComponentLocation location, Map<String, Object> parameters,
                                                         InterceptionEvent event, InterceptionAction action) {
        throw expectedException;
      }
    });
    ProcessorInterceptor interceptor2 = prepareInterceptor(new ProcessorInterceptor() {});
    startFlowWithInterceptors(interceptor1, interceptor2);

    expected.expectCause(sameInstance(expectedException));
    try {
      process(flow, eventBuilder().message(Message.of("")).build());
    } finally {
      if (useMockInterceptor) {
        InOrder inOrder = inOrder(processor, interceptor1, interceptor2);

        inOrder.verify(interceptor1).before(any(), any(), any());
        inOrder.verify(interceptor1).around(any(), any(), any(), any());
        inOrder.verify(interceptor2, never()).before(any(), any(), any());
        inOrder.verify(interceptor2, never()).around(any(), any(), any(), any());
        inOrder.verify(processor, never()).process(any());
        inOrder.verify(interceptor2, never()).after(any(), any(), eq(of(expectedException)));
        inOrder.verify(interceptor1).after(any(), any(), eq(of(expectedException)));

        verifyParametersResolvedAndDisposed(times(1));
      }
    }
  }

  @Test
  public void secondInterceptorThrowsExceptionAround() throws Exception {
    // TODO fix or create jira issue
    if (useMockInterceptor == true && processor instanceof OperationProcessorInApp) {
      return;
    }
    RuntimeException expectedException = new RuntimeException("Some Error");
    ProcessorInterceptor interceptor1 = prepareInterceptor(new ProcessorInterceptor() {});
    ProcessorInterceptor interceptor2 = prepareInterceptor(new ProcessorInterceptor() {

      @Override
      public CompletableFuture<InterceptionEvent> around(ComponentLocation location, Map<String, Object> parameters,
                                                         InterceptionEvent event, InterceptionAction action) {
        throw expectedException;
      }
    });
    startFlowWithInterceptors(interceptor1, interceptor2);

    expected.expectCause(sameInstance(expectedException));
    try {
      process(flow, eventBuilder().message(Message.of("")).build());
    } finally {
      if (useMockInterceptor) {
        InOrder inOrder = inOrder(processor, interceptor1, interceptor2);

        inOrder.verify(interceptor1).before(any(), any(), any());
        inOrder.verify(interceptor1).around(any(), any(), any(), any());
        inOrder.verify(interceptor2).before(any(), any(), any());
        inOrder.verify(interceptor2).around(any(), any(), any(), any());
        inOrder.verify(processor, never()).process(any());
        inOrder.verify(interceptor2).after(any(), any(), eq(of(expectedException)));
        inOrder.verify(interceptor1).after(any(), any(), eq(of(expectedException)));

        verifyParametersResolvedAndDisposed(times(1));
      }
    }
  }

  @Test
  public void firstInterceptorThrowsExceptionAroundAfterProceed() throws Exception {
    // TODO fix or create jira issue
    if (useMockInterceptor == true && processor instanceof OperationProcessorInApp) {
      return;
    }
    RuntimeException expectedException = new RuntimeException("Some Error");
    ProcessorInterceptor interceptor1 = prepareInterceptor(new ProcessorInterceptor() {

      @Override
      public CompletableFuture<InterceptionEvent> around(ComponentLocation location, Map<String, Object> parameters,
                                                         InterceptionEvent event, InterceptionAction action) {
        action.proceed();
        throw expectedException;
      }
    });
    ProcessorInterceptor interceptor2 = prepareInterceptor(new ProcessorInterceptor() {});
    startFlowWithInterceptors(interceptor1, interceptor2);

    expected.expectCause(sameInstance(expectedException));
    try {
      process(flow, eventBuilder().message(Message.of("")).build());
    } finally {
      if (useMockInterceptor) {
        InOrder inOrder = inOrder(processor, interceptor1, interceptor2);

        inOrder.verify(interceptor1).before(any(), any(), any());
        inOrder.verify(interceptor1).around(any(), any(), any(), any());
        inOrder.verify(interceptor2).before(any(), any(), any());
        inOrder.verify(interceptor2).around(any(), any(), any(), any());
        inOrder.verify(processor).process(any());
        inOrder.verify(interceptor2).after(any(), any(), eq(empty()));
        inOrder.verify(interceptor1).after(any(), any(), eq(of(expectedException)));

        verifyParametersResolvedAndDisposed(times(1));
      }
    }
  }

  @Test
  public void secondInterceptorThrowsExceptionAroundAfterProceed() throws Exception {
    // TODO fix or create jira issue
    if (useMockInterceptor == true && processor instanceof OperationProcessorInApp) {
      return;
    }
    RuntimeException expectedException = new RuntimeException("Some Error");
    ProcessorInterceptor interceptor1 = prepareInterceptor(new ProcessorInterceptor() {});
    ProcessorInterceptor interceptor2 = prepareInterceptor(new ProcessorInterceptor() {

      @Override
      public CompletableFuture<InterceptionEvent> around(ComponentLocation location, Map<String, Object> parameters,
                                                         InterceptionEvent event, InterceptionAction action) {
        action.proceed();
        throw expectedException;
      }
    });
    startFlowWithInterceptors(interceptor1, interceptor2);

    expected.expectCause(sameInstance(expectedException));
    try {
      process(flow, eventBuilder().message(Message.of("")).build());
    } finally {
      if (useMockInterceptor) {
        InOrder inOrder = inOrder(processor, interceptor1, interceptor2);

        inOrder.verify(interceptor1).before(any(), any(), any());
        inOrder.verify(interceptor1).around(any(), any(), any(), any());
        inOrder.verify(interceptor2).before(any(), any(), any());
        inOrder.verify(interceptor2).around(any(), any(), any(), any());
        inOrder.verify(processor).process(any());
        inOrder.verify(interceptor2).after(any(), any(), eq(of(expectedException)));
        inOrder.verify(interceptor1).after(any(), any(), eq(of(expectedException)));

        verifyParametersResolvedAndDisposed(times(1));
      }
    }
  }

  @Test
  public void firstInterceptorSkipsProcessor() throws Exception {
    ProcessorInterceptor interceptor1 = prepareInterceptor(new ProcessorInterceptor() {

      @Override
      public CompletableFuture<InterceptionEvent> around(ComponentLocation location, Map<String, Object> parameters,
                                                         InterceptionEvent event, InterceptionAction action) {
        return action.skip();
      }
    });
    ProcessorInterceptor interceptor2 = prepareInterceptor(new ProcessorInterceptor() {});
    startFlowWithInterceptors(interceptor1, interceptor2);

    Event result = process(flow, eventBuilder().message(Message.of("")).build());
    assertThat(result.getMessage().getPayload().getValue(), is(""));

    if (useMockInterceptor) {
      InOrder inOrder = inOrder(processor, interceptor1, interceptor2);

      inOrder.verify(interceptor1).before(any(), any(), any());
      inOrder.verify(interceptor1).around(any(), any(), any(), any());
      inOrder.verify(interceptor2, never()).before(any(), any(), any());
      inOrder.verify(interceptor2, never()).around(any(), any(), any(), any());
      inOrder.verify(processor, never()).process(any());
      inOrder.verify(interceptor2, never()).after(any(), any(), eq(empty()));
      inOrder.verify(interceptor1).after(any(), argThat(interceptionHasPayloadValue("")), eq(empty()));

      assertThat(result.getInternalParameters().entrySet(), hasSize(0));
      verifyParametersResolvedAndDisposed(times(1));
    }
  }

  @Test
  public void secondInterceptorSkipsProcessor() throws Exception {
    ProcessorInterceptor interceptor1 = prepareInterceptor(new ProcessorInterceptor() {});
    ProcessorInterceptor interceptor2 = prepareInterceptor(new ProcessorInterceptor() {

      @Override
      public CompletableFuture<InterceptionEvent> around(ComponentLocation location, Map<String, Object> parameters,
                                                         InterceptionEvent event, InterceptionAction action) {
        return action.skip();
      }
    });
    startFlowWithInterceptors(interceptor1, interceptor2);

    Event result = process(flow, eventBuilder().message(Message.of("")).build());
    assertThat(result.getMessage().getPayload().getValue(), is(""));

    if (useMockInterceptor) {
      InOrder inOrder = inOrder(processor, interceptor1, interceptor2);

      inOrder.verify(interceptor1).before(any(), any(), any());
      inOrder.verify(interceptor1).around(any(), any(), any(), any());
      inOrder.verify(interceptor2).before(any(), any(), any());
      inOrder.verify(interceptor2).around(any(), any(), any(), any());
      inOrder.verify(processor, never()).process(any());
      inOrder.verify(interceptor2).after(any(), any(), eq(empty()));
      inOrder.verify(interceptor1).after(any(), any(), eq(empty()));

      assertThat(result.getInternalParameters().entrySet(), hasSize(0));
      verifyParametersResolvedAndDisposed(times(1));
    }
  }

  @Test
  public void firstInterceptorDoesntApply() throws Exception {
    ProcessorInterceptor interceptor1 = prepareInterceptor(new ProcessorInterceptor() {});
    ProcessorInterceptor interceptor2 = prepareInterceptor(new ProcessorInterceptor() {});
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

    if (useMockInterceptor) {
      InOrder inOrder = inOrder(processor, interceptor1, interceptor2);

      inOrder.verify(interceptor1, never()).before(any(), any(), any());
      inOrder.verify(interceptor1, never()).around(any(), any(), any(), any());
      inOrder.verify(interceptor2).before(any(), any(), any());
      inOrder.verify(interceptor2).around(any(), any(), any(), any());
      inOrder.verify(processor).process(any());
      inOrder.verify(interceptor2).after(any(), any(), eq(empty()));
      inOrder.verify(interceptor1, never()).after(any(), any(), eq(empty()));

      assertThat(result.getInternalParameters().entrySet(), hasSize(0));
      verifyParametersResolvedAndDisposed(times(1));
    }
  }

  @Test
  public void secondInterceptorDoesntApply() throws Exception {
    ProcessorInterceptor interceptor1 = prepareInterceptor(new ProcessorInterceptor() {});
    ProcessorInterceptor interceptor2 = prepareInterceptor(new ProcessorInterceptor() {});
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

    if (useMockInterceptor) {
      InOrder inOrder = inOrder(processor, interceptor1, interceptor2);

      inOrder.verify(interceptor1).before(any(), any(), any());
      inOrder.verify(interceptor1).around(any(), any(), any(), any());
      inOrder.verify(interceptor2, never()).before(any(), any(), any());
      inOrder.verify(interceptor2, never()).around(any(), any(), any(), any());
      inOrder.verify(processor).process(any());
      inOrder.verify(interceptor2, never()).after(any(), any(), eq(empty()));
      inOrder.verify(interceptor1).after(any(), any(), eq(empty()));

      assertThat(result.getInternalParameters().entrySet(), hasSize(0));
      verifyParametersResolvedAndDisposed(times(1));
    }
  }

  private void verifyParametersResolvedAndDisposed(final VerificationMode times) {
    if (processor instanceof OperationProcessorInApp) {
      verify((OperationProcessorInApp) processor, times).resolveParameters(any());
      verify((OperationProcessorInApp) processor, times).disposeResolvedParameters(any());
    }
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
          .put(LOCATION_KEY, buildLocation("test:processor"))
          .build());
    }

    @Override
    public Event process(Event event) throws MuleException {
      return event;
    }
  }

  private static class OperationProcessorInApp extends AbstractAnnotatedObject
      implements ParametersResolverProcessor, Processor {

    private final ExecutionContext executionContext = mock(ExecutionContext.class);

    public OperationProcessorInApp() {
      setAnnotations(ImmutableMap.<QName, Object>builder()
          .put(ANNOTATION_PARAMETERS, singletonMap("param", "#[payload]"))
          .put(LOCATION_KEY, buildLocation("test:operationProcessor"))
          .build());
    }

    @Override
    public Event process(Event event) throws MuleException {



      return event;
    }

    @Override
    public ParametersResolverProcessorResult resolveParameters(Event event) {
      return new ParametersResolverProcessorResult(singletonMap("operationParam", "operationParamValue"),
                                                   executionContext);
    }

    @Override
    public void disposeResolvedParameters(ExecutionContext<OperationModel> executionContext) {
      assertThat(executionContext, sameInstance(this.executionContext));
    }
  }

  private static DefaultComponentLocation buildLocation(final String componentIdentifier) {
    final TypedComponentIdentifier part =
        builder().identifier(buildFromStringRepresentation(componentIdentifier)).type(PROCESSOR).build();
    return new DefaultComponentLocation(of("flowName"), singletonList(new DefaultLocationPart("0", of(part), empty(), empty())));
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
