/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal;

import static java.util.stream.Collectors.toList;

import org.mule.runtime.config.internal.registry.AbstractSpringRegistry;

import java.util.List;

public class AutoDiscoveredDependencyResolver {

  private AbstractSpringRegistry springRegistry;

  public AutoDiscoveredDependencyResolver(AbstractSpringRegistry springRegistry) {
    this.springRegistry = springRegistry;
  }

  public List<BeanVertexWrapper> getAutoDiscoveredDependencies(String beanName) {
    return springRegistry.getDependencies(beanName).entrySet()
        .stream()
        .map(x -> new BeanVertexWrapper(x.getKey(), x.getValue()))
        .collect(toList());
  }


}
