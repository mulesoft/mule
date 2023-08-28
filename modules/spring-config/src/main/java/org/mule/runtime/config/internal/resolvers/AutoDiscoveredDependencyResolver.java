/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.config.internal.resolvers;

import static java.util.stream.Collectors.toList;

import org.mule.runtime.config.internal.BeanWrapper;
import org.mule.runtime.config.internal.registry.AbstractSpringRegistry;

import java.util.List;
import javax.inject.Inject;

import org.springframework.beans.factory.config.BeanDefinition;

/**
 * Provides the dependencies that are explicit on the {@link BeanDefinition}. These were inferred from introspecting fields
 * annotated with {@link Inject} or were programmatically added to the definition
 */
public class AutoDiscoveredDependencyResolver {

  private AbstractSpringRegistry springRegistry;

  public AutoDiscoveredDependencyResolver(AbstractSpringRegistry springRegistry) {
    this.springRegistry = springRegistry;
  }

  public List<BeanWrapper> getAutoDiscoveredDependencies(String beanName) {
    return springRegistry.getDependencies(beanName).entrySet()
        .stream()
        .map(x -> new BeanWrapper(x.getKey(), x.getValue()))
        .collect(toList());
  }


}
