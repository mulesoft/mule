/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.config.internal.processor;

import org.mule.runtime.core.api.lifecycle.LifecycleState;
import org.mule.runtime.core.api.lifecycle.LifecycleStateAware;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;

/**
 * A {@link BeanPostProcessor} which sets a given {@link #state} into instances of {@link LifecycleStateAware}
 *
 * @since 3.7.0
 */
public final class LifecycleStatePostProcessor implements BeanPostProcessor {

  private final LifecycleState state;

  public LifecycleStatePostProcessor(LifecycleState state) {
    this.state = state;
  }

  @Override
  public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
    if (bean instanceof LifecycleStateAware) {
      ((LifecycleStateAware) bean).setLifecycleState(state);
    }

    return bean;
  }

  @Override
  public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
    return bean;
  }
}
