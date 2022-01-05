/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal;

import org.mule.runtime.api.util.Pair;
import org.mule.runtime.config.internal.dsl.model.ConfigurationDependencyResolver;
import org.mule.runtime.config.internal.registry.BeanDependencyResolver;
import org.mule.runtime.config.internal.registry.SpringContextRegistry;
import org.mule.runtime.core.internal.lifecycle.InjectedDependenciesProvider;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.BeanDefinition;

import javax.inject.Inject;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import static java.util.stream.Collectors.toList;

public class DummyDependencyResolver implements BeanDependencyResolver {

  private final SpringContextRegistry springRegistry;
  private Set<String> processedKey;
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

  public List<Object> getDirectDependencies(String name) {
    return resolveBeanDependencies(name);
  }

  @Override
  public List<Object> resolveBeanDependencies(String beanName) {
    Object currentObject = springRegistry.get(beanName);
    final DependencyNode currentNode = new DependencyNode(currentObject);

    addDirectDependency(beanName, currentObject, currentNode);

    return currentNode.getChildren()
        .stream()
        .map(DependencyNode::getValue)
        .collect(toList());
  }

  public List<Pair<Object, String>> resolveBeanDependencies(String beanName, boolean includeBeanName) {
    Object currentObject = springRegistry.get(beanName);
    final DependencyNode currentNode = new DependencyNode(currentObject, beanName);

    addDirectDependency(beanName, currentObject, currentNode);

    return currentNode.getChildren()
        .stream()
        .map(DependencyNode::getObjectKeyPair)
        .collect(toList());
  }



  private void addDirectDependency(String key, Object currentObject, DependencyNode currentNode) {
    addDirectDependency(key, currentObject, currentNode, processedKey);
  }

  private void addDirectDependency(String key, Object object, DependencyNode node, Set<String> processedKeys) {
    addDirectAutoDiscoveredDependencies(key, processedKeys, node);
    addDirectConfigurationDependencies(key, processedKeys, node);
    addDirectDeclaredDependencies(object, processedKeys, node);
  }

  /**
   * If the target object implements {@link InjectedDependenciesProvider}, then the custom dependencies declared by it are added.
   */
  private void addDirectDeclaredDependencies(Object object, Set<String> processedKeys, DependencyNode node) {
    declaredDependencyResolver.getDeclaredDirectDependencies(object)
        .forEach(pair -> addDirectChild(node, pair.getFirst(), pair.getSecond(), processedKeys));
  }
  // private void addDirectDeclaredDependencies(Object object, Set<String> processedKeys, DependencyNode node) {
  // if (object instanceof InjectedDependenciesProvider) {
  // ((InjectedDependenciesProvider) object).getInjectedDependencies()
  // .forEach(dependency -> dependency
  // .reduce(type -> Stream.of(springRegistry.applicationContext.getBeanNamesForType(dependency.getLeft()))
  // .map(name -> new Pair<>(name, springRegistry.get(name)))
  // .collect(toList()), name -> asList(new Pair<>(name, springRegistry.get(name))))
  // .forEach(pair -> addDirectChild(node, pair.getFirst(), pair.getSecond(), processedKeys)));
  // }
  //
  // }

  /**
   * These are obtained through the {@link #configurationDependencyResolver}
   */
  private void addDirectConfigurationDependencies(String key, Set<String> processedKeys, DependencyNode node) {
    if (configurationDependencyResolver == null) {
      return;
    }
    for (String dependency : configurationDependencyResolver.getDirectComponentDependencies(key)) {
      try {
        if (springRegistry.isSingleton(dependency)) { // to use it, configResolver, make it static..
          addDirectChild(node, dependency, springRegistry.get(dependency), processedKeys);
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
  private void addDirectAutoDiscoveredDependencies(String key, Set<String> processedKeys, DependencyNode node) {
    autoDiscoveredDependencyResolver.getAutoDiscoveredDependencies(key)
        .stream().filter(x -> !x.getValue().equals(node.getValue())) // todo: write test for this case
        .forEach(dependency -> addDirectChild(node, dependency.getKey(), dependency.getValue(), processedKeys));
  }
  // private void addDirectAutoDiscoveredDependencies(String key, Set<String> processedKeys, DependencyNode node) {
  // for (Map.Entry<String, Object> dependency : springRegistry.getDependencies(key).entrySet()) {
  // addDirectChild(node, dependency.getKey(), dependency.getValue(), processedKeys);
  // }
  // }
  // todo: move this to sorter (tree related part)

  private void addDirectChild(DependencyNode parent, String key, Object childObject, Set<String> processedKeys) {
    if (!processedKeys.add(key))
      return; // A relies on B, D, E, G and new
    // DependencyNode childNode = new DependencyNode(childObject);
    DependencyNode childNode = new DependencyNode(childObject, key);
    parent.addChild(childNode);

  }



}
