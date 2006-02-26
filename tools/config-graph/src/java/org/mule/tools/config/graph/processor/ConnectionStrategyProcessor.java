package org.mule.tools.config.graph.processor;

import com.oy.shared.lm.graph.Graph;
import com.oy.shared.lm.graph.GraphNode;
import org.jdom.Element;
import org.mule.tools.config.graph.config.ColorRegistry;
import org.mule.tools.config.graph.config.GraphConfig;
import org.mule.tools.config.graph.util.MuleTag;

public class ConnectionStrategyProcessor extends TagProcessor {
	public ConnectionStrategyProcessor(GraphConfig config) {
		super(config);
	}

	public void parseConnectionStrategy(Graph graph, Element connector, GraphNode parent) {
        //Process connection strategy
        Element cs = connector.getChild(MuleTag.ELEMENT_CONNECTION_STRATEGY);
        if(cs!=null) {
            GraphNode csNode = graph.addNode();
            csNode.getInfo().setFillColor(ColorRegistry.COLOR_CONNECTION_STRATEGY);
            csNode.getInfo().setHeader(cs.getAttributeValue(MuleTag.ATTRIBUTE_CLASS_NAME));

            StringBuffer caption = new StringBuffer();
            appendProperties(cs, caption);
            appendDescription(cs, caption);
            csNode.getInfo().setCaption(caption.toString());
            graph.addEdge(parent, csNode).getInfo().setArrowHeadNone();

        }
	}
}
