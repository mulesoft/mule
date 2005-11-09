package org.mule.tools.config.graph.components;

import java.util.Map;

import org.mule.tools.config.graph.config.GraphConfig;

import com.oy.shared.lm.graph.Graph;

public interface PostRenderer {

	public abstract void postRender(GraphConfig config, Map context,Graph graph);

}