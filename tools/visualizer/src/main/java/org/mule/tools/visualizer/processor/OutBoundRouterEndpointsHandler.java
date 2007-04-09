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

import org.mule.tools.visualizer.config.GraphEnvironment;
import org.mule.tools.visualizer.util.MuleTag;

import com.oy.shared.lm.graph.Graph;
import com.oy.shared.lm.graph.GraphNode;

import java.util.Iterator;
import java.util.List;

import org.jdom.Element;

public class OutBoundRouterEndpointsHandler extends TagProcessor
{

    private String componentName;

    public OutBoundRouterEndpointsHandler(GraphEnvironment environment, String componentName)
    {
        super(environment);
        this.componentName = componentName;
    }

    public void process(Graph graph, Element currentElement, GraphNode parent)
    {
        List epList = currentElement.getChildren(MuleTag.ELEMENT_ENDPOINT);
        process(graph, epList, parent);

        epList = currentElement.getChildren(MuleTag.ELEMENT_GLOBAL_ENDPOINT);
        process(graph, epList, parent);
    }

    public void process(Graph graph, List epList, GraphNode parent)
    {
        int x = 1;
        for (Iterator iterator = epList.iterator(); iterator.hasNext(); x++)
        {
            Element outEndpoint = (Element) iterator.next();

            String url = outEndpoint.getAttributeValue(MuleTag.ATTRIBUTE_ADDRESS);
            if (url == null)
            {
                url = outEndpoint.getAttributeValue(MuleTag.ATTRIBUTE_NAME);
            }

            if (url != null)
            {
                GraphNode out = getEnvironment().getEndpointRegistry().getEndpoint(url, componentName);
                if (out == null)
                {
                    out = graph.addNode();
                    StringBuffer caption = new StringBuffer();
                    // caption.append(url).append("\n");
                    appendProperties(outEndpoint, caption);
                    appendDescription(outEndpoint, caption);
                    out.getInfo().setCaption(caption.toString());
                    getEnvironment().getEndpointRegistry().addEndpoint(url, out);
                    processOutboundFilter(graph, outEndpoint, out, parent);
                }
                else
                {
                    String caption = "out";
                    if (epList.size() > 1)
                    {
                        caption += " (" + x + " of " + epList.size() + ")";
                    }
                    addEdge(graph, parent, out, caption, isTwoWay(outEndpoint));

                }
            }

            GraphNode[] virtual = getEnvironment().getEndpointRegistry().getVirtualEndpoint(componentName);
            if (virtual.length > 0)
            {
                for (int i = 0; i < virtual.length; i++)
                {
                    addEdge(graph, parent, virtual[i], "out (dynamic)", isTwoWay(outEndpoint));
                }
            }
        }
    }

    private void processOutboundFilter(Graph graph, Element outEndpoint, GraphNode out, GraphNode routerNode)
    {

        OutboundFilterProcessor processor = new OutboundFilterProcessor(getEnvironment(), out);
        processor.process(graph, outEndpoint, routerNode);
    }

}
