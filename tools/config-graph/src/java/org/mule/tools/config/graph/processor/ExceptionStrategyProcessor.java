package org.mule.tools.config.graph.processor;

import com.oy.shared.lm.graph.Graph;
import com.oy.shared.lm.graph.GraphNode;
import org.jdom.Element;
import org.mule.tools.config.graph.components.EndpointRegistry;
import org.mule.tools.config.graph.config.ColorRegistry;
import org.mule.tools.config.graph.config.GraphConfig;
import org.mule.tools.config.graph.util.MuleTag;

public class ExceptionStrategyProcessor extends TagProcessor{

	private EndpointRegistry registry;

	public ExceptionStrategyProcessor(EndpointRegistry registry, GraphConfig config) {
		super(config);
		this.registry=registry;		
	}
	public void processExceptionStrategy(Graph graph, Element descriptor, GraphNode node) {
		String edgeCaption = MuleTag.ELEMENT_CATCH_ALL_STRATEGY;
        Element exceptionStrategy = descriptor.getChild(edgeCaption);
		if (exceptionStrategy == null) {
            edgeCaption = MuleTag.ELEMENT_EXCEPTION_STRATEGY;
			exceptionStrategy = descriptor.getChild(edgeCaption);

        }

		if (exceptionStrategy != null) {

			String className = exceptionStrategy.getAttributeValue(MuleTag.ATTRIBUTE_CLASS_NAME);
			GraphNode exceptionNode = graph.addNode();
			exceptionNode.getInfo().setHeader(className);
			exceptionNode.getInfo().setFillColor(ColorRegistry.COLOR_EXCEPTION_STRATEGY);

			graph.addEdge(node, exceptionNode).getInfo().setCaption(edgeCaption);

			Element endpoint = exceptionStrategy.getChild(MuleTag.ELEMENT_ENDPOINT);
			if (endpoint != null) {
				String url = endpoint.getAttributeValue(MuleTag.ATTRIBUTE_ADDRESS);
				if (url != null) {
					GraphNode out = (GraphNode) registry.getEndpoint(url, node.getInfo()
							.getHeader());
					if (out == null) {
						out = graph.addNode();
						out.getInfo().setCaption(url);
						registry.addEndpoint(url, out);
					}
					graph.addEdge(exceptionNode, out);
				}
			}
		}

	}
}
