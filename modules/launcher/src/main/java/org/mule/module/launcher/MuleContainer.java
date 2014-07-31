/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.launcher;

import org.mule.api.DefaultMuleException;
import org.mule.api.MuleException;
import org.mule.api.config.MuleProperties;
import org.mule.config.ExceptionHelper;
import org.mule.config.StartupContext;
import org.mule.config.i18n.CoreMessages;
import org.mule.config.i18n.Message;
import org.mule.module.launcher.coreextension.DefaultMuleCoreExtensionManager;
import org.mule.module.launcher.coreextension.MuleCoreExtensionManager;
import org.mule.module.launcher.log4j2.MuleLog4jContextFactory;
import org.mule.util.MuleUrlStreamHandlerFactory;
import org.mule.util.StringMessageUtils;
import org.mule.util.SystemUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.logging.log4j.LogManager;

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
     * logger used by this class
     */
    private static final Log logger;

    /**
     * A properties file to be read at startup. This can be useful for setting
     * properties which depend on the run-time environment (dev, test, production).
     */
    private static String startupPropertiesFile = null;

    /**
     * The Runtime shutdown thread used to undeploy this server
     */
    private static MuleShutdownHook muleShutdownHook;

    protected final DeploymentService deploymentService;
    private final MuleCoreExtensionManager coreExtensionManager;
    private final PluginClassLoaderManager pluginClassLoaderManager = new MulePluginClassLoaderManager();

    static
    {
        if (System.getProperty(MuleProperties.MULE_SIMPLE_LOG) == null)
        {
            LogManager.setFactory(new MuleLog4jContextFactory());
        }

        logger = LogFactory.getLog(MuleContainer.class);
    }
    /**
     * Application entry point.
     *
     * @param args command-line args
     */
    public static void main(String[] args) throws Exception
    {
        MuleContainer container = new MuleContainer(args);
        container.start(true);
    }

    public MuleContainer(String[] args)
    {
        this.deploymentService = new MuleDeploymentService(pluginClassLoaderManager);
        this.coreExtensionManager = new DefaultMuleCoreExtensionManager();

        init(args);
    }

    public MuleContainer(DeploymentService deploymentService, MuleCoreExtensionManager coreExtensionManager)
    {
        this(new String[0], deploymentService, coreExtensionManager);
    }

    /**
     * Configure the server with command-line arguments.
     */
    public MuleContainer(String[] args, DeploymentService deploymentService, MuleCoreExtensionManager coreExtensionManager) throws IllegalArgumentException
    {
        //TODO(pablo.kraan): remove the args argument and use the already existing setters to set everything needed
        this.deploymentService = deploymentService;
        this.coreExtensionManager = coreExtensionManager;

        init(args);
    }

    protected void init(String[] args) throws IllegalArgumentException
    {
        //TODO(pablo.kraan): move initialization of others classes outside this method
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

    public void start(boolean registerShutdownHook) throws MuleException
    {
        if (registerShutdownHook)
        {
            registerShutdownHook();
        }
        showSplashScreen();
        try
        {
            coreExtensionManager.setDeploymentService(deploymentService);
            coreExtensionManager.setPluginClassLoaderManager(pluginClassLoaderManager);
            coreExtensionManager.initialise();
            coreExtensionManager.start();

            deploymentService.start();
        }
        catch (Throwable e)
        {
            shutdown(e);
        }
    }

    protected void showSplashScreen()
    {
        final MuleContainerStartupSplashScreen splashScreen = new MuleContainerStartupSplashScreen();
        splashScreen.doBody();
        logger.info(splashScreen.toString());
    }

    /**
     * Will shut down the server displaying the cause and time of the shutdown
     *
     * @param e the exception that caused the shutdown
     */
    public void shutdown(Throwable e) throws MuleException
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
    public void shutdown() throws MuleException
    {
        logger.info("Mule container shutting down due to normal shutdown request");

        unregisterShutdownHook();
        doShutdown();
    }

    protected void doShutdown() throws MuleException
    {
        stop();

        System.exit(0);
    }

    public void stop() throws MuleException
    {
        coreExtensionManager.stop();

        if (deploymentService != null)
        {
            deploymentService.stop();
        }

        coreExtensionManager.dispose();
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
            super("Mule.shutdown.hook");
        }

        @Override
        public void run()
        {
            try
            {
                doShutdown();
            }
            catch (MuleException e)
            {
                logger.warn("Error shutting down mule container", e);
            }
        }
    }
}

