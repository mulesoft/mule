/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime;

import static java.lang.Thread.currentThread;
import static java.util.Optional.of;
import static java.util.concurrent.Executors.newFixedThreadPool;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.disposeIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.startIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.stopIfNeeded;
import static org.mule.tck.probe.PollingProber.probe;
import static reactor.core.scheduler.Schedulers.fromExecutorService;

import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.component.location.Location;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.meta.model.ComponentModel;
import org.mule.runtime.api.meta.model.EnrichableModel;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.XmlDslModel;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.extension.ExtensionManager;
import org.mule.runtime.core.api.processor.ReactiveProcessor;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategy;
import org.mule.runtime.core.internal.metadata.cache.MetadataCacheIdGeneratorFactory;
import org.mule.runtime.core.internal.policy.OperationParametersProcessor;
import org.mule.runtime.core.internal.policy.OperationPolicy;
import org.mule.runtime.core.internal.policy.PolicyManager;
import org.mule.runtime.extension.api.runtime.config.ConfigurationProvider;
import org.mule.runtime.extension.api.runtime.operation.CompletableComponentExecutor;
import org.mule.runtime.extension.api.runtime.operation.CompletableComponentExecutor.ExecutorCallback;
import org.mule.runtime.module.extension.api.loader.java.property.CompletableComponentExecutorModelProperty;
import org.mule.runtime.module.extension.api.runtime.privileged.ExecutionContextAdapter;
import org.mule.runtime.module.extension.internal.runtime.operation.ComponentMessageProcessor;
import org.mule.runtime.module.extension.internal.runtime.operation.ExecutionMediator;
import org.mule.runtime.module.extension.internal.runtime.resolver.ResolverSet;
import org.mule.runtime.module.extension.internal.runtime.resolver.ResolverSetResult;
import org.mule.runtime.module.extension.internal.runtime.resolver.ValueResolvingContext;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reactor.core.publisher.Mono;

public class ComponentMessageProcessorPolicyProcessingStrategyTestCase extends AbstractMuleContextTestCase {

  private static final Logger LOGGER = LoggerFactory.getLogger(ComponentMessageProcessorPolicyProcessingStrategyTestCase.class);

  private static final OperationPolicy NO_POLICY_OPERATION =
      (operationEvent, operationExecutionFunction, opParamProcessor, componentLocation, callback) -> operationExecutionFunction
          .execute(opParamProcessor.getOperationParameters(), operationEvent, callback);

  private ComponentMessageProcessor<ComponentModel> processor;
  private Location mpRootContainerLocation;
  private FlowConstruct rootContainer;
  private ProcessingStrategy processingStrategy;

  private ExtensionModel extensionModel;
  private ComponentModel componentModel;

  private ResolverSet resolverSet;
  private ExtensionManager extensionManager;
  private PolicyManager policyManager;

  private final ExecutorService threadSwitcher = newFixedThreadPool(2);
  private final AssertingExecutionMediator mediator = new AssertingExecutionMediator();

  @Before
  public void before() throws MuleException {
    CoreEvent response = testEvent();

    mpRootContainerLocation = mock(Location.class);

    extensionModel = mock(ExtensionModel.class);
    when(extensionModel.getXmlDslModel()).thenReturn(XmlDslModel.builder().setPrefix("mock").build());

    componentModel = mock(ComponentModel.class, withSettings().extraInterfaces(EnrichableModel.class));
    when((componentModel).getModelProperty(CompletableComponentExecutorModelProperty.class))
        .thenReturn(of(new CompletableComponentExecutorModelProperty((cp, p) -> (ctx, callback) -> callback.complete(response))));
    resolverSet = mock(ResolverSet.class);
    when(resolverSet.resolve(any(ValueResolvingContext.class))).thenReturn(mock(ResolverSetResult.class));

    extensionManager = mock(ExtensionManager.class);
    policyManager = mock(PolicyManager.class);

    processor = new ComponentMessageProcessor<ComponentModel>(extensionModel,
                                                              componentModel, null, null, null,
                                                              resolverSet, null, null,
                                                              extensionManager,
                                                              policyManager, null) {

      @Override
      protected void validateOperationConfiguration(ConfigurationProvider configurationProvider) {}

      @Override
      public ProcessingType getInnerProcessingType() {
        return ProcessingType.CPU_LITE;
      }

      @Override
      protected ExecutionMediator createExecutionMediator() {
        return mediator;
      }

      @Override
      public ComponentLocation getLocation() {
        return mock(ComponentLocation.class);
      }

      @Override
      public Location getRootContainerLocation() {
        return mpRootContainerLocation;
      }

    };

    rootContainer = mock(FlowConstruct.class);
    processingStrategy = mock(ProcessingStrategy.class);
    when(componentLocator.find(mpRootContainerLocation)).thenReturn(of(rootContainer));
    when(rootContainer.getProcessingStrategy()).thenReturn(processingStrategy);

    processor.setComponentLocator(componentLocator);
    processor.setCacheIdGeneratorFactory(mock(MetadataCacheIdGeneratorFactory.class));

    initialiseIfNeeded(processor, muleContext);
    startIfNeeded(processor);
  }

  @After
  public void after() throws MuleException {
    stopIfNeeded(processor);
    disposeIfNeeded(processor, LOGGER);
    threadSwitcher.shutdownNow();
  }

  private static class AssertingExecutionMediator implements ExecutionMediator {

    private Thread executionThread;
    private boolean executed;

    @Override
    public void execute(CompletableComponentExecutor executor,
                        ExecutionContextAdapter context,
                        ExecutorCallback callback) {
      executed = true;
      executionThread = currentThread();
      callback.complete("");
    }
  }

  @Test
  public void noOpPolicy() throws MuleException {
    when(policyManager.createOperationPolicy(eq(processor), any(CoreEvent.class), any(OperationParametersProcessor.class)))
        .thenReturn(NO_POLICY_OPERATION);

    processor.process(testEvent());
    assertThat(mediator.executed, is(true));
    assertThat(mediator.executionThread, sameInstance(currentThread()));
  }

  @Test
  public void policyChangesThreadBefore() throws MuleException {
    AtomicReference<Thread> switchedRef = new AtomicReference<>();

    when(policyManager.createOperationPolicy(eq(processor), any(CoreEvent.class), any(OperationParametersProcessor.class)))
        .thenReturn((operationEvent, operationExecutionFunction, opParamProcessor, componentLocation, callback) -> {

          threadSwitcher.execute(() -> {
            switchedRef.set(currentThread());
            operationExecutionFunction.execute(opParamProcessor.getOperationParameters(), operationEvent, callback);
          });

        });

    processor.process(testEvent());

    probe(() -> {
      assertThat(mediator.executed, is(true));
      assertThat(mediator.executionThread, sameInstance(switchedRef.get()));
      return true;
    });
  }

  @Test
  public void policyChangesThreadAfter() throws MuleException {
    when(policyManager.createOperationPolicy(eq(processor), any(CoreEvent.class), any(OperationParametersProcessor.class)))
        .thenReturn((operationEvent, operationExecutionFunction, opParamProcessor, componentLocation, callback) -> {
          operationExecutionFunction.execute(opParamProcessor.getOperationParameters(), operationEvent, new ExecutorCallback() {

            @Override
            public void error(Throwable e) {
              threadSwitcher.execute(() -> {
                callback.error(e);
              });
            }

            @Override
            public void complete(Object value) {
              threadSwitcher.execute(() -> {
                callback.complete(value);
              });
            }
          });
        });

    processor.process(testEvent());

    probe(() -> {
      assertThat(mediator.executed, is(true));
      assertThat(mediator.executionThread, sameInstance(currentThread()));
      return true;
    });
  }

  @Test
  public void processingStrategyChangesThreadBefore() throws MuleException {
    AtomicReference<Thread> switchedRef = new AtomicReference<>();

    when(processingStrategy.onProcessor(any(ReactiveProcessor.class)))
        .thenAnswer(inv -> {
          return (ReactiveProcessor) t -> Mono.from(t)
              .publishOn(fromExecutorService(threadSwitcher))
              .doOnNext(e -> switchedRef.set(currentThread()))
              .transform(inv.getArgument(0, ReactiveProcessor.class));
        });

    stopIfNeeded(processor);
    disposeIfNeeded(processor, LOGGER);
    initialiseIfNeeded(processor, muleContext);
    startIfNeeded(processor);

    when(policyManager.createOperationPolicy(eq(processor), any(CoreEvent.class), any(OperationParametersProcessor.class)))
        .thenReturn((operationEvent, operationExecutionFunction, opParamProcessor, componentLocation, callback) -> {
          operationExecutionFunction.execute(opParamProcessor.getOperationParameters(), operationEvent, callback);
        });

    processor.process(testEvent());
    probe(() -> {
      assertThat(mediator.executed, is(true));
      assertThat(mediator.executionThread, sameInstance(switchedRef.get()));
      return true;
    });
  }

  @Test
  public void processingStrategyChangesThreadAfter() throws MuleException {
    when(processingStrategy.onProcessor(any(ReactiveProcessor.class)))
        .thenAnswer(inv -> {
          return (ReactiveProcessor) t -> Mono.from(t)
              .transform(inv.getArgument(0, ReactiveProcessor.class))
              .publishOn(fromExecutorService(threadSwitcher));
        });

    stopIfNeeded(processor);
    disposeIfNeeded(processor, LOGGER);
    initialiseIfNeeded(processor, muleContext);
    startIfNeeded(processor);

    when(policyManager.createOperationPolicy(eq(processor), any(CoreEvent.class), any(OperationParametersProcessor.class)))
        .thenReturn((operationEvent, operationExecutionFunction, opParamProcessor, componentLocation, callback) -> {
          operationExecutionFunction.execute(opParamProcessor.getOperationParameters(), operationEvent, callback);
        });

    processor.process(testEvent());
    probe(() -> {
      assertThat(mediator.executed, is(true));
      assertThat(mediator.executionThread, sameInstance(currentThread()));
      return true;
    });
  }
}
