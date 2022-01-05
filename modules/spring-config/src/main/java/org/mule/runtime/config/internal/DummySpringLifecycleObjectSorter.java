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
import org.mule.runtime.api.util.Pair;
import org.mule.runtime.core.internal.lifecycle.phases.LifecycleObjectSorter;

import java.util.*;
import java.util.stream.Collectors;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Collections.reverse;
import static java.util.Objects.requireNonNull;

public class DummySpringLifecycleObjectSorter implements LifecycleObjectSorter {

  private DefaultDirectedGraph<Object, DefaultEdge> dependencyGraph;
  private DummyDependencyResolver resolver;
  private static Map<String, Integer> keyHashcodeMap;

  public DummySpringLifecycleObjectSorter(DummyDependencyResolver resolver) {
    dependencyGraph = new DefaultDirectedGraph<>(DefaultEdge.class);
    this.resolver = resolver;
    this.keyHashcodeMap = new HashMap<>();
  }

  // vertices
  @Override
  public void addObject(String name, Object currentObject) {
    // null check
    requireNonNull(currentObject, "currentObject cannot be null");
    // add vertex
    VertexWrapper currentVertex = new VertexWrapper(currentObject);
    VertexWrapper existingVertex;
    if (keyHashcodeMap.containsKey(name) && keyHashcodeMap.containsValue(currentObject.hashCode())) {
      VertexWrapper finalCurrentVertex1 = currentVertex;
      existingVertex = (VertexWrapper) dependencyGraph.vertexSet().stream()
          .filter(x -> ((VertexWrapper) x).equals(finalCurrentVertex1)).collect(Collectors.toList()).get(0);
      currentVertex = existingVertex;
    } else {
      dependencyGraph.addVertex(currentVertex);
      keyHashcodeMap.put(name, currentObject.hashCode());
    }

    // get prerequisite objects for the current object
    // List<Object> prerequisiteObjects = resolver.getDirectDependencies(name);
    List<Pair<Object, String>> prerequisiteObjects = resolver.resolveBeanDependencies(name, true);

    // add children & edge
    if (prerequisiteObjects.isEmpty()) {
      return;
    }
    VertexWrapper finalCurrentVertex = currentVertex;
    prerequisiteObjects.forEach(
                                (prerequisite) -> {
                                  VertexWrapper preReqVertex = new VertexWrapper(prerequisite.getFirst());
                                  VertexWrapper existingPreReqVertex;
                                  if (keyHashcodeMap.containsKey(prerequisite.getSecond())
                                      && keyHashcodeMap.containsValue(prerequisite.getFirst().hashCode())) {
                                    VertexWrapper finalPreReqVertex = preReqVertex;
                                    existingPreReqVertex = (VertexWrapper) dependencyGraph.vertexSet().stream()
                                        .filter(x -> ((VertexWrapper) x).equals(finalPreReqVertex))
                                        .collect(Collectors.toList()).get(0);
                                    preReqVertex = existingPreReqVertex;
                                  } else {
                                    dependencyGraph.addVertex(preReqVertex);
                                    keyHashcodeMap.put(prerequisite.getSecond(), prerequisite.getFirst().hashCode());
                                  }
                                  // todo: fix below later (interdependent vertices)
                                  if (!dependencyGraph.containsEdge(preReqVertex, finalCurrentVertex)) {
                                    dependencyGraph.addEdge(finalCurrentVertex, preReqVertex);
                                  }
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
    return sortedObjects.stream().map(x -> ((VertexWrapper) x).getOriginalObject()).collect(Collectors.toList());
  }

}
