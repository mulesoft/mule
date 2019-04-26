/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.launcher;

import static java.lang.System.getProperties;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.apache.commons.lang.StringUtils.isEmpty;
import static org.mule.api.config.MuleProperties.SYSTEM_PROPERTY_PREFIX;
import static org.mule.module.launcher.ArtifactDeploymentTemplate.NOP_ARTIFACT_DEPLOYMENT_TEMPLATE;
import static org.mule.module.launcher.DefaultArchiveDeployer.ZIP_FILE_SUFFIX;
import org.mule.config.StartupContext;
import org.mule.module.launcher.application.Application;
import org.mule.module.launcher.application.ApplicationClassLoaderFactory;
import org.mule.module.launcher.application.ApplicationFactory;
import org.mule.module.launcher.application.CompositeApplicationClassLoaderFactory;
import org.mule.module.launcher.application.DefaultApplicationFactory;
import org.mule.module.launcher.application.MuleApplicationClassLoaderFactory;
import org.mule.module.launcher.artifact.ShutdownListener;
import org.mule.module.launcher.domain.DefaultDomainFactory;
import org.mule.module.launcher.domain.Domain;
import org.mule.module.launcher.domain.DomainClassLoaderRepository;
import org.mule.module.launcher.domain.DomainFactory;
import org.mule.module.launcher.domain.MuleDomainClassLoaderRepository;
import org.mule.module.launcher.nativelib.DefaultNativeLibraryFinderFactory;
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
    public static final String PARALLEL_DEPLOYMENT_PROPERTY = SYSTEM_PROPERTY_PREFIX + "deployment.parallel";

    protected transient final Log logger = LogFactory.getLog(getClass());
    // fair lock
    private final ReentrantLock deploymentLock = new DebuggableReentrantLock(true);

    private final ObservableList<Application> applications = new ObservableList<Application>();
    private final ObservableList<Domain> domains = new ObservableList<Domain>();
    private final List<StartupListener> startupListeners = new ArrayList<StartupListener>();
    private final List<ShutdownListener> shutdownListeners = new ArrayList<ShutdownListener>();

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

        ApplicationClassLoaderFactory applicationClassLoaderFactory = new MuleApplicationClassLoaderFactory(domainClassLoaderRepository, new DefaultNativeLibraryFinderFactory());
        applicationClassLoaderFactory = new CompositeApplicationClassLoaderFactory(applicationClassLoaderFactory, pluginClassLoaderManager);

        DefaultDomainFactory domainFactory = new DefaultDomainFactory(domainClassLoaderRepository);
        domainFactory.setDeploymentListener(domainDeploymentListener);
        DefaultApplicationFactory applicationFactory = new DefaultApplicationFactory(applicationClassLoaderFactory, domainFactory);
        applicationFactory.setDeploymentListener(applicationDeploymentListener);

        ArtifactDeployer<Application> applicationMuleDeployer = new DefaultArtifactDeployer<Application>();

        this.applicationDeployer = new DefaultArchiveDeployer<>(applicationMuleDeployer, applicationFactory, applications, NOP_ARTIFACT_DEPLOYMENT_TEMPLATE);
        this.applicationDeployer.setDeploymentListener(applicationDeploymentListener);
        this.domainDeployer = createDomainArchiveDeployer(domainFactory, domains, applicationDeployer);
        this.domainDeployer.setDeploymentListener(domainDeploymentListener);
        if (useParallelDeployment())
        {
            if (isDeployingSelectedAppsInOrder())
            {
                throw new IllegalArgumentException("Deployment parameters 'app' and '" + PARALLEL_DEPLOYMENT_PROPERTY + "' cannot be used together");
            }
            logger.info("Using parallel deployment");
            this.deploymentDirectoryWatcher = new ParallelDeploymentDirectoryWatcher(domainDeployer, applicationDeployer, domains, applications, deploymentLock);
        }
        else
        {
            this.deploymentDirectoryWatcher = new DeploymentDirectoryWatcher(domainDeployer, applicationDeployer, domains, applications, deploymentLock);
        }
    }

    private boolean useParallelDeployment()
    {
        return getProperties().containsKey(PARALLEL_DEPLOYMENT_PROPERTY);
    }

    private boolean isDeployingSelectedAppsInOrder()
    {
        final Map<String, Object> options = StartupContext.get().getStartupOptions();
        String appString = (String) options.get("app");

        return !isEmpty(appString);
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
        for (ShutdownListener listener : shutdownListeners)
        {
            try
            {
                listener.execute();
            }
            catch (Throwable t)
            {
                logger.error(t);
            }
        }

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
    public void undeploy(final String appName)
    {
        executeSynchronized(new SynchronizedDeploymentAction()
        {
            @Override
            public void execute()
            {
                applicationDeployer.undeployArtifact(appName);
            }
        });
    }

    @Override
    public void deploy(final URL appArchiveUrl) throws IOException
    {
        executeSynchronized(new SynchronizedDeploymentAction()
        {
            @Override
            public void execute()
            {
                applicationDeployer.deployPackagedArtifact(appArchiveUrl);
            }
        });
    }

    @Override
    public void redeploy(final String artifactName)
    {
        executeSynchronized(new SynchronizedDeploymentAction()
        {
            @Override
            public void execute()
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
        });
    }

    @Override
    public void undeployDomain(final String domainName)
    {
        executeSynchronized(new SynchronizedDeploymentAction()
        {
            @Override
            public void execute()
            {
                domainDeployer.undeployArtifact(domainName);
            }
        });
    }

    @Override
    public void deployDomain(final URL domainArchiveUrl) throws IOException
    {
        executeSynchronized(new SynchronizedDeploymentAction()
        {
            @Override
            public void execute()
            {
                domainDeployer.deployPackagedArtifact(domainArchiveUrl);
            }
        });
    }

    @Override
    public void redeployDomain(final String domainName)
    {
        executeSynchronized(new SynchronizedDeploymentAction()
        {
            @Override
            public void execute()
            {
                domainDeployer.redeploy(findDomain(domainName));
            }
        });
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
    public void addShutdownListener(ShutdownListener listener)
    {
        this.shutdownListeners.add(listener);
    }

    @Override
    public void removeShutdownListener(ShutdownListener listener)
    {
        this.removeShutdownListener(listener);
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

    private interface SynchronizedDeploymentAction
    {
        void execute();
    }

    private void executeSynchronized(SynchronizedDeploymentAction deploymentAction)
    {
        try
        {
            if (!deploymentLock.tryLock(0, SECONDS))
            {
                if (logger.isDebugEnabled())
                {
                    logger.debug("Another deployment operation in progress, will skip this cycle. Owner thread: " +
                                 (deploymentLock instanceof DebuggableReentrantLock ? ((DebuggableReentrantLock) deploymentLock).getOwner() : "Unknown"));
                }
                return;
            }
            deploymentAction.execute();
        }
        catch (InterruptedException e)
        {
            e.printStackTrace();
        }
        finally
        {
            if (deploymentLock.isHeldByCurrentThread())
            {
                deploymentLock.unlock();
            }
        }
    }

    protected DomainArchiveDeployer createDomainArchiveDeployer(DomainFactory domainFactory, ObservableList<Domain> domains, DefaultArchiveDeployer<Application> applicationDeployer)
    {
        return new DomainArchiveDeployer(
                new DefaultArchiveDeployer<>(new DefaultArtifactDeployer<Domain>(), domainFactory, domains,
                                             new DomainDeploymentTemplate(applicationDeployer, this)),
                applicationDeployer, this);

    }
}
