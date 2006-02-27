package org.mule.tools.config.graph.processor;

import com.oy.shared.lm.graph.Graph;
import com.oy.shared.lm.graph.GraphNode;
import org.jdom.Element;
import org.mule.tools.config.graph.config.ColorRegistry;
import org.mule.tools.config.graph.config.GraphEnvironment;
import org.mule.tools.config.graph.util.MuleTag;

public class ConnectionStrategyProcessor extends TagProcessor {

	public ConnectionStrategyProcessor( GraphEnvironment environment ) {
		super(environment);
	}

	public void process(Graph graph, Element currentElement, GraphNode parent) {
        //Process connection strategy
        Element cs = currentElement.getChild(MuleTag.ELEMENT_CONNECTION_STRATEGY);
        if(cs!=null) {
            GraphNode csNode = graph.addNode();
            csNode.getInfo().setFillColor(ColorRegistry.COLOR_CONNECTION_STRATEGY);
            csNode.getInfo().setHeader(cs.getAttributeValue(MuleTag.ATTRIBUTE_CLASS_NAME));

            StringBuffer caption = new StringBuffer();
            appendProperties(cs, caption);
            appendDescription(cs, caption);
            csNode.getInfo().setCaption(caption.toString());
            addRelation(graph, parent, csNode, null);
        }
	}
}
