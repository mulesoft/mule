/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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

import org.mule.runtime.config.internal.registry.AbstractSpringRegistry;
import org.mule.runtime.config.internal.resolvers.DependencyGraphBeanDependencyResolver;
import org.mule.runtime.core.internal.lifecycle.RegistryLifecycleManager;
import org.mule.runtime.core.internal.lifecycle.phases.DefaultLifecycleObjectSorter;
import org.mule.runtime.core.internal.lifecycle.phases.LifecycleObjectSorter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jgrapht.alg.connectivity.ConnectivityInspector;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.traverse.TopologicalOrderIterator;
import org.springframework.beans.factory.config.BeanDefinition;

import jakarta.inject.Inject;

/**
 * Specialization of {@link DefaultLifecycleObjectSorter} which uses an {@link AbstractSpringRegistry} to not only consider the
 * provided objects but the beans on which that object depends on. This is accomplished by introspecting the
 * {@link BeanDefinition} that was derived from the {@link Inject} annotations. This sorter was introduced to replace
 * SpringLifecycleObjectSorter.
 *
 * @since 4.5.0
 */
public class DependencyGraphLifecycleObjectSorter implements LifecycleObjectSorter {

  private List<DefaultDirectedGraph<BeanWrapper, DefaultEdge>> dependencyGraphs;
  private List<DefaultDirectedGraph<BeanWrapper, DefaultEdge>> reverseGraphs;
  private DependencyGraphBeanDependencyResolver resolver;
  protected final Class<?>[] orderedLifecycleTypes;
  private Map<String, Integer> lifecycleObjectNameOrderMap;

  public DependencyGraphLifecycleObjectSorter(DependencyGraphBeanDependencyResolver resolver, Class<?>[] orderedLifecycleTypes) {
    this.dependencyGraphs = new ArrayList<>(orderedLifecycleTypes.length);
    this.reverseGraphs = new ArrayList<>(orderedLifecycleTypes.length);
    this.resolver = resolver;
    this.orderedLifecycleTypes = orderedLifecycleTypes;
    for (int i = 0; i < orderedLifecycleTypes.length; i++) {
      DefaultDirectedGraph<BeanWrapper, DefaultEdge> graph = new DefaultDirectedGraph<>(DefaultEdge.class);
      dependencyGraphs.add(graph);
      DefaultDirectedGraph<BeanWrapper, DefaultEdge> reverseGraph = new DefaultDirectedGraph<>(DefaultEdge.class);
      reverseGraphs.add(reverseGraph);
    }
    this.lifecycleObjectNameOrderMap = new HashMap<>();
  }

  /**
   * Building a single dependency graph(bucket) for each lifecycle type
   *
   * @param beanName      current object(bean)'s name to resolve dependencies
   * @param currentObject current object that is going to be added to the graph(bucket)
   */
  @Override
  public void addObject(String beanName, Object currentObject) {

    requireNonNull(currentObject, "currentObject cannot be null");

    if (stream(orderedLifecycleTypes).noneMatch(x -> x.isInstance(currentObject))) {
      return;
    }
    int graphIndex = getDependencyGraphIndex(currentObject);

    DefaultDirectedGraph<BeanWrapper, DefaultEdge> dependencyGraph = getDependencyGraphForLifecycleType(graphIndex);
    DefaultDirectedGraph<BeanWrapper, DefaultEdge> reverseGraph = getReverseGraphForLifecycleType(graphIndex);
    ConnectivityInspector pathInspector = new ConnectivityInspector(reverseGraph);

    BeanWrapper currentVertex = new BeanWrapper(beanName, currentObject);
    dependencyGraph.addVertex(currentVertex);
    reverseGraph.addVertex(currentVertex);

    // get (direct) prerequisite objects for the current object
    Map<BeanWrapper, List<BeanWrapper>> prerequisiteObjectsMap =
        resolver.getTransitiveDependencies(beanName, graphIndex);

    // add direct prerequisites to the graph & create edges(current object -> prerequisite)
    for (BeanWrapper source : prerequisiteObjectsMap.keySet()) {
      List<BeanWrapper> prerequisiteObjects = prerequisiteObjectsMap.get(source);
      BeanWrapper current = new BeanWrapper(source.getName(), source.getWrappedObject());
      dependencyGraph.addVertex(current);
      reverseGraph.addVertex(current);

      if (prerequisiteObjects.isEmpty()) {
        continue;
      }
      prerequisiteObjects.forEach(
                                  prerequisite -> {
                                    String preReqName = prerequisite.getName();
                                    Object preReqObject = prerequisite.getWrappedObject();

                                    BeanWrapper preReqVertex = new BeanWrapper(preReqName, preReqObject);
                                    dependencyGraph.addVertex(preReqVertex);
                                    reverseGraph.addVertex(preReqVertex);

                                    // check if a path exists in the edge reversed graph to avoid cycles
                                    // (W-10704588 : tested multiple options for faster cycle detection)
                                    if (!pathInspector.pathExists(current, preReqVertex)) {
                                      reverseGraph.addEdge(preReqVertex, current);
                                      dependencyGraph.addEdge(current, preReqVertex);
                                    }
                                  });
    }
  }

  /**
   * Provides the index of the graph(bucket) the current object should be added to
   * 
   * @param currentObject current object that is going to be added to the graph(bucket)
   * @return index of the relevant dependency graph(bucket)
   */
  private int getDependencyGraphIndex(Object currentObject) {
    for (int index = 0; index < orderedLifecycleTypes.length; index++) {
      if (orderedLifecycleTypes[index].isInstance(currentObject)) {
        return index;
      }
    }
    return orderedLifecycleTypes.length - 1;
  }

  /**
   * Provides the graph(bucket) the current object should be added to
   * 
   * @param graphIndex index of the graph for the current object
   * @return relevant dependency graph(bucket)
   */
  private DefaultDirectedGraph<BeanWrapper, DefaultEdge> getDependencyGraphForLifecycleType(int graphIndex) {
    return dependencyGraphs.get(graphIndex);
  }

  /**
   * Provides the edge reversed graph(bucket) the current object should be added to
   *
   * @param graphIndex index of the graph for the current object
   * @return edge reversed dependency graph(bucket)
   */
  private DefaultDirectedGraph<BeanWrapper, DefaultEdge> getReverseGraphForLifecycleType(int graphIndex) {
    return reverseGraphs.get(graphIndex);
  }

  /**
   * The objects in each graph(bucket) will be sorted based on the topological order. By reversing the order, The object that
   * should be initialised/disposed first will come first.
   * 
   * @return A list with all the objects sorted based on the lifecycle type
   */
  @Override
  public List<Object> getSortedObjects() {
    List<BeanWrapper> res = dependencyGraphs.stream().map(graph -> {

      List<BeanWrapper> sortedObjects = newArrayList(new TopologicalOrderIterator<>(graph, (o1, o2) -> {
        if (getLifeCycleObjectNameOrder().getOrDefault(o1.getName(), -1) > getLifeCycleObjectNameOrder()
            .getOrDefault(o2.getName(), -1)) {
          return -1;
        } else {
          return 1;
        }
      }));
      reverse(sortedObjects);
      return sortedObjects;
    }).reduce(new ArrayList<>(), (sortedObjectList, b) -> {
      for (BeanWrapper v : b) {
        if (!sortedObjectList.contains(v)) {
          sortedObjectList.add(v);
        }
      }
      return sortedObjectList;

    });

    return res.stream().map(BeanWrapper::getWrappedObject).collect(toList());

  }

  /**
   * Provides the information that will be needed for the comparison during the top sort
   *
   * @param lookupObjects lifecycle object list which is ordered based on the type
   */
  @Override
  public void setLifeCycleObjectNameOrder(List<String> lookupObjects) {
    int index = 0;
    for (String objectName : lookupObjects) {
      lifecycleObjectNameOrderMap.put(objectName, index++);
    }
  }

  /**
   * Provides the information about the lookup order of objects that should be initialized
   *
   * @return map with the order of objects that should be initialized from {@link RegistryLifecycleManager}'s lookup
   */
  private Map<String, Integer> getLifeCycleObjectNameOrder() {
    return lifecycleObjectNameOrderMap;
  }

}
