/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule;

import org.mule.config.ConfigurationBuilder;
import org.mule.config.ExceptionHelper;
import org.mule.config.i18n.Message;
import org.mule.config.i18n.Messages;
import org.mule.impl.MuleShutdownHook;
import org.mule.umo.UMOException;
import org.mule.util.ClassUtils;
import org.mule.util.IOUtils;
import org.mule.util.StringMessageUtils;
import org.mule.util.SystemUtils;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * <code>MuleServer</code> is a simple application that represents a local Mule
 * Server daemon. It is initialised with a mule-config.xml file.
 */
public class MuleServer implements Runnable
{
    public static final String CLI_OPTIONS[][] = {
            {"builder", "true", "Configuration Builder Type"},
            {"config", "true", "Configuration File"},
            {"idle", "false", "Whether to run in idle (unconfigured) mode"},
            {"main", "true", "Main Class"},
            {"mode", "true", "Run Mode"},
            {"props", "true", "Startup Properties"}
    };

    /**
     * Don't use a class object so the core doesn't depend on mule-module-builders.
     */
    protected static final String CLASSNAME_DEFAULT_CONFIG_BUILDER = "org.mule.config.builders.MuleXmlConfigurationBuilder";

    /**
     * This builder sets up the configuration for an idle Mule node - a node
     * that doesn't do anything initially but is fed configuration during
     * runtime
     */
    protected static final String CLASSNAME_DEFAULT_IDLE_CONFIG_BUILDER = "org.mule.config.builders.MuleIdleConfigurationBuilder";

    /**
     * Required to support the '-config spring' shortcut. Don't use a class object so
     * the core doesn't depend on mule-module-spring. TODO this may not be a problem
     * for Mule 2.x
     */
    protected static final String CLASSNAME_SPRING_CONFIG_BUILDER = "org.mule.extras.spring.config.MuleXmlConfigurationBuilder";

    /**
     * logger used by this class
     */
    private static Log logger = LogFactory.getLog(MuleServer.class);

    public static final String DEFAULT_CONFIGURATION = "mule-config.xml";

    /**
     * the message to display when the server shuts down
     */
    private static String shutdownMessage = null;

    /**
     * one or more configuration urls or filenames separated by commas
     */
    private String configurationResources = null;

    /**
     * A FQN of the #configBuilder class, required in case MuleServer is
     * reinitialised.
     */
    private static String configBuilderClassName = null;

    /**
     * A properties file to be read at startup. This can be useful for setting
     * properties which depend on the run-time environment (dev, test, production).
     */
    private static String startupPropertiesFile = null;

    /**
     * The Runtime shutdown thread used to dispose this server
     */
    private static MuleShutdownHook muleShutdownHook;

    protected Map options = Collections.EMPTY_MAP;

    /**
     * Application entry point.
     *
     * @param args command-line args
     */
    public static void main(String[] args) throws Exception
    {
        MuleServer server = new MuleServer(args);
        muleShutdownHook = new MuleShutdownHook(server);
        server.start(false, true);

    }

    public MuleServer()
    {
        this(new String[0]);
    }

    public MuleServer(String configResources)
    {
        setConfigurationResources(configResources);
    }

    /**
     * Configure the server with command-line arguments.
     */
    public MuleServer(String[] args) throws IllegalArgumentException
    {
        Map options;

        try
        {
            options = SystemUtils.getCommandLineOptions(args, CLI_OPTIONS);
        }
        catch (MuleException me)
        {
            throw new IllegalArgumentException(me.toString());
        }

        String config = (String) options.get("config");
        // Try default if no config file was given.
        if (config == null && !options.containsKey("idle"))
        {
            logger.warn("A configuration file was not set, using default: " + DEFAULT_CONFIGURATION);
            // try to load the config as a file as well
            URL configUrl = IOUtils.getResourceAsUrl(DEFAULT_CONFIGURATION, MuleServer.class, true);
            if (configUrl != null)
            {
                config = configUrl.toExternalForm();
            }
            else
            {
                System.out.println(new Message(Messages.CONFIG_NOT_FOUND_USAGE));
                System.exit(-1);
            }
        }

        if (config != null)
        {
            setConfigurationResources(config);
        }

        // Configuration builder
        String cfgBuilderClassName = (String)options.get("builder");

        if (options.containsKey("idle"))
        {
            setConfigurationResources("IDLE");
            cfgBuilderClassName = CLASSNAME_DEFAULT_IDLE_CONFIG_BUILDER;
        }

        // Configuration builder
        if (cfgBuilderClassName != null)
        {
            try
            {
                // Provide a shortcut for Spring: "-builder spring"
                if (cfgBuilderClassName.equalsIgnoreCase("spring"))
                {
                    cfgBuilderClassName = CLASSNAME_SPRING_CONFIG_BUILDER;
                }
                setConfigBuilderClassName(cfgBuilderClassName);
            }
            catch (Exception e)
            {
                throw new IllegalArgumentException(new Message(Messages.FAILED_LOAD_X, "Builder: " + cfgBuilderClassName).toString());
            }
        }

        // Startup properties
        String propertiesFile = (String) options.get("props");
        if (propertiesFile != null)
        {
            setStartupPropertiesFile(propertiesFile);
        }
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
            registerShutdownHook(muleShutdownHook);
        }
        if (ownThread)
        {
            Thread serverThread = new Thread(this, "MuleServer");
            serverThread.setDaemon(true);
            serverThread.start();
        }
        else
        {
            run();
        }
    }

    /**
     * Overloaded the [main] thread run method. This calls initialise and shuts down
     * if an exception occurs
     */
    public void run()
    {
        try
        {
            initialize();
        }
        catch (Throwable e)
        {
            shutdown(e);
        }
    }

    /**
     * Sets the configuration builder to use for this server. Note that if a builder
     * is not set and the server's start method is called the default is an instance
     * of <code>MuleXmlConfigurationBuilder</code>.
     *
     * @param builderClassName the configuration builder FQN to use
     * @throws ClassNotFoundException if the class with the given name can not be
     *                                loaded
     */
    public static void setConfigBuilderClassName(String builderClassName) throws ClassNotFoundException
    {
        if (builderClassName != null)
        {
            Class cls = ClassUtils.loadClass(builderClassName, MuleServer.class);
            if (ConfigurationBuilder.class.isAssignableFrom(cls))
            {
                MuleServer.configBuilderClassName = builderClassName;
            }
            else
            {
                throw new IllegalArgumentException("Not a usable ConfigurationBuilder class: "
                        + builderClassName);
            }
        }
        else
        {
            MuleServer.configBuilderClassName = null;
        }
    }

    /**
     * Returns the class name of the configuration builder used to create this
     * MuleServer.
     *
     * @return FQN of the current config builder
     */
    public static String getConfigBuilderClassName()
    {
        if (configBuilderClassName != null)
        {
            return configBuilderClassName;
        }
        else
        {
            return CLASSNAME_DEFAULT_CONFIG_BUILDER;
        }
    }

    /**
     * Initializes this daemon. Derived classes could add some extra behaviour if
     * they wish.
     *
     * @throws Exception if failed to initialize
     */
    public void initialize() throws Exception
    {
        logger.info("Mule Server starting...");

        // install an RMI security manager in case we expose any RMI objects
        //if (System.getSecurityManager() == null)
        //{
            // TODO Why is this disabled?
            // System.setSecurityManager(new RMISecurityManager());
        //}

        // create a new ConfigurationBuilder that is disposed afterwards
        Class cfgBuilderClass = ClassUtils.loadClass(getConfigBuilderClassName(), MuleServer.class);
        ConfigurationBuilder cfgBuilder = (ConfigurationBuilder) cfgBuilderClass.newInstance();

        if (!cfgBuilder.isConfigured())
        {
            if (configurationResources == null)
            {
                logger.warn("A configuration file was not set, using default: " + DEFAULT_CONFIGURATION);
                configurationResources = DEFAULT_CONFIGURATION;
            }
            cfgBuilder.configure(configurationResources, getStartupPropertiesFile());
        }
        logger.info("Mule Server initialized.");
    }

    /**
     * Will shut down the server displaying the cause and time of the shutdown
     *
     * @param e the exception that caused the shutdown
     */
    public void shutdown(Throwable e)
    {
        Message msg = new Message(Messages.FATAL_ERROR_WHILE_RUNNING);
        UMOException muleException = ExceptionHelper.getRootMuleException(e);
        if (muleException != null)
        {
            logger.fatal(muleException.getDetailedMessage());
        }
        else
        {
            logger.fatal(msg.toString() + " " + e.getMessage(), e);
        }
        List msgs = new ArrayList();
        msgs.add(msg.getMessage());
        Throwable root = ExceptionHelper.getRootException(e);
        msgs.add(root.getMessage() + " (" + root.getClass().getName() + ")");
        msgs.add(" ");
        msgs.add(new Message(Messages.FATAL_ERROR_SHUTDOWN));
        //TODO msgs.add(new Message(Messages.SERVER_STARTED_AT_X, new Date(managementContext.getStartDate())));
        msgs.add(new Message(Messages.SERVER_SHUTDOWN_AT_X, new Date().toString()));

        shutdownMessage = StringMessageUtils.getBoilerPlate(msgs, '*', 80);
        logger.fatal(shutdownMessage);
        System.exit(0);
    }

    /**
     * shutdown the server. This just displays the time the server shut down
     */
    public void shutdown()
    {
        logger.info("Mule server shutting down due to normal shutdown request");
        List msgs = new ArrayList();
        msgs.add(new Message(Messages.NORMAL_SHUTDOWN).getMessage());
        //TODO msgs.add(new Message(Messages.SERVER_STARTED_AT_X, new Date(managementContext.getStartDate())).getMessage());
        msgs.add(new Message(Messages.SERVER_SHUTDOWN_AT_X, new Date().toString()).getMessage());
        shutdownMessage = StringMessageUtils.getBoilerPlate(msgs, '*', 80);

        System.exit(0);

    }

    public void registerShutdownHook(MuleShutdownHook muleShutdownHook)
    {
        Runtime.getRuntime().removeShutdownHook(muleShutdownHook);
        Runtime.getRuntime().addShutdownHook(muleShutdownHook);
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

    /**
     * Setter for property messengerURL.
     *
     * @param configurationResources New value of property configurationResources.
     */
    public void setConfigurationResources(String configurationResources)
    {
        this.configurationResources = configurationResources;
    }

    public static String getStartupPropertiesFile()
    {
        return startupPropertiesFile;
    }

    public static void setStartupPropertiesFile(String startupPropertiesFile)
    {
        MuleServer.startupPropertiesFile = startupPropertiesFile;
    }
}
