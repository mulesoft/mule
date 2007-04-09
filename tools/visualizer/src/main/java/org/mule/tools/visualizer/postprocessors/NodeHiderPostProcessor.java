/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the BSD style
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.tools.visualizer.postprocessors;

import org.mule.tools.visualizer.components.PostProcessor;
import org.mule.tools.visualizer.config.GraphEnvironment;

import com.oy.shared.lm.graph.Graph;
import com.oy.shared.lm.graph.GraphNode;

public class NodeHiderPostProcessor implements PostProcessor
{

    public void postProcess(Graph graph, GraphEnvironment env)
    {
        if (env.getConfig().getMappings().size() > 0)
        {
            GraphNode[] nodes = graph.getNodes();
            for (int i = 0; i < nodes.length; i++)
            {
                GraphNode node = nodes[i];
                boolean hide = Boolean.valueOf(
                    env.getConfig().getMappings().getProperty(node.getInfo().getHeader() + ".hide", "false"))
                    .booleanValue();

                boolean hideWhenCombined = Boolean.valueOf(
                    env.getConfig().getMappings().getProperty(
                        node.getInfo().getHeader() + ".hideWhenCombined", "false")).booleanValue();
                if (hide || (env.isDoingCombinedGeneration() && hideWhenCombined))
                {
                    env.log("Hiding node '" + node.getInfo().getHeader() + "'");
                    graph.removeNode(node);
                }
            }
        }
    }

}
