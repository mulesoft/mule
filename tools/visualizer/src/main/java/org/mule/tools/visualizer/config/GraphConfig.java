/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.tools.visualizer.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;

/**
 * TODO - document
 *
 */
public class GraphConfig
{

    public static final String ARG_FILES = "-files";
    public static final String ARG_EXEC = "-exec";
    public static final String ARG_OUTPUT_DIR = "-outputdir";
    public static final String ARG_OUTPUT_FILE = "-outputfile";
    public static final String ARG_CAPTION = "-caption";
    public static final String ARG_MAPPINGS = "-mappings";
    public static final String ARG_HELP = "-?";
    public static final String ARG_KEEP_DOT_FILES = "-keepdotfiles";
    public static final String ARG_COMBINE_FILES = "-combinefiles";
    public static final String ARG_URLS = "-urls";
    public static final String ARG_CONFIG = "-config";
    public static final String ARG_WORKING_DIRECTORY = "-workingdir";
    public static final String ARG_DEBUG = "-debug";

    public static final String ARG_SHOW_CONNECTORS = "-showconnectors";
    public static final String ARG_SHOW_MODELS = "-showmodels";
    public static final String ARG_SHOW_CONFIG = "-showconfig";
    public static final String ARG_SHOW_AGENTS = "-showagents";
    public static final String ARG_SHOW_TRANSFORMERS = "-showtransformers";
    public static final String ARG_SHOW_ALL = "-showall";
    public static final String ARG_TEMPLATE_PROPS = "-templateprops";

    private List files;
    private String executeCommand;
    private File outputDirectory;
    private String outputFilename;
    private String workingDirectory;
    private String caption;
    private File mappingsFile;
    private File urlsFile;
    private boolean combineFiles = false;
    private boolean keepDotFiles = false;
    private List ignoredAttributes = null;

    private Properties mappings = new Properties();
    private Properties urls = new Properties();
    private Properties propsFromFile = null;
    private Properties templateProps = null;

    private boolean showConnectors = true;
    private boolean showTransformers = false;
    private boolean showModels = false;
    private boolean showConfig = false;
    private boolean showAgents = false;
    private boolean showAll = false;
    private boolean debug = false;

    public GraphConfig()
    {
        files = new ArrayList();
        addIgnorredArributes();
    }

    public GraphEnvironment init()
    {
        return new GraphEnvironment(this);
    }

    public GraphEnvironment init(String[] args) throws IOException
    {
        loadProperties(getOpt(args, ARG_CONFIG, null));
        setWorkingDirectory(getOpt(args, ARG_WORKING_DIRECTORY, null));
        setFilesString(getOpt(args, ARG_FILES, null));
        loadTemplateProps(getOpt(args, ARG_TEMPLATE_PROPS, null));
        setOutputDirectory(getOpt(args, ARG_OUTPUT_DIR, null));
        setOutputFilename(getOpt(args, ARG_OUTPUT_FILE, null));
        setCaption(getOpt(args, ARG_CAPTION, null));
        setExecuteCommand(getOpt(args, ARG_EXEC, null));
        setKeepDotFiles(Boolean.valueOf(getOpt(args, ARG_KEEP_DOT_FILES, "false")).booleanValue());
        setCombineFiles(Boolean.valueOf(getOpt(args, ARG_COMBINE_FILES, "false")).booleanValue());
        setShowAll(Boolean.valueOf(getOpt(args, ARG_SHOW_ALL, String.valueOf(showAll))).booleanValue());
        if (!showAll)
        {
            setShowConnectors(Boolean.valueOf(
                    getOpt(args, ARG_SHOW_CONNECTORS, String.valueOf(showConnectors))).booleanValue());
            setShowConfig(Boolean.valueOf(getOpt(args, ARG_SHOW_CONFIG, String.valueOf(showConfig)))
                    .booleanValue());
            setShowAgents(Boolean.valueOf(getOpt(args, ARG_SHOW_AGENTS, String.valueOf(showAgents)))
                    .booleanValue());
            setShowModels(Boolean.valueOf(getOpt(args, ARG_SHOW_MODELS, String.valueOf(showModels)))
                    .booleanValue());
            setShowTransformers(Boolean.valueOf(getOpt(args, ARG_SHOW_TRANSFORMERS,
                    String.valueOf(showTransformers))).booleanValue());
        }
        setMappingsFile(getOpt(args, ARG_MAPPINGS, null));
        setUrlsFile(getOpt(args, ARG_URLS, null));
        setDebug(Boolean.valueOf(getOpt(args, ARG_DEBUG, String.valueOf(debug))).booleanValue());

        return init();
    }

    protected void addIgnorredArributes()
    {
        ignoredAttributes = new ArrayList();
        ignoredAttributes.add("className");
        ignoredAttributes.add("inboundEndpoint");
        ignoredAttributes.add("outboundEndpoint");
        ignoredAttributes.add("responseEndpoint");
        ignoredAttributes.add("inboundTransformer");
        ignoredAttributes.add("outboundTransformer");
        ignoredAttributes.add("type");
        ignoredAttributes.add("singleton");
        ignoredAttributes.add("containerManaged");
        // ignoredAttributes.add("address");
        // ignoredAttributes.add("transformers");
        ignoredAttributes.add("name");
    }

    /**
     * Add workingDir parameter.
     *
     * @param path - relative to working directorry
     * @return os dependent path
     */
    public String applyWorkingDirectory(String path)
    {
        return this.applyDirectory(workingDirectory, path);
    }

    /**
     * Add outputDir parameter.
     *
     * @param path - relative to output directory
     * @return os dependent path
     */
    public String applyOutputDirectory(String path)
    {
        return this.applyDirectory(this.getOutputDirectory().getPath(), path);
    }

    /**
     * Create os dependent path using directory and relative path.
     *
     * @param dirPath - directory
     * @param path    - relative path
     * @return os dependent path
     */
    protected String applyDirectory(String dirPath, String path)
    {
        if (path == null)
        {
            return null;
        }
        if (dirPath == null)
        {
            return path;
        }
        if (path.startsWith("/") || path.startsWith("\\"))
        {
            return path;
        }
        return dirPath + File.separator + path;
    }

    public void loadProperties(String props) throws IOException
    {
        if (null != props)
        {
            propsFromFile = new Properties();
            propsFromFile.load(new FileInputStream(props));
        }
    }

    public void loadTemplateProps(String props) throws IOException
    {
        templateProps = new Properties();
        if (props != null)
        {
            for (StringTokenizer stringTokenizer = new StringTokenizer(props, ",");
                 stringTokenizer.hasMoreTokens();)
            {
                Properties p = new Properties();
                p.load(new FileInputStream(applyWorkingDirectory(stringTokenizer.nextToken())));

                for (Iterator iterator = p.entrySet().iterator(); iterator.hasNext();)
                {
                    Map.Entry e = (Map.Entry) iterator.next();
                    if (templateProps.getProperty(e.getKey().toString()) != null)
                    {
                        System.err.println("There is a properties conflict with property: " + e.getKey());
                    }
                    else
                    {
                        templateProps.put(e.getKey(), e.getValue());
                    }
                }
            }
        }
    }

    public void validate()
    {
        if (files == null || files.size() == 0)
        {
            throw new IllegalStateException("At least one mule config file must be set");
        }
    }

    private String getOpt(String[] args, String name, String defaultValue)
    {

        if (propsFromFile != null)
        {
            return propsFromFile.getProperty(name.substring(1), defaultValue);
        }

        String rval = defaultValue;
        for (int i = 0; i < args.length; i++)
        {
            if (args[i].equals(name))
            {
                if (i + 1 >= args.length)
                {
                    break;
                }
                else
                {
                    String arg = args[i + 1];
                    if (arg.startsWith("-"))
                    {
                        break;
                    }
                    else
                    {
                        rval = arg;
                        break;
                    }
                }
            }
        }
        if (rval == null || rval.length() == 0)
        {
            rval = null;
        }
        return rval;
    }

    public List getFiles()
    {
        return files;
    }

    public void setFiles(List files)
    {
        this.files = files;
    }

    public void setFilesString(String filesString)
    {
        if (filesString != null)
        {
            files = new ArrayList();
            for (StringTokenizer stringTokenizer = new StringTokenizer(filesString, ","); stringTokenizer
                    .hasMoreTokens();)
            {
                files.add(applyWorkingDirectory(stringTokenizer.nextToken()));
            }
        }

    }

    public String getExecuteCommand()
    {
        return executeCommand;
    }

    public void setExecuteCommand(String executeCommand)
    {
        this.executeCommand = executeCommand;
    }

    public File getOutputDirectory()
    {
        return outputDirectory;
    }

    public void setOutputDirectory(String outputDir)
    {
        if (outputDir == null)
        {
            outputDir = (workingDirectory == null ? "." : workingDirectory);
        }
        outputDirectory = new File(applyWorkingDirectory(outputDir));
        if (!outputDirectory.exists())
        {
            outputDirectory.mkdirs();
        }
    }

    public String getCaption()
    {
        return caption;
    }

    public void setCaption(String caption)
    {
        this.caption = caption;
    }

    public File getMappingsFile()
    {
        return mappingsFile;
    }

    public void setMappingsFile(String temp) throws IOException
    {
        if (temp != null)
        {
            File file = new File(applyWorkingDirectory(temp));
            if (file.exists())
            {
                mappings = new Properties();
                mappings.load(new FileInputStream(file));
                mappings.list(System.out);
                mappingsFile = file;
            }
            else
            {
                throw new FileNotFoundException("Could not find file: " + file.getAbsolutePath());
            }
        }
    }

    public boolean isCombineFiles()
    {
        return combineFiles;
    }

    public void setCombineFiles(boolean combineFiles)
    {
        this.combineFiles = combineFiles;
    }

    public boolean isKeepDotFiles()
    {
        return keepDotFiles;
    }

    public void setKeepDotFiles(boolean keepDotFiles)
    {
        this.keepDotFiles = keepDotFiles;
    }

    public List getIgnoredAttributes()
    {
        return ignoredAttributes;
    }

    public void setIgnoredAttributes(List ignoredAttributes)
    {
        this.ignoredAttributes = ignoredAttributes;
    }

    public Properties getMappings()
    {
        return mappings;
    }

    public void setMappings(Properties mappings)
    {
        this.mappings = mappings;
    }

    public String getOutputFilename()
    {
        return outputFilename;
    }

    public void setOutputFilename(String outputFilename)
    {
        this.outputFilename = outputFilename;
    }

    public Properties getUrls()
    {
        return urls;
    }

    public void setUrls(Properties urls)
    {
        this.urls = urls;
    }

    public File getUrlsFile()
    {
        return urlsFile;
    }

    public void setUrlsFile(String temp) throws IOException
    {
        if (temp != null)
        {
            urlsFile = new File(applyWorkingDirectory(temp));
            if (urlsFile.exists())
            {
                urls = new Properties();
                urls.load(new FileInputStream(urlsFile));
                urls.list(System.out);
            }
        }
    }

    public boolean isShowConnectors()
    {
        return showConnectors;
    }

    public void setShowConnectors(boolean showConnectors)
    {
        this.showConnectors = showConnectors;
    }

    public boolean isShowModels()
    {
        return showModels;
    }

    public void setShowModels(boolean showModels)
    {
        this.showModels = showModels;
    }

    public boolean isShowConfig()
    {
        return showConfig;
    }

    public void setShowConfig(boolean showConfig)
    {
        this.showConfig = showConfig;
    }

    public boolean isShowAgents()
    {
        return showAgents;
    }

    public void setShowAgents(boolean showAgents)
    {
        this.showAgents = showAgents;
    }

    public boolean isShowTransformers()
    {
        return showTransformers;
    }

    public void setShowTransformers(boolean showTransformers)
    {
        this.showTransformers = showTransformers;
    }

    public boolean isShowAll()
    {
        return showAll;
    }

    public void setShowAll(boolean showAll)
    {
        this.showAll = showAll;
        showConfig = true;
        showConnectors = true;
        showAgents = true;
        showModels = true;
        showTransformers = true;
    }

    public void setWorkingDirectory(String temp)
    {
        if (temp != null)
        {
            File f = new File(temp);
            if (!f.exists())
            {
                f.mkdirs();
            }
            workingDirectory = f.getAbsolutePath();
        }
    }

    public String getWorkingDirectory()
    {
        return workingDirectory;
    }

    public boolean isDebug()
    {
        return this.debug;
    }

    public void setDebug(boolean debug)
    {
        this.debug = debug;
    }
}
