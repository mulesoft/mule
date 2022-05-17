/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.launcher;

import static org.mule.runtime.api.exception.ExceptionHelper.getRootException;
import static org.mule.runtime.api.exception.ExceptionHelper.getRootMuleException;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.api.util.MuleSystemProperties.DEPLOYMENT_APPLICATION_PROPERTY;
import static org.mule.runtime.api.util.MuleSystemProperties.MULE_SIMPLE_LOG;
import static org.mule.runtime.api.util.MuleSystemProperties.SYSTEM_PROPERTY_PREFIX;
import static org.mule.runtime.container.api.MuleFoldersUtil.getExecutionFolder;
import static org.mule.runtime.core.api.config.i18n.CoreMessages.fatalErrorInShutdown;
import static org.mule.runtime.core.api.config.i18n.CoreMessages.fatalErrorWhileRunning;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.startIfNeeded;
import static org.mule.runtime.core.api.util.ClassUtils.withContextClassLoader;
import static org.mule.runtime.core.api.util.StringMessageUtils.getBoilerPlate;
import static org.mule.runtime.core.internal.logging.LogUtil.log;
import static org.mule.runtime.module.deployment.internal.MuleDeploymentService.findSchedulerService;
import static org.mule.runtime.module.deployment.internal.processor.SerializedAstArtifactConfigurationProcessor.serializedAstWithFallbackArtifactConfigurationProcessor;

import java.security.Security;
import static java.lang.ClassLoader.getSystemClassLoader;
import static java.lang.Runtime.getRuntime;
import static java.lang.String.format;
import static java.lang.System.getProperty;
import static java.lang.System.setProperty;

import static org.apache.commons.lang3.reflect.MethodUtils.invokeStaticMethod;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.i18n.I18nMessage;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.core.internal.context.DefaultMuleContext;
import org.mule.runtime.core.internal.lock.ServerLockFactory;
import org.mule.runtime.deployment.model.internal.artifact.extension.ExtensionModelLoaderManager;
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
import org.mule.runtime.module.launcher.log4j2.MuleLog4jContextFactory;
import org.mule.runtime.module.reboot.MuleContainerBootstrap;
import org.mule.runtime.module.repository.api.RepositoryService;
import org.mule.runtime.module.repository.internal.RepositoryServiceFactory;
import org.mule.runtime.module.service.api.manager.ServiceManager;
import org.mule.runtime.module.tooling.api.ToolingService;
import org.mule.runtime.module.tooling.internal.DefaultToolingService;
import org.mule.runtime.module.troubleshooting.api.TroubleshootingService;
import org.mule.runtime.module.troubleshooting.internal.DefaultTroubleshootingService;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.security.Provider;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.spi.LoggerContextFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MuleContainer {

  private static Logger LOGGER = LoggerFactory.getLogger(MuleContainer.class.getName());

  private static final String PROPERTY_SECURITY_MODEL = SYSTEM_PROPERTY_PREFIX + "security.model";
  private static final String FIPS_PROVIDER = "org.bouncycastle.jcajce.provider.BouncyCastleFipsProvider";
  private static final String JSSE_PROVIDER = "org.bouncycastle.jsse.provider.BouncyCastleJsseProvider";
  private static final String KEY_MANAGER_FACTORY_ALGORITHM_KEY = "ssl.KeyManagerFactory.algorithm";
  private static final String TRUST_MANAGER_FACTORY_ALGORITHM_KEY = "ssl.KeyManagerFactory.algorithm";
  private static final String KEY_MANAGER_FACTORY_ALGORITHM_VALUE = "PKIX";
  private static final String KEYSTORE_TYPE_KEY = "keystore.type";
  private static final String KEYSTORE_TYPE_VALUE = "PKCS12";
  private static final String FIPS_SECURITY_MODEL = "fips140-2";
  private static final String SUN_JSSE_PROVIDER = "SunJSSE";
  private static final String LEGACY_SUN_JSSE_PROVIDER = "com.sun.net.ssl.internal.ssl.Provider";
  private static final String FIPS_KEY = "fips";
  private static final String FIPS_VALUE = "BCFIPS";

  public static final String CLI_OPTIONS[][] =
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
  private final TroubleshootingService troubleshootingService;
  private ServerLockFactory muleLockFactory;
  private final MuleArtifactResourcesRegistry artifactResourcesRegistry = new MuleArtifactResourcesRegistry.Builder()
      .artifactConfigurationProcessor(serializedAstWithFallbackArtifactConfigurationProcessor())
      .build();
  private static MuleLog4jContextFactory log4jContextFactory;

  static {
    if (getProperty(MULE_SIMPLE_LOG) == null) {
      // We need to force the creation of a logger before we can change the manager factory.
      // This is because if not, any logger that will be acquired by MuleLog4jContextFactory code
      // will fail since it will try to use a null factory.
      LoggerFactory.getLogger("triggerDefaultFactoryCreation");
      // We need to set this property so log4j uses the same context factory everywhere
      setProperty("log4j2.loggerContextFactory", MuleLog4jContextFactory.class.getName());
      log4jContextFactory = new MuleLog4jContextFactory();
      LogManager.setFactory(log4jContextFactory);
    }

    logger = LoggerFactory.getLogger(MuleContainer.class);
  }

  private final ServiceManager serviceManager;
  private final ExtensionModelLoaderManager extensionModelLoaderManager;
  private boolean embeddedMode = false;

  /**
   * Application entry point.
   *
   * @param args command-line args
   */
  public static void main(String[] args) throws Exception {
    MuleContainer container = new MuleContainer(args);
    container.start(true);
  }

  public MuleContainer(String[] args) throws InitialisationException {
    init(args);

    this.serviceManager = artifactResourcesRegistry.getServiceManager();

    this.extensionModelLoaderManager = artifactResourcesRegistry.getExtensionModelLoaderManager();

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

  public MuleContainer(DeploymentService deploymentService, RepositoryService repositoryService, ToolingService toolingService,
                       MuleCoreExtensionManagerServer coreExtensionManager, ServiceManager serviceManager,
                       ExtensionModelLoaderManager extensionModelLoaderManager, TroubleshootingService troubleshootingService)
      throws InitialisationException {
    this(new String[0], deploymentService, repositoryService, toolingService, coreExtensionManager, serviceManager,
         extensionModelLoaderManager, troubleshootingService);
  }

  /**
   * Configure the server with command-line arguments.
   */
  public MuleContainer(String[] args, DeploymentService deploymentService, RepositoryService repositoryService,
                       ToolingService toolingService, MuleCoreExtensionManagerServer coreExtensionManager,
                       ServiceManager serviceManager, ExtensionModelLoaderManager extensionModelLoaderManager,
                       TroubleshootingService troubleshootingService)
      throws IllegalArgumentException, InitialisationException {
    // TODO(pablo.kraan): remove the args argument and use the already existing setters to set everything needed
    init(args);

    this.deploymentService = deploymentService;
    this.coreExtensionManager = coreExtensionManager;
    this.repositoryService = repositoryService;
    this.serviceManager = serviceManager;
    this.extensionModelLoaderManager = extensionModelLoaderManager;
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

    // Startup properties
    String propertiesFile = (String) commandlineOptions.get("props");
    if (propertiesFile != null) {
      setStartupPropertiesFile(propertiesFile);
    }

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

    if (isFipsEnabled()) {
      configureSecurityManager();
      setSecurityAlgorithm();
    }
  }

  private void configureSecurityManager() throws InitialisationException {
    try {
      Class<?> classDef =
          Class.forName(FIPS_PROVIDER);
      Constructor<?> constructor = classDef.getConstructor();
      Provider fipsProvider = (Provider) constructor.newInstance();
      Security.insertProviderAt(fipsProvider, 1);

      classDef =
          Class.forName(JSSE_PROVIDER);
      constructor = classDef.getConstructor();
      Provider jsseProvider = (Provider) constructor.newInstance();
      jsseProvider.setProperty(FIPS_KEY, FIPS_VALUE);
      Security.insertProviderAt(jsseProvider, 2);

      Provider sunJsseProvider = Security.getProvider(SUN_JSSE_PROVIDER);
      // for java 8
      Provider sunJsseProviderLegacy = Security.getProvider(LEGACY_SUN_JSSE_PROVIDER);

      if (sunJsseProvider != null) {
        Security.removeProvider(SUN_JSSE_PROVIDER);
      }

      if (sunJsseProviderLegacy != null) {
        sunJsseProviderLegacy.setProperty(FIPS_VALUE, "");
      }
    } catch (ClassNotFoundException | InvocationTargetException | NoSuchMethodException | InstantiationException
        | IllegalAccessException e) {
      LOGGER.error("Critical error while enabling FIPS", e);
      throw new InitialisationException(createStaticMessage("Critical error while enabling FIPS:"), e, null);
    }
  }

  private void setSecurityAlgorithm() {
    Security.setProperty(KEY_MANAGER_FACTORY_ALGORITHM_KEY, KEY_MANAGER_FACTORY_ALGORITHM_VALUE);
    Security.setProperty(TRUST_MANAGER_FACTORY_ALGORITHM_KEY,
                         KEY_MANAGER_FACTORY_ALGORITHM_VALUE);
    Security.setProperty(KEYSTORE_TYPE_KEY, KEYSTORE_TYPE_VALUE);
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

  public void start(boolean registerShutdownHook) throws MuleException {
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

      validateLicense();
      showSplashScreen();

      coreExtensionManager.initialise();
      coreExtensionManager.start();
      toolingService.initialise();

      extensionModelLoaderManager.start();
      deploymentService.start();
    } catch (Throwable e) {
      shutdown(e);
    }
  }

  private void validateLicense() {
    try {
      invokeStaticMethod(MuleContainerBootstrap.class, "awaitLicenseValidation");
    } catch (NoSuchMethodException e) {
      return;
    } catch (Exception e) {
      throw new MuleRuntimeException(e);
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
    final MuleContainerStartupSplashScreen splashScreen = new MuleContainerStartupSplashScreen(isEmbeddedMode());
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

    if (deploymentService != null) {
      deploymentService.stop();
    }

    if (muleLockFactory != null) {
      muleLockFactory.dispose();
    }

    if (extensionModelLoaderManager != null) {
      extensionModelLoaderManager.stop();
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


  public static String getStartupPropertiesFile() {
    return startupPropertiesFile;
  }

  public static void setStartupPropertiesFile(String startupPropertiesFile) {
    MuleContainer.startupPropertiesFile = startupPropertiesFile;
  }

  /**
   * This flag can be set to true to indicate that the container is being ran in embedded mode which can be used to adapt some
   * behaviours such as the info in the splash screen
   * 
   * @param embeddedMode set to true for embedded mode
   */
  public void setEmbeddedMode(boolean embeddedMode) {
    this.embeddedMode = embeddedMode;
  }

  /**
   * @return true if the container is set to embedded mode (instead of standalone)
   */
  public boolean isEmbeddedMode() {
    return embeddedMode;
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

  private boolean isFipsEnabled() {
    return FIPS_SECURITY_MODEL.equals(getProperty(PROPERTY_SECURITY_MODEL));
  }
}

