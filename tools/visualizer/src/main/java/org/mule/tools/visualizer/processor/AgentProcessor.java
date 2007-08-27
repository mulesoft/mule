/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
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

public class AgentProcessor extends TagProcessor
{
    public AgentProcessor(GraphEnvironment environment)
    {
        super(environment);
    }

    public void process(Graph graph, Element currentElement, GraphNode parent)
    {
        if (!getEnvironment().getConfig().isShowAgents())
        {
            return;
        }

        Element agents = currentElement.getChild(MuleTag.ELEMENT_AGENTS);
        if (agents == null)
        {
            return;
        }

        List agentsElement = agents.getChildren(MuleTag.ELEMENT_AGENT);
        for (Iterator iter = agentsElement.iterator(); iter.hasNext();)
        {
            Element connector = (Element) iter.next();
            GraphNode connectorNode = graph.addNode();
            connectorNode.getInfo().setFillColor(ColorRegistry.COLOR_AGENTS);
            String name = connector.getAttributeValue(MuleTag.ATTRIBUTE_NAME);
            connectorNode.getInfo().setHeader(name);

            StringBuffer caption = new StringBuffer();

            String className = connector.getAttributeValue(MuleTag.ATTRIBUTE_CLASS_NAME);
            caption.append(MuleTag.ATTRIBUTE_CLASS_NAME + " :" + className + "\n");

            appendProperties(connector, caption);
            appendDescription(connector, caption);
            connectorNode.getInfo().setCaption(caption.toString());
        }
    }
}
