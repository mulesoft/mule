/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal;

import org.mule.runtime.core.internal.lifecycle.phases.DefaultLifecycleObjectSorter;

import java.util.List;

public class SpringLifecycleObjectSorter extends DefaultLifecycleObjectSorter {

  private final SpringRegistry registry;

  public SpringLifecycleObjectSorter(Class<?>[] orderedLifecycleTypes, SpringRegistry registry) {
    super(orderedLifecycleTypes);
    this.registry = registry;
  }

  @Override
  protected int doAddObject(String name, Object object, List<Object> bucket) {
    final List<Object> dependencies = registry.getBeanDependencyResolver().resolveBeanDependencies(name);
    bucket.addAll(dependencies);
    bucket.add(object);
    return dependencies.size() + 1;
  }
}
