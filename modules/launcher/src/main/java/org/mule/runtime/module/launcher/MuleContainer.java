/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.launcher;

import static java.lang.ClassLoader.getSystemClassLoader;
import static org.mule.runtime.api.exception.ExceptionHelper.getRootException;
import static org.mule.runtime.api.exception.ExceptionHelper.getRootMuleException;
import static org.mule.runtime.core.api.config.i18n.CoreMessages.fatalErrorInShutdown;
import static org.mule.runtime.core.api.config.i18n.CoreMessages.fatalErrorWhileRunning;
import static org.mule.runtime.core.api.util.ClassUtils.withContextClassLoader;
import static org.mule.runtime.core.api.util.StringMessageUtils.getBoilerPlate;
import static org.mule.runtime.module.deployment.internal.MuleDeploymentService.findSchedulerService;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.i18n.I18nMessage;
import org.mule.runtime.container.api.MuleFoldersUtil;
import org.mule.runtime.core.api.config.MuleProperties;
import org.mule.runtime.core.api.config.i18n.CoreMessages;
import org.mule.runtime.core.api.util.SystemUtils;
import org.mule.runtime.core.internal.config.StartupContext;
import org.mule.runtime.core.internal.context.DefaultMuleContext;
import org.mule.runtime.module.artifact.api.classloader.ArtifactClassLoader;
import org.mule.runtime.module.artifact.api.classloader.net.MuleArtifactUrlStreamHandler;
import org.mule.runtime.module.artifact.api.classloader.net.MuleUrlStreamHandlerFactory;
import org.mule.runtime.module.artifact.internal.classloader.DefaultResourceInitializer;
import org.mule.runtime.module.deployment.api.DeploymentService;
import org.mule.runtime.module.deployment.impl.internal.MuleArtifactResourcesRegistry;
import org.mule.runtime.module.deployment.internal.MuleDeploymentService;
import org.mule.runtime.module.extension.internal.loader.ExtensionModelLoaderManager;
import org.mule.runtime.module.launcher.coreextension.ClasspathMuleCoreExtensionDiscoverer;
import org.mule.runtime.module.launcher.coreextension.DefaultMuleCoreExtensionManagerServer;
import org.mule.runtime.module.launcher.coreextension.MuleCoreExtensionManagerServer;
import org.mule.runtime.module.launcher.coreextension.ReflectionMuleCoreExtensionDependencyResolver;
import org.mule.runtime.module.launcher.log4j2.MuleLog4jContextFactory;
import org.mule.runtime.module.reboot.MuleContainerBootstrap;
import org.mule.runtime.module.repository.api.RepositoryService;
import org.mule.runtime.module.repository.internal.RepositoryServiceFactory;
import org.mule.runtime.module.service.api.manager.ServiceManager;
import org.mule.runtime.module.tooling.api.ToolingService;
import org.mule.runtime.module.tooling.internal.DefaultToolingService;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MuleContainer {

  public static final String CLI_OPTIONS[][] =
      {{"builder", "true", "Configuration Builder Type"}, {"config", "true", "Configuration File"},
          {"idle", "false", "Whether to run in idle (unconfigured) mode"}, {"main", "true", "Main Class"},
          {"mode", "true", "Run Mode"}, {"props", "true", "Startup Properties"}, {"production", "false", "Production Mode"},
          {"debug", "false", "Configure Mule for JPDA remote debugging."}, {"app", "true", "Application to start"}};

  /**
   * logger used by this class
   */
  private static final Logger logger;

  /**
   * A properties file to be read at startup. This can be useful for setting properties which depend on the run-time environment
   * (dev, test, production).
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
  private MuleArtifactResourcesRegistry artifactResourcesRegistry = new MuleArtifactResourcesRegistry.Builder().build();
  private static MuleLog4jContextFactory log4jContextFactory;

  static {
    if (System.getProperty(MuleProperties.MULE_SIMPLE_LOG) == null) {
      // We need to force the creation of a logger before we can change the manager factory.
      // This is because if not, any logger that will be acquired by MuleLog4jContextFactory code
      // will fail since it will try to use a null factory.
      LoggerFactory.getLogger("triggerDefaultFactoryCreation");
      // We need to set this property so log4j uses the same context factory everywhere
      System.setProperty("log4j2.loggerContextFactory", MuleLog4jContextFactory.class.getName());
      log4jContextFactory = new MuleLog4jContextFactory();
      LogManager.setFactory(log4jContextFactory);
    }

    logger = LoggerFactory.getLogger(MuleContainer.class);
  }

  private ServiceManager serviceManager;
  private ExtensionModelLoaderManager extensionModelLoaderManager;

  /**
   * Application entry point.
   *
   * @param args command-line args
   */
  public static void main(String[] args) throws Exception {
    MuleContainer container = new MuleContainer(args);
    container.start(true);
  }

  public MuleContainer(String[] args) {
    init(args);

    this.serviceManager = artifactResourcesRegistry.getServiceManager();

    this.extensionModelLoaderManager = artifactResourcesRegistry.getExtensionModelLoaderManager();

    this.deploymentService = new MuleDeploymentService(artifactResourcesRegistry.getDomainFactory(),
                                                       artifactResourcesRegistry.getApplicationFactory(),
                                                       () -> findSchedulerService(serviceManager));
    this.repositoryService = new RepositoryServiceFactory().createRepositoryService();

    this.toolingService = new DefaultToolingService(artifactResourcesRegistry.getApplicationFactory());
    this.coreExtensionManager = new DefaultMuleCoreExtensionManagerServer(
                                                                          new ClasspathMuleCoreExtensionDiscoverer(artifactResourcesRegistry
                                                                              .getContainerClassLoader()),
                                                                          new ReflectionMuleCoreExtensionDependencyResolver());

    artifactResourcesRegistry.getContainerClassLoader().dispose();
  }

  public MuleContainer(DeploymentService deploymentService, RepositoryService repositoryService, ToolingService toolingService,
                       MuleCoreExtensionManagerServer coreExtensionManager, ServiceManager serviceManager,
                       ExtensionModelLoaderManager extensionModelLoaderManager) {
    this(new String[0], deploymentService, repositoryService, toolingService, coreExtensionManager, serviceManager,
         extensionModelLoaderManager);
  }

  /**
   * Configure the server with command-line arguments.
   */
  public MuleContainer(String[] args, DeploymentService deploymentService, RepositoryService repositoryService,
                       ToolingService toolingService, MuleCoreExtensionManagerServer coreExtensionManager,
                       ServiceManager serviceManager, ExtensionModelLoaderManager extensionModelLoaderManager)
      throws IllegalArgumentException {
    // TODO(pablo.kraan): remove the args argument and use the already existing setters to set everything needed
    init(args);

    this.deploymentService = deploymentService;
    this.coreExtensionManager = coreExtensionManager;
    this.repositoryService = repositoryService;
    this.serviceManager = serviceManager;
    this.extensionModelLoaderManager = extensionModelLoaderManager;
    this.toolingService = toolingService;
  }

  protected void init(String[] args) throws IllegalArgumentException {
    // TODO(pablo.kraan): move initialization of others classes outside this method
    Map<String, Object> commandlineOptions;

    try {
      commandlineOptions = SystemUtils.getCommandLineOptions(args, CLI_OPTIONS);
    } catch (MuleException me) {
      throw new IllegalArgumentException(me.toString());
    }

    // set our own UrlStreamHandlerFactory to become more independent of system
    // properties
    MuleUrlStreamHandlerFactory.installUrlStreamHandlerFactory();
    MuleArtifactUrlStreamHandler.register();

    // Startup properties
    String propertiesFile = (String) commandlineOptions.get("props");
    if (propertiesFile != null) {
      setStartupPropertiesFile(propertiesFile);
    }
    StartupContext.get().setStartupOptions(commandlineOptions);
  }

  private void createExecutionMuleFolder() {
    File executionFolder = MuleFoldersUtil.getExecutionFolder();
    if (!executionFolder.exists()) {
      if (!executionFolder.mkdirs()) {
        throw new MuleRuntimeException(CoreMessages.createStaticMessage(String.format(
                                                                                      "Could not create folder %s, validate that the process has permissions over that directory",
                                                                                      executionFolder.getAbsolutePath())));
      }
    }
  }

  public void start(boolean registerShutdownHook) throws MuleException {
    if (registerShutdownHook) {
      registerShutdownHook();
    }
    showSplashScreen();
    try {
      doResourceInitialization();

      createExecutionMuleFolder();

      coreExtensionManager.setDeploymentService(deploymentService);
      coreExtensionManager.setRepositoryService(repositoryService);
      coreExtensionManager.setArtifactClassLoaderManager(artifactResourcesRegistry.getArtifactClassLoaderManager());
      coreExtensionManager.setToolingService(toolingService);
      coreExtensionManager.initialise();
      coreExtensionManager.start();

      serviceManager.start();

      extensionModelLoaderManager.start();

      deploymentService.start();
    } catch (Throwable e) {
      shutdown(e);
    }
  }

  private void doResourceInitialization() {
    withContextClassLoader(getSystemClassLoader(), () -> {
      try {
        new DefaultResourceInitializer().initialize();
      } catch (Exception e) {
        logger.error("Cannot create resource initializer instance", e);
      }
    });
  }

  protected void showSplashScreen() {
    final MuleContainerStartupSplashScreen splashScreen = new MuleContainerStartupSplashScreen();
    splashScreen.doBody();
    logger.info(splashScreen.toString());
  }

  /**
   * Will shut down the server displaying the cause and time of the shutdown
   *
   * @param e the exception that caused the shutdown
   */
  public void shutdown(Throwable e) throws MuleException {
    I18nMessage msg = fatalErrorWhileRunning();
    MuleException muleException = getRootMuleException(e);
    if (muleException != null) {
      logger.error(muleException.getDetailedMessage());
    } else {
      logger.error(msg.toString() + " " + e.getMessage(), e);
    }
    List<String> msgs = new ArrayList<>();
    msgs.add(msg.getMessage());
    Throwable root = getRootException(e);
    msgs.add(root.getMessage() + " (" + root.getClass().getName() + ")");
    msgs.add(" ");
    msgs.add(fatalErrorInShutdown().getMessage());
    String shutdownMessage = getBoilerPlate(msgs, '*', 80);
    logger.error(shutdownMessage);

    doShutdown();
    System.exit(1);
  }

  /**
   * shutdown the server. This just displays the time the server shut down
   */
  public void shutdown() throws MuleException {
    logger.info("Mule container shutting down due to normal shutdown request");

    doShutdown();
  }

  protected void doShutdown() throws MuleException {
    unregisterShutdownHook();
    stop();
  }

  public void stop() throws MuleException {
    MuleContainerBootstrap.dispose();

    coreExtensionManager.stop();

    if (deploymentService != null) {
      deploymentService.stop();
    }

    if (serviceManager != null) {
      serviceManager.stop();
    }

    if (extensionModelLoaderManager != null) {
      extensionModelLoaderManager.stop();
    }

    coreExtensionManager.dispose();
    if (LogManager.getFactory() instanceof MuleLog4jContextFactory) {
      ((MuleLog4jContextFactory) LogManager.getFactory()).dispose();
    }

    if (log4jContextFactory != null) {
      log4jContextFactory.dispose();
    }
  }

  public Logger getLogger() {
    return logger;
  }

  public void registerShutdownHook() {
    if (muleShutdownHook == null) {
      muleShutdownHook = new MuleShutdownHook();
    } else {
      Runtime.getRuntime().removeShutdownHook(muleShutdownHook);
    }
    Runtime.getRuntime().addShutdownHook(muleShutdownHook);
  }

  public void unregisterShutdownHook() {
    if (muleShutdownHook != null) {
      Runtime.getRuntime().removeShutdownHook(muleShutdownHook);
    }
  }

  // /////////////////////////////////////////////////////////////////
  // Getters and setters
  // /////////////////////////////////////////////////////////////////


  public static String getStartupPropertiesFile() {
    return startupPropertiesFile;
  }

  public static void setStartupPropertiesFile(String startupPropertiesFile) {
    MuleContainer.startupPropertiesFile = startupPropertiesFile;
  }

  /**
   * This class is installed only for MuleContainer running as commandline app. A clean Mule shutdown can be achieved by disposing
   * the {@link DefaultMuleContext}.
   */
  private class MuleShutdownHook extends Thread {

    public MuleShutdownHook() {
      super("Mule.shutdown.hook");
    }

    @Override
    public void run() {
      try {
        MuleContainer.this.stop();
      } catch (MuleException e) {
        logger.warn("Error stopping mule container", e);
      }
    }
  }

  /**
   * @return {@link DeploymentService} of the runtime.
   */
  public DeploymentService getDeploymentService() {
    return deploymentService;
  }

  /**
   * @return {@link ArtifactClassLoader} of the runtime.
   */
  public ArtifactClassLoader getContainerClassLoader() {
    return artifactResourcesRegistry.getContainerClassLoader();
  }
}

