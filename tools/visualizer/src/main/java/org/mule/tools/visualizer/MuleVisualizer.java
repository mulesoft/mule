/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.tools.visualizer;

import org.mule.config.MuleDtdResolver;
import org.mule.tools.visualizer.components.EndpointRegistry;
import org.mule.tools.visualizer.components.GraphRenderer;
import org.mule.tools.visualizer.components.MuleParser;
import org.mule.tools.visualizer.components.PostGrapher;
import org.mule.tools.visualizer.config.GraphConfig;
import org.mule.tools.visualizer.config.GraphEnvironment;
import org.mule.tools.visualizer.postgraphers.DocIndexerPostGrapher;
import org.mule.tools.visualizer.postgraphers.GalleryPostGrapher;
import org.mule.tools.visualizer.postgraphers.MediaCopierPostGrapher;

import com.oy.shared.lm.graph.Graph;
import com.oy.shared.lm.graph.GraphFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.jdom.Document;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;

public class MuleVisualizer
{

    private GraphEnvironment env;

    private final GraphRenderer graphRenderer;

    private final List postGraphers = new ArrayList();

    public static void main(String[] args)
    {
        if (args.length == 0 || args[0].equals(GraphConfig.ARG_HELP))
        {
            printUsage();
            System.exit(0);
        }

        MuleVisualizer visualizer = null;
        GraphEnvironment env = null;

        try
        {
            env = new GraphConfig().init(args);

            visualizer = new MuleVisualizer(env);
        }
        catch (Exception e)
        {
            if (null != env)
            {
                env.logError("MuleGrapher failed to process: " + e.getMessage(), e);
            }
            System.exit(0);
        }

        try
        {
            visualizer.visualize(env.getConfig().getFiles());
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    public MuleVisualizer(GraphEnvironment environment) throws Exception
    {
        env = environment;
        this.graphRenderer = new GraphRenderer(env);
        this.postGraphers.add(new DocIndexerPostGrapher(env));
        this.postGraphers.add(new GalleryPostGrapher(env));
        this.postGraphers.add(new MediaCopierPostGrapher());
    }

    public List visualize(List files) throws Exception
    {
        List results;
        env.getConfig().setFiles(files);
        env.getConfig().validate();
        if (env.getConfig().isCombineFiles())
        {
            generateIndividual();
            results = generateCombined();
        }
        else
        {
            results = generateIndividual();
        }

        for (Iterator iter = postGraphers.iterator(); iter.hasNext();)
        {
            PostGrapher postGrapher = (PostGrapher) iter.next();
            env.log("************ " + postGrapher.getStatusTitle());
            postGrapher.postGrapher(env);
        }

        return results;
    }

    protected List generateCombined() throws IOException, JDOMException
    {
        env.setDoingCombinedGeneration(true);
        env.setEndpointRegistry(new EndpointRegistry(env));
//        env.log("Doing Combined Generation with file name: " + filename);
//        if (filename == null)
//        {
//            filename = env.getConfig().getFiles().get(0).toString() + ".combined";
//        }
        return generateGraph(1, env.getConfig().getFiles(), env.getConfig().getOutputDirectory(), env.getConfig()
                .getCaption());
    }

    protected List generateIndividual() throws IOException, JDOMException
    {
        env.setDoingCombinedGeneration(false);

        int ind = 0;
        List resultFiles = new ArrayList(env.getConfig().getFiles().size());
        for (Iterator iterator = env.getConfig().getFiles().iterator(); iterator.hasNext();)
        {
            env.setEndpointRegistry(new EndpointRegistry(env));
            ind++;
            Object o = iterator.next();
            List list = new ArrayList(1);
            list.add(o);
            env.log("Doing inividual generation for file: " + o);
            resultFiles.add(generateGraph(ind, list, env.getConfig().getOutputDirectory(), env.getConfig().getCaption()));
        }
        return resultFiles;
    }

    protected List generateGraph(int i, List files, File outputDir, String caption)
            throws JDOMException, IOException
    {
        List results = new ArrayList();
        String fileName = env.getConfig().getOutputFilename();
        SAXBuilder builder = new SAXBuilder();
        builder.setValidation(true);
        builder.setEntityResolver(new MuleDtdResolver());
        Graph graph = GraphFactory.newGraph();

        builder.setIgnoringElementContentWhitespace(true);
        MuleParser muleParser = new MuleParser(env, builder);
        for (Iterator iterator = files.iterator(); iterator.hasNext();)
        {

            Object o = iterator.next();
            File myFile = null;
            if (o instanceof String)
            {
                myFile = new File(o.toString());

            }
            else if (o instanceof File)
            {
                myFile = (File) o;

            }

            if (myFile != null)
            {
                env.log("**************** processing " + i + " of " + files.size() + 1 + " : "
                        + myFile.getCanonicalPath());

                if (fileName == null)
                {
                    fileName = myFile.getName();
                }
                muleParser.parseMuleConfig(myFile, graph);
            }
            else if (o instanceof InputStream)
            {
                muleParser.parseMuleConfig((InputStream) o, graph);
            }
            else if (o instanceof Document)
            {
                muleParser.parseMuleConfig((Document) o, graph);
            }
            else
            {
                throw new IllegalArgumentException("Object cannot be processed, unrecognised format: " + o.getClass());
            }

            if (fileName == null)
            {
                fileName = "mule-visualised";
            }

            if (files.size() > 1)
            {
                if (caption == null)
                {
                    caption = "(no caption set)";
                }
                graph.getInfo().setCaption(caption);
            }
            if (!env.getConfig().isCombineFiles())
            {
                muleParser.finalise(graph);
                results.add(graphRenderer.saveGraph(graph, fileName, outputDir));
            }
        }
        if (env.getConfig().isCombineFiles())
        {
            muleParser.finalise(graph);
            results = new ArrayList(1);
            results.add(graphRenderer.saveGraph(graph, fileName, outputDir));
        }
        return results;
    }

    public static void printUsage()
    {
        System.out.println("Mule Configuration Grapher");
        System.out.println("Generates  graphs for Mule configuration files");
        System.out.println("-----------------------------------------------");
        System.out.println("-files      A comma-seperated list of Mule configuration files (required)");
        System.out
                .println("-outputdir  The directory to write the generated graphs to. Defaults to the current directory (optional)");
        System.out
                .println("-exec       The executable file used for Graph generation. Defaults to ./win32/dot.exe (optional)");
        System.out
                .println("-caption    Default caption for the generated graphs. Defaults to the 'id' attribute in the config file (optional)");
        System.out.println("-?          Displays this help");
    }

}
