/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.log4j.internal;

import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.selector.ContextSelector;
import org.mule.runtime.core.internal.util.CompositeClassLoader;
import org.mule.runtime.deployment.model.api.policy.PolicyTemplateDescriptor;
import org.mule.runtime.module.artifact.activation.internal.classloader.MuleSharedDomainClassLoader;
import org.mule.runtime.module.artifact.api.classloader.ArtifactClassLoader;
import org.mule.runtime.module.artifact.api.classloader.RegionClassLoader;

import java.net.URI;
import java.util.List;

import static java.lang.ClassLoader.getSystemClassLoader;
import static java.util.Arrays.asList;
import static org.mule.runtime.deployment.model.internal.artifact.CompositeClassLoaderArtifactFinder.findClassLoader;

/**
 * A {@link ContextSelector} for single app mode.
 *
 * @since 4.7.0
 */
public class SingleAppModeContextSelector implements ContextSelector {

  public static final String FILE_APPENDER_NAME = "Console";
  private final MuleLoggerContextFactory loggerContextFactory = new MuleLoggerContextFactory();
  private static final ClassLoader SYSTEM_CLASSLOADER = getSystemClassLoader();
  private final LoggerContext containerLoggerContext;


  private LoggerContext applicationClassLoaderLoggerContext;

  public SingleAppModeContextSelector() {
    this.containerLoggerContext = this.loggerContextFactory.build(SYSTEM_CLASSLOADER, this, false, null);
  }

  public LoggerContext getContext(String fqcn, ClassLoader loader, boolean currentContext) {
    if (applicationClassLoaderLoggerContext != null) {
      return applicationClassLoaderLoggerContext;
    }

    ClassLoader classloader = getLoggerClassLoader(loader);

    if (classloader == SYSTEM_CLASSLOADER) {
      return containerLoggerContext;
    }

    applicationClassLoaderLoggerContext = this.loggerContextFactory
        .build(classloader, this, false, containerLoggerContext.getConfiguration().getAppender(FILE_APPENDER_NAME));
    return applicationClassLoaderLoggerContext;
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
}
