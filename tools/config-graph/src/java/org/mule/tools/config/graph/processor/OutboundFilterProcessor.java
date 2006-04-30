package org.mule.tools.config.graph.processor;

import com.oy.shared.lm.graph.Graph;
import com.oy.shared.lm.graph.GraphNode;
import org.jdom.Element;
import org.mule.tools.config.graph.config.ColorRegistry;
import org.mule.tools.config.graph.config.GraphEnvironment;
import org.mule.tools.config.graph.util.MuleTag;

public class OutboundFilterProcessor extends TagProcessor{

    private GraphNode endpointNode;

      public OutboundFilterProcessor(GraphEnvironment environment , GraphNode endpointNode) {
        super(environment);
          this.endpointNode = endpointNode;
    }

    public void process(Graph graph, Element currentElement, GraphNode parent) {
        process(graph, currentElement, endpointNode, parent);
    }

    /**
     *
     * todo doesn't currently support And/Or logic filters
     */
    private void process(Graph graph, Element endpoint, GraphNode endpointNode, GraphNode parent) {
        // TODO doesn't currently support And/Or logic filters
        Element filter=endpoint.getChild(MuleTag.ELEMENT_FILTER);
        if (filter == null) filter=endpoint.getChild(MuleTag.ELEMENT_LEFT_FILTER);
        if (filter == null) filter=endpoint.getChild(MuleTag.ELEMENT_RIGHT_FILTER);

        if (filter != null) {
            GraphNode filterNode = graph.addNode();
            filterNode.getInfo().setHeader(filter.getAttributeValue(MuleTag.ATTRIBUTE_CLASS_NAME));
            filterNode.getInfo().setFillColor(ColorRegistry.COLOR_FILTER);
            StringBuffer caption = new StringBuffer();
            appendProperties(filter, caption);
            filterNode.getInfo().setCaption(caption.toString());
            
            process(graph, filter, filterNode, parent);
            addEdge(graph, filterNode, endpointNode, "out", isTwoWay(endpoint));

        } else {
            addEdge(graph, parent, endpointNode, "filters on", isTwoWay(endpoint));
        }
    }
}
