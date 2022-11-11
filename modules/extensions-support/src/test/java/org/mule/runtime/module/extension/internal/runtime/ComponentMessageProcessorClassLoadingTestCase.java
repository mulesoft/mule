/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime;

import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.disposeIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.startIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.stopIfNeeded;
import static org.mule.runtime.oauth.internal.util.ClassLoaderUtils.setContextClassLoader;

import static java.lang.Thread.currentThread;
import static java.util.Optional.of;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.meta.model.ComponentModel;
import org.mule.runtime.api.meta.model.EnrichableModel;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.XmlDslModel;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.extension.ExtensionManager;
import org.mule.runtime.core.internal.policy.PolicyManager;
import org.mule.runtime.core.privileged.processor.chain.DefaultMessageProcessorChainBuilder;
import org.mule.runtime.core.privileged.processor.chain.MessageProcessorChain;
import org.mule.runtime.extension.api.runtime.config.ConfigurationProvider;
import org.mule.runtime.metadata.api.cache.MetadataCacheIdGeneratorFactory;
import org.mule.runtime.module.extension.api.loader.java.property.CompletableComponentExecutorModelProperty;
import org.mule.runtime.module.extension.internal.runtime.operation.ComponentMessageProcessor;
import org.mule.runtime.module.extension.internal.runtime.resolver.ResolverSet;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.junit.After;
import org.junit.Test;

import io.qameta.allure.Description;
import io.qameta.allure.Issue;

public class ComponentMessageProcessorClassLoadingTestCase extends AbstractMuleContextTestCase {

  private static final Logger LOGGER = LoggerFactory.getLogger(ComponentMessageProcessorTestCase.class);
  private ComponentMessageProcessor<ComponentModel> processor;
  private ExtensionModel extensionModel;
  private ComponentModel componentModel;
  private ResolverSet resolverSet;
  private MessageProcessorChain nestedChain;
  private ClassLoader applicationClassLoader;
  private ExtensionManager extensionManager;
  private PolicyManager policyManager;
  private ClassLoader contextClassLoader;

  @After
  public void after() throws MuleException {
    stopIfNeeded(processor);
    disposeIfNeeded(processor, LOGGER);
  }

  @Test
  @Issue("W-11625237")
  @Description("The ComponentMessageProcessor loads the application classLoader to start the processors and then goes back to the context classLoader.")
  public void componentMessageProcessorUsesTheApplicationClassLoaderToStartIfNeeded() throws MuleException {
    CoreEvent response = testEvent();

    extensionModel = mock(ExtensionModel.class);
    when(extensionModel.getXmlDslModel()).thenReturn(XmlDslModel.builder().setPrefix("mock").build());

    componentModel = mock(ComponentModel.class, withSettings().extraInterfaces(EnrichableModel.class));
    when((componentModel).getModelProperty(CompletableComponentExecutorModelProperty.class))
        .thenReturn(of(new CompletableComponentExecutorModelProperty((cp, p) -> (ctx, callback) -> callback.complete(response))));

    resolverSet = mock(ResolverSet.class);
    nestedChain = new DefaultMessageProcessorChainBuilder().build();
    applicationClassLoader = mock(ClassLoader.class);
    extensionManager = mock(ExtensionManager.class);
    policyManager = mock(PolicyManager.class);
    contextClassLoader = Thread.currentThread().getContextClassLoader();
    processor = new TestComponentMessageProcessor(extensionModel, componentModel, null, null, null,
                                                  resolverSet, null, null,
                                                  nestedChain, applicationClassLoader, extensionManager,
                                                  policyManager, null, null,
                                                  muleContext.getConfiguration().getShutdownTimeout()) {

      @Override
      protected void validateOperationConfiguration(ConfigurationProvider configurationProvider) {}

      @Override
      protected void startIfNeededNestedChain() throws MuleException {
        if (nestedChain != null) {
          final Thread currentThread = Thread.currentThread();
          final ClassLoader currentClassLoader = currentThread.getContextClassLoader();

          assertThat(currentThread().getContextClassLoader(), is(sameInstance(contextClassLoader)));

          setContextClassLoader(currentThread, currentClassLoader, this.nestedChainClassLoader);
          try {
            assertThat(currentThread().getContextClassLoader(), is(sameInstance(applicationClassLoader)));

            startIfNeeded(nestedChain);
          } finally {
            setContextClassLoader(currentThread, this.nestedChainClassLoader, currentClassLoader);

            assertThat(currentClassLoader, is(sameInstance(contextClassLoader)));
          }
        }
      }
    };

    processor.setAnnotations(getAppleFlowComponentLocationAnnotations());
    processor.setComponentLocator(componentLocator);
    processor.setCacheIdGeneratorFactory(of(mock(MetadataCacheIdGeneratorFactory.class)));

    initialiseIfNeeded(processor, muleContext);
    processor.doStart();

    assertThat(contextClassLoader, not(sameInstance(applicationClassLoader)));
  }
}
