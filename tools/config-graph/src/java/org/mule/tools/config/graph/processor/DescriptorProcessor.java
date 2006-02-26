package org.mule.tools.config.graph.processor;

import com.oy.shared.lm.graph.Graph;
import com.oy.shared.lm.graph.GraphNode;
import org.jdom.Element;
import org.mule.tools.config.graph.components.EndpointRegistry;
import org.mule.tools.config.graph.config.ColorRegistry;
import org.mule.tools.config.graph.config.GraphConfig;
import org.mule.tools.config.graph.util.MuleTag;

import java.util.Iterator;
import java.util.List;

public class DescriptorProcessor extends TagProcessor {

	private final ExceptionStrategyProcessor exceptionStrategyProcessor;

	private final ShortestNotationHandler shortestNotationHandler;

	private final InboundRoutersProcessor inboundRoutersProcessor;

	private final ResponseRouterProcessor responseRouterProcessor;

	private final OutBoundRoutersProcessor outBoundRoutersProcessor;

	public DescriptorProcessor(final EndpointRegistry endpointRegistry,
			final GraphConfig config) {
		super(config);
		this.shortestNotationHandler = new ShortestNotationHandler(
				endpointRegistry);
		this.inboundRoutersProcessor = new InboundRoutersProcessor(
				endpointRegistry, config);
		this.responseRouterProcessor = new ResponseRouterProcessor(
				endpointRegistry);
		exceptionStrategyProcessor = new ExceptionStrategyProcessor(
				endpointRegistry, config);
		OutBoundRouterEndpointsHandler outBoundRouterEndpointsHandler = new OutBoundRouterEndpointsHandler(
				endpointRegistry, config);

		this.outBoundRoutersProcessor = new OutBoundRoutersProcessor(config,
				exceptionStrategyProcessor, outBoundRouterEndpointsHandler,
				endpointRegistry);

	}

	public void parseModel(Graph graph, Element model) {
		if (model == null) {
			System.err.println("model is null");
			return;
		}

		List descriptors = model.getChildren(MuleTag.ELEMENT_MULE_DESCRIPTOR);
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

			shortestNotationHandler.processShortestNotation(graph, descriptor,
					node);

			exceptionStrategyProcessor.processExceptionStrategy(graph,
					descriptor, node);

			inboundRoutersProcessor.processInboundRouters(graph, descriptor,
					node);

			outBoundRoutersProcessor.processOutBoundRouters(graph, descriptor,
					node);

			responseRouterProcessor.processResponseRouter(graph, descriptor,
					node);

		}

	}

}
