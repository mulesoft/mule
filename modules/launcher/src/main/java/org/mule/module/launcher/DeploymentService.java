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

import org.mule.config.StartupContext;
import org.mule.module.launcher.application.Application;
import org.mule.module.launcher.application.ApplicationFactory;
import org.mule.module.launcher.util.DebuggableReentrantLock;
import org.mule.module.launcher.util.ElementAddedEvent;
import org.mule.module.launcher.util.ElementRemovedEvent;
import org.mule.module.launcher.util.ObservableList;
import org.mule.module.reboot.MuleContainerBootstrapUtils;
import org.mule.util.CollectionUtils;
import org.mule.util.StringUtils;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
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

    // @GuardedBy #lock
    private ObservableList<Application> applications = new ObservableList<Application>();

    public DeploymentService()
    {
        deployer = new DefaultMuleDeployer(this);
        appFactory = new ApplicationFactory(this);
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

        if (!explicitAppSet)
        {
            // explode any app zips first
            final String[] zips = appsDir.list(new SuffixFileFilter(".zip"));
            Arrays.sort(zips);
            for (String zip : zips)
            {
                try
                {
                    // we don't care about the returned app object on startup
                    deployer.installFromAppDir(zip);
                }
                catch (IOException e)
                {
                    // TODO logging
                    e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
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
                a = appFactory.createApp(app);
                applications.add(a);
            }
            catch (IOException e)
            {
                // TODO logging
                e.printStackTrace();
            }
        }


        for (Application application : applications)
        {
            try
            {
                deployer.deploy(application);
            }
            catch (Throwable t)
            {
                // TODO logging
                t.printStackTrace();
            }
        }

        // only start the monitor thread if we launched in default mode without explicitly
        // stated applications to launch
        if (!explicitAppSet)
        {
            scheduleChangeMonitor(appsDir);
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
            logger.info("Application directory check interval: " + reloadIntervalMs);
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
                // TODO logging
                t.printStackTrace();
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

        deployer.undeploy(app);
    }

    public void undeploy(String appName)
    {
        Application app = (Application) CollectionUtils.find(applications, new BeanPropertyValueEqualsPredicate("appName", appName));
        applications.remove(app);
        deployer.undeploy(app);
    }

    public void deploy(URL appArchiveUrl) throws IOException
    {
        final Application application = deployer.installFrom(appArchiveUrl);
        applications.add(application);
        deployer.deploy(application);
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
                    try
                    {
                        // check if this app is running first, undeploy it then
                        final String appName = StringUtils.removeEnd(zip, ".zip");
                        Application app = (Application) CollectionUtils.find(applications, new BeanPropertyValueEqualsPredicate("appName", appName));
                        if (app != null)
                        {
                            undeploy(appName);
                        }
                        deploy(new File(appsDir, zip).toURI().toURL());
                    }
                    catch (Throwable t)
                    {
                        logger.error("Failed to deploy application archive: " + zip, t);
                    }
                }

                // re-scan exploded apps and update our state, as deploying Mule app archives might have added some
                if (zips.length > 0 || dirty)
                {
                    apps = appsDir.list(DirectoryFileFilter.DIRECTORY);
                }

                Collection deployedAppNames = CollectionUtils.collect(applications, new BeanToPropertyValueTransformer("appName"));

                // new exploded Mule apps
                @SuppressWarnings("unchecked")
                final Collection<String> addedApps = CollectionUtils.subtract(Arrays.asList(apps), deployedAppNames);
                for (String addedApp : addedApps)
                {
                    try
                    {
                        onNewExplodedApplication(addedApp);
                    }
                    catch (Throwable t)
                    {
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
                lock.unlock();
                dirty = false;
            }
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
            deployer.deploy(a);
        }

    }

}
