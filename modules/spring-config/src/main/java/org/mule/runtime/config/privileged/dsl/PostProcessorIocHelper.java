/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.privileged.dsl;

import org.mule.runtime.dsl.api.component.config.ComponentConfiguration;

import java.util.List;
import java.util.Map;

/**
 * Provides a way to decouple {@link BeanDefinitionPostProcessor} implementations from the underlying IoC container.
 * 
 * @since 4.0
 */
public interface PostProcessorIocHelper {

  void replaceDefinitionWithRoot(Class beanClass, boolean wrap, Object... constructorArgs);

  void addReferenceConstructorArg(String beanName);

  void addPropertyValue(String propertyName, Object propertyValue);

  void addReferencesPropertyValue(String propertyName, String[] refBeanNames);

  void addDefinitionPropertyValue(String propertyName, Class beanClass, Map<String, Object> properties, boolean addContext,
                                  Object... constructorArgs);

  void removePropertyValue(String propertyName);

  Object getPropertyValue(String propertyName);

  List toBeanDefinitions(List<ComponentConfiguration> components);

  Map toBeanDefinitionsProperties(List<ComponentConfiguration> components);

  boolean isDefinitionFor(Object definition, Class beanClass);

}
