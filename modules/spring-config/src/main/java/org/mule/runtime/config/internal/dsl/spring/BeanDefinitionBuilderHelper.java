/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.dsl.spring;

import static org.mule.runtime.api.util.Preconditions.checkArgument;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.support.ManagedMap;

/**
 * Helper for {@link org.springframework.beans.factory.support.BeanDefinitionBuilder} to use the proper methods to populate a
 * reference or value to a constructor or setter.
 *
 * @since 4.0
 */
class BeanDefinitionBuilderHelper {

  public BeanDefinitionBuilder beanDefinitionBuilder;

  public BeanDefinitionBuilderHelper(BeanDefinitionBuilder beanDefinitionBuilder) {
    this.beanDefinitionBuilder = beanDefinitionBuilder;
  }

  /**
   * @param value adds a constructor value
   */
  public void addConstructorValue(Object value) {
    this.beanDefinitionBuilder.addConstructorArgValue(value);
  }

  /**
   * @param propertyName name of the object property to use to populate values
   * @return a helper that allows to populate values for the specified property
   */
  public BeanDefinitionPropertyHelper forProperty(String propertyName) {
    return new BeanDefinitionPropertyHelper(propertyName);
  }

  /**
   * @param propertyName name of a bean property
   * @return true if the bean already has a value set for that property, false otherwise
   */
  public boolean hasValueForProperty(String propertyName) {
    return this.beanDefinitionBuilder.getBeanDefinition().getPropertyValues().contains(propertyName);
  }

  class BeanDefinitionPropertyHelper {

    private String propertyName;

    private BeanDefinitionPropertyHelper(String propertyName) {
      checkArgument(propertyName != null, "propertyName must be not null");
      this.propertyName = propertyName;
    }

    @Override
    public int hashCode() {
      return super.hashCode();
    }

    /**
     * @param reference sets a reference value to the property
     */
    public void addReference(String reference) {
      beanDefinitionBuilder.addPropertyReference(propertyName, reference);
    }

    /**
     * @param value sets a value to the property
     */
    public void addValue(Object value) {
      if (!isEmpty(value)) {
        beanDefinitionBuilder.addPropertyValue(propertyName, value);
      }
    }

    private boolean isEmpty(Object value) {
      return value == null || (value instanceof ManagedList && ((ManagedList) value).isEmpty())
          || (value instanceof ManagedMap && ((ManagedMap) value).isEmpty());
    }
  }
}
