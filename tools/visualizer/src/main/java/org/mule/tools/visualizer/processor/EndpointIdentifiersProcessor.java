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

public class EndpointIdentifiersProcessor extends TagProcessor
{
    public EndpointIdentifiersProcessor(GraphEnvironment environment)
    {
        super(environment);
    }

    public void process(Graph graph, Element currentElement, GraphNode parent)
    {
        Element endpointIdentifiers = currentElement.getChild("endpoint-identifiers");

        if (endpointIdentifiers == null)
        {
            getEnvironment().log("no endpoint-identifiers tag");
            return;
        }

        List namedChildren = endpointIdentifiers.getChildren("endpoint-identifier");

        for (Iterator iter = namedChildren.iterator(); iter.hasNext();)
        {
            Element endpoint = (Element) iter.next();
            GraphNode node = graph.addNode();
            node.getInfo().setFillColor(ColorRegistry.COLOR_DEFINED_ENDPOINTS);
            String name = endpoint.getAttributeValue("name");

            String value = lookupPropertyTemplate(endpoint.getAttributeValue("value"));
            node.getInfo().setHeader(name);
            node.getInfo().setCaption(value);
            getEnvironment().getEndpointRegistry().addEndpoint(name, node);
            getEnvironment().getProperties().setProperty(name, value);

        }
    }
}
