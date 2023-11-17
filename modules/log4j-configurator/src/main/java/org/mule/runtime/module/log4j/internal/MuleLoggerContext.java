/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.log4j.internal;

import org.mule.runtime.module.artifact.api.classloader.ArtifactClassLoader;
import org.mule.runtime.module.artifact.api.classloader.RegionClassLoader;
import org.mule.runtime.module.artifact.api.descriptor.ApplicationDescriptor;
import org.mule.runtime.module.artifact.api.descriptor.DeployableArtifactDescriptor;

import java.net.URI;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.core.Appender;

import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.selector.ContextSelector;
import org.apache.logging.log4j.message.MessageFactory;

/**
 * Subclass of {@link LoggerContext} which adds some information about the mule artifact being logged.
 * <p/>
 * The most important function of this class though is to override the {@link #reconfigure()} method to to its inherited purpose
 * plus invoking {@link LoggerContextConfigurer#configure(MuleLoggerContext)}.
 * <p/>
 * The {@link LoggerContextConfigurer} needs to be invoked here so that it's invoked each time the configuration is reloaded.
 * <p/>
 * This class must not hold any reference to a {@link ClassLoader} since otherwise {@link Logger} instances held on static fields
 * will make that class loader GC unreachable
 *
 * @since 4.5
 */
class MuleLoggerContext extends LoggerContext {

  private final LoggerContextConfigurer loggerContextConfigurer = new LoggerContextConfigurer();

  private final URI configFile;
  private final boolean standalone;
  private final boolean logSeparationEnabled;
  private final Appender containerAppender;
  private ContextSelector contextSelector;
  private final boolean artifactClassloader;
  private final boolean applicationClassloader;
  private final String artifactName;
  private final int ownerClassLoaderHash;

  private DeployableArtifactDescriptor artifactDescriptor;

  MuleLoggerContext(String name, ContextSelector contextSelector, boolean standalone, boolean logSeparationEnabled) {
    this(name, null, null, contextSelector, standalone, logSeparationEnabled, null);
  }

  MuleLoggerContext(String name,
                    URI configLocn,
                    ClassLoader ownerClassLoader,
                    ContextSelector contextSelector,
                    boolean standalone,
                    boolean logSeparationEnabled,
                    Appender containerAppender) {
    super(name, null, configLocn);
    configFile = configLocn;
    this.contextSelector = contextSelector;
    this.standalone = standalone;
    this.containerAppender = containerAppender;
    this.logSeparationEnabled = logSeparationEnabled;
    ownerClassLoaderHash =
        ownerClassLoader != null ? ownerClassLoader.hashCode() : getClass().getClassLoader().getSystemClassLoader().hashCode();

    if (ownerClassLoader instanceof ArtifactClassLoader) {
      artifactClassloader = true;
      artifactName = getArtifactName((ArtifactClassLoader) ownerClassLoader);
      artifactDescriptor = getArtifactDescriptor((ArtifactClassLoader) ownerClassLoader);
      applicationClassloader = ownerClassLoader instanceof RegionClassLoader && ((RegionClassLoader) ownerClassLoader)
          .getOwnerClassLoader().getArtifactDescriptor() instanceof ApplicationDescriptor;
    } else {
      artifactClassloader = false;
      applicationClassloader = false;
      artifactName = null;
    }
  }

  private DeployableArtifactDescriptor getArtifactDescriptor(ArtifactClassLoader ownerClassLoader) {
    if (!(ownerClassLoader.getArtifactDescriptor() instanceof DeployableArtifactDescriptor)) {
      throw new IllegalArgumentException("Artifact should be a deployable, i.e. an application or domain");
    }

    return ownerClassLoader.getArtifactDescriptor();
  }

  private String getArtifactName(ArtifactClassLoader ownerClassLoader) {
    return ownerClassLoader.getArtifactDescriptor().getName();
  }

  @Override
  public synchronized void reconfigure() {
    loggerContextConfigurer.configure(this);
    if (loggerContextConfigurer.shouldConfigureContext(this)) {
      super.reconfigure();
    }
  }

  @Override
  public void updateLoggers(Configuration config) {
    loggerContextConfigurer.update(this);
    super.updateLoggers(config);
  }

  /**
   * Override to return a {@link DispatchingLogger} instead of a simple logger {@inheritDoc}
   *
   * @return a {@link DispatchingLogger}
   */
  @Override
  protected Logger newInstance(LoggerContext ctx, final String name, final MessageFactory messageFactory) {
    Logger logger = super.newInstance(ctx, name, messageFactory);
    if (containerAppender != null) {
      return new NoFileAppenderLogger(ctx, name, messageFactory, containerAppender);
    }

    if (artifactClassloader || applicationClassloader || !logSeparationEnabled) {
      return logger;
    }

    return new DispatchingLogger(logger, ownerClassLoaderHash, this, contextSelector, messageFactory) {

      // force the name due to log4j2's cyclic constructor dependencies
      // aren't a friend of the wrapper pattern
      @Override
      public String getName() {
        return name;
      }
    };
  }

  protected URI getConfigFile() {
    return configFile;
  }

  protected boolean isStandalone() {
    return standalone;
  }

  protected boolean isArtifactClassloader() {
    return artifactClassloader;
  }

  protected boolean isApplicationClassloader() {
    return applicationClassloader;
  }

  protected DeployableArtifactDescriptor getArtifactDescriptor() {
    return artifactDescriptor;
  }

  protected String getArtifactName() {
    return artifactName;
  }

  @Override
  public void stop() {
    super.stop();
    // Clean up reference to avoid class loader leaks
    this.artifactDescriptor = null;
    this.contextSelector = null;
  }

  @Override
  public boolean stop(long timeout, TimeUnit timeUnit) {
    boolean result = super.stop(timeout, timeUnit);
    // Clean up reference to avoid class loader leaks
    this.artifactDescriptor = null;
    this.contextSelector = null;
    return result;
  }
}
