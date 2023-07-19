/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.config.internal.factories;

import static org.mule.runtime.api.util.Preconditions.checkArgument;
import org.mule.runtime.api.component.AbstractComponent;
import org.mule.runtime.api.component.Component;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.Lifecycle;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.context.MuleContextAware;

import javax.inject.Inject;

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

  public ConstantFactoryBean(T value) {
    checkArgument(value != null, "value cannot be null");
    this.value = value;
  }

  @Override
  public T getObject() throws Exception {
    if (value instanceof Component) {
      ((Component) value).setAnnotations(getAnnotations());
    }

    if (!(value instanceof Initialisable)) {
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
