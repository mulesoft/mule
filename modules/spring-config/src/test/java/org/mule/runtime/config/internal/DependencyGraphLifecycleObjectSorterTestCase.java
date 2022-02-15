/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal;

import static org.mule.test.allure.AllureConstants.LifecycleAndDependencyInjectionFeature.LIFECYCLE_AND_DEPENDENCY_INJECTION;
import static org.mule.test.allure.AllureConstants.LifecycleAndDependencyInjectionFeature.LifecyclePhaseStory.LIFECYCLE_PHASE_STORY;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInRelativeOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.mule.runtime.api.el.ExpressionLanguage;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lock.LockFactory;
import org.mule.runtime.api.store.ObjectStoreManager;
import org.mule.runtime.config.internal.resolvers.DependencyGraphBeanDependencyResolver;
import org.mule.runtime.core.api.config.Config;
import org.mule.runtime.core.api.config.MuleConfiguration;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.security.SecurityManager;
import org.mule.runtime.core.api.streaming.DefaultStreamingManager;
import org.mule.runtime.core.api.streaming.StreamingManager;
import org.mule.runtime.core.api.util.queue.QueueManager;
import org.mule.runtime.core.internal.el.mvel.ExpressionLanguageExtension;
import org.mule.runtime.extension.api.runtime.config.ConfigurationProvider;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Story;
import org.junit.Before;
import org.junit.Test;

@Issue("MULE-19984")
@Feature(LIFECYCLE_AND_DEPENDENCY_INJECTION)
@Story(LIFECYCLE_PHASE_STORY)
public class DependencyGraphLifecycleObjectSorterTestCase {

  private DependencyGraphLifecycleObjectSorter sorter;
  private DependencyGraphBeanDependencyResolver resolver;
  private DefaultStreamingManager objectA;
  private DefaultStreamingManager objectB;
  private DefaultStreamingManager objectC;
  private List<String> lookupObjects;

  @Before
  public void setUp() throws Exception {
    resolver = mock(DependencyGraphBeanDependencyResolver.class);
    sorter = new DependencyGraphLifecycleObjectSorter(resolver, new Class<?>[] {
        LockFactory.class,
        ObjectStoreManager.class,
        ExpressionLanguageExtension.class,
        ExpressionLanguage.class,
        QueueManager.class,
        StreamingManager.class,
        ConfigurationProvider.class,
        Config.class,
        SecurityManager.class,
        FlowConstruct.class,
        MuleConfiguration.class,
        Initialisable.class
    });
    lookupObjects = new ArrayList<>();
    objectA = new DefaultStreamingManager();
    objectB = new DefaultStreamingManager();
    objectC = new DefaultStreamingManager();
    lookupObjects.add("objectA");
    lookupObjects.add("objectB");
    lookupObjects.add("objectC");
    sorter.setLifeCycleObjectNameOrder(lookupObjects);
  }


  @Test
  @Description("When one of the 12 lifecycle type objects is added, the object should be on the final list.")
  public void addOneLifecycleTypeObjectTest() {
    DefaultStreamingManager lifecycleObject = new DefaultStreamingManager();
    sorter.addObject("lifecycleObject", lifecycleObject);
    assertThat(sorter.getSortedObjects(), contains(lifecycleObject));
  }

  @Test
  @Description("When an object that isn't on the ignored types list is added, it shouldn't be on the final list.")
  public void addIgnoredObjectTest() {
    String ignoredObject = "string";
    sorter.addObject("ignoredObject", ignoredObject);
    assertThat(sorter.getSortedObjects(), empty());
  }

  @Test
  @Description("Sort components without adding any components.")
  public void emptyListTest() {
    assertThat(sorter.getSortedObjects(), empty());
  }


  @Test
  @Description("Sort components after adding duplicate components.")
  public void detectDuplicateComponentsTest() {
    DefaultStreamingManager sameComponent = new DefaultStreamingManager();
    sorter.addObject("sameComponent", sameComponent);
    sorter.addObject("sameComponent", sameComponent);
    sorter.addObject("sameComponent", sameComponent);
    sorter.addObject("sameComponent", sameComponent);

    assertThat(sorter.getSortedObjects().size(), is(1));

  }

  @Test
  @Description("Sort components for a graph with multiple levels " +
      "(When A -> C means A depends on C, A->C and C->B should return a list B - C - A.")
  public void sortComponentsTest() {
    BeanWrapper componentA = new BeanWrapper("objectA", objectA);
    BeanWrapper componentB = new BeanWrapper("objectB", objectB);
    BeanWrapper componentC = new BeanWrapper("objectC", objectC);

    List<BeanWrapper> directDependenciesOfA = asList(componentC);
    List<BeanWrapper> directDependenciesOfC = asList(componentB);

    Map<BeanWrapper, List<BeanWrapper>> transitiveDependenciesOfA = new LinkedHashMap<>();
    transitiveDependenciesOfA.put(componentA, directDependenciesOfA);
    transitiveDependenciesOfA.put(componentC, directDependenciesOfC);

    Map<BeanWrapper, List<BeanWrapper>> transitiveDependenciesOfB = new LinkedHashMap<>();
    transitiveDependenciesOfB.put(componentB, emptyList()); // no dependencies

    Map<BeanWrapper, List<BeanWrapper>> transitiveDependenciesOfC = new LinkedHashMap<>();
    transitiveDependenciesOfC.put(componentC, emptyList()); // already processed for this bucket

    when(resolver.getTransitiveDependencies("objectA", 5)).thenReturn(transitiveDependenciesOfA);
    when(resolver.getTransitiveDependencies("objectB", 5)).thenReturn(transitiveDependenciesOfB);
    when(resolver.getTransitiveDependencies("objectC", 5)).thenReturn(transitiveDependenciesOfC);

    sorter.addObject("objectA", objectA);
    sorter.addObject("objectB", objectB);
    sorter.addObject("objectC", objectC);

    assertThat(sorter.getSortedObjects(), containsInRelativeOrder(objectB, objectC, objectA));
  }


  @Test
  @Description("Sort components when two components are sharing the same prerequisite. " +
      "A -> C, B -> C: C should be initialized before A and B.")
  public void sortComponentsWithSharedChildTest() {
    BeanWrapper componentA = new BeanWrapper("objectA", objectA);
    BeanWrapper componentB = new BeanWrapper("objectB", objectB);
    BeanWrapper componentC = new BeanWrapper("objectC", objectC);

    List<BeanWrapper> dependenciesOfA = asList(componentC);
    List<BeanWrapper> dependenciesOfB = asList(componentC);

    Map<BeanWrapper, List<BeanWrapper>> transitiveDependenciesOfA = new HashMap<>();
    transitiveDependenciesOfA.put(componentA, dependenciesOfA);
    transitiveDependenciesOfA.put(componentC, emptyList());
    Map<BeanWrapper, List<BeanWrapper>> transitiveDependenciesOfB = new HashMap<>();
    transitiveDependenciesOfB.put(componentB, dependenciesOfB);
    transitiveDependenciesOfB.put(componentC, emptyList());

    when(resolver.getTransitiveDependencies("objectA", 5)).thenReturn(transitiveDependenciesOfA);
    when(resolver.getTransitiveDependencies("objectB", 5)).thenReturn(transitiveDependenciesOfB);
    when(resolver.getTransitiveDependencies("objectC", 5)).thenReturn(emptyMap());
    sorter.setLifeCycleObjectNameOrder(lookupObjects);

    sorter.addObject("objectA", objectA);
    sorter.addObject("objectB", objectB);
    sorter.addObject("objectC", objectC);

    assertThat(sorter.getSortedObjects(), anyOf(containsInRelativeOrder(objectC, objectA),
                                                containsInRelativeOrder(objectC, objectB)));
  }

  @Test(expected = NullPointerException.class)
  @Description("If a null component is added to the graph, it will throw NullPointerException.")
  public void handleNullObjectTest() {
    sorter.addObject("objectA", null);
  }

  @Test
  @Description("Duplicate components should be ignored if added again")
  public void sortComponentsWhenAddingDuplicatesTest() {
    BeanWrapper componentA = new BeanWrapper("objectA", objectA);
    BeanWrapper componentB = new BeanWrapper("objectB", objectB);

    when(resolver.getDirectBeanDependencies(componentA, 5)).thenReturn(asList(componentB));
    when(resolver.getDirectBeanDependencies(componentB, 5)).thenReturn(asList());

    sorter.addObject("objectA", objectA);
    sorter.addObject("objectB", objectB);
    sorter.addObject("objectA", objectA);
    sorter.addObject("objectA", objectA);

    assertThat(sorter.getSortedObjects().size(), is(2));
  }


  @Test
  @Description("Detect cycles and remove the latest edge added to the graph to use top sort. " +
      "(When A -> B, B -> C, C -> A, the last edge that creates a cycle will be removed.")
  public void detectIndirectCycleTest() {
    BeanWrapper componentA = new BeanWrapper("objectA", objectA);
    BeanWrapper componentB = new BeanWrapper("objectB", objectB);
    BeanWrapper componentC = new BeanWrapper("objectC", objectC);

    List<BeanWrapper> dependenciesOfA = asList(componentB);
    List<BeanWrapper> dependenciesOfB = asList(componentC);
    List<BeanWrapper> dependenciesOfC = asList(componentA);


    Map<BeanWrapper, List<BeanWrapper>> transitiveDependenciesOfA = new LinkedHashMap<>();
    transitiveDependenciesOfA.put(componentA, dependenciesOfA);
    transitiveDependenciesOfA.put(componentB, dependenciesOfB);
    transitiveDependenciesOfA.put(componentC, dependenciesOfC);

    Map<BeanWrapper, List<BeanWrapper>> transitiveDependenciesOfB = new LinkedHashMap<>();
    transitiveDependenciesOfB.put(componentB, emptyList());

    Map<BeanWrapper, List<BeanWrapper>> transitiveDependenciesOfC = new LinkedHashMap<>();
    transitiveDependenciesOfC.put(componentC, emptyList());
    // components that were already processed won't be added to the map.


    when(resolver.getDirectBeanDependencies(componentA, 5)).thenReturn(dependenciesOfA);
    when(resolver.getDirectBeanDependencies(componentB, 5)).thenReturn(dependenciesOfB);
    when(resolver.getDirectBeanDependencies(componentC, 5)).thenReturn(dependenciesOfC);

    when(resolver.getTransitiveDependencies("objectA", 5)).thenReturn(transitiveDependenciesOfA);
    when(resolver.getTransitiveDependencies("objectB", 5)).thenReturn(transitiveDependenciesOfB);
    when(resolver.getTransitiveDependencies("objectC", 5)).thenReturn(transitiveDependenciesOfC);

    sorter.setLifeCycleObjectNameOrder(lookupObjects);

    sorter.addObject("objectA", objectA);
    sorter.addObject("objectB", objectB);
    sorter.addObject("objectC", objectC);

    assertThat(sorter.getSortedObjects(), containsInRelativeOrder(objectC, objectA));
  }

  @Test
  @Description("When adding C -> (non initializable) D -> A-> B, the order of initialisables should be BAC.")
  public void transitiveDependenciesTest() {
    String objectD = "objectD";

    BeanWrapper componentA = new BeanWrapper("objectA", objectA);
    BeanWrapper componentB = new BeanWrapper("objectB", objectB);
    BeanWrapper componentC = new BeanWrapper("objectC", objectC);
    BeanWrapper componentD = new BeanWrapper("objectD", objectD);

    List<BeanWrapper> dependenciesOfA = asList(componentB);
    List<BeanWrapper> dependenciesOfC = asList(componentD);
    List<BeanWrapper> dependenciesOfD = asList(componentA);

    Map<BeanWrapper, List<BeanWrapper>> transitiveDependenciesOfA = new LinkedHashMap<>();
    transitiveDependenciesOfA.put(componentA, dependenciesOfA);
    transitiveDependenciesOfA.put(componentB, emptyList());

    Map<BeanWrapper, List<BeanWrapper>> transitiveDependenciesOfB = new LinkedHashMap<>();
    transitiveDependenciesOfB.put(componentB, emptyList());

    Map<BeanWrapper, List<BeanWrapper>> transitiveDependenciesOfC = new LinkedHashMap<>();
    transitiveDependenciesOfC.put(componentC, dependenciesOfC);
    transitiveDependenciesOfC.put(componentD, dependenciesOfD);
    transitiveDependenciesOfC.put(componentA, emptyList()); // already processed in the same bucket
    transitiveDependenciesOfC.put(componentB, emptyList()); // already processed in the same bucket


    when(resolver.getDirectBeanDependencies(componentA, 5)).thenReturn(dependenciesOfA);
    when(resolver.getDirectBeanDependencies(componentB, 5)).thenReturn(emptyList());
    when(resolver.getDirectBeanDependencies(componentC, 5)).thenReturn(dependenciesOfC);
    when(resolver.getDirectBeanDependencies(componentD, 5)).thenReturn(dependenciesOfD);
    when(resolver.getTransitiveDependencies("objectA", 5)).thenReturn(transitiveDependenciesOfA);
    when(resolver.getTransitiveDependencies("objectB", 5)).thenReturn(transitiveDependenciesOfB);
    when(resolver.getTransitiveDependencies("objectC", 5)).thenReturn(transitiveDependenciesOfC);
    sorter.setLifeCycleObjectNameOrder(lookupObjects);

    sorter.addObject("objectA", objectA);
    sorter.addObject("objectB", objectB);
    sorter.addObject("objectC", objectC);

    assertThat(sorter.getSortedObjects(), containsInRelativeOrder(objectB, objectA, objectC));
  }
}
