/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.launcher;

import static org.mule.util.SplashScreen.miniSplash;
import org.mule.MuleCoreExtension;
import org.mule.config.StartupContext;
import org.mule.config.i18n.MessageFactory;
import org.mule.module.launcher.application.Application;
import org.mule.module.launcher.application.ApplicationFactory;
import org.mule.module.launcher.util.DebuggableReentrantLock;
import org.mule.module.launcher.util.ElementAddedEvent;
import org.mule.module.launcher.util.ElementRemovedEvent;
import org.mule.module.launcher.util.ObservableList;
import org.mule.module.reboot.MuleContainerBootstrapUtils;
import org.mule.util.ArrayUtils;
import org.mule.util.CollectionUtils;
import org.mule.util.FileUtils;
import org.mule.util.StringUtils;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.beanutils.BeanPropertyValueEqualsPredicate;
import org.apache.commons.beanutils.BeanToPropertyValueTransformer;
import org.apache.commons.io.filefilter.AndFileFilter;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.io.filefilter.FileFileFilter;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.filefilter.SuffixFileFilter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class DeploymentService
{
    public static final String APP_ANCHOR_SUFFIX = "-anchor.txt";
    public static final String ZIP_FILE_SUFFIX = ".zip";
    public static final IOFileFilter ZIP_APPS_FILTER = new AndFileFilter(new SuffixFileFilter(ZIP_FILE_SUFFIX), FileFileFilter.FILE);

    protected static final int DEFAULT_CHANGES_CHECK_INTERVAL_MS = 5000;

    protected ScheduledExecutorService appDirMonitorTimer;

    protected transient final Log logger = LogFactory.getLog(getClass());
    protected MuleDeployer deployer;
    protected ApplicationFactory appFactory;
    // fair lock
    private ReentrantLock lock = new DebuggableReentrantLock(true);

    private ObservableList<Application> applications = new ObservableList<Application>();
    private Map<URL, Long> zombieMap = new HashMap<URL, Long>();

    private List<StartupListener> startupListeners = new ArrayList<StartupListener>();

    private List<DeploymentListener> deploymentListeners = new CopyOnWriteArrayList<DeploymentListener>();

    public DeploymentService(Map<Class<? extends MuleCoreExtension>, MuleCoreExtension> coreExtensions)
    {
        deployer = new DefaultMuleDeployer(this);
        appFactory = new ApplicationFactory(this, coreExtensions);
    }

    public void start()
    {
        lock.lock();
        try
        {
            // install phase
            final Map<String, Object> options = StartupContext.get().getStartupOptions();
            String appString = (String) options.get("app");

            final File appsDir = MuleContainerBootstrapUtils.getMuleAppsDir();

            // delete any leftover anchor files from previous unclean shutdowns
            String[] appAnchors = appsDir.list(new SuffixFileFilter(APP_ANCHOR_SUFFIX));
            for (String anchor : appAnchors)
            {
                // ignore result
                new File(appsDir, anchor).delete();
            }

            String[] apps = ArrayUtils.EMPTY_STRING_ARRAY;

            // mule -app app1:app2:app3 will restrict deployment only to those specified apps
            final boolean explicitAppSet = appString != null;

            DeploymentStatusTracker deploymentStatusTracker = new DeploymentStatusTracker();
            addDeploymentListener(deploymentStatusTracker);

            StartupSummaryDeploymentListener summaryDeploymentListener = new StartupSummaryDeploymentListener(deploymentStatusTracker);
            addStartupListener(summaryDeploymentListener);

            if (!explicitAppSet)
            {
                String[] dirApps = appsDir.list(DirectoryFileFilter.DIRECTORY);
                apps = (String[]) ArrayUtils.addAll(apps, dirApps);

                String[] zipApps = appsDir.list(ZIP_APPS_FILTER);
                for (int i = 0; i < zipApps.length; i++)
                {
                    zipApps[i] = StringUtils.removeEndIgnoreCase(zipApps[i], ZIP_FILE_SUFFIX);
                }

                // TODO this is a place to put a FQN of the custom sorter (use AND filter)
                // Add string shortcuts for bundled ones
                apps = (String[]) ArrayUtils.addAll(dirApps, zipApps);
                Arrays.sort(apps);
            }
            else
            {
                apps = appString.split(":");
            }

            apps = removeDuplicateAppNames(apps);

            for (String app : apps)
            {
                final Application a;
                String appMarker = app;
                File applicationFile = null;
                try
                {
                    // if there's a zip, explode and install it
                    applicationFile = new File(appsDir, app + ".zip");
                    if (applicationFile.exists() && applicationFile.isFile())
                    {
                        appMarker = app + ZIP_FILE_SUFFIX;
                        a = deployer.installFromAppDir(applicationFile.getName());
                    }
                    else
                    {
                        // otherwise just create an app object from a deployed app
                        applicationFile = new File(appsDir, appMarker);
                        a = appFactory.createApp(app);
                    }
                    applications.add(a);
                }
                catch (Throwable t)
                {
                    fireOnDeploymentFailure(appMarker, t);
                    addZombie(applicationFile);
                    logger.error(String.format("Failed to create application [%s]", appMarker), t);
                }
            }

            for (Application application : applications)
            {
                try
                {
                    fireOnDeploymentStart(application.getAppName());
                    deployer.deploy(application);
                    fireOnDeploymentSuccess(application.getAppName());
                }
                catch (Throwable t)
                {
                    fireOnDeploymentFailure(application.getAppName(), t);

                    // error text has been created by the deployer already
                    final String msg = miniSplash(String.format("Failed to deploy app '%s', see below", application.getAppName()));
                    logger.error(msg, t);
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
            if (!explicitAppSet)
            {
                scheduleChangeMonitor(appsDir);
            }
            else
            {
                if (logger.isInfoEnabled())
                {
                    logger.info(miniSplash("Mule is up and running in a fixed app set mode"));
                }
            }
        }
        finally
        {
            if (lock.isHeldByCurrentThread())
            {
                lock.unlock();
            }
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

    protected void scheduleChangeMonitor(File appsDir)
    {
        final int reloadIntervalMs = DEFAULT_CHANGES_CHECK_INTERVAL_MS;
        appDirMonitorTimer = Executors.newSingleThreadScheduledExecutor(new AppDeployerMonitorThreadFactory());

        appDirMonitorTimer.scheduleWithFixedDelay(new AppDirWatcher(appsDir),
                                                  0,
                                                  reloadIntervalMs,
                                                  TimeUnit.MILLISECONDS);

        if (logger.isInfoEnabled())
        {
            logger.info(miniSplash(String.format("Mule is up and kicking (every %dms)", reloadIntervalMs)));
        }
    }

    public void stop()
    {
        if (appDirMonitorTimer != null)
        {
            appDirMonitorTimer.shutdownNow();
        }

        // tear down apps in reverse order
        Collections.reverse(applications);
        for (Application application : applications)
        {
            try
            {
                application.stop();
                application.dispose();
            }
            catch (Throwable t)
            {
                logger.error(t);
            }
        }

    }

    /**
     * Find an active application.
     * @return null if not found
     */
    public Application findApplication(String appName)
    {
        return (Application) CollectionUtils.find(applications, new BeanPropertyValueEqualsPredicate("appName", appName));
    }

    /**
     * @return immutable applications list
     */
    public List<Application> getApplications()
    {
        return Collections.unmodifiableList(applications);
    }

    /**
     * @return URL/lastModified of apps which previously failed to deploy
     */
    public Map<URL, Long> getZombieMap()
    {
        return zombieMap;
    }

    protected MuleDeployer getDeployer()
    {
        return deployer;
    }

    public void setDeployer(MuleDeployer deployer)
    {
        this.deployer = deployer;
    }

    public void setAppFactory(ApplicationFactory appFactory)
    {
        this.appFactory = appFactory;
    }

    public ApplicationFactory getAppFactory()
    {
        return appFactory;
    }

    public ReentrantLock getLock() {
        return lock;
    }

    public void onApplicationInstalled(Application a)
    {
        applications.add(a);
    }

    protected void undeploy(Application app)
    {
        if (logger.isInfoEnabled())
        {
            logger.info("================== Request to Undeploy Application: " + app.getAppName());
        }

        try {
           fireOnUndeploymentStart(app.getAppName());

           applications.remove(app);
           deployer.undeploy(app);

           fireOnUndeploymentSuccess(app.getAppName());
        } catch (RuntimeException e) {
           fireOnUndeploymentFailure(app.getAppName(), e);

           throw e;
        }
    }

    public void undeploy(String appName)
    {
        Application app = (Application) CollectionUtils.find(applications, new BeanPropertyValueEqualsPredicate("appName", appName));
        undeploy(app);
    }

    public void deploy(URL appArchiveUrl) throws IOException
    {
        final Application application;
        try
        {
            application = deployer.installFrom(appArchiveUrl);
            applications.add(application);

            try
            {
                fireOnDeploymentStart(application.getAppName());
                deployer.deploy(application);
                fireOnDeploymentSuccess(application.getAppName());
            }
            catch (Throwable t)
            {
                fireOnDeploymentFailure(application.getAppName(), t);

                throw t;
            }
        }
        catch (Throwable t)
        {
            addZombie(FileUtils.toFile(appArchiveUrl));
            if (t instanceof DeploymentException)
            {
                // re-throw
                throw ((DeploymentException) t);
            }

            final String msg = "Failed to deploy from URL: " + appArchiveUrl;
            throw new DeploymentException(MessageFactory.createStaticMessage(msg), t);
        }
    }

    protected void addZombie(File marker)
    {
        // no sync required as deploy operations are single-threaded
        if (marker == null)
        {
            return;
        }

        if (!marker.exists())
        {
            return;
        }

        try
        {
            if (marker.isDirectory())
            {
                final File appConfig = new File(marker, "mule-config.xml");
                if (appConfig.exists())
                {
                    long lastModified = appConfig.lastModified();
                    zombieMap.put(appConfig.toURI().toURL(), lastModified);
                }
            }
            else
            {
                // zip deployment
                long lastModified = marker.lastModified();

                zombieMap.put(marker.toURI().toURL(), lastModified);
            }
        }
        catch (MalformedURLException e)
        {
            logger.debug(String.format("Failed to mark an exploded app [%s] as a zombie", marker.getName()), e);
        }
    }

    public void addStartupListener(StartupListener listener)
    {
        this.startupListeners.add(listener);
    }

    public void removeStartupListener(StartupListener listener)
    {
        this.startupListeners.remove(listener);
    }

    public void addDeploymentListener(DeploymentListener listener)
    {
        this.deploymentListeners.add(listener);
    }

    public void removeDeploymentListener(DeploymentListener listener)
    {
        this.deploymentListeners.remove(listener);
    }

    /**
     * Notifies all deployment listeners that the deploy for a given application
     * has just started.
     *
     * @param appName the name of the application being deployed.
     */
    public void fireOnDeploymentStart(String appName)
    {
        for (DeploymentListener listener : deploymentListeners)
        {
            try
            {
                listener.onDeploymentStart(appName);
            }
            catch (Throwable t)
            {
                logger.error("Listener failed to process onDeploymentStart notification", t);
            }
        }
    }

    /**
     * Notifies all deployment listeners that the deploy for a given application
     * has successfully finished.
     *
     * @param appName the name of the deployed application.
     */
    public void fireOnDeploymentSuccess(String appName)
    {
        for (DeploymentListener listener : deploymentListeners)
        {
            try
            {
                listener.onDeploymentSuccess(appName);
            }
            catch (Throwable t)
            {
                logger.error("Listener failed to process onDeploymentSuccess notification", t);
            }
        }
    }

    /**
     * Notifies all deployment listeners that the deploy for a given application
     * has finished with a failure.
     *
     * @param appName the name of the deployed application.
     * @param cause the cause of the deployment failure.
     */
    public void fireOnDeploymentFailure(String appName, Throwable cause)
    {
        for (DeploymentListener listener : deploymentListeners)
        {
            try
            {
                listener.onDeploymentFailure(appName, cause);
            }
            catch (Throwable t)
            {
                logger.error("Listener failed to process onDeploymentFailure notification", t);
            }
        }
    }

    /**
     * Notifies all deployment listeners that un-deployment for a given application
     * has just started.
     *
     * @param appName the name of the application being un-deployed.
     */
    public void fireOnUndeploymentStart(String appName)
    {
        for (DeploymentListener listener : deploymentListeners)
        {
            try
            {
                listener.onUndeploymentStart(appName);
            }
            catch (Throwable t)
            {
                logger.error("Listener failed to process onUndeploymentStart notification", t);
            }
        }
    }

    /**
     * Notifies all deployment listeners that un-deployment for a given application
     * has successfully finished.
     *
     * @param appName the name of the un-deployed application.
     */
    public void fireOnUndeploymentSuccess(String appName)
    {
        for (DeploymentListener listener : deploymentListeners)
        {
            try
            {
                listener.onUndeploymentSuccess(appName);
            }
            catch (Throwable t)
            {
                logger.error("Listener failed to process onUndeploymentSuccess notification", t);
            }
        }
    }

    /**
     * Notifies all deployment listeners that un-deployment for a given application
     * has finished with a failure.
     *
     * @param appName the name of the un-deployed application.
     * @param cause the cause of the deployment failure.
     */
    public void fireOnUndeploymentFailure(String appName, Throwable cause)
    {
        for (DeploymentListener listener : deploymentListeners)
        {
            try
            {
                listener.onUndeploymentFailure(appName, cause);
            }
            catch (Throwable t)
            {
                logger.error("Listener failed to process onUndeploymentFailure notification", t);
            }
        }
    }

    public interface StartupListener
    {

        /**
         * Invoked after all apps have passed the deployment phase. Any exceptions thrown by implementations
         * will be ignored.
         */
        void onAfterStartup();
    }

    /**
     * Not thread safe. Correctness is guaranteed by a single-threaded executor.
     */
    protected class AppDirWatcher implements Runnable
    {
        protected File appsDir;

        protected volatile boolean dirty;

        public AppDirWatcher(final File appsDir)
        {
            this.appsDir = appsDir;
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
        }

        // Cycle is:
        //   undeploy removed apps
        //   deploy archives
        //   deploy exploded
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
                if (!lock.tryLock(0, TimeUnit.SECONDS))
                {
                    if (logger.isDebugEnabled())
                    {
                        logger.debug("Another deployment operation in progress, will skip this cycle. Owner thread: " +
                                     ((DebuggableReentrantLock) lock).getOwner());
                    }
                    return;
                }

                // list new apps
                final String[] zips = appsDir.list(ZIP_APPS_FILTER);
                String[] apps = appsDir.list(DirectoryFileFilter.DIRECTORY);

                // we care only about removed anchors
                String[] currentAnchors = appsDir.list(new SuffixFileFilter(APP_ANCHOR_SUFFIX));
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

                String[] appAnchors = findExpectedAnchorFiles();

                @SuppressWarnings("unchecked")
                final Collection<String> deletedAnchors = CollectionUtils.subtract(Arrays.asList(appAnchors), Arrays.asList(currentAnchors));
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
                    String appName = StringUtils.removeEnd(deletedAnchor, APP_ANCHOR_SUFFIX);
                    try
                    {
                        if (findApplication(appName) != null)
                        {
                            undeploy(appName);
                        }
                        else if (logger.isDebugEnabled())
                        {
                            logger.debug(String.format("Application [%s] has already been undeployed via API", appName));
                        }
                    }
                    catch (Throwable t)
                    {
                        logger.error("Failed to undeploy application: " + appName, t);
                    }
                }

                // new packed Mule apps
                for (String zip : zips)
                {
                    URL url;
                    File appZip = null;
                    try
                    {
                        // check if this app is running first, undeploy it then
                        final String appName = StringUtils.removeEnd(zip, ".zip");
                        Application app = (Application) CollectionUtils.find(applications, new BeanPropertyValueEqualsPredicate("appName", appName));
                        if (app != null)
                        {
                            undeploy(appName);
                        }
                        appZip = new File(appsDir, zip);
                        url = appZip.toURI().toURL();

                        if (isZombieApplication(appZip))
                        {
                            // Skips the file because it was already deployed with failure
                            continue;
                        }

                        deploy(url);
                    }
                    catch (Throwable t)
                    {
                        logger.error("Failed to deploy application archive: " + zip, t);
                        addZombie(appZip);
                    }
                }

                // re-scan exploded apps and update our state, as deploying Mule app archives might have added some
                if (zips.length > 0 || dirty)
                {
                    apps = appsDir.list(DirectoryFileFilter.DIRECTORY);
                }

                @SuppressWarnings("rawtypes")
                Collection deployedAppNames = CollectionUtils.collect(applications, new BeanToPropertyValueTransformer("appName"));

                // new exploded Mule apps
                @SuppressWarnings("unchecked")
                final Collection<String> addedApps = CollectionUtils.subtract(Arrays.asList(apps), deployedAppNames);
                for (String addedApp : addedApps)
                {
                    final File appDir = new File(appsDir, addedApp);
                    if (isZombieApplication(appDir))
                    {
                        continue;
                    }
                    try
                    {
                        onNewExplodedApplication(addedApp);
                    }
                    catch (Throwable t)
                    {
                        addZombie(appDir);
                        logger.error("Failed to deploy exploded application: " + addedApp, t);
                    }
                }

            }
            catch (InterruptedException e)
            {
                // preserve the flag for the thread
                Thread.currentThread().interrupt();
            }
            finally
            {
                if (lock.isHeldByCurrentThread())
                {
                    lock.unlock();
                }
                dirty = false;
            }
        }

        /**
         * Determines if a given URL points to the same file as an existing
         * Returns the list of anchor file names for the deployed apps
         *
         * @return a non null list of file names
         */
        private String[] findExpectedAnchorFiles()
        {
            String[] appAnchors = new String[applications.size()];
            int i =0;
            for (Application application : applications)
            {
                appAnchors[i++] = application.getAppName() + APP_ANCHOR_SUFFIX;
            }
            return appAnchors;
        }

        /**
         * Determines if a given URL points to the same file as an existing
         * zombie application.
         *
         * @param marker a pointer to a zip or exploded mule app (in the latter case
         *        an app's 'mule-config.xml' will be monitored for updates
         * @return true if the URL already a zombie application and both file
         *         timestamps are the same.
         */
        protected boolean isZombieApplication(File marker)
        {
            URL url;

            if (!marker.exists())
            {
                return false;
            }

            try
            {
                if (marker.isDirectory())
                {
                    // this is an exploded app
                    url = new File(marker, "mule-config.xml").toURI().toURL();
                }
                else
                {
                    url = marker.toURI().toURL();
                }
            }
            catch (MalformedURLException e)
            {
                throw new RuntimeException(e);
            }

            boolean result = false;

            if (zombieMap.containsKey(url))
            {
                long originalTimeStamp = zombieMap.get(url);
                long newTimeStamp = FileUtils.getFileTimeStamp(url);

                if (originalTimeStamp == newTimeStamp)
                {
                    result = true;
                }
            }

            return result;
        }

        /**
         * @param appName application name as it appears in $MULE_HOME/apps
         */
        protected void onNewExplodedApplication(String appName) throws Exception
        {
            if (logger.isInfoEnabled())
            {
                logger.info("================== New Exploded Application: " + appName);
            }

            Application a = appFactory.createApp(appName);
            // add to the list of known apps first to avoid deployment loop on failure
            onApplicationInstalled(a);

            try
            {
                fireOnDeploymentStart(a.getAppName());
                deployer.deploy(a);
                fireOnDeploymentSuccess(a.getAppName());
            }
            catch (Exception e)
            {
                fireOnDeploymentFailure(a.getAppName(), e);

                throw e;
            }
        }

    }
}
