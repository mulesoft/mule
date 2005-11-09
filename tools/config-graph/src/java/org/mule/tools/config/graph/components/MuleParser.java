package org.mule.tools.config.graph.components;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.mule.tools.config.graph.config.GraphConfig;
import org.mule.tools.config.graph.postprocessors.NodeHiderPostProcessor;
import org.mule.tools.config.graph.postprocessors.UrlAssignerPostProcessor;
import org.mule.tools.config.graph.processor.ConnectorProcessor;
import org.mule.tools.config.graph.processor.MuleModelProcessor;
import org.mule.tools.config.graph.processor.TagProcessor;

import com.oy.shared.lm.graph.Graph;

public class MuleParser extends TagProcessor {

	public MuleParser(GraphConfig config,SAXBuilder builder) {
		super(config);
		this.builder = builder;
		this.endpointRegistry = new EndpointRegistry(config);
		this.connectorProcessor = new ConnectorProcessor(config);
		this.muleModelProcessor = new MuleModelProcessor(endpointRegistry,config);
		
		postProcessors.add(new NodeHiderPostProcessor());
		postProcessors.add(new UrlAssignerPostProcessor());
		
	}

	private final SAXBuilder builder;

	private final EndpointRegistry endpointRegistry;

	private final ConnectorProcessor connectorProcessor;

	private final MuleModelProcessor muleModelProcessor;
	private final List postProcessors = new ArrayList();

	public void parseMuleConfig(File myFile, Graph graph) throws JDOMException,
			IOException {
		Document doc = builder.build(myFile);
		String caption = "";
		Element root = doc.getRootElement();
		if (caption == null) {
			caption = root.getAttribute("id").getValue();
			if (caption != null) {
				caption = caption.replaceAll("_", " ");
			} else {
				caption = "Mule Configuration";
			}
		}
		StringBuffer captionBuffer = new StringBuffer();
		captionBuffer.append(caption);
		appendDescription(root, captionBuffer);
		graph.getInfo().setCaption(captionBuffer.toString());

		endpointRegistry.parseEndpointIdentifiers(graph, root);

		endpointRegistry.parseEndpoints(graph, root);

		Element model = root.getChild("model");
		if (model != null) {
			muleModelProcessor.parseModel(graph, model);

		} else {
			muleModelProcessor.parseModel(graph, root);

		}
		connectorProcessor.parseConnectors(graph, root);

		for (Iterator iter = postProcessors.iterator(); iter.hasNext();) {
			PostProcessor postProcessor = (PostProcessor) iter.next();
			postProcessor.postProcess(graph,config);
		}
	}

	
}
