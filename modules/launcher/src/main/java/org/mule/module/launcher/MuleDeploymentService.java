/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.launcher;

import static org.mule.module.launcher.ArtifactDeployer.ARTIFACT_NAME_PROPERTY;
import static org.mule.util.SplashScreen.miniSplash;

import org.mule.config.StartupContext;
import org.mule.module.launcher.application.Application;
import org.mule.module.launcher.application.ApplicationClassLoaderFactory;
import org.mule.module.launcher.application.ApplicationFactory;
import org.mule.module.launcher.application.CompositeApplicationClassLoaderFactory;
import org.mule.module.launcher.application.DefaultApplicationFactory;
import org.mule.module.launcher.application.MuleApplicationClassLoaderFactory;
import org.mule.module.launcher.artifact.Artifact;
import org.mule.module.launcher.domain.DefaultDomainFactory;
import org.mule.module.launcher.domain.Domain;
import org.mule.module.launcher.domain.DomainClassLoaderFactory;
import org.mule.module.launcher.domain.DomainFactory;
import org.mule.module.launcher.domain.MuleDomainClassLoaderFactory;
import org.mule.module.launcher.util.DebuggableReentrantLock;
import org.mule.module.launcher.util.ElementAddedEvent;
import org.mule.module.launcher.util.ElementRemovedEvent;
import org.mule.module.launcher.util.ObservableList;
import org.mule.module.reboot.MuleContainerBootstrapUtils;
import org.mule.util.ArrayUtils;
import org.mule.util.CollectionUtils;
import org.mule.util.StringUtils;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.beanutils.BeanPropertyValueEqualsPredicate;
import org.apache.commons.io.filefilter.AndFileFilter;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.io.filefilter.FileFileFilter;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.filefilter.SuffixFileFilter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class MuleDeploymentService implements DeploymentService
{

    public static final String ARTIFACT_ANCHOR_SUFFIX = "-anchor.txt";
    public static final String ZIP_FILE_SUFFIX = ".zip";
    public static final IOFileFilter ZIP_ARTIFACT_FILTER = new AndFileFilter(new SuffixFileFilter(ZIP_FILE_SUFFIX), FileFileFilter.FILE);

    protected static final int DEFAULT_CHANGES_CHECK_INTERVAL_MS = 5000;

    public static final String CHANGE_CHECK_INTERVAL_PROPERTY = "mule.launcher.changeCheckInterval";

    protected ScheduledExecutorService artifactDirMonitorTimer;

    protected transient final Log logger = LogFactory.getLog(getClass());
    // fair lock
    private final ReentrantLock deploymentInProgressLock = new DebuggableReentrantLock(true);

    private final ObservableList<Application> applications = new ObservableList<Application>();
    private final ObservableList<Domain> domains = new ObservableList<Domain>();
    private final ApplicationTimestampListener applicationTimestampListener;
    private final File appsDir = MuleContainerBootstrapUtils.getMuleAppsDir();
    private final File domainsDir = MuleContainerBootstrapUtils.getMuleDomainsDir();

    private final List<StartupListener> startupListeners = new ArrayList<StartupListener>();

    private final CompositeDeploymentListener applicationDeploymentListener = new CompositeDeploymentListener();
    private final CompositeDeploymentListener domainDeploymentListener = new CompositeDeploymentListener();
    private final ArtifactDeployer<Application> applicationDeployer;
    private final ArtifactDeployer<Domain> domainDeployer;

    public MuleDeploymentService(PluginClassLoaderManager pluginClassLoaderManager)
    {
        DomainClassLoaderFactory domainClassLoaderFactory = new MuleDomainClassLoaderFactory();

        ApplicationClassLoaderFactory applicationClassLoaderFactory = new MuleApplicationClassLoaderFactory(domainClassLoaderFactory);
        applicationClassLoaderFactory = new CompositeApplicationClassLoaderFactory(applicationClassLoaderFactory, pluginClassLoaderManager);

        DefaultDomainFactory domainFactory = new DefaultDomainFactory(domainClassLoaderFactory);
        domainFactory.setDeploymentListener(domainDeploymentListener);
        DefaultApplicationFactory applicationFactory = new DefaultApplicationFactory(applicationClassLoaderFactory, domainFactory);
        applicationFactory.setDeploymentListener(applicationDeploymentListener);

        DefaultMuleDeployer<Application> applicationMuleDeployer = new DefaultMuleDeployer<Application>();
        applicationMuleDeployer.setArtifactFactory(applicationFactory);

        DefaultMuleDeployer<Domain> domainMuleDeployer = new DefaultMuleDeployer<Domain>();
        domainMuleDeployer.setArtifactFactory(domainFactory);

        this.applicationTimestampListener = new ApplicationTimestampListener(applications);

        this.applicationDeployer = new ArtifactDeployer(applicationDeploymentListener, applicationMuleDeployer, applicationFactory, applications, deploymentInProgressLock);
        this.domainDeployer = new ArtifactDeployer(domainDeploymentListener, domainMuleDeployer, domainFactory, domains, deploymentInProgressLock);
    }

    @Override
    public void start()
    {
        DeploymentStatusTracker deploymentStatusTracker = new DeploymentStatusTracker();
        addDeploymentListener(deploymentStatusTracker);

        StartupSummaryDeploymentListener summaryDeploymentListener = new StartupSummaryDeploymentListener(deploymentStatusTracker);
        addStartupListener(summaryDeploymentListener);

        deleteAllAnchors();

        // mule -app app1:app2:app3 will restrict deployment only to those specified apps
        final Map<String, Object> options = StartupContext.get().getStartupOptions();
        String appString = (String) options.get("app");

        String[] explodedDomains = domainsDir.list(DirectoryFileFilter.DIRECTORY);
        String[] packagedDomains = domainsDir.list(ZIP_ARTIFACT_FILTER);

        deployPackedDomains(packagedDomains);
        deployExplodedDomains(explodedDomains);

        if (appString == null)
        {
            String[] explodedApps = appsDir.list(DirectoryFileFilter.DIRECTORY);
            String[] packagedApps = appsDir.list(ZIP_ARTIFACT_FILTER);

            deployPackedApps(packagedApps);
            deployExplodedApps(explodedApps);
        }
        else
        {
            String[] apps = appString.split(":");
            apps = removeDuplicateAppNames(apps);

            for (String app : apps)
            {
                try
                {
                    File applicationFile = new File(appsDir, app + ZIP_FILE_SUFFIX);

                    if (applicationFile.exists() && applicationFile.isFile())
                    {
                        applicationDeployer.deployPackagedArtifact(app + ZIP_FILE_SUFFIX);
                    }
                    else
                    {
                        applicationDeployer.deployExplodedArtifact(app);
                    }
                }
                catch (Exception e)
                {
                    // Ignore and continue
                }
            }
        }

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

        // only start the monitor thread if we launched in default mode without explicitly
        // stated applications to launch
        if (!(appString != null))
        {
            scheduleChangeMonitor(appsDir, domainsDir);
        }
        else
        {
            if (logger.isInfoEnabled())
            {
                logger.info(miniSplash("Mule is up and running in a fixed app set mode"));
            }
        }
    }

    private void deployExplodedDomains(String[] domains)
    {
        for (String addedApp : domains)
        {
            try
            {
                domainDeployer.deployExplodedArtifact(addedApp);
            }
            catch (DeploymentException e)
            {
                // Ignore and continue
            }
        }
    }

    private void deployPackedDomains(String[] zips)
    {
        for (String zip : zips)
        {
            try
            {
                domainDeployer.deployPackagedArtifact(zip);
            }
            catch (Exception e)
            {
                // Ignore and continue
            }
        }
    }

    private void deleteAllAnchors()
    {
        deleteAnchorsFromDirectory(domainsDir);
        deleteAnchorsFromDirectory(appsDir);
    }

    private void deleteAnchorsFromDirectory(final File directory)
    {
        // Deletes any leftover anchor files from previous shutdowns
        String[] anchors = directory.list(new SuffixFileFilter(ARTIFACT_ANCHOR_SUFFIX));
        for (String anchor : anchors)
        {
            // ignore result
            new File(directory, anchor).delete();
        }
    }

    private String[] removeDuplicateAppNames(String[] apps)
    {
        List<String> appNames = new LinkedList<String>();

        for (String appName : apps)
        {
            if (!appNames.contains(appName))
            {
                appNames.add(appName);
            }
        }

        return appNames.toArray(ArrayUtils.EMPTY_STRING_ARRAY);
    }

    protected void scheduleChangeMonitor(File appsDir, File domainsDir)
    {
        final int reloadIntervalMs = getChangesCheckIntervalMs();
        artifactDirMonitorTimer = Executors.newSingleThreadScheduledExecutor(new ArtifactDeployerMonitorThreadFactory());

        artifactDirMonitorTimer.scheduleWithFixedDelay(new ArtifactDirWatcher(appsDir, domainsDir),
                                                       0,
                                                       reloadIntervalMs,
                                                       TimeUnit.MILLISECONDS);

        if (logger.isInfoEnabled())
        {
            logger.info(miniSplash(String.format("Mule is up and kicking (every %dms)", reloadIntervalMs)));
        }
    }

    public static int getChangesCheckIntervalMs()
    {
        try
        {
            String value = System.getProperty(CHANGE_CHECK_INTERVAL_PROPERTY);
            return Integer.parseInt(value);
        }
        catch (NumberFormatException e)
        {
            return DEFAULT_CHANGES_CHECK_INTERVAL_MS;
        }
    }

    @Override
    public void stop()
    {
        stopAppDirMonitorTimer();

        deploymentInProgressLock.lock();
        try
        {
            // tear down apps in reverse order
            Collections.reverse(applications);

            for (Artifact artifact : applications)
            {
                try
                {
                    artifact.stop();
                    artifact.dispose();
                }
                catch (Throwable t)
                {
                    logger.error(t);
                }
            }

            // tear down domains in reverse order
            Collections.reverse(domains);

            for (Artifact artifact : domains)
            {
                try
                {
                    artifact.stop();
                    artifact.dispose();
                }
                catch (Throwable t)
                {
                    logger.error(t);
                }
            }
        }
        finally
        {
            deploymentInProgressLock.unlock();
        }
    }

    private void stopAppDirMonitorTimer()
    {
        if (artifactDirMonitorTimer != null)
        {
            artifactDirMonitorTimer.shutdown();
            try
            {
                artifactDirMonitorTimer.awaitTermination(getChangesCheckIntervalMs(), TimeUnit.MILLISECONDS);
            }
            catch (InterruptedException e)
            {
                throw new RuntimeException(e);
            }
        }
    }

    public Domain findDomain(String domainName)
    {
        return findArtifact(domainName, domains);
    }

    @Override
    public Application findApplication(String appName)
    {
        return findArtifact(appName, applications);
    }

    private <T extends Artifact> T findArtifact(String artifactName, ObservableList<T> artifacts)
    {
        return (T) CollectionUtils.find(artifacts, new BeanPropertyValueEqualsPredicate(ARTIFACT_NAME_PROPERTY, artifactName));
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
    public Map<URL, Long> getApplicationsZombieMap()
    {
        return applicationDeployer.getArtifactsZombieMap();
    }

    public Map<URL, Long> getDomainsZombieMap()
    {
        return domainDeployer.getArtifactsZombieMap();
    }

    protected MuleDeployer getDeployer()
    {
        return applicationDeployer.getDeployer();
    }

    public void setAppFactory(ApplicationFactory appFactory)
    {
        this.applicationDeployer.setArtifactFactory(appFactory);
    }

    public void setDeployer(MuleDeployer deployer)
    {
        this.applicationDeployer.setDeployer(deployer);
    }

    public ApplicationFactory getAppFactory()
    {
        //Cast required to maintain backward compatibility.
        return (ApplicationFactory) applicationDeployer.getArtifactFactory();
    }

    @Override
    public ReentrantLock getLock()
    {
        return deploymentInProgressLock;
    }

    protected void undeploy(Application app)
    {
        applicationDeployer.undeploy(app);
    }

    public void undeployDomain(String appName)
    {
        domainDeployer.undeploy(appName);
    }

    @Override
    public void undeploy(String appName)
    {
        applicationDeployer.undeploy(appName);
    }

    @Override
    public void deploy(URL appArchiveUrl) throws IOException
    {
        applicationDeployer.deploy(appArchiveUrl);
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

    private void deployPackedApps(String[] zips)
    {
        for (String zip : zips)
        {
            try
            {
                applicationDeployer.deployPackagedArtifact(zip);
            }
            catch (Exception e)
            {
                // Ignore and continue
            }
        }
    }

    private void deployExplodedApps(String[] apps)
    {
        for (String addedApp : apps)
        {
            try
            {
                applicationDeployer.deployExplodedArtifact(addedApp);
            }
            catch (DeploymentException e)
            {
                // Ignore and continue
            }
        }
    }

    /**
     * Returns the list of anchor file names for the deployed apps
     *
     * @return a non null list of file names
     */
    private String[] findExpectedAnchorFiles(ObservableList<? extends Artifact> artifacts)
    {
        String[] anchors = new String[artifacts.size()];
        int i = 0;
        for (Artifact artifact : artifacts)
        {
            anchors[i++] = artifact.getArtifactName() + ARTIFACT_ANCHOR_SUFFIX;
        }
        return anchors;
    }

    public void undeploy(Domain domain)
    {
        domainDeployer.undeploy(domain);
    }

    public void setDomainFactory(DomainFactory domainFactory)
    {
        this.domainDeployer.setArtifactFactory(domainFactory);
    }

    /**
     * Not thread safe. Correctness is guaranteed by a single-threaded executor.
     */
    protected class ArtifactDirWatcher implements Runnable
    {

        protected File appsDir;
        protected File domainsDir;

        protected volatile boolean dirty;

        public ArtifactDirWatcher(final File appsDir, final File domainsDir)
        {
            this.appsDir = appsDir;
            this.domainsDir = domainsDir;
            applications.addPropertyChangeListener(new PropertyChangeListener()
            {
                public void propertyChange(PropertyChangeEvent e)
                {
                    if (e instanceof ElementAddedEvent || e instanceof ElementRemovedEvent)
                    {
                        if (logger.isDebugEnabled())
                        {
                            logger.debug("Deployed applications set has been modified, flushing state.");
                        }
                        dirty = true;
                    }
                }
            });
            domains.addPropertyChangeListener(new PropertyChangeListener()
            {
                public void propertyChange(PropertyChangeEvent e)
                {
                    if (e instanceof ElementAddedEvent || e instanceof ElementRemovedEvent)
                    {
                        if (logger.isDebugEnabled())
                        {
                            logger.debug("Deployed applications set has been modified, flushing state.");
                        }
                        dirty = true;
                    }
                }
            });
        }

        // Cycle is:
        //   undeploy removed apps
        //   undeploy removed domains
        //   deploy domain archives
        //   deploy domain exploded
        //   redeploy modified apps
        //   deploy archives apps
        //   deploy exploded apps
        public void run()
        {
            try
            {
                if (logger.isDebugEnabled())
                {
                    logger.debug("Checking for changes...");
                }
                // use non-barging lock to preserve fairness, according to javadocs
                // if there's a lock present - wait for next poll to do anything
                if (!deploymentInProgressLock.tryLock(0, TimeUnit.SECONDS))
                {
                    if (logger.isDebugEnabled())
                    {
                        logger.debug("Another deployment operation in progress, will skip this cycle. Owner thread: " +
                                     ((DebuggableReentrantLock) deploymentInProgressLock).getOwner());
                    }
                    return;
                }

                undeployRemovedApps();

                undeployRemovedDomains();

                // list new apps
                String[] domains = domainsDir.list(DirectoryFileFilter.DIRECTORY);

                final String[] domainZips = domainsDir.list(ZIP_ARTIFACT_FILTER);

                deployPackedDomains(domainZips);

                // re-scan exploded apps and update our state, as deploying Mule app archives might have added some
                if (domainZips.length > 0 || dirty)
                {
                    domains = domainsDir.list(DirectoryFileFilter.DIRECTORY);
                }

                deployExplodedDomains(domains);

                redeployModifiedApplications();

                // list new apps
                String[] apps = appsDir.list(DirectoryFileFilter.DIRECTORY);

                final String[] appZips = appsDir.list(ZIP_ARTIFACT_FILTER);

                deployPackedApps(appZips);

                // re-scan exploded apps and update our state, as deploying Mule app archives might have added some
                if (appZips.length > 0 || dirty)
                {
                    apps = appsDir.list(DirectoryFileFilter.DIRECTORY);
                }

                deployExplodedApps(apps);
            }
            catch (Exception e)
            {
                // preserve the flag for the thread
                Thread.currentThread().interrupt();
            }
            finally
            {
                if (deploymentInProgressLock.isHeldByCurrentThread())
                {
                    deploymentInProgressLock.unlock();
                }
                dirty = false;
            }
        }

        private void undeployRemovedDomains()
        {
            undeployRemovedArtifacts(domainsDir, domains, domainDeployer);
        }

        private void undeployRemovedApps()
        {
            undeployRemovedArtifacts(appsDir, applications, applicationDeployer);
        }

        private void undeployRemovedArtifacts(File artifactDir, ObservableList<? extends Artifact> artifacts, ArtifactDeployer<? extends Artifact> artifactDeployer)
        {
            // we care only about removed anchors
            String[] currentAnchors = artifactDir.list(new SuffixFileFilter(ARTIFACT_ANCHOR_SUFFIX));
            if (logger.isDebugEnabled())
            {
                StringBuilder sb = new StringBuilder();
                sb.append(String.format("Current anchors:%n"));
                for (String currentAnchor : currentAnchors)
                {
                    sb.append(String.format("  %s%n", currentAnchor));
                }
                logger.debug(sb.toString());
            }

            String[] artifactAnchors = findExpectedAnchorFiles(artifacts);
            @SuppressWarnings("unchecked")
            final Collection<String> deletedAnchors = CollectionUtils.subtract(Arrays.asList(artifactAnchors), Arrays.asList(currentAnchors));
            if (logger.isDebugEnabled())
            {
                StringBuilder sb = new StringBuilder();
                sb.append(String.format("Deleted anchors:%n"));
                for (String deletedAnchor : deletedAnchors)
                {
                    sb.append(String.format("  %s%n", deletedAnchor));
                }
                logger.debug(sb.toString());
            }

            for (String deletedAnchor : deletedAnchors)
            {
                String artifactName = StringUtils.removeEnd(deletedAnchor, ARTIFACT_ANCHOR_SUFFIX);
                try
                {
                    if (findArtifact(artifactName, artifacts) != null)
                    {
                        artifactDeployer.undeploy(artifactName);
                    }
                    else if (logger.isDebugEnabled())
                    {
                        logger.debug(String.format("Artifact [%s] has already been undeployed via API", artifactName));
                    }
                }
                catch (Throwable t)
                {
                    logger.error("Failed to undeploy artifact: " + artifactName, t);
                }
            }
        }
    }

    private void redeployModifiedApplications()
    {
        for (Application application : applications)
        {
            if (application.getDescriptor().isRedeploymentEnabled())
            {
                if (applicationTimestampListener.isApplicationResourceUpdated(application))
                {
                    applicationDeployer.redeploy(application);
                }
            }
        }
    }

    private static class ApplicationTimestampListener implements PropertyChangeListener
    {

        private Map<String, ApplicationResourcesTimestamps> applicationConfigResourcesTimestap = new HashMap<String, ApplicationResourcesTimestamps>();

        public ApplicationTimestampListener(ObservableList<Application> applications)
        {
            applications.addPropertyChangeListener(this);
        }

        @Override
        public void propertyChange(PropertyChangeEvent evt)
        {
            if (evt instanceof ElementAddedEvent)
            {
                Application applicationAdded = (Application) evt.getNewValue();
                applicationConfigResourcesTimestap.put(applicationAdded.getArtifactName(), new ApplicationResourcesTimestamps(applicationAdded));
            }
            else if (evt instanceof ElementRemovedEvent)
            {
                Application applicationRemoved = (Application) evt.getNewValue();
                applicationConfigResourcesTimestap.remove(applicationRemoved.getArtifactName());
            }
        }

        public boolean isApplicationResourceUpdated(Application application)
        {
            ApplicationResourcesTimestamps applicationResourcesTimestamps = applicationConfigResourcesTimestap.get(application.getArtifactName());
            return !applicationResourcesTimestamps.resourcesHaveSameTimestamp(application);
        }
    }

    private static class ApplicationResourcesTimestamps
    {

        private final Map<String, Long> timestampsPerResource = new HashMap<String, Long>();

        public ApplicationResourcesTimestamps(final Application application)
        {
            for (File configResourceFile : application.getConfigResourcesFile())
            {
                timestampsPerResource.put(configResourceFile.getAbsolutePath(), configResourceFile.lastModified());
            }
        }

        public boolean resourcesHaveSameTimestamp(final Application application)
        {
            boolean resourcesHaveSameTimestamp = true;
            for (File configResourceFile : application.getConfigResourcesFile())
            {
                long originalTimestamp = timestampsPerResource.get(configResourceFile.getAbsolutePath());
                long currentTimestamp = configResourceFile.lastModified();

                if (originalTimestamp != currentTimestamp)
                {
                    timestampsPerResource.put(configResourceFile.getAbsolutePath(), currentTimestamp);
                    resourcesHaveSameTimestamp = false;
                }
            }
            return resourcesHaveSameTimestamp;
        }
    }

}
