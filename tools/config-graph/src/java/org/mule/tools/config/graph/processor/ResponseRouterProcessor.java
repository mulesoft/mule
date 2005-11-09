package org.mule.tools.config.graph.processor;

import org.jdom.Element;
import org.mule.tools.config.graph.components.EndpointRegistry;
import org.mule.tools.config.graph.config.ColorRegistry;
import org.mule.tools.config.graph.util.MuleTag;

import com.oy.shared.lm.graph.Graph;
import com.oy.shared.lm.graph.GraphNode;

public class ResponseRouterProcessor {
	
	EndpointRegistry endpointRegistry ;
	
	public ResponseRouterProcessor(EndpointRegistry endpointRegistry ) {
		this.endpointRegistry=endpointRegistry; 
	}

	public void processResponseRouter(Graph graph, Element descriptor,
			GraphNode node) {
		Element responseRouterElement = descriptor.getChild("response-router");
		if (responseRouterElement != null) {

			Element router = responseRouterElement.getChild("router");
			String className = router.getAttributeValue(MuleTag.ATTRIBUTE_CLASS_NAME);
			GraphNode responseRouter = graph.addNode();
			responseRouter.getInfo().setFillColor(ColorRegistry.COLOR_ROUTER);
			responseRouter.getInfo().setHeader(className);
			graph.addEdge(responseRouter, node).getInfo().setCaption(
					"response-router");
			Element endpoint = responseRouterElement
					.getChild(MuleTag.TAG_ENDPOINT);
			String endpointAdress = endpoint
					.getAttributeValue(MuleTag.TAG_ATTRIBUTE_ADDRESS);
			GraphNode out = (GraphNode) endpointRegistry.getEndpoint(endpointAdress, node
					.getInfo().getHeader());
			graph.addEdge(out, responseRouter).getInfo().setCaption("in");
		}
	}
}
