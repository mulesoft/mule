package org.mule.tools.config.graph.processor;

import com.oy.shared.lm.graph.Graph;
import com.oy.shared.lm.graph.GraphNode;
import org.jdom.Element;
import org.mule.tools.config.graph.config.ColorRegistry;
import org.mule.tools.config.graph.config.GraphEnvironment;
import org.mule.tools.config.graph.util.MuleTag;

public class ExceptionStrategyProcessor extends TagProcessor{

	public ExceptionStrategyProcessor( GraphEnvironment environment) {
		super(environment);
	}

	public void process(Graph graph, Element currentElement, GraphNode parent) {
		String edgeCaption = MuleTag.ELEMENT_CATCH_ALL_STRATEGY;
        Element exceptionStrategy = currentElement.getChild(edgeCaption);
		if (exceptionStrategy == null) {
            edgeCaption = MuleTag.ELEMENT_EXCEPTION_STRATEGY;
			exceptionStrategy = currentElement.getChild(edgeCaption);

        }

		if (exceptionStrategy != null) {

			String className = exceptionStrategy.getAttributeValue(MuleTag.ATTRIBUTE_CLASS_NAME);
			GraphNode exceptionNode = graph.addNode();
			exceptionNode.getInfo().setHeader(className);
			exceptionNode.getInfo().setFillColor(ColorRegistry.COLOR_EXCEPTION_STRATEGY);

            addEdge(graph, parent, exceptionNode, edgeCaption, false);
			Element endpoint = exceptionStrategy.getChild(MuleTag.ELEMENT_ENDPOINT);
			if (endpoint != null) {
				String url = endpoint.getAttributeValue(MuleTag.ATTRIBUTE_ADDRESS);
				if (url != null) {
					GraphNode out = (GraphNode) environment.getEndpointRegistry().getEndpoint(url, parent.getInfo()
							.getHeader());
					if (out == null) {
						out = graph.addNode();
						out.getInfo().setCaption(url);
						environment.getEndpointRegistry().addEndpoint(url, out);
					}
					addEdge(graph, exceptionNode, out, "out", false);
				}
			}
		}

	}
}
