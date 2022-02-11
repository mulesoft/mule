/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal;

import static org.mule.test.allure.AllureConstants.LifecycleAndDependencyInjectionFeature.LIFECYCLE_AND_DEPENDENCY_INJECTION;
import static org.mule.test.allure.AllureConstants.LifecycleAndDependencyInjectionFeature.LifecyclePhaseStory.LIFECYCLE_PHASE_STORY;

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
import org.mule.runtime.api.util.Pair;
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

import java.util.Arrays;
import java.util.HashMap;
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

  DependencyGraphLifecycleObjectSorter sorter;
  DependencyGraphBeanDependencyResolver resolver;

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
    Map<String, Object> lookupObjects = new HashMap<>();
    lookupObjects.put("A", new DefaultStreamingManager());
    lookupObjects.put("B", new DefaultStreamingManager());
    lookupObjects.put("C", new DefaultStreamingManager());
    lookupObjects.put("D", new DefaultStreamingManager());
    sorter.setLifeCycleObjectNameOrderMap(lookupObjects);
  }


  @Test
  @Description("sort eligible components after adding one component")
  public void addOneComponentTest() {
    DefaultStreamingManager streamingManager = new DefaultStreamingManager();
    sorter.addObject("streamingManager", streamingManager);
    assertThat(sorter.getSortedObjects(), contains(streamingManager));
  }

  @Test
  @Description("components that are not eligible shouldn't be added")
  public void ignoreComponentTest() {
    String object = "str";
    sorter.addObject("string", object);
    assertThat(sorter.getSortedObjects(), empty());
  }

  @Test
  @Description("sort components after adding no component")
  public void emptyListTest() {
    assertThat(sorter.getSortedObjects(), empty());
  }


  @Test
  @Description("sort components after adding duplicates")
  public void detectDuplicateTest() {
    DefaultStreamingManager streamingManager = new DefaultStreamingManager();
    sorter.addObject("streamingManager", streamingManager);
    sorter.addObject("streamingManager", streamingManager);
    sorter.addObject("streamingManager", streamingManager);
    sorter.addObject("streamingManager", streamingManager);

    assertThat(sorter.getSortedObjects().size(), is(1));

  }

  // @Test
  // @Description("sort components for a graph with multiple levels " +
  // "(When A -> C means A depends on C, A->C and C->B should return a list B - C - A")
  // public void sortComponentsTest() {
  // DefaultStreamingManager streamingManagerA = new DefaultStreamingManager();
  // DefaultStreamingManager streamingManagerB = new DefaultStreamingManager();
  // DefaultStreamingManager streamingManagerC = new DefaultStreamingManager();
  //
  // Pair<String, Object> componentA = new Pair<>("A", new DefaultStreamingManager());
  // Pair<String, Object> componentB = new Pair<>("B", new DefaultStreamingManager());
  // Pair<String, Object> componentC = new Pair<>("C", new DefaultStreamingManager());
  //
  // List<Pair<String, Object>> directDependenciesOfA = Arrays.asList(new Pair<>("C", streamingManagerC));
  // List<Pair<String, Object>> directDependenciesOfC = Arrays.asList(new Pair<>("B", streamingManagerB));
  //
  // Map<Pair<String, Object>, List<Pair<String, Object>>> transitiveDependenciesOfA = new HashMap<>();
  // transitiveDependenciesOfA.put(componentA, directDependenciesOfA);
  // transitiveDependenciesOfA.put(componentC, directDependenciesOfC);
  //
  // Map<Pair<String, Object>, List<Pair<String, Object>>> transitiveDependenciesOfC = new HashMap<>();
  // transitiveDependenciesOfC.put(componentC, directDependenciesOfC);
  // transitiveDependenciesOfC.put(componentB, emptyList());
  //
  // when(resolver.getTransitiveDependencies("A", 5)).thenReturn(transitiveDependenciesOfA);
  // when(resolver.getTransitiveDependencies("B", 5)).thenReturn(emptyMap());
  // when(resolver.getTransitiveDependencies("C", 5)).thenReturn(transitiveDependenciesOfC);
  //
  // sorter.addObject("A", streamingManagerA);
  // sorter.addObject("B", streamingManagerB);
  // sorter.addObject("C", streamingManagerC);
  //
  // assertThat(sorter.getSortedObjects(), containsInRelativeOrder(streamingManagerB, streamingManagerC, streamingManagerA));
  // }


  @Test
  @Description("sort components when two components are sharing same prerequisite: " +
      "A -> C, B -> C: C should come before A and C should come before B")
  public void sortComponentsWithSharedChildTest() {
    DefaultStreamingManager streamingManagerA = new DefaultStreamingManager();
    DefaultStreamingManager streamingManagerB = new DefaultStreamingManager();
    DefaultStreamingManager streamingManagerC = new DefaultStreamingManager();
    Pair<String, Object> componentA = new Pair<>("A", streamingManagerA);
    Pair<String, Object> componentB = new Pair<>("B", streamingManagerB);
    Pair<String, Object> componentC = new Pair<>("C", streamingManagerC);


    List<Pair<String, Object>> depA = Arrays.asList(new Pair<>("C", streamingManagerC));
    List<Pair<String, Object>> depB = Arrays.asList(new Pair<>("C", streamingManagerC));
    Map<Pair<String, Object>, List<Pair<String, Object>>> transitiveDependenciesOfA = new HashMap<>();
    transitiveDependenciesOfA.put(componentA, depA);
    transitiveDependenciesOfA.put(componentC, emptyList());
    Map<Pair<String, Object>, List<Pair<String, Object>>> transitiveDependenciesOfB = new HashMap<>();
    transitiveDependenciesOfB.put(componentB, depB);
    transitiveDependenciesOfB.put(componentC, emptyList());

    when(resolver.getTransitiveDependencies("A", 5)).thenReturn(transitiveDependenciesOfA);
    when(resolver.getTransitiveDependencies("B", 5)).thenReturn(transitiveDependenciesOfB);
    when(resolver.getTransitiveDependencies("C", 5)).thenReturn(emptyMap());

    sorter.addObject("A", streamingManagerA);
    sorter.addObject("B", streamingManagerB);
    sorter.addObject("C", streamingManagerC);

    assertThat(sorter.getSortedObjects(), anyOf(containsInRelativeOrder(streamingManagerC, streamingManagerA),
                                                containsInRelativeOrder(streamingManagerC, streamingManagerB)));
  }

  @Test(expected = NullPointerException.class)
  @Description("If a null component is added to the graph, it will throw NullPointerException")
  public void handleNullComponentTest() {
    sorter.addObject("A", null);
  }

  @Test
  @Description("Duplicates should be ignored if added again")
  public void sortComponentsWhenAddingDuplicatesTest() {
    DefaultStreamingManager streamingManagerA = new DefaultStreamingManager();
    DefaultStreamingManager streamingManagerB = new DefaultStreamingManager();
    Pair<String, Object> componentA = new Pair<>("A", streamingManagerA);
    Pair<String, Object> componentB = new Pair<>("B", streamingManagerB);

    when(resolver.getDirectBeanDependencies(componentA, 5)).thenReturn(Arrays.asList(new Pair<>("B", streamingManagerB)));
    when(resolver.getDirectBeanDependencies(componentB, 5)).thenReturn(Arrays.asList());

    sorter.addObject("A", streamingManagerA);
    sorter.addObject("B", streamingManagerB);
    sorter.addObject("A", streamingManagerA);
    sorter.addObject("A", streamingManagerA);

    assertThat(sorter.getSortedObjects().size(), is(2));
  }


  // @Test
  // @Description("detect cycle and remove the latest edge added to the graph to avoid cycles " +
  // "(When A -> B, B -> C, C->A, the last edge that introduces cycle will be removed")
  // public void detectIndirectCycleTest() {
  // DefaultStreamingManager streamingManagerA = new DefaultStreamingManager();
  // DefaultStreamingManager streamingManagerB = new DefaultStreamingManager();
  // DefaultStreamingManager streamingManagerC = new DefaultStreamingManager();
  // Pair<String, Object> componentA = new Pair<>("A", streamingManagerA);
  // Pair<String, Object> componentB = new Pair<>("B", streamingManagerB);
  // Pair<String, Object> componentC = new Pair<>("C", streamingManagerC);
  //
  // when(resolver.getDirectBeanDependencies(componentA, 5)).thenReturn(Arrays.asList(new Pair<>("B", streamingManagerB)));
  // when(resolver.getDirectBeanDependencies(componentB, 5)).thenReturn(Arrays.asList(new Pair<>("C", streamingManagerC)));
  // when(resolver.getDirectBeanDependencies(componentC, 5)).thenReturn(Arrays.asList(new Pair<>("A", streamingManagerA)));
  //
  // sorter.addObject("A", streamingManagerA);
  // sorter.addObject("B", streamingManagerB);
  // sorter.addObject("C", streamingManagerC);
  //
  // assertThat(sorter.getSortedObjects(), containsInRelativeOrder(streamingManagerC, streamingManagerA));
  // }

  @Test
  @Description("initializable C -> not initializable D -> initialisable A-> initialisable B : should 'BAC")
  public void transitiveDependencyTest() {
    DefaultStreamingManager streamingManagerA = new DefaultStreamingManager();
    DefaultStreamingManager streamingManagerB = new DefaultStreamingManager();
    DefaultStreamingManager streamingManagerC = new DefaultStreamingManager();
    String stringD = "testD";

    Pair<String, Object> componentA = new Pair<>("A", streamingManagerA);
    Pair<String, Object> componentB = new Pair<>("B", streamingManagerB);
    Pair<String, Object> componentC = new Pair<>("C", streamingManagerC);
    Pair<String, Object> componentD = new Pair<>("D", stringD);

    List<Pair<String, Object>> depC = Arrays.asList(componentD);
    List<Pair<String, Object>> depD = Arrays.asList(componentA);
    List<Pair<String, Object>> depA = Arrays.asList(componentB);

    Map<Pair<String, Object>, List<Pair<String, Object>>> transitiveDependenciesOfA = new HashMap<>();
    transitiveDependenciesOfA.put(componentA, depA);
    transitiveDependenciesOfA.put(componentB, emptyList());

    Map<Pair<String, Object>, List<Pair<String, Object>>> transitiveDependenciesOfB = new HashMap<>();
    transitiveDependenciesOfB.put(componentB, emptyList());

    Map<Pair<String, Object>, List<Pair<String, Object>>> transitiveDependenciesOfC = new HashMap<>();
    transitiveDependenciesOfC.put(componentC, depC);
    transitiveDependenciesOfC.put(componentD, depD);
    transitiveDependenciesOfC.put(componentA, emptyList()); // already processed in the same bucket
    transitiveDependenciesOfC.put(componentB, emptyList()); // already processed in the same bucket

    when(resolver.getTransitiveDependencies("C", 5)).thenReturn(transitiveDependenciesOfC);


    sorter.addObject("A", streamingManagerA);
    sorter.addObject("B", streamingManagerB);
    sorter.addObject("C", streamingManagerC);


    System.out.println(sorter.getSortedObjects());
    assertThat(sorter.getSortedObjects(), containsInRelativeOrder(streamingManagerA, streamingManagerC));
  }
}
