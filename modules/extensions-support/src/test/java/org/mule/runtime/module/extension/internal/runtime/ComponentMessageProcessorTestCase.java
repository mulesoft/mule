/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime;

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
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.disposeIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.startIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.stopIfNeeded;
import static org.mule.runtime.core.api.rx.Exceptions.unwrap;
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
import org.mule.runtime.extension.api.runtime.config.ConfigurationProvider;
import org.mule.runtime.module.extension.api.loader.java.property.CompletableComponentExecutorModelProperty;
import org.mule.runtime.module.extension.internal.runtime.operation.ComponentMessageProcessor;
import org.mule.runtime.module.extension.internal.runtime.resolver.ResolverSet;
import org.mule.runtime.module.extension.internal.runtime.resolver.ResolverSetResult;
import org.mule.runtime.module.extension.internal.runtime.resolver.ValueResolvingContext;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ComponentMessageProcessorTestCase extends AbstractMuleContextTestCase {

  private static final Logger LOGGER = LoggerFactory.getLogger(ComponentMessageProcessorTestCase.class);

  private ComponentMessageProcessor<ComponentModel> processor;

  private ExtensionModel extensionModel;
  private ComponentModel componentModel;

  private ResolverSet resolverSet;
  private ExtensionManager extensionManager;

  @Rule
  public ExpectedException expected = ExpectedException.none();

  @Before
  public void before() throws MuleException {
    CoreEvent response = testEvent();

    extensionModel = mock(ExtensionModel.class);
    when(extensionModel.getXmlDslModel()).thenReturn(XmlDslModel.builder().setPrefix("mock").build());

    componentModel = mock(ComponentModel.class, withSettings().extraInterfaces(EnrichableModel.class));
    when((componentModel).getModelProperty(CompletableComponentExecutorModelProperty.class))
        .thenReturn(of(new CompletableComponentExecutorModelProperty((cp, p) -> (ctx, callback) -> callback.complete(response))));
    resolverSet = mock(ResolverSet.class);

    extensionManager = mock(ExtensionManager.class);

    processor = new ComponentMessageProcessor<ComponentModel>(extensionModel,
                                                              componentModel, null, null, null,
                                                              resolverSet, null, null,
                                                              extensionManager,
                                                              null, null) {

      @Override
      protected void validateOperationConfiguration(ConfigurationProvider configurationProvider) {}

      @Override
      public ProcessingType getInnerProcessingType() {
        return ProcessingType.CPU_LITE;
      }
    };
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

    expect(thrown);
    from(processor.apply(just(testEvent()))).block();
  }

  @Test
  public void muleExceptionInResolutionResult() throws MuleException {
    final Exception thrown = new DefaultMuleException(createStaticMessage("Expected"));

    when(resolverSet.resolve(any(ValueResolvingContext.class))).thenThrow(thrown);

    expect(thrown);
    from(processor.apply(just(testEvent()))).block();
  }

  @Test
  public void runtimeExceptionInResolutionResult() throws MuleException {
    final Exception thrown = new NullPointerException("Expected");

    when(resolverSet.resolve(any(ValueResolvingContext.class))).thenThrow(thrown);

    expect(thrown);
    from(processor.apply(just(testEvent()))).block();
  }

  private void expect(Exception expect) {
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
}
