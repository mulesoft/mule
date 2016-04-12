/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.launcher;

import static org.mule.module.launcher.ArtifactDeploymentTemplate.NOP_ARTIFACT_DEPLOYMENT_TEMPLATE;
import static org.mule.module.launcher.DefaultArchiveDeployer.ZIP_FILE_SUFFIX;
import org.mule.module.artifact.classloader.ArtifactClassLoaderFactory;
import org.mule.module.artifact.classloader.ArtifactClassLoaderFilterFactory;
import org.mule.module.artifact.classloader.MuleClassLoaderLookupPolicy;
import org.mule.module.launcher.application.Application;
import org.mule.module.launcher.application.CompositeApplicationClassLoaderFactory;
import org.mule.module.launcher.application.DefaultApplicationFactory;
import org.mule.module.launcher.application.MuleApplicationClassLoaderFactory;
import org.mule.module.launcher.artifact.ArtifactFactory;
import org.mule.module.launcher.descriptor.DomainDescriptor;
import org.mule.module.launcher.domain.DefaultDomainFactory;
import org.mule.module.launcher.domain.DefaultDomainManager;
import org.mule.module.launcher.domain.Domain;
import org.mule.module.launcher.domain.DomainClassLoaderFactory;
import org.mule.module.launcher.domain.DomainFactory;
import org.mule.module.launcher.domain.DomainManager;
import org.mule.module.launcher.nativelib.DefaultNativeLibraryFinderFactory;
import org.mule.module.launcher.plugin.ApplicationPluginDescriptorFactory;
import org.mule.module.launcher.util.DebuggableReentrantLock;
import org.mule.module.launcher.util.ObservableList;
import org.mule.util.Preconditions;

import com.google.common.collect.ImmutableSet;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;
import org.apache.commons.io.filefilter.AndFileFilter;
import org.apache.commons.io.filefilter.FileFileFilter;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.filefilter.SuffixFileFilter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class MuleDeploymentService implements DeploymentService
{

    //TODO(pablo.kraan): MULE-9524: Add a way to configure system packages used on class loading lookup
    public static final Set<String> SYSTEM_PACKAGES = ImmutableSet.of(
            "java.", "org.mule", "com.mulesoft.mule.config", "com.mulesoft.mule.bti", "com.mulesoft.mule.cache",
            "com.mulesoft.mule.cluster", "com.mulesoft.mule.plugin", "com.mulesoft.mule.saml", "com.mulesoft.mule.throttling",
            "com.mulesoft.mule.tracking", "com.mulesoft.mule.transaction", "com.mulesoft.module",
            "org.apache.logging.log4j", "org.slf4j", "org.apache.commons.logging", "org.apache.log4j"
    );
    public static final String ARTIFACT_ANCHOR_SUFFIX = "-anchor.txt";
    public static final IOFileFilter ZIP_ARTIFACT_FILTER = new AndFileFilter(new SuffixFileFilter(ZIP_FILE_SUFFIX), FileFileFilter.FILE);

    protected transient final Log logger = LogFactory.getLog(getClass());
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

    public MuleDeploymentService(ServerPluginClassLoaderManager serverPluginClassLoaderManager)
    {
        final MuleClassLoaderLookupPolicy containerLookupPolicy = new MuleClassLoaderLookupPolicy(Collections.emptyMap(), SYSTEM_PACKAGES);

        ArtifactClassLoaderFactory<DomainDescriptor> domainClassLoaderFactory = new DomainClassLoaderFactory(containerLookupPolicy);

        ArtifactClassLoaderFactory applicationClassLoaderFactory = new MuleApplicationClassLoaderFactory(new DefaultNativeLibraryFinderFactory());
        applicationClassLoaderFactory = new CompositeApplicationClassLoaderFactory(applicationClassLoaderFactory, serverPluginClassLoaderManager);

        DefaultDomainFactory domainFactory = new DefaultDomainFactory(domainClassLoaderFactory, domainManager);
        domainFactory.setDeploymentListener(domainDeploymentListener);

        final ApplicationDescriptorFactory applicationDescriptorFactory = new ApplicationDescriptorFactory(new ApplicationPluginDescriptorFactory(new ArtifactClassLoaderFilterFactory()));
        DefaultApplicationFactory applicationFactory = new DefaultApplicationFactory(applicationClassLoaderFactory, applicationDescriptorFactory, domainManager);
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
                logger.error(t);
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
