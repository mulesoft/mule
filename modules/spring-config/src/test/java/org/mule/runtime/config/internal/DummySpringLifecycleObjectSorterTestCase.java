/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInRelativeOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;

import org.mule.runtime.api.el.ExpressionLanguage;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lock.LockFactory;
import org.mule.runtime.api.store.ObjectStoreManager;
import org.mule.runtime.api.util.Pair;
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

import io.qameta.allure.Description;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;


public class DummySpringLifecycleObjectSorterTestCase {

  DummySpringLifecycleObjectSorter sorter;
  DummyDependencyResolver resolver;

  @Before
  public void setUp() throws Exception {
    resolver = Mockito.mock(DummyDependencyResolver.class);
    sorter = new DummySpringLifecycleObjectSorter(resolver, new Class<?>[] {
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
  }


  @Test
  @Description("sort eligible components after adding one component")
  public void addOneComponentTest() {
    DefaultStreamingManager streamingManager = new DefaultStreamingManager();
    sorter.addObject("streamingManager", streamingManager);
    assertThat(sorter.getSortedObjects(), contains(streamingManager));
  }

  // todo: addObject w/ different classes

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

  @Test
  @Description("sort components for a graph with multiple levels " +
      "(When A -> C means A depends on C, A->C and C->B should return a list B - C - A")
  public void sortComponentsTest() {
    DefaultStreamingManager streamingManagerA = new DefaultStreamingManager();
    DefaultStreamingManager streamingManagerB = new DefaultStreamingManager();
    DefaultStreamingManager streamingManagerC = new DefaultStreamingManager();

    Mockito.when(resolver.getDirectBeanDependencies("A")).thenReturn(Arrays.asList(new Pair<>("C", streamingManagerC)));
    Mockito.when(resolver.getDirectBeanDependencies("B")).thenReturn(Arrays.asList());
    Mockito.when(resolver.getDirectBeanDependencies("C")).thenReturn(Arrays.asList(new Pair<>("B", streamingManagerB)));

    sorter.addObject("A", streamingManagerA);
    sorter.addObject("B", streamingManagerB);
    sorter.addObject("C", streamingManagerC);

    assertThat(sorter.getSortedObjects(), containsInRelativeOrder(streamingManagerB, streamingManagerC, streamingManagerA));
  }


  @Test
  @Description("sort components when two components are sharing same prerequisite: " +
      "A -> C, B -> C: C should come before A and C should come before B")
  public void sortComponentsWithSharedChildTest() {
    DefaultStreamingManager streamingManagerA = new DefaultStreamingManager();
    DefaultStreamingManager streamingManagerB = new DefaultStreamingManager();
    DefaultStreamingManager streamingManagerC = new DefaultStreamingManager();

    Mockito.when(resolver.getDirectBeanDependencies("A")).thenReturn(Arrays.asList(new Pair<>("C", streamingManagerC)));
    Mockito.when(resolver.getDirectBeanDependencies("B")).thenReturn(Arrays.asList(new Pair<>("C", streamingManagerC)));
    Mockito.when(resolver.getDirectBeanDependencies("C")).thenReturn(Arrays.asList());

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

    Mockito.when(resolver.getDirectBeanDependencies("A")).thenReturn(Arrays.asList(new Pair<>("B", streamingManagerB)));
    Mockito.when(resolver.getDirectBeanDependencies("B")).thenReturn(Arrays.asList());

    sorter.addObject("A", streamingManagerA);
    sorter.addObject("B", streamingManagerB);
    sorter.addObject("A", streamingManagerA);
    sorter.addObject("A", streamingManagerA);

    assertThat(sorter.getSortedObjects().size(), is(2));
  }

}
