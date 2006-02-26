package org.mule.tools.config.graph.components;

import com.oy.shared.lm.graph.Graph;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.mule.tools.config.graph.config.GraphConfig;
import org.mule.tools.config.graph.postprocessors.ExternalSystemPostProcessor;
import org.mule.tools.config.graph.postprocessors.NodeHiderPostProcessor;
import org.mule.tools.config.graph.postprocessors.UrlAssignerPostProcessor;
import org.mule.tools.config.graph.processor.AgentProcessor;
import org.mule.tools.config.graph.processor.ConnectorProcessor;
import org.mule.tools.config.graph.processor.MuleConfigProcessor;
import org.mule.tools.config.graph.processor.MuleModelProcessor;
import org.mule.tools.config.graph.processor.TagProcessor;
import org.mule.tools.config.graph.processor.TransformerProcessor;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class MuleParser extends TagProcessor {

    private final SAXBuilder builder;

        private final EndpointRegistry endpointRegistry;

        private final ConnectorProcessor connectorProcessor;
        private final AgentProcessor agentProcessor;
        private final TransformerProcessor transformerProcessor;
        private final MuleConfigProcessor configProcessor;

        private final MuleModelProcessor muleModelProcessor;

        private final List postProcessors = new ArrayList();


	public MuleParser(GraphConfig config,SAXBuilder builder) {
		super(config);
		this.builder = builder;
		this.endpointRegistry = new EndpointRegistry(config);
		this.connectorProcessor = new ConnectorProcessor(config);
		this.muleModelProcessor = new MuleModelProcessor(endpointRegistry,config);
		this.transformerProcessor = new TransformerProcessor(config);
		this.agentProcessor = new AgentProcessor(config);
		this.configProcessor = new MuleConfigProcessor(config);


		postProcessors.add(new UrlAssignerPostProcessor());
		postProcessors.add(new ExternalSystemPostProcessor());
        //Always set this to last as deleting nodes earlier seems to have adverse affects when adding nodes
        postProcessors.add(new NodeHiderPostProcessor());

	}

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

	    muleModelProcessor.parseModel(graph, root);
		connectorProcessor.parseConnectors(graph, root);
		transformerProcessor.parseTransformers(graph, root);
		agentProcessor.parseAgents(graph, root);
		configProcessor.parseConfig(graph, root);
	}

    public void finalise(Graph graph) {
        for (Iterator iter = postProcessors.iterator(); iter.hasNext();) {
			PostProcessor postProcessor = (PostProcessor) iter.next();
			postProcessor.postProcess(graph, config);
		}
    }

}
