package org.mule.tools.config.graph.postprocessors;

import org.mule.tools.config.graph.components.PostProcessor;
import org.mule.tools.config.graph.config.GraphConfig;

import com.oy.shared.lm.graph.Graph;
import com.oy.shared.lm.graph.GraphNode;

public class NodeHiderPostProcessor implements PostProcessor {

	public void postProcess(Graph graph, GraphConfig config) {
		if (config.getMappings().size() > 0) {
			GraphNode[] nodes = graph.getNodes();
			for (int i = 0; i < nodes.length; i++) {
				GraphNode node = nodes[i];
				boolean hide = Boolean.valueOf(
						config.getMappings().getProperty(
								node.getInfo().getHeader() + ".hide", "false"))
						.booleanValue();
				if (hide) {
					System.out.println("Hiding node '"
							+ node.getInfo().getHeader() + "'");
					graph.removeNode(node);
				}
			}
		}
	}

}
