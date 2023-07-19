/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.config.privileged.dsl;

import org.mule.api.annotation.NoImplement;
import org.mule.runtime.dsl.api.component.config.ComponentConfiguration;

import java.util.List;
import java.util.Map;

/**
 * Provides a way to decouple {@link BeanDefinitionPostProcessor} implementations from the underlying IoC container.
 * 
 * @since 4.0
 */
@NoImplement
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
