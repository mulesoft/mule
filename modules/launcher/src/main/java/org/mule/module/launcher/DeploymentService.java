package org.mule.module.launcher;

import org.mule.config.StartupContext;
import org.mule.module.reboot.MuleContainerBootstrapUtils;
import org.mule.util.CollectionUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.io.filefilter.SuffixFileFilter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class DeploymentService
{
    protected static final int DEFAULT_CHANGES_CHECK_INTERVAL_MS = 5000;

    protected ScheduledExecutorService appDirMonitorTimer;
    protected transient final Log logger = LogFactory.getLog(getClass());

    private List<Application> applications = new ArrayList<Application>();
    protected DefaultMuleDeployer deployer = new DefaultMuleDeployer();

    public void start()
    {
        // install phase
        final Map<String, Object> options = StartupContext.get().getStartupOptions();
        String appString = (String) options.get("app");

        final File appsDir = MuleContainerBootstrapUtils.getMuleAppsDir();
        String[] apps;

        // mule -app app1:app2:app3 will restrict deployment only to those specified apps
        final boolean explicitAppSet = appString != null;

        if (!explicitAppSet)
        {
            // explode any app zips first
            final String[] zips = appsDir.list(new SuffixFileFilter(".zip"));
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
        }
        else
        {
            apps = appString.split(":");
        }

        for (String app : apps)
        {
            final ApplicationWrapper a = new ApplicationWrapper(new DefaultMuleApplication(app));
            applications.add(a);
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

        // TODO based on the final design, pass in 0 for initial delay for immediate first-time execution
        appDirMonitorTimer.scheduleWithFixedDelay(new AppDirFileWatcher(appsDir),
                                                  reloadIntervalMs,
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
     * @return immutable applications list
     */
    public List<Application> getApplications()
    {
        return Collections.unmodifiableList(applications);
    }

    /**
     * Not thread safe. Correctness is guaranteed by a single-threaded executor.
     */
    protected class AppDirFileWatcher implements Runnable
    {
        protected File appsDir;

        protected String[] deployedApps;


        public AppDirFileWatcher(File appsDir)
        {
            this.appsDir = appsDir;
            // save the list of known apps on startup
            this.deployedApps = new String[applications.size()];
            for (int i = 0; i < applications.size(); i++)
            {
                deployedApps[i] = applications.get(i).getAppName();

            }
        }

        public void run()
        {
            // list new apps
            final String[] zips = appsDir.list(new SuffixFileFilter(".zip"));
            String[] apps = appsDir.list(DirectoryFileFilter.DIRECTORY);

            // TODO deleting apps not yet implemented
            @SuppressWarnings("unchecked")
            final Collection<String> removedApps = CollectionUtils.subtract(Arrays.asList(deployedApps), Arrays.asList(apps));

            // new packed Mule apps
            for (String zip : zips)
            {
                try
                {
                    onNewApplicationArchive(new File(appsDir, zip));
                }
                catch (Throwable t)
                {
                    logger.error("Failed to deploy application archive: " + zip, t);
                }
            }

            // re-scan exploded apps and update our state, as deploying Mule app archives might have added some
            if (zips.length > 0)
            {
                apps = appsDir.list(DirectoryFileFilter.DIRECTORY);
                deployedApps = apps;
            }

            // new exploded Mule apps
            @SuppressWarnings("unchecked")
            final Collection<String> addedApps = CollectionUtils.subtract(Arrays.asList(apps), Arrays.asList(deployedApps));
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

            deployedApps = apps;
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

            Application a = new ApplicationWrapper(new DefaultMuleApplication(appName));
            // add to the list of known apps first to avoid deployment loop on failure
            applications.add(a);
            deployer.deploy(a);
        }

        protected void onNewApplicationArchive(File file) throws Exception
        {
            if (logger.isInfoEnabled())
            {
                logger.info("================== New Application Archive: " + file);
            }

            Application app = deployer.installFrom(file.toURL());
            // add to the list of known apps first to avoid deployment loop on failure
            applications.add(app);
            deployer.deploy(app);
        }
    }

}
