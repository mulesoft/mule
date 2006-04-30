package org.mule.tools.config.graph.processor;

import com.oy.shared.lm.graph.Graph;
import com.oy.shared.lm.graph.GraphNode;
import org.jdom.Element;
import org.mule.tools.config.graph.config.ColorRegistry;
import org.mule.tools.config.graph.config.GraphEnvironment;
import org.mule.tools.config.graph.util.MuleTag;

import java.util.Iterator;
import java.util.List;

public class AgentProcessor extends TagProcessor {
    public AgentProcessor(GraphEnvironment environment) {
        super(environment);
    }

    public void process(Graph graph, Element currentElement, GraphNode parent) {
        if(!environment.getConfig().isShowAgents()) return;

        Element agents = currentElement.getChild(MuleTag.ELEMENT_AGENTS);
        if(agents==null) return;

        List agentsElement = agents.getChildren(MuleTag.ELEMENT_AGENT);
        for (Iterator iter = agentsElement.iterator(); iter.hasNext();) {
            Element connector = (Element) iter.next();
            GraphNode connectorNode = graph.addNode();
            connectorNode.getInfo().setFillColor(ColorRegistry.COLOR_AGENTS);
            String name = connector.getAttributeValue(MuleTag.ATTRIBUTE_NAME);
            connectorNode.getInfo().setHeader(name);

            StringBuffer caption = new StringBuffer();

            String className = connector.getAttributeValue(MuleTag.ATTRIBUTE_CLASS_NAME);
            caption.append(MuleTag.ATTRIBUTE_CLASS_NAME + " :" + className + "\n");

            appendProperties(connector, caption);
            appendDescription(connector, caption);
            connectorNode.getInfo().setCaption(caption.toString());
        }
    }
}
