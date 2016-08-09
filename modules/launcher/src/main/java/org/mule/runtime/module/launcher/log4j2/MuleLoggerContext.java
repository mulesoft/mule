/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.launcher.log4j2;

import static org.reflections.ReflectionUtils.getAllFields;
import static org.reflections.ReflectionUtils.withName;

import org.mule.runtime.core.logging.LogConfigChangeSubject;
import org.mule.runtime.module.artifact.classloader.ArtifactClassLoader;
import org.mule.runtime.module.launcher.application.ApplicationClassLoader;

import java.beans.PropertyChangeListener;
import java.lang.reflect.Field;
import java.net.URI;
import java.util.Collection;
import java.util.concurrent.ConcurrentMap;

import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.selector.ContextSelector;
import org.apache.logging.log4j.message.MessageFactory;

/**
 * Subclass of {@link org.apache.logging.log4j.core.LoggerContext} which adds some information about the mule artifact being
 * logged.
 * <p/>
 * The most important function of this class though is to override the {@link #reconfigure()} method to to its inherited purpose
 * plus invoking {@link org.mule.runtime.module.launcher.log4j2.LoggerContextConfigurer#configure(MuleLoggerContext)}.
 * <p/>
 * The {@link org.mule.runtime.module.launcher.log4j2.LoggerContextConfigurer} needs to be invoked here so that it's invoked each
 * time the configuration is reloaded.
 * <p/>
 * This class must not hold any reference to a {@link java.lang.ClassLoader} since otherwise
 * {@link org.apache.logging.log4j.core.Logger} instances held on static fields will make that class loader GC unreachable
 *
 * @since 3.6.0
 */
class MuleLoggerContext extends LoggerContext implements LogConfigChangeSubject {

  private final LoggerContextConfigurer loggerContextConfigurer = new LoggerContextConfigurer();

  private final URI configFile;
  private final boolean standlone;
  private final ContextSelector contextSelector;
  private final boolean artifactClassloader;
  private final boolean applicationClassloader;
  private final String artifactName;
  private final int ownerClassLoaderHash;
  private final Field loggersField;

  MuleLoggerContext(String name, ContextSelector contextSelector, boolean standalone) {
    this(name, null, null, contextSelector, standalone);
  }

  MuleLoggerContext(String name, URI configLocn, ClassLoader ownerClassLoader, ContextSelector contextSelector,
                    boolean standalone) {
    super(name, null, configLocn);
    configFile = configLocn;
    this.contextSelector = contextSelector;
    this.standlone = standalone;
    ownerClassLoaderHash =
        ownerClassLoader != null ? ownerClassLoader.hashCode() : getClass().getClassLoader().getSystemClassLoader().hashCode();

    if (ownerClassLoader instanceof ArtifactClassLoader) {
      artifactClassloader = true;
      artifactName = ((ArtifactClassLoader) ownerClassLoader).getArtifactName();
      applicationClassloader = ownerClassLoader instanceof ApplicationClassLoader;
    } else {
      artifactClassloader = false;
      applicationClassloader = false;
      artifactName = null;
    }

    // https://issues.apache.org/jira/browse/LOG4J2-1318
    // Allow for overriding #getLogger
    Collection<Field> candidateFields = getAllFields(LoggerContext.class, withName("loggers"));
    if (candidateFields.size() == 1) {
      loggersField = candidateFields.iterator().next();
      loggersField.setAccessible(true);
    } else {
      loggersField = null;
    }
  }

  @Override
  public synchronized void reconfigure() {
    loggerContextConfigurer.configure(this);
    super.reconfigure();
  }

  @Override
  public void updateLoggers(Configuration config) {
    loggerContextConfigurer.update(this);
    super.updateLoggers(config);
  }

  @Override
  public void registerLogConfigChangeListener(PropertyChangeListener logConfigChangeListener) {
    addPropertyChangeListener(logConfigChangeListener);
  }

  @Override
  public void unregisterLogConfigChangeListener(PropertyChangeListener logConfigChangeListener) {
    removePropertyChangeListener(logConfigChangeListener);
  }

  /**
   * Override to return a {@link DispatchingLogger} instead of a simple logger {@inheritDoc}
   *
   * @return a {@link DispatchingLogger}
   */
  @Override
  protected Logger newInstance(LoggerContext ctx, final String name, final MessageFactory messageFactory) {

    return new DispatchingLogger(super.newInstance(ctx, name, messageFactory), ownerClassLoaderHash, this, contextSelector,
                                 messageFactory) {

      // force the name due to log4j2's cyclic constructor dependencies
      // aren't a friend of the wrapper pattern
      @Override
      public String getName() {
        return name;
      }
    };
  }

  private ConcurrentMap<String, Logger> lookupLoggersField() {
    try {
      return (ConcurrentMap<String, Logger>) loggersField.get(this);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Overriding to avoid a performance issue introduced in https://issues.apache.org/jira/browse/LOG4J2-1180, which doubles the
   * pressure on the GC and thus lowering the throughput of mule applications. (https://issues.apache.org/jira/browse/LOG4J2-1318)
   * <p>
   * Obtains a Logger from the Context.
   * 
   * @param name The name of the Logger to return.
   * @param messageFactory The message factory is used only when creating a logger, subsequent use does not change the logger but
   *        will log a warning if mismatched.
   * @return The Logger.
   */
  @Override
  public Logger getLogger(final String name, final MessageFactory messageFactory) {
    Logger logger = lookupLoggersField().get(name);
    if (logger != null) {
      return logger;
    }

    logger = newInstance(this, name, messageFactory);
    final Logger prev = lookupLoggersField().putIfAbsent(name, logger);
    return prev == null ? logger : prev;
  }

  protected URI getConfigFile() {
    return configFile;
  }

  protected boolean isStandlone() {
    return standlone;
  }

  protected boolean isArtifactClassloader() {
    return artifactClassloader;
  }

  protected boolean isApplicationClassloader() {
    return applicationClassloader;
  }

  protected String getArtifactName() {
    return artifactName;
  }
}
