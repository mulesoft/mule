package org.mule.tools.config.graph.processor;

import com.oy.shared.lm.graph.Graph;
import com.oy.shared.lm.graph.GraphNode;
import org.jdom.Element;
import org.mule.tools.config.graph.config.GraphEnvironment;
import org.mule.tools.config.graph.util.MuleTag;

public class ShortestNotationHandler extends TagProcessor {


	public ShortestNotationHandler(GraphEnvironment environment) {
        super(environment);
	}
	
	public void process(Graph graph, Element currentElement, GraphNode parent) {
		String inbound = currentElement.getAttributeValue(MuleTag.ATTRIBUTE_INBOUNDENDPOINT);
		if (inbound != null) {
			GraphNode in = (GraphNode) environment.getEndpointRegistry().getEndpoint(inbound,
					parent.getInfo().getHeader());
			if (in == null) {
				in = graph.addNode();
				in.getInfo().setCaption(inbound);
				environment.getEndpointRegistry().addEndpoint(inbound, in);
			}
			addEdge(graph, in, parent, "in", isTwoWay(null));
		}
		String outbound = currentElement.getAttributeValue(MuleTag.ATTRIBUTE_OUTBOUNDENDPOINT);
		if (outbound != null) {
			GraphNode out = (GraphNode) environment.getEndpointRegistry().getEndpoint(outbound, parent.getInfo()
					.getHeader());
			if (out == null) {
				out = graph.addNode();
				out.getInfo().setCaption(outbound);
				environment.getEndpointRegistry().addEndpoint(outbound, out);
			}
			addEdge(graph, parent, out, "out", isTwoWay(null));
		}

		String inboundTransformers = currentElement
				.getAttributeValue("inboundTransformer");
		if (inboundTransformers != null) {
			String[] transformers = inboundTransformers.split(" ");
			StringBuffer caption = new StringBuffer();
			for (int i = 0; i < transformers.length; i++) {
				caption.append("transformer " + i + " : " + transformers[i]
						+ "\n");
			}
			parent.getInfo().setCaption(parent.getInfo().getCaption()+"\n"+caption.toString());
		}

		GraphNode[] virtual = environment.getEndpointRegistry().getVirtualEndpoint(parent
				.getInfo().getHeader());
		if (virtual.length > 0) {
			for (int i = 0; i < virtual.length; i++) {
				addEdge(graph, parent, virtual[i], "out (dynamic)", false);
			}
		}
	}
}
