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

import java.io.IOException;
import java.util.Hashtable;

import org.apache.log4j.BasicConfigurator;
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

    private Hashtable<ClassLoader, LoggerRepository> repos = new Hashtable<ClassLoader, LoggerRepository>();

    public LoggerRepository getLoggerRepository()
    {
        final ClassLoader ccl = Thread.currentThread().getContextClassLoader();

        // TODO don't use classloader, but rather an app name
        LoggerRepository repository = repos.get(ccl);
        if (repository == null)
        {
            final RootLogger root = new RootLogger(Level.DEBUG);
            repository = new Hierarchy(root);
            repos.put(ccl, repository);

            try
            {
                RollingFileAppender appender = new RollingFileAppender( new PatternLayout( "%d - %m%n"), "proggy_log" + ccl.hashCode() + ".txt", true);
                appender.setMaxBackupIndex( 1 );
                appender.setMaximumFileSize( 1000000 );

                root.addAppender(appender);
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
