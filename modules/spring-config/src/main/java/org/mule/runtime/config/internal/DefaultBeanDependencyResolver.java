/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.mule.runtime.config.internal.dsl.model.ConfigurationDependencyResolver;

import com.google.common.collect.TreeTraverser;

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
   * @param springRegistry the context spring registry
   */
  public DefaultBeanDependencyResolver(ConfigurationDependencyResolver configurationDependencyResolver,
                                       SpringRegistry springRegistry) {
    this.configurationDependencyResolver = configurationDependencyResolver;
    this.springRegistry = springRegistry;
  }

  public Collection<Object> resolveBeanDependencies(Set<String> beanNames) {
    final DependencyNode root = new DependencyNode(null);

    for (String beanName : beanNames) {
      addDependency(root, beanName, springRegistry.get(beanName));
    }

    Iterable<DependencyNode> orderedNodes = new TreeTraverser<DependencyNode>() {

      @Override
      public Iterable children(DependencyNode node) {
        return node.getChildren();
      }
    }.postOrderTraversal(root);

    List<Object> orderedObjects = new LinkedList<>();
    for (DependencyNode node : orderedNodes) {
      if (node == root) {
        break;
      }

      orderedObjects.add(node.getValue());
    }
    return orderedObjects;
  }

  private void addDependency(DependencyNode parent, String key, Object object) {
    addDependency(parent, key, object, new HashSet<>());
  }

  private void addDependency(DependencyNode parent, String key, Object object,
                             Set<String> processedKeys) {
    final DependencyNode node = new DependencyNode(object);
    parent.addChild(node);
    if (!processedKeys.contains(key)) {
      processedKeys.add(key);
      for (Map.Entry<String, Object> dependency : springRegistry.getDependencies(key).entrySet()) {
        addDependency(node, dependency.getKey(), dependency.getValue(), processedKeys);
      }
      Collection<String> dependencies = configurationDependencyResolver.resolveComponentDependencies(key);
      for (String dependency : dependencies) {
        if (springRegistry.isSingleton(dependency)) {
          addDependency(node, dependency, springRegistry.get(dependency), processedKeys);
        }
      }
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
