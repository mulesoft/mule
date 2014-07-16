/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.launcher;

import static org.mule.module.launcher.DefaultArchiveDeployer.ZIP_FILE_SUFFIX;

import org.mule.module.launcher.application.Application;
import org.mule.module.launcher.application.ApplicationClassLoaderFactory;
import org.mule.module.launcher.application.ApplicationFactory;
import org.mule.module.launcher.application.CompositeApplicationClassLoaderFactory;
import org.mule.module.launcher.application.DefaultApplicationFactory;
import org.mule.module.launcher.application.MuleApplicationClassLoaderFactory;
import org.mule.module.launcher.domain.DefaultDomainFactory;
import org.mule.module.launcher.domain.Domain;
import org.mule.module.launcher.domain.DomainClassLoaderRepository;
import org.mule.module.launcher.domain.DomainFactory;
import org.mule.module.launcher.domain.MuleDomainClassLoaderRepository;
import org.mule.module.launcher.util.DebuggableReentrantLock;
import org.mule.module.launcher.util.ObservableList;
import org.mule.util.Preconditions;

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
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class MuleDeploymentService implements DeploymentService
{

    public static final String ARTIFACT_ANCHOR_SUFFIX = "-anchor.txt";
    public static final IOFileFilter ZIP_ARTIFACT_FILTER = new AndFileFilter(new SuffixFileFilter(ZIP_FILE_SUFFIX), FileFileFilter.FILE);

    protected transient final Log logger = LogFactory.getLog(getClass());
    // fair lock
    private final ReentrantLock deploymentLock = new DebuggableReentrantLock(true);

    private final ObservableList<Application> applications = new ObservableList<Application>();
    private final ObservableList<Domain> domains = new ObservableList<Domain>();
    private final List<StartupListener> startupListeners = new ArrayList<StartupListener>();

    /**
     * TODO: move to setter as in previous version.
     */
    private final CompositeDeploymentListener applicationDeploymentListener = new CompositeDeploymentListener();
    private final CompositeDeploymentListener domainDeploymentListener = new CompositeDeploymentListener();
    private final ArchiveDeployer<Domain> domainDeployer;
    private final DeploymentDirectoryWatcher deploymentDirectoryWatcher;
    private DefaultArchiveDeployer<Application> applicationDeployer;

    public MuleDeploymentService(PluginClassLoaderManager pluginClassLoaderManager)
    {
        DomainClassLoaderRepository domainClassLoaderRepository = new MuleDomainClassLoaderRepository();

        ApplicationClassLoaderFactory applicationClassLoaderFactory = new MuleApplicationClassLoaderFactory(domainClassLoaderRepository);
        applicationClassLoaderFactory = new CompositeApplicationClassLoaderFactory(applicationClassLoaderFactory, pluginClassLoaderManager);

        DefaultDomainFactory domainFactory = new DefaultDomainFactory(domainClassLoaderRepository);
        domainFactory.setDeploymentListener(domainDeploymentListener);
        DefaultApplicationFactory applicationFactory = new DefaultApplicationFactory(applicationClassLoaderFactory, domainFactory);
        applicationFactory.setDeploymentListener(applicationDeploymentListener);

        ArtifactDeployer<Application> applicationMuleDeployer = new DefaultArtifactDeployer<Application>();
        ArtifactDeployer<Domain> domainMuleDeployer = new DefaultArtifactDeployer<Domain>();

        this.applicationDeployer = new DefaultArchiveDeployer(applicationMuleDeployer, applicationFactory, applications, deploymentLock);
        this.applicationDeployer.setDeploymentListener(applicationDeploymentListener);
        this.domainDeployer = new DomainArchiveDeployer(new DefaultArchiveDeployer(domainMuleDeployer, domainFactory, domains, deploymentLock), applicationDeployer, applicationMuleDeployer, this);
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

    public Collection<Application> findDomainApplications(final String domain)
    {
        Preconditions.checkArgument(domain != null, "Domain name cannot be null");
        return (Collection<Application>) CollectionUtils.select(applications, new Predicate()
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

    public void setAppFactory(ApplicationFactory appFactory)
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
