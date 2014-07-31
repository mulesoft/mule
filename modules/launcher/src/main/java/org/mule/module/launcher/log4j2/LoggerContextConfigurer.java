/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.launcher.log4j2;

import org.mule.api.MuleRuntimeException;
import org.mule.config.i18n.MessageFactory;
import org.mule.module.launcher.application.ApplicationClassLoader;
import org.mule.module.launcher.artifact.ArtifactClassLoader;
import org.mule.module.reboot.MuleContainerBootstrapUtils;
import org.mule.util.ClassUtils;
import org.mule.util.FileUtils;

import java.io.File;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.List;
import java.util.zip.Deflater;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.ConsoleAppender;
import org.apache.logging.log4j.core.appender.FileAppender;
import org.apache.logging.log4j.core.appender.RandomAccessFileAppender;
import org.apache.logging.log4j.core.appender.RollingFileAppender;
import org.apache.logging.log4j.core.appender.rolling.DefaultRolloverStrategy;
import org.apache.logging.log4j.core.appender.rolling.RolloverStrategy;
import org.apache.logging.log4j.core.appender.rolling.TimeBasedTriggeringPolicy;
import org.apache.logging.log4j.core.appender.rolling.TriggeringPolicy;
import org.apache.logging.log4j.core.config.AbstractConfiguration;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.ConfigurationListener;
import org.apache.logging.log4j.core.config.FileConfigurationMonitor;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.config.Reconfigurable;
import org.apache.logging.log4j.core.layout.PatternLayout;

/**
 * This component grabs a {link MuleLoggerContext} which has just been created reading a configuration file
 * and applies configuration changes to it so that it complies with mule's logging strategy.
 * <p/>
 * Its basic functions are:
 * <ul>
 * <li>Disable log4j's shutdown hook so that it doesn't collide with mule's {@link org.mule.module.launcher.artifact.ShutdownListener},
 * which would result in a classloader leak.</li>
 * <li>When using a default configuration (one which doesn't come from a config file), the console appender is removed</li>
 * <li>if the classloader is an {@link org.mule.module.launcher.artifact.ArtifactClassLoader}, then it adds a rolling file appender
 * to collect the artifact's logs</li>
 * <li>if the configuration did not include a monitorInterval, then one is set to a default value of 60</li>
 * <li>if the context is standalone, then it adds a rolling file appender associated to the artifact</li>
 * <li>if the context is not standalone, then it just logs to a file named mule-main.log</li>
 * </ul>
 *
 * @since 3.6.0
 */
final class LoggerContextConfigurer
{
    private static final String MULE_APP_LOG_FILE_TEMPLATE = "mule-app-%s.log";
    private static final String MULE_DOMAIN_LOG_FILE_TEMPLATE = "mule-domain-%s.log";
    private static final String PATTERN_LAYOUT = "%-5p %d [%t] %c: %m%n";
    private static final int DEFAULT_MONITOR_INTERVAL_SECS = 60;

    LoggerContextConfigurer()
    {
    }

    protected void configure(MuleLoggerContext context)
    {
        disableShutdownHook(context);
        configureMonitor(context);


        ClassLoader classLoader = context.getOwnerClassLoader();

        if (context.getConfigFile() == null)
        {
            removeConsoleAppender(context);
        }

        if (classLoader instanceof ArtifactClassLoader)
        {
            addDefaultArtifactContext(context);
        }
        else if (!context.isStandlone())
        {
            addDefaultAppender(context, "mule-main.log");
        }
    }

    private void disableShutdownHook(LoggerContext context)
    {
        try
        {
            ClassUtils.setFieldValue(context.getConfiguration(), "isShutdownHookEnabled", false, true);
        }
        catch (Exception e)
        {
            throw new MuleRuntimeException(MessageFactory.createStaticMessage("Could not configure shutdown hook. Unexpected configuration type"), e);
        }

    }

    private void configureMonitor(MuleLoggerContext context)
    {
        Configuration configuration = context.getConfiguration();
        if (!(configuration.getConfigurationMonitor() instanceof FileConfigurationMonitor))
        {
            File configFile = null;
            if (context.getConfigFile() != null)
            {
                configFile = new File(context.getConfigFile().getPath());
            }
            else if (!StringUtils.isEmpty(configuration.getName()))
            {
                configFile = new File(configuration.getName());
            }

            if (configFile != null)
            {
                configuration.setConfigurationMonitor(new FileConfigurationMonitor(
                        (Reconfigurable) configuration,
                        configFile,
                        getListeners(configuration),
                        DEFAULT_MONITOR_INTERVAL_SECS));
            }
        }
    }

    private List<ConfigurationListener> getListeners(Configuration configuration)
    {
        try
        {
            return ClassUtils.getFieldValue(configuration, "listeners", true);
        }
        catch (Exception e)
        {
            throw new MuleRuntimeException(MessageFactory.createStaticMessage("Could not get listeners. Unexpected configuration type"), e);
        }
    }

    private void addDefaultAppender(MuleLoggerContext context, String logFilePath)
    {
        RollingFileAppender appender = createRollingFileAppender(logFilePath, "'.'%d{yyyy-MM-dd}", "defaultFileAppender", context.getConfiguration());
        appender.start();

        context.getConfiguration().addAppender(appender);
        getRootLogger(context).addAppender(appender, Level.INFO, null);
    }

    private RollingFileAppender createRollingFileAppender(String logFilePath, String filePattern, String appenderName, Configuration configuration)
    {
        Layout<? extends Serializable> layout = PatternLayout.createLayout(PATTERN_LAYOUT, configuration, null, null, true, false, null, null);
        TriggeringPolicy triggeringPolicy = TimeBasedTriggeringPolicy.createPolicy("1", "true");
        RolloverStrategy rolloverStrategy = DefaultRolloverStrategy.createStrategy("30", "1", null, String.valueOf(Deflater.NO_COMPRESSION), configuration);

        return RollingFileAppender.createAppender(logFilePath,
                                                  logFilePath + filePattern,
                                                  "true",
                                                  appenderName,
                                                  "true",
                                                  null, null,
                                                  triggeringPolicy,
                                                  rolloverStrategy,
                                                  layout,
                                                  null, null, null, null,
                                                  configuration);
    }

    private void addDefaultArtifactContext(MuleLoggerContext context)
    {
        String artifactName = ((ArtifactClassLoader) context.getOwnerClassLoader()).getArtifactName();
        String logFileNameTemplate = getFilenamePattern(context.getOwnerClassLoader());

        String logName = String.format(logFileNameTemplate, (artifactName != null ? artifactName : ""));
        File logDir = new File(MuleContainerBootstrapUtils.getMuleHome(), "logs");
        File logFile = new File(logDir, logName);

        if (context.getConfigLocation() == null)
        {

            addDefaultAppender(context, logFile.getAbsolutePath());
        }
        else
        {
            // If the artifact logging is configured using the global config file and there is no file appender for the artifact, then configure a default one
            if (isUrlInsideDirectory(context.getConfigFile(), MuleContainerBootstrapUtils.getMuleConfDir()))
            {
                if (!hasFileAppender(context))
                {
                    addDefaultAppender(context, logFile.getAbsolutePath());
                    removeConsoleAppender(context);
                }
            }
        }
    }

    private void removeConsoleAppender(LoggerContext context)
    {
        for (Appender appender : getRootLogger(context).getAppenders().values())
        {
            if (appender instanceof ConsoleAppender)
            {
                removeAppender(context, appender);
                getRootLogger(context).removeAppender(appender.getName());

                break;
            }
        }
    }

    private boolean hasFileAppender(LoggerContext context)
    {
        for (Appender appender : getRootLogger(context).getAppenders().values())
        {
            if (appender instanceof FileAppender ||
                appender instanceof RollingFileAppender ||
                appender instanceof RandomAccessFileAppender)
            {
                return true;
            }
        }

        return false;
    }

    private boolean isUrlInsideDirectory(URI uri, File directory)
    {
        if (uri == null)
        {
            return false;
        }

        URL url;
        try
        {
            url = uri.toURL();
        }
        catch (MalformedURLException e)
        {
            throw new MuleRuntimeException(MessageFactory.createStaticMessage("Could not locate file " + uri), e);
        }

        if (directory != null && FileUtils.isFile(url))
        {
            File urlFile = new File(url.getFile());
            return directory.equals(urlFile.getParentFile());
        }

        return false;
    }

    private String getFilenamePattern(ClassLoader classLoader)
    {
        if (classLoader instanceof ArtifactClassLoader)
        {
            return classLoader instanceof ApplicationClassLoader ? MULE_APP_LOG_FILE_TEMPLATE : MULE_DOMAIN_LOG_FILE_TEMPLATE;
        }

        return null;
    }

    private LoggerConfig getRootLogger(LoggerContext context)
    {
        return ((AbstractConfiguration) context.getConfiguration()).getRootLogger();
    }

    private void removeAppender(LoggerContext context, Appender appender)
    {
        ((AbstractConfiguration) context.getConfiguration()).removeAppender(appender.getName());
    }
}
