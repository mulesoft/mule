package org.mule.tools.config.graph.processor;

import com.oy.shared.lm.graph.Graph;
import com.oy.shared.lm.graph.GraphEdge;
import com.oy.shared.lm.graph.GraphNode;
import org.jdom.Element;
import org.mule.tools.config.graph.components.EndpointRegistry;
import org.mule.tools.config.graph.config.GraphConfig;
import org.mule.tools.config.graph.util.MuleTag;

import java.util.Iterator;
import java.util.List;

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
		List epList = router.getChildren(MuleTag.ELEMENT_ENDPOINT);
        int x=1;
		for (Iterator iterator = epList.iterator(); iterator.hasNext(); x++) {
			Element outEndpoint = (Element) iterator.next();

			String url = outEndpoint
					.getAttributeValue(MuleTag.ATTRIBUTE_ADDRESS);
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
					GraphEdge e = graph.addEdge(routerNode, out);
                    if(epList.size()==1) {
                        e.getInfo().setCaption("out");
                    } else {
                        e.getInfo().setCaption("out (" + x + " of " + epList.size() + ")");
                    }
                    if("true".equalsIgnoreCase(outEndpoint.getAttributeValue(MuleTag.ATTRIBUTE_SYNCHRONOUS))) {
                        e.getInfo().setArrowTailNormal();
                    }
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
