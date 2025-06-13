/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.log4j.internal;

import static org.mule.runtime.core.api.util.ClassUtils.setContextClassLoader;
import static org.mule.runtime.module.log4j.internal.ArtifactAwareContextSelector.resolveLoggerContextClassLoader;
import static org.mule.runtime.module.log4j.internal.Log4JBlockingLoggerResolutionClassRegistry.getClassNamesNeedingBlockingLoggerResolution;

import static java.lang.Thread.currentThread;

import static com.github.benmanes.caffeine.cache.Caffeine.newBuilder;

import org.mule.runtime.api.util.Reference;

import java.util.Iterator;
import java.util.Map;

import com.github.benmanes.caffeine.cache.LoadingCache;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogBuilder;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.selector.ContextSelector;
import org.apache.logging.log4j.message.EntryMessage;
import org.apache.logging.log4j.message.FlowMessageFactory;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.MessageFactory;
import org.apache.logging.log4j.spi.AbstractLogger;
import org.apache.logging.log4j.spi.ExtendedLogger;
import org.apache.logging.log4j.util.MessageSupplier;
import org.apache.logging.log4j.util.Supplier;

/**
 * Suppose that class X is used in applications Y and Z. If X holds a static reference to a logger L, then all the log events are
 * going to be added into the context {@link LoggerContext} on which L fast first initialized, regardless of which application
 * generated the event.
 * <p/>
 * This class is a wrapper for {@link Logger} class which is capable of detecting that the log event is being generated from an
 * application which {@link LoggerContext} is different than L's, and thus forward the event to the correct context.
 * <p/>
 * Because this class is a fix for issues in static loggers, it must not hold any reference to any {@link ClassLoader} since
 * otherwise that class loader would be GC unreachable. For that reason, it uses {@link #ownerClassLoaderHash} instead of the real
 * reference
 *
 * @since 4.5
 */
abstract class DispatchingLogger extends Logger {

  private final Logger originalLogger;
  private final ContextSelector contextSelector;
  private final int ownerClassLoaderHash;
  private final boolean requiresBlockingLoggerResolution;
  private final LoadingCache<ClassLoader, Reference<Logger>> loggerCache = newBuilder()
      .weakKeys()
      .weakValues()
      .build(key -> new Reference<>());

  DispatchingLogger(Logger originalLogger, int ownerClassLoaderHash, LoggerContext loggerContext, ContextSelector contextSelector,
                    MessageFactory messageFactory) {
    super(loggerContext, originalLogger.getName(), messageFactory);
    this.originalLogger = originalLogger;
    this.contextSelector = contextSelector;
    this.ownerClassLoaderHash = ownerClassLoaderHash;
    requiresBlockingLoggerResolution = getClassNamesNeedingBlockingLoggerResolution().contains(originalLogger.getName());
  }

  private Logger getLogger() {
    return getLogger(resolveLoggerContextClassLoader(currentThread().getContextClassLoader()));
  }

  private Logger getLogger(final ClassLoader resolvedCtxClassLoader) {
    if (useThisLoggerContextClassLoader(resolvedCtxClassLoader)) {
      return originalLogger;
    }

    Reference<Logger> loggerReference;

    // Switch back the tccl for the cache lookup, to avoid caffeine internal threads to have a reference to an app classloader.
    Thread thread = Thread.currentThread();
    ClassLoader currentClassLoader = thread.getContextClassLoader();
    setContextClassLoader(thread, currentClassLoader, getClass().getClassLoader());
    try {
      // we need to cache reference objects and do this double lookup to avoid cyclic resolutions of the same classloader
      // key which would result in an exception or a deadlock, depending on the cache implementation
      loggerReference = loggerCache.get(resolvedCtxClassLoader);
    } finally {
      setContextClassLoader(thread, getClass().getClassLoader(), currentClassLoader);
    }

    if (requiresBlockingLoggerResolution
        && contextSelector instanceof ArtifactAwareContextSelector artifactAwareContextSelector) {
      synchronized (artifactAwareContextSelector.getLoggerContextCache()) {
        return getLogger(resolvedCtxClassLoader, loggerReference);
      }
    } else {
      return getLogger(resolvedCtxClassLoader, loggerReference);
    }
  }

  protected Logger getLogger(ClassLoader resolvedCtxClassLoader, Reference<Logger> loggerReference) {
    Logger logger = loggerReference.get();
    if (logger == null) {
      synchronized (loggerReference) {
        logger = loggerReference.get();
        if (logger == null) {
          try {
            logger = resolveLogger(resolvedCtxClassLoader);
          } catch (RecursiveLoggerContextInstantiationException rle) {
            // The required Logger is already under construction by a previous resolveLogger call. Falling back to container
            // classloader.
            try {
              return resolveLogger(this.getClass().getClassLoader());
            } catch (RecursiveLoggerContextInstantiationException e) {
              // TODO: W-12337087 - this shouldn't happen, we have to check why the container logger is still in the process of
              // being created.
              return originalLogger;
            }
          }
          loggerReference.set(logger);
        }
      }
    }
    return logger;
  }

  private Logger resolveLogger(ClassLoader resolvedCtxClassLoader) {
    Logger logger;
    // trick - this is probably a logger declared in a static field
    // the classloader used to create it and the TCCL can be different
    // ask contextSelector for the correct context
    if (contextSelector instanceof ArtifactAwareContextSelector artifactAwareContextSelector) {
      logger = artifactAwareContextSelector.getContextWithResolvedContextClassLoader(resolvedCtxClassLoader)
          .getLogger(getName(), getMessageFactory());
    } else {
      logger = contextSelector.getContext(getName(), resolvedCtxClassLoader, true).getLogger(getName(), getMessageFactory());
    }

    if (logger instanceof DispatchingLogger) {
      return ((DispatchingLogger) logger).getLogger(resolvedCtxClassLoader);
    } else {
      return logger;
    }
  }

  /**
   * @param currentClassLoader execution classloader of the logging operation
   * @return true if the logger context associated with this instance must be used for logging, false if we still need to continue
   *         searching for the right logger context
   */
  private boolean useThisLoggerContextClassLoader(ClassLoader currentClassLoader) {
    return currentClassLoader.hashCode() == ownerClassLoaderHash;
  }

  @Override
  public MessageFactory getMessageFactory() {
    return originalLogger.getMessageFactory();
  }

  @Override
  public FlowMessageFactory getFlowMessageFactory() {
    return originalLogger.getFlowMessageFactory();
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
  public LogBuilder atDebug() {
    return getLogger().atDebug();
  }

  @Override
  public LogBuilder atError() {
    return getLogger().atError();
  }

  @Override
  public LogBuilder atFatal() {
    return getLogger().atFatal();
  }

  @Override
  public LogBuilder atInfo() {
    return getLogger().atInfo();
  }

  @Override
  public LogBuilder atLevel(Level level) {
    return getLogger().atLevel(level);
  }

  @Override
  public LogBuilder atTrace() {
    return getLogger().atTrace();
  }

  @Override
  public LogBuilder atWarn() {
    return getLogger().atWarn();
  }

  @Override
  public void logMessage(String fqcn, Level level, Marker marker, Message message, Throwable t) {
    getLogger().logMessage(fqcn, level, marker, message, t);
  }

  @Override
  public void logMessage(Level level, Marker marker, String fqcn, StackTraceElement location, Message message,
                         Throwable throwable) {
    getLogger().logMessage(level, marker, fqcn, location, message, throwable);
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
  public boolean isEnabled(Level level, Marker marker, String message, Object p0) {
    return getLogger().isEnabled(level, marker, message, p0);
  }

  @Override
  public boolean isEnabled(Level level, Marker marker, String message, Object p0, Object p1) {
    return getLogger().isEnabled(level, marker, message, p0, p1);
  }

  @Override
  public boolean isEnabled(Level level, Marker marker, String message, Object p0, Object p1, Object p2) {
    return getLogger().isEnabled(level, marker, message, p0, p1, p2);
  }

  @Override
  public boolean isEnabled(Level level, Marker marker, String message, Object p0, Object p1, Object p2, Object p3) {
    return getLogger().isEnabled(level, marker, message, p0, p1, p2, p3);
  }

  @Override
  public boolean isEnabled(Level level, Marker marker, String message, Object p0, Object p1, Object p2, Object p3, Object p4) {
    return getLogger().isEnabled(level, marker, message, p0, p1, p2, p3, p4);
  }

  @Override
  public boolean isEnabled(Level level, Marker marker, String message, Object p0, Object p1, Object p2, Object p3, Object p4,
                           Object p5) {
    return getLogger().isEnabled(level, marker, message, p0, p1, p2, p3, p4, p5);
  }

  @Override
  public boolean isEnabled(Level level, Marker marker, String message, Object p0, Object p1, Object p2, Object p3, Object p4,
                           Object p5, Object p6) {
    return getLogger().isEnabled(level, marker, message, p0, p1, p2, p3, p4, p5, p6);
  }

  @Override
  public boolean isEnabled(Level level, Marker marker, String message, Object p0, Object p1, Object p2, Object p3, Object p4,
                           Object p5, Object p6, Object p7) {
    return getLogger().isEnabled(level, marker, message, p0, p1, p2, p3, p4, p5, p6, p7);
  }

  @Override
  public boolean isEnabled(Level level, Marker marker, String message, Object p0, Object p1, Object p2, Object p3, Object p4,
                           Object p5, Object p6, Object p7, Object p8) {
    return getLogger().isEnabled(level, marker, message, p0, p1, p2, p3, p4, p5, p6, p7, p8);
  }

  @Override
  public boolean isEnabled(Level level, Marker marker, String message, Object p0, Object p1, Object p2, Object p3, Object p4,
                           Object p5, Object p6, Object p7, Object p8, Object p9) {
    return getLogger().isEnabled(level, marker, message, p0, p1, p2, p3, p4, p5, p6, p7, p8, p9);
  }

  @Override
  public boolean isEnabled(Level level, Marker marker, CharSequence message, Throwable t) {
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
  public LogBuilder always() {
    return getLogger().always();
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
  public void debug(Marker marker, CharSequence message) {
    getLogger().debug(marker, message);
  }

  @Override
  public void debug(Marker marker, CharSequence message, Throwable throwable) {
    getLogger().debug(marker, message, throwable);
  }

  @Override
  public void debug(CharSequence message) {
    getLogger().debug(message);
  }

  @Override
  public void debug(CharSequence message, Throwable throwable) {
    getLogger().debug(message, throwable);
  }

  @Override
  public void debug(Supplier<?> messageSupplier) {
    getLogger().debug(messageSupplier);
  }

  @Override
  public void debug(Supplier<?> messageSupplier, Throwable throwable) {
    getLogger().debug(messageSupplier, throwable);
  }

  @Override
  public void debug(Marker marker, Supplier<?> messageSupplier) {
    getLogger().debug(marker, messageSupplier);
  }

  @Override
  public void debug(Marker marker, String message, Supplier<?>... paramSuppliers) {
    getLogger().debug(marker, message, paramSuppliers);
  }

  @Override
  public void debug(Marker marker, Supplier<?> messageSupplier, Throwable throwable) {
    getLogger().debug(marker, messageSupplier, throwable);
  }

  @Override
  public void debug(String message, Supplier<?>... paramSuppliers) {
    getLogger().debug(message, paramSuppliers);
  }

  @Override
  public void debug(Marker marker, MessageSupplier messageSupplier) {
    getLogger().debug(marker, messageSupplier);
  }

  @Override
  public void debug(Marker marker, MessageSupplier messageSupplier, Throwable throwable) {
    getLogger().debug(marker, messageSupplier, throwable);
  }

  @Override
  public void debug(MessageSupplier messageSupplier) {
    getLogger().debug(messageSupplier);
  }

  @Override
  public void debug(MessageSupplier messageSupplier, Throwable throwable) {
    getLogger().debug(messageSupplier, throwable);
  }

  @Override
  public void debug(Marker marker, String message, Object p0) {
    getLogger().debug(marker, message, p0);
  }

  @Override
  public void debug(Marker marker, String message, Object p0, Object p1) {
    getLogger().debug(marker, message, p0, p1);
  }

  @Override
  public void debug(Marker marker, String message, Object p0, Object p1, Object p2) {
    getLogger().debug(marker, message, p0, p1, p2);
  }

  @Override
  public void debug(Marker marker, String message, Object p0, Object p1, Object p2, Object p3) {
    getLogger().debug(marker, message, p0, p1, p2, p3);
  }

  @Override
  public void debug(Marker marker, String message, Object p0, Object p1, Object p2, Object p3, Object p4) {
    getLogger().debug(marker, message, p0, p1, p2, p3, p4);
  }

  @Override
  public void debug(Marker marker, String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5) {
    getLogger().debug(marker, message, p0, p1, p2, p3, p4, p5);
  }

  @Override
  public void debug(Marker marker, String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6) {
    getLogger().debug(marker, message, p0, p1, p2, p3, p4, p5, p6);
  }

  @Override
  public void debug(Marker marker, String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6,
                    Object p7) {
    getLogger().debug(marker, message, p0, p1, p2, p3, p4, p5, p6, p7);
  }

  @Override
  public void debug(Marker marker, String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6,
                    Object p7, Object p8) {
    getLogger().debug(marker, message, p0, p1, p2, p3, p4, p5, p6, p7, p8);
  }

  @Override
  public void debug(Marker marker, String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6,
                    Object p7, Object p8, Object p9) {
    getLogger().debug(marker, message, p0, p1, p2, p3, p4, p5, p6, p7, p8, p9);
  }

  @Override
  public void debug(String message, Object p0) {
    getLogger().debug(message, p0);
  }

  @Override
  public void debug(String message, Object p0, Object p1) {
    getLogger().debug(message, p0, p1);
  }

  @Override
  public void debug(String message, Object p0, Object p1, Object p2) {
    getLogger().debug(message, p0, p1, p2);
  }

  @Override
  public void debug(String message, Object p0, Object p1, Object p2, Object p3) {
    getLogger().debug(message, p0, p1, p2, p3);
  }

  @Override
  public void debug(String message, Object p0, Object p1, Object p2, Object p3, Object p4) {
    getLogger().debug(message, p0, p1, p2, p3, p4);
  }

  @Override
  public void debug(String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5) {
    getLogger().debug(message, p0, p1, p2, p3, p4, p5);
  }

  @Override
  public void debug(String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6) {
    getLogger().debug(message, p0, p1, p2, p3, p4, p5, p6);
  }

  @Override
  public void debug(String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6, Object p7) {
    getLogger().debug(message, p0, p1, p2, p3, p4, p5, p6, p7);
  }

  @Override
  public void debug(String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6, Object p7,
                    Object p8) {
    getLogger().debug(message, p0, p1, p2, p3, p4, p5, p6, p7, p8);
  }

  @Override
  public void debug(String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6, Object p7,
                    Object p8, Object p9) {
    getLogger().debug(message, p0, p1, p2, p3, p4, p5, p6, p7, p8, p9);
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
  public void error(Marker marker, CharSequence message) {
    getLogger().error(marker, message);
  }

  @Override
  public void error(Marker marker, CharSequence message, Throwable throwable) {
    getLogger().error(marker, message, throwable);
  }

  @Override
  public void error(CharSequence message) {
    getLogger().error(message);
  }

  @Override
  public void error(CharSequence message, Throwable throwable) {
    getLogger().error(message, throwable);
  }

  @Override
  public void error(Supplier<?> messageSupplier) {
    getLogger().error(messageSupplier);
  }

  @Override
  public void error(Supplier<?> messageSupplier, Throwable throwable) {
    getLogger().error(messageSupplier, throwable);
  }

  @Override
  public void error(Marker marker, Supplier<?> messageSupplier) {
    getLogger().error(marker, messageSupplier);
  }

  @Override
  public void error(Marker marker, String message, Supplier<?>... paramSuppliers) {
    getLogger().error(marker, message, paramSuppliers);
  }

  @Override
  public void error(Marker marker, Supplier<?> messageSupplier, Throwable throwable) {
    getLogger().error(marker, messageSupplier, throwable);
  }

  @Override
  public void error(String message, Supplier<?>... paramSuppliers) {
    getLogger().error(message, paramSuppliers);
  }

  @Override
  public void error(Marker marker, MessageSupplier messageSupplier) {
    getLogger().error(marker, messageSupplier);
  }

  @Override
  public void error(Marker marker, MessageSupplier messageSupplier, Throwable throwable) {
    getLogger().error(marker, messageSupplier, throwable);
  }

  @Override
  public void error(MessageSupplier messageSupplier) {
    getLogger().error(messageSupplier);
  }

  @Override
  public void error(MessageSupplier messageSupplier, Throwable throwable) {
    getLogger().error(messageSupplier, throwable);
  }

  @Override
  public void error(Marker marker, String message, Object p0) {
    getLogger().error(marker, message, p0);
  }

  @Override
  public void error(Marker marker, String message, Object p0, Object p1) {
    getLogger().error(marker, message, p0, p1);
  }

  @Override
  public void error(Marker marker, String message, Object p0, Object p1, Object p2) {
    getLogger().error(marker, message, p0, p1, p2);
  }

  @Override
  public void error(Marker marker, String message, Object p0, Object p1, Object p2, Object p3) {
    getLogger().error(marker, message, p0, p1, p2, p3);
  }

  @Override
  public void error(Marker marker, String message, Object p0, Object p1, Object p2, Object p3, Object p4) {
    getLogger().error(marker, message, p0, p1, p2, p3, p4);
  }

  @Override
  public void error(Marker marker, String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5) {
    getLogger().error(marker, message, p0, p1, p2, p3, p4, p5);
  }

  @Override
  public void error(Marker marker, String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6) {
    getLogger().error(marker, message, p0, p1, p2, p3, p4, p5, p6);
  }

  @Override
  public void error(Marker marker, String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6,
                    Object p7) {
    getLogger().error(marker, message, p0, p1, p2, p3, p4, p5, p6, p7);
  }

  @Override
  public void error(Marker marker, String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6,
                    Object p7, Object p8) {
    getLogger().error(marker, message, p0, p1, p2, p3, p4, p5, p6, p7, p8);
  }

  @Override
  public void error(Marker marker, String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6,
                    Object p7, Object p8, Object p9) {
    getLogger().error(marker, message, p0, p1, p2, p3, p4, p5, p6, p7, p8, p9);
  }

  @Override
  public void error(String message, Object p0) {
    getLogger().error(message, p0);
  }

  @Override
  public void error(String message, Object p0, Object p1) {
    getLogger().error(message, p0, p1);
  }

  @Override
  public void error(String message, Object p0, Object p1, Object p2) {
    getLogger().error(message, p0, p1, p2);
  }

  @Override
  public void error(String message, Object p0, Object p1, Object p2, Object p3) {
    getLogger().error(message, p0, p1, p2, p3);
  }

  @Override
  public void error(String message, Object p0, Object p1, Object p2, Object p3, Object p4) {
    getLogger().error(message, p0, p1, p2, p3, p4);
  }

  @Override
  public void error(String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5) {
    getLogger().error(message, p0, p1, p2, p3, p4, p5);
  }

  @Override
  public void error(String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6) {
    getLogger().error(message, p0, p1, p2, p3, p4, p5, p6);
  }

  @Override
  public void error(String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6, Object p7) {
    getLogger().error(message, p0, p1, p2, p3, p4, p5, p6, p7);
  }

  @Override
  public void error(String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6, Object p7,
                    Object p8) {
    getLogger().error(message, p0, p1, p2, p3, p4, p5, p6, p7, p8);
  }

  @Override
  public void error(String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6, Object p7,
                    Object p8, Object p9) {
    getLogger().error(message, p0, p1, p2, p3, p4, p5, p6, p7, p8, p9);
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
  public void fatal(Marker marker, CharSequence message) {
    getLogger().fatal(marker, message);
  }

  @Override
  public void fatal(Marker marker, CharSequence message, Throwable throwable) {
    getLogger().fatal(marker, message, throwable);
  }

  @Override
  public void fatal(CharSequence message) {
    getLogger().fatal(message);
  }

  @Override
  public void fatal(CharSequence message, Throwable throwable) {
    getLogger().fatal(message, throwable);
  }

  @Override
  public void fatal(Supplier<?> messageSupplier) {
    getLogger().fatal(messageSupplier);
  }

  @Override
  public void fatal(Supplier<?> messageSupplier, Throwable throwable) {
    getLogger().fatal(messageSupplier, throwable);
  }

  @Override
  public void fatal(Marker marker, Supplier<?> messageSupplier) {
    getLogger().fatal(marker, messageSupplier);
  }

  @Override
  public void fatal(Marker marker, String message, Supplier<?>... paramSuppliers) {
    getLogger().fatal(marker, message, paramSuppliers);
  }

  @Override
  public void fatal(Marker marker, Supplier<?> messageSupplier, Throwable throwable) {
    getLogger().fatal(marker, messageSupplier, throwable);
  }

  @Override
  public void fatal(String message, Supplier<?>... paramSuppliers) {
    getLogger().fatal(message, paramSuppliers);
  }

  @Override
  public void fatal(Marker marker, MessageSupplier messageSupplier) {
    getLogger().fatal(marker, messageSupplier);
  }

  @Override
  public void fatal(Marker marker, MessageSupplier messageSupplier, Throwable throwable) {
    getLogger().fatal(marker, messageSupplier, throwable);
  }

  @Override
  public void fatal(MessageSupplier messageSupplier) {
    getLogger().fatal(messageSupplier);
  }

  @Override
  public void fatal(MessageSupplier messageSupplier, Throwable throwable) {
    getLogger().fatal(messageSupplier, throwable);
  }

  @Override
  public void fatal(Marker marker, String message, Object p0) {
    getLogger().fatal(marker, message, p0);
  }

  @Override
  public void fatal(Marker marker, String message, Object p0, Object p1) {
    getLogger().fatal(marker, message, p0, p1);
  }

  @Override
  public void fatal(Marker marker, String message, Object p0, Object p1, Object p2) {
    getLogger().fatal(marker, message, p0, p1, p2);
  }

  @Override
  public void fatal(Marker marker, String message, Object p0, Object p1, Object p2, Object p3) {
    getLogger().fatal(marker, message, p0, p1, p2, p3);
  }

  @Override
  public void fatal(Marker marker, String message, Object p0, Object p1, Object p2, Object p3, Object p4) {
    getLogger().fatal(marker, message, p0, p1, p2, p3, p4);
  }

  @Override
  public void fatal(Marker marker, String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5) {
    getLogger().fatal(marker, message, p0, p1, p2, p3, p4, p5);
  }

  @Override
  public void fatal(Marker marker, String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6) {
    getLogger().fatal(marker, message, p0, p1, p2, p3, p4, p5, p6);
  }

  @Override
  public void fatal(Marker marker, String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6,
                    Object p7) {
    getLogger().fatal(marker, message, p0, p1, p2, p3, p4, p5, p6, p7);
  }

  @Override
  public void fatal(Marker marker, String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6,
                    Object p7, Object p8) {
    getLogger().fatal(marker, message, p0, p1, p2, p3, p4, p5, p6, p7, p8);
  }

  @Override
  public void fatal(Marker marker, String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6,
                    Object p7, Object p8, Object p9) {
    getLogger().fatal(marker, message, p0, p1, p2, p3, p4, p5, p6, p7, p8, p9);
  }

  @Override
  public void fatal(String message, Object p0) {
    getLogger().fatal(message, p0);
  }

  @Override
  public void fatal(String message, Object p0, Object p1) {
    getLogger().fatal(message, p0, p1);
  }

  @Override
  public void fatal(String message, Object p0, Object p1, Object p2) {
    getLogger().fatal(message, p0, p1, p2);
  }

  @Override
  public void fatal(String message, Object p0, Object p1, Object p2, Object p3) {
    getLogger().fatal(message, p0, p1, p2, p3);
  }

  @Override
  public void fatal(String message, Object p0, Object p1, Object p2, Object p3, Object p4) {
    getLogger().fatal(message, p0, p1, p2, p3, p4);
  }

  @Override
  public void fatal(String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5) {
    getLogger().fatal(message, p0, p1, p2, p3, p4, p5);
  }

  @Override
  public void fatal(String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6) {
    getLogger().fatal(message, p0, p1, p2, p3, p4, p5, p6);
  }

  @Override
  public void fatal(String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6, Object p7) {
    getLogger().fatal(message, p0, p1, p2, p3, p4, p5, p6, p7);
  }

  @Override
  public void fatal(String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6, Object p7,
                    Object p8) {
    getLogger().fatal(message, p0, p1, p2, p3, p4, p5, p6, p7, p8);
  }

  @Override
  public void fatal(String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6, Object p7,
                    Object p8, Object p9) {
    getLogger().fatal(message, p0, p1, p2, p3, p4, p5, p6, p7, p8, p9);
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
  public void info(Marker marker, CharSequence message) {
    getLogger().info(marker, message);
  }

  @Override
  public void info(Marker marker, CharSequence message, Throwable throwable) {
    getLogger().info(marker, message, throwable);
  }

  @Override
  public void info(CharSequence message) {
    getLogger().info(message);
  }

  @Override
  public void info(CharSequence message, Throwable throwable) {
    getLogger().info(message, throwable);
  }

  @Override
  public void info(Supplier<?> messageSupplier) {
    getLogger().info(messageSupplier);
  }

  @Override
  public void info(Supplier<?> messageSupplier, Throwable throwable) {
    getLogger().info(messageSupplier, throwable);
  }

  @Override
  public void info(Marker marker, Supplier<?> messageSupplier) {
    getLogger().info(marker, messageSupplier);
  }

  @Override
  public void info(Marker marker, String message, Supplier<?>... paramSuppliers) {
    getLogger().info(marker, message, paramSuppliers);
  }

  @Override
  public void info(Marker marker, Supplier<?> messageSupplier, Throwable throwable) {
    getLogger().info(marker, messageSupplier, throwable);
  }

  @Override
  public void info(String message, Supplier<?>... paramSuppliers) {
    getLogger().info(message, paramSuppliers);
  }

  @Override
  public void info(Marker marker, MessageSupplier messageSupplier) {
    getLogger().info(marker, messageSupplier);
  }

  @Override
  public void info(Marker marker, MessageSupplier messageSupplier, Throwable throwable) {
    getLogger().info(marker, messageSupplier, throwable);
  }

  @Override
  public void info(MessageSupplier messageSupplier) {
    getLogger().info(messageSupplier);
  }

  @Override
  public void info(MessageSupplier messageSupplier, Throwable throwable) {
    getLogger().info(messageSupplier, throwable);
  }

  @Override
  public void info(Marker marker, String message, Object p0) {
    getLogger().info(marker, message, p0);
  }

  @Override
  public void info(Marker marker, String message, Object p0, Object p1) {
    getLogger().info(marker, message, p0, p1);
  }

  @Override
  public void info(Marker marker, String message, Object p0, Object p1, Object p2) {
    getLogger().info(marker, message, p0, p1, p2);
  }

  @Override
  public void info(Marker marker, String message, Object p0, Object p1, Object p2, Object p3) {
    getLogger().info(marker, message, p0, p1, p2, p3);
  }

  @Override
  public void info(Marker marker, String message, Object p0, Object p1, Object p2, Object p3, Object p4) {
    getLogger().info(marker, message, p0, p1, p2, p3, p4);
  }

  @Override
  public void info(Marker marker, String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5) {
    getLogger().info(marker, message, p0, p1, p2, p3, p4, p5);
  }

  @Override
  public void info(Marker marker, String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6) {
    getLogger().info(marker, message, p0, p1, p2, p3, p4, p5, p6);
  }

  @Override
  public void info(Marker marker, String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6,
                   Object p7) {
    getLogger().info(marker, message, p0, p1, p2, p3, p4, p5, p6, p7);
  }

  @Override
  public void info(Marker marker, String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6,
                   Object p7, Object p8) {
    getLogger().info(marker, message, p0, p1, p2, p3, p4, p5, p6, p7, p8);
  }

  @Override
  public void info(Marker marker, String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6,
                   Object p7, Object p8, Object p9) {
    getLogger().info(marker, message, p0, p1, p2, p3, p4, p5, p6, p7, p8, p9);
  }

  @Override
  public void info(String message, Object p0) {
    getLogger().info(message, p0);
  }

  @Override
  public void info(String message, Object p0, Object p1) {
    getLogger().info(message, p0, p1);
  }

  @Override
  public void info(String message, Object p0, Object p1, Object p2) {
    getLogger().info(message, p0, p1, p2);
  }

  @Override
  public void info(String message, Object p0, Object p1, Object p2, Object p3) {
    getLogger().info(message, p0, p1, p2, p3);
  }

  @Override
  public void info(String message, Object p0, Object p1, Object p2, Object p3, Object p4) {
    getLogger().info(message, p0, p1, p2, p3, p4);
  }

  @Override
  public void info(String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5) {
    getLogger().info(message, p0, p1, p2, p3, p4, p5);
  }

  @Override
  public void info(String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6) {
    getLogger().info(message, p0, p1, p2, p3, p4, p5, p6);
  }

  @Override
  public void info(String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6, Object p7) {
    getLogger().info(message, p0, p1, p2, p3, p4, p5, p6, p7);
  }

  @Override
  public void info(String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6, Object p7,
                   Object p8) {
    getLogger().info(message, p0, p1, p2, p3, p4, p5, p6, p7, p8);
  }

  @Override
  public void info(String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6, Object p7,
                   Object p8, Object p9) {
    getLogger().info(message, p0, p1, p2, p3, p4, p5, p6, p7, p8, p9);
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
  public void log(Level level, Marker marker, CharSequence message) {
    getLogger().log(level, marker, message);
  }

  @Override
  public void log(Level level, Marker marker, CharSequence message, Throwable throwable) {
    getLogger().log(level, marker, message, throwable);
  }

  @Override
  public void log(Level level, CharSequence message) {
    getLogger().log(level, message);
  }

  @Override
  public void log(Level level, CharSequence message, Throwable throwable) {
    getLogger().log(level, message, throwable);
  }

  @Override
  public void log(Level level, Supplier<?> messageSupplier) {
    getLogger().log(level, messageSupplier);
  }

  @Override
  public void log(Level level, Supplier<?> messageSupplier, Throwable throwable) {
    getLogger().log(level, messageSupplier, throwable);
  }

  @Override
  public void log(Level level, Marker marker, Supplier<?> messageSupplier) {
    getLogger().log(level, marker, messageSupplier);
  }

  @Override
  public void log(Level level, Marker marker, String message, Supplier<?>... paramSuppliers) {
    getLogger().log(level, marker, message, paramSuppliers);
  }

  @Override
  public void log(Level level, Marker marker, Supplier<?> messageSupplier, Throwable throwable) {
    getLogger().log(level, marker, messageSupplier, throwable);
  }

  @Override
  public void log(Level level, String message, Supplier<?>... paramSuppliers) {
    getLogger().log(level, message, paramSuppliers);
  }

  @Override
  public void log(Level level, Marker marker, MessageSupplier messageSupplier) {
    getLogger().log(level, marker, messageSupplier);
  }

  @Override
  public void log(Level level, Marker marker, MessageSupplier messageSupplier, Throwable throwable) {
    getLogger().log(level, marker, messageSupplier, throwable);
  }

  @Override
  public void log(Level level, MessageSupplier messageSupplier) {
    getLogger().log(level, messageSupplier);
  }

  @Override
  public void log(Level level, MessageSupplier messageSupplier, Throwable throwable) {
    getLogger().log(level, messageSupplier, throwable);
  }

  @Override
  public void log(Level level, Marker marker, String message, Object p0) {
    getLogger().log(level, marker, message, p0);
  }

  @Override
  public void log(Level level, Marker marker, String message, Object p0, Object p1) {
    getLogger().log(level, marker, message, p0, p1);
  }

  @Override
  public void log(Level level, Marker marker, String message, Object p0, Object p1, Object p2) {
    getLogger().log(level, marker, message, p0, p1, p2);
  }

  @Override
  public void log(Level level, Marker marker, String message, Object p0, Object p1, Object p2, Object p3) {
    getLogger().log(level, marker, message, p0, p1, p2, p3);
  }

  @Override
  public void log(Level level, Marker marker, String message, Object p0, Object p1, Object p2, Object p3, Object p4) {
    getLogger().log(level, marker, message, p0, p1, p2, p3, p4);
  }

  @Override
  public void log(Level level, Marker marker, String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5) {
    getLogger().log(level, marker, message, p0, p1, p2, p3, p4, p5);
  }

  @Override
  public void log(Level level, Marker marker, String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5,
                  Object p6) {
    getLogger().log(level, marker, message, p0, p1, p2, p3, p4, p5, p6);
  }

  @Override
  public void log(Level level, Marker marker, String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5,
                  Object p6, Object p7) {
    getLogger().log(level, marker, message, p0, p1, p2, p3, p4, p5, p6, p7);
  }

  @Override
  public void log(Level level, Marker marker, String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5,
                  Object p6, Object p7, Object p8) {
    getLogger().log(level, marker, message, p0, p1, p2, p3, p4, p5, p6, p7, p8);
  }

  @Override
  public void log(Level level, Marker marker, String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5,
                  Object p6, Object p7, Object p8, Object p9) {
    getLogger().log(level, marker, message, p0, p1, p2, p3, p4, p5, p6, p7, p8, p9);
  }

  @Override
  public void log(Level level, String message, Object p0) {
    getLogger().log(level, message, p0);
  }

  @Override
  public void log(Level level, String message, Object p0, Object p1) {
    getLogger().log(level, message, p0, p1);
  }

  @Override
  public void log(Level level, String message, Object p0, Object p1, Object p2) {
    getLogger().log(level, message, p0, p1, p2);
  }

  @Override
  public void log(Level level, String message, Object p0, Object p1, Object p2, Object p3) {
    getLogger().log(level, message, p0, p1, p2, p3);
  }

  @Override
  public void log(Level level, String message, Object p0, Object p1, Object p2, Object p3, Object p4) {
    getLogger().log(level, message, p0, p1, p2, p3, p4);
  }

  @Override
  public void log(Level level, String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5) {
    getLogger().log(level, message, p0, p1, p2, p3, p4, p5);
  }

  @Override
  public void log(Level level, String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6) {
    getLogger().log(level, message, p0, p1, p2, p3, p4, p5, p6);
  }

  @Override
  public void log(Level level, String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6,
                  Object p7) {
    getLogger().log(level, message, p0, p1, p2, p3, p4, p5, p6, p7);
  }

  @Override
  public void log(Level level, String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6,
                  Object p7, Object p8) {
    getLogger().log(level, message, p0, p1, p2, p3, p4, p5, p6, p7, p8);
  }

  @Override
  public void log(Level level, String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6,
                  Object p7, Object p8, Object p9) {
    getLogger().log(level, message, p0, p1, p2, p3, p4, p5, p6, p7, p8, p9);
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
  public void logIfEnabled(String fqcn, Level level, Marker marker, String message, Object p0) {
    getLogger().logIfEnabled(fqcn, level, marker, message, p0);
  }

  @Override
  public void logIfEnabled(String fqcn, Level level, Marker marker, MessageSupplier msgSupplier, Throwable t) {
    getLogger().logIfEnabled(fqcn, level, marker, msgSupplier, t);
  }

  @Override
  public void logIfEnabled(String fqcn, Level level, Marker marker, CharSequence message, Throwable t) {
    getLogger().logIfEnabled(fqcn, level, marker, message, t);
  }

  @Override
  public void logIfEnabled(String fqcn, Level level, Marker marker, Supplier<?> msgSupplier, Throwable t) {
    getLogger().logIfEnabled(fqcn, level, marker, msgSupplier, t);
  }

  @Override
  public void logIfEnabled(String fqcn, Level level, Marker marker, String message, Supplier<?>... paramSuppliers) {
    getLogger().logIfEnabled(fqcn, level, marker, message, paramSuppliers);
  }

  @Override
  public void logIfEnabled(String fqcn, Level level, Marker marker, String message, Object p0, Object p1) {
    getLogger().logIfEnabled(fqcn, level, marker, message, p0, p1);
  }

  @Override
  public void logIfEnabled(String fqcn, Level level, Marker marker, String message, Object p0, Object p1, Object p2) {
    getLogger().logIfEnabled(fqcn, level, marker, message, p0, p1, p2);
  }

  @Override
  public void logIfEnabled(String fqcn, Level level, Marker marker, String message, Object p0, Object p1, Object p2, Object p3) {
    getLogger().logIfEnabled(fqcn, level, marker, message, p0, p1, p2, p3);
  }

  @Override
  public void logIfEnabled(String fqcn, Level level, Marker marker, String message, Object p0, Object p1, Object p2, Object p3,
                           Object p4) {
    getLogger().logIfEnabled(fqcn, level, marker, message, p0, p1, p2, p3, p4);
  }

  @Override
  public void logIfEnabled(String fqcn, Level level, Marker marker, String message, Object p0, Object p1, Object p2, Object p3,
                           Object p4, Object p5) {
    getLogger().logIfEnabled(fqcn, level, marker, message, p0, p1, p2, p3, p4, p5);
  }

  @Override
  public void logIfEnabled(String fqcn, Level level, Marker marker, String message, Object p0, Object p1, Object p2, Object p3,
                           Object p4, Object p5, Object p6) {
    getLogger().logIfEnabled(fqcn, level, marker, message, p0, p1, p2, p3, p4, p5, p6);
  }

  @Override
  public void logIfEnabled(String fqcn, Level level, Marker marker, String message, Object p0, Object p1, Object p2, Object p3,
                           Object p4, Object p5, Object p6, Object p7) {
    getLogger().logIfEnabled(fqcn, level, marker, message, p0, p1, p2, p3, p4, p5, p6, p7);
  }

  @Override
  public void logIfEnabled(String fqcn, Level level, Marker marker, String message, Object p0, Object p1, Object p2, Object p3,
                           Object p4, Object p5, Object p6, Object p7, Object p8) {
    getLogger().logIfEnabled(fqcn, level, marker, message, p0, p1, p2, p3, p4, p5, p6, p7, p8);
  }

  @Override
  public void logIfEnabled(String fqcn, Level level, Marker marker, String message, Object p0, Object p1, Object p2, Object p3,
                           Object p4, Object p5, Object p6, Object p7, Object p8, Object p9) {
    getLogger().logIfEnabled(fqcn, level, marker, message, p0, p1, p2, p3, p4, p5, p6, p7, p8, p9);
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
  public void trace(Marker marker, CharSequence message) {
    getLogger().trace(marker, message);
  }

  @Override
  public void trace(Marker marker, CharSequence message, Throwable throwable) {
    getLogger().trace(marker, message, throwable);
  }

  @Override
  public void trace(CharSequence message) {
    getLogger().trace(message);
  }

  @Override
  public void trace(CharSequence message, Throwable throwable) {
    getLogger().trace(message, throwable);
  }

  @Override
  public void trace(Supplier<?> messageSupplier) {
    getLogger().trace(messageSupplier);
  }

  @Override
  public void trace(Supplier<?> messageSupplier, Throwable throwable) {
    getLogger().trace(messageSupplier, throwable);
  }

  @Override
  public void trace(Marker marker, Supplier<?> messageSupplier) {
    getLogger().trace(marker, messageSupplier);
  }

  @Override
  public void trace(Marker marker, String message, Supplier<?>... paramSuppliers) {
    getLogger().trace(marker, message, paramSuppliers);
  }

  @Override
  public void trace(Marker marker, Supplier<?> messageSupplier, Throwable throwable) {
    getLogger().trace(marker, messageSupplier, throwable);
  }

  @Override
  public void trace(String message, Supplier<?>... paramSuppliers) {
    getLogger().trace(message, paramSuppliers);
  }

  @Override
  public void trace(Marker marker, MessageSupplier messageSupplier) {
    getLogger().trace(marker, messageSupplier);
  }

  @Override
  public void trace(Marker marker, MessageSupplier messageSupplier, Throwable throwable) {
    getLogger().trace(marker, messageSupplier, throwable);
  }

  @Override
  public void trace(MessageSupplier messageSupplier) {
    getLogger().trace(messageSupplier);
  }

  @Override
  public void trace(MessageSupplier messageSupplier, Throwable throwable) {
    getLogger().trace(messageSupplier, throwable);
  }

  @Override
  public void trace(Marker marker, String message, Object p0) {
    getLogger().trace(marker, message, p0);
  }

  @Override
  public void trace(Marker marker, String message, Object p0, Object p1) {
    getLogger().trace(marker, message, p0, p1);
  }

  @Override
  public void trace(Marker marker, String message, Object p0, Object p1, Object p2) {
    getLogger().trace(marker, message, p0, p1, p2);
  }

  @Override
  public void trace(Marker marker, String message, Object p0, Object p1, Object p2, Object p3) {
    getLogger().trace(marker, message, p0, p1, p2, p3);
  }

  @Override
  public void trace(Marker marker, String message, Object p0, Object p1, Object p2, Object p3, Object p4) {
    getLogger().trace(marker, message, p0, p1, p2, p3, p4);
  }

  @Override
  public void trace(Marker marker, String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5) {
    getLogger().trace(marker, message, p0, p1, p2, p3, p4, p5);
  }

  @Override
  public void trace(Marker marker, String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6) {
    getLogger().trace(marker, message, p0, p1, p2, p3, p4, p5, p6);
  }

  @Override
  public void trace(Marker marker, String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6,
                    Object p7) {
    getLogger().trace(marker, message, p0, p1, p2, p3, p4, p5, p6, p7);
  }

  @Override
  public void trace(Marker marker, String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6,
                    Object p7, Object p8) {
    getLogger().trace(marker, message, p0, p1, p2, p3, p4, p5, p6, p7, p8);
  }

  @Override
  public void trace(Marker marker, String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6,
                    Object p7, Object p8, Object p9) {
    getLogger().trace(marker, message, p0, p1, p2, p3, p4, p5, p6, p7, p8, p9);
  }

  @Override
  public void trace(String message, Object p0) {
    getLogger().trace(message, p0);
  }

  @Override
  public void trace(String message, Object p0, Object p1) {
    getLogger().trace(message, p0, p1);
  }

  @Override
  public void trace(String message, Object p0, Object p1, Object p2) {
    getLogger().trace(message, p0, p1, p2);
  }

  @Override
  public void trace(String message, Object p0, Object p1, Object p2, Object p3) {
    getLogger().trace(message, p0, p1, p2, p3);
  }

  @Override
  public void trace(String message, Object p0, Object p1, Object p2, Object p3, Object p4) {
    getLogger().trace(message, p0, p1, p2, p3, p4);
  }

  @Override
  public void trace(String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5) {
    getLogger().trace(message, p0, p1, p2, p3, p4, p5);
  }

  @Override
  public void trace(String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6) {
    getLogger().trace(message, p0, p1, p2, p3, p4, p5, p6);
  }

  @Override
  public void trace(String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6, Object p7) {
    getLogger().trace(message, p0, p1, p2, p3, p4, p5, p6, p7);
  }

  @Override
  public void trace(String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6, Object p7,
                    Object p8) {
    getLogger().trace(message, p0, p1, p2, p3, p4, p5, p6, p7, p8);
  }

  @Override
  public void trace(String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6, Object p7,
                    Object p8, Object p9) {
    getLogger().trace(message, p0, p1, p2, p3, p4, p5, p6, p7, p8, p9);
  }

  @Override
  public EntryMessage traceEntry() {
    return getLogger().traceEntry();
  }

  @Override
  public EntryMessage traceEntry(String format, Object... params) {
    return getLogger().traceEntry(format, params);
  }

  @Override
  public EntryMessage traceEntry(Supplier<?>... paramSuppliers) {
    return getLogger().traceEntry(paramSuppliers);
  }

  @Override
  public EntryMessage traceEntry(String format, Supplier<?>... paramSuppliers) {
    return getLogger().traceEntry(format, paramSuppliers);
  }

  @Override
  public EntryMessage traceEntry(Message message) {
    return getLogger().traceEntry(message);
  }

  @Override
  public void traceExit() {
    getLogger().traceExit();
  }

  @Override
  public <R> R traceExit(R result) {
    return getLogger().traceExit(result);
  }

  @Override
  public <R> R traceExit(String format, R result) {
    return getLogger().traceExit(format, result);
  }

  @Override
  public void traceExit(EntryMessage message) {
    getLogger().traceExit(message);
  }

  @Override
  public <R> R traceExit(EntryMessage message, R result) {
    return getLogger().traceExit(message, result);
  }

  @Override
  public <R> R traceExit(Message message, R result) {
    return getLogger().traceExit(message, result);
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

  @Override
  public void warn(Marker marker, CharSequence message) {
    getLogger().warn(marker, message);
  }

  @Override
  public void warn(Marker marker, CharSequence message, Throwable throwable) {
    getLogger().warn(marker, message, throwable);
  }

  @Override
  public void warn(CharSequence message) {
    getLogger().warn(message);
  }

  @Override
  public void warn(CharSequence message, Throwable throwable) {
    getLogger().warn(message, throwable);
  }

  @Override
  public void warn(Supplier<?> messageSupplier) {
    getLogger().warn(messageSupplier);
  }

  @Override
  public void warn(Supplier<?> messageSupplier, Throwable throwable) {
    getLogger().warn(messageSupplier, throwable);
  }

  @Override
  public void warn(Marker marker, Supplier<?> messageSupplier) {
    getLogger().warn(marker, messageSupplier);
  }

  @Override
  public void warn(Marker marker, String message, Supplier<?>... paramSuppliers) {
    getLogger().warn(marker, message, paramSuppliers);
  }

  @Override
  public void warn(Marker marker, Supplier<?> messageSupplier, Throwable throwable) {
    getLogger().warn(marker, messageSupplier, throwable);
  }

  @Override
  public void warn(String message, Supplier<?>... paramSuppliers) {
    getLogger().warn(message, paramSuppliers);
  }

  @Override
  public void warn(Marker marker, MessageSupplier messageSupplier) {
    getLogger().warn(marker, messageSupplier);
  }

  @Override
  public void warn(Marker marker, MessageSupplier messageSupplier, Throwable throwable) {
    getLogger().warn(marker, messageSupplier, throwable);
  }

  @Override
  public void warn(MessageSupplier messageSupplier) {
    getLogger().warn(messageSupplier);
  }

  @Override
  public void warn(MessageSupplier messageSupplier, Throwable throwable) {
    getLogger().warn(messageSupplier, throwable);
  }

  @Override
  public void warn(Marker marker, String message, Object p0) {
    getLogger().warn(marker, message, p0);
  }

  @Override
  public void warn(Marker marker, String message, Object p0, Object p1) {
    getLogger().warn(marker, message, p0, p1);
  }

  @Override
  public void warn(Marker marker, String message, Object p0, Object p1, Object p2) {
    getLogger().warn(marker, message, p0, p1, p2);
  }

  @Override
  public void warn(Marker marker, String message, Object p0, Object p1, Object p2, Object p3) {
    getLogger().warn(marker, message, p0, p1, p2, p3);
  }

  @Override
  public void warn(Marker marker, String message, Object p0, Object p1, Object p2, Object p3, Object p4) {
    getLogger().warn(marker, message, p0, p1, p2, p3, p4);
  }

  @Override
  public void warn(Marker marker, String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5) {
    getLogger().warn(marker, message, p0, p1, p2, p3, p4, p5);
  }

  @Override
  public void warn(Marker marker, String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6) {
    getLogger().warn(marker, message, p0, p1, p2, p3, p4, p5, p6);
  }

  @Override
  public void warn(Marker marker, String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6,
                   Object p7) {
    getLogger().warn(marker, message, p0, p1, p2, p3, p4, p5, p6, p7);
  }

  @Override
  public void warn(Marker marker, String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6,
                   Object p7, Object p8) {
    getLogger().warn(marker, message, p0, p1, p2, p3, p4, p5, p6, p7, p8);
  }

  @Override
  public void warn(Marker marker, String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6,
                   Object p7, Object p8, Object p9) {
    getLogger().warn(marker, message, p0, p1, p2, p3, p4, p5, p6, p7, p8, p9);
  }

  @Override
  public void warn(String message, Object p0) {
    getLogger().warn(message, p0);
  }

  @Override
  public void warn(String message, Object p0, Object p1) {
    getLogger().warn(message, p0, p1);
  }

  @Override
  public void warn(String message, Object p0, Object p1, Object p2) {
    getLogger().warn(message, p0, p1, p2);
  }

  @Override
  public void warn(String message, Object p0, Object p1, Object p2, Object p3) {
    getLogger().warn(message, p0, p1, p2, p3);
  }

  @Override
  public void warn(String message, Object p0, Object p1, Object p2, Object p3, Object p4) {
    getLogger().warn(message, p0, p1, p2, p3, p4);
  }

  @Override
  public void warn(String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5) {
    getLogger().warn(message, p0, p1, p2, p3, p4, p5);
  }

  @Override
  public void warn(String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6) {
    getLogger().warn(message, p0, p1, p2, p3, p4, p5, p6);
  }

  @Override
  public void warn(String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6, Object p7) {
    getLogger().warn(message, p0, p1, p2, p3, p4, p5, p6, p7);
  }

  @Override
  public void warn(String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6, Object p7,
                   Object p8) {
    getLogger().warn(message, p0, p1, p2, p3, p4, p5, p6, p7, p8);
  }

  @Override
  public void warn(String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6, Object p7,
                   Object p8, Object p9) {
    getLogger().warn(message, p0, p1, p2, p3, p4, p5, p6, p7, p8, p9);
  }

}
