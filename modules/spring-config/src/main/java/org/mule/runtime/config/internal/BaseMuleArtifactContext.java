/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal;

import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_MULE_CONTEXT;

import static java.lang.String.format;

import org.mule.runtime.config.internal.processor.MuleInjectorProcessor;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.internal.context.MuleContextWithRegistry;
import org.mule.runtime.core.internal.registry.DefaultRegistry;
import org.mule.runtime.core.internal.registry.MuleRegistryHelper;
import org.mule.runtime.core.internal.registry.Registry;

import java.io.IOException;

import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.annotation.ContextAnnotationAutowireCandidateResolver;
import org.springframework.context.support.AbstractRefreshableConfigApplicationContext;

/**
 * Spring context specialization that contains just some standard constant beans for every Mule deployable artifact.
 * 
 * @since 4.5
 */
public class BaseMuleArtifactContext extends AbstractRefreshableConfigApplicationContext {

  private final DefaultRegistry serviceDiscoverer;
  private final MuleContextWithRegistry muleContext;

  /**
   * Configures the context.
   *
   * @param muleContext the {@link MuleContext} that own this context
   */
  public BaseMuleArtifactContext(MuleContext muleContext) {
    this.muleContext = (MuleContextWithRegistry) muleContext;
    this.serviceDiscoverer = new DefaultRegistry(muleContext);
  }

  @Override
  protected void prepareBeanFactory(ConfigurableListableBeanFactory beanFactory) {
    super.prepareBeanFactory(beanFactory);
    beanFactory.setBeanExpressionResolver(null);
    registerInjectorProcessor(beanFactory);
    beanFactory.registerSingleton(OBJECT_MULE_CONTEXT, muleContext);
  }

  @Override
  protected void loadBeanDefinitions(DefaultListableBeanFactory beanFactory) throws IOException {
    // Nothing to do
  }

  @Override
  protected void customizeBeanFactory(DefaultListableBeanFactory beanFactory) {
    super.customizeBeanFactory(beanFactory);
    Registry originalRegistry = ((MuleRegistryHelper) (muleContext.getRegistry())).getDelegate();
    new BaseSpringMuleContextServiceConfigurator(muleContext,
                                                 beanFactory,
                                                 serviceDiscoverer,
                                                 originalRegistry)
                                                     .createArtifactServices();
  }

  private void registerInjectorProcessor(ConfigurableListableBeanFactory beanFactory) {
    MuleInjectorProcessor muleInjectorProcessor = new MuleInjectorProcessor();
    muleInjectorProcessor.setBeanFactory(beanFactory);
    beanFactory.addBeanPostProcessor(muleInjectorProcessor);
  }

  @Override
  protected DefaultListableBeanFactory createBeanFactory() {
    // Copy all postProcessors defined in the defaultMuleConfig so that they get applied to the child container
    DefaultListableBeanFactory beanFactory = new ObjectProviderAwareBeanFactory(getInternalParentBeanFactory());
    beanFactory.setAutowireCandidateResolver(new ContextAnnotationAutowireCandidateResolver());
    return beanFactory;
  }

  @Override
  public String toString() {
    return format("%s: %s (base)", this.getClass().getName(), muleContext.getConfiguration().getId());
  }

}
