/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.launcher;

import static org.mule.util.SplashScreen.miniSplash;
import org.mule.config.StartupContext;
import org.mule.config.i18n.MessageFactory;
import org.mule.module.launcher.application.Application;
import org.mule.module.launcher.application.ApplicationClassLoaderFactory;
import org.mule.module.launcher.application.ApplicationFactory;
import org.mule.module.launcher.application.CompositeApplicationClassLoaderFactory;
import org.mule.module.launcher.application.DefaultApplicationFactory;
import org.mule.module.launcher.application.MuleApplicationClassLoaderFactory;
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

public class MuleDeploymentService implements DeploymentService
{
    public static final String APP_ANCHOR_SUFFIX = "-anchor.txt";
    public static final String ZIP_FILE_SUFFIX = ".zip";
    public static final IOFileFilter ZIP_APPS_FILTER = new AndFileFilter(new SuffixFileFilter(ZIP_FILE_SUFFIX), FileFileFilter.FILE);

    public static final String ANOTHER_DEPLOYMENT_OPERATION_IS_IN_PROGRESS = "Another deployment operation is in progress";
    public static final String INSTALL_OPERATION_HAS_BEEN_INTERRUPTED = "Install operation has been interrupted";

    protected static final int DEFAULT_CHANGES_CHECK_INTERVAL_MS = 5000;
    public static final String CHANGE_CHECK_INTERVAL_PROPERTY = "mule.launcher.changeCheckInterval";

    protected ScheduledExecutorService appDirMonitorTimer;

    protected transient final Log logger = LogFactory.getLog(getClass());
    protected MuleDeployer deployer;
    protected ApplicationFactory appFactory;
    // fair lock
    private ReentrantLock lock = new DebuggableReentrantLock(true);

    private ObservableList<Application> applications = new ObservableList<Application>();
    private Map<String, ZombieFile> zombieMap = new HashMap<String, ZombieFile>();
    private final File appsDir = MuleContainerBootstrapUtils.getMuleAppsDir();

    private List<StartupListener> startupListeners = new ArrayList<StartupListener>();

    private CompositeDeploymentListener deploymentListener = new CompositeDeploymentListener();

    public MuleDeploymentService(PluginClassLoaderManager pluginClassLoaderManager)
    {
        ApplicationClassLoaderFactory applicationClassLoaderFactory = new MuleApplicationClassLoaderFactory();
        applicationClassLoaderFactory = new CompositeApplicationClassLoaderFactory(applicationClassLoaderFactory, pluginClassLoaderManager);
        DefaultApplicationFactory appFactory = new DefaultApplicationFactory(applicationClassLoaderFactory);
        appFactory.setDeploymentListener(deploymentListener);
        this.appFactory = appFactory;

        DefaultMuleDeployer deployer = new DefaultMuleDeployer();
        deployer.setApplicationFactory(this.appFactory);
        this.deployer = deployer;
    }

    @Override
    public void start()
    {
        lock.lock();
        try
        {
            DeploymentStatusTracker deploymentStatusTracker = new DeploymentStatusTracker();
            addDeploymentListener(deploymentStatusTracker);

            StartupSummaryDeploymentListener summaryDeploymentListener = new StartupSummaryDeploymentListener(deploymentStatusTracker);
            addStartupListener(summaryDeploymentListener);

            deleteAllAnchors();

            // mule -app app1:app2:app3 will restrict deployment only to those specified apps
            final Map<String, Object> options = StartupContext.get().getStartupOptions();
            String appString = (String) options.get("app");

            if (appString == null)
            {
                String[] explodedApps = appsDir.list(DirectoryFileFilter.DIRECTORY);
                String[] packagedApps = appsDir.list(ZIP_APPS_FILTER);

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
                            deployPackedApp(app + ZIP_FILE_SUFFIX);
                        }
                        else
                        {
                            deployExplodedApp(app);
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

    private void deleteAllAnchors()
    {
        // Deletes any leftover anchor files from previous shutdowns
        String[] appAnchors = appsDir.list(new SuffixFileFilter(APP_ANCHOR_SUFFIX));
        for (String anchor : appAnchors)
        {
            // ignore result
            new File(appsDir, anchor).delete();
        }
    }

    private void deployApplication(Application application) throws DeploymentException
    {
        try
        {
            deploymentListener.onDeploymentStart(application.getAppName());
            guardedDeploy(application);
            deploymentListener.onDeploymentSuccess(application.getAppName());
            zombieMap.remove(application.getAppName());
        }
        catch (Throwable t)
        {
            // error text has been created by the deployer already
            String msg = miniSplash(String.format("Failed to deploy app '%s', see below", application.getAppName()));
            logger.error(msg, t);

            addZombieApp(application);

            deploymentListener.onDeploymentFailure(application.getAppName(), t);
            if (t instanceof DeploymentException)
            {
                throw (DeploymentException) t;
            }
            else
            {
                msg = "Failed to deploy application: " + application.getAppName();
                throw new DeploymentException(MessageFactory.createStaticMessage(msg), t);
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
        final int reloadIntervalMs = getChangesCheckIntervalMs();
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

        lock.lock();
        try
        {
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
        finally
        {
            lock.unlock();
        }
    }

    private void stopAppDirMonitorTimer()
    {
        if (appDirMonitorTimer != null)
        {
            appDirMonitorTimer.shutdown();
            try
            {
                appDirMonitorTimer.awaitTermination(getChangesCheckIntervalMs(), TimeUnit.MILLISECONDS);
            }
            catch (InterruptedException e)
            {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public Application findApplication(String appName)
    {
        return (Application) CollectionUtils.find(applications, new BeanPropertyValueEqualsPredicate("appName", appName));
    }

    @Override
    public List<Application> getApplications()
    {
        return Collections.unmodifiableList(applications);
    }

    /**
     * @return URL/lastModified of apps which previously failed to deploy
     */
    public Map<URL, Long> getZombieMap()
    {
        Map<URL, Long> result = new HashMap<URL, Long>();

        for (String app : zombieMap.keySet())
        {
            ZombieFile file = zombieMap.get(app);
            result.put(file.url, file.originalTimestamp);
        }

        return result;
    }

    protected MuleDeployer getDeployer()
    {
        return deployer;
    }

    public void setAppFactory(ApplicationFactory appFactory)
    {
        this.appFactory = appFactory;
    }

    public void setDeployer(MuleDeployer deployer)
    {
        this.deployer = deployer;
    }

    public ApplicationFactory getAppFactory()
    {
        return appFactory;
    }

    @Override
    public ReentrantLock getLock() {
        return lock;
    }

    protected void onApplicationInstalled(Application a)
    {
        trackApplication(a);
    }

    private void trackApplication(Application application)
    {
        Application previousApplication = findApplication(application.getAppName());
        applications.remove(previousApplication);

        applications.add(application);
    }

    protected void undeploy(Application app)
    {
        if (logger.isInfoEnabled())
        {
            logger.info("================== Request to Undeploy Application: " + app.getAppName());
        }

        try
        {
            deploymentListener.onUndeploymentStart(app.getAppName());

            applications.remove(app);
            guardedUndeploy(app);

            deploymentListener.onUndeploymentSuccess(app.getAppName());
        }
        catch (RuntimeException e)
        {
            deploymentListener.onUndeploymentFailure(app.getAppName(), e);
            throw e;
        }
    }

    @Override
    public void undeploy(String appName)
    {
        Application app = (Application) CollectionUtils.find(applications, new BeanPropertyValueEqualsPredicate("appName", appName));
        undeploy(app);
    }

    @Override
    public void deploy(URL appArchiveUrl) throws IOException
    {
        Application application;

        try
        {
            try
            {
                application = guardedInstallFrom(appArchiveUrl);
                trackApplication(application);
            }
            catch (Throwable t)
            {
                File appArchive = new File(appArchiveUrl.toURI());
                String appName = StringUtils.removeEnd(appArchive.getName(), ZIP_FILE_SUFFIX);

                //// error text has been created by the deployer already
                final String msg = miniSplash(String.format("Failed to deploy app '%s', see below", appName));
                logger.error(msg, t);

                addZombieFile(appName, appArchive);

                deploymentListener.onDeploymentFailure(appName, t);

                throw t;
            }

            deployApplication(application);
        }
        catch (Throwable t)
        {
            if (t instanceof DeploymentException)
            {
                // re-throw
                throw ((DeploymentException) t);
            }

            final String msg = "Failed to deploy from URL: " + appArchiveUrl;
            throw new DeploymentException(MessageFactory.createStaticMessage(msg), t);
        }
    }

    private void guardedDeploy(Application application)
    {
        try
        {
            if (!lock.tryLock(0, TimeUnit.SECONDS))
            {
                return;
            }

            deployer.deploy(application);
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
        }
        finally
        {
            if (lock.isHeldByCurrentThread())
            {
                lock.unlock();
            }
        }
    }

    private void guardedUndeploy(Application app)
    {
        try
        {
            if (!lock.tryLock(0, TimeUnit.SECONDS))
            {
                return;
            }

            deployer.undeploy(app);
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
        }
        finally
        {
            if (lock.isHeldByCurrentThread())
            {
                lock.unlock();
            }
        }
    }

    private Application guardedInstallFrom(URL appArchiveUrl) throws IOException
    {
        try
        {
            if (!lock.tryLock(0, TimeUnit.SECONDS))
            {
                throw new IOException(ANOTHER_DEPLOYMENT_OPERATION_IS_IN_PROGRESS);
            }

            return deployer.installFrom(appArchiveUrl);
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
            throw new IOException(INSTALL_OPERATION_HAS_BEEN_INTERRUPTED);
        }
        finally
        {
            if (lock.isHeldByCurrentThread())
            {
                lock.unlock();
            }
        }
    }

    protected void addZombieApp(Application application)
    {
        final File appDir = new File(MuleContainerBootstrapUtils.getMuleAppsDir(), application.getAppName()) ;

        String resource = application.getDescriptor().getConfigResources()[0];
        File resourceFile = new File(appDir, resource);

        if (resourceFile.exists())
        {
            try
            {
                zombieMap.put(application.getAppName(), new ZombieFile(resourceFile));
            }
            catch (Exception e)
            {
                // Ignore resource
            }
        }
    }

    protected void addZombieFile(String appName, File marker)
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
            zombieMap.put(appName, new ZombieFile(marker));
        }
        catch (Exception e)
        {
            logger.debug(String.format("Failed to mark an exploded app [%s] as a zombie", marker.getName()), e);
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
        deploymentListener.addDeploymentListener(listener);
    }

    @Override
    public void removeDeploymentListener(DeploymentListener listener)
    {
        deploymentListener.removeDeploymentListener(listener);
    }

    private void deployPackedApps(String[] zips)
    {
        for (String zip : zips)
        {
            try
            {
                deployPackedApp(zip);
            }
            catch (Exception e)
            {
                // Ignore and continue
            }
        }
    }

    private void deployPackedApp(String zip) throws Exception
    {
        URL url;
        File appZip;

        final String appName = StringUtils.removeEnd(zip, ZIP_FILE_SUFFIX);

        appZip = new File(appsDir, zip);
        url = appZip.toURI().toURL();

        ZombieFile zombieFile = zombieMap.get(appName);
        if (zombieFile != null)
        {
            if (zombieFile.isFor(url) && !zombieFile.updatedZombieApp())
            {
                // Skips the file because it was already deployed with failure
                return;
            }
        }

        // check if this app is running first, undeploy it then
        Application app = (Application) CollectionUtils.find(applications, new BeanPropertyValueEqualsPredicate("appName", appName));
        if (app != null)
        {
            undeploy(appName);
        }

        deploy(url);
    }

    private void deployExplodedApps(String[] apps)
    {
        @SuppressWarnings("rawtypes")
        Collection<String> deployedAppNames = CollectionUtils.collect(applications, new BeanToPropertyValueTransformer("appName"));

        for (String addedApp : apps)
        {
            ZombieFile zombieFile = zombieMap.get(addedApp);

            if ((zombieFile != null) && (!zombieFile.updatedZombieApp()))
            {
                continue;
            }

            if (deployedAppNames.contains(addedApp) && (!zombieMap.containsKey(addedApp)))
            {
                continue;
            }

            try
            {
                deployExplodedApp(addedApp);
            }
            catch (DeploymentException e)
            {
                // Ignore and continue
            }
        }
    }

    private void deployExplodedApp(String addedApp) throws DeploymentException
    {
        if (logger.isInfoEnabled())
        {
            logger.info("================== New Exploded Application: " + addedApp);
        }

        Application application;
        try
        {
            application = appFactory.createApp(addedApp);

            // add to the list of known apps first to avoid deployment loop on failure
            onApplicationInstalled(application);
        }
        catch (Throwable t)
        {
            final File appsDir1 = MuleContainerBootstrapUtils.getMuleAppsDir();
            File appDir1 = new File(appsDir1, addedApp);

            addZombieFile(addedApp, appDir1);

            String msg = miniSplash(String.format("Failed to deploy exploded application: '%s', see below", addedApp));
            logger.error(msg, t);

            deploymentListener.onDeploymentFailure(addedApp, t);

            if (t instanceof DeploymentException)
            {
                throw (DeploymentException) t;
            }
            else
            {
                msg = "Failed to deploy application: " + addedApp;
                throw new DeploymentException(MessageFactory.createStaticMessage(msg), t);
            }
        }

        deployApplication(application);
    }

    /**
     * Returns the list of anchor file names for the deployed apps
     *
     * @return a non null list of file names
     */
    private String[] findExpectedAnchorFiles()
    {
        String[] appAnchors = new String[applications.size()];
        int i = 0;

        for (Application application : applications)
        {
            appAnchors[i++] = application.getAppName() + APP_ANCHOR_SUFFIX;
        }

        return appAnchors;
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

                undeployRemovedApps();

                // list new apps
                String[] apps = appsDir.list(DirectoryFileFilter.DIRECTORY);

                final String[] zips = appsDir.list(ZIP_APPS_FILTER);

                deployPackedApps(zips);

                // re-scan exploded apps and update our state, as deploying Mule app archives might have added some
                if (zips.length > 0 || dirty)
                {
                    apps = appsDir.list(DirectoryFileFilter.DIRECTORY);
                }

                deployExplodedApps(apps);
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

        private void undeployRemovedApps()
        {
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
                    if (zombieMap.containsKey(appName))
                    {
                        continue;
                    }

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
        }
    }

    private static class ZombieFile
    {

        URL url;
        Long originalTimestamp;
        File file;

        private ZombieFile(File file)
        {
            this.file = file;
            originalTimestamp = file.lastModified();
            try
            {
                url = file.toURI().toURL();
            }
            catch (MalformedURLException e)
            {
                throw new IllegalArgumentException(e);
            }
        }

        public boolean isFor(URL url)
        {
            return this.url.equals(url);
        }

        public boolean updatedZombieApp()
        {
            return originalTimestamp != file.lastModified();
        }
    }
}
