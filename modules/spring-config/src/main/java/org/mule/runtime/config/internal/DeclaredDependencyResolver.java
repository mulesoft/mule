/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;

import org.mule.runtime.api.util.Pair;
import org.mule.runtime.config.internal.registry.SpringContextRegistry;
import org.mule.runtime.core.internal.lifecycle.InjectedDependenciesProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class DeclaredDependencyResolver {

  private final SpringContextRegistry springRegistry;

  public DeclaredDependencyResolver(SpringContextRegistry springRegistry) {
    this.springRegistry = springRegistry;
  }

  public List<Pair<String, Object>> getDeclaredDependencies(Object object) {
    List<Pair<String, Object>> declaredDependencies = new ArrayList<>();
    if (object instanceof InjectedDependenciesProvider) {
      ((InjectedDependenciesProvider) object).getInjectedDependencies()
          .forEach(dependency -> dependency
              .reduce(type -> Stream.of(springRegistry.getBeanNamesForType(dependency.getLeft()))
                  .map(name -> new Pair<>(name, springRegistry.get(name)))
                  .collect(toList()), name -> asList(new Pair<>(name, springRegistry.get(name))))
              .forEach(pair -> declaredDependencies.add(pair)));
    }
    return declaredDependencies;
  }
}
