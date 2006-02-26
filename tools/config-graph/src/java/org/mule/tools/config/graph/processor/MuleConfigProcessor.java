package org.mule.tools.config.graph.processor;

import com.oy.shared.lm.graph.Graph;
import com.oy.shared.lm.graph.GraphNode;
import org.jdom.Element;
import org.mule.tools.config.graph.config.ColorRegistry;
import org.mule.tools.config.graph.config.GraphConfig;
import org.mule.tools.config.graph.util.MuleTag;

public class MuleConfigProcessor extends TagProcessor {

    private ConnectionStrategyProcessor connectionStrategyProcessor;

	public MuleConfigProcessor(GraphConfig config) {
		super(config);
        connectionStrategyProcessor =new ConnectionStrategyProcessor(config);
	}

	public void parseConfig(Graph graph, Element root) {
        if(!config.isShowConfig()) return;
        
		Element config = root.getChild(MuleTag.ELEMENT_MULE_ENVIRONMENT_PROPERTIES);
		if(config!=null) {
			GraphNode configNode = graph.addNode();
			configNode.getInfo().setFillColor(ColorRegistry.COLOR_CONFIG);
			configNode.getInfo().setHeader("Mule Config");

			StringBuffer caption = new StringBuffer();
            appendAttribute(config, MuleTag.ATTRIBUTE_SYNCHRONOUS, caption);
            appendAttribute(config, "serverUrl", caption);
            appendAttribute(config, "clientMode", caption);
            appendAttribute(config, "embedded", caption);
            appendAttribute(config, "enableMessageEvents", caption);
            appendAttribute(config, "encoding", caption);
            appendAttribute(config, "osEncoding", caption);
            appendAttribute(config, "recoverableMode", caption);
            appendAttribute(config, "remoteSync", caption);
            appendAttribute(config, "synchronousEventTimeout", caption);
            appendAttribute(config, "transactionTimeout", caption);
            appendAttribute(config, "workingDirectory", caption);


            connectionStrategyProcessor.parseConnectionStrategy(graph, config, configNode);
			appendProfiles(config, caption);
			appendDescription(config, caption);

			configNode.getInfo().setCaption(caption.toString());
		}
	}
}
