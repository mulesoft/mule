/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.operation;

import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.core.api.event.EventContextFactory.create;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.disposeIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.startIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.stopIfNeeded;
import static org.mule.runtime.core.api.rx.Exceptions.unwrap;
import static org.mule.runtime.core.internal.policy.DefaultPolicyManager.noPolicyOperation;
import static org.mule.test.allure.AllureConstants.ExecutionEngineFeature.EXECUTION_ENGINE;
import static org.mule.test.allure.AllureConstants.ExecutionEngineFeature.ExecutionEngineStory.REACTOR;

import static java.util.Optional.of;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;
import static reactor.core.publisher.Mono.from;
import static reactor.core.publisher.Mono.just;

import org.mule.runtime.api.exception.DefaultMuleException;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.meta.model.ComponentModel;
import org.mule.runtime.api.meta.model.EnrichableModel;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.XmlDslModel;
import org.mule.runtime.core.api.construct.Flow;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.expression.ExpressionRuntimeException;
import org.mule.runtime.core.api.extension.ExtensionManager;
import org.mule.runtime.core.internal.exception.MessagingException;
import org.mule.runtime.core.internal.message.InternalEvent;
import org.mule.runtime.core.internal.policy.PolicyManager;
import org.mule.runtime.extension.api.runtime.config.ConfigurationProvider;
import org.mule.runtime.metadata.api.cache.MetadataCacheIdGeneratorFactory;
import org.mule.runtime.module.extension.api.loader.java.property.CompletableComponentExecutorModelProperty;
import org.mule.runtime.module.extension.internal.runtime.TestComponentMessageProcessor;
import org.mule.runtime.module.extension.internal.runtime.resolver.ResolverSet;
import org.mule.runtime.module.extension.internal.runtime.resolver.ResolverSetResult;
import org.mule.runtime.module.extension.internal.runtime.resolver.RouteBuilderValueResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.ValueResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.ValueResolvingContext;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import java.util.HashMap;
import java.util.Map;

import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Story;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;

@Feature(EXECUTION_ENGINE)
@Story(REACTOR)
public class ComponentMessageProcessorTestCase extends AbstractMuleContextTestCase {

  private static final Logger LOGGER = LoggerFactory.getLogger(ComponentMessageProcessorTestCase.class);
  @Rule
  public ExpectedException expected = ExpectedException.none();
  protected ComponentMessageProcessor<ComponentModel> processor;
  protected ExtensionModel extensionModel;
  protected ComponentModel componentModel;
  protected ResolverSet resolverSet;
  protected ExtensionManager extensionManager;
  protected PolicyManager mockPolicyManager;

  // A cached flow for creating test events. It doesn't even have to contain the processor we are going to test, because we will
  // be sending the events directly through the processor, without using the flow.
  private Flow testFlow;

  @Before
  public void before() throws MuleException {
    extensionModel = mock(ExtensionModel.class);
    when(extensionModel.getXmlDslModel()).thenReturn(XmlDslModel.builder().setPrefix("mock").build());

    componentModel = mock(ComponentModel.class, withSettings().extraInterfaces(EnrichableModel.class));
    when((componentModel).getModelProperty(CompletableComponentExecutorModelProperty.class))
        .thenReturn(of(new CompletableComponentExecutorModelProperty(IdentityExecutor::create)));
    resolverSet = mock(ResolverSet.class);

    extensionManager = mock(ExtensionManager.class);
    mockPolicyManager = mock(PolicyManager.class);
    when(mockPolicyManager.createOperationPolicy(any(), any(), any())).thenReturn(noPolicyOperation());

    testFlow = getTestFlow(muleContext);
    initialiseIfNeeded(testFlow, muleContext);

    processor = createProcessor();
    processor.setAnnotations(getAppleFlowComponentLocationAnnotations());
    processor.setComponentLocator(componentLocator);
    processor.setCacheIdGeneratorFactory(of(mock(MetadataCacheIdGeneratorFactory.class)));
    processor.setMuleConfiguration(muleContext.getConfiguration());

    initialiseIfNeeded(processor, muleContext);

    // Since the initialization of the message processor reassigns its inner resolver set, we need to set the spy back.
    processor.resolverSet = resolverSet;

    startIfNeeded(processor);
  }

  @After
  public void after() throws MuleException {
    stopIfNeeded(processor);
    disposeIfNeeded(processor, LOGGER);

    stopIfNeeded(testFlow);
    disposeIfNeeded(testFlow, LOGGER);
  }

  protected ComponentMessageProcessor<ComponentModel> createProcessor() {
    return new TestComponentMessageProcessor(extensionModel,
                                             componentModel, null, null, null,
                                             resolverSet, null, null, null,
                                             null, extensionManager,
                                             mockPolicyManager, null, null,
                                             muleContext.getConfiguration().getShutdownTimeout()) {

      @Override
      protected void validateOperationConfiguration(ConfigurationProvider configurationProvider) {}

      @Override
      public ProcessingType getInnerProcessingType() {
        return ProcessingType.CPU_LITE;
      }
    };
  }

  @Override
  protected CoreEvent.Builder getEventBuilder() {
    // Overrides to avoid creating a new test flow for each new test event
    return InternalEvent.builder(create(testFlow, TEST_CONNECTOR_LOCATION));
  }

  @Test
  public void happyPath() throws MuleException {
    final ResolverSetResult resolverSetResult = mock(ResolverSetResult.class);

    when(resolverSet.resolve(any(ValueResolvingContext.class))).thenReturn(resolverSetResult);

    assertNotNull(from(processor.apply(just(testEvent()))).block());
  }

  @Test
  public void muleRuntimeExceptionInResolutionResult() throws MuleException {
    final Exception thrown = new ExpressionRuntimeException(createStaticMessage("Expected"));

    when(resolverSet.resolve(any(ValueResolvingContext.class))).thenThrow(thrown);

    expectWrapped(thrown);
    from(processor.apply(just(testEvent()))).block();
  }

  @Test
  public void muleExceptionInResolutionResult() throws MuleException {
    final Exception thrown = new DefaultMuleException(createStaticMessage("Expected"));

    when(resolverSet.resolve(any(ValueResolvingContext.class))).thenThrow(thrown);

    expectWrapped(thrown);
    from(processor.apply(just(testEvent()))).block();
  }

  @Test
  public void runtimeExceptionInResolutionResult() throws MuleException {
    final Exception thrown = new NullPointerException("Expected");

    when(resolverSet.resolve(any(ValueResolvingContext.class))).thenThrow(thrown);

    expectWrapped(thrown);
    from(processor.apply(just(testEvent()))).block();
  }

  @Test
  public void messageProcessorLifecycleIsPropagatedToRouteChains() throws MuleException {
    clearInvocations(resolverSet);

    Map<String, ValueResolver<?>> valueResolvers = new HashMap<>();
    RouteBuilderValueResolver routeBuilderValueResolver = mock(RouteBuilderValueResolver.class);
    valueResolvers.put("resolver", routeBuilderValueResolver);
    when(resolverSet.getResolvers()).thenReturn(valueResolvers);

    processor.stop();
    verify(routeBuilderValueResolver, times(1)).stop();

    processor.dispose();
    verify(routeBuilderValueResolver, times(1)).dispose();

    // initialization of the route chains is done within the initialization of the whole value resolvers set
    processor.initialise();
    verify(resolverSet, times(1)).initialise();

    processor.start();
    verify(routeBuilderValueResolver, times(1)).start();
  }

  @Test
  public void happyPathFluxPublisher() throws MuleException, InterruptedException {
    final ResolverSetResult resolverSetResult = mock(ResolverSetResult.class);
    when(resolverSet.resolve(any(ValueResolvingContext.class))).thenReturn(resolverSetResult);

    subscribeToParallelPublisherAndAwait(3);
  }

  @Test
  public void multipleUpstreamPublishers() throws MuleException, InterruptedException {
    final ResolverSetResult resolverSetResult = mock(ResolverSetResult.class);
    when(resolverSet.resolve(any(ValueResolvingContext.class))).thenReturn(resolverSetResult);

    InfiniteEmitter<CoreEvent> eventsEmitter = new InfiniteEmitter<>(this::newEvent);
    InfiniteEmitter<CoreEvent> eventsEmitter2 = new InfiniteEmitter<>(this::newEvent);
    ItemsConsumer<CoreEvent> eventsConsumer = new ItemsConsumer<>(10);
    ItemsConsumer<CoreEvent> eventsConsumer2 = new ItemsConsumer<>(3);

    Flux.create(eventsEmitter)
        .transform(processor)
        .subscribe(eventsConsumer);

    Flux.create(eventsEmitter2)
        .transform(processor)
        .subscribe(eventsConsumer2);

    eventsEmitter.start();
    eventsEmitter2.start();
    try {
      assertThat(eventsConsumer.await(RECEIVE_TIMEOUT, MILLISECONDS), is(true));
      assertThat(eventsConsumer2.await(RECEIVE_TIMEOUT, MILLISECONDS), is(true));
    } finally {
      eventsEmitter.stop();
      eventsEmitter2.stop();
    }
  }

  @Test
  @Issue("W-13563214")
  public void newSubscriptionAfterPreviousPublisherTermination() throws MuleException, InterruptedException {
    final ResolverSetResult resolverSetResult = mock(ResolverSetResult.class);
    when(resolverSet.resolve(any(ValueResolvingContext.class))).thenReturn(resolverSetResult);

    subscribeToParallelPublisherAndAwait(5);
    subscribeToParallelPublisherAndAwait(4);
  }

  private void expectWrapped(Exception expect) {
    expected.expect(new BaseMatcher<Exception>() {

      @Override
      public boolean matches(Object o) {
        Exception e = (Exception) unwrap((Exception) o);
        assertThat(e, is(instanceOf(MessagingException.class)));
        assertThat(e.getCause(), is(sameInstance(expect)));

        return true;
      }

      @Override
      public void describeTo(Description description) {
        description.appendText("condition not met");
      }
    });
  }

  private void subscribeToParallelPublisherAndAwait(int numEvents) throws InterruptedException {
    InfiniteEmitter<CoreEvent> eventsEmitter = new InfiniteEmitter<>(this::newEvent);
    ItemsConsumer<CoreEvent> eventsConsumer = new ItemsConsumer<>(numEvents);

    Flux.create(eventsEmitter)
        .transform(processor)
        .doOnNext(Assert::assertNotNull)
        .subscribe(eventsConsumer);

    eventsEmitter.start();
    try {
      assertThat(eventsConsumer.await(RECEIVE_TIMEOUT, MILLISECONDS), is(true));
    } finally {
      eventsEmitter.stop();
    }
  }

}
