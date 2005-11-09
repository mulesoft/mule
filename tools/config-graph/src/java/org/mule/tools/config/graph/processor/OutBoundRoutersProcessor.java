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

public class OutBoundRoutersProcessor {

	private final ExceptionStrategyProcessor exceptionStrategyProcessor;

	private final OutBoundRouterEndpointsHandler outBoundRouterEndpointsHandler;

	//TODO dead code ?
	//private final OutboundFilterProcessor outboundFilterProcessor;

	private final EndpointRegistry endpointRegistry;

	public OutBoundRoutersProcessor(
			final GraphConfig config,
			final ExceptionStrategyProcessor exceptionStrategyProcessor,
			final OutBoundRouterEndpointsHandler outBoundRouterEndpointsHandler,
			final EndpointRegistry endpointRegistry) {
		this.endpointRegistry = endpointRegistry;
		this.outBoundRouterEndpointsHandler = outBoundRouterEndpointsHandler;
	//	this.outboundFilterProcessor = new OutboundFilterProcessor(config);
		this.exceptionStrategyProcessor = exceptionStrategyProcessor;
	}

	public void processOutBoundRouters(Graph graph, Element descriptor,
			GraphNode node) {
		Element outboundRouter = descriptor.getChild("outbound-router");

		if (outboundRouter != null) {
			String componentName = node.getInfo().getHeader();
			List routers = outboundRouter.getChildren("router");
			exceptionStrategyProcessor.processExceptionStrategy(graph,
					outboundRouter, node);

			for (Iterator iterator = routers.iterator(); iterator.hasNext();) {
				Element router = (Element) iterator.next();

				if (router != null) {
					GraphNode routerNode = graph.addNode();
					routerNode.getInfo().setHeader(
							router.getAttributeValue("className"));
					routerNode.getInfo().setFillColor(
							ColorRegistry.COLOR_ROUTER);
					graph.addEdge(node, routerNode).getInfo().setCaption(
							"outbound router");
					
					// TODO  dead code ? but how to handle filter condition
					/*Element endpointEl = router.getChild("endpoint");
					
					if (endpointEl != null) {
						String endpointAdress = endpointEl
								.getAttributeValue("address");
						GraphNode endpoint = endpointRegistry.getEndpoint(
								endpointAdress, componentName);
						if (endpoint == null) {
							endpoint = graph.addNode();
							endpoint.getInfo().setHeader(endpointAdress);
							endpointRegistry.addEndpoint(endpointAdress,
									endpoint);
						}
						
						outboundFilterProcessor.process(graph, router,
								endpoint, routerNode);
					}else {*/ 
						outBoundRouterEndpointsHandler
									.processOutBoundRouterEndpoints(graph, router,
											routerNode, componentName);
					
					processReplyTOasElement(graph, router, routerNode,
							componentName);
					processReplyTOasProperty(graph, router, routerNode,
							componentName);

					GraphNode[] virtual = endpointRegistry
							.getVirtualEndpoint(componentName + "."
									+ router.getAttributeValue("className"));
					if (virtual.length > 0) {
						for (int i = 0; i < virtual.length; i++) {
							graph.addEdge(routerNode, virtual[i]).getInfo()
									.setCaption("out (dynamic)");
						}
					}

				}
			}

			GraphNode[] virtual = endpointRegistry
					.getVirtualEndpoint(componentName);
			if (virtual.length > 0) {
				for (int i = 0; i < virtual.length; i++) {
					graph.addEdge(node, virtual[i]).getInfo().setCaption(
							"out (dynamic)");
				}
			}

		}
	}

	private void processReplyTOasElement(Graph graph, Element router,
			GraphNode routerNode, String componentName) {
		Element replyToElement = router.getChild("reply-to");
		if (replyToElement != null) {
			String replyTo = replyToElement
					.getAttributeValue(MuleTag.TAG_ATTRIBUTE_ADDRESS);
			if (replyTo != null) {
				GraphNode out = (GraphNode) endpointRegistry.getEndpoint(
						replyTo, componentName);
				graph.addEdge(routerNode, out).getInfo().setCaption("sets");
			}
		}
	}

	private void processReplyTOasProperty(Graph graph, Element router,
			GraphNode routerNode, String componentName) {
		Element propertiesEl = router.getChild("properties");
		if (propertiesEl != null) {
			List properties = propertiesEl.getChildren("property");
			for (Iterator iterator = properties.iterator(); iterator.hasNext();) {
				Element property = (Element) iterator.next();
				String propertyName = property.getAttributeValue("name");
				if ("replyTo".equals(propertyName)) {
					String replyTo = property.getAttributeValue("value");
					if (replyTo != null) {
						GraphNode out = (GraphNode) endpointRegistry
								.getEndpoint(replyTo, componentName);
						graph.addEdge(routerNode, out).getInfo().setCaption(
								"sets");
					}
				}
			}
		}
	}
}
