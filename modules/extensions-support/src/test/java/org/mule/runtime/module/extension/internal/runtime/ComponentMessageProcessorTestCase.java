/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime;

import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.disposeIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.startIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.stopIfNeeded;
import static org.mule.runtime.core.api.rx.Exceptions.unwrap;
import static org.mule.runtime.core.internal.policy.DefaultPolicyManager.noPolicyOperation;
import static org.mule.test.allure.AllureConstants.ExecutionEngineFeature.EXECUTION_ENGINE;

import static java.util.Optional.of;

import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
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
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.expression.ExpressionRuntimeException;
import org.mule.runtime.core.api.extension.ExtensionManager;
import org.mule.runtime.core.internal.exception.MessagingException;
import org.mule.runtime.core.internal.metadata.cache.MetadataCacheIdGeneratorFactory;
import org.mule.runtime.core.internal.policy.PolicyManager;
import org.mule.runtime.extension.api.runtime.config.ConfigurationProvider;
import org.mule.runtime.module.extension.api.loader.java.property.CompletableComponentExecutorModelProperty;
import org.mule.runtime.module.extension.internal.runtime.operation.ComponentMessageProcessor;
import org.mule.runtime.module.extension.internal.runtime.resolver.ResolverSet;
import org.mule.runtime.module.extension.internal.runtime.resolver.ResolverSetResult;
import org.mule.runtime.module.extension.internal.runtime.resolver.ValueResolvingContext;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
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
public class ComponentMessageProcessorTestCase extends AbstractMuleContextTestCase {

  private static final Logger LOGGER = LoggerFactory.getLogger(ComponentMessageProcessorTestCase.class);

  protected ComponentMessageProcessor<ComponentModel> processor;

  protected ExtensionModel extensionModel;
  protected ComponentModel componentModel;

  protected ResolverSet resolverSet;
  protected ExtensionManager extensionManager;

  protected PolicyManager mockPolicyManager;

  @Rule
  public ExpectedException expected = ExpectedException.none();

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

    processor = createProcessor();
    processor.setAnnotations(getAppleFlowComponentLocationAnnotations());
    processor.setComponentLocator(componentLocator);
    processor.setCacheIdGeneratorFactory(mock(MetadataCacheIdGeneratorFactory.class));

    initialiseIfNeeded(processor, muleContext);
    startIfNeeded(processor);
  }

  @After
  public void after() throws MuleException {
    stopIfNeeded(processor);
    disposeIfNeeded(processor, LOGGER);
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
    eventsConsumer.await();
    eventsEmitter.stop();
    eventsConsumer2.await();
    eventsEmitter2.stop();
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
    eventsConsumer.await();
    eventsEmitter.stop();
  }

}
