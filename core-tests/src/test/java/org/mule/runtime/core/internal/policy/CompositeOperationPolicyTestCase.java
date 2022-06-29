/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.policy;

import static org.mule.runtime.core.api.rx.Exceptions.rxExceptionToMuleException;
import static org.mule.runtime.dsl.api.component.config.DefaultComponentLocation.fromSingleComponent;

import static java.lang.Runtime.getRuntime;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Optional.of;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.rules.ExpectedException.none;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static reactor.core.publisher.Mono.error;
import static reactor.core.publisher.Mono.just;

import org.mule.runtime.api.component.Component;
import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.scheduler.Scheduler;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.policy.OperationPolicyParametersTransformer;
import org.mule.runtime.core.api.policy.Policy;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.processor.ReactiveProcessor;
import org.mule.runtime.core.internal.exception.MessagingException;
import org.mule.runtime.core.internal.execution.SourcePolicyTestUtils;
import org.mule.runtime.extension.api.runtime.operation.CompletableComponentExecutor.ExecutorCallback;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runners.Parameterized;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

public class CompositeOperationPolicyTestCase extends AbstractCompositePolicyTestCase {

  @Rule
  public ExpectedException expectedException = none();

  private CompositeOperationPolicy compositeOperationPolicy;

  private final Component operation = mock(Component.class, RETURNS_DEEP_STUBS);

  private final Optional<OperationPolicyParametersTransformer> operationPolicyParametersTransformer =
      of(mock(OperationPolicyParametersTransformer.class, RETURNS_DEEP_STUBS));
  private final OperationParametersProcessor operationParametersProcessor = mock(OperationParametersProcessor.class);

  private ComponentLocation operationLocation;
  private CoreEvent initialEvent;
  private final OperationExecutionFunction operationExecutionFunction = mock(OperationExecutionFunction.class);
  private CoreEvent nextProcessResultEvent;

  private final OperationPolicyProcessorFactory operationPolicyProcessorFactory = mock(OperationPolicyProcessorFactory.class);

  private Scheduler fluxCompleteScheduler;

  private final boolean featureFlagsEnabled;

  public CompositeOperationPolicyTestCase(boolean policyChangeThread, boolean processChangeThread, boolean featureFlagsEnabled) {
    super(policyChangeThread, processChangeThread);
    this.featureFlagsEnabled = featureFlagsEnabled;
  }

  @Parameterized.Parameters(name = "Policy NB: {0}; Processor NB: {1}; Apply Feature flags: {2}")
  public static Collection<Object[]> data() {
    return asList(new Object[][] {
        {false, false, true},
        {false, false, false},
        {true, false, true},
        {true, false, false},
        {false, true, true},
        {false, true, false},
        {true, true, true},
        {true, true, false}
    });
  }

  @Before
  public void setUp() throws Exception {
    operationLocation = fromSingleComponent("flow/processors/0");

    initialEvent = createTestEvent();
    nextProcessResultEvent = CoreEvent.builder(createTestEvent()).message(Message.of("HELLO")).build();
    when(operationPolicyParametersTransformer.get().fromParametersToMessage(any())).thenReturn(Message.of(null));
    doAnswer(invocation -> {

      ExecutorCallback callback = invocation.getArgument(2);

      Mono<CoreEvent> mono = Mono.from(processor().apply(just(invocation.getArgument(1))));
      if (processChangeThread || policyChangeThread) {
        mono.publishOn(Schedulers.single())
            .subscribe(callback::complete, callback::error);
      } else {
        try {
          callback.complete(mono.block());
        } catch (Throwable t) {
          callback.error(t);
        }
      }

      return null;
    }).when(operationExecutionFunction).execute(any(), any(), any());

    when(operationPolicyProcessorFactory.createOperationPolicy(same(firstPolicy), any())).thenAnswer(
                                                                                                     policyFactoryInvocation -> firstPolicyProcessor(policyFactoryInvocation,
                                                                                                                                                     e -> e,
                                                                                                                                                     e -> e));
    when(operationPolicyProcessorFactory.createOperationPolicy(same(secondPolicy), any())).thenAnswer(
                                                                                                      policyFactoryInvocation -> secondPolicyProcessor(policyFactoryInvocation,
                                                                                                                                                       e -> e,
                                                                                                                                                       e -> e));
    this.fluxCompleteScheduler = muleContext.getSchedulerService().ioScheduler();
  }

  @After
  public void tearDown() {
    this.fluxCompleteScheduler.stop();
  }

  protected ReactiveProcessor processor() {
    return eventPub -> {
      Flux<CoreEvent> baseFlux = Flux.from(eventPub);
      if (processChangeThread) {
        baseFlux = baseFlux.publishOn(Schedulers.single());
      }
      return baseFlux
          .map(e -> CoreEvent.builder(e).message(nextProcessResultEvent.getMessage()).build());
    };
  }

  @Test
  public void singlePolicy() throws Throwable {
    compositeOperationPolicy = new CompositeOperationPolicy(operation, asList(firstPolicy),
                                                            operationPolicyParametersTransformer,
                                                            operationPolicyProcessorFactory,
                                                            muleContext.getConfiguration().getShutdownTimeout(),
                                                            fluxCompleteScheduler, feature -> featureFlagsEnabled);

    CoreEvent result = block(callback -> compositeOperationPolicy
        .process(initialEvent, operationExecutionFunction, operationParametersProcessor, operationLocation, callback));

    assertThat(result.getMessage(), is(nextProcessResultEvent.getMessage()));
    verify(operationExecutionFunction).execute(any(), any(), any());
    verify(operationPolicyProcessorFactory, atLeastOnce()).createOperationPolicy(same(firstPolicy), any());

    assertThat(getFirstPolicyActualResultEvent(), not(nullValue()));
  }

  @Test
  public void compositePolicy() throws Throwable {
    compositeOperationPolicy = new CompositeOperationPolicy(operation, asList(firstPolicy, secondPolicy),
                                                            operationPolicyParametersTransformer,
                                                            operationPolicyProcessorFactory,
                                                            muleContext.getConfiguration().getShutdownTimeout(),
                                                            fluxCompleteScheduler, feature -> featureFlagsEnabled);

    CoreEvent result =
        block(callback -> compositeOperationPolicy.process(initialEvent, operationExecutionFunction, operationParametersProcessor,
                                                           operationLocation, callback));
    assertThat(result.getMessage(), is(nextProcessResultEvent.getMessage()));
    verify(operationExecutionFunction).execute(any(), any(), any());
    verify(operationPolicyProcessorFactory, atLeastOnce()).createOperationPolicy(same(firstPolicy), any());
    verify(operationPolicyProcessorFactory, atLeastOnce()).createOperationPolicy(same(secondPolicy), any());

    assertThat(getFirstPolicyActualResultEvent(), not(nullValue()));
    assertThat(getSecondPolicyActualResultEvent(), not(nullValue()));
  }

  @Test(expected = IllegalArgumentException.class)
  public void emptyPolicyList() {
    compositeOperationPolicy = new CompositeOperationPolicy(operation, emptyList(),
                                                            operationPolicyParametersTransformer,
                                                            operationPolicyProcessorFactory,
                                                            muleContext.getConfiguration().getShutdownTimeout(),
                                                            fluxCompleteScheduler, feature -> featureFlagsEnabled);
  }

  @Test
  public void policyExecutionFailurePropagates() throws Exception {
    RuntimeException policyException = new RuntimeException("policy failure");
    when(operationPolicyProcessorFactory.createOperationPolicy(same(firstPolicy), any())).thenAnswer(policyFactoryInvocation -> {
      Processor firstPolicyOperationPolicyProcessor = mock(Processor.class);
      when(firstPolicyOperationPolicyProcessor.apply(any()))
          .thenAnswer(inv -> {
            Flux<CoreEvent> baseFlux = Flux.from(inv.getArgument(0));
            if (policyChangeThread) {
              baseFlux = baseFlux.publishOn(Schedulers.single());
            }
            return baseFlux.flatMap(event -> error(new MessagingException(event, policyException)));
          });
      return firstPolicyOperationPolicyProcessor;
    });
    compositeOperationPolicy = new CompositeOperationPolicy(operation, asList(firstPolicy, secondPolicy),
                                                            operationPolicyParametersTransformer,
                                                            operationPolicyProcessorFactory,
                                                            muleContext.getConfiguration().getShutdownTimeout(),
                                                            fluxCompleteScheduler, feature -> featureFlagsEnabled);
    expectedException.expect(MuleException.class);
    expectedException.expectCause(is(policyException));
    try {
      block(callback -> compositeOperationPolicy.process(initialEvent, operationExecutionFunction, operationParametersProcessor,
                                                         operationLocation, callback));
    } catch (Throwable throwable) {
      throw rxExceptionToMuleException(throwable);
    }
  }

  @Test
  public void nextProcessorExecutionFailurePropagates() throws Exception {
    RuntimeException policyException = new RuntimeException("policy failure");
    doAnswer(inv -> {
      CoreEvent event = inv.getArgument(1);
      Mono<CoreEvent> baseMono = just(event);
      ExecutorCallback callback = inv.getArgument(2);

      if (processChangeThread) {
        baseMono
            .doOnNext(ev -> callback.error(new MessagingException(ev, policyException)))
            .publishOn(Schedulers.single())
            .subscribe();
      } else {
        callback.error(new MessagingException(event, policyException));
      }

      return null;
    }).when(operationExecutionFunction).execute(any(), any(), any());

    compositeOperationPolicy = new CompositeOperationPolicy(operation, asList(firstPolicy, secondPolicy),
                                                            operationPolicyParametersTransformer,
                                                            operationPolicyProcessorFactory,
                                                            muleContext.getConfiguration().getShutdownTimeout(),
                                                            fluxCompleteScheduler, feature -> featureFlagsEnabled);
    expectedException.expect(MuleException.class);
    expectedException.expectCause(is(policyException));
    try {
      block(callback -> compositeOperationPolicy.process(initialEvent, operationExecutionFunction,
                                                         operationParametersProcessor,
                                                         operationLocation, callback));
    } catch (Throwable throwable) {
      throw rxExceptionToMuleException(throwable);
    }
  }

  @Test
  public void reactorPipelinesReused() throws Throwable {
    InvocationsRecordingCompositeOperationPolicy.reset();
    final InvocationsRecordingCompositeOperationPolicy operationPolicy =
        new InvocationsRecordingCompositeOperationPolicy(operation, asList(firstPolicy),
                                                         operationPolicyParametersTransformer,
                                                         operationPolicyProcessorFactory,
                                                         fluxCompleteScheduler,
                                                         featureFlagsEnabled);

    assertThat(operationPolicy.getNextOperationCount(), is(0));
    assertThat(operationPolicy.getPolicyCount(), is(0));

    for (int i = 0; i < getRuntime().availableProcessors() * 2; ++i) {
      block(callback -> operationPolicy
          .process(initialEvent, operationExecutionFunction, operationParametersProcessor, operationLocation, callback));
    }

    assertThat(operationPolicy.getNextOperationCount(), is(getRuntime().availableProcessors()));
    assertThat(operationPolicy.getPolicyCount(), is(getRuntime().availableProcessors()));
  }

  public static final class InvocationsRecordingCompositeOperationPolicy extends CompositeOperationPolicy {

    private static final AtomicInteger nextOperation = new AtomicInteger();
    private static final AtomicInteger policy = new AtomicInteger();

    public InvocationsRecordingCompositeOperationPolicy(Component operation, List<Policy> parameterizedPolicies,
                                                        Optional<OperationPolicyParametersTransformer> operationPolicyParametersTransformer,
                                                        OperationPolicyProcessorFactory operationPolicyProcessorFactory,
                                                        Scheduler completionCallbackScheduler,
                                                        boolean applyFeatureFlags) {
      super(operation, parameterizedPolicies, operationPolicyParametersTransformer, operationPolicyProcessorFactory,
            muleContext.getConfiguration().getShutdownTimeout(),
            completionCallbackScheduler, feature -> applyFeatureFlags);
    }

    public static void reset() {
      nextOperation.set(0);
      policy.set(0);
    }

    @Override
    protected Publisher<CoreEvent> applyNextOperation(Publisher<CoreEvent> eventPub) {
      nextOperation.incrementAndGet();
      return super.applyNextOperation(eventPub);
    }

    @Override
    protected Publisher<CoreEvent> applyPolicy(Policy policy, ReactiveProcessor nextProcessor, Publisher<CoreEvent> eventPub) {
      InvocationsRecordingCompositeOperationPolicy.policy.incrementAndGet();
      return super.applyPolicy(policy, nextProcessor, eventPub);
    }

    public int getNextOperationCount() {
      return nextOperation.get();
    }

    public int getPolicyCount() {
      return policy.get();
    }
  }

  private CoreEvent block(Consumer<ExecutorCallback> consumer) throws Throwable {
    return SourcePolicyTestUtils.block(callback -> consumer.accept(new ExecutorCallback() {

      @Override
      public void complete(Object value) {
        callback.complete((CoreEvent) value);
      }

      @Override
      public void error(Throwable e) {
        callback.error(e);
      }
    }));
  }
}
