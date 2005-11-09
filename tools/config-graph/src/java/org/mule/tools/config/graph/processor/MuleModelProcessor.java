package org.mule.tools.config.graph.processor;

import java.util.Iterator;
import java.util.List;

import org.jdom.Element;
import org.mule.tools.config.graph.components.EndpointRegistry;
import org.mule.tools.config.graph.config.GraphConfig;

import com.oy.shared.lm.graph.Graph;
import com.oy.shared.lm.graph.GraphNode;

public class MuleModelProcessor extends TagProcessor {

	private final ExceptionStrategyProcessor exceptionStrategyProcessor;

	private final ShortestNotationHandler shortestNotationHandler;

	private final InboundRoutersProcessor inboundRoutersProcessor;

	private final ResponseRouterProcessor responseRouterProcessor;

	private final OutBoundRoutersProcessor outBoundRoutersProcessor;

	public MuleModelProcessor(final EndpointRegistry endpointRegistry,
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
		List descriptors = model.getChildren("mule-descriptor");
		for (Iterator iter = descriptors.iterator(); iter.hasNext();) {
			Element descriptor = (Element) iter.next();
			String name = descriptor.getAttributeValue("name");
			GraphNode node = graph.addNode();
			node.getInfo().setHeader(name);
			node.getInfo().setFillColor("grey");

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

	private void appendProfiles(Element descriptor, StringBuffer caption) {
		Element threadingProfile = descriptor.getChild("threading-profile");
		if (threadingProfile != null) {
			caption.append("maxBufferSize = "
					+ threadingProfile.getAttributeValue("maxBufferSize")
					+ "\n");
			caption.append("threadTTL = "
					+ threadingProfile.getAttributeValue("threadTTL") + "\n");
			caption.append("maxThreadsActive = "
					+ threadingProfile.getAttributeValue("maxThreadsActive")
					+ "\n");
			caption.append("maxThreadsIdle = "
					+ threadingProfile.getAttributeValue("maxThreadsIdle")
					+ "\n");
		}
		Element poolingProfile = descriptor.getChild("pooling-profile");
		if (threadingProfile != null) {
			caption.append("exhaustedAction = "
					+ poolingProfile.getAttributeValue("exhaustedAction")
					+ "\n");
			caption.append("maxActive = "
					+ poolingProfile.getAttributeValue("maxActive") + "\n");
			caption.append("maxIdle = "
					+ poolingProfile.getAttributeValue("maxIdle") + "\n");
			caption.append("maxWait = "
					+ poolingProfile.getAttributeValue("maxWait") + "\n");
		}
	}

}
