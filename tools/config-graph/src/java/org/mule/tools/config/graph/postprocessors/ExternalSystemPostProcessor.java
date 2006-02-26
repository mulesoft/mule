package org.mule.tools.config.graph.postprocessors;

import com.oy.shared.lm.graph.Graph;
import com.oy.shared.lm.graph.GraphEdge;
import com.oy.shared.lm.graph.GraphNode;
import org.mule.tools.config.graph.components.PostProcessor;
import org.mule.tools.config.graph.config.ColorRegistry;
import org.mule.tools.config.graph.config.GraphConfig;

public class ExternalSystemPostProcessor implements PostProcessor {

	public void postProcess(Graph graph, GraphConfig config) {
		if (config.getMappings().size() > 0) {
			GraphNode[] nodes = graph.getNodes();
			for (int i = 0; i < nodes.length; i++) {
				GraphNode node = nodes[i];
				String ext = config.getMappings().getProperty(
								node.getInfo().getHeader() + ".external", null);
				if (ext!=null) {
					System.out.println("Adding External system '"
							+ ext + "'");

                    String type = config.getMappings().getProperty(
								node.getInfo().getHeader() + ".external.type", null);
					GraphNode n = graph.addNode();
                    n.getInfo().setShapeEllipse();
                    n.getInfo().setCaption(ext);
                    n.getInfo().setFillColor(ColorRegistry.COLOR_EXTERNAL);
                    GraphEdge edge;
                    if("server".equalsIgnoreCase(type)) {
                        edge = graph.addEdge(node, n);
                    } else {
                        edge = graph.addEdge(n, node);
                    }
                    edge.getInfo().setArrowTailNormal();
				}
			}
		}
	}

}
