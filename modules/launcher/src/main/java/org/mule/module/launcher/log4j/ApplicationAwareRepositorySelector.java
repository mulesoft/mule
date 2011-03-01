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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Hierarchy;
import org.apache.log4j.Level;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.RollingFileAppender;
import org.apache.log4j.spi.LoggerRepository;
import org.apache.log4j.spi.RepositorySelector;
import org.apache.log4j.spi.RootLogger;

public class ApplicationAwareRepositorySelector implements RepositorySelector
{

    protected static final String PATTERN_LAYOUT = "%-5p %d [%t] %c: %m%n";
    private ConcurrentMap<ClassLoader, LoggerRepository> repos = new ConcurrentHashMap<ClassLoader, LoggerRepository>();

    public LoggerRepository getLoggerRepository()
    {
        final ClassLoader ccl = Thread.currentThread().getContextClassLoader();

        LoggerRepository repository = repos.get(ccl);
        if (repository == null)
        {
            final RootLogger root = new RootLogger(Level.INFO);
            repository = new Hierarchy(root);

            try
            {
                String logName;
                if (ccl instanceof MuleApplicationClassLoader)
                {
                    MuleApplicationClassLoader muleCL = (MuleApplicationClassLoader) ccl;
                    logName = "-app-" + muleCL.getAppName();
                    File logDir = new File(MuleContainerBootstrapUtils.getMuleHome(), "logs");
                    File logFile = new File(logDir, "mule" + logName + ".log");
                    RollingFileAppender fileAppender = new RollingFileAppender(new PatternLayout(PATTERN_LAYOUT), logFile.getAbsolutePath(), true);
                    fileAppender.setMaxBackupIndex(1);
                    fileAppender.setMaximumFileSize(1000000);
                    fileAppender.activateOptions();
                    root.addAppender(fileAppender);
                }
                else
                {
                    // container logger handled by the wrapper, just output to the sys.out
                    final ConsoleAppender appender = new ConsoleAppender(new PatternLayout(PATTERN_LAYOUT));
                    appender.activateOptions();
                    root.addAppender(appender);
                }

                final LoggerRepository previous = repos.putIfAbsent(ccl, repository);
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
