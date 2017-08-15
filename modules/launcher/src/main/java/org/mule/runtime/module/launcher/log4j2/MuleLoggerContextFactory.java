/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.launcher.log4j2;

import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.module.launcher.log4j2.ArtifactAwareContextSelector.LOGGER;
import static org.mule.runtime.module.reboot.api.MuleContainerBootstrapUtils.getMuleBase;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.module.artifact.api.classloader.ArtifactClassLoader;
import org.mule.runtime.module.artifact.api.classloader.DirectoryResourceLocator;
import org.mule.runtime.module.artifact.api.classloader.LocalResourceLocator;
import org.mule.runtime.deployment.model.api.application.ApplicationDescriptor;
import org.mule.runtime.module.reboot.api.MuleContainerBootstrapUtils;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import org.apache.logging.log4j.core.LoggerContext;

/**
 * Encapsulates the logic to get the proper log configuration.
 * 
 * @since 3.8.0
 */
public class MuleLoggerContextFactory {

  /**
   * Builds a new {@link LoggerContext} for the given {@code classLoader} and {@code selector}
   * 
   * @param classLoader the classloader of the artifact this logger context is for.
   * @param selector the selector to bew used when building the loggers for the new context.
   * @return
   */
  public LoggerContext build(final ClassLoader classLoader, final ArtifactAwareContextSelector selector) {
    NewContextParameters parameters = resolveContextParameters(classLoader);
    if (parameters == null) {
      return getDefaultContext(selector);
    }

    MuleLoggerContext loggerContext =
        new MuleLoggerContext(parameters.contextName, parameters.loggerConfigFile, classLoader, selector, isStandalone());

    if (classLoader instanceof ArtifactClassLoader) {
      final ArtifactClassLoader artifactClassLoader = (ArtifactClassLoader) classLoader;

      artifactClassLoader.addShutdownListener(() -> selector
          .destroyLoggersFor(ArtifactAwareContextSelector.resolveLoggerContextClassLoader(classLoader)));
    }

    return loggerContext;
  }

  private NewContextParameters resolveContextParameters(ClassLoader classLoader) {
    if (classLoader instanceof ArtifactClassLoader) {
      ArtifactClassLoader artifactClassLoader = (ArtifactClassLoader) classLoader;
      return new NewContextParameters(getArtifactLoggingConfig(artifactClassLoader), artifactClassLoader.getArtifactId());
    } else {
      // this is not an app init, use the top-level defaults
      if (MuleContainerBootstrapUtils.getMuleConfDir() != null) {
        return new NewContextParameters(getLogConfig(new DirectoryResourceLocator(MuleContainerBootstrapUtils.getMuleConfDir()
            .getAbsolutePath())), classLoader.toString());
      }
    }

    return null;
  }

  private URI getArtifactLoggingConfig(ArtifactClassLoader muleCL) {
    URI appLogConfig;
    try {
      ApplicationDescriptor appDescriptor = muleCL.getArtifactDescriptor();

      if (appDescriptor.getLogConfigFile() == null) {
        appLogConfig = getLogConfig(muleCL);
      } else if (!appDescriptor.getLogConfigFile().exists()) {
        LOGGER
            .warn("Configured 'log.configFile' in app descriptor points to a non-existant file. Using default configuration.");
        appLogConfig = getLogConfig(muleCL);
      } else {
        appLogConfig = appDescriptor.getLogConfigFile().toURI();
      }
    } catch (Exception e) {
      LOGGER.warn("{} while looking for 'log.configFile' entry in app descriptor: {}. Using default configuration.",
                  e.getClass().getName(), e.getMessage());
      appLogConfig = getLogConfig(muleCL);
    }

    if (appLogConfig != null && LOGGER.isInfoEnabled()) {
      LOGGER.info("Found logging config for application '{}' at '{}'", muleCL.getArtifactId(), appLogConfig);
    }

    return appLogConfig;
  }

  private LoggerContext getDefaultContext(ArtifactAwareContextSelector selector) {
    return new MuleLoggerContext("Default", selector, isStandalone());
  }

  private boolean isStandalone() {
    return MuleContainerBootstrapUtils.getMuleConfDir() != null;
  }

  /**
   * Checks if there's an app-specific logging configuration available, scope the lookup to this classloader only, as
   * getResource() will delegate to parents locate xml config first, fallback to properties format if not found
   * 
   * @param localResourceLocator
   * @return
   */
  private URI getLogConfig(LocalResourceLocator localResourceLocator) {
    URL appLogConfig = localResourceLocator.findLocalResource("log4j2-test.xml");

    if (appLogConfig == null) {
      appLogConfig = localResourceLocator.findLocalResource("log4j2.xml");
    }

    if (appLogConfig == null) {
      File defaultConfigFile = new File(getMuleBase(), "conf");
      defaultConfigFile = new File(defaultConfigFile, "log4j2.xml");

      try {
        appLogConfig = defaultConfigFile.toURI().toURL();
      } catch (MalformedURLException e) {
        throw new MuleRuntimeException(createStaticMessage("Could not locate log config in MULE_HOME"), e);
      }
    }

    try {
      return appLogConfig.toURI();
    } catch (URISyntaxException e) {
      throw new MuleRuntimeException(createStaticMessage("Could not read log file " + appLogConfig), e);
    }
  }

  private class NewContextParameters {

    private final URI loggerConfigFile;
    private final String contextName;

    private NewContextParameters(URI loggerConfigFile, String contextName) {
      this.loggerConfigFile = loggerConfigFile;
      this.contextName = contextName;
    }
  }
}
