/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.config.internal.registry;

import org.mule.runtime.core.internal.registry.Registry;

import java.util.Map;

import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.ApplicationContext;

/**
 * Registry that wraps a Spring {@link ApplicationContext}.
 * 
 * @since 4.5
 */
public interface SpringContextRegistry extends Registry {

  /**
   * @see ApplicationContext#getBeanNamesForType(Class)
   */
  String[] getBeanNamesForType(Class<?> type);

  /**
   * @see ConfigurableBeanFactory#getDependenciesForBean(String)
   * @param key the bean to get dependencies for.
   * @return the dependencies of the bean with the provided {@code key}.
   */
  Map<String, Object> getDependencies(String key);

}
