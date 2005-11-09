package org.mule.tools.config.graph.components;

import org.mule.tools.config.graph.config.GraphConfig;

public interface PostGrapher {

	public String getStatusTitle();
	public abstract void postGrapher(GraphConfig config);

}