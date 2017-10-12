/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.launcher.log4j2;

import static java.lang.Boolean.parseBoolean;
import static java.lang.String.format;
import static java.util.zip.Deflater.NO_COMPRESSION;
import static org.apache.logging.log4j.core.appender.rolling.DefaultRolloverStrategy.createStrategy;
import static org.apache.logging.log4j.core.appender.rolling.TimeBasedTriggeringPolicy.createPolicy;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.core.api.config.MuleDeploymentProperties.MULE_MUTE_APP_LOGS_DEPLOYMENT_PROPERTY;
import static org.mule.runtime.core.api.config.MuleProperties.MULE_FORCE_CONSOLE_LOG;
import static org.mule.runtime.core.privileged.event.PrivilegedEvent.CORRELATION_ID_MDC_KEY;
import static org.mule.runtime.module.reboot.api.MuleContainerBootstrapUtils.getMuleBase;
import static org.mule.runtime.module.reboot.api.MuleContainerBootstrapUtils.getMuleConfDir;

import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.core.api.util.ClassUtils;
import org.mule.runtime.core.api.util.FileUtils;
import org.mule.runtime.module.artifact.api.classloader.ArtifactClassLoader;
import org.mule.runtime.module.artifact.api.classloader.ShutdownListener;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptor;

import java.io.File;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.List;
import java.util.Properties;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.ConsoleAppender;
import org.apache.logging.log4j.core.appender.FileAppender;
import org.apache.logging.log4j.core.appender.RandomAccessFileAppender;
import org.apache.logging.log4j.core.appender.RollingFileAppender;
import org.apache.logging.log4j.core.appender.rolling.RolloverStrategy;
import org.apache.logging.log4j.core.appender.rolling.TriggeringPolicy;
import org.apache.logging.log4j.core.config.AbstractConfiguration;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.ConfigurationListener;
import org.apache.logging.log4j.core.config.ConfiguratonFileWatcher;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.config.Reconfigurable;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.logging.log4j.core.util.FileWatcher;

/**
 * This component grabs a {link MuleLoggerContext} which has just been created reading a configuration file and applies
 * configuration changes to it so that it complies with mule's logging strategy.
 * <p/>
 * Its basic functions are:
 * <ul>
 * <li>Disable log4j's shutdown hook so that it doesn't collide with mule's {@link ShutdownListener}, which would result in a
 * classloader leak.</li>
 * <li>When using a default configuration (one which doesn't come from a config file), the console appender is removed</li>
 * <li>if the classloader is an {@link ArtifactClassLoader}, then it adds a rolling file appender to collect the artifact's
 * logs</li>
 * <li>if the configuration did not include a monitorInterval, then one is set to a default value of 60</li>
 * <li>if the context is standalone, then it adds a rolling file appender associated to the artifact</li>
 * <li>if the context is not standalone, then it just logs to a file named mule-main.log</li>
 * </ul>
 *
 * @since 3.6.0
 */
final class LoggerContextConfigurer {

  private static final String MULE_APP_LOG_FILE_TEMPLATE = "mule-app-%s.log";
  private static final String MULE_DOMAIN_LOG_FILE_TEMPLATE = "mule-domain-%s.log";
  private static final String PATTERN_LAYOUT = "%-5p %d [%t] %X{" + CORRELATION_ID_MDC_KEY + "}%c: %m%n";
  private static final int DEFAULT_MONITOR_INTERVAL_SECS = 60;
  static final String FORCED_CONSOLE_APPENDER_NAME = "Forced-Console";
  static final String PER_APP_FILE_APPENDER_NAME = "defaultFileAppender";

  protected void configure(MuleLoggerContext context) {
    disableShutdownHook(context);
    configureMonitor(context);
  }

  protected void update(MuleLoggerContext context) {
    if (!shouldConfigureContext(context)) {
      return;
    }

    boolean forceConsoleLog = System.getProperty(MULE_FORCE_CONSOLE_LOG) != null;
    if (context.getConfigFile() == null && !forceConsoleLog) {
      removeConsoleAppender(context);
    }

    if (context.isArtifactClassloader()) {
      addDefaultArtifactContext(context);
    } else if (!context.isStandlone()) {
      addDefaultAppender(context, "mule-main.log");
    }

    if (forceConsoleLog && !hasAppender(context, ConsoleAppender.class)) {
      forceConsoleAppender(context);
    }
  }

  private boolean shouldConfigureContext(MuleLoggerContext context) {
    if (!context.isApplicationClassloader()) {
      return true;
    }

    ArtifactDescriptor descriptor = context.getArtifactDescriptor();
    if (descriptor == null || !descriptor.getDeploymentProperties().isPresent()) {
      return true;
    }
    Properties properties = descriptor.getDeploymentProperties().get();
    return !parseBoolean(properties.getProperty(MULE_MUTE_APP_LOGS_DEPLOYMENT_PROPERTY, "false"));
  }

  private void disableShutdownHook(LoggerContext context) {
    try {
      ClassUtils.setFieldValue(context.getConfiguration(), "isShutdownHookEnabled", false, true);
    } catch (Exception e) {
      throw new MuleRuntimeException(createStaticMessage("Could not configure shutdown hook. Unexpected configuration type"), e);
    }

  }

  private void configureMonitor(MuleLoggerContext context) {
    Configuration configuration = context.getConfiguration();
    File configFile = null;
    if (context.getConfigFile() != null) {
      configFile = new File(context.getConfigFile().getPath());
    } else if (!StringUtils.isEmpty(configuration.getName())) {
      configFile = new File(configuration.getName());
    }

    if (configFile != null && configuration instanceof Reconfigurable) {
      configuration.getWatchManager().setIntervalSeconds(DEFAULT_MONITOR_INTERVAL_SECS);
      FileWatcher watcher = new ConfiguratonFileWatcher((Reconfigurable) configuration, getListeners(configuration));
      configuration.getWatchManager().watchFile(configFile, watcher);
    }
  }

  private List<ConfigurationListener> getListeners(Configuration configuration) {
    try {
      return ClassUtils.getFieldValue(configuration, "listeners", true);
    } catch (Exception e) {
      throw new MuleRuntimeException(createStaticMessage("Could not get listeners. Unexpected configuration type"),
                                     e);
    }
  }

  private void addDefaultAppender(MuleLoggerContext context, String logFilePath) {
    RollingFileAppender appender =
        createRollingFileAppender(logFilePath, ".%d{yyyy-MM-dd}", PER_APP_FILE_APPENDER_NAME, context.getConfiguration());
    doAddAppender(context, appender);
  }

  private void forceConsoleAppender(MuleLoggerContext context) {
    Appender appender = ConsoleAppender.createAppender(createLayout(context.getConfiguration()), null, null,
                                                       FORCED_CONSOLE_APPENDER_NAME, null, null);
    doAddAppender(context, appender);
  }

  private void doAddAppender(LoggerContext context, Appender appender) {
    appender.start();
    context.getConfiguration().addAppender(appender);
    getRootLogger(context).addAppender(appender, Level.ALL, null);
  }

  private RollingFileAppender createRollingFileAppender(String logFilePath, String filePattern, String appenderName,
                                                        Configuration configuration) {
    TriggeringPolicy triggeringPolicy = createPolicy("1", "true");
    RolloverStrategy rolloverStrategy =
        createStrategy("30", "1", null, String.valueOf(NO_COMPRESSION), null, true, configuration);

    return RollingFileAppender.createAppender(logFilePath, logFilePath + filePattern, "true", appenderName, "true", null, null,
                                              triggeringPolicy, rolloverStrategy, createLayout(configuration), null, null, null,
                                              null, configuration);
  }

  private Layout<? extends Serializable> createLayout(Configuration configuration) {
    return PatternLayout.createLayout(PATTERN_LAYOUT, null, configuration, null, null, true, false, null, null);
  }


  private void addDefaultArtifactContext(MuleLoggerContext context) {
    if (context.isStopping()) {
      // Avoid creating new appenders when the logging context is being stopped
      return;
    }

    String logFileNameTemplate = getFilenamePattern(context);

    if (logFileNameTemplate == null) {
      return;
    }

    String artifactName = context.getArtifactName();

    String logName = format(logFileNameTemplate, (artifactName != null ? artifactName : ""));
    File logDir = new File(getMuleBase(), "logs");
    File logFile = new File(logDir, logName);

    if (context.getConfigLocation() == null) {
      addDefaultAppender(context, logFile.getAbsolutePath());
    } else if (!hasFileAppender(context) && isUrlInsideDirectory(context.getConfigFile(), getMuleConfDir())
        || context.getConfiguration().getAppenders().isEmpty()) {
      // If the artifact logging is configured using the global config file and there is no file appender for the artifact, then
      // configure a default one. Same if there is no appender configured
      addDefaultAppender(context, logFile.getAbsolutePath());
      removeConsoleAppender(context);
    }
  }

  private void removeConsoleAppender(LoggerContext context) {
    for (Appender appender : getRootLogger(context).getAppenders().values()) {
      if (appender instanceof ConsoleAppender) {
        removeAppender(context, appender);
        getRootLogger(context).removeAppender(appender.getName());
      }
    }
  }

  private boolean hasFileAppender(LoggerContext context) {
    return hasAppender(context, FileAppender.class, RollingFileAppender.class, RandomAccessFileAppender.class);
  }

  private boolean hasAppender(LoggerContext context, Class<? extends Appender>... appenderTypes) {
    for (Appender appender : getRootLogger(context).getAppenders().values()) {
      for (Class<? extends Appender> appenderType : appenderTypes) {
        if (appenderType.isInstance(appender)) {
          return true;
        }
      }
    }

    return false;
  }

  private boolean isUrlInsideDirectory(URI uri, File directory) {
    if (uri == null) {
      return false;
    }

    URL url;
    try {
      url = uri.toURL();
    } catch (MalformedURLException e) {
      throw new MuleRuntimeException(createStaticMessage("Could not locate file " + uri), e);
    }

    if (directory != null && FileUtils.isFile(url)) {
      File uriFile = new File(uri);
      return directory.equals(uriFile.getParentFile());
    }

    return false;
  }

  private String getFilenamePattern(MuleLoggerContext context) {
    if (context.isArtifactClassloader()) {
      return context.isApplicationClassloader() ? MULE_APP_LOG_FILE_TEMPLATE : MULE_DOMAIN_LOG_FILE_TEMPLATE;
    }

    return null;
  }

  private LoggerConfig getRootLogger(LoggerContext context) {
    return context.getConfiguration().getRootLogger();
  }

  private void removeAppender(LoggerContext context, Appender appender) {
    ((AbstractConfiguration) context.getConfiguration()).removeAppender(appender.getName());
  }
}
