/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.launcher;

import org.mule.runtime.core.api.DefaultMuleException;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.MuleRuntimeException;
import org.mule.runtime.core.api.config.MuleProperties;
import org.mule.runtime.core.config.ExceptionHelper;
import org.mule.runtime.core.config.StartupContext;
import org.mule.runtime.core.config.i18n.CoreMessages;
import org.mule.runtime.core.config.i18n.Message;
import org.mule.runtime.core.util.MuleUrlStreamHandlerFactory;
import org.mule.runtime.core.util.StringMessageUtils;
import org.mule.runtime.core.util.SystemUtils;
import org.mule.runtime.module.artifact.classloader.ArtifactClassLoader;
import org.mule.runtime.module.launcher.coreextension.ClasspathMuleCoreExtensionDiscoverer;
import org.mule.runtime.module.launcher.coreextension.DefaultMuleCoreExtensionManagerServer;
import org.mule.runtime.module.launcher.coreextension.MuleCoreExtensionManagerServer;
import org.mule.runtime.module.launcher.coreextension.ReflectionMuleCoreExtensionDependencyResolver;
import org.mule.runtime.module.launcher.log4j2.MuleLog4jContextFactory;
import org.mule.runtime.module.launcher.service.ServiceManager;
import org.mule.runtime.module.repository.api.RepositoryService;
import org.mule.runtime.module.repository.internal.RepositoryServiceFactory;
import org.mule.runtime.module.tooling.api.ToolingService;
import org.mule.runtime.module.tooling.internal.DefaultToolingService;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private static final Logger logger;

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
    private final RepositoryService repositoryService;
    private final ToolingService toolingService;
    private final MuleCoreExtensionManagerServer coreExtensionManager;
    private MuleArtifactResourcesRegistry artifactResourcesRegistry;

    static
    {
        if (System.getProperty(MuleProperties.MULE_SIMPLE_LOG) == null)
        {
            LogManager.setFactory(new MuleLog4jContextFactory());
        }

        logger = LoggerFactory.getLogger(MuleContainer.class);
    }

    private ServiceManager serviceManager;


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
        artifactResourcesRegistry = new MuleArtifactResourcesRegistry();

        this.serviceManager = artifactResourcesRegistry.getServiceManager();

        this.deploymentService = new MuleDeploymentService(artifactResourcesRegistry.getDomainFactory(), artifactResourcesRegistry.getApplicationFactory());
        this.repositoryService = new RepositoryServiceFactory().createRepositoryService();

        this.toolingService = new DefaultToolingService(repositoryService, new TemporaryToolingArtifactBuilderFactory(artifactResourcesRegistry));
        this.coreExtensionManager = new DefaultMuleCoreExtensionManagerServer(new ClasspathMuleCoreExtensionDiscoverer(artifactResourcesRegistry.getContainerClassLoader()), new ReflectionMuleCoreExtensionDependencyResolver());

        init(args);
    }

    public MuleContainer(DeploymentService deploymentService, RepositoryService repositoryService, ToolingService toolingService, MuleCoreExtensionManagerServer coreExtensionManager, ServiceManager serviceManager)
    {
        this(new String[0], deploymentService, repositoryService, toolingService, coreExtensionManager, serviceManager);
    }

    /**
     * Configure the server with command-line arguments.
     */
    public MuleContainer(String[] args, DeploymentService deploymentService, RepositoryService repositoryService, ToolingService toolingService, MuleCoreExtensionManagerServer coreExtensionManager, ServiceManager serviceManager) throws IllegalArgumentException
    {
        //TODO(pablo.kraan): remove the args argument and use the already existing setters to set everything needed
        this.deploymentService = deploymentService;
        this.coreExtensionManager = coreExtensionManager;
        this.repositoryService = repositoryService;
        this.serviceManager = serviceManager;
        this.toolingService = toolingService;
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

    private void createExecutionMuleFolder()
    {
        File executionFolder = MuleFoldersUtil.getExecutionFolder();
        if (!executionFolder.exists())
        {
            if (!executionFolder.mkdirs())
            {
                throw new MuleRuntimeException(CoreMessages.createStaticMessage(
                        String.format("Could not create folder %s, validate that the process has permissions over that directory", executionFolder.getAbsolutePath())));
            }
        }
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
            createExecutionMuleFolder();

            coreExtensionManager.setDeploymentService(deploymentService);
            coreExtensionManager.setRepositoryService(repositoryService);
            coreExtensionManager.setToolingService(toolingService);
            coreExtensionManager.initialise();
            coreExtensionManager.start();

            serviceManager.start();

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
            logger.error(muleException.getDetailedMessage());
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

        if (serviceManager != null)
        {
            serviceManager.stop();
        }

        coreExtensionManager.dispose();
        if (LogManager.getFactory() instanceof MuleLog4jContextFactory)
        {
            ((MuleLog4jContextFactory) LogManager.getFactory()).dispose();
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
     * {@link org.mule.runtime.core.DefaultMuleContext}.
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

