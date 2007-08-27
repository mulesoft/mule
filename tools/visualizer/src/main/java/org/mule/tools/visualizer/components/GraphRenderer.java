/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.tools.visualizer.components;

import org.mule.tools.visualizer.config.GraphEnvironment;
import org.mule.tools.visualizer.postrenderers.FileCleanerPostRenderer;
import org.mule.tools.visualizer.postrenderers.MuleDocPostRenderer;
import org.mule.tools.visualizer.util.DOTtoMAP;
import org.mule.util.StringUtils;
import org.mule.util.SystemUtils;

import com.oy.shared.lm.graph.Graph;
import com.oy.shared.lm.out.GRAPHtoDOTtoGIF;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;

public class GraphRenderer
{

    public static final String MULE_GRAPHVIZ = "MULE_GRAPHVIZ";
    public static final String MULE_HOME = "MULE_HOME";

    private GraphEnvironment env;
    private List postRenderers = new ArrayList();

    public GraphRenderer(GraphEnvironment env) throws Exception
    {
        this.env = env;
        postRenderers.add(new MuleDocPostRenderer(env));
        postRenderers.add(new FileCleanerPostRenderer());

    }

    public String saveGraph(Graph graph, String filename, File outFolder) throws IOException
    {
        // output graph to *.gif
        final String dotFileName = new File(outFolder, filename + ".dot").getAbsolutePath();
        final String mapFileName = new File(outFolder, filename + ".cmapx").getAbsolutePath();
        final String gifFileName = new File(outFolder, filename + ".gif").getAbsolutePath();

        final String exeFile = getSaveExecutable();
        env.log("Executing: " + exeFile);
        GRAPHtoDOTtoGIF.transform(graph, dotFileName, gifFileName, exeFile);
        env.log("generating MAP");
        DOTtoMAP.transform(exeFile, dotFileName, mapFileName, env);

        Map context = new HashMap();
        String map = FileUtils.readFileToString(new File(mapFileName), "UTF-8");
        String path = env.getConfig().getOutputDirectory().getAbsolutePath() + File.separator;
        context.put("dotFileName", path + filename + ".dot");
        context.put("mapFileName", path + filename + ".cmapx");
        context.put("mapFile", map);
        context.put("gifFileName", filename + ".gif");
        context.put("htmlFileName", path + filename + ".html");
        context.put("outFolder", outFolder.getAbsolutePath());

        for (Iterator iter = postRenderers.iterator(); iter.hasNext();)
        {
            PostRenderer element = (PostRenderer) iter.next();
            element.postRender(env, context, graph);
        }
        return gifFileName;
    }

    private String getSaveExecutable() throws FileNotFoundException
    {
        if (env.getConfig().getExecuteCommand() == null)
        {
            String dot = SystemUtils.getenv(MULE_GRAPHVIZ);
            if (StringUtils.isNotBlank(dot))
            {
                env.getConfig().setExecuteCommand(new File(dot).getAbsolutePath());
            }
            else
            {
                String osName = System.getProperty("os.name").toLowerCase();
                if (osName.startsWith("windows"))
                {
                    setWindowsExecutable();
                }
                else // try anything else in a unix-like way
                {
                    setUnixExecutable();
                }
            }
        }

        File f = new File(env.getConfig().getExecuteCommand());
        if (!f.exists())
        {
            throw new FileNotFoundException(f.getAbsolutePath());
        }

        return env.getConfig().getExecuteCommand();
    }

    private void setUnixExecutable() throws FileNotFoundException
    {
        try 
        {
            Process process = Runtime.getRuntime().exec("which dot");
            File f = new File(new BufferedReader(new InputStreamReader(process.getInputStream())).readLine());
            env.getConfig().setExecuteCommand(f.getAbsolutePath());
        }
        catch (Exception e)
        {
            throw (FileNotFoundException) new FileNotFoundException(
                "Could not find the executable 'dot': " + e.getMessage()).initCause(e);
        }
    }

    private void setWindowsExecutable()
    {
        File f = new File("win32/dot.exe");
        if (!f.exists())
        {
            String home = SystemUtils.getenv(MULE_HOME);
            if (StringUtils.isNotBlank(home))
            {
                f = new File(home + "/tools/config-graph/win32/dot.exe");
            }
        }   
        if (f.exists())
        {
            env.getConfig().setExecuteCommand(f.getAbsolutePath());
        }
    }

}
