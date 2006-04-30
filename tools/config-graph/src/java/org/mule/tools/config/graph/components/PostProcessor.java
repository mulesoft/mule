package org.mule.tools.config.graph.components;

import com.oy.shared.lm.graph.Graph;
import org.mule.tools.config.graph.config.GraphEnvironment;

public interface PostProcessor {


    void postProcess(Graph graph, GraphEnvironment env);
}
