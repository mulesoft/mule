package org.mule.tools.config.graph.processor;

import com.oy.shared.lm.graph.Graph;
import com.oy.shared.lm.graph.GraphNode;
import org.jdom.Element;
import org.mule.tools.config.graph.config.ColorRegistry;
import org.mule.tools.config.graph.config.GraphEnvironment;

import java.util.Iterator;
import java.util.List;

public class EndpointsProcessor extends TagProcessor {
    public EndpointsProcessor( GraphEnvironment environment) {
        super(environment);
    }

    public void process(Graph graph, Element currentElement, GraphNode parent) {
        Element globalEndpoints = currentElement.getChild("global-endpoints");

        if (globalEndpoints == null) {
            environment.log("no global-endpoints");
            return;
        }

        List namedChildren = globalEndpoints.getChildren("endpoint");

        for (Iterator iter = namedChildren.iterator(); iter.hasNext();) {
            Element endpoint = (Element) iter.next();
            GraphNode node = graph.addNode();
            node.getInfo().setFillColor(ColorRegistry.COLOR_DEFINED_ENDPOINTS);
            String name = endpoint.getAttributeValue("name");

            node.getInfo().setHeader(
                    endpoint.getAttributeValue("address") + " (" + name + ")");
            StringBuffer caption = new StringBuffer();
            TagProcessor.appendProperties(endpoint, caption);
            node.getInfo().setCaption(caption.toString());
            environment.getEndpointRegistry().addEndpoint(name, node);
        }
    }
}
