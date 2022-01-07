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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.traverse.TopologicalOrderIterator;

public class DummySpringLifecycleObjectSorter implements LifecycleObjectSorter {

  private List<DefaultDirectedGraph<VertexWrapper, DefaultEdge>> dependencyGraphs;
  private DummyDependencyResolver resolver;
  private Map<String, Integer> keyHashcodeMap;
  private Set<Integer> hashcodeSet;
  protected Class<?>[] orderedLifecycleTypes;

  public DummySpringLifecycleObjectSorter(DummyDependencyResolver resolver, Class<?>[] orderedLifecycleTypes) {
    this.dependencyGraphs = new ArrayList<>();
    this.resolver = resolver;
    this.keyHashcodeMap = new HashMap<>();
    this.hashcodeSet = new HashSet<>();
    this.orderedLifecycleTypes = orderedLifecycleTypes;
    for (int i = 0; i < orderedLifecycleTypes.length; i++) {
      dependencyGraphs.add(new DefaultDirectedGraph<>(DefaultEdge.class));
    }
  }


  @Override
  public void addObject(String name, Object currentObject) {

    requireNonNull(currentObject, "currentObject cannot be null");

    if (stream(orderedLifecycleTypes).noneMatch(x -> x.isInstance(currentObject))) {
      return;
    }

    DefaultDirectedGraph<VertexWrapper, DefaultEdge> directedGraph = getDirectedGraph(currentObject);
    Map<String, Object> singleKeyHashcodeMap = new HashMap<>();
    VertexWrapper currentVertex = addVertex(name, currentObject, getDirectedGraph(currentObject), singleKeyHashcodeMap);

    // get prerequisite objects for the current object
    List<Pair<Object, String>> prerequisiteObjects = resolver.getDirectBeanDependencies(name);

    // add children & edge
    if (prerequisiteObjects.isEmpty()) {
      return;
    }
    prerequisiteObjects.forEach(
                                (prerequisite) -> {
                                  VertexWrapper preReqVertex =
                                      addVertex(prerequisite.getSecond(), prerequisite.getFirst(), directedGraph,
                                                singleKeyHashcodeMap);
                                  // todo: update the cycle check part below
                                  if (!directedGraph.containsEdge(preReqVertex, currentVertex)) {
                                    directedGraph.addEdge(currentVertex, preReqVertex);
                                  }
                                });
  }



  private int getGraphIndex(Object currentObject) {
    Class<?> clazz = asList(orderedLifecycleTypes).stream().filter(x -> x.isInstance(currentObject)).findFirst().get();
    return asList(orderedLifecycleTypes).indexOf(clazz);
  }

  private VertexWrapper addVertex(String name, Object currentObject,
                                  DefaultDirectedGraph<VertexWrapper, DefaultEdge> directedGraph,
                                  Map<String, Object> singleKeyHashcodeMap) {

    VertexWrapper currentVertex = new VertexWrapper(name, currentObject);

    if (singleKeyHashcodeMap.containsKey(name) && singleKeyHashcodeMap.containsValue(currentObject.hashCode())) {
      currentVertex = directedGraph.vertexSet().stream()
          .filter(isSameVertex(currentVertex)).collect(toList()).get(0);
    } else {
      directedGraph.addVertex(currentVertex);
      singleKeyHashcodeMap.put(name, currentObject.hashCode());
    }
    // directedGraph.addVertex(currentVertex);

    return currentVertex;
  }

  private DefaultDirectedGraph<VertexWrapper, DefaultEdge> getDirectedGraph(Object currentObject) {
    return dependencyGraphs.get(getGraphIndex(currentObject));
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
        if (!hashcodeSet.contains(o.hashCode())) {
          sortedObjectList.add(o);
          hashcodeSet.add(o.hashCode());
        }
      }
      return sortedObjectList;
    });
  }


}
