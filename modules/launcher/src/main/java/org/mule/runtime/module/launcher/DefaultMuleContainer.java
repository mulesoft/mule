/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.launcher;

import static org.mule.runtime.api.exception.ExceptionHelper.getRootException;
import static org.mule.runtime.api.exception.ExceptionHelper.getRootMuleException;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.api.util.MuleSystemProperties.DEPLOYMENT_APPLICATION_PROPERTY;
import static org.mule.runtime.api.util.MuleSystemProperties.MULE_SIMPLE_LOG;
import static org.mule.runtime.container.api.MuleFoldersUtil.getExecutionFolder;
import static org.mule.runtime.core.api.config.i18n.CoreMessages.fatalErrorInShutdown;
import static org.mule.runtime.core.api.config.i18n.CoreMessages.fatalErrorWhileRunning;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.startIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.stopIfNeeded;
import static org.mule.runtime.core.api.util.ClassUtils.withContextClassLoader;
import static org.mule.runtime.core.api.util.StringMessageUtils.getBoilerPlate;
import static org.mule.runtime.core.internal.logging.LogUtil.log;
import static org.mule.runtime.module.deployment.internal.MuleDeploymentService.findSchedulerService;
import static org.mule.runtime.module.deployment.internal.processor.SerializedAstArtifactConfigurationProcessor.serializedAstWithFallbackArtifactConfigurationProcessor;
import static org.mule.runtime.module.log4j.internal.MuleLog4jConfiguratorUtils.configureSelector;

import static java.lang.ClassLoader.getSystemClassLoader;
import static java.lang.Runtime.getRuntime;
import static java.lang.String.format;
import static java.lang.System.getProperty;
import static java.lang.System.setProperty;
import static java.util.Collections.emptyList;
import static java.util.concurrent.CompletableFuture.completedFuture;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.i18n.I18nMessage;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.core.internal.context.DefaultMuleContext;
import org.mule.runtime.core.internal.lock.ServerLockFactory;
import org.mule.runtime.module.artifact.activation.api.extension.discovery.ExtensionModelLoaderRepository;
import org.mule.runtime.module.artifact.api.classloader.ArtifactClassLoader;
import org.mule.runtime.module.artifact.api.classloader.net.MuleArtifactUrlStreamHandler;
import org.mule.runtime.module.artifact.api.classloader.net.MuleUrlStreamHandlerFactory;
import org.mule.runtime.module.artifact.internal.classloader.DefaultResourceInitializer;
import org.mule.runtime.module.deployment.api.DeploymentService;
import org.mule.runtime.module.deployment.impl.internal.MuleArtifactResourcesRegistry;
import org.mule.runtime.module.deployment.internal.MuleDeploymentService;
import org.mule.runtime.module.launcher.coreextension.ClasspathMuleCoreExtensionDiscoverer;
import org.mule.runtime.module.launcher.coreextension.DefaultMuleCoreExtensionManagerServer;
import org.mule.runtime.module.launcher.coreextension.MuleCoreExtensionManagerServer;
import org.mule.runtime.module.launcher.coreextension.ReflectionMuleCoreExtensionDependencyResolver;
import org.mule.runtime.module.launcher.internal.util.SystemUtils;
import org.mule.runtime.module.log4j.boot.api.MuleLog4jContextFactory;
import org.mule.runtime.module.reboot.internal.MuleContainer;
import org.mule.runtime.module.repository.api.RepositoryService;
import org.mule.runtime.module.repository.internal.RepositoryServiceFactory;
import org.mule.runtime.module.service.api.manager.ServiceManager;
import org.mule.runtime.module.tooling.api.ToolingService;
import org.mule.runtime.module.tooling.internal.DefaultToolingService;
import org.mule.runtime.module.troubleshooting.api.TroubleshootingService;
import org.mule.runtime.module.troubleshooting.internal.DefaultTroubleshootingService;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.spi.LoggerContextFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultMuleContainer implements MuleContainer {

  public static final String[][] CLI_OPTIONS =
      {{"builder", "true", "Configuration Builder Type"}, {"config", "true", "Configuration File"},
          {"idle", "false", "Whether to run in idle (unconfigured) mode"}, {"main", "true", "Main Class"},
          {"mode", "true", "Run Mode"}, {"props", "true", "Startup Properties"}, {"production", "false", "Production Mode"},
          {"debug", "false", "Configure Mule for JPDA remote debugging."}, {"app", "true", "Application to start"}};

  /**
   * logger used by this class
   */
  private static final Logger logger;

  static final String APP_COMMAND_LINE_OPTION = "app";
  static final String INVALID_DEPLOY_APP_CONFIGURATION_ERROR =
      format("Cannot set both '%s' option and '%s' property", APP_COMMAND_LINE_OPTION, DEPLOYMENT_APPLICATION_PROPERTY);

  /**
   * The Runtime shutdown thread used to undeploy this server
   */
  private static MuleShutdownHook muleShutdownHook;

  protected final DeploymentService deploymentService;
  private final RepositoryService repositoryService;
  private final ToolingService toolingService;
  private final MuleCoreExtensionManagerServer coreExtensionManager;
  private final TroubleshootingService troubleshootingService;
  private ServerLockFactory muleLockFactory;
  private final MuleArtifactResourcesRegistry artifactResourcesRegistry = new MuleArtifactResourcesRegistry.Builder()
      .artifactConfigurationProcessor(serializedAstWithFallbackArtifactConfigurationProcessor())
      .build();
  private static MuleLog4jContextFactory log4jContextFactory;

  static {
    if (getProperty(MULE_SIMPLE_LOG) == null) {
      LoggerContextFactory contextFactory = LogManager.getFactory();
      if (contextFactory instanceof MuleLog4jContextFactory) {
        log4jContextFactory = (MuleLog4jContextFactory) contextFactory;
      } else {
        LoggerFactory.getLogger("triggerDefaultFactoryCreation");
        setProperty("log4j2.loggerContextFactory", MuleLog4jContextFactory.class.getName());
        log4jContextFactory = new MuleLog4jContextFactory();
      }
      configureSelector(log4jContextFactory);
    }

    logger = LoggerFactory.getLogger(DefaultMuleContainer.class);
  }

  private final ServiceManager serviceManager;
  private final ExtensionModelLoaderRepository extensionModelLoaderRepository;

  /**
   * Application entry point (used only for experimentation purposes).
   *
   * @param args command-line args
   */
  // TODO W-12412027: remove this entry point once we have the alternative without Tanuki
  public static void main(String[] args) throws Exception {
    DefaultMuleContainer container = new DefaultMuleContainer(args);
    container.start(true);
  }

  public DefaultMuleContainer(String[] args) throws InitialisationException {
    init(args);

    this.serviceManager = artifactResourcesRegistry.getServiceManager();

    this.extensionModelLoaderRepository = artifactResourcesRegistry.getExtensionModelLoaderRepository();

    this.deploymentService = new MuleDeploymentService(artifactResourcesRegistry.getDomainFactory(),
                                                       artifactResourcesRegistry.getApplicationFactory(),
                                                       () -> findSchedulerService(serviceManager));
    this.troubleshootingService = new DefaultTroubleshootingService(deploymentService);
    this.repositoryService = new RepositoryServiceFactory().createRepositoryService();

    this.toolingService = new DefaultToolingService(artifactResourcesRegistry.getDomainRepository(),
                                                    artifactResourcesRegistry.getDomainFactory(),
                                                    artifactResourcesRegistry.getApplicationFactory(),
                                                    artifactResourcesRegistry.getToolingApplicationDescriptorFactory());
    this.coreExtensionManager = new DefaultMuleCoreExtensionManagerServer(
                                                                          new ClasspathMuleCoreExtensionDiscoverer(artifactResourcesRegistry
                                                                              .getContainerClassLoader()),
                                                                          new ReflectionMuleCoreExtensionDependencyResolver());
    this.muleLockFactory = artifactResourcesRegistry.getRuntimeLockFactory();

    artifactResourcesRegistry.getContainerClassLoader().dispose();
  }

  public DefaultMuleContainer(DeploymentService deploymentService, RepositoryService repositoryService,
                              ToolingService toolingService,
                              MuleCoreExtensionManagerServer coreExtensionManager, ServiceManager serviceManager,
                              ExtensionModelLoaderRepository extensionModelLoaderRepository,
                              TroubleshootingService troubleshootingService)
      throws InitialisationException {
    this(new String[0], deploymentService, repositoryService, toolingService, coreExtensionManager, serviceManager,
         extensionModelLoaderRepository, troubleshootingService);
  }

  /**
   * Configure the server with command-line arguments.
   */
  public DefaultMuleContainer(String[] args, DeploymentService deploymentService, RepositoryService repositoryService,
                              ToolingService toolingService, MuleCoreExtensionManagerServer coreExtensionManager,
                              ServiceManager serviceManager, ExtensionModelLoaderRepository extensionModelLoaderRepository,
                              TroubleshootingService troubleshootingService)
      throws IllegalArgumentException, InitialisationException {
    // TODO(pablo.kraan): remove the args argument and use the already existing setters to set everything needed
    init(args);

    this.deploymentService = deploymentService;
    this.coreExtensionManager = coreExtensionManager;
    this.repositoryService = repositoryService;
    this.serviceManager = serviceManager;
    this.extensionModelLoaderRepository = extensionModelLoaderRepository;
    this.toolingService = toolingService;
    this.troubleshootingService = troubleshootingService;
  }

  protected void init(String[] args) throws IllegalArgumentException, InitialisationException {
    // TODO(pablo.kraan): move initialization of others classes outside this method
    Map<String, Object> commandlineOptions = getCommandLineOptions(args);

    // set our own UrlStreamHandlerFactory to become more independent of system
    // properties
    MuleUrlStreamHandlerFactory.installUrlStreamHandlerFactory();
    MuleArtifactUrlStreamHandler.register();

    String appOption = (String) commandlineOptions.get(APP_COMMAND_LINE_OPTION);
    if (appOption != null) {
      if (getProperty(DEPLOYMENT_APPLICATION_PROPERTY) != null) {
        throw new IllegalArgumentException(INVALID_DEPLOY_APP_CONFIGURATION_ERROR);
      }
      setProperty(DEPLOYMENT_APPLICATION_PROPERTY, appOption);
    }

    artifactResourcesRegistry.getMemoryManagementService().initialise();
    artifactResourcesRegistry.inject(artifactResourcesRegistry.getContainerProfilingService());
    initialiseIfNeeded(artifactResourcesRegistry.getContainerProfilingService());
  }

  /**
   * Allows subclasses to obtain command line options differently.
   * <p>
   * Useful for testing purposes
   *
   * @param args arguments received from command line
   * @return map containing each configuration option name and value
   */
  Map<String, Object> getCommandLineOptions(String[] args) {
    Map<String, Object> commandlineOptions;
    try {
      commandlineOptions = SystemUtils.getCommandLineOptions(args, CLI_OPTIONS);
    } catch (MuleException me) {
      throw new IllegalArgumentException(me.toString());
    }
    return commandlineOptions;
  }

  private void createExecutionMuleFolder() {
    File executionFolder = getExecutionFolder();
    if (!executionFolder.exists()) {
      if (!executionFolder.mkdirs()) {
        throw new MuleRuntimeException(createStaticMessage(format(
                                                                  "Could not create folder %s, validate that the process has permissions over that directory",
                                                                  executionFolder.getAbsolutePath())));
      }
    }
  }

  @Override
  public void start(Future<Boolean> configurationsReady, List<String> additionalSplashEntries) throws MuleException {
    start(true, configurationsReady, additionalSplashEntries);
  }

  public void start(boolean registerShutdownHook) throws MuleException {
    start(registerShutdownHook, completedFuture(true), emptyList());
  }

  private void start(boolean registerShutdownHook, Future<Boolean> configurationsReady,
                     List<String> additionalSplashEntries)
      throws MuleException {
    if (registerShutdownHook) {
      registerShutdownHook();
    }
    try {
      startIfNeeded(artifactResourcesRegistry.getContainerProfilingService());

      doResourceInitialization();

      createExecutionMuleFolder();

      serviceManager.start();

      coreExtensionManager.setDeploymentService(deploymentService);
      coreExtensionManager.setRepositoryService(repositoryService);
      coreExtensionManager.setArtifactClassLoaderManager(artifactResourcesRegistry.getArtifactClassLoaderManager());
      coreExtensionManager.setToolingService(toolingService);
      coreExtensionManager.setServiceRepository(serviceManager);
      coreExtensionManager.setTroubleshootingService(troubleshootingService);

      // Waits for all bootstrapping configurations to be ready before progressing any further
      if (!configurationsReady.get()) {
        shutdown();
      }

      showSplashScreen(additionalSplashEntries);

      coreExtensionManager.initialise();
      coreExtensionManager.start();
      toolingService.initialise();

      startIfNeeded(extensionModelLoaderRepository);
      deploymentService.start();
    } catch (MuleException e) {
      shutdown(e);
      throw e;
    } catch (Throwable t) {
      shutdown(t);
      throw new MuleRuntimeException(t);
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

  private void showSplashScreen(List<String> additionalSplashEntries) {
    final MuleContainerStartupSplashScreen splashScreen = new MuleContainerStartupSplashScreen(additionalSplashEntries);
    splashScreen.doBody();
    log(splashScreen.toString());
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

    try {
      doShutdown();
    } catch (MuleException ex) {
      ex.addSuppressed(e);
      throw ex;
    }
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
    if (deploymentService != null) {
      deploymentService.stop();
    }

    if (muleLockFactory != null) {
      muleLockFactory.dispose();
    }

    if (extensionModelLoaderRepository != null) {
      stopIfNeeded(extensionModelLoaderRepository);
    }

    coreExtensionManager.stop();
    coreExtensionManager.dispose();

    if (serviceManager != null) {
      serviceManager.stop();
    }

    if (toolingService != null) {
      toolingService.stop();
    }

    LoggerContextFactory defaultLogManagerFactory = LogManager.getFactory();
    if (defaultLogManagerFactory instanceof MuleLog4jContextFactory) {
      ((MuleLog4jContextFactory) defaultLogManagerFactory).dispose();
    }

    if (log4jContextFactory != null && log4jContextFactory != defaultLogManagerFactory) {
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
      getRuntime().removeShutdownHook(muleShutdownHook);
    }
    getRuntime().addShutdownHook(muleShutdownHook);
  }

  public void unregisterShutdownHook() {
    if (muleShutdownHook != null) {
      getRuntime().removeShutdownHook(muleShutdownHook);
    }
  }

  // /////////////////////////////////////////////////////////////////
  // Getters and setters
  // /////////////////////////////////////////////////////////////////

  /**
   * This class is installed only for DefaultMuleContainer running as commandline app. A clean Mule shutdown can be achieved by
   * disposing the {@link DefaultMuleContext}.
   */
  private class MuleShutdownHook extends Thread {

    public MuleShutdownHook() {
      super("Mule.shutdown.hook");
    }

    @Override
    public void run() {
      try {
        DefaultMuleContainer.this.stop();
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

