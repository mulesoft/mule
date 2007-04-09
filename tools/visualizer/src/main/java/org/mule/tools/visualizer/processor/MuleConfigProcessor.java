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

import org.jdom.Element;

public class MuleConfigProcessor extends TagProcessor
{

    public MuleConfigProcessor(GraphEnvironment environment)
    {
        super(environment);
    }

    public void process(Graph graph, Element currentElement, GraphNode parent)
    {

        Element muleConfig = currentElement.getChild(MuleTag.ELEMENT_MULE_ENVIRONMENT_PROPERTIES);
        if (muleConfig != null)
        {
            // Set whether the event flows are synchronous or not by default. This
            // controls the style of arrows created
            String twoway = muleConfig.getAttributeValue(MuleTag.ATTRIBUTE_SYNCHRONOUS);
            getEnvironment().setDefaultTwoWay("true".equalsIgnoreCase(twoway));
            if (!getEnvironment().getConfig().isShowConfig())
            {
                return;
            }

            GraphNode configNode = graph.addNode();
            configNode.getInfo().setFillColor(ColorRegistry.COLOR_CONFIG);
            configNode.getInfo().setHeader("Mule Config");

            StringBuffer caption = new StringBuffer();
            appendAttribute(muleConfig, MuleTag.ATTRIBUTE_SYNCHRONOUS, caption);
            appendAttribute(muleConfig, "serverUrl", caption);
            appendAttribute(muleConfig, "clientMode", caption);
            appendAttribute(muleConfig, "embedded", caption);
            appendAttribute(muleConfig, "enableMessageEvents", caption);
            appendAttribute(muleConfig, "encoding", caption);
            appendAttribute(muleConfig, "osEncoding", caption);
            appendAttribute(muleConfig, "recoverableMode", caption);
            appendAttribute(muleConfig, "remoteSync", caption);
            appendAttribute(muleConfig, "synchronousEventTimeout", caption);
            appendAttribute(muleConfig, "transactionTimeout", caption);
            appendAttribute(muleConfig, "workingDirectory", caption);

            ConnectionStrategyProcessor processor = new ConnectionStrategyProcessor(getEnvironment());
            processor.process(graph, muleConfig, configNode);
            appendProfiles(muleConfig, caption);
            appendDescription(muleConfig, caption);

            configNode.getInfo().setCaption(caption.toString());
        }
    }
}
