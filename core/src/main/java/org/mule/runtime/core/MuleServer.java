/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core;

import static java.util.Collections.emptyMap;
import static org.mule.runtime.core.config.bootstrap.ArtifactType.APP;
import org.mule.runtime.core.api.DefaultMuleException;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.config.ConfigurationBuilder;
import org.mule.runtime.core.api.config.ConfigurationException;
import org.mule.runtime.core.api.config.MuleConfiguration;
import org.mule.runtime.core.api.context.MuleContextBuilder;
import org.mule.runtime.core.api.context.MuleContextFactory;
import org.mule.runtime.core.config.ExceptionHelper;
import org.mule.runtime.core.config.PropertiesMuleConfigurationFactory;
import org.mule.runtime.core.config.StartupContext;
import org.mule.runtime.core.config.bootstrap.ArtifactType;
import org.mule.runtime.core.config.builders.SimpleConfigurationBuilder;
import org.mule.runtime.core.config.i18n.CoreMessages;
import org.mule.runtime.core.config.i18n.Message;
import org.mule.runtime.core.context.DefaultMuleContextBuilder;
import org.mule.runtime.core.context.DefaultMuleContextFactory;
import org.mule.runtime.core.util.ClassUtils;
import org.mule.runtime.core.util.IOUtils;
import org.mule.runtime.core.util.JdkVersionUtils;
import org.mule.runtime.core.util.MuleUrlStreamHandlerFactory;
import org.mule.runtime.core.util.PropertiesUtils;
import org.mule.runtime.core.util.StringMessageUtils;
import org.mule.runtime.core.util.SystemUtils;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <code>MuleServer</code> is a simple application that represents a local Mule
 * Server daemon. It is initialised with a mule-config.xml file.
 */
public class MuleServer implements Runnable
{
    public static final String CLI_OPTIONS[][] = {
        {"builder", "true", "Configuration Builder Type"},
        {"config", "true", "Configuration File"},
        {"appconfig", "true", "Application configuration File"},
        {"idle", "false", "Whether to run in idle (unconfigured) mode"},
        {"main", "true", "Main Class"},
        {"mode", "true", "Run Mode"},
        {"props", "true", "Startup Properties"},
        {"production", "false", "Production Mode"},
        {"debug", "false", "Configure Mule for JPDA remote debugging."}
    };

    /**
     * Don't use a class object so the core doesn't depend on mule-module-spring-config.
     */
    public static final String CLASSNAME_DEFAULT_CONFIG_BUILDER = "org.mule.runtime.core.config.builders.AutoConfigurationBuilder";

    /**
     * This builder sets up the configuration for an idle Mule node - a node that
     * doesn't do anything initially but is fed configuration during runtime
     */
    protected static final String CLASSNAME_DEFAULT_IDLE_CONFIG_BUILDER = "org.mule.runtime.core.config.builders.MuleIdleConfigurationBuilder";

    /**
     * Required to support the '-config spring' shortcut. Don't use a class object so
     * the core doesn't depend on mule-module-spring.
     * for Mule 2.x
     */
    protected static final String CLASSNAME_SPRING_CONFIG_BUILDER = "org.mule.runtime.config.spring.SpringXmlConfigurationBuilder";

    /**
     * logger used by this class
     */
    private static final Logger logger = LoggerFactory.getLogger(MuleServer.class);

    public static final String DEFAULT_CONFIGURATION = "mule-config.xml";

    public static final String DEFAULT_APP_CONFIGURATION = "mule-app.properties";

    /**
     * one or more configuration urls or filenames separated by commas
     */
    private String configurationResources = null;

    private String appConfigurationResource = null;

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

    /**
     * The MuleContext should contain anything which does not belong in the Registry.
     * There is one MuleContext per Mule instance. Assuming it has been created, a
     * handle to the local MuleContext can be obtained from anywhere by calling
     * MuleServer.getMuleContext()
     */
    protected MuleContext muleContext = null;

    /**
     * Application entry point.
     *
     * @param args command-line args
     * @throws Exception if there is an exception creating the MuleServer
     */
    public static void main(String[] args) throws Exception
    {
        MuleServer server = new MuleServer(args);
        server.start(false, true);
    }

    public MuleServer()
    {
        init(new String[]{});
    }

    public MuleServer(String configResources)
    {
        // setConfigurationResources(configResources);
        init(new String[]{"-config", configResources});
    }

    /**
     * Configure the server with command-line arguments.
     * @param args Command line args passed in from the {@link #main(String[])} method
     * @throws IllegalArgumentException if an argument is passed in that is not recognised by the Mule Server
     */
    public MuleServer(String[] args) throws IllegalArgumentException
    {
        init(args);
    }

    protected void init(String[] args) throws IllegalArgumentException
    {
    	// validate the JDK version/vendor
    	try
    	{
    		JdkVersionUtils.validateJdk();
    	}
    	catch (RuntimeException e)
    	{
    		System.out.println(CoreMessages.invalidJdk(SystemUtils.JAVA_VERSION, 
    				JdkVersionUtils.getSupportedJdks()));
            System.exit(-1);
    	}
    	
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

        String config = (String) commandlineOptions.get("config");
        // Try default if no config file was given.
        if (config == null && !commandlineOptions.containsKey("idle"))
        {
            logger.warn("A configuration file was not set, using default: " + DEFAULT_CONFIGURATION);
            // try to load the config as a file as well
            URL configUrl = IOUtils.getResourceAsUrl(DEFAULT_CONFIGURATION, MuleServer.class, true, false);
            if (configUrl != null)
            {
                config = configUrl.toExternalForm();
            }
            else
            {
                System.out.println(CoreMessages.configNotFoundUsage());
                System.exit(-1);
            }
        }

        if (config != null)
        {
            setConfigurationResources(config);
        }

        String appconfig = (String) commandlineOptions.get("appconfig");
        this.appConfigurationResource = appconfig;

        // Configuration builder
        String cfgBuilderClassName = (String) commandlineOptions.get("builder");

        if (commandlineOptions.containsKey("idle"))
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
                final Message message = CoreMessages.failedToLoad("Builder: " + cfgBuilderClassName);
                logger.error(message.toString(), e);
                System.err.println(StringMessageUtils.getBoilerPlate("ERROR: " + message.toString()));
                System.exit(1);
            }
        }

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
     * @param registerShutdownHook whether to register the default Mule Server shutdown hock.  this will shut down mule cleanly if
     * the JVM is shutdown.  The only reason not to register this hook is to override it with a custom version
     */
    public void start(boolean ownThread, boolean registerShutdownHook)
    {
        if (registerShutdownHook)
        {
            registerShutdownHook();
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
            logger.info("Mule Server initializing...");
            initialize();
            logger.info("Mule Server starting...");
            muleContext.start();
        }
        catch (Throwable e)
        {
            shutdown(e);
        }
    }

    /**
     * Sets the configuration builder to use for this server. Note that if a builder
     * is not set and the server's start method is called the default is an instance
     * of <code>SpringXmlConfigurationBuilder</code>.
     *
     * @param builderClassName the configuration builder FQN to use
     * @throws ClassNotFoundException if the class with the given name can not be
     *                                loaded
     */
    public static void setConfigBuilderClassName(String builderClassName) throws ClassNotFoundException
    {
        if (builderClassName != null)
        {
            Class<?> cls = ClassUtils.loadClass(builderClassName, MuleServer.class);
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
        if (configurationResources == null)
        {
            logger.warn("A configuration file was not set, using default: " + DEFAULT_CONFIGURATION);
            configurationResources = DEFAULT_CONFIGURATION;
        }

        // create a new ConfigurationBuilder that is disposed afterwards
        ConfigurationBuilder cfgBuilder = createConfigurationBuilder();

        if (!cfgBuilder.isConfigured())
        {
            List<ConfigurationBuilder> configBuilders = new ArrayList<ConfigurationBuilder>(3);
            addStartupPropertiesConfigBuilder(configBuilders);
            configBuilders.add(cfgBuilder);

            MuleConfiguration configuration = createMuleConfiguration();

            MuleContextBuilder contextBuilder = new DefaultMuleContextBuilder();
            contextBuilder.setMuleConfiguration(configuration);

            MuleContextFactory contextFactory = new DefaultMuleContextFactory();
            muleContext = contextFactory.createMuleContext(configBuilders, contextBuilder);
        }
    }

    protected ConfigurationBuilder createConfigurationBuilder() throws ConfigurationException
    {
        try
        {
            return (ConfigurationBuilder) ClassUtils.instanciateClass(getConfigBuilderClassName(),
                                                                      new Object[]{ configurationResources , emptyMap(), APP}, MuleServer.class);
        }
        catch (Exception e)
        {
            throw new ConfigurationException(CoreMessages.failedToLoad(getConfigBuilderClassName()), e);
        }
    }

    protected void addStartupPropertiesConfigBuilder(List<ConfigurationBuilder> builders) throws IOException
    {
        Properties startupProperties = null;
        if (getStartupPropertiesFile() != null)
        {
            startupProperties = PropertiesUtils.loadProperties(getStartupPropertiesFile(), getClass());
        }

        builders.add(new SimpleConfigurationBuilder(startupProperties));
    }

    protected MuleConfiguration createMuleConfiguration()
    {
        String appPropertiesFile = null;
        if (this.appConfigurationResource == null)
        {
            appPropertiesFile = PropertiesMuleConfigurationFactory.getMuleAppConfiguration(this.configurationResources);
        }
        else
        {
            appPropertiesFile = this.appConfigurationResource;
        }

        return new PropertiesMuleConfigurationFactory(appPropertiesFile).createConfiguration();
    }

    /**
     * Will shut down the server displaying the cause and time of the shutdown
     *
     * @param e the exception that caused the shutdown
     */
    public void shutdown(Throwable e)
    {
        doShutdown();
        unregisterShutdownHook();

        Message msg = CoreMessages.fatalErrorWhileRunning();
        MuleException muleException = ExceptionHelper.getRootMuleException(e);
        int exitCode = 1;
        if (muleException != null)
        {
            logger.error(muleException.getDetailedMessage());
            exitCode = muleException.getExceptionCode();
        }
        else
        {
            logger.error(msg.toString() + " " + e.getMessage(), e);
        }
        List<String> msgs = new ArrayList<String>();
        msgs.add(msg.getMessage());
        Throwable root = ExceptionHelper.getRootException(e);
        msgs.add(root.getMessage() + " (" + root.getClass().getName() + ")");
        msgs.add(" ");
        msgs.add(CoreMessages.fatalErrorInShutdown().getMessage());
        String shutdownMessage = StringMessageUtils.getBoilerPlate(msgs, '*', 80);
        logger.error(shutdownMessage);

        System.exit(exitCode);
    }

    /**
     * shutdown the server. This just displays the time the server shut down
     */
    public void shutdown()
    {
        logger.info("Mule server shutting down due to normal shutdown request");

        unregisterShutdownHook();
        doShutdown();
        System.exit(0);
    }

    protected void doShutdown()
    {
        if (muleContext != null)
        {
            muleContext.dispose();
            muleContext = null;
        }
    }

    public Logger getLogger()
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

    /**
     * Setter for property configurationResources.
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

    public MuleContext getMuleContext()
    {
        return muleContext;
    }

    /**
     * This class is installed only for MuleServer running as commandline app. A
     * clean Mule shutdown can be achieved by disposing the
     * {@link org.mule.runtime.core.DefaultMuleContext}.
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
            System.exit(0);
        }
    }
}
