/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the BSD style
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.tools.visualizer.processor;

import org.mule.tools.visualizer.config.ColorRegistry;
import org.mule.tools.visualizer.config.GraphEnvironment;
import org.mule.tools.visualizer.util.MuleTag;

import com.oy.shared.lm.graph.Graph;
import com.oy.shared.lm.graph.GraphNode;

import java.util.Iterator;
import java.util.List;

import org.jdom.Element;

public class ConnectorProcessor extends TagProcessor
{

    public ConnectorProcessor(GraphEnvironment environment)
    {
        super(environment);
    }

    public void process(Graph graph, Element currentElement, GraphNode parent)
    {
        if (!getEnvironment().getConfig().isShowConnectors())
        {
            return;
        }

        List connectorsElement = currentElement.getChildren(MuleTag.ELEMENT_CONNECTOR);
        for (Iterator iter = connectorsElement.iterator(); iter.hasNext();)
        {
            Element connector = (Element) iter.next();
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
                getEnvironment());
            connectionStrategyProcessor.process(graph, connector, connectorNode);
        }
    }
}
