/*
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.tools.config.graph.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;

/**
 * Todo - document
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class GraphConfig {


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
    
    private List files;
    private String executeCommand;
    private File outputDirectory;
    private String outputFilename;
    private String caption;
    private File mappingsFile;
    private File urlsFile;
    private boolean combineFiles = false;
    private boolean keepDotFiles = false;
    private List ignoredAttributes = null;

    private Properties mappings = new Properties();
    private Properties urls = new Properties();

    public GraphConfig() {
        init();
    }

    public GraphConfig(String[] args) throws IOException {
        init();
        String filesString = getOpt(args, ARG_FILES, null);
        if (filesString != null) {
            files = new ArrayList();
            for (StringTokenizer stringTokenizer = new StringTokenizer(filesString, ","); stringTokenizer.hasMoreTokens();) {
                files.add(stringTokenizer.nextToken());
            }
        }
        String outputDir = getOpt(args, ARG_OUTPUT_DIR, ".");
        outputDirectory = new File(outputDir);
        if (!outputDirectory.exists()) outputDirectory.mkdirs();
        System.out.println("Outputting graphs to: " + outputDirectory.getAbsolutePath());

        outputFilename = getOpt(args, ARG_OUTPUT_FILE, null);

        caption = getOpt(args, ARG_CAPTION, null);

        executeCommand = getOpt(args, ARG_EXEC, null);

        keepDotFiles = Boolean.valueOf(getOpt(args, ARG_KEEP_DOT_FILES, "false")).booleanValue();

        combineFiles = Boolean.valueOf(getOpt(args, ARG_COMBINE_FILES, "false")).booleanValue();

        String temp = getOpt(args, ARG_MAPPINGS, null);
        if (temp != null) {
            mappingsFile = new File(temp);
            System.out.println("Using mappings file: " + mappingsFile.getAbsolutePath());
            if (mappingsFile.exists()) {
                mappings = new Properties();
                mappings.load(new FileInputStream(mappingsFile));
                System.out.println("Using Mappings: ");
                mappings.list(System.out);
            } else {
                throw new FileNotFoundException("Could not find file: " + mappingsFile.getAbsolutePath());
            }
        } else {
            System.out.println("No mappings file set");
        }

        temp = getOpt(args, ARG_URLS, null);
        if (temp != null) {
            urlsFile = new File(temp);
            System.out.println("Using urls file: " + urlsFile.getAbsolutePath());
            if (urlsFile.exists()) {
                urls = new Properties();
                urls.load(new FileInputStream(urlsFile));
                System.out.println("Using urls: ");
                urls.list(System.out);
            } 
        } else {
            System.out.println("No urls file set");
        }
        
    }

    protected void init() {
        files = new ArrayList();
        ignoredAttributes = new ArrayList();
        ignoredAttributes.add("className");
        ignoredAttributes.add("inboundEndpoint");
        ignoredAttributes.add("outboundEndpoint");
        ignoredAttributes.add("responseEndpoint");
        ignoredAttributes.add("inboundTransformer");
        ignoredAttributes.add("outboundTransformer");
        ignoredAttributes.add("type");
        ignoredAttributes.add("singleton");
        ignoredAttributes.add("address");
        ignoredAttributes.add("transformers");
        ignoredAttributes.add("name");
    }

    public void validate() throws IllegalStateException {
        if (files == null || files.size() == 0) {
            throw new IllegalStateException("At least one config file must be set");
        }
    }

    private String getOpt(String[] args, String name, String defaultValue) {
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals(name)) {
                if (i + 1 >= args.length) {
                    return defaultValue;
                } else {
                    String arg = args[i + 1];
                    if (arg.startsWith("-")) {
                        return defaultValue;
                    } else {
                        return arg;
                    }
                }
            }
        }
        return defaultValue;
    }

    public List getFiles() {
        return files;
    }

    public void setFiles(List files) {
        this.files = files;
    }

    public String getExecuteCommand() {
        return executeCommand;
    }

    public void setExecuteCommand(String executeCommand) {
        this.executeCommand = executeCommand;
    }

    public File getOutputDirectory() {
        return outputDirectory;
    }

    public void setOutputDirectory(File outputDirectory) {
        this.outputDirectory = outputDirectory;
    }

    public String getCaption() {
        return caption;
    }

    public void setCaption(String caption) {
        this.caption = caption;
    }

    public File getMappingsFile() {
        return mappingsFile;
    }

    public void setMappingsFile(File mappingsFile) {
        this.mappingsFile = mappingsFile;
    }

    public boolean isCombineFiles() {
        return combineFiles;
    }

    public void setCombineFiles(boolean combineFiles) {
        this.combineFiles = combineFiles;
    }

    public boolean isKeepDotFiles() {
        return keepDotFiles;
    }

    public void setKeepDotFiles(boolean keepDotFiles) {
        this.keepDotFiles = keepDotFiles;
    }

    public List getIgnoredAttributes() {
        return ignoredAttributes;
    }

    public void setIgnoredAttributes(List ignoredAttributes) {
        this.ignoredAttributes = ignoredAttributes;
    }

    public Properties getMappings() {
        return mappings;
    }

    public void setMappings(Properties mappings) {
        this.mappings = mappings;
    }

    public String getOutputFilename() {
        return outputFilename;
    }

    public void setOutputFilename(String outputFilename) {
        this.outputFilename = outputFilename;
    }

	public Properties getUrls() {
		return urls;
	}

	public void setUrls(Properties urls) {
		this.urls = urls;
	}

	public File getUrlsFile() {
		return urlsFile;
	}

	public void setUrlsFile(File urlsFile) {
		this.urlsFile = urlsFile;
	}

}
