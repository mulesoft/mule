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

import org.mule.tools.visualizer.config.GraphEnvironment;
import org.mule.tools.visualizer.util.MuleTag;

import com.oy.shared.lm.graph.Graph;
import com.oy.shared.lm.graph.GraphNode;

import org.jdom.Element;

public class ShortestNotationHandler extends TagProcessor
{

    public ShortestNotationHandler(GraphEnvironment environment)
    {
        super(environment);
    }

    public void process(Graph graph, Element currentElement, GraphNode parent)
    {
        String inbound = currentElement.getAttributeValue(MuleTag.ATTRIBUTE_INBOUNDENDPOINT);
        if (inbound != null)
        {
            GraphNode in = getEnvironment().getEndpointRegistry().getEndpoint(inbound,
                parent.getInfo().getHeader());
            if (in == null)
            {
                in = graph.addNode();
                in.getInfo().setCaption(inbound);
                getEnvironment().getEndpointRegistry().addEndpoint(inbound, in);
            }
            addEdge(graph, in, parent, "in", isTwoWay(null));
        }
        String outbound = currentElement.getAttributeValue(MuleTag.ATTRIBUTE_OUTBOUNDENDPOINT);
        if (outbound != null)
        {
            GraphNode out = getEnvironment().getEndpointRegistry().getEndpoint(outbound,
                parent.getInfo().getHeader());
            if (out == null)
            {
                out = graph.addNode();
                out.getInfo().setCaption(outbound);
                getEnvironment().getEndpointRegistry().addEndpoint(outbound, out);
            }
            addEdge(graph, parent, out, "out", isTwoWay(null));
        }

        String inboundTransformers = currentElement.getAttributeValue("inboundTransformer");
        if (inboundTransformers != null)
        {
            String[] transformers = inboundTransformers.split(" ");
            StringBuffer caption = new StringBuffer();
            for (int i = 0; i < transformers.length; i++)
            {
                caption.append("transformer " + i + " : " + transformers[i] + "\n");
            }
            parent.getInfo().setCaption(parent.getInfo().getCaption() + "\n" + caption.toString());
        }

        GraphNode[] virtual = getEnvironment().getEndpointRegistry().getVirtualEndpoint(
            parent.getInfo().getHeader());
        if (virtual.length > 0)
        {
            for (int i = 0; i < virtual.length; i++)
            {
                addEdge(graph, parent, virtual[i], "out (dynamic)", false);
            }
        }
    }
}
