/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.service.internal.test.artifact;

import static org.mule.runtime.module.service.internal.artifact.ModuleLayerGraph.setModuleLayerId;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import org.mule.runtime.module.service.internal.artifact.ModuleLayerGraph;
import org.junit.Before;
import org.junit.Test;

public class ModuleLayerGraphTestCase {

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
    assertThat(graph.graphString(), is("--------------\n" +
        "| testLayer  |\n" +
        "--------------\n" +
        "       |\n" +
        "       |\n" +
        "       V\n" +
        "--------------\n" +
        "|testParentLayer|\n" +
        "--------------\n"));
  }

}
