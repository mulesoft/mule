/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.infrastructure.deployment;

import static org.apache.commons.io.FileUtils.copyURLToFile;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import org.mule.runtime.container.api.MuleCoreExtension;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.MuleRuntimeException;
import org.mule.runtime.core.api.config.MuleProperties;
import org.mule.runtime.core.util.FileUtils;
import org.mule.runtime.core.util.FilenameUtils;
import org.mule.runtime.core.util.StringUtils;
import org.mule.runtime.module.artifact.classloader.ArtifactClassLoader;
import org.mule.runtime.module.launcher.DeploymentListener;
import org.mule.runtime.module.launcher.DeploymentService;
import org.mule.runtime.module.launcher.MuleArtifactResourcesRegistry;
import org.mule.runtime.module.launcher.MuleDeploymentService;
import org.mule.runtime.module.launcher.application.Application;
import org.mule.runtime.module.launcher.coreextension.DefaultMuleCoreExtensionManagerServer;
import org.mule.runtime.module.launcher.coreextension.ReflectionMuleCoreExtensionDependencyResolver;
import org.mule.runtime.module.launcher.service.ServiceManager;
import org.mule.tck.probe.PollingProber;
import org.mule.tck.probe.Probe;
import org.mule.tck.probe.Prober;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

public class FakeMuleServer
{

    protected static final int DEPLOYMENT_TIMEOUT = 20000;

    private File muleHome;
    private File appsDir;
    private File domainsDir;
    private File logsDir;
    private File serverPluginsDir;

    private final DeploymentService deploymentService;
    private final DeploymentListener deploymentListener;

    private final List<MuleCoreExtension> coreExtensions;

    public static final String FAKE_SERVER_DISABLE_LOG_REPOSITORY_SELECTOR = "fake.server.disablelogrepositoryselector";

    static
    {
        // NOTE: this causes mule.simpleLog to no work on these tests
        if (!Boolean.getBoolean(FAKE_SERVER_DISABLE_LOG_REPOSITORY_SELECTOR))
        {
            System.setProperty(MuleProperties.MULE_SIMPLE_LOG, "true");
        }
    }

    private DefaultMuleCoreExtensionManagerServer coreExtensionManager;
    private final ArtifactClassLoader containerClassLoader;
    private ServiceManager serviceManager;

    public FakeMuleServer(String muleHomePath)
    {
        this(muleHomePath, new LinkedList<>());
    }

    public FakeMuleServer(String muleHomePath, List<MuleCoreExtension> intialCoreExtensions)
    {
        MuleArtifactResourcesRegistry muleArtifactResourcesRegistry = new MuleArtifactResourcesRegistry();
        containerClassLoader = muleArtifactResourcesRegistry.getContainerClassLoader();
        serviceManager = muleArtifactResourcesRegistry.getServiceManager();

        this.coreExtensions = intialCoreExtensions;
        for (MuleCoreExtension extension : coreExtensions)
        {
            extension.setContainerClassLoader(containerClassLoader);
        }

        muleHome = new File(muleHomePath);
        muleHome.deleteOnExit();
        try
        {
            System.setProperty(MuleProperties.MULE_HOME_DIRECTORY_PROPERTY, getMuleHome().getCanonicalPath());
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }

        try
        {
            setMuleFolders();
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }

        deploymentService = new MuleDeploymentService(muleArtifactResourcesRegistry.getDomainFactory(), muleArtifactResourcesRegistry.getApplicationFactory());
        deploymentListener = mock(DeploymentListener.class);
        deploymentService.addDeploymentListener(deploymentListener);

        coreExtensionManager = new DefaultMuleCoreExtensionManagerServer(
                () -> coreExtensions,
                new ReflectionMuleCoreExtensionDependencyResolver());
        coreExtensionManager.setDeploymentService(deploymentService);
    }

    public void stop() throws MuleException
    {
        deploymentService.stop();
        serviceManager.stop();
        coreExtensionManager.stop();
        coreExtensionManager.dispose();
    }

    public void start() throws IOException, MuleException
    {
        coreExtensionManager.initialise();
        coreExtensionManager.start();
        serviceManager.start();
        deploymentService.start();
    }

    public void assertDeploymentSuccess(String appName)
    {
        assertDeploymentSuccess(deploymentListener, appName);
    }

    public void assertDeploymentFailure(String appName)
    {
        assertDeploymentFailure(deploymentListener, appName);
    }

    public void assertUndeploymentSuccess(String appName)
    {
        assertUndeploymentSuccess(deploymentListener, appName);
    }

    private void assertDeploymentFailure(final DeploymentListener listener, final String appName)
    {
        Prober prober = new PollingProber(DEPLOYMENT_TIMEOUT, 100);
        prober.check(new Probe()
        {
            public boolean isSatisfied()
            {
                try
                {
                    verify(listener, times(1)).onDeploymentFailure(eq(appName), any(Throwable.class));
                    return true;
                }
                catch (AssertionError e)
                {
                    return false;
                }
            }

            public String describeFailure()
            {
                return "Failed to deploy application: " + appName;
            }
        });
    }

    private void assertDeploymentSuccess(final DeploymentListener listener, final String appName)
    {
        Prober prober = new PollingProber(DEPLOYMENT_TIMEOUT, 100);
        prober.check(new Probe()
        {
            public boolean isSatisfied()
            {
                try
                {
                    verify(listener, times(1)).onDeploymentSuccess(appName);
                    return true;
                }
                catch (AssertionError e)
                {
                    return false;
                }
            }

            public String describeFailure()
            {
                return "Failed to deploy application: " + appName;
            }
        });
    }

    public void assertUndeploymentSuccess(final DeploymentListener listener, final String appName)
    {
        Prober prober = new PollingProber(DEPLOYMENT_TIMEOUT, 100);
        prober.check(new Probe()
        {
            public boolean isSatisfied()
            {
                try
                {
                    verify(listener, times(1)).onUndeploymentSuccess(appName);
                    return true;
                }
                catch (AssertionError e)
                {
                    return false;
                }
            }

            public String describeFailure()
            {
                return "Failed to deploy application: " + appName;
            }
        });
    }

    private void setMuleFolders() throws IOException
    {
        appsDir = createFolder("apps");
        logsDir = createFolder("logs");
        serverPluginsDir = createFolder("server-plugins");
        domainsDir = createFolder("domains");
        createFolder("domains/default");

        File confDir = createFolder("conf");
        URL log4jFile = getClass().getResource("/log4j2-test.xml");
        copyURLToFile(log4jFile, new File(confDir, "log4j2-test.xml"));

        createFolder("lib/shared/default");
    }

    private File createFolder(String folderName)
    {
        File folder = new File(getMuleHome(), folderName);

        if (!folder.exists())
        {
            if (!folder.mkdirs())
            {
                throw new IllegalStateException(String.format("Unable to create folder '%s'", folderName));
            }
        }

        return folder;
    }

    /**
     * Copies a given app archive to the apps folder for deployment.
     */
    public void addAppArchive(URL url) throws IOException
    {
        addAppArchive(url, null);
    }

    public void deploy(String resource) throws IOException
    {
        int lastSeparator = resource.lastIndexOf(File.separator);
        String appName = StringUtils.removeEndIgnoreCase(resource.substring(lastSeparator + 1), ".zip");
        deploy(resource, appName);
    }

    /**
     * Deploys an application from a classpath resource
     *
     * @param resource points to the resource to deploy. Non null.
     * @param targetAppName application name used to deploy the resource. Null to
     *                      maintain the original resource name
     * @throws IOException if the resource cannot be accessed
     */
    public void deploy(String resource, String targetAppName) throws IOException
    {
        URL url = getClass().getResource(resource);
        deploy(url, targetAppName);
    }

    /**
     * Deploys an application from an URL
     *
     * @param resource points to the resource to deploy. Non null.
     * @param targetAppName application name used to deploy the resource. Null to
     *                      maintain the original resource name
     * @throws IOException if the URL cannot be accessed
     */
    public void deploy(URL resource, String targetAppName) throws IOException
    {
        addAppArchive(resource, targetAppName + ".zip");
        assertDeploymentSuccess(targetAppName);
    }

    /**
     * Copies a given app archive with a given target name to the apps folder for deployment
     */
    private void addAppArchive(URL url, String targetFile) throws IOException
    {
        // copy is not atomic, copy to a temp file and rename instead (rename is atomic)
        final String tempFileName = new File((targetFile == null ? url.getFile() : targetFile) + ".part").getName();
        final File tempFile = new File(appsDir, tempFileName);
        copyURLToFile(url, tempFile);
        boolean renamed = tempFile.renameTo(new File(StringUtils.removeEnd(tempFile.getAbsolutePath(), ".part")));
        if (!renamed)
        {
            throw new IllegalStateException("Unable to add application archive");
        }
    }

    /**
     * Adds a server plugin file to the Mule server.
     *
     * @param plugin plugin file to add. Non null.
     * @throws IOException if the plugin file cannot be accessed
     */
    public void addZippedServerPlugin(File plugin) throws IOException
    {
        addZippedServerPlugin(plugin.toURI().toURL());
    }

    /**
     * Adds a server plugin to the Mule server .
     *
     * @param resource points to the plugin to add. Non null.
     * @throws IOException if the plugin URL cannot be accessed
     */
    public void addZippedServerPlugin(URL resource) throws IOException
    {
        String baseName = FilenameUtils.getName(resource.getPath());
        File tempFile = new File(getServerPluginsDir(), baseName);
        copyURLToFile(resource, tempFile);
    }

    public File getMuleHome()
    {
        return muleHome;
    }

    public File getLogsDir()
    {
        return logsDir;
    }

    public File getAppsDir()
    {
        return appsDir;
    }

    private File getDomainsDir()
    {
        return domainsDir;
    }

    public File getServerPluginsDir()
    {
        return serverPluginsDir;
    }

    public void resetDeploymentListener()
    {
        reset(deploymentListener);
    }

    public void addCoreExtension(MuleCoreExtension coreExtension)
    {
        coreExtension.setContainerClassLoader(containerClassLoader);
        coreExtensions.add(coreExtension);
    }

    public void addDeploymentListener(DeploymentListener listener)
    {
        deploymentService.addDeploymentListener(listener);
    }

    public void removeDeploymentListener(DeploymentListener listener)
    {
        deploymentService.removeDeploymentListener(listener);
    }

    /**
     * Finds deployed application by name.
     *
     * @return the application if found, null otherwise
     */
    public Application findApplication(String appName)
    {
        return deploymentService.findApplication(appName);
    }

    /**
     * Deploys a Domain from a classpath folder
     *
     * @param domainFolder folder in which the domain is defined
     * @param domainName name of the domain to use as domain artifact name
     */
    public void deployDomainFromClasspathFolder(String domainFolder, String domainName)
    {
        copyExplodedArtifactFromClasspathFolderToDeployFolder(domainFolder, getDomainsDir(), domainName);
    }

    /**
     * Deploys an Application from a classpath folder
     *
     * @param appFolder folder in which the app is defined
     * @param appName name of the domain to use as app artifact name
     */
    public void deployAppFromClasspathFolder(String appFolder, String appName)
    {
        copyExplodedArtifactFromClasspathFolderToDeployFolder(appFolder, getAppsDir(), appName);
    }

    private void copyExplodedArtifactFromClasspathFolderToDeployFolder(String artifactFolder, File artifactDirectory, String artifactName)
    {
        ReentrantLock lock = this.deploymentService.getLock();
        lock.lock();
        try
        {
            URL resource = getClass().getClassLoader().getResource(artifactFolder);
            FileUtils.copyDirectory(new File(resource.getFile()), new File(artifactDirectory, artifactName));
        }
        catch (IOException e)
        {
            throw new MuleRuntimeException(e);
        }
        finally
        {
            lock.unlock();
        }
    }
}
