/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal;

import static java.util.stream.Collectors.toList;

import org.mule.runtime.api.util.Pair;
import org.mule.runtime.config.internal.dsl.model.ConfigurationDependencyResolver;
import org.mule.runtime.config.internal.registry.BeanDependencyResolver;
import org.mule.runtime.config.internal.registry.SpringContextRegistry;
import org.mule.runtime.core.internal.lifecycle.InjectedDependenciesProvider;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.inject.Inject;

import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.BeanDefinition;

public class DummyDependencyResolver implements BeanDependencyResolver {

  private final SpringContextRegistry springRegistry;
  private final Set<String> processedKey;
  private final ConfigurationDependencyResolver configurationDependencyResolver;
  private final DeclaredDependencyResolver declaredDependencyResolver;
  private final AutoDiscoveredDependencyResolver autoDiscoveredDependencyResolver;


  public DummyDependencyResolver(ConfigurationDependencyResolver configurationDependencyResolver,
                                 DeclaredDependencyResolver declaredDependencyResolver,
                                 AutoDiscoveredDependencyResolver autoDiscoveredDependencyResolver,
                                 SpringContextRegistry springRegistry) {
    this.configurationDependencyResolver = configurationDependencyResolver;
    this.declaredDependencyResolver = declaredDependencyResolver;
    this.autoDiscoveredDependencyResolver = autoDiscoveredDependencyResolver;
    this.springRegistry = springRegistry;
    processedKey = new HashSet<>();
  }


  @Override
  public List<Object> resolveBeanDependencies(String beanName) {
    Object currentObject = springRegistry.get(beanName);
    final DependencyNode currentNode = new DependencyNode(currentObject);

    addDirectDependency(beanName, currentObject, currentNode, processedKey);

    return currentNode.getChildren()
        .stream()
        .map(DependencyNode::getObject)
        .collect(toList());
  }

  @Override
  public ConfigurationDependencyResolver getConfigurationDependencyResolver() {
    return configurationDependencyResolver;
  }

  public List<Pair<String, Object>> getDirectBeanDependencies(String beanName) {
    Object currentObject = springRegistry.get(beanName);
    final DependencyNode currentNode = new DependencyNode(beanName, currentObject);

    addDirectDependency(beanName, currentObject, currentNode, processedKey);

    return currentNode.getChildren()
        .stream()
        .map(DependencyNode::getKeyObjectPair)
        .collect(toList());
  }

  private void addDirectDependency(String beanName, Object object, DependencyNode node, Set<String> processedKeys) {
    addDirectAutoDiscoveredDependencies(beanName, processedKeys, node);
    addDirectConfigurationDependencies(beanName, node, processedKeys);
    addDirectDeclaredDependencies(object, processedKeys, node);
  }

  /**
   * If the target object implements {@link InjectedDependenciesProvider}, then the custom dependencies declared by it are added.
   */
  private void addDirectDeclaredDependencies(Object object, Set<String> processedKeys, DependencyNode node) {
    declaredDependencyResolver.getDeclaredDependencies(object)
        .forEach(v -> addDirectChild(v.getBeanName(), v.getWrappedObject(), node,
                                     processedKeys));
  }

  /**
   * These are obtained through the {@link #configurationDependencyResolver}
   */
  private void addDirectConfigurationDependencies(String beanName, DependencyNode node, Set<String> processedKeys) {
    if (configurationDependencyResolver == null) {
      return;
    }
    for (String name : configurationDependencyResolver.getDirectComponentDependencies(beanName)) {
      try {
        if (springRegistry.isSingleton(name)) {
          addDirectChild(name, springRegistry.get(name), node, processedKeys);
        }
      } catch (NoSuchBeanDefinitionException e) {
        // we're starting in lazy mode... disregard.
      }
    }
  }

  /**
   * Adds the dependencies that are explicit on the {@link BeanDefinition}. These were inferred from introspecting fields
   * annotated with {@link Inject} or were programmatically added to the definition
   */
  private void addDirectAutoDiscoveredDependencies(String beanName, Set<String> processedKeys, DependencyNode node) {
    autoDiscoveredDependencyResolver.getAutoDiscoveredDependencies(beanName)
        .stream()
        .filter(v -> !v.getWrappedObject().equals(node.getObject()))
        .forEach(v -> addDirectChild(v.getBeanName(), v.getWrappedObject(), node,
                                     processedKeys));
  }


  private void addDirectChild(String key, Object childObject, DependencyNode parent, Set<String> processedKeys) {
    if (!processedKeys.add(key)) {
      return;
    }
    parent.addChild(new DependencyNode(key, childObject));
  }



}
