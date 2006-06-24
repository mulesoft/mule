/*
 * $Id$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */

package org.mule.tools.config.graph.processor;

import com.oy.shared.lm.graph.Graph;
import com.oy.shared.lm.graph.GraphNode;

import java.util.Iterator;
import java.util.List;

import org.jdom.Element;
import org.mule.tools.config.graph.config.ColorRegistry;
import org.mule.tools.config.graph.config.GraphEnvironment;
import org.mule.tools.config.graph.util.MuleTag;

public class ConnectorProcessor extends TagProcessor
{

    public ConnectorProcessor(GraphEnvironment environment)
    {
        super(environment);
    }

    public void process(Graph graph, Element currentElement, GraphNode parent)
    {
        if (!environment.getConfig().isShowConnectors()) return;

        List connectorsElement = currentElement.getChildren(MuleTag.ELEMENT_CONNECTOR);
        for (Iterator iter = connectorsElement.iterator(); iter.hasNext();) {
            Element connector = (Element)iter.next();
            GraphNode connectorNode = graph.addNode();
            connectorNode.getInfo().setFillColor(ColorRegistry.COLOR_CONNECTOR);
            String name = connector.getAttributeValue(MuleTag.ATTRIBUTE_NAME);
            connectorNode.getInfo().setHeader(name);

            StringBuffer caption = new StringBuffer();

            String className = connector.getAttributeValue(MuleTag.ATTRIBUTE_CLASS_NAME);
            caption.append(MuleTag.ATTRIBUTE_CLASS_NAME + " :" + className + "\n");

            appendProfiles(connector, caption);
            appendProperties(connector, caption);
            appendDescription(connector, caption);
            connectorNode.getInfo().setCaption(caption.toString());

            // Process connection strategy
            ConnectionStrategyProcessor connectionStrategyProcessor = new ConnectionStrategyProcessor(
                            environment);
            connectionStrategyProcessor.process(graph, connector, connectorNode);
        }
    }
}
