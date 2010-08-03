/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.launcher;

import org.mule.config.StartupContext;
import org.mule.module.reboot.MuleContainerBootstrapUtils;
import org.mule.util.CollectionUtils;

import java.io.File;
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

/**
 *
 */
public class DefaultMuleDeployer
{
    protected static final int DEFAULT_CHANGES_CHECK_INTERVAL_MS = 5000;
    private List<Application> applications = new ArrayList<Application>();
    protected ScheduledExecutorService watchTimer;

    protected transient final Log logger = LogFactory.getLog(getClass());

    public void deploy()
    {
        // install phase
        final Map<String, Object> options = StartupContext.get().getStartupOptions();
        String appString = (String) options.get("app");

        final File appsDir = MuleContainerBootstrapUtils.getMuleAppsFile();
        String[] apps;
        if (appString == null)
        {
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
            final ApplicationWrapper<Map<String, Object>> a = new ApplicationWrapper<Map<String, Object>>(new DefaultMuleApplication(app));
            a.setMetaData(options);
            applications.add(a);
        }

        for (Application application : applications)
        {
            try
            {
                application.install();
                application.init();
                application.start();
            }
            catch (Throwable t)
            {
                // TODO logging
                t.printStackTrace();
            }
        }

        scheduleAppMonitor(new AppDirFileWatcher(appsDir));

    }

    public void undeploy()
    {
        if (watchTimer != null)
        {
            watchTimer.shutdownNow();
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

    protected void scheduleAppMonitor(AppDirFileWatcher watcher)
    {
        final int reloadIntervalMs = DEFAULT_CHANGES_CHECK_INTERVAL_MS;
        watchTimer = Executors.newSingleThreadScheduledExecutor(new AppDeployerMonitorThreadFactory());

        // TODO based on the final design, pass in 0 for initial delay for immediate first-time execution
        watchTimer.scheduleWithFixedDelay(watcher, reloadIntervalMs, reloadIntervalMs, TimeUnit.MILLISECONDS);

        if (logger.isInfoEnabled())
        {
            logger.info("Application directory check interval: " + reloadIntervalMs);
        }
    }

    protected class AppDirFileWatcher implements Runnable
    {
        protected File appsDir;

        protected String[] deployedApps = new String[0];


        public AppDirFileWatcher(File appsDir)
        {
            this.appsDir = appsDir;
        }

        public void run()
        {
            // list new apps
            final String[] zips = appsDir.list(new SuffixFileFilter(".zip"));
            final String[] apps = appsDir.list(DirectoryFileFilter.DIRECTORY);

            final Collection removedApps = CollectionUtils.subtract(Arrays.asList(deployedApps), Arrays.asList(apps));
            final Collection addedApps = CollectionUtils.subtract(Arrays.asList(apps), Arrays.asList(deployedApps));
            // list depoyed apps to compare with a previous run
            if (zips.length > 0)
            {
                // TODO only 1st for now
                onChange(new File(appsDir, zips[0]));
            }

            deployedApps = apps;
        }

        protected synchronized void onChange(File file)
        {
            if (logger.isInfoEnabled())
            {
                logger.info("================== New Application " + file);
            }
        }
    }

}
