/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal;

import static java.util.Arrays.stream;
import static java.util.Collections.reverse;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;

import static com.google.common.collect.Lists.newArrayList;

import org.mule.runtime.api.util.Pair;
import org.mule.runtime.core.internal.lifecycle.phases.LifecycleObjectSorter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.traverse.TopologicalOrderIterator;

public class DummySpringLifecycleObjectSorter implements LifecycleObjectSorter {

  private DefaultDirectedGraph<VertexWrapper, DefaultEdge> dependencyGraph;
  private DummyDependencyResolver resolver;
  private Map<String, Integer> keyHashcodeMap;
  protected Class<?>[] orderedLifecycleTypes;

  public DummySpringLifecycleObjectSorter(DummyDependencyResolver resolver, Class<?>[] orderedLifecycleTypes) {
    this.dependencyGraph = new DefaultDirectedGraph<>(DefaultEdge.class);
    this.resolver = resolver;
    this.keyHashcodeMap = new HashMap<>();
    this.orderedLifecycleTypes = orderedLifecycleTypes;
  }

  // add vertices
  @Override
  public void addObject(String name, Object currentObject) {
    // null check
    requireNonNull(currentObject, "currentObject cannot be null");

    if (stream(orderedLifecycleTypes).noneMatch(x -> x.isInstance(currentObject))) {
      return;
    }
    doAddObject(name, currentObject);
  }

  private void doAddObject(String name, Object currentObject) {
    VertexWrapper currentVertex = addVertex(name, currentObject);

    // get prerequisite objects for the current object
    List<Pair<Object, String>> prerequisiteObjects = resolver.getDirectBeanDependencies(name);

    // add children & edge
    if (prerequisiteObjects.isEmpty()) {
      return;
    }
    prerequisiteObjects.forEach(
                                (prerequisite) -> {
                                  VertexWrapper preReqVertex = addVertex(prerequisite.getSecond(), prerequisite.getFirst());
                                  // todo: fix below later (interdependent vertices)
                                  if (!dependencyGraph.containsEdge(preReqVertex, currentVertex)) {
                                    dependencyGraph.addEdge(currentVertex, preReqVertex);
                                  }
                                });
  }

  private VertexWrapper addVertex(String name, Object currentObject) {
    VertexWrapper currentVertex = new VertexWrapper(currentObject);

    if (keyHashcodeMap.containsKey(name) && keyHashcodeMap.containsValue(currentObject.hashCode())) {
      currentVertex = dependencyGraph.vertexSet().stream()
          .filter(isSameVertex(currentVertex)).collect(toList()).get(0);
    } else {
      dependencyGraph.addVertex(currentVertex);
      keyHashcodeMap.put(name, currentObject.hashCode());
    }
    return currentVertex;
  }

  private Predicate<VertexWrapper> isSameVertex(VertexWrapper currentVertex) {
    return x -> x.equals(currentVertex);
  }

  @Override
  public List<Object> getSortedObjects() {
    List<Object> sortedObjects = newArrayList(new TopologicalOrderIterator<>(dependencyGraph));
    reverse(sortedObjects);
    return sortedObjects.stream().map(x -> ((VertexWrapper) x).getWrappedObject()).collect(toList());
  }



  // temp method for mock
  public void addObject(String name, Object currentObject, List<Object> prerequisiteObjects) {
    // null check
    requireNonNull(currentObject, "currentObject cannot be null");
    // add vertex
    dependencyGraph.addVertex((VertexWrapper) currentObject);

    // add children & edge
    if (prerequisiteObjects.isEmpty()) {
      return;
    }
    prerequisiteObjects.forEach(
                                (prerequisite) -> {
                                  dependencyGraph.addVertex((VertexWrapper) prerequisite);
                                  dependencyGraph.addEdge((VertexWrapper) currentObject, (VertexWrapper) prerequisite);
                                });

  }


}
