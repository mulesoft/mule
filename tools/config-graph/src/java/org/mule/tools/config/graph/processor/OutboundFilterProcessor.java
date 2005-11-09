package org.mule.tools.config.graph.processor;

import org.jdom.Element;
import org.mule.tools.config.graph.config.ColorRegistry;
import org.mule.tools.config.graph.config.GraphConfig;
import org.mule.tools.config.graph.util.MuleTag;

import com.oy.shared.lm.graph.Graph;
import com.oy.shared.lm.graph.GraphNode;

public class OutboundFilterProcessor extends TagProcessor{

	  public OutboundFilterProcessor(GraphConfig config) {
		super(config);
	}

	/**
	 * 
	 * @todo doesn't currently support And/Or logic filters
	 */
    public void process(Graph graph, Element endpoint, GraphNode endpointNode, GraphNode parent) {
    	// TODO doesn't currently support And/Or logic filters
        Element filter=endpoint.getChild("filter");
        if (filter == null) filter=endpoint.getChild("left-filter");
        if (filter == null) filter=endpoint.getChild("right-filter");

        if (filter != null) {
            GraphNode filterNode = graph.addNode();
            filterNode.getInfo().setHeader(filter.getAttributeValue(MuleTag.ATTRIBUTE_CLASS_NAME));
            filterNode.getInfo().setFillColor(ColorRegistry.COLOR_FILTER);
            StringBuffer caption = new StringBuffer();
            appendProperties(filter, caption);
            filterNode.getInfo().setCaption(caption.toString());
            
            process(graph, filter, filterNode, parent);
            graph.addEdge(filterNode, endpointNode).getInfo().setCaption("out");
        } else {
            graph.addEdge(parent, endpointNode).getInfo().setCaption("filters on");
        }
    }
	
    
}
