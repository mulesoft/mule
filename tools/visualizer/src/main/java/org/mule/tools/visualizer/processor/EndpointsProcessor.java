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

import com.oy.shared.lm.graph.Graph;
import com.oy.shared.lm.graph.GraphNode;

import java.util.Iterator;
import java.util.List;

import org.jdom.Element;

public class EndpointsProcessor extends TagProcessor
{
    public EndpointsProcessor(GraphEnvironment environment)
    {
        super(environment);
    }

    public void process(Graph graph, Element currentElement, GraphNode parent)
    {
        Element globalEndpoints = currentElement.getChild("global-endpoints");

        if (globalEndpoints == null)
        {
            getEnvironment().log("no global-endpoints");
            return;
        }

        List namedChildren = globalEndpoints.getChildren("endpoint");

        for (Iterator iter = namedChildren.iterator(); iter.hasNext();)
        {
            Element endpoint = (Element) iter.next();
            GraphNode node = graph.addNode();
            node.getInfo().setFillColor(ColorRegistry.COLOR_DEFINED_ENDPOINTS);
            String name = endpoint.getAttributeValue("name");

            node.getInfo().setHeader(endpoint.getAttributeValue("address") + " (" + name + ")");
            StringBuffer caption = new StringBuffer();
            TagProcessor.appendProperties(endpoint, caption);
            node.getInfo().setCaption(caption.toString());
            getEnvironment().getEndpointRegistry().addEndpoint(name, node);
        }
    }
}
