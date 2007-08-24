/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.tools.visualizer.processor;

import org.mule.tools.visualizer.config.ColorRegistry;
import org.mule.tools.visualizer.config.GraphEnvironment;
import org.mule.tools.visualizer.util.MuleTag;

import com.oy.shared.lm.graph.Graph;
import com.oy.shared.lm.graph.GraphNode;

import org.jdom.Element;

public class InboundFilterProcessor extends TagProcessor
{

    private GraphNode endpointNode;

    public InboundFilterProcessor(GraphEnvironment environment, GraphNode endpointNode)
    {
        super(environment);
        this.endpointNode = endpointNode;
    }

    public void process(Graph graph, Element currentElement, GraphNode parent)
    {
        processInboundFilter(graph, currentElement, endpointNode, parent);
    }

    public void processInboundFilter(Graph graph, Element endpoint, GraphNode endpointNode, GraphNode parent)
    {
        Element filter = endpoint.getChild(MuleTag.ELEMENT_FILTER);
        boolean conditional = false;

        if (filter == null)
        {
            filter = endpoint.getChild(MuleTag.ELEMENT_LEFT_FILTER);
            conditional = filter != null;
        }

        if (filter != null)
        {

            GraphNode filterNode = graph.addNode();
            filterNode.getInfo().setHeader(filter.getAttributeValue(MuleTag.ATTRIBUTE_CLASS_NAME));
            filterNode.getInfo().setFillColor(ColorRegistry.COLOR_FILTER);
            StringBuffer caption = new StringBuffer();
            appendProperties(filter, caption);
            filterNode.getInfo().setCaption(caption.toString());
            // this is a hack to pick up and/or filter conditions
            // really we need a nice recursive way of doing this
            if (conditional)
            {
                filter = endpoint.getChild(MuleTag.ELEMENT_RIGHT_FILTER);
                GraphNode filterNode2 = graph.addNode();
                filterNode2.getInfo().setHeader(filter.getAttributeValue(MuleTag.ATTRIBUTE_CLASS_NAME));
                filterNode2.getInfo().setFillColor(ColorRegistry.COLOR_FILTER);
                StringBuffer caption2 = new StringBuffer();
                appendProperties(filter, caption2);
                filterNode2.getInfo().setCaption(caption2.toString());
                addEdge(graph, endpointNode, filterNode2, "filters on", isTwoWay(endpoint));
            }
            processInboundFilter(graph, filter, filterNode, parent);

            addEdge(graph, endpointNode, filterNode, "filters on", isTwoWay(endpoint));
        }
        else
        {
            addEdge(graph, endpointNode, parent, "in", isTwoWay(endpoint));
        }
    }

}
