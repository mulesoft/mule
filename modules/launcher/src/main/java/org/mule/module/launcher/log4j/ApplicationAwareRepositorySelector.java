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
import java.util.Hashtable;

import org.apache.log4j.Hierarchy;
import org.apache.log4j.Level;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.RollingFileAppender;
import org.apache.log4j.spi.LoggerRepository;
import org.apache.log4j.spi.RepositorySelector;
import org.apache.log4j.spi.RootLogger;

public class ApplicationAwareRepositorySelector implements RepositorySelector
{

    private Hashtable<ClassLoader, LoggerRepository> repos = new Hashtable<ClassLoader, LoggerRepository>();

    public LoggerRepository getLoggerRepository()
    {
        final ClassLoader ccl = Thread.currentThread().getContextClassLoader();

        // TODO don't use classloader, but rather an app name
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
                    logName = muleCL.getAppName();
                }
                else
                {
                    logName = "container";
                }
                File logDir = new File(MuleContainerBootstrapUtils.getMuleHome(), "log");
                File logFile = new File(logDir, "mule-" + logName + "-log.txt");
                RollingFileAppender appender = new RollingFileAppender(new PatternLayout("%-5p %d [%t] %c: %m%n"), logFile.getAbsolutePath(), true);
                appender.setMaxBackupIndex(1);
                appender.setMaximumFileSize(1000000);

                root.addAppender(appender);

                repos.put(ccl, repository);
            }
            catch (IOException e)
            {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
            //root.addAppender(new ConsoleAppender(
            //        new PatternLayout(PatternLayout.TTCC_CONVERSION_PATTERN)));
        }

        return repository;
    }
}
