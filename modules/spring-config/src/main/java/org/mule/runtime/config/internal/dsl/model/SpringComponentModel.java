/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.dsl.model;

import org.mule.runtime.api.ioc.ConfigurableObjectProvider;
import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.config.internal.model.ComponentModel;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanReference;

/**
 * Relates a {@link ComponentModel} to its Spring bean specification.
 *
 * @since 4.4
 */
public class SpringComponentModel {

  private ComponentAst component;
  private ConfigurableObjectProvider objectInstance;
  private Class<?> type;
  private BeanReference beanReference;
  private BeanDefinition beanDefinition;

  public void setComponent(ComponentAst component) {
    this.component = component;
  }

  public ComponentAst getComponent() {
    return component;
  }

  /**
   * @return the object instance already created for this model
   */
  public ConfigurableObjectProvider getObjectInstance() {
    return objectInstance;
  }

  /**
   * Setter used for components that should be created eagerly without going through spring. This is the case of models
   * contributing to IoC {@link org.mule.runtime.api.ioc.ObjectProvider} interface that require to be created before the
   * application components so they can be referenced.
   *
   * @param objectInstance the object instance created from this model.
   */
  public void setObjectInstance(ConfigurableObjectProvider objectInstance) {
    this.objectInstance = objectInstance;
  }

  /**
   * @return the type of the object to be created when processing this {@code ComponentModel}.
   */
  public <T> Class<T> getType() {
    return (Class<T>) type;
  }

  /**
   * @param type the type of the object to be created when processing this {@code ComponentModel}.
   */
  public void setType(Class<?> type) {
    this.type = type;
  }

  /**
   * @param beanDefinition the {@code BeanDefinition} created based on the {@code ComponentModel} values.
   */
  public void setBeanDefinition(BeanDefinition beanDefinition) {
    this.beanDefinition = beanDefinition;
  }

  /**
   * @return the {@code BeanDefinition} created based on the {@code ComponentModel} values.
   */
  public BeanDefinition getBeanDefinition() {
    return beanDefinition;
  }

  /**
   * @param beanReference the {@code BeanReference} that represents this object.
   */
  public void setBeanReference(BeanReference beanReference) {
    this.beanReference = beanReference;
  }

  /**
   * @return the {@code BeanReference} that represents this object.
   */
  public BeanReference getBeanReference() {
    return beanReference;
  }

}
