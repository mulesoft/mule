/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.artifact.classloader;

import org.mule.runtime.module.artifact.api.classloader.ArtifactClassLoader;
import org.mule.runtime.module.artifact.api.classloader.MuleArtifactClassLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

public class ThreadCommonMethodsUtil {

  private static final Logger logger = LoggerFactory.getLogger(ThreadCommonMethodsUtil.class);
  public static final String COMPOSITE_CLASS_LOADER_CLASS_NAME = "CompositeClassLoader";

  public static boolean isThreadLoadedByDisposedDomain(String undeployedArtifactId, ClassLoader threadContextClassLoader) {
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

  public static boolean isThreadLoadedByDisposedApplication(String undeployedArtifactId, ClassLoader threadContextClassLoader) {
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

  public static void clearReferencesStopTimerThread(Thread thread)
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
