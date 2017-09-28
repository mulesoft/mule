/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.api.dsl;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.core.api.util.ClassUtils;
import org.mule.runtime.dsl.api.component.AbstractComponentFactory;
import org.mule.runtime.dsl.api.component.ObjectFactory;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

/**
 * {@inheritDoc}
 *
 * @since 4.0
 */
public class ConfigurableObjectFactory<T> extends AbstractComponentFactory<T> {

  @Inject
  private MuleContext muleContext;

  private Class<ObjectFactoryCommonConfigurator> commonConfiguratorType;
  private ConfigurableInstanceFactory factory;
  private Map<String, Object> parameters = new HashMap<>();

  @Override
  public T doGetObject() throws Exception {
    Object instance = factory.createInstance(parameters);
    if (commonConfiguratorType != null) {
      ObjectFactoryCommonConfigurator commonConfigurator = ClassUtils.instantiateClass(commonConfiguratorType);
      commonConfigurator.configure(instance, parameters);
    }
    if (instance instanceof MuleContextAware) {
      ((MuleContextAware) instance).setMuleContext(muleContext);
    }
    return (T) instance;
  }

  /**
   * injection method for the {@link ObjectFactoryCommonConfigurator} of the object instance.
   *
   * @param commonConfiguratorType the common configuration type.
   */
  public void setCommonConfiguratorType(Class<ObjectFactoryCommonConfigurator> commonConfiguratorType) {
    this.commonConfiguratorType = commonConfiguratorType;
  }

  /**
   * Sets a {@link ConfigurableInstanceFactory} to create a do custom configuration for the object to be used at runtime.
   *
   * @param factory the factory to create the runtime object instance.
   */
  public void setFactory(ConfigurableInstanceFactory factory) {
    this.factory = factory;
  }

  /**
   * @param parameters set of component parameters map to this {@link ObjectFactory}
   */
  public void setParameters(Map<String, Object> parameters) {
    this.parameters = parameters;
  }
}
