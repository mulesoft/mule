package org.mule.tools.config.graph.postprocessors;

import com.oy.shared.lm.graph.Graph;
import com.oy.shared.lm.graph.GraphNode;
import org.mule.tools.config.graph.components.PostProcessor;
import org.mule.tools.config.graph.config.ColorRegistry;
import org.mule.tools.config.graph.config.GraphEnvironment;
import org.mule.tools.config.graph.processor.TagProcessor;

public class ExternalSystemPostProcessor implements PostProcessor {

    public void postProcess(Graph graph, GraphEnvironment env) {
        if (env.getConfig().getMappings().size() > 0) {
            GraphNode[] nodes = graph.getNodes();
            for (int i = 0; i < nodes.length; i++) {
                GraphNode node = nodes[i];
                String ext = env.getConfig().getMappings().getProperty(
                                node.getInfo().getHeader() + ".external", null);
                if (ext!=null) {
                    env.log("Adding External system '"
                            + ext + "'");

                    String type = env.getConfig().getMappings().getProperty(
                                node.getInfo().getHeader() + ".external.type", null);
                    GraphNode n = graph.addNode();
                    n.getInfo().setShapeEllipse();
                    n.getInfo().setCaption(ext);
                    n.getInfo().setFillColor(ColorRegistry.COLOR_EXTERNAL);
                    if("server".equalsIgnoreCase(type)) {
                        TagProcessor.addEdge(graph, node, n, null, true);
                    } else {
                        TagProcessor.addEdge(graph, n, node, null, true);
                    }
                }
            }
        }
    }

}
