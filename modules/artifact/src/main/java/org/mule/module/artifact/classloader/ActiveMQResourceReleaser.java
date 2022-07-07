/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.artifact.classloader;

import static java.lang.Thread.activeCount;
import static java.lang.Thread.enumerate;
import static org.slf4j.LoggerFactory.getLogger;

import org.mule.runtime.module.artifact.api.classloader.ArtifactClassLoader;
import org.mule.runtime.module.artifact.api.classloader.MuleArtifactClassLoader;
import org.mule.runtime.module.artifact.api.classloader.ResourceReleaser;
import org.slf4j.Logger;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

/**
 * A utility class to release all resources associated to Active MQ driver on un-deployment to prevent classloader leaks
 */
public class ActiveMQResourceReleaser implements ResourceReleaser {

  public static final String ACTIVEMQ_DRIVER_TIMER_THREAD_CLASS_NAME = "TimerThread";
  public static final String ACTIVEMQ_DRIVER_TIMER_THREAD_NAME = "ActiveMQ InactivityMonitor ReadCheckTimer";
  public static final String COMPOSITE_CLASS_LOADER_CLASS_NAME = "CompositeClassLoader";

  private final ClassLoader classLoader;
  private static final Logger logger = getLogger(ActiveMQResourceReleaser.class);


  /**
   * Creates a new Instance of ActiveMQResourceReleaser
   *
   * @param classLoader ClassLoader which loaded the ActiveMQ Driver.
   */
  public ActiveMQResourceReleaser(ClassLoader classLoader) {
    this.classLoader = classLoader;
  }

  @Override
  public void release() {
    disposeActiveMQThreads();
  }

  /**
   * Method for delete threads from active mq that still alive. The reason if because they have a reference to other classloader
   * and cant be removed with the GC.
   */
  private void disposeActiveMQThreads() {
    try {
      Thread[] threads = new Thread[activeCount()];
      try {
        enumerate(threads);
      } catch (SecurityException t) {
        logger.debug("An error occurred trying to obtain available Threads. Thread cleanup will be skipped.", t);
        return;
      }

      for (Thread thread : threads) {
        if (isActiveMQInactivityMonitorTimerThread(classLoader, thread)) {
          clearReferencesStopTimerThread(thread);
          thread.interrupt();
          thread.join();
        }
      }
    } catch (Exception e) {
      logger.error("An exception occurred while attempting to dispose of activeMQ timer threads: {}", e.getMessage());
    }
  }

  /**
   * Method that returns if the actual thread is the thread that we want to delete this name have to be -ActiveMQ
   * InactivityMonitor ReadCheckTimer- And make other assertions to know is the thread belong to the current classloader.
   */
  private boolean isActiveMQInactivityMonitorTimerThread(ClassLoader classLoader, Thread thread) {

    if (!(classLoader instanceof ArtifactClassLoader)) {
      return false;
    }
    String artifactId = ((ArtifactClassLoader) classLoader).getArtifactId();

    return thread.getClass().getSimpleName().equals(ACTIVEMQ_DRIVER_TIMER_THREAD_CLASS_NAME)
        && thread.getName().equals(ACTIVEMQ_DRIVER_TIMER_THREAD_NAME)
        && (isThreadLoadedByDisposedApplication(artifactId, thread.getContextClassLoader())
            || isThreadLoadedByDisposedDomain(artifactId, thread.getContextClassLoader()));
  }

  private boolean isThreadLoadedByDisposedDomain(String undeployedArtifactId, ClassLoader threadContextClassLoader) {
    try {

      Class threadContextClassLoaderClass = threadContextClassLoader.getClass();
      if (!threadContextClassLoaderClass.getSimpleName().equals(COMPOSITE_CLASS_LOADER_CLASS_NAME)) {
        return false;
      }


      Method getDelegateClassLoadersMethod = threadContextClassLoaderClass.getMethod("getDelegates");
      List<ClassLoader> classLoaderList = (List<ClassLoader>) getDelegateClassLoadersMethod.invoke(threadContextClassLoader);


      for (ClassLoader classLoaderDelegate : classLoaderList) {
        if (classLoaderDelegate instanceof ArtifactClassLoader) {
          ArtifactClassLoader artifactClassLoader = (ArtifactClassLoader) classLoaderDelegate;
          if (artifactClassLoader.getArtifactId().contains(undeployedArtifactId)) {
            return true;
          }
        }
      }

    } catch (Exception e) {
      logger.warn("Exception occurred while attempting to compare {} and {} artifactId.", threadContextClassLoader,
                  threadContextClassLoader.getClass().getClassLoader());
    }
    return false;
  }

  private boolean isThreadLoadedByDisposedApplication(String undeployedArtifactId, ClassLoader threadContextClassLoader) {
    try {
      if (!(threadContextClassLoader instanceof MuleArtifactClassLoader)) {
        return false;
      }
      String threadClassLoaderArtifactId = ((MuleArtifactClassLoader) threadContextClassLoader).getArtifactId();
      return threadClassLoaderArtifactId != null && threadClassLoaderArtifactId.equals(undeployedArtifactId);
    } catch (Exception e) {
      logger.warn("Exception occurred while attempting to compare {} and {} artifact id.", threadContextClassLoader,
                  threadContextClassLoader.getClass().getClassLoader());
    }
    return false;
  }

  private void clearReferencesStopTimerThread(Thread thread)
      throws InvocationTargetException, IllegalAccessException, NoSuchMethodException {
    // Need to get references to:
    // in Sun/Oracle JDK:
    // - newTasksMayBeScheduled field (in java.util.TimerThread)
    // - queue field
    // - queue.clear()
    // in IBM JDK, Apache Harmony:
    // - cancel() method (in java.util.Timer$TimerImpl)
    try {
      Field newTasksMayBeScheduledField =
          thread.getClass().getDeclaredField("newTasksMayBeScheduled");
      newTasksMayBeScheduledField.setAccessible(true);
      Field queueField = thread.getClass().getDeclaredField("queue");
      queueField.setAccessible(true);
      Object queue = queueField.get(thread);
      Method clearMethod = queue.getClass().getDeclaredMethod("clear");
      clearMethod.setAccessible(true);
      synchronized (queue) {
        newTasksMayBeScheduledField.setBoolean(thread, false);
        clearMethod.invoke(queue);
        // In case queue was already empty. Should only be one
        // thread waiting but use notifyAll() to be safe.
        queue.notifyAll();
      }
    } catch (NoSuchFieldException noSuchFieldEx) {
      logger.warn("Unable to clear timer references using 'clear' method. Attempting to use 'cancel' method.");
      Method cancelMethod = thread.getClass().getDeclaredMethod("cancel");
      cancelMethod.setAccessible(true);
      cancelMethod.invoke(thread);
    }
  }
}
