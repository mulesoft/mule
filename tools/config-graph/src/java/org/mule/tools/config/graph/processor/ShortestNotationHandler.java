/*
 * $Id: $
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
import org.jdom.Element;
import org.mule.tools.config.graph.config.GraphEnvironment;
import org.mule.tools.config.graph.util.MuleTag;

public class ShortestNotationHandler extends TagProcessor
{

    public ShortestNotationHandler(GraphEnvironment environment)
    {
        super(environment);
    }

    public void process(Graph graph, Element currentElement, GraphNode parent)
    {
        String inbound = currentElement.getAttributeValue(MuleTag.ATTRIBUTE_INBOUNDENDPOINT);
        if (inbound != null) {
            GraphNode in = environment.getEndpointRegistry().getEndpoint(inbound,
                            parent.getInfo().getHeader());
            if (in == null) {
                in = graph.addNode();
                in.getInfo().setCaption(inbound);
                environment.getEndpointRegistry().addEndpoint(inbound, in);
            }
            addEdge(graph, in, parent, "in", isTwoWay(null));
        }
        String outbound = currentElement.getAttributeValue(MuleTag.ATTRIBUTE_OUTBOUNDENDPOINT);
        if (outbound != null) {
            GraphNode out = environment.getEndpointRegistry().getEndpoint(outbound,
                            parent.getInfo().getHeader());
            if (out == null) {
                out = graph.addNode();
                out.getInfo().setCaption(outbound);
                environment.getEndpointRegistry().addEndpoint(outbound, out);
            }
            addEdge(graph, parent, out, "out", isTwoWay(null));
        }

        String inboundTransformers = currentElement.getAttributeValue("inboundTransformer");
        if (inboundTransformers != null) {
            String[] transformers = inboundTransformers.split(" ");
            StringBuffer caption = new StringBuffer();
            for (int i = 0; i < transformers.length; i++) {
                caption.append("transformer " + i + " : " + transformers[i] + "\n");
            }
            parent.getInfo().setCaption(parent.getInfo().getCaption() + "\n" + caption.toString());
        }

        GraphNode[] virtual = environment.getEndpointRegistry().getVirtualEndpoint(
                        parent.getInfo().getHeader());
        if (virtual.length > 0) {
            for (int i = 0; i < virtual.length; i++) {
                addEdge(graph, parent, virtual[i], "out (dynamic)", false);
            }
        }
    }
}
