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
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import org.slf4j.Logger;

/**
 * Class created to solve classloading leaks
 */
public class JMSResourceReleaser implements ResourceReleaser {

  private static final String TEST_CLASSLOADER_ARTIFACT_ID = "test";
  private static final String CLASSLOADER_CLASS_TEST_CONTEXT = "AppClassLoader";
  public static final String ACTIVEMQ_DRIVER_TIMER_THREAD_CLASS_NAME = "TimerThread";
  public static final String COMPOSITE_CLASS_LOADER_CLASS_NAME = "CompositeClassLoader";
  public static final String ACTIVEMQ_DRIVER_TIMER_THREAD_NAME = "ActiveMQ InactivityMonitor ReadCheckTimer";

  private final ClassLoader classLoader;

  private static final Logger logger = getLogger(JMSResourceReleaser.class);

  /**
   * Creates a new Instance of JMSResourceReleaser
   *
   * @param classLoader ClassLoader which loaded the ActiveMQ Driver.
   */
  public JMSResourceReleaser(ClassLoader classLoader) {
    this.classLoader = classLoader;
  }

  @Override
  public void release() {
    disposeJMSActiveMQThreads();
  }

  /**
   * Method for delete threads from active mq that still alive. The reason if because they have a reference to other classloader
   * and cant be removed with the GC.
   */
  private void disposeJMSActiveMQThreads() {
    try {
      Thread[] threads = new Thread[activeCount()];
      try {
        enumerate(threads);
      } catch (Throwable t) {
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

    if (isActiveMQInactivityMonitorTimerThreadTestContext(classLoader, thread)) {
      return true;
    }
    if (!(classLoader instanceof ArtifactClassLoader)) {
      return false;
    }
    String artifactId = ((ArtifactClassLoader) classLoader).getArtifactId();
    boolean isActiveMqThread = thread.getName().equals(ACTIVEMQ_DRIVER_TIMER_THREAD_NAME);

    return thread.getClass().getSimpleName().equals(ACTIVEMQ_DRIVER_TIMER_THREAD_CLASS_NAME)
        && isActiveMqThread
        && (isThreadLoadedByDisposedApplication(artifactId, thread.getContextClassLoader())
            || isThreadLoadedByDisposedDomain(artifactId, thread.getContextClassLoader()));
  }

  /**
   * Test Context: threads are loaded from a classloader type - AppClassLoader - Normal Context: loaded by - CompositeClassLoader
   */
  private boolean isActiveMQInactivityMonitorTimerThreadTestContext(ClassLoader classLoader, Thread thread) {

    String artifactId = ((ArtifactClassLoader) classLoader).getArtifactId();
    return artifactId.equals(TEST_CLASSLOADER_ARTIFACT_ID)
        && thread.getContextClassLoader().getClass().getSimpleName().equals(CLASSLOADER_CLASS_TEST_CONTEXT)
        && thread.getName().equals(ACTIVEMQ_DRIVER_TIMER_THREAD_NAME);
  }

  /**
   * To ensure that tha thread belong to the current domain
   */
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
                  this.getClass().getClassLoader());
    }
    return false;
  }

  /**
   * To ensure that tha thread belong to the classloader that we are trying to dispose.
   */
  private boolean isThreadLoadedByDisposedApplication(String undeployedArtifactId, ClassLoader threadContextClassLoader) {
    try {
      if (!(threadContextClassLoader instanceof MuleArtifactClassLoader)) {
        return false;
      }
      String threadClassLoaderArtifactId = ((MuleArtifactClassLoader) threadContextClassLoader).getArtifactId();
      return threadClassLoaderArtifactId != null && threadClassLoaderArtifactId.equals(undeployedArtifactId);
    } catch (Exception e) {
      logger.warn("Exception occurred while attempting to compare {} and {} artifact id.", threadContextClassLoader,
                  this.getClass().getClassLoader());
    }

    return false;
  }

  /**
   * Removing references to allow to delete the timer thread.
   */
  private void clearReferencesStopTimerThread(Thread thread)
      throws InvocationTargetException, IllegalAccessException, NoSuchMethodException {
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
