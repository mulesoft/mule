/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal;

import static java.util.Arrays.asList;
import static java.util.Arrays.stream;
import static java.util.Collections.reverse;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;

import static com.google.common.collect.Lists.newArrayList;

import org.mule.runtime.api.util.Pair;
import org.mule.runtime.core.internal.lifecycle.phases.LifecycleObjectSorter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.traverse.TopologicalOrderIterator;

public class DummySpringLifecycleObjectSorter implements LifecycleObjectSorter {

  private List<DefaultDirectedGraph<VertexWrapper, DefaultEdge>> dependencyGraphs;
  private DummyDependencyResolver resolver;
  private Map<String, Integer> keyHashcodeMap;
  protected Class<?>[] orderedLifecycleTypes;

  public DummySpringLifecycleObjectSorter(DummyDependencyResolver resolver, Class<?>[] orderedLifecycleTypes) {
    this.dependencyGraphs = new ArrayList<>();
    for (int i = 0; i < 12; i++) {
      dependencyGraphs.add(new DefaultDirectedGraph<>(DefaultEdge.class));
    }

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
    DefaultDirectedGraph<VertexWrapper, DefaultEdge> directedGraph = getDirectedGraph(currentObject);
    VertexWrapper currentVertex = addVertex(name, currentObject, getDirectedGraph(currentObject));

    // get prerequisite objects for the current object
    List<Pair<Object, String>> prerequisiteObjects = resolver.getDirectBeanDependencies(name);

    // add children & edge
    if (prerequisiteObjects.isEmpty()) {
      return;
    }
    prerequisiteObjects.forEach(
                                (prerequisite) -> {
                                  VertexWrapper preReqVertex =
                                      addVertex(prerequisite.getSecond(), prerequisite.getFirst(), directedGraph);
                                  // todo: fix below later (interdependent vertices)
                                  if (!directedGraph.containsEdge(preReqVertex, currentVertex)) {
                                    directedGraph.addEdge(currentVertex, preReqVertex);
                                  }
                                });
  }

  private int getBucketIndex(Object currentObject) {
    Class<?> clazz = asList(orderedLifecycleTypes).stream().filter(x -> x.isInstance(currentObject)).findFirst().get();
    return asList(orderedLifecycleTypes).indexOf(clazz);
  }

  private VertexWrapper addVertex(String name, Object currentObject,
                                  DefaultDirectedGraph<VertexWrapper, DefaultEdge> directedGraph) {

    VertexWrapper currentVertex = new VertexWrapper(currentObject);

    directedGraph.addVertex(currentVertex);

    return currentVertex;
  }

  private DefaultDirectedGraph<VertexWrapper, DefaultEdge> getDirectedGraph(Object currentObject) {
    return dependencyGraphs.get(getBucketIndex(currentObject));
  }

  private Predicate<VertexWrapper> isSameVertex(VertexWrapper currentVertex) {
    return x -> x.equals(currentVertex);
  }

  @Override
  public List<Object> getSortedObjects() {
    return dependencyGraphs.stream().map(x -> {
      List<Object> sortedObjects = newArrayList(new TopologicalOrderIterator<>(x));
      reverse(sortedObjects);
      return sortedObjects.stream().map(v -> ((VertexWrapper) v).getWrappedObject()).collect(toList());
    }).reduce(new ArrayList<>(), (sortedObjectList, b) -> {
      for (Object o : b) {
        if (!sortedObjectList.contains(o)) {
          sortedObjectList.add(o);
        }
      }
      return sortedObjectList;
    });
  }



  // temp method for mock
  public void addObject(String name, Object currentObject, List<Object> prerequisiteObjects) {
    // null check
    requireNonNull(currentObject, "currentObject cannot be null");

    DefaultDirectedGraph<VertexWrapper, DefaultEdge> directedGraph = getDirectedGraph(currentObject);
    // add vertex
    directedGraph.addVertex((VertexWrapper) currentObject);

    // add children & edge
    if (prerequisiteObjects.isEmpty()) {
      return;
    }
    prerequisiteObjects.forEach(
                                (prerequisite) -> {
                                  directedGraph.addVertex((VertexWrapper) prerequisite);
                                  directedGraph.addEdge((VertexWrapper) currentObject, (VertexWrapper) prerequisite);
                                });

  }


}
