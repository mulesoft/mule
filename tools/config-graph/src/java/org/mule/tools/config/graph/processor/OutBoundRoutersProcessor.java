package org.mule.tools.config.graph.processor;

import com.oy.shared.lm.graph.Graph;
import com.oy.shared.lm.graph.GraphNode;
import org.jdom.Element;
import org.mule.tools.config.graph.config.ColorRegistry;
import org.mule.tools.config.graph.config.GraphEnvironment;
import org.mule.tools.config.graph.util.MuleTag;

import java.util.Iterator;
import java.util.List;

public class OutBoundRoutersProcessor extends TagProcessor {

	public OutBoundRoutersProcessor( GraphEnvironment environment) {
        super(environment);
	}

	public void process(Graph graph, Element currentElement, GraphNode parent) {
		Element outboundRouter = currentElement.getChild(MuleTag.ELEMENT_OUTBOUND_ROUTER);

		if (outboundRouter != null) {
			String componentName = parent.getInfo().getHeader();
			List routers = outboundRouter.getChildren(MuleTag.ELEMENT_ROUTER);
            ExceptionStrategyProcessor processor = new ExceptionStrategyProcessor(environment);
            processor.process(graph, outboundRouter, parent);

			for (Iterator iterator = routers.iterator(); iterator.hasNext();) {
				Element router = (Element) iterator.next();

				if (router != null) {
					GraphNode routerNode = graph.addNode();
					routerNode.getInfo().setHeader(
							router.getAttributeValue(MuleTag.ATTRIBUTE_CLASS_NAME));
					routerNode.getInfo().setFillColor(ColorRegistry.COLOR_ROUTER);


				    addRelation(graph, parent, routerNode, "outbound router");
					
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

                    OutBoundRouterEndpointsHandler processor2 = new OutBoundRouterEndpointsHandler(environment, componentName);
				    processor2.process(graph, router, routerNode);

					processReplyTOasElement(graph, router, routerNode,
							componentName);
					processReplyTOasProperty(graph, router, routerNode,
							componentName);

					GraphNode[] virtual = environment.getEndpointRegistry()
							.getVirtualEndpoint(componentName + "."
									+ router.getAttributeValue(MuleTag.ATTRIBUTE_CLASS_NAME));
					if (virtual.length > 0) {
						for (int i = 0; i < virtual.length; i++) {
							addRelation(graph, routerNode, virtual[i], "out (dynamic)");
						}
					}

				}
			}

			GraphNode[] virtual = environment.getEndpointRegistry()
					.getVirtualEndpoint(componentName);
			if (virtual.length > 0) {
				for (int i = 0; i < virtual.length; i++) {
					addRelation(graph, parent, virtual[i], "out (dynamic)");
				}
			}

		}
	}

	private void processReplyTOasElement(Graph graph, Element router,
			GraphNode routerNode, String componentName) {
		Element replyToElement = router.getChild(MuleTag.ELEMENT_REPLY_TO);
		if (replyToElement != null) {
			String replyTo = replyToElement
					.getAttributeValue(MuleTag.ATTRIBUTE_ADDRESS);
			if (replyTo != null) {
				GraphNode out = (GraphNode) environment.getEndpointRegistry().getEndpoint(
						replyTo, componentName);
				addRelation(graph, routerNode, out, "sets");
			}
		}
	}

	private void processReplyTOasProperty(Graph graph, Element router,
			GraphNode routerNode, String componentName) {
		Element propertiesEl = router.getChild(MuleTag.ELEMENT_PROPERTIES);
		if (propertiesEl != null) {
			List properties = propertiesEl.getChildren(MuleTag.ELEMENT_PROPERTY);
			for (Iterator iterator = properties.iterator(); iterator.hasNext();) {
				Element property = (Element) iterator.next();
				String propertyName = property.getAttributeValue(MuleTag.ATTRIBUTE_NAME);
				if ("replyTo".equals(propertyName)) {
					String replyTo = property.getAttributeValue(MuleTag.ATTRIBUTE_VALUE);
					if (replyTo != null) {
						GraphNode out = (GraphNode) environment.getEndpointRegistry()
								.getEndpoint(replyTo, componentName);
						addRelation(graph, routerNode, out, "sets");
					}
				}
			}
		}
	}
}
