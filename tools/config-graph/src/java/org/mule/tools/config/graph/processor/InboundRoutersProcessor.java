package org.mule.tools.config.graph.processor;

import java.util.Iterator;
import java.util.List;

import org.jdom.Element;
import org.mule.tools.config.graph.components.EndpointRegistry;
import org.mule.tools.config.graph.config.ColorRegistry;
import org.mule.tools.config.graph.config.GraphConfig;
import org.mule.tools.config.graph.util.MuleTag;

import com.oy.shared.lm.graph.Graph;
import com.oy.shared.lm.graph.GraphNode;

public class InboundRoutersProcessor extends TagProcessor {
	private EndpointRegistry endpointRegistry;

	private InboundFilterProcessor inboundFilterProcessor;

	public InboundRoutersProcessor(EndpointRegistry endpointRegistry,
			GraphConfig config) {
		super(config);
		this.endpointRegistry = endpointRegistry;
		this.inboundFilterProcessor = new InboundFilterProcessor(config);
	}

	public void processInboundRouters(Graph graph, Element descriptor,
			GraphNode node) {
		Element inboundRouter = descriptor.getChild("inbound-router");

		if (inboundRouter != null) {

			GraphNode endpointsLink = node;

			Element router = inboundRouter.getChild("router");
			if (router != null) {
				GraphNode routerNode = graph.addNode();
				routerNode.getInfo().setHeader(
						router.getAttributeValue(MuleTag.ATTRIBUTE_CLASS_NAME));
				routerNode.getInfo().setFillColor(ColorRegistry.COLOR_ROUTER);

				graph.addEdge(routerNode, node).getInfo().setCaption(
						"inbound router");
				endpointsLink = routerNode;
			}

			List inbounEndpoints = inboundRouter
					.getChildren(MuleTag.TAG_ENDPOINT);
			for (Iterator iterator = inbounEndpoints.iterator(); iterator
					.hasNext();) {
				Element inEndpoint = (Element) iterator.next();
				String url = inEndpoint
						.getAttributeValue(MuleTag.TAG_ATTRIBUTE_ADDRESS);
				if (url != null) {
					GraphNode in = (GraphNode) endpointRegistry.getEndpoint(
							url, node.getInfo().getHeader());
					StringBuffer caption = new StringBuffer();
					if (in == null) {
						in = graph.addNode();
						in.getInfo().setFillColor(ColorRegistry.COLOR_ENDPOINT);
						caption.append(url).append("\n");
						appendProperties(inEndpoint, caption);
						appendDescription(inEndpoint, caption);
						in.getInfo().setCaption(caption.toString());
					} else {
						// rewrite the properties
						// TODO really we need a cleaner way of handling in/out
						// endpoints between components
						caption.append(url).append("\n");
						appendProperties(inEndpoint, caption);
						appendDescription(inEndpoint, caption);
						in.getInfo().setCaption(caption.toString());
					}

					if (in != null) {
						inboundFilterProcessor.processInboundFilter(graph,
								inEndpoint, in, endpointsLink);
					}
				}
			}

		}
	}

}
