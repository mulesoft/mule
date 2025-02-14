/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.service.internal.test.artifact;

import static org.hamcrest.CoreMatchers.containsString;
import static org.mule.runtime.module.service.internal.artifact.ModuleLayerGraph.setModuleLayerId;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import org.mule.runtime.module.service.internal.artifact.ModuleLayerGraph;
import org.junit.Before;
import org.junit.Test;

public class ModuleLayerGraphTestCase {

  private static final String GRAPH_REPR = "--------------\n" +
      "| testLayer  |\n" +
      "--------------\n" +
      "       |\n" +
      "       |\n" +
      "       V\n" +
      "--------------\n" +
      "|testParentLayer|\n" +
      "--------------\n";

  private ModuleLayerGraph graph;

  @Before
  public void setUp() throws Exception {
    ModuleLayer layer = this.getClass().getModule().getLayer();
    setModuleLayerId(this.getClass().getModule().getLayer(), "testLayer");
    setModuleLayerId(layer.parents().get(0), "testParentLayer");
    graph = new ModuleLayerGraph(layer);
  }

  @Test
  public void graphString() {
    assertThat(graph.graphString(), is(GRAPH_REPR));
  }

  @Test
  public void moduleLayerModules() {
    String info = graph.moduleLayerModules();
    // We don't assert for the exact string because it may come in different order, and depending on the modifications of
    // dependencies of this module, then this test may start failing even if we sort those dependencies' modules.
    assertThat(info, containsString("org.mule.runtime.metadata.model.api"));
    assertThat(info, containsString("org.mule.runtime.jpms.utils"));
    assertThat(info, containsString("org.mule.runtime.metrics.api"));
    assertThat(info, containsString("org.mule.runtime.tracer.api"));
    assertThat(info, containsString("org.mule.runtime.manifest"));
    assertThat(info, containsString("org.mule.runtime.dsl.api"));
    assertThat(info, containsString("org.mule.runtime.jar.handling.utils"));
    assertThat(info, containsString("org.mule.runtime.container"));
    assertThat(info, containsString("org.mule.runtime.metadata.model.message"));
    assertThat(info, containsString("org.mule.runtime.profiling.api"));
    assertThat(info, containsString("org.mule.runtime.tracer.customization.api"));
    assertThat(info, containsString("org.mule.runtime.tracer.exporter.configuration.api"));
    assertThat(info, containsString("org.mule.runtime.policy.api"));
    assertThat(info, containsString("org.mule.runtime.policy.api"));
    assertThat(info, containsString("org.mule.runtime.core"));
    assertThat(info, containsString("org.mule.runtime.metadata.model.java"));
    assertThat(info, containsString("org.mule.runtime.artifact.declaration"));
    assertThat(info, containsString("org.mule.runtime.errors"));
    assertThat(info, containsString("org.mule.sdk.api"));
  }

}
