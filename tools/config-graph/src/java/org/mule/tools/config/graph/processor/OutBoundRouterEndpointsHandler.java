package org.mule.tools.config.graph.processor;

import java.util.Iterator;
import java.util.List;

import org.jdom.Element;
import org.mule.tools.config.graph.components.EndpointRegistry;
import org.mule.tools.config.graph.config.GraphConfig;
import org.mule.tools.config.graph.util.MuleTag;

import com.oy.shared.lm.graph.Graph;
import com.oy.shared.lm.graph.GraphNode;

public class OutBoundRouterEndpointsHandler extends TagProcessor{

	private final EndpointRegistry endpointRegistry;
	private final OutboundFilterProcessor outboundFilterProcessor;
	
	public OutBoundRouterEndpointsHandler(EndpointRegistry endpointRegistry,GraphConfig config) {
		super(config);
		this.endpointRegistry = endpointRegistry;
		this.outboundFilterProcessor = new OutboundFilterProcessor(config);

	}
	
	public void processOutBoundRouterEndpoints(Graph graph, Element router,
			GraphNode routerNode, String componentName) {
		List epList = router.getChildren(MuleTag.TAG_ENDPOINT);
		for (Iterator iterator = epList.iterator(); iterator.hasNext();) {
			Element outEndpoint = (Element) iterator.next();

			String url = outEndpoint
					.getAttributeValue(MuleTag.TAG_ATTRIBUTE_ADDRESS);
			if (url != null) {
				GraphNode out = (GraphNode) endpointRegistry.getEndpoint(url, componentName);
				if (out == null) {
					out = graph.addNode();
					StringBuffer caption = new StringBuffer();
					caption.append(url).append("\n");
					appendProperties(outEndpoint, caption);
					appendDescription(outEndpoint, caption);
					out.getInfo().setCaption(caption.toString());
					endpointRegistry.addEndpoint(url, out);
					processOutboundFilter(graph, outEndpoint, out, routerNode);
				} else {
					graph.addEdge(routerNode, out).getInfo().setCaption("out");
				}
			}

			GraphNode[] virtual = endpointRegistry
					.getVirtualEndpoint(componentName);
			if (virtual.length > 0) {
				for (int i = 0; i < virtual.length; i++) {
					graph.addEdge(routerNode, virtual[i]).getInfo().setCaption(
							"out (dynamic)");
				}
			}
		}
	}
	
	private void processOutboundFilter(Graph graph, Element outEndpoint,
			GraphNode out, GraphNode routerNode) {

		outboundFilterProcessor.process(graph, outEndpoint, out, routerNode);

	}
	
}
