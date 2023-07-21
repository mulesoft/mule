/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.config.internal.registry;

import org.mule.runtime.config.internal.DependencyGraphLifecycleObjectSorter;
import org.mule.runtime.core.internal.lifecycle.phases.DefaultLifecycleObjectSorter;

import java.util.List;

import javax.inject.Inject;

import org.springframework.beans.factory.config.BeanDefinition;

/**
 * Specialization of {@link DefaultLifecycleObjectSorter} which uses an {@link AbstractSpringRegistry} to not only consider the
 * provided objects but the beans on which that object depends on. This is accomplished by introspecting the
 * {@link BeanDefinition} that was derived from the {@link Inject} annotations
 *
 * @since 4.2.0
 * @deprecated as of 4.5.0, Use {@link DependencyGraphLifecycleObjectSorter }instead
 */
@Deprecated
public class SpringLifecycleObjectSorter extends DefaultLifecycleObjectSorter {

  private final AbstractSpringRegistry registry;

  /**
   * Creates a new instance
   *
   * @param orderedLifecycleTypes an ordered array specifying a type based order
   * @param registry              an {@link AbstractSpringRegistry}
   */
  public SpringLifecycleObjectSorter(Class<?>[] orderedLifecycleTypes, AbstractSpringRegistry registry) {
    super(orderedLifecycleTypes);
    this.registry = registry;
  }

  /**
   * Adds all the dependencies of the given {@code object} first, and the actual object last {@inheritDoc}
   */
  @Override
  protected int doAddObject(String name, Object object, List<Object> bucket) {
    final List<Object> dependencies = registry.getBeanDependencyResolver().resolveBeanDependencies(name);
    bucket.addAll(dependencies);
    bucket.add(object);
    return dependencies.size() + 1;
  }
}
