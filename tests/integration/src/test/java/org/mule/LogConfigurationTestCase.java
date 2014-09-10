/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import org.mule.api.MuleException;
import org.mule.module.launcher.application.Application;
import org.mule.module.launcher.log4j.ArtifactAwareRepositorySelector;
import org.mule.test.infrastructure.deployment.AbstractFakeMuleServerTestCase;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import org.apache.log4j.Appender;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Test;

/**
 * Checks log4j configuration for application and domains
 */
public class LogConfigurationTestCase extends AbstractFakeMuleServerTestCase
{

    public static final String APP_NAME = "app1";
    public static final String DOMAIN_NAME = "domain";

    @Test
    public void defaultAppLoggingConfigurationOnlyLogsOnApplicationLogFile() throws IOException, MuleException
    {
        muleServer.start();
        muleServer.deploy("/log/emptyApp.zip", APP_NAME);
        ensureOnlyDefaultAppender();
    }

    @Test
    public void defaultAppInDomainLoggingConfigurationOnlyLogsOnApplicationLogFile() throws IOException, MuleException
    {
        muleServer.start();
        muleServer.deployDomainFromClasspathFolder("log/empty-domain", DOMAIN_NAME);
        muleServer.deploy("/log/appInDomain.zip", APP_NAME);
        ensureOnlyDefaultAppender();
    }

    @Test
    public void honorLog4jConfigFileForApp() throws IOException, MuleException
    {
        muleServer.start();
        muleServer.deploy("/log/appWithLog4j.zip", APP_NAME);
        ensureArtifactAppender("consoleForApp");
    }

    @Test
    public void honorLog4jConfigFileForAppInDomain() throws IOException, MuleException
    {
        muleServer.start();
        muleServer.deployDomainFromClasspathFolder("log/empty-domain-with-log4j", DOMAIN_NAME);
        muleServer.deploy("/log/appInDomain.zip", APP_NAME);
        ensureArtifactAppender("consoleForDomain");
    }

    private void ensureOnlyDefaultAppender()
    {
        Logger logger = getRootLoggerForApp(APP_NAME);

        assertEquals(1, appendersCount(logger));
        assertEquals(1, selectByClass(logger, FileAppender.class).size());

        FileAppender fileAppender = (FileAppender) selectByClass(logger, FileAppender.class).get(0);
        assertEquals("defaultFileAppender", fileAppender.getName());
        assertTrue(fileAppender.getFile().contains(String.format(ArtifactAwareRepositorySelector.MULE_APP_LOG_FILE_TEMPLATE, APP_NAME)));
    }

    private void ensureArtifactAppender(String appenderName)
    {
        Logger logger = getRootLoggerForApp(APP_NAME);
        assertEquals(Level.DEBUG, logger.getEffectiveLevel());

        assertEquals(1, appendersCount(logger));
        assertEquals(1, selectByClass(logger, ConsoleAppender.class).size());
        assertEquals(appenderName, selectByClass(logger, ConsoleAppender.class).get(0).getName());
    }

    private Logger getRootLoggerForApp(String appName)
    {
        Application app = muleServer.findApplication(appName);
        ClassLoader ccl = Thread.currentThread().getContextClassLoader();
        try
        {
            Thread.currentThread().setContextClassLoader(app.getMuleContext().getExecutionClassLoader());
            return Logger.getLogger(app.getClass()).getLoggerRepository().getRootLogger();
        }
        finally
        {
            Thread.currentThread().setContextClassLoader(ccl);
        }
    }

    private List<Appender> selectByClass(Logger root, Class<?> appenderClass)
    {
        List<Appender> filteredAppenders = new ArrayList<Appender>();
        Enumeration appenders = root.getAllAppenders();
        while (appenders.hasMoreElements())
        {
            Appender appender = (Appender) appenders.nextElement();
            if (appenderClass.isAssignableFrom(appender.getClass()))
            {
                filteredAppenders.add(appender);
            }
        }
        return filteredAppenders;
    }

    private int appendersCount(Logger root)
    {
        return selectByClass(root, Appender.class).size();
    }

}
