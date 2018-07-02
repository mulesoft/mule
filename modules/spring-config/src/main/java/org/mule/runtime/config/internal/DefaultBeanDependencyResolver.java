/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal;

import static com.google.common.graph.Traverser.forTree;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import org.mule.runtime.api.util.Pair;
import org.mule.runtime.config.internal.dsl.model.ConfigurationDependencyResolver;
import org.mule.runtime.core.internal.lifecycle.InjectedDependenciesProvider;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import javax.inject.Inject;

import org.springframework.beans.factory.config.BeanDefinition;

/**
 * Default {@link BeanDependencyResolver} to resolve dependencies based on spring
 * {@link org.springframework.beans.factory.config.BeanDefinition}s and dependencies in the configuration.
 *
 * @since 4.0
 */
public class DefaultBeanDependencyResolver implements BeanDependencyResolver {

  private final SpringRegistry springRegistry;
  private final ConfigurationDependencyResolver configurationDependencyResolver;

  /**
   * Creates a new instance
   *
   * @param configurationDependencyResolver the configuration dependency resolver
   * @param springRegistry                  the context spring registry
   */
  public DefaultBeanDependencyResolver(ConfigurationDependencyResolver configurationDependencyResolver,
                                       SpringRegistry springRegistry) {
    this.configurationDependencyResolver = configurationDependencyResolver;
    this.springRegistry = springRegistry;
  }

  @Override
  public List<Object> resolveBeanDependencies(String beanName) {
    final DependencyNode root = new DependencyNode(null);

    addDependency(root, beanName, springRegistry.get(beanName));

    List<Object> orderedObjects = new LinkedList<>();
    forTree(DependencyNode::getChildren).depthFirstPostOrder(root).forEach(node -> {
      if (node != root) {
        orderedObjects.add(node.getValue());
      }
    });

    return orderedObjects;
  }

  private void addDependency(DependencyNode parent, String key, Object object) {
    addDependency(parent, key, object, new HashSet<>());
  }

  private void addDependency(DependencyNode parent, String key, Object object,
                             Set<String> processedKeys) {
    final DependencyNode node = new DependencyNode(object);
    parent.addChild(node);
    if (!processedKeys.add(key)) {
      return;
    }

    addAutoDiscoveredDependencies(key, processedKeys, node);
    addConfigurationDependencies(key, processedKeys, node);
    addDeclaredDependencies(object, processedKeys, node);
  }

  /**
   * If the target object implements {@link InjectedDependenciesProvider}, then the custom dependencies
   * declared by it are added.
   */
  private void addDeclaredDependencies(Object object, Set<String> processedKeys,
                                       DependencyNode node) {
    if (object instanceof InjectedDependenciesProvider) {
      ((InjectedDependenciesProvider) object).getInjectedDependencies()
          .forEach(dependency -> dependency
              .reduce(type -> Stream.of(springRegistry.applicationContext.getBeanNamesForType(dependency.getLeft()))
                  .map(name -> new Pair<>(name, springRegistry.get(name)))
                  .collect(toList()), name -> asList(new Pair<>(name, springRegistry.get(name))))
              .forEach(pair -> addDependency(node, pair.getFirst(), pair.getSecond(), processedKeys)));
    }
  }

  /**
   * These are obtained through the {@link #configurationDependencyResolver}
   */
  private void addConfigurationDependencies(String key, Set<String> processedKeys,
                                            DependencyNode node) {
    Collection<String> dependencies = configurationDependencyResolver.resolveComponentDependencies(key);
    for (String dependency : dependencies) {
      if (springRegistry.isSingleton(dependency)) {
        addDependency(node, dependency, springRegistry.get(dependency), processedKeys);
      }
    }
  }

  /**
   * Adds the dependencies that are explicit on the {@link BeanDefinition}. These were inferred from
   * introspecting fields annotated with {@link Inject} or were programatically added to the definition
   */
  private void addAutoDiscoveredDependencies(String key, Set<String> processedKeys,
                                             DependencyNode node) {
    for (Map.Entry<String, Object> dependency : springRegistry.getDependencies(key).entrySet()) {
      addDependency(node, dependency.getKey(), dependency.getValue(), processedKeys);
    }
  }

  private class DependencyNode {

    private final Object value;
    private final List<DependencyNode> children = new LinkedList<>();

    private DependencyNode(Object value) {
      this.value = value;
    }

    public DependencyNode addChild(DependencyNode child) {
      children.add(child);
      return this;
    }

    public List<DependencyNode> getChildren() {
      return children;
    }

    public Object getValue() {
      return value;
    }
  }
}
