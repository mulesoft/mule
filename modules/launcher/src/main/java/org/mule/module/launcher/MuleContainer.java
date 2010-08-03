/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.launcher;

import org.mule.api.DefaultMuleException;
import org.mule.api.MuleException;
import org.mule.config.ExceptionHelper;
import org.mule.config.StartupContext;
import org.mule.config.i18n.CoreMessages;
import org.mule.config.i18n.Message;
import org.mule.util.MuleUrlStreamHandlerFactory;
import org.mule.util.StringMessageUtils;
import org.mule.util.SystemUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 *
 */
public class MuleContainer
{
    public static final String CLI_OPTIONS[][] = {
        {"builder", "true", "Configuration Builder Type"},
        {"config", "true", "Configuration File"},
        {"idle", "false", "Whether to run in idle (unconfigured) mode"},
        {"main", "true", "Main Class"},
        {"mode", "true", "Run Mode"},
        {"props", "true", "Startup Properties"},
        {"production", "false", "Production Mode"},
        {"debug", "false", "Configure Mule for JPDA remote debugging."},
        {"app", "true", "Application to start"}
    };

    /**
     * Don't use a class object so the core doesn't depend on mule-module-spring-config.
     */
    protected static final String CLASSNAME_DEFAULT_CONFIG_BUILDER = "org.mule.config.builders.AutoConfigurationBuilder";

    /**
     * This builder sets up the configuration for an idle Mule node - a node that
     * doesn't do anything initially but is fed configuration during runtime
     */
    protected static final String CLASSNAME_DEFAULT_IDLE_CONFIG_BUILDER = "org.mule.config.builders.MuleIdleConfigurationBuilder";

    /**
     * If the annotations module is on the classpath, also enable annotations config builder
     */
    public static final String CLASSNAME_ANNOTATIONS_CONFIG_BUILDER = "org.mule.config.AnnotationsConfigurationBuilder";

    /**
     * logger used by this class
     */
    private static final Log logger = LogFactory.getLog(MuleContainer.class);

    public static final String DEFAULT_CONFIGURATION = "mule-config.xml";

    /**
     * one or more configuration urls or filenames separated by commas
     */
    private String configurationResources = null;

    /**
     * A FQN of the #configBuilder class, required in case MuleContainer is
     * reinitialised.
     */
    private static String configBuilderClassName = null;

    /**
     * A properties file to be read at startup. This can be useful for setting
     * properties which depend on the run-time environment (dev, test, production).
     */
    private static String startupPropertiesFile = null;

    /**
     * The Runtime shutdown thread used to undeploy this server
     */
    private static MuleShutdownHook muleShutdownHook;

    protected DefaultMuleDeployer deployer;

    /**
     * Application entry point.
     *
     * @param args command-line args
     */
    public static void main(String[] args) throws Exception
    {
        MuleContainer container = new MuleContainer(args);
        container.start(false, true);
    }

    public MuleContainer()
    {
        init(new String[0]);
    }

    public MuleContainer(String configResources)
    {
        init(new String[] {"-config", configResources});
    }

    /**
     * Configure the server with command-line arguments.
     */
    public MuleContainer(String[] args) throws IllegalArgumentException
    {
        init(args);
    }

    protected void init(String[] args) throws IllegalArgumentException
    {
        Map<String, Object> commandlineOptions;

        try
        {
            commandlineOptions = SystemUtils.getCommandLineOptions(args, CLI_OPTIONS);
        }
        catch (DefaultMuleException me)
        {
            throw new IllegalArgumentException(me.toString());
        }

        // set our own UrlStreamHandlerFactory to become more independent of system
        // properties
        MuleUrlStreamHandlerFactory.installUrlStreamHandlerFactory();

        // Startup properties
        String propertiesFile = (String) commandlineOptions.get("props");
        if (propertiesFile != null)
        {
            setStartupPropertiesFile(propertiesFile);
        }
        StartupContext.get().setStartupOptions(commandlineOptions);
    }

    /**
     * Start the mule server
     *
     * @param ownThread determines if the server will run in its own daemon thread or
     *                  the current calling thread
     */
    public void start(boolean ownThread, boolean registerShutdownHook)
    {
        if (registerShutdownHook)
        {
            registerShutdownHook();
        }

        final MuleContainerStartupSplashScreen splashScreen = new MuleContainerStartupSplashScreen();
        splashScreen.doBody();
        logger.info(splashScreen.toString());

        try
        {
            // TODO pluggable deployer
            deployer = new DefaultMuleDeployer();
            deployer.deploy();
        }
        catch (Throwable e)
        {
            shutdown(e);
        }
    }

    /**
     * Will shut down the server displaying the cause and time of the shutdown
     *
     * @param e the exception that caused the shutdown
     */
    public void shutdown(Throwable e)
    {
        Message msg = CoreMessages.fatalErrorWhileRunning();
        MuleException muleException = ExceptionHelper.getRootMuleException(e);
        if (muleException != null)
        {
            logger.fatal(muleException.getDetailedMessage());
        }
        else
        {
            logger.fatal(msg.toString() + " " + e.getMessage(), e);
        }
        List<String> msgs = new ArrayList<String>();
        msgs.add(msg.getMessage());
        Throwable root = ExceptionHelper.getRootException(e);
        msgs.add(root.getMessage() + " (" + root.getClass().getName() + ")");
        msgs.add(" ");
        msgs.add(CoreMessages.fatalErrorInShutdown().getMessage());
        String shutdownMessage = StringMessageUtils.getBoilerPlate(msgs, '*', 80);
        logger.fatal(shutdownMessage);

        unregisterShutdownHook();
        doShutdown();
    }

    /**
     * shutdown the server. This just displays the time the server shut down
     */
    public void shutdown()
    {
        logger.info("Mule container shutting down due to normal shutdown request");

        unregisterShutdownHook();
        doShutdown();
    }

    protected void doShutdown()
    {
        if (deployer != null)
        {
            deployer.undeploy();
        }
        System.exit(0);
    }

    public Log getLogger()
    {
        return logger;
    }

    public void registerShutdownHook()
    {
        if (muleShutdownHook == null)
        {
            muleShutdownHook = new MuleShutdownHook();
        }
        else
        {
            Runtime.getRuntime().removeShutdownHook(muleShutdownHook);
        }
        Runtime.getRuntime().addShutdownHook(muleShutdownHook);
    }

    public void unregisterShutdownHook()
    {
        if (muleShutdownHook != null)
        {
            Runtime.getRuntime().removeShutdownHook(muleShutdownHook);
        }
    }

    // /////////////////////////////////////////////////////////////////
    // Getters and setters
    // /////////////////////////////////////////////////////////////////

    /**
     * Getter for property messengerURL.
     *
     * @return Value of property messengerURL.
     */
    public String getConfigurationResources()
    {
        return configurationResources;
    }


    public static String getStartupPropertiesFile()
    {
        return startupPropertiesFile;
    }

    public static void setStartupPropertiesFile(String startupPropertiesFile)
    {
        MuleContainer.startupPropertiesFile = startupPropertiesFile;
    }

    /**
     * This class is installed only for MuleContainer running as commandline app. A
     * clean Mule shutdown can be achieved by disposing the
     * {@link org.mule.DefaultMuleContext}.
     */
    private class MuleShutdownHook extends Thread
    {
        public MuleShutdownHook()
        {
            super();
        }

        @Override
        public void run()
        {
            doShutdown();
        }
    }
}

