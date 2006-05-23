package org.mule.tools.config.graph.processor;

import com.oy.shared.lm.graph.Graph;
import com.oy.shared.lm.graph.GraphNode;

import org.jdom.Element;
import org.mule.tools.config.graph.config.ColorRegistry;
import org.mule.tools.config.graph.config.GraphEnvironment;
import org.mule.tools.config.graph.util.MuleTag;

public class ResponseRouterProcessor extends TagProcessor {

    public ResponseRouterProcessor( GraphEnvironment environment ) {
        super(environment);
    }

    public void process(Graph graph, Element currentElement, GraphNode parent) {
        Element responseRouterElement = currentElement.getChild(MuleTag.ELEMENT_RESPONSE_ROUTER);
        if (responseRouterElement != null) {

            Element router = responseRouterElement.getChild(MuleTag.ELEMENT_ROUTER);
            String className = router.getAttributeValue(MuleTag.ATTRIBUTE_CLASS_NAME);
            GraphNode responseRouter = graph.addNode();
            responseRouter.getInfo().setFillColor(ColorRegistry.COLOR_ROUTER);
            responseRouter.getInfo().setHeader(className);
            addEdge(graph,responseRouter, parent, "response router", false);

            Element endpoint = responseRouterElement
                    .getChild(MuleTag.ELEMENT_ENDPOINT);
            String endpointAdress = endpoint
                    .getAttributeValue(MuleTag.ATTRIBUTE_ADDRESS);
            GraphNode out = environment.getEndpointRegistry().getEndpoint(endpointAdress,
                    parent.getInfo().getHeader());
            addEdge(graph, out, responseRouter, "in", isTwoWay(endpoint));
        }
    }
}
