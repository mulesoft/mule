/* 
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 * 
 * Copyright (c) Cubis Limited. All rights reserved.
 * http://www.cubis.co.uk 
 * 
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file. 
 *
 */

package org.mule;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.config.ConfigurationBuilder;
import org.mule.config.builders.MuleXmlConfigurationBuilder;
import org.mule.util.ClassHelper;
import org.mule.util.StringMessageHelper;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * <code>MuleServer</code> is a simple application that represents a local
 * Mule Server deamon. It is initalised with a mule-configuration.xml file.
 *
 * @author <a href="mailto:ross.mason@cubis.co.uk">Ross Mason</a>
 * @version $Revision$
 */
public class MuleServer implements Runnable
{

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
    private String configurationResources;

    /**
     * The configuration builder used to construct the MuleManager
     */
    private static ConfigurationBuilder configBuilder = null;

    /**
     * default constructor
     */
    public MuleServer()
    {
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

        if(opts.size() > 0) {
            config = getOption("-config", opts);
            if(config != null) {
                server.setConfigurationResources(config);
            }
        } else {
            URL configUrl = ClassHelper.getResource("mule-config.xml", MuleServer.class);
            if(configUrl != null)
            {
                config = configUrl.toExternalForm();
                server.setConfigurationResources(config);
            }
        }

        if(config==null)
        {
            System.out.println("No config file was specified and no config by the name of mule-config.xml was found on the classpath");
            System.out.println("Usage: MuleServer -config <mule-configuration> [-builder <config-builder>]");
            System.out.println("   mule-configuration = a URL for the Mule Config XML to use, if not specified the config will be loaded from the class path");
            System.out.println("   config-builder = a fully qualified class name of the builder to use to configure Mule");

            System.exit(0);
        }

        String builder = getOption("-builder", opts);
        if(builder != null) {
            try
            {
                configBuilder = (ConfigurationBuilder)ClassHelper.loadClass(builder, MuleServer.class).newInstance();
            }
            catch (Exception e)
            {
                e.printStackTrace();
                System.out.println("Failed to load builder: " + builder);
            }
        } else {
            //use this by default
            configBuilder = new MuleXmlConfigurationBuilder();
        }

        server.start(false);
    }

    private static String getOption(String option, List options)
    {
        if(options.contains(option)) {
            int i = options.indexOf(option);
            if(i < options.size() -1) {
                return options.get(i + 1).toString();
            }
        }
        return null;
    }

    /**
     * Start the mule server
     * @param ownThread determines if the server will run in its own daemon thread
     * or the current calling thread
     */
    public void start(boolean ownThread) {
        if(ownThread) {
            Thread serverThread = new Thread(this, "MuleServer");
            serverThread.setDaemon(true);
            serverThread.start();
        } else {
            run();
        }
    }

    /**
     * Overloaded the [main] thread run method.  This calls initialise and shuts down
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

    /**
     * gets the configuration builder used by this server.
     * Note that if a builder is not set and the server's start method is call
     * the default is an instance of <code>MuleXmlConfigurationBuilder</code>.
     * @return a configuration builder or null if one has not been set
     */
    public static ConfigurationBuilder getConfigBuilder()
    {
        return configBuilder;
    }

    /**
     * sets the configuration builder to use for this server.
     * @param configBuilder the configuration builder instance to use
     */
    public static void setConfigBuilder(ConfigurationBuilder configBuilder)
    {
        MuleServer.configBuilder = configBuilder;
    }

    /**
     * Initializes this daemon. Derived classes could add some extra behaviour
     * if they wish.
     */
    protected void initialize() throws Exception
    {
        System.out.println("Mule Server starting...");

        //registerShutdownHook();
        // install an RMI security manager in case we expose any RMI objects
        if (System.getSecurityManager() == null)
        {
            //System.setSecurityManager(new RMISecurityManager());
        }

        if(configBuilder == null) {
            configBuilder = new MuleXmlConfigurationBuilder();
        }
        if (configurationResources != null)
        {
            configBuilder.configure(configurationResources);
        }
        else
        {
            logger.warn("A configuration file was not set, using default: " + DEFAULT_CONFIGURATION);
            configBuilder.configure(DEFAULT_CONFIGURATION);
        }
        System.out.println("Mule Server initialized.");
    }

    /**
     * Will shut down the server displaying the cause and time of the shutdown
     *
     * @param e the exception that caused the shutdown
     */
    void shutdown(Throwable e)
    {
        logger.fatal(e, e);
        List msgs = new ArrayList();
        msgs.add("A Fatal error has occurred while the server was running-");
        msgs.add(" ");
        msgs.add(e.getMessage() + "[" + e.getClass().getName() + "]");

        Throwable cause = e.getCause();
        while (cause != null)
        {
            msgs.add("Caused by: " + cause.getMessage() + "[" + cause.getClass().getName() + "]");
            cause = cause.getCause();
        }
        msgs.add("The error is fatal, the system must shutdown.");
        msgs.add("The system was started at : " + new Date(MuleManager.getInstance().getStartDate()));        
        msgs.add("The system shutdown at : " + new Date().toString());

        shutdownMessage = StringMessageHelper.getBoilerPlate(msgs, '*', 80);
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
        msgs.add("Mule server shutting dow due to normal shutdown request");
        msgs.add("The system was started at : " + new Date(MuleManager.getInstance().getStartDate()));
        msgs.add("The system shutdown at : " + new Date().toString());
        shutdownMessage = StringMessageHelper.getBoilerPlate(msgs, '*', 80);

        System.exit(0);

    }

}
