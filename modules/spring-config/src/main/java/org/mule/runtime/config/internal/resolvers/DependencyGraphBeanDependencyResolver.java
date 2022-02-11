/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.resolvers;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;

import org.mule.runtime.api.util.Pair;
import org.mule.runtime.config.internal.DependencyNode;
import org.mule.runtime.config.internal.registry.AbstractSpringRegistry;
import org.mule.runtime.config.internal.registry.BeanDependencyResolver;
import org.mule.runtime.core.internal.lifecycle.InjectedDependenciesProvider;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;

import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.BeanDefinition;

/**
 * A {@link BeanDependencyResolver} to resolve direct dependencies based on spring
 * {@link org.springframework.beans.factory.config.BeanDefinition}s and dependencies in the configuration.
 *
 * @since 4.5
 */
public class DependencyGraphBeanDependencyResolver implements BeanDependencyResolver {

  private AbstractSpringRegistry springRegistry;
  private final ConfigurationDependencyResolver configurationDependencyResolver;
  private final DeclaredDependencyResolver declaredDependencyResolver;
  private final AutoDiscoveredDependencyResolver autoDiscoveredDependencyResolver;
  private Map<Integer, Set<Pair<String, Object>>> visitedComponentsForBuckets;
  private Map<Pair<String, Object>, List<Pair<String, Object>>> visitedComponents;


  public DependencyGraphBeanDependencyResolver(ConfigurationDependencyResolver configurationDependencyResolver,
                                               DeclaredDependencyResolver declaredDependencyResolver,
                                               AutoDiscoveredDependencyResolver autoDiscoveredDependencyResolver,
                                               AbstractSpringRegistry springRegistry) {
    this.configurationDependencyResolver = configurationDependencyResolver;
    this.declaredDependencyResolver = declaredDependencyResolver;
    this.autoDiscoveredDependencyResolver = autoDiscoveredDependencyResolver;
    this.springRegistry = springRegistry;
    this.visitedComponentsForBuckets = new HashMap<>();
    this.visitedComponents = new HashMap<>();
  }

  /**
   * Default method used in other sorters to resolve bean dependencies
   * 
   * @param beanName the name of the bean to resolve dependencies
   * @return
   */
  @Override
  public List<Object> resolveBeanDependencies(String beanName) {
    Object currentObject = springRegistry.get(beanName);
    final DependencyNode currentNode = new DependencyNode(currentObject);

    addDirectDependency(beanName, currentObject, currentNode, new HashSet<>());

    return currentNode.getChildren()
        .stream()
        .map(DependencyNode::getObject)
        .collect(toList());
  }

  @Override
  public ConfigurationDependencyResolver getConfigurationDependencyResolver() {
    return configurationDependencyResolver;
  }

  /**
   * Provides only direct dependencies/required components for the object provided
   * 
   * @return direct children(required components) of the current object If the component is already visited for the current
   *         bucket, we don't need to get dependencies again, return emptyList If the component is already visited for any other
   *         buckets, but not for this bucket, return the dependencies already saved If it was never visited before, get direct
   *         dependencies for the object
   */
  public List<Pair<String, Object>> getDirectBeanDependencies(Pair<String, Object> current, int bucketIndex) {
    // if the component was already processed for the current bucket, we don't process it
    visitedComponentsForBuckets.putIfAbsent(bucketIndex, new HashSet<>());
    if (visitedComponentsForBuckets.get(bucketIndex).stream().map(x -> x.getSecond())
        .anyMatch(x -> x.equals(current.getSecond()))) {
      return emptyList();
    }
    visitedComponentsForBuckets.get(bucketIndex).add(current);

    // if the component was already processed for any other bucket, it returns the direct dependencies that were saved before
    if (visitedComponents.containsKey(current)) {
      return visitedComponents.get(current);
    }

    final DependencyNode currentNode = new DependencyNode(current.getFirst(), current.getSecond());

    addDirectDependency(current.getFirst(), current.getSecond(), currentNode, new HashSet<>());

    List<Pair<String, Object>> directDependencies = currentNode.getChildren()
        .stream()
        .map(DependencyNode::getKeyObjectPair)
        .collect(toList());

    // save the dependencies for current component for future usages
    visitedComponents.put(current, directDependencies);

    return directDependencies;
  }

  /**
   * Provides all the dependencies/required components for the object provided
   *
   * @param beanName    the name of the bean to resolve dependencies
   * @param bucketIndex the bucket(graph) we calculate the dependencies for
   * @return a map with any relevant object as key, direct dependencies of the key as value
   *
   */
  public Map<Pair<String, Object>, List<Pair<String, Object>>> getTransitiveDependencies(String beanName, int bucketIndex) {
    Map<Pair<String, Object>, List<Pair<String, Object>>> transitiveDependencies = new HashMap<>();
    Set<Pair<String, Object>> processedComponents = new HashSet<>();

    ArrayDeque<Pair<String, Object>> queue = new ArrayDeque<>();
    queue.add(new Pair<>(beanName, springRegistry.get(beanName)));

    while (!queue.isEmpty()) {
      Pair<String, Object> current = queue.remove();
      // if the map already has the information about the component, we skip the component
      if (!processedComponents.add(current)) {
        continue;
      }
      List<Pair<String, Object>> dependencies = getDirectBeanDependencies(current, bucketIndex);
      queue.addAll(dependencies);
      transitiveDependencies.put(current, dependencies);
    }
    return transitiveDependencies;
  }

  /**
   * Delegates the task to the three different resolvers
   * 
   * @param beanName      current object(bean)'s name
   * @param object        current object
   * @param node          a node for the current objects to add direct children
   * @param processedKeys set of nodes that were already processed
   */
  private void addDirectDependency(String beanName, Object object, DependencyNode node, Set<DependencyNode> processedKeys) {
    addDirectAutoDiscoveredDependencies(beanName, processedKeys, node);
    addDirectConfigurationDependencies(beanName, node, processedKeys);
    addDirectDeclaredDependencies(object, processedKeys, node);
  }

  /**
   * If the target object implements {@link InjectedDependenciesProvider}, then the custom dependencies declared by it are added.
   */
  private void addDirectDeclaredDependencies(Object object, Set<DependencyNode> processedKeys, DependencyNode node) {
    declaredDependencyResolver.getDeclaredDependencies(object)
        .forEach(v -> addDirectChild(v.getBeanName(), v.getWrappedObject(), node,
                                     processedKeys));
  }

  /**
   * These are obtained through the {@link #configurationDependencyResolver}
   */
  private void addDirectConfigurationDependencies(String beanName, DependencyNode node, Set<DependencyNode> processedKeys) {
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
  private void addDirectAutoDiscoveredDependencies(String beanName, Set<DependencyNode> processedKeys,
                                                   DependencyNode node) {
    autoDiscoveredDependencyResolver.getAutoDiscoveredDependencies(beanName)
        .stream()
        .filter(v -> !v.getWrappedObject().equals(node.getObject()))
        .forEach(v -> addDirectChild(v.getBeanName(), v.getWrappedObject(), node,
                                     processedKeys));
  }

  /**
   * Add only direct children of current object
   */
  private void addDirectChild(String key, Object childObject, DependencyNode parent, Set<DependencyNode> processedKeys) {
    if (!processedKeys.add(new DependencyNode(key, childObject))) {
      return;
    }
    parent.addChild(new DependencyNode(key, childObject));
  }

}
