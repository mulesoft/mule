package org.mule.tools.config.graph.processor;

import org.jdom.Element;
import org.mule.tools.config.graph.config.ColorRegistry;
import org.mule.tools.config.graph.config.GraphConfig;
import org.mule.tools.config.graph.util.MuleTag;

import com.oy.shared.lm.graph.Graph;
import com.oy.shared.lm.graph.GraphNode;

public class InboundFilterProcessor extends TagProcessor {

	
	public InboundFilterProcessor(GraphConfig config) {
		super(config);
		
	}

	public void processInboundFilter(Graph graph, Element endpoint,
			GraphNode endpointNode, GraphNode parent) {
		Element filter = endpoint.getChild("filter");
		boolean conditional = false;

		if (filter == null) {
			filter = endpoint.getChild("left-filter");
			conditional = filter != null;
		}

		if (filter != null) {

			GraphNode filterNode = graph.addNode();
			filterNode.getInfo().setHeader(
					filter.getAttributeValue(MuleTag.ATTRIBUTE_CLASS_NAME));
			filterNode.getInfo().setFillColor(ColorRegistry.COLOR_FILTER);
			StringBuffer caption = new StringBuffer();
			appendProperties(filter, caption);
			filterNode.getInfo().setCaption(caption.toString());
			// this is a hack to pick up and/or filter conditions
			// really we need a nice recursive way of doing this
			if (conditional) {
				filter = endpoint.getChild("right-filter");
				GraphNode filterNode2 = graph.addNode();
				filterNode2.getInfo().setHeader(
						filter.getAttributeValue(MuleTag.ATTRIBUTE_CLASS_NAME));
				filterNode2.getInfo().setFillColor(ColorRegistry.COLOR_FILTER);
				StringBuffer caption2 = new StringBuffer();
				appendProperties(filter, caption2);
				filterNode2.getInfo().setCaption(caption2.toString());
				graph.addEdge(endpointNode, filterNode2).getInfo().setCaption(
						"filters on");
			}
			processInboundFilter(graph, filter, filterNode, parent);

			graph.addEdge(endpointNode, filterNode).getInfo().setCaption(
					"filters on");
		} else {
			graph.addEdge(endpointNode, parent).getInfo().setCaption("in");
		}
	}

}
