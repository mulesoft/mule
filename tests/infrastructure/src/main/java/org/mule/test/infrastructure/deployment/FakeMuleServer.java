/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.infrastructure.deployment;

import static org.mule.runtime.api.util.MuleSystemProperties.MULE_SIMPLE_LOG;
import static org.mule.runtime.container.api.MuleFoldersUtil.APPS_FOLDER;
import static org.mule.runtime.container.api.MuleFoldersUtil.DOMAINS_FOLDER;
import static org.mule.runtime.container.api.MuleFoldersUtil.SERVICES_FOLDER;
import static org.mule.runtime.core.api.config.MuleProperties.MULE_HOME_DIRECTORY_PROPERTY;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.startIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.stopIfNeeded;
import static org.mule.runtime.internal.memory.management.DefaultMemoryManagementService.newDefaultMemoryManagementService;
import static org.mule.runtime.module.deployment.internal.DefaultArchiveDeployer.JAR_FILE_SUFFIX;
import static org.mule.runtime.module.deployment.internal.MuleDeploymentService.findSchedulerService;
import static org.mule.runtime.module.deployment.internal.processor.SerializedAstArtifactConfigurationProcessor.serializedAstWithFallbackArtifactConfigurationProcessor;
import static org.mule.runtime.module.log4j.internal.MuleLog4jConfiguratorUtils.getDefaultReconfigurationAction;

import static java.lang.System.setProperty;
import static java.lang.Thread.currentThread;

import static org.apache.commons.io.FileUtils.copyDirectory;
import static org.apache.commons.io.FileUtils.copyFile;
import static org.apache.commons.io.FileUtils.copyURLToFile;
import static org.apache.commons.io.FilenameUtils.getBaseName;
import static org.apache.commons.io.FilenameUtils.getName;
import static org.apache.commons.lang3.StringUtils.removeEnd;
import static org.apache.commons.lang3.StringUtils.removeEndIgnoreCase;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.container.api.MuleCoreExtension;
import org.mule.runtime.deployment.model.api.application.Application;
import org.mule.runtime.module.artifact.activation.api.extension.discovery.ExtensionModelLoaderRepository;
import org.mule.runtime.module.artifact.api.classloader.ArtifactClassLoader;
import org.mule.runtime.module.deployment.api.DeploymentListener;
import org.mule.runtime.module.deployment.api.DeploymentService;
import org.mule.runtime.module.deployment.impl.internal.MuleArtifactResourcesRegistry;
import org.mule.runtime.module.deployment.internal.DeploymentServiceBuilder;
import org.mule.runtime.module.launcher.coreextension.DefaultMuleCoreExtensionManagerServer;
import org.mule.runtime.module.launcher.coreextension.ReflectionMuleCoreExtensionDependencyResolver;
import org.mule.runtime.module.repository.api.RepositoryService;
import org.mule.runtime.module.repository.internal.RepositoryServiceFactory;
import org.mule.runtime.module.service.api.manager.ServiceManager;
import org.mule.runtime.module.tooling.api.ToolingService;
import org.mule.runtime.module.tooling.internal.DefaultToolingService;
import org.mule.tck.probe.PollingProber;
import org.mule.tck.probe.Probe;
import org.mule.tck.probe.Prober;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

public class FakeMuleServer {

  protected static final int DEPLOYMENT_TIMEOUT = 20000;
  private final RepositoryService repositoryService;

  private final File muleHome;
  private File appsDir;
  private File domainsDir;
  private File logsDir;
  private File serverPluginsDir;
  private File servicesDir;

  private final DeploymentService deploymentService;
  private final DeploymentListener domainDeploymentListener;
  private final DeploymentListener deploymentListener;
  private final ToolingService toolingService;

  private final List<MuleCoreExtension> coreExtensions;

  public static final String FAKE_SERVER_DISABLE_LOG_REPOSITORY_SELECTOR = "fake.server.disablelogrepositoryselector";

  static {
    // NOTE: this causes mule.simpleLog to no work on these tests
    if (!Boolean.getBoolean(FAKE_SERVER_DISABLE_LOG_REPOSITORY_SELECTOR)) {
      setProperty(MULE_SIMPLE_LOG, "true");
    }
  }

  private final DefaultMuleCoreExtensionManagerServer coreExtensionManager;
  private final ArtifactClassLoader containerClassLoader;
  private final ServiceManager serviceManager;
  private final ExtensionModelLoaderRepository extensionModelLoaderRepository;

  public FakeMuleServer(String muleHomePath) {
    this(muleHomePath, new LinkedList<>());
  }

  public FakeMuleServer(String muleHomePath, List<MuleCoreExtension> intialCoreExtensions) {
    MuleArtifactResourcesRegistry muleArtifactResourcesRegistry = new MuleArtifactResourcesRegistry.Builder()
        .artifactConfigurationProcessor(serializedAstWithFallbackArtifactConfigurationProcessor())
        // This is done to guarantee that different fake servers (containers)
        // have different memory management services.
        .withMemoryManagementService(newDefaultMemoryManagementService())
        .withActionOnMuleArtifactDeployment(getDefaultReconfigurationAction())
        .build();
    muleArtifactResourcesRegistry.inject(muleArtifactResourcesRegistry.getContainerProfilingService());
    containerClassLoader = muleArtifactResourcesRegistry.getContainerClassLoader();
    serviceManager = muleArtifactResourcesRegistry.getServiceManager();
    extensionModelLoaderRepository = muleArtifactResourcesRegistry.getExtensionModelLoaderRepository();

    this.coreExtensions = intialCoreExtensions;
    for (MuleCoreExtension extension : coreExtensions) {
      extension.setContainerClassLoader(containerClassLoader);
    }

    muleHome = new File(muleHomePath);
    muleHome.deleteOnExit();
    try {
      setProperty(MULE_HOME_DIRECTORY_PROPERTY, getMuleHome().getCanonicalPath());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    try {
      setMuleFolders();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    repositoryService = new RepositoryServiceFactory().createRepositoryService();

    toolingService = new DefaultToolingService(muleArtifactResourcesRegistry.getDomainRepository(),
                                               muleArtifactResourcesRegistry.getDomainFactory(),
                                               muleArtifactResourcesRegistry.getApplicationFactory(),
                                               muleArtifactResourcesRegistry.getToolingApplicationDescriptorFactory());
    deploymentService = DeploymentServiceBuilder.deploymentServiceBuilder()
        .withArtifactStartExecutorSupplier(() -> findSchedulerService(serviceManager))
        .withDomainFactory(muleArtifactResourcesRegistry.getDomainFactory())
        .withApplicationFactory(muleArtifactResourcesRegistry.getApplicationFactory())
        .build();
    deploymentListener = mock(DeploymentListener.class);
    doAnswer(inv -> {
      final String artifactName = inv.getArgument(0);
      final Throwable cause = inv.getArgument(1);

      System.err.println("Deployment failure for " + artifactName + ":");
      cause.printStackTrace();

      return null;
    })
        .when(deploymentListener)
        .onDeploymentFailure(anyString(), any());
    deploymentService.addDeploymentListener(deploymentListener);
    domainDeploymentListener = mock(DeploymentListener.class);
    deploymentService.addDomainDeploymentListener(domainDeploymentListener);

    coreExtensionManager =
        new DefaultMuleCoreExtensionManagerServer(() -> coreExtensions, new ReflectionMuleCoreExtensionDependencyResolver());
    coreExtensionManager.setDeploymentService(deploymentService);
    coreExtensionManager.setArtifactClassLoaderManager(muleArtifactResourcesRegistry.getArtifactClassLoaderManager());
    coreExtensionManager.setRepositoryService(repositoryService);
    coreExtensionManager.setServiceRepository(serviceManager);
  }

  public void stop() throws MuleException {
    deploymentService.stop();
    serviceManager.stop();
    stopIfNeeded(extensionModelLoaderRepository);
    coreExtensionManager.stop();
    coreExtensionManager.dispose();
  }

  public void start() throws IOException, MuleException {
    serviceManager.start();
    coreExtensionManager.initialise();
    coreExtensionManager.start();
    startIfNeeded(extensionModelLoaderRepository);
    deploymentService.start();
  }

  public ToolingService toolingService() {
    return this.toolingService;
  }

  public void assertDeploymentSuccess(String appName) {
    assertDeploymentSuccess(deploymentListener, appName);
  }

  public void assertDeploymentFailure(String appName) {
    assertDeploymentFailure(deploymentListener, appName);
  }

  public void assertUndeploymentSuccess(String appName) {
    assertUndeploymentSuccess(deploymentListener, appName);
  }

  private void assertDeploymentFailure(final DeploymentListener listener, final String appName) {
    Prober prober = new PollingProber(DEPLOYMENT_TIMEOUT, 100);
    prober.check(new Probe() {

      @Override
      public boolean isSatisfied() {
        try {
          verify(listener, times(1)).onDeploymentFailure(eq(appName), any(Throwable.class));
          return true;
        } catch (AssertionError e) {
          return false;
        }
      }

      @Override
      public String describeFailure() {
        return "Failed to deploy application: " + appName;
      }
    });
  }

  private void assertDeploymentSuccess(final DeploymentListener listener, final String appName) {
    Prober prober = new PollingProber(DEPLOYMENT_TIMEOUT, 100);
    prober.check(new Probe() {

      @Override
      public boolean isSatisfied() {
        try {
          verify(listener, times(1)).onDeploymentSuccess(appName);
          return true;
        } catch (AssertionError e) {
          return false;
        }
      }

      @Override
      public String describeFailure() {
        return "Failed to deploy application: " + appName;
      }
    });
  }

  public void assertUndeploymentSuccess(final DeploymentListener listener, final String appName) {
    Prober prober = new PollingProber(DEPLOYMENT_TIMEOUT, 100);
    prober.check(new Probe() {

      @Override
      public boolean isSatisfied() {
        try {
          verify(listener, times(1)).onUndeploymentSuccess(appName);
          return true;
        } catch (AssertionError e) {
          return false;
        }
      }

      @Override
      public String describeFailure() {
        return "Failed to deploy application: " + appName;
      }
    });
  }

  private void setMuleFolders() throws IOException {
    appsDir = createFolder(APPS_FOLDER);
    logsDir = createFolder("logs");
    serverPluginsDir = createFolder("server-plugins");
    servicesDir = createFolder(SERVICES_FOLDER);
    domainsDir = createFolder(DOMAINS_FOLDER);
    createFolder(DOMAINS_FOLDER + "/default");

    File confDir = createFolder("conf");
    URL log4jFile = currentThread().getContextClassLoader().getResource("log4j2-test.xml");
    copyURLToFile(log4jFile, new File(confDir, "log4j2-test.xml"));
  }

  private File createFolder(String folderName) {
    File folder = new File(getMuleHome(), folderName);

    if (!folder.exists()) {
      if (!folder.mkdirs()) {
        throw new IllegalStateException(String.format("Unable to create folder '%s'", folderName));
      }
    }

    return folder;
  }

  /**
   * Copies a given app archive to the apps folder for deployment.
   *
   * @throws URISyntaxException
   */
  public void addAppArchive(URL url) throws IOException, URISyntaxException {
    addAppArchive(url, null);
  }

  public void deploy(String resource) throws IOException, URISyntaxException {
    int lastSeparator = resource.lastIndexOf(File.separator);
    String appName = removeEndIgnoreCase(resource.substring(lastSeparator + 1), JAR_FILE_SUFFIX);
    deploy(resource, appName);
  }

  /**
   * Deploys an application from a classpath resource
   *
   * @param resource      points to the resource to deploy. Non null.
   * @param targetAppName application name used to deploy the resource. Null to maintain the original resource name
   * @throws IOException        if the resource cannot be accessed
   * @throws URISyntaxException
   */
  public void deploy(String resource, String targetAppName) throws IOException, URISyntaxException {
    URL url = getClass().getResource(resource);
    deploy(url, targetAppName);
  }

  /**
   * Deploys an application from an URL
   *
   * @param resource      points to the resource to deploy. Non null.
   * @param targetAppName application name used to deploy the resource. Null to maintain the original resource name
   * @throws IOException        if the URL cannot be accessed
   * @throws URISyntaxException
   */
  public void deploy(URL resource, String targetAppName) throws IOException, URISyntaxException {
    addAppArchive(resource, targetAppName + JAR_FILE_SUFFIX);
    assertDeploymentSuccess(targetAppName);
  }

  /**
   * Copies a given app archive with a given target name to the apps folder for deployment
   *
   * @throws URISyntaxException
   */
  private void addAppArchive(URL url, String targetFile) throws IOException, URISyntaxException {
    // copy is not atomic, copy to a temp file and rename instead (rename is atomic)
    final String tempFileName = (targetFile == null ? new File(url.toURI()) : new File(targetFile)).getName() + ".part";
    final File tempFile = new File(appsDir, tempFileName);
    copyURLToFile(url, tempFile);
    boolean renamed = tempFile.renameTo(new File(removeEnd(tempFile.getAbsolutePath(), ".part")));
    if (!renamed) {
      throw new IllegalStateException("Unable to add application archive");
    }
  }

  /**
   * Adds a server plugin file to the Mule server.
   *
   * @param plugin plugin file to add. Non null.
   * @throws IOException if the plugin file cannot be accessed
   */
  public void addZippedServerPlugin(File plugin) throws IOException {
    addZippedServerPlugin(plugin.toURI().toURL());
  }

  /**
   * Adds a server plugin to the Mule server .
   *
   * @param resource points to the plugin to add. Non null.
   * @throws IOException if the plugin URL cannot be accessed
   */
  public void addZippedServerPlugin(URL resource) throws IOException {
    String baseName = getName(resource.getPath());
    File tempFile = new File(getServerPluginsDir(), baseName);
    copyURLToFile(resource, tempFile);
  }

  /**
   * Adds a service implementation file to the Mule server.
   *
   * @param service service file to add. Non null.
   * @throws IOException if the service file cannot be accessed
   */
  public void addZippedService(File service) throws IOException {
    String baseName = getName(service.getPath());
    File tempFile = new File(getServicesDir(), baseName);
    copyDirectory(service, tempFile);
  }

  public File getMuleHome() {
    return muleHome;
  }

  public File getLogsDir() {
    return logsDir;
  }

  public File getAppsDir() {
    return appsDir;
  }

  private File getDomainsDir() {
    return domainsDir;
  }

  public File getServerPluginsDir() {
    return serverPluginsDir;
  }

  public File getServicesDir() {
    return servicesDir;
  }

  public void resetDeploymentListener() {
    reset(deploymentListener);
  }

  public void addCoreExtension(MuleCoreExtension coreExtension) {
    coreExtension.setContainerClassLoader(containerClassLoader);
    coreExtensions.add(coreExtension);
  }

  public void addDeploymentListener(DeploymentListener listener) {
    deploymentService.addDeploymentListener(listener);
  }

  public void removeDeploymentListener(DeploymentListener listener) {
    deploymentService.removeDeploymentListener(listener);
  }

  /**
   * Finds deployed application by name.
   *
   * @return the application if found, null otherwise
   */
  public Application findApplication(String appName) {
    return deploymentService.findApplication(appName);
  }

  /**
   * Deploys a Domain from a classpath folder
   *
   * @param domainFolder folder in which the domain is defined
   * @param domainName   name of the domain to use as domain artifact name
   * @throws URISyntaxException
   */
  public void deployDomainFromClasspathFolder(String domainFolder, String domainName) throws URISyntaxException {
    copyExplodedArtifactFromClasspathFolderToDeployFolder(domainFolder, getDomainsDir(), domainName);
  }

  /**
   * Deploys a Domain artifact file
   *
   * @param domain domain file to deploy
   */
  public void deployDomainFile(File domain) throws IOException {
    ReentrantLock lock = this.deploymentService.getLock();
    lock.lock();
    try {
      copyFile(domain, new File(getDomainsDir(), domain.getName()));
    } finally {
      lock.unlock();
    }
    assertDeploymentSuccess(domainDeploymentListener, getBaseName(domain.getName()));
  }

  /**
   * Deploys an Application from a classpath folder
   *
   * @param appFolder folder in which the app is defined
   * @param appName   name of the domain to use as app artifact name
   * @throws URISyntaxException
   */
  public void deployAppFromClasspathFolder(String appFolder, String appName) throws URISyntaxException {
    copyExplodedArtifactFromClasspathFolderToDeployFolder(appFolder, getAppsDir(), appName);
  }

  private void copyExplodedArtifactFromClasspathFolderToDeployFolder(String artifactFolderPath, File artifactDirectory,
                                                                     String artifactName)
      throws URISyntaxException {
    ReentrantLock lock = this.deploymentService.getLock();
    lock.lock();
    try {
      URL resource = getClass().getClassLoader().getResource(artifactFolderPath);
      File artifactFolder = new File(resource.toURI());
      copyExplodedArtifactFromFolderToDeployFolder(artifactFolder, artifactDirectory, artifactName);
    } finally {
      lock.unlock();
    }
  }

  private void copyExplodedArtifactFromFolderToDeployFolder(File artifactFolder, File artifactDirectory,
                                                            String artifactName) {
    ReentrantLock lock = this.deploymentService.getLock();
    lock.lock();
    try {
      copyDirectory(artifactFolder, new File(artifactDirectory, artifactName));
    } catch (IOException e) {
      throw new MuleRuntimeException(e);
    } finally {
      lock.unlock();
    }
  }

  /**
   * @return repository server to download artifacts. To configure it use system properties from {@link RepositoryServiceFactory}.
   */
  public RepositoryService getRepositoryService() {
    return repositoryService;
  }
}
