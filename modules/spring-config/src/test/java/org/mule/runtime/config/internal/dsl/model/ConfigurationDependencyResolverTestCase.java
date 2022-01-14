/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.dsl.model;

import static java.util.Optional.of;

import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.ast.graph.internal.DefaultArtifactAstDependencyGraph;

import java.util.Collection;
import java.util.Set;

import com.google.common.collect.Sets;
import io.qameta.allure.Description;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;

public class ConfigurationDependencyResolverTestCase {

  ConfigurationDependencyResolver configurationDependencyResolver;
  private DefaultArtifactAstDependencyGraph graph;

  @Before
  public void setUp() throws Exception {
    graph = mock(DefaultArtifactAstDependencyGraph.class);
    configurationDependencyResolver = new ConfigurationDependencyResolver(graph);
  }



  @Test
  @Description("2 dependencies will be returned when there are two required components")
  public void defaultTest() {
    // given
    // mockito.any(regardless of the param)
    Set<ComponentAst> components = Sets.newHashSet(createComponentWithId("A"),
                                                   createComponentWithId("B"));
    when(graph.getRequiredComponents(any())).thenReturn(components);
    // when
    Collection<String> result = configurationDependencyResolver.getDirectComponentDependencies("component");
    // then
    assertThat(result.size(), Matchers.is(2));
  }

  @Test
  @Description("3 dependencies will be returned when there are 5 required components, only 3 have id")
  public void defaultTest2() {
    // given
    // mockito.any(regardless of the param)

    Set<ComponentAst> components = Sets.newHashSet(createComponentWithId("A"),
                                                   createComponentWithId("B"), createComponentWithId("C"),
                                                   createComponentWithoutId("D"), createComponentWithoutId("E"));
    when(graph.getRequiredComponents(any())).thenReturn(components);
    // when
    Collection<String> result = configurationDependencyResolver.getDirectComponentDependencies("component");
    // then
    assertThat(result.size(), Matchers.is(3));
  }

  private ComponentAst createComponentWithId(String componentName) {
    ComponentAst component = mock(ComponentAst.class);
    when(component.getComponentId()).thenReturn(of(componentName));
    return component;

  }

  private ComponentAst createComponentWithoutId(String componentName) {
    ComponentAst component = mock(ComponentAst.class);
    return component;
  }

  // todo: change L61 component name to a random string
  // using a verify captor: getRequiredComponents
  // (check if the same string is passed)
  // https://www.baeldung.com/mockito-argument-matchers
  // https://www.baeldung.com/mockito-verify


}
