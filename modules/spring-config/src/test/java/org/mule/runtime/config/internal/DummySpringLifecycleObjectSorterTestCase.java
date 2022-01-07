/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal;

import io.qameta.allure.Description;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mule.runtime.api.el.ExpressionLanguage;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.store.ObjectStore;
import org.mule.runtime.core.api.config.Config;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.security.SecurityManager;
import org.mule.runtime.core.api.streaming.StreamingManager;
import org.mule.runtime.extension.api.runtime.config.ConfigurationProvider;

import java.util.Arrays;

import static org.hamcrest.MatcherAssert.assertThat;


public class DummySpringLifecycleObjectSorterTestCase {

  DummySpringLifecycleObjectSorter sorter;
  DummyDependencyResolver resolver;

  @Before
  public void setUp() throws Exception {
    resolver = Mockito.mock(DummyDependencyResolver.class);
    sorter = new DummySpringLifecycleObjectSorter(resolver, new Class<?>[] {
        StreamingManager.class,
        ObjectStore.class,
        ExpressionLanguage.class,
        ConfigurationProvider.class,
        Config.class,
        SecurityManager.class,
        FlowConstruct.class,
        Initialisable.class,
        String.class,
        VertexWrapper.class
    });
  }


  @Test
  @Description("sort components after adding one component")
  public void addOneComponentTest() {
    // Given
    Object dummyA = new Object();
    // When
    sorter.addObject("A", dummyA);
    // Then
    assertThat(sorter.getSortedObjects(), Matchers.contains(dummyA));
  }

  @Test
  @Description("sort components after adding no component")
  public void emptyListTest() {
    // Given
    // When
    // Then
    assertThat(sorter.getSortedObjects(), Matchers.empty());
  }


  @Test
  @Description("sort components after adding duplicates")
  public void detectDuplicateTest() {
    // Given
    Object dummyA = new Object();
    // When
    sorter.addObject("A", dummyA);
    sorter.addObject("A", dummyA);
    sorter.addObject("A", dummyA);
    sorter.addObject("A", dummyA);

    assertThat(sorter.getSortedObjects().size(), Matchers.is(1));

  }

  // @Test
  // @Description("sort components for a graph with multiple levels " +
  // "(When A -> C means A depends on C, A->C and C->B should return a list B - C - A")
  // public void sortComponentsTest() {
  // // given
  // Object dummyA = new Object();
  // Object dummyB = new Object();
  // Object dummyC = new Object();
  //
  // Mockito.when(resolver.getDirectDependencies("A")).thenReturn(Arrays.asList(dummyC));
  // Mockito.when(resolver.getDirectDependencies("B")).thenReturn(Arrays.asList());
  // Mockito.when(resolver.getDirectDependencies("C")).thenReturn(Arrays.asList(dummyB));
  //
  // // when
  // sorter.addObject(dummyA, resolver.getDirectDependencies("A"));
  // sorter.addObject(dummyB, resolver.getDirectDependencies("B"));
  // sorter.addObject(dummyC, resolver.getDirectDependencies("C"));
  //
  // assertThat(sorter.getSortedObjects(), Matchers.containsInRelativeOrder(dummyB, dummyC, dummyA));
  // }


  // @Test
  // @Description("sort components when two components are sharing same prerequisite: " +
  // "A -> C, B -> C: C should come before A and C should come before B")
  // public void sortComponentsWithSharedChildTest() {
  // // given
  // Object dummyA = new Object();
  // Object dummyB = new Object();
  // Object dummyC = new Object();
  //
  // Mockito.when(resolver.getDirectDependencies("A")).thenReturn(Arrays.asList(dummyC));
  // Mockito.when(resolver.getDirectDependencies("B")).thenReturn(Arrays.asList(dummyC));
  // Mockito.when(resolver.getDirectDependencies("C")).thenReturn(Arrays.asList());
  //
  // // when
  // sorter.addObject(dummyA, resolver.getDirectDependencies("A"));
  // sorter.addObject(dummyB, resolver.getDirectDependencies("B"));
  // sorter.addObject(dummyC, resolver.getDirectDependencies("C"));
  //
  // assertThat(sorter.getSortedObjects(), Matchers.anyOf(Matchers.containsInRelativeOrder(dummyC, dummyA),
  // Matchers.containsInRelativeOrder(dummyC, dummyB)));
  // }

  @Test(expected = NullPointerException.class)
  @Description("If a null component is added to the graph, it will throw NullPointerException")
  public void handleNullComponentTest() {
    sorter.addObject("A", null);
  }

  // @Test
  // @Description("Duplicates should be ignored if added again")
  // public void sortComponentsWhenAddingDuplicatesTest() {
  // // given
  // Object dummyA = new Object();
  // Object dummyB = new Object();
  //
  // Mockito.when(resolver.getDirectDependencies("A")).thenReturn(Arrays.asList(dummyB));
  // Mockito.when(resolver.getDirectDependencies("B")).thenReturn(Arrays.asList());
  //
  // // when
  // sorter.addObject(dummyA, resolver.getDirectDependencies("A"));
  // sorter.addObject(dummyB, resolver.getDirectDependencies("B"));
  // sorter.addObject(dummyA, resolver.getDirectDependencies("A"));
  // sorter.addObject(dummyA, resolver.getDirectDependencies("A"));
  //
  // assertThat(sorter.getSortedObjects().size(), Matchers.is(2));
  // }

  // todo:find addVertex and replace input with VertexWrapper

}
