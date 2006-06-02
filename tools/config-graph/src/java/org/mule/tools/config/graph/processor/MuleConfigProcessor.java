package org.mule.tools.config.graph.processor;

import com.oy.shared.lm.graph.Graph;
import com.oy.shared.lm.graph.GraphNode;
import org.jdom.Element;
import org.mule.tools.config.graph.config.ColorRegistry;
import org.mule.tools.config.graph.config.GraphEnvironment;
import org.mule.tools.config.graph.util.MuleTag;

public class MuleConfigProcessor extends TagProcessor {

    public MuleConfigProcessor( GraphEnvironment environment) {
        super(environment);
    }

    public void process(Graph graph, Element currentElement, GraphNode parent) {

        Element muleConfig = currentElement.getChild(MuleTag.ELEMENT_MULE_ENVIRONMENT_PROPERTIES);
        if(muleConfig!=null) {
            //Set whether the event flows are synchronous or not by default. This controls the style of arrows created
            String twoway = muleConfig.getAttributeValue(MuleTag.ATTRIBUTE_SYNCHRONOUS);
            environment.setDefaultTwoWay("true".equalsIgnoreCase(twoway));
            if(!environment.getConfig().isShowConfig()) return;

            GraphNode configNode = graph.addNode();
            configNode.getInfo().setFillColor(ColorRegistry.COLOR_CONFIG);
            configNode.getInfo().setHeader("Mule Config");

            StringBuffer caption = new StringBuffer();
            appendAttribute(muleConfig, MuleTag.ATTRIBUTE_SYNCHRONOUS, caption);
            appendAttribute(muleConfig, "serverUrl", caption);
            appendAttribute(muleConfig, "clientMode", caption);
            appendAttribute(muleConfig, "embedded", caption);
            appendAttribute(muleConfig, "enableMessageEvents", caption);
            appendAttribute(muleConfig, "encoding", caption);
            appendAttribute(muleConfig, "osEncoding", caption);
            appendAttribute(muleConfig, "recoverableMode", caption);
            appendAttribute(muleConfig, "remoteSync", caption);
            appendAttribute(muleConfig, "synchronousEventTimeout", caption);
            appendAttribute(muleConfig, "transactionTimeout", caption);
            appendAttribute(muleConfig, "workingDirectory", caption);


            ConnectionStrategyProcessor processor = new ConnectionStrategyProcessor(environment);
            processor.process(graph, muleConfig, configNode);
            appendProfiles(muleConfig, caption);
            appendDescription(muleConfig, caption);

            configNode.getInfo().setCaption(caption.toString());
        }
    }
}
