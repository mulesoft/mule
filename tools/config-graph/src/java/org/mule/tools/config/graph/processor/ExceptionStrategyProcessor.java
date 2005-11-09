package org.mule.tools.config.graph.processor;

import org.jdom.Element;
import org.mule.tools.config.graph.components.EndpointRegistry;
import org.mule.tools.config.graph.config.ColorRegistry;
import org.mule.tools.config.graph.config.GraphConfig;
import org.mule.tools.config.graph.util.MuleTag;

import com.oy.shared.lm.graph.Graph;
import com.oy.shared.lm.graph.GraphNode;

public class ExceptionStrategyProcessor extends TagProcessor{

	private EndpointRegistry registry;

	public ExceptionStrategyProcessor(EndpointRegistry registry, GraphConfig config) {
		super(config);
		this.registry=registry;		
	}
	public void processExceptionStrategy(Graph graph, Element descriptor,
			GraphNode node) {
		Element exceptionStrategy = descriptor.getChild("catch-all-strategy");
		if (exceptionStrategy == null)
			exceptionStrategy = descriptor.getChild("exception-strategy");

		if (exceptionStrategy != null) {

			String className = exceptionStrategy.getAttributeValue(MuleTag.ATTRIBUTE_CLASS_NAME);
			GraphNode exceptionNode = graph.addNode();
			exceptionNode.getInfo().setHeader(className);
			exceptionNode.getInfo().setFillColor(ColorRegistry.COLOR_EXCEPTION_STRATEGY);

			graph.addEdge(node, exceptionNode).getInfo().setCaption(
					"catch-all-strategy");

			Element endpoint = exceptionStrategy.getChild(MuleTag.TAG_ENDPOINT);
			if (endpoint != null) {
				String url = endpoint.getAttributeValue(MuleTag.TAG_ATTRIBUTE_ADDRESS);
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
