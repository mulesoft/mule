/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.agent;

import org.mule.AbstractAgent;
import org.mule.MuleServer;
import org.mule.api.MuleException;
import org.mule.api.config.ConfigurationBuilder;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.util.ClassUtils;
import org.mule.util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * EXPERIMENTAL!!!
 *
 * This agent scans a defined directory for dropped configuration files. It
 * defaults to .mule/conf.
 *
 * Once configuration files are ready, they should be moved instead of deleted.
 * We need a strategy for this.
 *
 * This agent should also respond to wire transfers (tcp, multicast).
 */
public class ConfigScannerAgent extends AbstractAgent
{
    public static final String AGENT_NAME = "Mule Config Scanner";

    /**
     * logger used by this class
     */
    private static final Log logger = LogFactory.getLog(ConfigScannerAgent.class);

    private String configDirName = null;

    private File configDir = null;

    private int sleepInterval = 5000;

    private boolean doStop = false;

    private ScannerThread scannerThread = null;

    public ConfigScannerAgent()
    {
        super(AGENT_NAME);
    }

    public String getConfigDirName()
    {
        return configDirName;
    }

    public void setConfigDirName(String configDirName)
    {
        this.configDirName = configDirName;
    }

    /**
     * Should be a 1 line description of the agent
     * 
     * @return
     */
    public String getDescription()
    {
        return getName() + " scanning for files in " + configDirName;
    }

    public void start() throws MuleException
    {
        scannerThread = new ScannerThread();
        scannerThread.start();
    }

    public void stop() throws MuleException
    {
        // do nothing
    }

    public void dispose()
    {
        // do nothing
    }

    public void registered()
    {
        // do nothing
    }

    public void unregistered()
    {
        // do nothing
    }

    public void initialise() throws InitialisationException
    {
        if (configDirName == null)
        {
            String workDir = muleContext.getConfiguration().getWorkingDirectory();
            configDirName = workDir + "/conf";
        }

        try 
        {
            configDir = FileUtils.openDirectory(configDirName);
        }
        catch (IOException ioe)
        {
            throw new InitialisationException(ioe, this);
        }
    }

    public List getDependentAgents()
    {
        return Collections.EMPTY_LIST;
    }

    public String toString()
    {
        return getDescription();
    }

    public void setDoStop(boolean doStop) 
    {
        this.doStop = doStop;
    }

    class ScannerThread extends Thread
    {
        int errorCount = 0;
        int errorThreshold = 3;
        List processedFiles = new ArrayList();

        public void run()
        {
            while (true)
            {
                if (doStop || errorCount >= errorThreshold)
                {
                    break;
                }

                try
                {
                    File[] configFiles = configDir.listFiles();
                    for (int i = 0; i < configFiles.length; i++)
                    {
                        File configFile = configFiles[i];
                        String path = configFile.getCanonicalPath();

                        if (processedFiles.contains(path))
                        {
                            // TODO: probably shouldn't delete here
                            configFile.delete();
                            if (configFile.exists())
                            {
                                processedFiles.remove(processedFiles.indexOf(path));
                            }
                        }
                        else
                        {
                            logger.info(path);
                            processConfigFile(path);
                            // TODO: probably shouldn't delete here
                            configFile.delete();
                            processedFiles.add(path);
                        }
                    }
                } 
                catch (IOException ioe)
                {
                    logger.error("Unable to check directory: " + ioe.toString());
                    errorCount++;
                }

                try
                {
                    sleep(sleepInterval);
                }
                catch (InterruptedException e)
                {
                }
            }
        }
    }

    private void processConfigFile(String configFile)
    {
        try
        {
            Class cfgBuilderClass = ClassUtils.loadClass(
                "org.mule.config.spring.SpringXmlConfigurationBuilder", MuleServer.class);
            ConfigurationBuilder cfgBuilder = (ConfigurationBuilder) ClassUtils.instanciateClass(
                cfgBuilderClass, configFile);

            if (!cfgBuilder.isConfigured())
            {
                // TODO Update after MULE-1988
                cfgBuilder.configure(muleContext);
            }
        }
        catch (Exception e)
        {
            logger.error(e);
        }
    }

}

