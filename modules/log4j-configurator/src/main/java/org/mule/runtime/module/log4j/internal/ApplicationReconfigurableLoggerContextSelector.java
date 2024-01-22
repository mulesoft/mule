/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.log4j.internal;

import static org.mule.runtime.deployment.model.internal.artifact.CompositeClassLoaderArtifactFinder.findClassLoader;

import static java.lang.ClassLoader.getSystemClassLoader;
import static java.util.Arrays.asList;

import org.mule.runtime.core.internal.util.CompositeClassLoader;
import org.mule.runtime.deployment.model.api.policy.PolicyTemplateDescriptor;
import org.mule.runtime.module.artifact.activation.internal.classloader.MuleSharedDomainClassLoader;
import org.mule.runtime.module.artifact.api.classloader.ArtifactClassLoader;
import org.mule.runtime.module.artifact.api.classloader.RegionClassLoader;

import java.net.URI;
import java.util.List;

import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.selector.ContextSelector;

/**
 * A {@link ContextSelector} that will use the context for the application once it is deployed. It may be used in environments
 * that deploys only one app (W-12356154).
 *
 * @since 4.7.0
 */
public class ApplicationReconfigurableLoggerContextSelector implements ContextSelector {

  private final MuleLoggerContextFactory loggerContextFactory = new MuleLoggerContextFactory();
  private static final ClassLoader SYSTEM_CLASSLOADER = getSystemClassLoader();
  private LoggerContext containerLoggerContext;

  private boolean reconfigured;

  public ApplicationReconfigurableLoggerContextSelector() {
    // The container logger context is created with no logging separation.
    // This will guarantee that by default this will work as no separation in logs.
    this.containerLoggerContext =
        this.loggerContextFactory.build(SYSTEM_CLASSLOADER, this, false);
  }

  public LoggerContext getContext(String fqcn, ClassLoader loader, boolean currentContext) {
    return containerLoggerContext;
  }

  public LoggerContext getContext(String fqcn, ClassLoader loader, boolean currentContext, URI configLocation) {
    return getContext(fqcn, loader, currentContext);
  }

  public List<LoggerContext> getLoggerContexts() {
    return asList(containerLoggerContext);
  }

  public void removeContext(LoggerContext context) {}

  private static ClassLoader getLoggerClassLoader(ClassLoader loggerClassLoader) {
    if (loggerClassLoader instanceof CompositeClassLoader) {
      return getLoggerClassLoader(findClassLoader((CompositeClassLoader) loggerClassLoader));
    }

    // Obtains the first artifact class loader in the hierarchy
    while (!(loggerClassLoader instanceof ArtifactClassLoader) && loggerClassLoader != null) {
      loggerClassLoader = loggerClassLoader.getParent();
    }

    if (loggerClassLoader == null) {
      loggerClassLoader = SYSTEM_CLASSLOADER;
    } else if (isRegionClassLoaderMember(loggerClassLoader)) {
      loggerClassLoader =
          isPolicyClassLoader(loggerClassLoader.getParent()) ? loggerClassLoader.getParent().getParent()
              : loggerClassLoader.getParent();
    } else if (!(loggerClassLoader instanceof RegionClassLoader)
        && !(loggerClassLoader instanceof MuleSharedDomainClassLoader)) {
      loggerClassLoader = SYSTEM_CLASSLOADER;
    }
    return loggerClassLoader;
  }

  private static boolean isPolicyClassLoader(ClassLoader loggerClassLoader) {
    return ((ArtifactClassLoader) loggerClassLoader).getArtifactDescriptor() instanceof PolicyTemplateDescriptor;
  }

  private static boolean isRegionClassLoaderMember(ClassLoader classLoader) {
    return !(classLoader instanceof RegionClassLoader) && classLoader.getParent() instanceof RegionClassLoader;
  }

  public void reconfigureAccordingToClassloader(ClassLoader classloader) {
    LoggerContext applicationClassLoaderLoggerContext = this.loggerContextFactory
        .build(classloader, this, true);

    applicationClassLoaderLoggerContext.reconfigure();

    // We reconfigure the loggers that were already provided to get the app log4j configuration.
    containerLoggerContext.updateLoggers(applicationClassLoaderLoggerContext.getConfiguration());

    // We change the configuration for the logger context. This only sets the configuration.
    containerLoggerContext.reconfigure(applicationClassLoaderLoggerContext.getConfiguration());

    // This is needed so that the configuration set is reconfigured.
    containerLoggerContext.reconfigure();

    containerLoggerContext = applicationClassLoaderLoggerContext;
  }
}
