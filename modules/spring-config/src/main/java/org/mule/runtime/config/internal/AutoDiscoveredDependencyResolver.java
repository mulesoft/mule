/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal;

import org.mule.runtime.config.internal.registry.SpringContextRegistry;

import java.util.Map;
import java.util.Set;

public class AutoDiscoveredDependencyResolver {

  private final SpringContextRegistry springRegistry;

  public AutoDiscoveredDependencyResolver(SpringContextRegistry springRegistry) {
    this.springRegistry = springRegistry;
  }

  public Set<Map.Entry<String, Object>> getAutoDiscoveredDependencies(String key) {
    return springRegistry.getDependencies(key).entrySet();
    // todo: map , transform the keys & values to pairs
  }
}
