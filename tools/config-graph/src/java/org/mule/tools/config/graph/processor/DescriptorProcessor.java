package org.mule.tools.config.graph.processor;

import com.oy.shared.lm.graph.Graph;
import com.oy.shared.lm.graph.GraphNode;
import org.jdom.Element;
import org.mule.tools.config.graph.config.ColorRegistry;
import org.mule.tools.config.graph.config.GraphEnvironment;
import org.mule.tools.config.graph.util.MuleTag;

import java.util.Iterator;
import java.util.List;

public class DescriptorProcessor extends TagProcessor {

	private final ExceptionStrategyProcessor exceptionStrategyProcessor;

	private final ShortestNotationHandler shortestNotationHandler;

	private final InboundRoutersProcessor inboundRoutersProcessor;

	private final ResponseRouterProcessor responseRouterProcessor;

	private final OutBoundRoutersProcessor outBoundRoutersProcessor;

	public DescriptorProcessor( GraphEnvironment environment) {
		super(environment);
		this.shortestNotationHandler = new ShortestNotationHandler(environment);
		this.inboundRoutersProcessor = new InboundRoutersProcessor(environment);
		this.responseRouterProcessor = new ResponseRouterProcessor(environment);
		exceptionStrategyProcessor = new ExceptionStrategyProcessor(environment);

		this.outBoundRoutersProcessor = new OutBoundRoutersProcessor(environment);

	}

	public void process(Graph graph, Element currentElement, GraphNode parent) {
		if (currentElement == null) {
			System.err.println("model is null");
			return;
		}

		List descriptors = currentElement.getChildren(MuleTag.ELEMENT_MULE_DESCRIPTOR);
		for (Iterator iter = descriptors.iterator(); iter.hasNext();) {
			Element descriptor = (Element) iter.next();
			String name = descriptor.getAttributeValue(MuleTag.ATTRIBUTE_NAME);
			GraphNode node = graph.addNode();
			node.getInfo().setHeader(name);
			node.getInfo().setFillColor(ColorRegistry.COLOR_COMPONENT);

			StringBuffer caption = new StringBuffer();

		/*	caption.append("implementation : "+descriptor.getAttributeValue("implementation")
					+ "\n");*/

			appendProfiles(descriptor, caption);
			appendProperties(descriptor, caption);
			appendDescription(descriptor, caption);

			node.getInfo().setCaption(caption.toString());

			shortestNotationHandler.process(graph, descriptor, node);

			exceptionStrategyProcessor.process(graph, descriptor, node);

			inboundRoutersProcessor.process(graph, descriptor, node);

			outBoundRoutersProcessor.process(graph, descriptor, node);

			responseRouterProcessor.process(graph, descriptor, node);

		}

	}

}
