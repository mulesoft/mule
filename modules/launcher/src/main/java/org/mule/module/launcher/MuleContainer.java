/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.launcher;

import org.mule.MuleCoreExtension;
import org.mule.api.DefaultMuleException;
import org.mule.api.MuleException;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.config.ExceptionHelper;
import org.mule.config.StartupContext;
import org.mule.config.i18n.CoreMessages;
import org.mule.config.i18n.Message;
import org.mule.module.launcher.log4j.ApplicationAwareRepositorySelector;
import org.mule.util.ClassUtils;
import org.mule.util.MuleUrlStreamHandlerFactory;
import org.mule.util.StringMessageUtils;
import org.mule.util.SystemUtils;

import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.LogManager;

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

    public static final String SERVICE_PATH = "META-INF/services/org/mule/config/";

    public static final String CORE_EXTENSION_PROPERTIES = "core-extensions.properties";

    /**
     * logger used by this class
     */
    private static Log logger;

    /**
     * A properties file to be read at startup. This can be useful for setting
     * properties which depend on the run-time environment (dev, test, production).
     */
    private static String startupPropertiesFile = null;

    /**
     * The Runtime shutdown thread used to undeploy this server
     */
    private static MuleShutdownHook muleShutdownHook;

    protected DeploymentService deploymentService;

    protected Map<Class<? extends MuleCoreExtension>, MuleCoreExtension> coreExtensions = new HashMap<Class<? extends MuleCoreExtension>, MuleCoreExtension>();

    static
    {
        if (System.getProperty("mule.simpleLog") == null)
        {
            // TODO save this guard ref for later
            LogManager.setRepositorySelector(new ApplicationAwareRepositorySelector(), new Object());
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

    public MuleContainer()
    {
        init(new String[0]);
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

    public void start(boolean registerShutdownHook)
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
            coreExtensions = loadCoreExtensions();

            // TODO pluggable deployer
            deploymentService = new DeploymentService();

            initializeCoreExtensions();

            deploymentService.start();
        }
        catch (Throwable e)
        {
            shutdown(e);
        }
    }

    private void initializeCoreExtensions() throws InitialisationException
    {
        for (MuleCoreExtension extension : coreExtensions.values())
        {
            if (extension instanceof DeploymentServiceAware)
            {
                ((DeploymentServiceAware) extension).setDeploymentService(deploymentService);
            }

            if (extension instanceof MuleCoreExtensionAware)
            {
                ((MuleCoreExtensionAware) extension).setMuleCoreExtensions(coreExtensions);
            }

            extension.initialise();
        }
    }

    /**
     * Load all core extensions defined in the classpath
     */
    private Map<Class<? extends MuleCoreExtension>, MuleCoreExtension> loadCoreExtensions() throws MuleException
    {
        Map<Class<? extends MuleCoreExtension>, MuleCoreExtension> result = new HashMap<Class<? extends MuleCoreExtension>, MuleCoreExtension>();
        Enumeration<?> e = ClassUtils.getResources(SERVICE_PATH + CORE_EXTENSION_PROPERTIES, getClass());
        List<Properties> extensions = new LinkedList<Properties>();

        // load ALL of the extension files first
        while (e.hasMoreElements())
        {
            try
            {
                URL url = (URL) e.nextElement();
                if (logger.isDebugEnabled())
                {
                    logger.debug("Reading extension file: " + url.toString());
                }
                Properties p = new Properties();
                p.load(url.openStream());
                extensions.add(p);
            }
            catch (Exception ex)
            {
                throw new DefaultMuleException("Error loading Mule core extensions", ex);
            }
        }

        for (Properties extProps : extensions)
        {
            for (Map.Entry entry : extProps.entrySet())
            {
                String extName = (String) entry.getKey();
                String extClass = (String) entry.getValue();
                try
                {
                    MuleCoreExtension extension = (MuleCoreExtension) ClassUtils.instanciateClass(extClass);
                    result.put(extension.getClass(), extension);
                }
                catch (Exception ex)
                {
                    throw new DefaultMuleException("Error starting Mule core extension " + extName, ex);
                }
            }
        }

        return result;
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
        if (deploymentService != null)
        {
            deploymentService.stop();
        }

        for (MuleCoreExtension extension : coreExtensions.values())
        {
            try
            {
                extension.dispose();
            }
            catch (Exception ex)
            {
                logger.fatal("Error shutting down core extension " + extension.getName());
            }
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
            doShutdown();
        }
    }
}

