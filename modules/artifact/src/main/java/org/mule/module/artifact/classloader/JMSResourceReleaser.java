/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.artifact.classloader;

import static org.mule.module.artifact.classloader.ThreadCommonMethodsUtil.*;
import static java.lang.Thread.activeCount;
import static java.lang.Thread.enumerate;
import static org.slf4j.LoggerFactory.getLogger;

import org.mule.runtime.module.artifact.api.classloader.ArtifactClassLoader;
import org.mule.runtime.module.artifact.api.classloader.ResourceReleaser;

import org.slf4j.Logger;

/**
 * Class created to solve classloading leaks
 */
public class JMSResourceReleaser implements ResourceReleaser {

  private static final String TEST_CLASSLOADER_ARTIFACT_ID = "test";
  private static final String CLASSLOADER_CLASS_TEST_CONTEXT = "AppClassLoader";
  public static final String ACTIVEMQ_DRIVER_TIMER_THREAD_CLASS_NAME = "TimerThread";
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
}