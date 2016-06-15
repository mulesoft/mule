/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.launcher;

import static org.mule.runtime.module.launcher.ArtifactDeploymentTemplate.NOP_ARTIFACT_DEPLOYMENT_TEMPLATE;
import static org.mule.runtime.module.launcher.DefaultArchiveDeployer.ZIP_FILE_SUFFIX;
import org.mule.runtime.core.util.Preconditions;
import org.mule.runtime.module.artifact.classloader.ArtifactClassLoader;
import org.mule.runtime.module.artifact.classloader.ArtifactClassLoaderFactory;
import org.mule.runtime.module.artifact.classloader.DefaultArtifactClassLoaderFilterFactory;
import org.mule.runtime.module.launcher.application.Application;
import org.mule.runtime.module.launcher.application.ApplicationPluginClassLoaderFactory;
import org.mule.runtime.module.launcher.application.ApplicationPluginFactory;
import org.mule.runtime.module.launcher.application.DefaultApplicationFactory;
import org.mule.runtime.module.launcher.application.DefaultApplicationPluginFactory;
import org.mule.runtime.module.launcher.application.MuleApplicationClassLoaderFactory;
import org.mule.runtime.module.launcher.artifact.ArtifactFactory;
import org.mule.runtime.module.launcher.descriptor.DomainDescriptor;
import org.mule.runtime.module.launcher.domain.DefaultDomainFactory;
import org.mule.runtime.module.launcher.domain.DefaultDomainManager;
import org.mule.runtime.module.launcher.domain.Domain;
import org.mule.runtime.module.launcher.domain.DomainClassLoaderFactory;
import org.mule.runtime.module.launcher.domain.DomainFactory;
import org.mule.runtime.module.launcher.domain.DomainManager;
import org.mule.runtime.module.launcher.nativelib.DefaultNativeLibraryFinderFactory;
import org.mule.runtime.module.launcher.plugin.ApplicationPluginDescriptorFactory;
import org.mule.runtime.module.launcher.plugin.ApplicationPluginRepository;
import org.mule.runtime.module.launcher.plugin.DefaultApplicationPluginRepository;
import org.mule.runtime.module.launcher.util.DebuggableReentrantLock;
import org.mule.runtime.module.launcher.util.ObservableList;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;
import org.apache.commons.io.filefilter.AndFileFilter;
import org.apache.commons.io.filefilter.FileFileFilter;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.filefilter.SuffixFileFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MuleDeploymentService implements DeploymentService
{

    public static final String ARTIFACT_ANCHOR_SUFFIX = "-anchor.txt";
    public static final IOFileFilter ZIP_ARTIFACT_FILTER = new AndFileFilter(new SuffixFileFilter(ZIP_FILE_SUFFIX), FileFileFilter.FILE);

    protected transient final Logger logger = LoggerFactory.getLogger(getClass());
    // fair lock
    private final ReentrantLock deploymentLock = new DebuggableReentrantLock(true);

    private final ObservableList<Application> applications = new ObservableList<>();
    private final ObservableList<Domain> domains = new ObservableList<>();
    private final List<StartupListener> startupListeners = new ArrayList<>();

    /**
     * TODO: move to setter as in previous version.
     */
    private final CompositeDeploymentListener applicationDeploymentListener = new CompositeDeploymentListener();
    private final CompositeDeploymentListener domainDeploymentListener = new CompositeDeploymentListener();
    private final ArchiveDeployer<Domain> domainDeployer;
    private final DeploymentDirectoryWatcher deploymentDirectoryWatcher;
    private DefaultArchiveDeployer<Application> applicationDeployer;
    private final DomainManager domainManager = new DefaultDomainManager();
    private final ApplicationPluginRepository applicationPluginRepository;

    public MuleDeploymentService(ArtifactClassLoader containerClassLoader)
    {
        ArtifactClassLoaderFactory<DomainDescriptor> domainClassLoaderFactory = new DomainClassLoaderFactory(containerClassLoader.getClassLoader());

        ArtifactClassLoaderFactory applicationClassLoaderFactory = new MuleApplicationClassLoaderFactory(new DefaultNativeLibraryFinderFactory());

        DefaultDomainFactory domainFactory = new DefaultDomainFactory(domainClassLoaderFactory, domainManager, containerClassLoader);
        domainFactory.setDeploymentListener(domainDeploymentListener);

        final ApplicationPluginFactory applicationPluginFactory = new DefaultApplicationPluginFactory(new ApplicationPluginClassLoaderFactory());
        final ApplicationPluginDescriptorFactory applicationPluginDescriptorFactory = new ApplicationPluginDescriptorFactory(new DefaultArtifactClassLoaderFilterFactory());
        applicationPluginRepository = new DefaultApplicationPluginRepository(applicationPluginDescriptorFactory);

        final ApplicationDescriptorFactory applicationDescriptorFactory = new ApplicationDescriptorFactory(applicationPluginDescriptorFactory, applicationPluginRepository);

        DefaultApplicationFactory applicationFactory = new DefaultApplicationFactory(applicationClassLoaderFactory, applicationDescriptorFactory, applicationPluginFactory, domainManager, applicationPluginRepository);
        applicationFactory.setDeploymentListener(applicationDeploymentListener);

        ArtifactDeployer<Application> applicationMuleDeployer = new DefaultArtifactDeployer<>();
        ArtifactDeployer<Domain> domainMuleDeployer = new DefaultArtifactDeployer<>();

        this.applicationDeployer = new DefaultArchiveDeployer<>(applicationMuleDeployer, applicationFactory, applications, deploymentLock, NOP_ARTIFACT_DEPLOYMENT_TEMPLATE);
        this.applicationDeployer.setDeploymentListener(applicationDeploymentListener);
        this.domainDeployer = new DomainArchiveDeployer(
                new DefaultArchiveDeployer<>(domainMuleDeployer, domainFactory, domains, deploymentLock,
                        new DomainDeploymentTemplate(applicationDeployer, this)),
                applicationDeployer, this);
        this.domainDeployer.setDeploymentListener(domainDeploymentListener);
        this.deploymentDirectoryWatcher = new DeploymentDirectoryWatcher(domainDeployer, applicationDeployer, domains, applications, deploymentLock);
    }

    @Override
    public void start()
    {
        DeploymentStatusTracker deploymentStatusTracker = new DeploymentStatusTracker();
        addDeploymentListener(deploymentStatusTracker.getApplicationDeploymentStatusTracker());
        addDomainDeploymentListener(deploymentStatusTracker.getDomainDeploymentStatusTracker());

        StartupSummaryDeploymentListener summaryDeploymentListener = new StartupSummaryDeploymentListener(deploymentStatusTracker, this);
        addStartupListener(summaryDeploymentListener);

        deploymentDirectoryWatcher.start();

        for (StartupListener listener : startupListeners)
        {
            try
            {
                listener.onAfterStartup();
            }
            catch (Throwable t)
            {
                logger.error("Error executing startup listener {}", listener, t);
            }
        }
    }

    @Override
    public void stop()
    {
        deploymentDirectoryWatcher.stop();
    }

    @Override
    public Domain findDomain(String domainName)
    {
        return deploymentDirectoryWatcher.findArtifact(domainName, domains);
    }

    @Override
    public Application findApplication(String appName)
    {
        return deploymentDirectoryWatcher.findArtifact(appName, applications);
    }

    @Override
    public Collection<Application> findDomainApplications(final String domain)
    {
        Preconditions.checkArgument(domain != null, "Domain name cannot be null");
        return CollectionUtils.select(applications, new Predicate()
        {
            @Override
            public boolean evaluate(Object object)
            {
                return ((Application) object).getDomain().getArtifactName().equals(domain);
            }
        });
    }


    @Override
    public List<Application> getApplications()
    {
        return Collections.unmodifiableList(applications);
    }

    @Override
    public List<Domain> getDomains()
    {
        return Collections.unmodifiableList(domains);
    }

    /**
     * @return URL/lastModified of apps which previously failed to deploy
     */
    Map<URL, Long> getZombieApplications()
    {
        return applicationDeployer.getArtifactsZombieMap();
    }

    Map<URL, Long> getZombieDomains()
    {
        return domainDeployer.getArtifactsZombieMap();
    }

    public void setAppFactory(ArtifactFactory<Application> appFactory)
    {
        this.applicationDeployer.setArtifactFactory(appFactory);
    }

    @Override
    public ReentrantLock getLock()
    {
        return deploymentLock;
    }

    @Override
    public void undeploy(String appName)
    {
        applicationDeployer.undeployArtifact(appName);
    }

    @Override
    public void deploy(URL appArchiveUrl) throws IOException
    {
        applicationDeployer.deployPackagedArtifact(appArchiveUrl);
    }

    @Override
    public void redeploy(String artifactName)
    {
        try
        {
            applicationDeployer.redeploy(findApplication(artifactName));
        }
        catch (DeploymentException e)
        {
            if (logger.isDebugEnabled())
            {
                logger.debug("Failure while redeploying application: " + artifactName, e);
            }
        }
    }

    @Override
    public void undeployDomain(String domainName)
    {
        domainDeployer.undeployArtifact(domainName);
    }

    @Override
    public void deployDomain(URL domainArchiveUrl) throws IOException
    {
        domainDeployer.deployPackagedArtifact(domainArchiveUrl);
    }

    @Override
    public void redeployDomain(String domainName)
    {
        domainDeployer.redeploy(findDomain(domainName));
    }

    @Override
    public void addStartupListener(StartupListener listener)
    {
        this.startupListeners.add(listener);
    }

    @Override
    public void removeStartupListener(StartupListener listener)
    {
        this.startupListeners.remove(listener);
    }

    @Override
    public void addDeploymentListener(DeploymentListener listener)
    {
        applicationDeploymentListener.addDeploymentListener(listener);
    }

    @Override
    public void removeDeploymentListener(DeploymentListener listener)
    {
        applicationDeploymentListener.removeDeploymentListener(listener);
    }

    @Override
    public void addDomainDeploymentListener(DeploymentListener listener)
    {
        domainDeploymentListener.addDeploymentListener(listener);
    }

    @Override
    public void removeDomainDeploymentListener(DeploymentListener listener)
    {
        domainDeploymentListener.removeDeploymentListener(listener);
    }

    public void setDomainFactory(DomainFactory domainFactory)
    {
        this.domainDeployer.setArtifactFactory(domainFactory);
    }

    void undeploy(Application app)
    {
        applicationDeployer.undeployArtifact(app.getArtifactName());
    }

    void undeploy(Domain domain)
    {
        domainDeployer.undeployArtifact(domain.getArtifactName());
    }

}
