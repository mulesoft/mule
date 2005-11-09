package org.mule.tools.config.graph.processor;

import org.jdom.Element;
import org.mule.tools.config.graph.components.EndpointRegistry;
import org.mule.tools.config.graph.util.MuleTag;

import com.oy.shared.lm.graph.Graph;
import com.oy.shared.lm.graph.GraphNode;

public class ShortestNotationHandler {

	private EndpointRegistry endpointRegistry;
	
	public ShortestNotationHandler(EndpointRegistry endpointRegistry) {
		this.endpointRegistry =endpointRegistry;
	}
	
	public void processShortestNotation(Graph graph, Element descriptor,
			GraphNode node) {
		String inbound = descriptor.getAttributeValue(MuleTag.ATTRIBUTE_INBOUNDENDPOINT);
		if (inbound != null) {
			GraphNode in = (GraphNode) endpointRegistry.getEndpoint(inbound,
					node.getInfo().getHeader());
			if (in == null) {
				in = graph.addNode();
				in.getInfo().setCaption(inbound);
				endpointRegistry.addEndpoint(inbound, in);
			}
			graph.addEdge(in, node).getInfo().setCaption("in");
		}
		String outbound = descriptor.getAttributeValue(MuleTag.ATTRIBUTE_OUTBOUNDENDPOINT);
		if (outbound != null) {
			GraphNode out = (GraphNode) endpointRegistry.getEndpoint(outbound, node.getInfo()
					.getHeader());
			if (out == null) {
				out = graph.addNode();
				out.getInfo().setCaption(outbound);
				endpointRegistry.addEndpoint(outbound, out);
			}
			graph.addEdge(node, out).getInfo().setCaption("out");
		}

		String inboundTransformers = descriptor
				.getAttributeValue("inboundTransformer");
		if (inboundTransformers != null) {
			String[] transformers = inboundTransformers.split(" ");
			StringBuffer caption = new StringBuffer();
			for (int i = 0; i < transformers.length; i++) {
				caption.append("transformer " + i + " : " + transformers[i]
						+ "\n");
			}
			node.getInfo().setCaption(node.getInfo().getCaption()+"\n"+caption.toString());
		}

		GraphNode[] virtual = endpointRegistry.getVirtualEndpoint(node
				.getInfo().getHeader());
		if (virtual.length > 0) {
			for (int i = 0; i < virtual.length; i++) {
				graph.addEdge(node, virtual[i]).getInfo().setCaption(
						"out (dynamic)");
			}
		}
	}
}
