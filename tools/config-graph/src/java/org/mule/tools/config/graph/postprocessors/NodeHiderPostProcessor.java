package org.mule.tools.config.graph.postprocessors;

import com.oy.shared.lm.graph.Graph;
import com.oy.shared.lm.graph.GraphNode;
import org.mule.tools.config.graph.components.PostProcessor;
import org.mule.tools.config.graph.config.GraphEnvironment;

public class NodeHiderPostProcessor implements PostProcessor {

	public void postProcess(Graph graph, GraphEnvironment env) {
		if (env.getConfig().getMappings().size() > 0) {
			GraphNode[] nodes = graph.getNodes();
			for (int i = 0; i < nodes.length; i++) {
				GraphNode node = nodes[i];
				boolean hide = Boolean.valueOf(
						env.getConfig().getMappings().getProperty(
								node.getInfo().getHeader() + ".hide", "false"))
						.booleanValue();

                boolean hideWhenCombined = Boolean.valueOf(
						env.getConfig().getMappings().getProperty(
								node.getInfo().getHeader() + ".hideWhenCombined", "false"))
						.booleanValue();
				if (hide || (env.isDoingCombinedGeneration() && hideWhenCombined)) {
					env.log("Hiding node '"
							+ node.getInfo().getHeader() + "'");
					graph.removeNode(node);
				}
			}
		}
	}

}
