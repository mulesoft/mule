/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.operation;

import static org.mule.runtime.api.component.AbstractComponent.ANNOTATION_NAME;
import static org.mule.runtime.api.component.AbstractComponent.LOCATION_KEY;
import static org.mule.runtime.api.component.AbstractComponent.ROOT_CONTAINER_NAME_KEY;
import static org.mule.runtime.api.component.ComponentIdentifier.buildFromStringRepresentation;
import static org.mule.runtime.api.component.TypedComponentIdentifier.ComponentType.FLOW;
import static org.mule.runtime.api.config.FeatureFlaggingService.FEATURE_FLAGGING_SERVICE_KEY;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.disposeIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.startIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.stopIfNeeded;
import static org.mule.runtime.core.api.processor.ReactiveProcessor.ProcessingType.CPU_INTENSIVE;
import static org.mule.runtime.core.api.processor.ReactiveProcessor.ProcessingType.CPU_LITE;
import static org.mule.runtime.core.api.processor.ReactiveProcessor.ProcessingType.CPU_LITE_ASYNC;
import static org.mule.runtime.core.internal.policy.DefaultPolicyManager.noPolicyOperation;
import static org.mule.runtime.core.privileged.processor.MessageProcessors.processToApply;
import static org.mule.tck.MuleTestUtils.APPLE_FLOW;
import static org.mule.tck.probe.PollingProber.probe;

import static java.lang.Thread.currentThread;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyMap;
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
import static reactor.core.scheduler.Schedulers.fromExecutorService;

import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.component.TypedComponentIdentifier;
import org.mule.runtime.api.component.location.Location;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.meta.model.ComponentModel;
import org.mule.runtime.api.meta.model.EnrichableModel;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.XmlDslModel;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.ConfigurationBuilder;
import org.mule.runtime.core.api.config.builders.AbstractConfigurationBuilder;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.extension.ExtensionManager;
import org.mule.runtime.core.api.processor.ReactiveProcessor;
import org.mule.runtime.core.api.processor.ReactiveProcessor.ProcessingType;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategy;
import org.mule.runtime.core.internal.policy.OperationParametersProcessor;
import org.mule.runtime.core.internal.policy.PolicyManager;
import org.mule.runtime.dsl.api.component.config.DefaultComponentLocation;
import org.mule.runtime.dsl.api.component.config.DefaultComponentLocation.DefaultLocationPart;
import org.mule.runtime.extension.api.runtime.config.ConfigurationProvider;
import org.mule.runtime.extension.api.runtime.operation.CompletableComponentExecutor;
import org.mule.runtime.extension.api.runtime.operation.CompletableComponentExecutor.ExecutorCallback;
import org.mule.runtime.feature.internal.config.DefaultFeatureFlaggingService;
import org.mule.runtime.metadata.api.cache.MetadataCacheIdGeneratorFactory;
import org.mule.runtime.module.extension.api.loader.java.property.CompletableComponentExecutorModelProperty;
import org.mule.runtime.module.extension.api.runtime.privileged.ExecutionContextAdapter;
import org.mule.runtime.module.extension.api.runtime.resolver.ResolverSet;
import org.mule.runtime.module.extension.api.runtime.resolver.ResolverSetResult;
import org.mule.runtime.module.extension.api.runtime.resolver.ValueResolvingContext;
import org.mule.runtime.module.extension.internal.runtime.TestComponentMessageProcessor;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicReference;

import javax.xml.namespace.QName;

import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import reactor.core.publisher.Mono;

@RunWith(Parameterized.class)
public class ComponentMessageProcessorPolicyProcessingStrategyTestCase extends AbstractMuleContextTestCase {

  private static final Logger LOGGER = LoggerFactory.getLogger(ComponentMessageProcessorPolicyProcessingStrategyTestCase.class);
  public static final String TEST_ARTIFACT_ID = "artifactId";

  @Parameters(name = "async: {0}, processingType: {1}")
  public static List<Object[]> parameters() {
    return asList(new Object[] {true, CPU_LITE_ASYNC},
                  new Object[] {false, CPU_INTENSIVE},
                  new Object[] {true, CPU_INTENSIVE});
  }

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

  private final boolean async;
  private final ProcessingType processingType;

  public ComponentMessageProcessorPolicyProcessingStrategyTestCase(boolean async, ProcessingType processingType) {
    this.async = async;
    this.processingType = processingType;
  }

  @Override
  protected void addBuilders(List<ConfigurationBuilder> builders) {
    super.addBuilders(builders);
    builders.add(new AbstractConfigurationBuilder() {

      @Override
      protected void doConfigure(MuleContext muleContext) throws Exception {
        muleContext.getCustomizationService().overrideDefaultServiceImpl(FEATURE_FLAGGING_SERVICE_KEY,
                                                                         new DefaultFeatureFlaggingService(TEST_ARTIFACT_ID,
                                                                                                           emptyMap()));
      }
    });
  }

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

    processor = new TestComponentMessageProcessor(extensionModel,
                                                  componentModel, null, null, null,
                                                  resolverSet, null, null, null,
                                                  null, extensionManager,
                                                  policyManager, null, null,
                                                  muleContext.getConfiguration().getShutdownTimeout()) {

      @Override
      protected void validateOperationConfiguration(ConfigurationProvider configurationProvider) {}

      @Override
      public ProcessingType getInnerProcessingType() {
        return processingType;
      }

      @Override
      protected ExecutionMediator createExecutionMediator() {
        return mediator;
      }

      @Override
      public Location getRootContainerLocation() {
        return mpRootContainerLocation;
      }

      @Override
      protected boolean requiresConfig() {
        return async;
      }

    };

    rootContainer = mock(FlowConstruct.class);
    processingStrategy = mock(ProcessingStrategy.class);
    when(processingStrategy.onProcessor(any(ReactiveProcessor.class))).thenAnswer(inv -> inv.getArgument(0));
    when(processingStrategy.configureInternalPublisher(any(Publisher.class))).thenAnswer(inv -> inv.getArgument(0));

    when(componentLocator.find(mpRootContainerLocation)).thenReturn(of(rootContainer));
    when(rootContainer.getProcessingStrategy()).thenReturn(processingStrategy);

    processor.setComponentLocator(componentLocator);
    processor.setCacheIdGeneratorFactory(of(mock(MetadataCacheIdGeneratorFactory.class)));

    final Map<QName, Object> annotations = new HashMap<>();
    final ComponentIdentifier opIdentifier = buildFromStringRepresentation("ns:op");
    annotations.put(LOCATION_KEY, new DefaultComponentLocation(of(APPLE_FLOW),
                                                               asList(new DefaultLocationPart(APPLE_FLOW,
                                                                                              of(TypedComponentIdentifier
                                                                                                  .builder()
                                                                                                  .identifier(opIdentifier)
                                                                                                  .type(FLOW).build()),
                                                                                              Optional.empty(),
                                                                                              OptionalInt.empty(),
                                                                                              OptionalInt.empty()))));
    annotations.put(ROOT_CONTAINER_NAME_KEY, APPLE_FLOW);
    annotations.put(ANNOTATION_NAME, opIdentifier);
    processor.setAnnotations(annotations);

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
  public void policyChangesThreadBefore() throws MuleException {
    AtomicReference<Thread> switchedRef = new AtomicReference<>();

    when(policyManager.createOperationPolicy(eq(processor), any(CoreEvent.class), any(OperationParametersProcessor.class)))
        .thenReturn((operationEvent, operationExecutionFunction, opParamProcessor, componentLocation, callback) -> {

          threadSwitcher.execute(() -> {
            switchedRef.set(currentThread());
            operationExecutionFunction.execute(opParamProcessor.getOperationParameters(), operationEvent, callback);
          });

        });

    processToApply(testEvent(), processingStrategy.onProcessor(processor));

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

    processToApply(testEvent(), processingStrategy.onProcessor(processor));

    probe(() -> {
      assertThat(mediator.executed, is(true));
      assertThat(mediator.executionThread, sameInstance(currentThread()));
      return true;
    });
  }

  @Test
  public void noPolicyProcessingStrategyChangesThreadBefore() throws MuleException {
    AtomicReference<Thread> switchedRef = new AtomicReference<>();

    when(processingStrategy.onProcessor(any(ReactiveProcessor.class)))
        .thenAnswer(inv -> {
          final ProcessingType processingType = inv.getArgument(0, ReactiveProcessor.class).getProcessingType();
          if (processingType.equals(CPU_LITE_ASYNC) || processingType.equals(CPU_LITE)) {
            return (ReactiveProcessor) t -> Mono.from(t)
                .transform(inv.getArgument(0, ReactiveProcessor.class));
          } else {
            return (ReactiveProcessor) t -> Mono.from(t)
                .publishOn(fromExecutorService(threadSwitcher))
                .doOnNext(e -> switchedRef.set(currentThread()))
                .transform(inv.getArgument(0, ReactiveProcessor.class));
          }
        });

    stopIfNeeded(processor);
    disposeIfNeeded(processor, LOGGER);
    initialiseIfNeeded(processor, muleContext);
    startIfNeeded(processor);

    when(policyManager.createOperationPolicy(eq(processor), any(CoreEvent.class), any(OperationParametersProcessor.class)))
        .thenReturn(noPolicyOperation());

    processToApply(testEvent(), processingStrategy.onProcessor(processor));
    probe(() -> {
      assertThat(mediator.executed, is(true));
      assertThat(mediator.executionThread, processingType.equals(CPU_LITE_ASYNC)
          ? sameInstance(currentThread())
          : sameInstance(switchedRef.get()));
      return true;
    });
  }

  @Test
  public void noPolicyProcessingStrategyChangesThreadAfter() throws MuleException {
    when(processingStrategy.onProcessor(any(ReactiveProcessor.class)))
        .thenAnswer(inv -> {
          final ProcessingType processingType = inv.getArgument(0, ReactiveProcessor.class).getProcessingType();
          if (processingType.equals(CPU_LITE_ASYNC)) {
            return (ReactiveProcessor) t -> Mono.from(t)
                .transform(inv.getArgument(0, ReactiveProcessor.class))
                .publishOn(fromExecutorService(threadSwitcher));
          } else {
            return (ReactiveProcessor) t -> Mono.from(t)
                .transform(inv.getArgument(0, ReactiveProcessor.class));
          }
        });

    stopIfNeeded(processor);
    disposeIfNeeded(processor, LOGGER);
    initialiseIfNeeded(processor, muleContext);
    startIfNeeded(processor);

    when(policyManager.createOperationPolicy(eq(processor), any(CoreEvent.class), any(OperationParametersProcessor.class)))
        .thenReturn(noPolicyOperation());

    processToApply(testEvent(), processingStrategy.onProcessor(processor));
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
          final ProcessingType processingType = inv.getArgument(0, ReactiveProcessor.class).getProcessingType();
          if (processingType.equals(CPU_LITE_ASYNC) || processingType.equals(CPU_LITE)) {
            return (ReactiveProcessor) t -> Mono.from(t)
                .transform(inv.getArgument(0, ReactiveProcessor.class));
          } else {
            return (ReactiveProcessor) t -> Mono.from(t)
                .publishOn(fromExecutorService(threadSwitcher))
                .doOnNext(e -> switchedRef.set(currentThread()))
                .transform(inv.getArgument(0, ReactiveProcessor.class));
          }
        });

    stopIfNeeded(processor);
    disposeIfNeeded(processor, LOGGER);
    initialiseIfNeeded(processor, muleContext);
    startIfNeeded(processor);

    when(policyManager.createOperationPolicy(eq(processor), any(CoreEvent.class), any(OperationParametersProcessor.class)))
        .thenReturn((operationEvent, operationExecutionFunction, opParamProcessor, componentLocation, callback) -> {
          operationExecutionFunction.execute(opParamProcessor.getOperationParameters(), operationEvent, callback);
        });

    processToApply(testEvent(), processingStrategy.onProcessor(processor));
    probe(() -> {
      assertThat(mediator.executed, is(true));
      assertThat(mediator.executionThread, processingType.equals(CPU_LITE_ASYNC)
          ? sameInstance(currentThread())
          : sameInstance(switchedRef.get()));
      return true;
    });
  }

  @Test
  public void processingStrategyChangesThreadAfter() throws MuleException {
    when(processingStrategy.onProcessor(any(ReactiveProcessor.class)))
        .thenAnswer(inv -> {
          final ProcessingType processingType = inv.getArgument(0, ReactiveProcessor.class).getProcessingType();
          if (processingType.equals(CPU_LITE_ASYNC)) {
            return (ReactiveProcessor) t -> Mono.from(t)
                .transform(inv.getArgument(0, ReactiveProcessor.class))
                .publishOn(fromExecutorService(threadSwitcher));
          } else {
            return (ReactiveProcessor) t -> Mono.from(t)
                .transform(inv.getArgument(0, ReactiveProcessor.class));
          }
        });

    stopIfNeeded(processor);
    disposeIfNeeded(processor, LOGGER);
    initialiseIfNeeded(processor, muleContext);
    startIfNeeded(processor);

    when(policyManager.createOperationPolicy(eq(processor), any(CoreEvent.class), any(OperationParametersProcessor.class)))
        .thenReturn((operationEvent, operationExecutionFunction, opParamProcessor, componentLocation, callback) -> {
          operationExecutionFunction.execute(opParamProcessor.getOperationParameters(), operationEvent, callback);
        });

    processToApply(testEvent(), processingStrategy.onProcessor(processor));
    probe(() -> {
      assertThat(mediator.executed, is(true));
      assertThat(mediator.executionThread, sameInstance(currentThread()));
      return true;
    });
  }

}
