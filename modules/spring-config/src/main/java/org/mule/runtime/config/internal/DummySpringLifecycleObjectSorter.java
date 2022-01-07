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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.traverse.TopologicalOrderIterator;

public class DummySpringLifecycleObjectSorter implements LifecycleObjectSorter {

  private List<DefaultDirectedGraph<VertexWrapper, DefaultEdge>> dependencyGraphs;
  private DummyDependencyResolver resolver;
  private Set<Integer> hashcodeSet;
  protected Class<?>[] orderedLifecycleTypes;

  public DummySpringLifecycleObjectSorter(DummyDependencyResolver resolver, Class<?>[] orderedLifecycleTypes) {
    this.dependencyGraphs = new ArrayList<>();
    this.resolver = resolver;
    this.hashcodeSet = new HashSet<>();
    this.orderedLifecycleTypes = orderedLifecycleTypes;
    for (int i = 0; i < orderedLifecycleTypes.length; i++) {
      dependencyGraphs.add(new DefaultDirectedGraph<>(DefaultEdge.class));
    }
  }

  // building a single dependency graph for each lifecycle type during initialization phase
  @Override
  public void addObject(String beanName, Object currentObject) {

    requireNonNull(currentObject, "currentObject cannot be null");

    if (stream(orderedLifecycleTypes).noneMatch(x -> x.isInstance(currentObject))) {
      return;
    }

    DefaultDirectedGraph<VertexWrapper, DefaultEdge> directedGraph = getDependencyGraphForLifecycleType(currentObject);
    VertexWrapper currentVertex = addVertex(beanName, currentObject, directedGraph);

    // get (direct) prerequisite objects for the current object
    List<Pair<String, Object>> prerequisiteObjects = resolver.getDirectBeanDependencies(beanName);

    // add direct prerequisites to the graph & edges
    if (prerequisiteObjects.isEmpty()) {
      return;
    }
    prerequisiteObjects.forEach(
                                (prerequisite) -> {
                                  String preReqName = prerequisite.getFirst();
                                  Object preReqObject = prerequisite.getSecond();
                                  VertexWrapper preReqVertex =
                                      addVertex(preReqName, preReqObject, directedGraph);
                                  // todo: update the cycle check part below
                                  if (!directedGraph.containsEdge(preReqVertex, currentVertex)) {
                                    directedGraph.addEdge(currentVertex, preReqVertex);
                                  }
                                });
  }

  private int getDependencyGraphIndex(Object currentObject) {
    Class<?> clazz = stream(orderedLifecycleTypes).filter(x -> x.isInstance(currentObject)).findFirst().get();
    return asList(orderedLifecycleTypes).indexOf(clazz);
  }

  private DefaultDirectedGraph<VertexWrapper, DefaultEdge> getDependencyGraphForLifecycleType(Object currentObject) {
    return dependencyGraphs.get(getDependencyGraphIndex(currentObject));
  }

  private VertexWrapper addVertex(String name, Object currentObject,
                                  DefaultDirectedGraph<VertexWrapper, DefaultEdge> directedGraph) {

    VertexWrapper currentVertex = new VertexWrapper(name, currentObject);
    // todo: duplicate check? (example: same objects being added to same class bucket, can we prevent it with this impl?)
    directedGraph.addVertex(currentVertex);

    return currentVertex;
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
