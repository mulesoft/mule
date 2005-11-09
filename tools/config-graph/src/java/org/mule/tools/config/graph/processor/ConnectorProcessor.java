package org.mule.tools.config.graph.processor;

import java.util.Iterator;
import java.util.List;

import org.jdom.Element;
import org.mule.tools.config.graph.config.ColorRegistry;
import org.mule.tools.config.graph.config.GraphConfig;
import org.mule.tools.config.graph.util.MuleTag;

import com.oy.shared.lm.graph.Graph;
import com.oy.shared.lm.graph.GraphNode;

public class ConnectorProcessor extends TagProcessor {
	public ConnectorProcessor(GraphConfig config) {
		super(config);
	}

	public void parseConnectors(Graph graph, Element root) {
		List connectorsElement = root.getChildren("connector");
		for (Iterator iter = connectorsElement.iterator(); iter.hasNext();) {
			Element connector = (Element) iter.next();
			GraphNode connectorNode = graph.addNode();
			connectorNode.getInfo().setFillColor(ColorRegistry.COLOR_CONNECTOR);
			String name = connector.getAttributeValue("name");
			connectorNode.getInfo().setHeader(name);

			StringBuffer caption = new StringBuffer();

			String className = connector.getAttributeValue(MuleTag.ATTRIBUTE_CLASS_NAME);
			caption.append("className :" + className + "\n");

			appendProperties(connector, caption);
			appendDescription(connector, caption);
			connectorNode.getInfo().setCaption(caption.toString());
		}
	}
}
