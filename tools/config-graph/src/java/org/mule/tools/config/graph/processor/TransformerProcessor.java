package org.mule.tools.config.graph.processor;

import com.oy.shared.lm.graph.Graph;
import com.oy.shared.lm.graph.GraphNode;
import org.jdom.Element;
import org.mule.tools.config.graph.config.ColorRegistry;
import org.mule.tools.config.graph.config.GraphConfig;
import org.mule.tools.config.graph.util.MuleTag;

import java.util.Iterator;
import java.util.List;

public class TransformerProcessor extends TagProcessor {
	public TransformerProcessor(GraphConfig config) {
		super(config);
	}

	public void parseTransformers(Graph graph, Element root) {
        if(!config.isShowTransformers()) return;

        Element transformers = root.getChild(MuleTag.ELEMENT_TRANSFORMERS);
        if(transformers==null) return;

		List agentsElement = transformers.getChildren(MuleTag.ELEMENT_TRANSFORMER);
		for (Iterator iter = agentsElement.iterator(); iter.hasNext();) {
			Element connector = (Element) iter.next();
			GraphNode connectorNode = graph.addNode();
			connectorNode.getInfo().setFillColor(ColorRegistry.COLOR_TRANSFORMER);
			String name = connector.getAttributeValue(MuleTag.ATTRIBUTE_NAME);
			String returnClass = connector.getAttributeValue(MuleTag.ATTRIBUTE_RETURN_CLASS);
			connectorNode.getInfo().setHeader(name + ", return: " + returnClass);

			StringBuffer caption = new StringBuffer();

			String className = connector.getAttributeValue(MuleTag.ATTRIBUTE_CLASS_NAME);
			caption.append(MuleTag.ATTRIBUTE_CLASS_NAME + " :" + className + "\n");

			appendProperties(connector, caption);
			appendDescription(connector, caption);
			connectorNode.getInfo().setCaption(caption.toString());
		}
	}
}
