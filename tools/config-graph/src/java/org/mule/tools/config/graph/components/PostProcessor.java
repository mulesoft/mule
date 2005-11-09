package org.mule.tools.config.graph.components;

import org.mule.tools.config.graph.config.GraphConfig;

import com.oy.shared.lm.graph.Graph;

public interface PostProcessor {

	
	void postProcess(Graph graph,GraphConfig config);	
}
