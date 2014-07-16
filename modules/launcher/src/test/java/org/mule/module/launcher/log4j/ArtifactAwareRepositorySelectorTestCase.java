/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.launcher.log4j;


import org.mule.module.launcher.MuleApplicationClassLoader;
import org.mule.module.launcher.application.CompositeApplicationClassLoader;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.probe.PollingProber;
import org.mule.tck.probe.Probe;
import org.mule.tck.probe.Prober;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.spi.LoggerRepository;
import org.apache.log4j.spi.RepositorySelector;
import org.junit.Test;

public class ArtifactAwareRepositorySelectorTestCase extends AbstractMuleTestCase
{
    private static final String TEST_APP_NAME = "TEST";

    @Test
    public void configWatchdogThreadIsDestroyedWhenClosingClassLoader() throws Exception
    {
        Prober prober = new PollingProber(3000, 100);
        final ClassLoader currentClassLoader = Thread.currentThread().getContextClassLoader();

        List<ClassLoader> classLoaders = new ArrayList<ClassLoader>();
        classLoaders.add(currentClassLoader);
        classLoaders.add(new MuleApplicationClassLoader(TEST_APP_NAME, currentClassLoader)
        {
            public URL findLocalResource(String resourceName)
            {
                // This MuleApplicationClassLoader is created without the filesystem structure required for a mule application
                // so we have to obtain the log4j.properties resource from the classpath.
                // This change is related to the new way to search the local resources (MULE-7366)
                if(resourceName!=null && resourceName.startsWith("log4j"))
                {
                    return currentClassLoader.getResource(resourceName);
                }
                return super.findLocalResource(resourceName);
            }
        });

        CompositeApplicationClassLoader compositeClassLoader = new CompositeApplicationClassLoader(TEST_APP_NAME, classLoaders);

        Thread.currentThread().setContextClassLoader(compositeClassLoader);

        RepositorySelector repositorySelector = new ArtifactAwareRepositorySelector();
        LoggerRepository repository = repositorySelector.getLoggerRepository();

        // A config watch dog thread should be started when creating the logger repository with an ApplicationClassLoader

        prober.check(new Probe()
        {
            @Override
            public boolean isSatisfied()
            {
                return isConfigWatchDogThreadRunning();
            }
            @Override
            public String describeFailure()
            {
                return "Config watch dog thread was not started";
            }
        });

        compositeClassLoader.dispose();

        // The config watch dog thread should be stopped when the class loader is closed.

        prober.check(new Probe()
        {
            @Override
            public boolean isSatisfied()
            {
                return !isConfigWatchDogThreadRunning();
            }
            @Override
            public String describeFailure()
            {
                return "Config watch dog thread was not stopped";
            }
        });

    }


    private boolean isConfigWatchDogThreadRunning()
    {
        for (Thread thread : Thread.getAllStackTraces().keySet())
        {
            if (thread instanceof ArtifactAwareRepositorySelector.ConfigWatchDog)
            {
                return true;
            }
        }
        return false;
    }
}
