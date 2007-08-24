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

public class ExceptionStrategyProcessor extends TagProcessor
{

    public ExceptionStrategyProcessor(GraphEnvironment environment)
    {
        super(environment);
    }

    public void process(Graph graph, Element currentElement, GraphNode parent)
    {
        String edgeCaption = MuleTag.ELEMENT_CATCH_ALL_STRATEGY;
        Element exceptionStrategy = currentElement.getChild(edgeCaption);
        if (exceptionStrategy == null)
        {
            edgeCaption = MuleTag.ELEMENT_EXCEPTION_STRATEGY;
            exceptionStrategy = currentElement.getChild(edgeCaption);

        }

        if (exceptionStrategy != null)
        {
            String className = exceptionStrategy.getAttributeValue(MuleTag.ATTRIBUTE_CLASS_NAME);
            GraphNode exceptionNode = graph.addNode();
            exceptionNode.getInfo().setHeader(className);
            exceptionNode.getInfo().setFillColor(ColorRegistry.COLOR_EXCEPTION_STRATEGY);

            addEdge(graph, parent, exceptionNode, edgeCaption, false);
            Element endpoint = exceptionStrategy.getChild(MuleTag.ELEMENT_ENDPOINT);
            if (endpoint == null)
            {
                endpoint = exceptionStrategy.getChild(MuleTag.ELEMENT_GLOBAL_ENDPOINT);
            }
            if (endpoint != null)
            {
                String url = endpoint.getAttributeValue(MuleTag.ATTRIBUTE_ADDRESS);
                if (url == null)
                {
                    url = endpoint.getAttributeValue(MuleTag.ATTRIBUTE_NAME);
                }
                if (url != null)
                {
                    GraphNode out = getEnvironment().getEndpointRegistry().getEndpoint(url,
                        parent.getInfo().getHeader());
                    if (out == null)
                    {
                        out = graph.addNode();
                        out.getInfo().setCaption(url);
                        getEnvironment().getEndpointRegistry().addEndpoint(url, out);
                    }
                    addEdge(graph, exceptionNode, out, "out", false);
                }
            }
        }

    }
}
