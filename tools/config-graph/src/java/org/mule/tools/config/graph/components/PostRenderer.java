package org.mule.tools.config.graph.components;

import com.oy.shared.lm.graph.Graph;
import org.mule.tools.config.graph.config.GraphEnvironment;

import java.util.Map;

public interface PostRenderer {

	public abstract void postRender(GraphEnvironment env, Map context,Graph graph);

}