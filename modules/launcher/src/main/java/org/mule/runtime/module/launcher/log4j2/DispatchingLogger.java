/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.launcher.log4j2;

import static org.mule.runtime.module.launcher.log4j2.ArtifactAwareContextSelector.resolveLoggerContextClassLoader;
import static org.reflections.ReflectionUtils.getAllMethods;
import static org.reflections.ReflectionUtils.withName;
import static org.reflections.ReflectionUtils.withParameters;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.selector.ContextSelector;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.MessageFactory;
import org.apache.logging.log4j.spi.AbstractLogger;
import org.apache.logging.log4j.spi.ExtendedLogger;

/**
 * Suppose that class X is used in applications Y and Z. If X holds a static reference to a logger L, then all the log events are
 * going to be added into the context {@link org.apache.logging.log4j.core.LoggerContext} on which L fast first initialized,
 * regardless of which application generated the event.
 * <p/>
 * This class is a wrapper for {@link org.apache.logging.log4j.core.Logger} class which is capable of detecting that the log event
 * is being generated from an application which {@link org.apache.logging.log4j.core.LoggerContext} is different than L's, and
 * thus forward the event to the correct context.
 * <p/>
 * Because this class is a fix for issues in static loggers, it must not hold any reference to any {@link java.lang.ClassLoader}
 * since otherwise that class loader would be GC unreachable. For that reason, it uses {@link #ownerClassLoaderHash} instead of
 * the real reference
 *
 * @since 3.6.0
 */
abstract class DispatchingLogger extends Logger {

  private final Logger originalLogger;
  private Method updateConfigurationMethod = null;
  private final ContextSelector contextSelector;
  private final int ownerClassLoaderHash;

  DispatchingLogger(Logger originalLogger, int ownerClassLoaderHash, LoggerContext loggerContext, ContextSelector contextSelector,
                    MessageFactory messageFactory) {
    super(loggerContext, originalLogger.getName(), messageFactory);
    this.originalLogger = originalLogger;
    this.contextSelector = contextSelector;
    this.ownerClassLoaderHash = ownerClassLoaderHash;
  }


  private Logger getLogger() {
    final ClassLoader currentClassLoader = resolveLoggerContextClassLoader(Thread.currentThread().getContextClassLoader());
    if (useThisLoggerContextClassLoader(currentClassLoader)) {
      return originalLogger;
    }

    // trick - this is probably a logger declared in a static field
    // the classloader used to create it and the TCCL can be different
    // ask contextSelector for the correct context
    return contextSelector.getContext(getName(), currentClassLoader, true).getLogger(getName(), getMessageFactory());
  }

  /**
   * @param currentClassLoader execution classloader of the logging operation
   * @return true if the logger context associated with this instance must be used for logging, false if we still need to continue
   *         searching for the right logger context
   */
  private boolean useThisLoggerContextClassLoader(ClassLoader currentClassLoader) {
    return currentClassLoader.hashCode() == ownerClassLoaderHash;
  }

  /**
   * This is workaround for the low visibility of the {@link Logger#updateConfiguration(Configuration)} method, which invokes it
   * on the {@code originalLogger}.
   * 
   * Using a wrapper in the log4j package causes an {@link IllegalAccessError}.
   *
   * @param config
   */
  @Override
  protected void updateConfiguration(final Configuration config) {
    if (lookupUpdateConfigurationMethod()) {
      try {
        updateConfigurationMethod.invoke(originalLogger, config);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
  }

  private boolean lookupUpdateConfigurationMethod() {
    if (updateConfigurationMethod == null) {
      Collection<Method> candidateMethods =
          getAllMethods(originalLogger.getClass(), withName("updateConfiguration"), withParameters(Configuration.class));
      if (candidateMethods.size() == 1) {
        updateConfigurationMethod = candidateMethods.iterator().next();
        updateConfigurationMethod.setAccessible(true);
      }
    }

    return updateConfigurationMethod != null;
  }

  @Override
  public MessageFactory getMessageFactory() {
    return originalLogger.getMessageFactory();
  }

  @Override
  public Logger getParent() {
    return getLogger().getParent();
  }

  @Override
  public LoggerContext getContext() {
    return getLogger().getContext();
  }

  @Override
  public void setLevel(Level level) {
    getLogger().setLevel(level);
  }

  @Override
  public void logMessage(String fqcn, Level level, Marker marker, Message message, Throwable t) {
    getLogger().logMessage(fqcn, level, marker, message, t);
  }

  @Override
  public boolean isEnabled(Level level, Marker marker, String message, Throwable t) {
    return getLogger().isEnabled(level, marker, message, t);
  }

  @Override
  public boolean isEnabled(Level level, Marker marker, String message) {
    return getLogger().isEnabled(level, marker, message);
  }

  @Override
  public boolean isEnabled(Level level, Marker marker, String message, Object... params) {
    return getLogger().isEnabled(level, marker, message, params);
  }

  @Override
  public boolean isEnabled(Level level, Marker marker, Object message, Throwable t) {
    return getLogger().isEnabled(level, marker, message, t);
  }

  @Override
  public boolean isEnabled(Level level, Marker marker, Message message, Throwable t) {
    return getLogger().isEnabled(level, marker, message, t);
  }

  @Override
  public void addAppender(Appender appender) {
    getLogger().addAppender(appender);
  }

  @Override
  public void removeAppender(Appender appender) {
    getLogger().removeAppender(appender);
  }

  @Override
  public Map<String, Appender> getAppenders() {
    return getLogger().getAppenders();
  }

  @Override
  public Iterator<Filter> getFilters() {
    return getLogger().getFilters();
  }

  @Override
  public Level getLevel() {
    return getLogger().getLevel();
  }

  @Override
  public int filterCount() {
    return getLogger().filterCount();
  }

  @Override
  public void addFilter(Filter filter) {
    getLogger().addFilter(filter);
  }

  @Override
  public boolean isAdditive() {
    return getLogger().isAdditive();
  }

  @Override
  public void setAdditive(boolean additive) {
    getLogger().setAdditive(additive);
  }

  @Override
  public String toString() {
    return getLogger().toString();
  }

  public static void checkMessageFactory(ExtendedLogger logger, MessageFactory messageFactory) {
    AbstractLogger.checkMessageFactory(logger, messageFactory);
  }

  @Override
  public void catching(Level level, Throwable t) {
    getLogger().catching(level, t);
  }

  @Override
  public void catching(Throwable t) {
    getLogger().catching(t);
  }

  @Override
  public void debug(Marker marker, Message msg) {
    getLogger().debug(marker, msg);
  }

  @Override
  public void debug(Marker marker, Message msg, Throwable t) {
    getLogger().debug(marker, msg, t);
  }

  @Override
  public void debug(Marker marker, Object message) {
    getLogger().debug(marker, message);
  }

  @Override
  public void debug(Marker marker, Object message, Throwable t) {
    getLogger().debug(marker, message, t);
  }

  @Override
  public void debug(Marker marker, String message) {
    getLogger().debug(marker, message);
  }

  @Override
  public void debug(Marker marker, String message, Object... params) {
    getLogger().debug(marker, message, params);
  }

  @Override
  public void debug(Marker marker, String message, Throwable t) {
    getLogger().debug(marker, message, t);
  }

  @Override
  public void debug(Message msg) {
    getLogger().debug(msg);
  }

  @Override
  public void debug(Message msg, Throwable t) {
    getLogger().debug(msg, t);
  }

  @Override
  public void debug(Object message) {
    getLogger().debug(message);
  }

  @Override
  public void debug(Object message, Throwable t) {
    getLogger().debug(message, t);
  }

  @Override
  public void debug(String message) {
    getLogger().debug(message);
  }

  @Override
  public void debug(String message, Object... params) {
    getLogger().debug(message, params);
  }

  @Override
  public void debug(String message, Throwable t) {
    getLogger().debug(message, t);
  }

  @Override
  public void entry() {
    getLogger().entry();
  }

  @Override
  public void entry(Object... params) {
    getLogger().entry(params);
  }

  @Override
  public void entry(String fqcn, Object... params) {
    getLogger().entry(fqcn, params);
  }

  @Override
  public void error(Marker marker, Message msg) {
    getLogger().error(marker, msg);
  }

  @Override
  public void error(Marker marker, Message msg, Throwable t) {
    getLogger().error(marker, msg, t);
  }

  @Override
  public void error(Marker marker, Object message) {
    getLogger().error(marker, message);
  }

  @Override
  public void error(Marker marker, Object message, Throwable t) {
    getLogger().error(marker, message, t);
  }

  @Override
  public void error(Marker marker, String message) {
    getLogger().error(marker, message);
  }

  @Override
  public void error(Marker marker, String message, Object... params) {
    getLogger().error(marker, message, params);
  }

  @Override
  public void error(Marker marker, String message, Throwable t) {
    getLogger().error(marker, message, t);
  }

  @Override
  public void error(Message msg) {
    getLogger().error(msg);
  }

  @Override
  public void error(Message msg, Throwable t) {
    getLogger().error(msg, t);
  }

  @Override
  public void error(Object message) {
    getLogger().error(message);
  }

  @Override
  public void error(Object message, Throwable t) {
    getLogger().error(message, t);
  }

  @Override
  public void error(String message) {
    getLogger().error(message);
  }

  @Override
  public void error(String message, Object... params) {
    getLogger().error(message, params);
  }

  @Override
  public void error(String message, Throwable t) {
    getLogger().error(message, t);
  }

  @Override
  public void exit() {
    getLogger().exit();
  }

  @Override
  public <R> R exit(R result) {
    return getLogger().exit(result);
  }

  @Override
  public void fatal(Marker marker, Message msg) {
    getLogger().fatal(marker, msg);
  }

  @Override
  public void fatal(Marker marker, Message msg, Throwable t) {
    getLogger().fatal(marker, msg, t);
  }

  @Override
  public void fatal(Marker marker, Object message) {
    getLogger().fatal(marker, message);
  }

  @Override
  public void fatal(Marker marker, Object message, Throwable t) {
    getLogger().fatal(marker, message, t);
  }

  @Override
  public void fatal(Marker marker, String message) {
    getLogger().fatal(marker, message);
  }

  @Override
  public void fatal(Marker marker, String message, Object... params) {
    getLogger().fatal(marker, message, params);
  }

  @Override
  public void fatal(Marker marker, String message, Throwable t) {
    getLogger().fatal(marker, message, t);
  }

  @Override
  public void fatal(Message msg) {
    getLogger().fatal(msg);
  }

  @Override
  public void fatal(Message msg, Throwable t) {
    getLogger().fatal(msg, t);
  }

  @Override
  public void fatal(Object message) {
    getLogger().fatal(message);
  }

  @Override
  public void fatal(Object message, Throwable t) {
    getLogger().fatal(message, t);
  }

  @Override
  public void fatal(String message) {
    getLogger().fatal(message);
  }

  @Override
  public void fatal(String message, Object... params) {
    getLogger().fatal(message, params);
  }

  @Override
  public void fatal(String message, Throwable t) {
    getLogger().fatal(message, t);
  }

  @Override
  public void info(Marker marker, Message msg) {
    getLogger().info(marker, msg);
  }

  @Override
  public void info(Marker marker, Message msg, Throwable t) {
    getLogger().info(marker, msg, t);
  }

  @Override
  public void info(Marker marker, Object message) {
    getLogger().info(marker, message);
  }

  @Override
  public void info(Marker marker, Object message, Throwable t) {
    getLogger().info(marker, message, t);
  }

  @Override
  public void info(Marker marker, String message) {
    getLogger().info(marker, message);
  }

  @Override
  public void info(Marker marker, String message, Object... params) {
    getLogger().info(marker, message, params);
  }

  @Override
  public void info(Marker marker, String message, Throwable t) {
    getLogger().info(marker, message, t);
  }

  @Override
  public void info(Message msg) {
    getLogger().info(msg);
  }

  @Override
  public void info(Message msg, Throwable t) {
    getLogger().info(msg, t);
  }

  @Override
  public void info(Object message) {
    getLogger().info(message);
  }

  @Override
  public void info(Object message, Throwable t) {
    getLogger().info(message, t);
  }

  @Override
  public void info(String message) {
    getLogger().info(message);
  }

  @Override
  public void info(String message, Object... params) {
    getLogger().info(message, params);
  }

  @Override
  public void info(String message, Throwable t) {
    getLogger().info(message, t);
  }

  @Override
  public boolean isDebugEnabled() {
    return getLogger().isDebugEnabled();
  }

  @Override
  public boolean isDebugEnabled(Marker marker) {
    return getLogger().isDebugEnabled(marker);
  }

  @Override
  public boolean isEnabled(Level level) {
    return getLogger().isEnabled(level);
  }

  @Override
  public boolean isEnabled(Level level, Marker marker) {
    return getLogger().isEnabled(level, marker);
  }

  @Override
  public boolean isErrorEnabled() {
    return getLogger().isErrorEnabled();
  }

  @Override
  public boolean isErrorEnabled(Marker marker) {
    return getLogger().isErrorEnabled(marker);
  }

  @Override
  public boolean isFatalEnabled() {
    return getLogger().isFatalEnabled();
  }

  @Override
  public boolean isFatalEnabled(Marker marker) {
    return getLogger().isFatalEnabled(marker);
  }

  @Override
  public boolean isInfoEnabled() {
    return getLogger().isInfoEnabled();
  }

  @Override
  public boolean isInfoEnabled(Marker marker) {
    return getLogger().isInfoEnabled(marker);
  }

  @Override
  public boolean isTraceEnabled() {
    return getLogger().isTraceEnabled();
  }

  @Override
  public boolean isTraceEnabled(Marker marker) {
    return getLogger().isTraceEnabled(marker);
  }

  @Override
  public boolean isWarnEnabled() {
    return getLogger().isWarnEnabled();
  }

  @Override
  public boolean isWarnEnabled(Marker marker) {
    return getLogger().isWarnEnabled(marker);
  }

  @Override
  public void log(Level level, Marker marker, Message msg) {
    getLogger().log(level, marker, msg);
  }

  @Override
  public void log(Level level, Marker marker, Message msg, Throwable t) {
    getLogger().log(level, marker, msg, t);
  }

  @Override
  public void log(Level level, Marker marker, Object message) {
    getLogger().log(level, marker, message);
  }

  @Override
  public void log(Level level, Marker marker, Object message, Throwable t) {
    getLogger().log(level, marker, message, t);
  }

  @Override
  public void log(Level level, Marker marker, String message) {
    getLogger().log(level, marker, message);
  }

  @Override
  public void log(Level level, Marker marker, String message, Object... params) {
    getLogger().log(level, marker, message, params);
  }

  @Override
  public void log(Level level, Marker marker, String message, Throwable t) {
    getLogger().log(level, marker, message, t);
  }

  @Override
  public void log(Level level, Message msg) {
    getLogger().log(level, msg);
  }

  @Override
  public void log(Level level, Message msg, Throwable t) {
    getLogger().log(level, msg, t);
  }

  @Override
  public void log(Level level, Object message) {
    getLogger().log(level, message);
  }

  @Override
  public void log(Level level, Object message, Throwable t) {
    getLogger().log(level, message, t);
  }

  @Override
  public void log(Level level, String message) {
    getLogger().log(level, message);
  }

  @Override
  public void log(Level level, String message, Object... params) {
    getLogger().log(level, message, params);
  }

  @Override
  public void log(Level level, String message, Throwable t) {
    getLogger().log(level, message, t);
  }

  @Override
  public void logIfEnabled(String fqcn, Level level, Marker marker, Message msg, Throwable t) {
    getLogger().logIfEnabled(fqcn, level, marker, msg, t);
  }

  @Override
  public void logIfEnabled(String fqcn, Level level, Marker marker, Object message, Throwable t) {
    getLogger().logIfEnabled(fqcn, level, marker, message, t);
  }

  @Override
  public void logIfEnabled(String fqcn, Level level, Marker marker, String message) {
    getLogger().logIfEnabled(fqcn, level, marker, message);
  }

  @Override
  public void logIfEnabled(String fqcn, Level level, Marker marker, String message, Object... params) {
    getLogger().logIfEnabled(fqcn, level, marker, message, params);
  }

  @Override
  public void logIfEnabled(String fqcn, Level level, Marker marker, String message, Throwable t) {
    getLogger().logIfEnabled(fqcn, level, marker, message, t);
  }

  @Override
  public void printf(Level level, Marker marker, String format, Object... params) {
    getLogger().printf(level, marker, format, params);
  }

  @Override
  public void printf(Level level, String format, Object... params) {
    getLogger().printf(level, format, params);
  }

  @Override
  public <T extends Throwable> T throwing(T t) {
    return getLogger().throwing(t);
  }

  @Override
  public <T extends Throwable> T throwing(Level level, T t) {
    return getLogger().throwing(level, t);
  }

  @Override
  public void trace(Marker marker, Message msg) {
    getLogger().trace(marker, msg);
  }

  @Override
  public void trace(Marker marker, Message msg, Throwable t) {
    getLogger().trace(marker, msg, t);
  }

  @Override
  public void trace(Marker marker, Object message) {
    getLogger().trace(marker, message);
  }

  @Override
  public void trace(Marker marker, Object message, Throwable t) {
    getLogger().trace(marker, message, t);
  }

  @Override
  public void trace(Marker marker, String message) {
    getLogger().trace(marker, message);
  }

  @Override
  public void trace(Marker marker, String message, Object... params) {
    getLogger().trace(marker, message, params);
  }

  @Override
  public void trace(Marker marker, String message, Throwable t) {
    getLogger().trace(marker, message, t);
  }

  @Override
  public void trace(Message msg) {
    getLogger().trace(msg);
  }

  @Override
  public void trace(Message msg, Throwable t) {
    getLogger().trace(msg, t);
  }

  @Override
  public void trace(Object message) {
    getLogger().trace(message);
  }

  @Override
  public void trace(Object message, Throwable t) {
    getLogger().trace(message, t);
  }

  @Override
  public void trace(String message) {
    getLogger().trace(message);
  }

  @Override
  public void trace(String message, Object... params) {
    getLogger().trace(message, params);
  }

  @Override
  public void trace(String message, Throwable t) {
    getLogger().trace(message, t);
  }

  @Override
  public void warn(Marker marker, Message msg) {
    getLogger().warn(marker, msg);
  }

  @Override
  public void warn(Marker marker, Message msg, Throwable t) {
    getLogger().warn(marker, msg, t);
  }

  @Override
  public void warn(Marker marker, Object message) {
    getLogger().warn(marker, message);
  }

  @Override
  public void warn(Marker marker, Object message, Throwable t) {
    getLogger().warn(marker, message, t);
  }

  @Override
  public void warn(Marker marker, String message) {
    getLogger().warn(marker, message);
  }

  @Override
  public void warn(Marker marker, String message, Object... params) {
    getLogger().warn(marker, message, params);
  }

  @Override
  public void warn(Marker marker, String message, Throwable t) {
    getLogger().warn(marker, message, t);
  }

  @Override
  public void warn(Message msg) {
    getLogger().warn(msg);
  }

  @Override
  public void warn(Message msg, Throwable t) {
    getLogger().warn(msg, t);
  }

  @Override
  public void warn(Object message) {
    getLogger().warn(message);
  }

  @Override
  public void warn(Object message, Throwable t) {
    getLogger().warn(message, t);
  }

  @Override
  public void warn(String message) {
    getLogger().warn(message);
  }

  @Override
  public void warn(String message, Object... params) {
    getLogger().warn(message, params);
  }

  @Override
  public void warn(String message, Throwable t) {
    getLogger().warn(message, t);
  }
}
