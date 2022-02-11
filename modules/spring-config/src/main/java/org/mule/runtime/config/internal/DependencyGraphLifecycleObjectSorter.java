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
import org.mule.runtime.config.internal.registry.AbstractSpringRegistry;
import org.mule.runtime.config.internal.resolvers.DependencyGraphBeanDependencyResolver;
import org.mule.runtime.core.internal.lifecycle.phases.DefaultLifecycleObjectSorter;
import org.mule.runtime.core.internal.lifecycle.phases.LifecycleObjectSorter;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;

import org.jgrapht.alg.cycle.CycleDetector;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.traverse.BreadthFirstIterator;
import org.jgrapht.traverse.DepthFirstIterator;
import org.jgrapht.traverse.TopologicalOrderIterator;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Bean;

/**
 * Specialization of {@link DefaultLifecycleObjectSorter} which uses an {@link AbstractSpringRegistry} to not only consider the
 * provided objects but the beans on which that object depends on. This is accomplished by introspecting the
 * {@link BeanDefinition} that was derived from the {@link Inject} annotations. This sorter was introduced to replace
 * SpringLifecycleObjectSorter.
 *
 * @since 4.5.0
 */
public class DependencyGraphLifecycleObjectSorter implements LifecycleObjectSorter {

  private List<DefaultDirectedGraph<BeanVertexWrapper, DefaultEdge>> dependencyGraphs;
  private DependencyGraphBeanDependencyResolver resolver;
  protected Class<?>[] orderedLifecycleTypes;
  private Map<String, Integer> lifecycleObjectNameOrderMap;

  public DependencyGraphLifecycleObjectSorter(DependencyGraphBeanDependencyResolver resolver, Class<?>[] orderedLifecycleTypes) {
    this.dependencyGraphs = new ArrayList<>();
    this.resolver = resolver;
    this.orderedLifecycleTypes = orderedLifecycleTypes;
    for (int i = 0; i < orderedLifecycleTypes.length; i++) {
      DefaultDirectedGraph<BeanVertexWrapper, DefaultEdge> graph = new DefaultDirectedGraph(DefaultEdge.class);
      dependencyGraphs.add(graph);
    }
    this.lifecycleObjectNameOrderMap = new HashMap<>();
  }

  /**
   * Building a single dependency graph(bucket) for each lifecycle type
   * 
   * @param beanName
   * @param currentObject
   */
  @Override
  public void addObject(String beanName, Object currentObject) {

    requireNonNull(currentObject, "currentObject cannot be null");

    if (stream(orderedLifecycleTypes).noneMatch(x -> x.isInstance(currentObject))) {
      return;
    }

    DefaultDirectedGraph<BeanVertexWrapper, DefaultEdge> dependencyGraph = getDependencyGraphForLifecycleType(currentObject);
    CycleDetector cycleDetector = new CycleDetector(dependencyGraph);

    BeanVertexWrapper currentVertex = new BeanVertexWrapper(beanName, currentObject);
    dependencyGraph.addVertex(currentVertex);

    // get (direct) prerequisite objects for the current object
    // List<Pair<String, Object>> prerequisiteObjects = resolver.getDirectBeanDependencies(beanName);
    Map<Pair<String, Object>, List<Pair<String, Object>>> prerequisiteObjectsMap =
        resolver.getTransitiveDependencies(beanName, getDependencyGraphIndex(currentObject));

    // add direct prerequisites to the graph & create edges(current object -> prerequisite)
    for (Pair<String, Object> source : prerequisiteObjectsMap.keySet()) {
      List<Pair<String, Object>> prerequisiteObjects = prerequisiteObjectsMap.get(source);
      BeanVertexWrapper current = new BeanVertexWrapper(source.getFirst(), source.getSecond());
      dependencyGraph.addVertex(current);
      if (prerequisiteObjects.isEmpty()) {
        continue;
      }
      prerequisiteObjects.forEach(
                                  prerequisite -> {
                                    String preReqName = prerequisite.getFirst();
                                    Object preReqObject = prerequisite.getSecond();

                                    BeanVertexWrapper preReqVertex = new BeanVertexWrapper(preReqName, preReqObject);
                                    dependencyGraph.addVertex(preReqVertex);

                                    // remove any additional edge that creates cycle
                                    if (!dependencyGraph.containsEdge(preReqVertex, current)) {
                                      dependencyGraph.addEdge(current, preReqVertex);
                                      if (cycleDetector.detectCycles()) {
                                        dependencyGraph.removeEdge(current, preReqVertex);
                                      }
                                    }
                                  });
    }
  }

  /**
   * Provides the index of the graph(bucket) the current object should be added to
   * 
   * @param currentObject
   * @return
   */
  private int getDependencyGraphIndex(Object currentObject) {
    Class<?> clazz = stream(orderedLifecycleTypes).filter(x -> x.isInstance(currentObject)).findFirst().get();
    return asList(orderedLifecycleTypes).indexOf(clazz);
  }

  /**
   * Provides the graph(bucket) the current object should be added to
   * 
   * @param currentObject
   * @return
   */
  private DefaultDirectedGraph<BeanVertexWrapper, DefaultEdge> getDependencyGraphForLifecycleType(Object currentObject) {
    return dependencyGraphs.get(getDependencyGraphIndex(currentObject));
  }

  /**
   * The objects in each graph(bucket) will be sorted based on the topological order. By reversing the order, The object that
   * should be initialised/disposed first will come first.
   * 
   * @return A list with all the objects sorted based on the lifecycle type
   */
  @Override
  public List<Object> getSortedObjects() {
    List<BeanVertexWrapper> res = dependencyGraphs.stream().map(x -> {
      List<BeanVertexWrapper> sortedObjects = newArrayList(new TopologicalOrderIterator<>(x, new Comparator<BeanVertexWrapper>() {

        // tie-breaker: respect the order of lifecycleLookupObjects
        @Override
        public int compare(BeanVertexWrapper o1, BeanVertexWrapper o2) {
          if (getLifeCycleObjectNameOrderMap().getOrDefault(o1.getBeanName(), -1) > getLifeCycleObjectNameOrderMap()
              .getOrDefault(o2.getBeanName(), -1)) {
            return -1;
          } else {
            return 1;
          }
        }
      }));
      reverse(sortedObjects);
      return sortedObjects;

    }).reduce(new ArrayList<>(), (sortedObjectList, b) -> {
      for (BeanVertexWrapper v : b) {
        if (!sortedObjectList.contains(v)) {
          sortedObjectList.add(v);
        }
      }
      return sortedObjectList;

    });
    System.out.println(res.stream().map(y -> y.getBeanName()).collect(toList()));
    return res.stream().map(BeanVertexWrapper::getWrappedObject).collect(toList());

  }

  /**
   * Provides the information that will be needed for the comparison during the top sort
   * 
   * @param lookupObjectsForLifecycle lifecycle object list which is ordered based on the type
   */
  public void setLifeCycleObjectNameOrderMap(Map<String, Object> lookupObjectsForLifecycle) {
    int index = 0;
    for (Map.Entry<String, Object> entry : lookupObjectsForLifecycle.entrySet()) {
      String objectName = entry.getKey();
      lifecycleObjectNameOrderMap.put(objectName, index++);
    }
  }

  public Map<String, Integer> getLifeCycleObjectNameOrderMap() {
    return lifecycleObjectNameOrderMap;
  }


}
