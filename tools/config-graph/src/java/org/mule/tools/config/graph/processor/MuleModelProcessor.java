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

public class MuleModelProcessor extends TagProcessor {

    private final ExceptionStrategyProcessor exceptionStrategyProcessor;

    private DescriptorProcessor descriptorProcessor;

    public MuleModelProcessor(final EndpointRegistry endpointRegistry,
                              final GraphConfig config) {
        super(config);

        exceptionStrategyProcessor = new ExceptionStrategyProcessor(endpointRegistry, config);

        descriptorProcessor = new DescriptorProcessor(endpointRegistry, config);

    }

    public void parseModel(Graph graph, Element root) {

        List models = root.getChildren(MuleTag.ELEMENT_MODEL);
        for (Iterator iter = models.iterator(); iter.hasNext();) {
            Element modelElement = (Element) iter.next();
            if (config.isShowModels()) {
                String name = modelElement.getAttributeValue(MuleTag.ATTRIBUTE_NAME);
                String type = modelElement.getAttributeValue(MuleTag.ATTRIBUTE_TYPE);
                String className = modelElement.getAttributeValue(MuleTag.ATTRIBUTE_CLASS_NAME);
                if (type != null) {
                    if (type.equals("custom")) {
                        name += " (custom: " + className + ")";
                    } else {
                        name += " (" + type + ")";
                    }
                }
                GraphNode node = graph.addNode();
                node.getInfo().setHeader(name);
                node.getInfo().setFillColor(ColorRegistry.COLOR_MODEL);

                StringBuffer caption = new StringBuffer();
                appendProperties(modelElement, caption);
                appendDescription(modelElement, caption);
                node.getInfo().setCaption(caption.toString());

                exceptionStrategyProcessor.processExceptionStrategy(graph,
                        modelElement, node);
            }
            descriptorProcessor.parseModel(graph, modelElement);
        }
        if (models.size() == 0) {
            descriptorProcessor.parseModel(graph, root);
        }
    }
}
