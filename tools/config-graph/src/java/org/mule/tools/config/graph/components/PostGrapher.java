package org.mule.tools.config.graph.components;

import org.mule.tools.config.graph.config.GraphEnvironment;

public interface PostGrapher {

    public String getStatusTitle();
    public abstract void postGrapher(GraphEnvironment env);

}