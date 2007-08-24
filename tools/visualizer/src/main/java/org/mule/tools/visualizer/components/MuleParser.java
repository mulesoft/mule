/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.tools.visualizer.components;

import org.mule.tools.visualizer.config.GraphEnvironment;
import org.mule.tools.visualizer.postprocessors.ExternalSystemPostProcessor;
import org.mule.tools.visualizer.postprocessors.NodeHiderPostProcessor;
import org.mule.tools.visualizer.postprocessors.UrlAssignerPostProcessor;
import org.mule.tools.visualizer.processor.AgentProcessor;
import org.mule.tools.visualizer.processor.ConnectorProcessor;
import org.mule.tools.visualizer.processor.EndpointIdentifiersProcessor;
import org.mule.tools.visualizer.processor.EndpointsProcessor;
import org.mule.tools.visualizer.processor.MuleConfigProcessor;
import org.mule.tools.visualizer.processor.MuleModelProcessor;
import org.mule.tools.visualizer.processor.TagProcessor;
import org.mule.tools.visualizer.processor.TransformerProcessor;

import com.oy.shared.lm.graph.Graph;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;

public class MuleParser
{

    private final SAXBuilder builder;
    private final List processors = new ArrayList();
    private final List postProcessors = new ArrayList();
    private GraphEnvironment env;

    public MuleParser(GraphEnvironment env, SAXBuilder builder)
    {
        this.env = env;
        this.builder = builder;
        processors.add(new MuleConfigProcessor(env));
        processors.add(new EndpointIdentifiersProcessor(env));
        processors.add(new EndpointsProcessor(env));
        processors.add(new ConnectorProcessor(env));
        processors.add(new MuleModelProcessor(env));
        processors.add(new TransformerProcessor(env));
        processors.add(new AgentProcessor(env));

        postProcessors.add(new UrlAssignerPostProcessor());
        postProcessors.add(new ExternalSystemPostProcessor());
        // Always set this to last as deleting nodes earlier seems to have adverse
        // affects
        // when adding nodes
        postProcessors.add(new NodeHiderPostProcessor());

    }

    public void parseMuleConfig(InputStream in, Graph graph) throws JDOMException, IOException
    {
        Document doc = builder.build(in);
        parseMuleConfig(doc, graph);
    }

    public void parseMuleConfig(File myFile, Graph graph) throws JDOMException, IOException
    {
        Document doc = builder.build(myFile);
        parseMuleConfig(doc, graph);
    }

    public void parseMuleConfig(Document doc, Graph graph) throws JDOMException, IOException
    {
        Element root = doc.getRootElement();
        String caption = root.getAttribute("id").getValue();
        if (caption != null)
        {
            caption = caption.replaceAll("_", " ");
        }
        else
        {
            caption = "Mule Configuration";
        }

        StringBuffer captionBuffer = new StringBuffer();
        captionBuffer.append(caption);
        TagProcessor.appendDescription(root, captionBuffer);
        graph.getInfo().setCaption(captionBuffer.toString());

        for (Iterator iterator = processors.iterator(); iterator.hasNext();)
        {
            TagProcessor tagProcessor = (TagProcessor) iterator.next();
            tagProcessor.process(graph, root, null);
        }
    }

    public void finalise(Graph graph)
    {
        for (Iterator iter = postProcessors.iterator(); iter.hasNext();)
        {
            PostProcessor postProcessor = (PostProcessor) iter.next();
            postProcessor.postProcess(graph, env);
        }
    }

}
