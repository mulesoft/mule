/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.registry;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.internal.lifecycle.LifecycleInterceptor;

import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.context.ApplicationContext;

public class BaseSpringRegistry extends AbstractSpringRegistry {

  public static final String REGISTRY_ID = "org.mule.Registry.Spring";

  /**
   * Key used to lookup Spring Application Context from SpringRegistry via Mule's Registry interface.
   */
  public static final String SPRING_APPLICATION_CONTEXT = "springApplicationContext";
  private final BeanDependencyResolver beanDependencyResolver;

  // This is used to track the Spring context lifecycle since there is no way to confirm the
  // lifecycle phase from the application context
  protected AtomicBoolean springContextInitialised = new AtomicBoolean(false);

  public BaseSpringRegistry(ApplicationContext applicationContext,
                            MuleContext muleContext,
                            LifecycleInterceptor lifecycleInterceptor) {
    super(applicationContext, muleContext, lifecycleInterceptor);
    this.beanDependencyResolver = new DefaultBeanDependencyResolver(null, this);
  }

  @Override
  public BeanDependencyResolver getBeanDependencyResolver() {
    return beanDependencyResolver;
  }
}
