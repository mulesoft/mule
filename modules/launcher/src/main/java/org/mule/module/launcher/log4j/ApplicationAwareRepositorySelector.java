/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.launcher.log4j;

import org.mule.module.launcher.MuleApplicationClassLoader;
import org.mule.module.reboot.MuleContainerBootstrapUtils;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.log4j.Hierarchy;
import org.apache.log4j.Level;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.RollingFileAppender;
import org.apache.log4j.spi.LoggerRepository;
import org.apache.log4j.spi.RepositorySelector;
import org.apache.log4j.spi.RootLogger;

public class ApplicationAwareRepositorySelector implements RepositorySelector
{

    protected static final String PATTERN_LAYOUT = "%-5p %d [%t] %c: %m%n";
    private ConcurrentMap<Integer, LoggerRepository> repository = new ConcurrentHashMap<Integer, LoggerRepository>();

    public LoggerRepository getLoggerRepository()
    {
        final ClassLoader ccl = Thread.currentThread().getContextClassLoader();

        LoggerRepository repository = this.repository.get(ccl.hashCode());
        if (repository == null)
        {
            final RootLogger root = new RootLogger(Level.INFO);
            repository = new Hierarchy(root);

            try
            {
                if (ccl instanceof MuleApplicationClassLoader)
                {
                    MuleApplicationClassLoader muleCL = (MuleApplicationClassLoader) ccl;
                    // check if there's an app-specific logging configuration available,
                    // scope the lookup to this classloader only, as getResource() will delegate to parents
                    final URL appLogConfig = muleCL.findResource("log4j.properties");
                    if (appLogConfig == null)
                    {
                        // fallback to defaults
                        String logName = String.format("mule-app-%s.log", muleCL.getAppName());
                        File logDir = new File(MuleContainerBootstrapUtils.getMuleHome(), "logs");
                        File logFile = new File(logDir, logName);
                        RollingFileAppender fileAppender = new RollingFileAppender(new PatternLayout(PATTERN_LAYOUT), logFile.getAbsolutePath(), true);
                        fileAppender.setMaxBackupIndex(100);
                        fileAppender.setMaximumFileSize(1000000);
                        fileAppender.activateOptions();
                        root.addAppender(fileAppender);
                    }
                    else
                    {
                        new PropertyConfigurator().doConfigure(appLogConfig, repository);
                    }
                }
                else
                {
                    // this is not an app init, but a Mule container, use the top-level defaults
                    final File defaultSystemLog = new File(MuleContainerBootstrapUtils.getMuleHome(), "conf/log4j.properties");
                    new PropertyConfigurator().doConfigure(defaultSystemLog.getAbsolutePath(), repository);
                }

                final LoggerRepository previous = this.repository.putIfAbsent(ccl.hashCode(), repository);
                if (previous != null)
                {
                    repository = previous;
                }
            }
            catch (IOException e)
            {
                throw new RuntimeException(e);
            }
        }

        return repository;
    }
}
