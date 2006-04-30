package org.mule.tools.config.graph.processor;

import com.oy.shared.lm.graph.Graph;
import com.oy.shared.lm.graph.GraphNode;
import org.jdom.Element;
import org.mule.tools.config.graph.config.ColorRegistry;
import org.mule.tools.config.graph.config.GraphEnvironment;

import java.util.Iterator;
import java.util.List;

public class EndpointIdentifiersProcessor extends TagProcessor {
    public EndpointIdentifiersProcessor( GraphEnvironment environment) {
        super(environment);
    }

    public void process(Graph graph, Element currentElement, GraphNode parent) {
        Element endpointIdentifiers = currentElement.getChild("endpoint-identifiers");

        if (endpointIdentifiers == null) {
            environment.log("no endpoint-identifiers tag");
            return;
        }

        List namedChildren = endpointIdentifiers.getChildren("endpoint-identifier");

        for (Iterator iter = namedChildren.iterator(); iter.hasNext();) {
            Element endpoint = (Element) iter.next();
            GraphNode node = graph.addNode();
            node.getInfo().setFillColor(ColorRegistry.COLOR_DEFINED_ENDPOINTS);
            String name = endpoint.getAttributeValue("name");

            String value = lookupPropertyTemplate(endpoint.getAttributeValue("value"));
            node.getInfo().setHeader(name);
            node.getInfo().setCaption(value);
            environment.getEndpointRegistry().addEndpoint(name, node);
            environment.getProperties().setProperty(name, value);

        }
    }
}
