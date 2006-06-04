/*
 * $Id$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.config.ConfigurationBuilder;
import org.mule.config.ExceptionHelper;
import org.mule.config.builders.MuleXmlConfigurationBuilder;
import org.mule.config.i18n.Message;
import org.mule.config.i18n.Messages;
import org.mule.umo.UMOException;
import org.mule.util.ClassUtils;
import org.mule.util.StringMessageUtils;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * <code>MuleServer</code> is a simple application that represents a local
 * Mule Server deamon. It is initalised with a mule-config.xml file.
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class MuleServer implements Runnable
{
    /**
     * Default configuration builder class name.
     */
    public static final Class DEFAULT_CONFIG_BUILDER_CLASS = MuleXmlConfigurationBuilder.class;

    /**
     * logger used by this class
     */
    private static transient Log logger = LogFactory.getLog(MuleServer.class);

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
     * A FQN of the #configBuilder class, required in case
     * MuleServer is reinitialised.
     */
    private static String configBuilderClassName = null;

    /**
     * default constructor
     */
    public MuleServer()
    {
        super();
    }

    public MuleServer(String configResources)
    {
        setConfigurationResources(configResources);
    }

    /**
     * application entry point
     */
    public static void main(String[] args)
    {
        MuleServer server = new MuleServer();
        List opts = Arrays.asList(args);
        String config = null;

        if (opts.size() > 0) {
            config = getOption("-config", opts);
            if (config != null) {
                server.setConfigurationResources(config);
            }
        } else {
            logger.warn("A configuration file was not set, using default: " + DEFAULT_CONFIGURATION);
            URL configUrl = ClassUtils.getResource(DEFAULT_CONFIGURATION, MuleServer.class);
            if (configUrl != null) {
                config = configUrl.toExternalForm();
                server.setConfigurationResources(config);
            }
        }

        if (config == null) {
            Message message = new Message(Messages.CONFIG_NOT_FOUND_USAGE);
            logger.fatal(message.toString());
            System.exit(1);
        }

        // save the ConfigrationBuilderClass name
        String cfgBuilderClassName = getOption("-builder", opts);
        if (cfgBuilderClassName != null) {
            try {
                setConfigBuilderClassName(cfgBuilderClassName);
            } catch (Exception e) {
                logger.fatal(new Message(Messages.FAILED_LOAD_X, "Builder: " + cfgBuilderClassName), e);
                System.exit(1);
            }
        }

        server.start(false);
    }

    private static String getOption(String option, List options)
    {
        if (options.contains(option)) {
            int i = options.indexOf(option);
            if (i < options.size() - 1) {
                return options.get(i + 1).toString();
            }
        }
        return null;
    }

    /**
     * Start the mule server
     *
     * @param ownThread determines if the server will run in its own daemon
     *            thread or the current calling thread
     */
    public void start(boolean ownThread)
    {
        if (ownThread) {
            Thread serverThread = new Thread(this, "MuleServer");
            serverThread.setDaemon(true);
            serverThread.start();
        } else {
            run();
        }
    }

    /**
     * Overloaded the [main] thread run method. This calls initialise and shuts
     * down if an exception occurs
     */
    public void run()
    {
        try {
            initialize();
        } catch (Throwable e) {
            shutdown(e);
        }
    }

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
     * @param configurationResources New value of property
     *            configurationResources.
     */
    public void setConfigurationResources(String configurationResources)
    {
        this.configurationResources = configurationResources;
    }

    /**
     * Sets the configuration builder to use for this server.
     * Note that if a builder is not set and the server's start method is called
     * the default is an instance of <code>MuleXmlConfigurationBuilder</code>.
     *
     * @param builderClassName the configuration builder FQN to use
     * @throws ClassNotFoundException if the class with the given name can not be loaded
     */
    public static void setConfigBuilderClassName(String builderClassName)
        throws ClassNotFoundException
    {
        if (builderClassName != null) {
            Class cls = ClassUtils.loadClass(builderClassName, MuleServer.class);
            if (ConfigurationBuilder.class.isAssignableFrom(cls)) {
                MuleServer.configBuilderClassName = builderClassName;
            } else {
                throw new IllegalArgumentException("Not a usable ConfigurationBuilder class: "
                        + builderClassName);
            }
        } else {
            MuleServer.configBuilderClassName = null;
        }
    }

    /**
     * Returns the class name of the configuration builder used to create
     * this MuleServer.
     * @return FQN of the current config builder
     */
    public static String getConfigBuilderClassName()
    {
        if (configBuilderClassName != null) {
            return configBuilderClassName;
        } else {
            return DEFAULT_CONFIG_BUILDER_CLASS.getName();
        }
    }

    /**
     * Initializes this daemon. Derived classes could add some extra behaviour
     * if they wish.
     * @throws Exception if failed to initialize
     */
    protected void initialize() throws Exception
    {
        logger.info("Mule Server starting...");

        // registerShutdownHook();
        // install an RMI security manager in case we expose any RMI objects
        if (System.getSecurityManager() == null) {
            // System.setSecurityManager(new RMISecurityManager());
        }

        // create a new ConfigurationBuilder that is disposed afterwards
        Class cfgBuilderClass = ClassUtils.loadClass(getConfigBuilderClassName(), MuleServer.class);
        ConfigurationBuilder cfgBuilder = (ConfigurationBuilder) cfgBuilderClass.newInstance();

        if (!cfgBuilder.isConfigured()) {
            if (configurationResources != null) {
                cfgBuilder.configure(configurationResources);
            } else {
                logger.warn("A configuration file was not set, using default: " + DEFAULT_CONFIGURATION);
                cfgBuilder.configure(DEFAULT_CONFIGURATION);
            }
        }

        logger.info("Mule Server initialized.");
    }

    /**
     * Will shut down the server displaying the cause and time of the shutdown
     * 
     * @param e the exception that caused the shutdown
     */
    void shutdown(Throwable e)
    {
        Message msg = new Message(Messages.FATAL_ERROR_WHILE_RUNNING);
        UMOException muleException = ExceptionHelper.getRootMuleException(e);
        if (muleException != null) {
            logger.fatal(muleException.getDetailedMessage());
        } else {
            logger.fatal(msg.toString() + " " + e.getMessage(), e);
        }
        List msgs = new ArrayList();
        msgs.add(msg.getMessage());
        Throwable root = ExceptionHelper.getRootException(e);
        msgs.add(root.getMessage() + " (" + root.getClass().getName() + ")");
        msgs.add(" ");
        msgs.add(new Message(Messages.FATAL_ERROR_SHUTDOWN));
        msgs.add(new Message(Messages.SERVER_STARTED_AT_X, new Date(MuleManager.getInstance().getStartDate())));
        msgs.add(new Message(Messages.SERVER_SHUTDOWN_AT_X, new Date().toString()));

        shutdownMessage = StringMessageUtils.getBoilerPlate(msgs, '*', 80);
        logger.fatal(shutdownMessage);
        System.exit(0);
    }

    /**
     * shutdown the server. This just displays the time the server shut down
     */
    void shutdown()
    {
        logger.info("Mule server shutting dow due to normal shutdown request");
        List msgs = new ArrayList();
        msgs.add(new Message(Messages.NORMAL_SHUTDOWN).getMessage());
        msgs.add(new Message(Messages.SERVER_STARTED_AT_X, new Date(MuleManager.getInstance().getStartDate())).getMessage());
        msgs.add(new Message(Messages.SERVER_SHUTDOWN_AT_X, new Date().toString()).getMessage());
        shutdownMessage = StringMessageUtils.getBoilerPlate(msgs, '*', 80);

        System.exit(0);

    }

}
