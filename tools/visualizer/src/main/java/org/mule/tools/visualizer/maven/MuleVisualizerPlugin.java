/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.tools.visualizer.maven;

import org.mule.tools.visualizer.MuleVisualizer;
import org.mule.tools.visualizer.config.GraphConfig;
import org.mule.tools.visualizer.config.GraphEnvironment;
import org.mule.util.FileUtils;

import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

/** @goal graph */
public class MuleVisualizerPlugin extends AbstractMojo
{

    /** @parameter */
    private List files = new LinkedList();

    /** @parameter * */
    private String exec;

    /** @parameter * */
    private String outputdir;

    /** @parameter * */
    private String outputfile;

    /** @parameter * */
    private String caption;

    /** @parameter * */
    private String mappings;

    /** @parameter * */
    private boolean keepdotfiles;

    /** @parameter * */
    private boolean combinefiles;

    /** @parameter * */
    private String urls;

    /** @parameter * */
    private String config;

    /** @parameter * */
    private String workingdir;

    /** @parameter * */
    private boolean showconnectors;

    /** @parameter * */
    private boolean showmodels;

    /** @parameter * */
    private boolean showconfig;

    /** @parameter * */
    private boolean showagents;

    /** @parameter * */
    private boolean showtransformers;

    /** @parameter * */
    private boolean showall;

    /** @parameter * */
    private String templateprops;

    public MuleVisualizerPlugin()
    {
        try
        {
            setWorkingdir(FileUtils.getResourcePath("target", getClass()));
        }
        catch (Exception e)
        {
            setWorkingdir(null);
        }
    }

    public void execute() throws MojoExecutionException, MojoFailureException
    {
        dumpParameters();

        try
        {
            GraphConfig config = buildConfig();
            GraphEnvironment environment = new GraphEnvironment(config);
            MuleVisualizer visualizer = new MuleVisualizer(environment);
            visualizer.visualize(files);
        }
        catch (Exception e)
        {
            throw new MojoExecutionException("Failed to run visualizer: " + e.getMessage(), e);
        }
    }

    private void dumpParameters()
    {
        Iterator file = getFiles().iterator();
        while (file.hasNext())
        {
            getLog().info("file: " + file.next().toString());
        }
        getLog().info("workingdir: " + getWorkingdir());
        getLog().info("outputdir: " + getOutputdir());
    }

    protected GraphConfig buildConfig() throws IOException
    {
        // order here as in GraphConfig.init(String[]) in case of depepndencies
        GraphConfig config = new GraphConfig();
        config.loadProperties(getConfig());
        config.setWorkingDirectory(getWorkingdir());
        config.setFiles(getFiles());
        config.loadTemplateProps(getTemplateprops());
        config.setOutputDirectory(getOutputdir());
        config.setOutputFilename(getOutputfile());
        config.setCaption(getCaption());
        config.setExecuteCommand(getExec());
        config.setKeepDotFiles(isKeepdotfiles());
        config.setCombineFiles(isCombinefiles());
        config.setShowAll(isShowall());
        if (!config.isShowAll())
        {
            config.setShowAgents(isShowagents());
            config.setShowConfig(isShowconfig());
            config.setShowConnectors(isShowconnectors());
            config.setShowModels(isShowmodels());
            config.setShowTransformers(isShowtransformers());
        }
        config.setMappingsFile(getMappings());
        config.setUrlsFile(getUrls());
        return config;
    }

    public void setFiles(List files)
    {
        this.files = files;
    }

    public List getFiles()
    {
        return files;
    }

    public void setOutputdir(String outputdir)
    {
        this.outputdir = outputdir;
    }

    public String getOutputdir()
    {
        return outputdir;
    }

    public void setExec(String exec)
    {
        this.exec = exec;
    }

    public String getExec()
    {
        return exec;
    }

    public void setOutputfile(String outputfile)
    {
        this.outputfile = outputfile;
    }

    public String getOutputfile()
    {
        return outputfile;
    }

    public void setCaption(String caption)
    {
        this.caption = caption;
    }

    public String getCaption()
    {
        return caption;
    }

    public void setMappings(String mappings)
    {
        this.mappings = mappings;
    }

    public String getMappings()
    {
        return mappings;
    }

    public void setKeepdotfiles(boolean keepdotfiles)
    {
        this.keepdotfiles = keepdotfiles;
    }

    public boolean isKeepdotfiles()
    {
        return keepdotfiles;
    }

    public void setCombinefiles(boolean combinefiles)
    {
        this.combinefiles = combinefiles;
    }

    public boolean isCombinefiles()
    {
        return combinefiles;
    }

    public void setUrls(String urls)
    {
        this.urls = urls;
    }

    public String getUrls()
    {
        return urls;
    }

    public void setConfig(String config)
    {
        this.config = config;
    }

    public String getConfig()
    {
        return config;
    }

    public void setWorkingdir(String workingdir)
    {
        this.workingdir = workingdir;
    }

    public String getWorkingdir()
    {
        return workingdir;
    }

    public void setShowconnectors(boolean showconnectors)
    {
        this.showconnectors = showconnectors;
    }

    public boolean isShowconnectors()
    {
        return showconnectors;
    }

    public void setShowmodels(boolean showmodels)
    {
        this.showmodels = showmodels;
    }

    public boolean isShowmodels()
    {
        return showmodels;
    }

    public void setShowconfig(boolean showconfig)
    {
        this.showconfig = showconfig;
    }

    public boolean isShowconfig()
    {
        return showconfig;
    }

    public void setShowagents(boolean showagents)
    {
        this.showagents = showagents;
    }

    public boolean isShowagents()
    {
        return showagents;
    }

    public void setShowtransformers(boolean showtransformers)
    {
        this.showtransformers = showtransformers;
    }

    public boolean isShowtransformers()
    {
        return showtransformers;
    }

    public void setShowall(boolean showall)
    {
        this.showall = showall;
    }

    public boolean isShowall()
    {
        return showall;
    }

    public void setTemplateprops(String templateprops)
    {
        this.templateprops = templateprops;
    }

    public String getTemplateprops()
    {
        return templateprops;
    }
}
