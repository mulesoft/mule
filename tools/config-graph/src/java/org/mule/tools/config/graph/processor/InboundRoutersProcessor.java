package org.mule.tools.config.graph.processor;

import com.oy.shared.lm.graph.Graph;
import com.oy.shared.lm.graph.GraphNode;
import org.jdom.Element;
import org.mule.tools.config.graph.config.ColorRegistry;
import org.mule.tools.config.graph.config.GraphEnvironment;
import org.mule.tools.config.graph.util.MuleTag;

import java.util.Iterator;
import java.util.List;

public class InboundRoutersProcessor extends TagProcessor {

	public InboundRoutersProcessor( GraphEnvironment environment) {
		super(environment);

	}

	public void process(Graph graph, Element currentElement, GraphNode parent) {
		Element inboundRouter = currentElement.getChild(MuleTag.ELEMENT_INBOUND_ROUTER);

		if (inboundRouter != null) {

			GraphNode endpointsLink = parent;

			Element router = inboundRouter.getChild(MuleTag.ELEMENT_ROUTER);
			if (router != null) {
				GraphNode routerNode = graph.addNode();
				routerNode.getInfo().setHeader(
						router.getAttributeValue(MuleTag.ATTRIBUTE_CLASS_NAME));
				routerNode.getInfo().setFillColor(ColorRegistry.COLOR_ROUTER);

                addEdge(graph, routerNode, parent, "inbound router", isTwoWay(router));
				endpointsLink = routerNode;
			}

			List inbounEndpoints = inboundRouter
					.getChildren(MuleTag.ELEMENT_ENDPOINT);
			for (Iterator iterator = inbounEndpoints.iterator(); iterator
					.hasNext();) {
				Element inEndpoint = (Element) iterator.next();
				String url = inEndpoint
						.getAttributeValue(MuleTag.ATTRIBUTE_ADDRESS);
				if (url != null) {
					GraphNode in = (GraphNode) environment.getEndpointRegistry().getEndpoint(
							url, parent.getInfo().getHeader());
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
                        //Mark boundary endpoints between configurations
//                        if(environment.getConfig().isCombineFiles()) {
//                            in.getInfo().setLineColor("red");
//                        }
					}

					if (in != null) {
                        InboundFilterProcessor processor = new InboundFilterProcessor(environment, endpointsLink);
						processor.processInboundFilter(graph, inEndpoint, in, endpointsLink);
					}
				}
			}

		}
	}

}
