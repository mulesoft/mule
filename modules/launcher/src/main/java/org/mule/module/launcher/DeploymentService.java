/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.launcher;

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
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.beanutils.BeanPropertyValueEqualsPredicate;
import org.apache.commons.beanutils.BeanToPropertyValueTransformer;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.io.filefilter.SuffixFileFilter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import static org.mule.util.SplashScreen.miniSplash;

public class DeploymentService
{
    public static final String APP_ANCHOR_SUFFIX = "-anchor.txt";
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

    private CompositeDeploymentListener deploymentListener = new CompositeDeploymentListener();

    public DeploymentService(Map<Class<? extends MuleCoreExtension>, MuleCoreExtension> coreExtensions)
    {
        deployer = new DefaultMuleDeployer(this);
        appFactory = new ApplicationFactory(this, coreExtensions, deploymentListener);
    }

    public void start()
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

        String[] apps;

        // mule -app app1:app2:app3 will restrict deployment only to those specified apps
        final boolean explicitAppSet = appString != null;

        DeploymentStatusTracker deploymentStatusTracker = new DeploymentStatusTracker();
        addDeploymentListener(deploymentStatusTracker);

        StartupSummaryDeploymentListener summaryDeploymentListener = new StartupSummaryDeploymentListener(deploymentStatusTracker);
        addStartupListener(summaryDeploymentListener);

        if (!explicitAppSet)
        {
            // explode any app zips first
            final String[] zips = appsDir.list(new SuffixFileFilter(".zip"));
            Arrays.sort(zips);
            for (String zip : zips)
            {
                String appName = StringUtils.removeEnd(zip, ".zip");

                try
                {
                    // we don't care about the returned app object on startup
                    deployer.installFromAppDir(zip);
                }
                catch (Throwable t)
                {
                    logger.error(String.format("Failed to install app from archive '%s'", zip), t);

                    deploymentListener.onDeploymentFailure(appName, t);

                    File appZip = new File(appsDir, zip);
                    addZombie(appZip);
                }
            }

            // TODO this is a place to put a FQN of the custom sorter (use AND filter)
            // Add string shortcuts for bundled ones
            apps = appsDir.list(DirectoryFileFilter.DIRECTORY);
            Arrays.sort(apps);
        }
        else
        {
            apps = appString.split(":");
        }

        for (String app : apps)
        {
            final Application a;
            try
            {
                // if there's a zip, explode and install it
                final File appZip = new File(appsDir, app + ".zip");
                if (appZip.exists())
                {
                    a = deployer.installFromAppDir(appZip.getName());
                }
                else
                {
                    // otherwise just create an app object from a deployed app
                    a = appFactory.createApp(app);
                }
                applications.add(a);
            }
            catch (Throwable t)
            {
                deploymentListener.onDeploymentFailure(app, t);
                addZombie(new File(appsDir, app));
                logger.error(String.format("Failed to create application [%s]", app), t);
            }
        }


        for (Application application : applications)
        {
            try
            {
                deploymentListener.onDeploymentStart(application.getAppName());
                deployer.deploy(application);
                deploymentListener.onDeploymentSuccess(application.getAppName());
            }
            catch (Throwable t)
            {
                deploymentListener.onDeploymentFailure(application.getAppName(), t);

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
           deploymentListener.onUndeploymentStart(app.getAppName());

           applications.remove(app);
           deployer.undeploy(app);

            deploymentListener.onUndeploymentSuccess(app.getAppName());
        } catch (RuntimeException e) {
           deploymentListener.onUndeploymentFailure(app.getAppName(), e);
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
                deploymentListener.onDeploymentStart(application.getAppName());
                deployer.deploy(application);
                deploymentListener.onDeploymentSuccess(application.getAppName());
            }
            catch (Throwable t)
            {
                deploymentListener.onDeploymentFailure(application.getAppName(), t);

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
        deploymentListener.addDeploymentListener(listener);
    }

    public void removeDeploymentListener(DeploymentListener listener)
    {
        deploymentListener.removeDeploymentListener(listener);
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

        // written on app start, will be used to cleanly undeploy the app without file locking issues
        protected String[] appAnchors = new String[0];
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
                final String[] zips = appsDir.list(new SuffixFileFilter(".zip"));
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
                appAnchors = currentAnchors;


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
                deploymentListener.onDeploymentStart(a.getAppName());
                deployer.deploy(a);
                deploymentListener.onDeploymentSuccess(a.getAppName());
            }
            catch (Exception e)
            {
                deploymentListener.onDeploymentFailure(a.getAppName(), e);

                throw e;
            }
        }

    }

}
