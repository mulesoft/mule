/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal;

import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.traverse.TopologicalOrderIterator;
import org.mule.runtime.core.internal.lifecycle.phases.LifecycleObjectSorter;

import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Collections.reverse;
import static java.util.Objects.requireNonNull;

public class DummySpringLifecycleObjectSorter implements LifecycleObjectSorter {

  private DefaultDirectedGraph<Object, DefaultEdge> dependencyGraph;
  private DummyDependencyResolver resolver;

  public DummySpringLifecycleObjectSorter(DummyDependencyResolver resolver) {
    dependencyGraph = new DefaultDirectedGraph<>(DefaultEdge.class);
    this.resolver = resolver;
  }

  @Override
  public void addObject(String name, Object currentObject) {
    // null check
    requireNonNull(currentObject, "currentObject cannot be null");
    // add vertex
    dependencyGraph.addVertex(currentObject);

    // get prerequisite objects for the current object
    List<Object> prerequisiteObjects = resolver.getDirectDependencies(name);

    // add children & edge
    if (prerequisiteObjects.isEmpty()) {
      return;
    }
    prerequisiteObjects.forEach(
                                (prerequisite) -> {
                                  dependencyGraph.addVertex(prerequisite);
                                  dependencyGraph.addEdge(currentObject, prerequisite);
                                });
  }

  // temp method for mock
  public void addObject(String name, Object currentObject, List<Object> prerequisiteObjects) {
    // null check
    requireNonNull(currentObject, "currentObject cannot be null");
    // add vertex
    dependencyGraph.addVertex(currentObject);

    // add children & edge
    if (prerequisiteObjects.isEmpty()) {
      return;
    }
    prerequisiteObjects.forEach(
                                (prerequisite) -> {
                                  dependencyGraph.addVertex(prerequisite);
                                  dependencyGraph.addEdge(currentObject, prerequisite);
                                });

  }

  @Override
  public List<Object> getSortedObjects() {
    List<Object> sortedObjects = newArrayList(new TopologicalOrderIterator<>(dependencyGraph));
    reverse(sortedObjects);

    return sortedObjects;
  }

}
