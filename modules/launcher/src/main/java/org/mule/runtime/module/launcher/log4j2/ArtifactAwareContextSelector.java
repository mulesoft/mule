/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.launcher.log4j2;

import static java.lang.ClassLoader.getSystemClassLoader;

import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.deployment.model.api.policy.PolicyTemplateDescriptor;
import org.mule.runtime.deployment.model.internal.domain.MuleSharedDomainClassLoader;
import org.mule.runtime.module.artifact.api.classloader.ArtifactClassLoader;
import org.mule.runtime.module.artifact.api.classloader.RegionClassLoader;
import org.mule.runtime.module.artifact.api.classloader.ShutdownListener;

import java.net.URI;
import java.util.List;

import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.selector.ContextSelector;
import org.apache.logging.log4j.status.StatusLogger;

/**
 * Implementation of {@link org.apache.logging.log4j.core.selector.ContextSelector} which is used to implement log separation
 * based on provided or current {@link java.lang.ClassLoader}
 * <p/>
 * This component is responsible for managing the {@link org.apache.logging.log4j.core.LoggerContext} that corresponds to each
 * artifact (aka applications, domains, container), using its classloader as an identifier. The same classloader always gets the
 * same {@link org.apache.logging.log4j.core.LoggerContext}
 * <p/>
 * This component also overrides log4j2's default algorithm for locating configuration files, although it does it in a way
 * consistent with the replaced behavior:
 * <ul>
 * <li>A file called log4j2-test.xml is fetched from the corresponding search path</li>
 * <li>If log4j2-test.xml is not found, then log4j2.xml is attempted</li>
 * <li>If not found, a default configuration consisting of a single rolling file appender is used</li>
 * <li>The search path is derived from the artifact for which a logging context is being requested, following a child first
 * strategy (artifact - domain - container). Each artifact starts looking in the phase that makes sense for it</li>
 * </ul>
 * <p/>
 * If the classloader is an artifact one, then it adds a {@link ShutdownListener} to destroy the logging context when the app is
 * undeployed, preventing memory leaks.
 * <p/>
 * If mule is running in embedded mode, then all of this logic described above is discarded and it simply logs to a file called
 * mule-main.log
 *
 * @since 3.6.0
 */
class ArtifactAwareContextSelector implements ContextSelector, Disposable {

  static final StatusLogger LOGGER = StatusLogger.getLogger();
  private static final ClassLoader SYSTEM_CLASSLOADER = getSystemClassLoader();

  private final MuleLoggerContextFactory loggerContextFactory = new MuleLoggerContextFactory();

  private LoggerContextCache cache = new LoggerContextCache(this, getClass().getClassLoader());

  ArtifactAwareContextSelector() {}

  @Override
  public LoggerContext getContext(String fqcn, ClassLoader loader, boolean currentContext) {
    return getContext(fqcn, loader, currentContext, null);
  }

  @Override
  public LoggerContext getContext(String fqcn, ClassLoader classLoader, boolean currentContext, URI configLocation) {
    return cache.getLoggerContext(resolveLoggerContextClassLoader(classLoader));
  }

  @Override
  public List<LoggerContext> getLoggerContexts() {
    return cache.getAllLoggerContexts();
  }

  @Override
  public void removeContext(LoggerContext context) {
    cache.remove(context);
  }

  /**
   * Given a {@code classLoader} this method will resolve which is the {@code classLoader} associated with the logger context to
   * use for this {@code classLoader} .
   * <p/>
   * When the provided {@code classLoader} is from an application or a domain it will return the {@code classLoader} associated
   * with the logger context of the application or domain. So far the artifact (domain or app) {@code classLoader} will be
   * resolved to it self.
   * <p/>
   * If the {@code classLoader} belongs to the container or any other {@code classLoader} created from a library running outside
   * the context of an artifact then the system {@code classLoader} will be used.
   *
   * @param classLoader {@link ClassLoader} running the code where the logging was done
   * @return the {@link ClassLoader} owner of the logger context
   */
  static ClassLoader resolveLoggerContextClassLoader(ClassLoader classLoader) {
    ClassLoader loggerClassLoader = classLoader == null ? Thread.currentThread().getContextClassLoader() : classLoader;

    // Obtains the first artifact class loader in the hierarchy
    while (!(loggerClassLoader instanceof ArtifactClassLoader) && loggerClassLoader != null) {
      loggerClassLoader = loggerClassLoader.getParent();
    }

    if (loggerClassLoader == null) {
      return SYSTEM_CLASSLOADER;
    } else if (isRegionClassLoaderMember(loggerClassLoader)) {
      loggerClassLoader =
          isPolicyClassLoader(loggerClassLoader.getParent()) ? loggerClassLoader.getParent().getParent()
              : loggerClassLoader.getParent();
    } else if (!(loggerClassLoader instanceof RegionClassLoader) && !(loggerClassLoader instanceof MuleSharedDomainClassLoader)) {
      return SYSTEM_CLASSLOADER;
    }

    return loggerClassLoader;
  }

  private static boolean isPolicyClassLoader(ClassLoader loggerClassLoader) {
    return ((ArtifactClassLoader) loggerClassLoader).getArtifactDescriptor() instanceof PolicyTemplateDescriptor;
  }

  private static boolean isRegionClassLoaderMember(ClassLoader classLoader) {
    return !(classLoader instanceof RegionClassLoader) && classLoader.getParent() instanceof RegionClassLoader;
  }

  @Override
  public void dispose() {
    cache.dispose();
  }

  public void destroyLoggersFor(ClassLoader classLoader) {
    cache.remove(classLoader);
  }

  LoggerContext buildContext(final ClassLoader classLoader) {
    return loggerContextFactory.build(classLoader, this);
  }
}
