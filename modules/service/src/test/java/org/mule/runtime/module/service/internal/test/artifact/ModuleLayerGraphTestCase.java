/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.service.internal.test.artifact;

import static java.lang.ModuleLayer.defineModulesWithOneLoader;
import static java.lang.ModuleLayer.empty;
import static java.util.Collections.emptyList;
import static org.mule.runtime.module.service.internal.artifact.ModuleLayerGraph.setModuleLayerId;
import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.mule.runtime.module.artifact.api.classloader.ArtifactClassLoader;
import org.mule.runtime.module.service.internal.artifact.ModuleLayerGraph;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.junit.Before;
import org.junit.Test;
import java.lang.module.Configuration;
import java.net.URL;
import java.net.URLClassLoader;

public class ModuleLayerGraphTestCase {

  private ModuleLayerGraph graph;

  @Before
  public void setUp() throws Exception {
    ModuleLayer parent =
        defineModulesWithOneLoader(Configuration.empty(), emptyList(), new URLClassLoader(new URL[] {}))
            .layer();
    ModuleLayer secondParent =
        defineModulesWithOneLoader(Configuration.empty(), emptyList(), new URLClassLoader(new URL[] {}))
            .layer();
    ModuleLayer layer = defineModulesWithOneLoader(Configuration.resolveAndBind(null, ImmutableList.of(parent.configuration(), secondParent.configuration()), null, ImmutableList.of("module 1", "module 2")), ImmutableList.of(parent, secondParent), new URLClassLoader(new URL[] {})).layer();

    /*
     * Module module1 = mock(Module.class); Module module2 = mock(Module.class); Module module3 = mock(Module.class); Module
     * module4 = mock(Module.class); when(module1.getName()).thenReturn("module1"); when(module2.getName()).thenReturn("module2");
     * when(module3.getName()).thenReturn("module3"); when(module4.getName()).thenReturn("module4");
     * when(parent.modules()).thenReturn(ImmutableSet.of(module1, module2)); when(secondParent.modules()).thenReturn(emptySet());
     * when(layer.modules()).thenReturn(ImmutableSet.of(module3, module4));
     */
    setModuleLayerId(parent, "First Parent");
    setModuleLayerId(secondParent, "Second Parent");
    setModuleLayerId(layer, "The Layer");

    graph = new ModuleLayerGraph(layer);
  }

  @Test
  public void graphString() {
    assertThat(graph.graphString(), is(""));
  }

  @Test
  public void moduleInfo() {
    assertThat(graph.moduleLayerModules(), is(""));
  }

  @Test
  public void entireRepresentation() {
    assertThat(graph.retrieveRepresentation(), is(""));
  }

}
