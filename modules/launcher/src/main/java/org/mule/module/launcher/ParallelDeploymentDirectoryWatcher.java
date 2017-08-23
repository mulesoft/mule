/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.launcher;

import org.mule.module.launcher.application.Application;
import org.mule.module.launcher.domain.Domain;
import org.mule.module.launcher.util.ObservableList;
import org.mule.util.concurrent.WaitPolicy;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Provides parallel deployment of Mule applications.
 *
 * @since 3.8.2
 */
public class ParallelDeploymentDirectoryWatcher extends DeploymentDirectoryWatcher
{

    private final ThreadPoolExecutor threadPoolExecutor;

    public ParallelDeploymentDirectoryWatcher(ArchiveDeployer<Domain> domainArchiveDeployer, ArchiveDeployer<Application> applicationArchiveDeployer, ObservableList<Domain> domains, ObservableList<Application> applications, ReentrantLock deploymentLock)
    {
        super(domainArchiveDeployer, applicationArchiveDeployer, domains, applications, deploymentLock);
        this.threadPoolExecutor = new ThreadPoolExecutor(0, 20, 5, TimeUnit.SECONDS, new SynchronousQueue(), new WaitPolicy());
    }

    @Override
    protected void deployPackedApps(String[] zips)
    {
        if (zips.length == 0)
        {
            return;
        }

        List<Callable<Object>> tasks = new ArrayList<>(zips.length);
        for (final String zip : zips)
        {
            tasks.add(new Callable<Object>()
            {

                @Override
                public Object call() throws Exception
                {
                    try
                    {
                        applicationArchiveDeployer.deployPackagedArtifact(zip);
                    }
                    catch (Exception e)
                    {
                        // Ignore and continue
                    }
                    return null;
                }
            });
        }

        waitForTasksToFinish(tasks);
    }


    @Override
    protected void deployExplodedApps(String[] apps)
    {
        List<Callable<Object>> tasks = new ArrayList<>(apps.length);

        for (final String addedApp : apps)
        {
            if (applicationArchiveDeployer.isUpdatedZombieArtifact(addedApp))
            {
                tasks.add(new Callable<Object>()
                {

                    @Override
                    public Object call() throws Exception
                    {
                        try
                        {
                            applicationArchiveDeployer.deployExplodedArtifact(addedApp);
                        }
                        catch (Exception e)
                        {
                            // Ignore and continue
                        }
                        return null;
                    }
                });
            }
        }

        if (!tasks.isEmpty())
        {
            waitForTasksToFinish(tasks);
        }
    }

    private void waitForTasksToFinish(List<Callable<Object>> tasks)
    {
        try
        {
            final List<Future<Object>> futures = threadPoolExecutor.invokeAll(tasks);

            for (Future<Object> future : futures)
            {
                try
                {
                    future.get();
                }
                catch (ExecutionException e)
                {
                    // Ignore and continue with the next one
                }
            }

        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
        }
    }
}
