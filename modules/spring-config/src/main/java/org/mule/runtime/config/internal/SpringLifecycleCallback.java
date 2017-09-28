/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal;

import org.mule.runtime.api.lifecycle.Lifecycle;
import org.mule.runtime.core.api.lifecycle.LifecycleObject;
import org.mule.runtime.core.internal.lifecycle.RegistryLifecycleCallback;
import org.mule.runtime.core.internal.lifecycle.RegistryLifecycleManager;

import java.util.Collection;
import java.util.Map;

/**
 * A {@link RegistryLifecycleCallback} to be used with instances of {@link SpringRegistry}. For each object in which a
 * {@link Lifecycle} phase is going to be applied, it detects all the dependencies for that object and applies the same phase on
 * those dependencies first (recursively).
 * <p/>
 * This guarantees that if object A depends on object B and C, necessary lifecycle phases will have been applied on B and C before
 * it is applied to A
 *
 * @since 3.7.0
 */
class SpringLifecycleCallback extends RegistryLifecycleCallback<SpringRegistry> {

  private final SpringRegistry springRegistry;

  public SpringLifecycleCallback(RegistryLifecycleManager registryLifecycleManager,
                                 SpringRegistry springRegistry) {
    super(registryLifecycleManager);
    this.springRegistry = springRegistry;
  }

  @Override
  protected Collection<?> lookupObjectsForLifecycle(LifecycleObject lo) {
    Map<String, Object> objects = getSpringRegistry().lookupEntriesForLifecycle(lo.getType());
    return springRegistry.getBeanDependencyResolver().resolveBeanDependencies(objects.keySet());
  }

  private SpringRegistry getSpringRegistry() {
    return (SpringRegistry) registryLifecycleManager.getLifecycleObject();
  }

}
