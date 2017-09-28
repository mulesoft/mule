/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.factories;

import static org.mule.runtime.api.util.Preconditions.checkArgument;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.disposeIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.startIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.stopIfNeeded;
import org.mule.runtime.api.component.AbstractComponent;
import org.mule.runtime.api.component.Component;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.lifecycle.Lifecycle;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.context.MuleContextAware;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
public class ConstantFactoryBean<T> extends AbstractComponent implements FactoryBean<T>, MuleContextAware, Lifecycle {

  private static final Logger LOGGER = LoggerFactory.getLogger(ConstantFactoryBean.class);

  private final T value;
  private MuleContext muleContext;

  public ConstantFactoryBean(T value) {
    checkArgument(value != null, "value cannot be null");
    this.value = value;
  }

  @Override
  public T getObject() throws Exception {
    if (value instanceof Component) {
      ((Component) value).setAnnotations(getAnnotations());
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

  @Override
  public void setMuleContext(MuleContext muleContext) {
    this.muleContext = muleContext;
  }

  @Override
  public void initialise() throws InitialisationException {
    initialiseIfNeeded(value, true, muleContext);
  }

  @Override
  public void start() throws MuleException {
    startIfNeeded(value);
  }

  @Override
  public void stop() throws MuleException {
    stopIfNeeded(value);
  }

  @Override
  public void dispose() {
    disposeIfNeeded(value, LOGGER);
  }
}
