/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.factories;

import static org.mule.runtime.api.util.Preconditions.checkArgument;

import org.mule.runtime.api.component.AbstractComponent;
import org.mule.runtime.api.component.Component;
import org.mule.runtime.api.lifecycle.Lifecycle;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.context.MuleContextAware;

import jakarta.inject.Inject;

import org.springframework.beans.factory.FactoryBean;

/**
 * A {@link FactoryBean} which returns a fixed instanced obtained through the constructor. {@link #isSingleton()} always returns
 * {@code true}.
 * <p/>
 * Invocations related to the {@link MuleContextAware} and {@link Lifecycle} interfaces are delegated into the {@link #value}
 * object when applies.
 *
 * @param <T>
 * @since 3.7.0
 */
public class ConstantFactoryBean<T> extends AbstractComponent implements FactoryBean<T> {

  @Inject
  private MuleContext muleContext;
  private final T value;
  private final boolean inject;

  public ConstantFactoryBean(T value) {
    this(value, false);
  }

  public ConstantFactoryBean(T value, boolean inject) {
    checkArgument(value != null, "value cannot be null");
    this.value = value;
    this.inject = inject;
  }

  @Override
  public T getObject() throws Exception {
    if (value instanceof Component) {
      ((Component) value).setAnnotations(getAnnotations());
    }

    if (inject) {
      muleContext.getInjector().inject(value);
    }

    return value;
  }

  @Override
  public boolean isSingleton() {
    return true;
  }

  @Override
  public Class<?> getObjectType() {
    return value.getClass();
  }

  public void setMuleContext(MuleContext muleContext) {
    this.muleContext = muleContext;
  }
}
