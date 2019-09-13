/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.artifact.classloader;

import static java.beans.Introspector.flushCaches;
import static java.lang.String.format;
import static org.mule.module.artifact.classloader.ThreadGroupContextClassLoaderSoftReferenceBuster.bustSoftReferences;

import org.mule.runtime.module.artifact.api.classloader.ResourceReleaser;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.ResourceBundle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ResourceReleaser implementation that references to the artifact class loader to clean up any reference before the class
 * loader is disposed in order to prevent leaks.
 */
public class ClassLoaderResourceReleaser implements ResourceReleaser {

  private final transient Logger logger = LoggerFactory.getLogger(getClass());

  private volatile ClassLoader classLoader;

  public ClassLoaderResourceReleaser(ClassLoader classLoader) {
    this.classLoader = classLoader;
  }

  @Override
  public void release() {
    shutdownAwsIdleConnectionReaperThread();

    cleanUpResourceBundle();

    clearClassLoaderSoftkeys();
  }

  private void cleanUpResourceBundle() {
    try {
      ResourceBundle.clearCache(this.classLoader);
    } catch (Exception e) {
      logger.warn("Couldn't clean up ResourceBundle. This can cause a memory leak.", e);
    }
  }

  private void clearClassLoaderSoftkeys() {
    try {
      flushCaches();
      bustSoftReferences(this.classLoader);
      // This is added to prompt a gc in the JVM if possible
      // to release the softkeys recently cleared in the caches.
      System.gc();
    } catch (Exception e) {
      logger.warn("Couldn't clear soft keys in caches. This can cause a classloader memory leak.", e);
    }
  }

  /**
   * Shutdowns the AWS IdleConnectionReaper Thread if one is present, since it will cause a leak if not closed correctly.
   */
  private void shutdownAwsIdleConnectionReaperThread() {

    Class<?> idleConnectionReaperClass;
    try {
      idleConnectionReaperClass = this.classLoader.loadClass("com.amazonaws.http.IdleConnectionReaper");
      try {
        Method registeredManagersMethod = idleConnectionReaperClass.getMethod("getRegisteredConnectionManagers");
        List<Object> httpClientConnectionManagers = (List<Object>) registeredManagersMethod.invoke(null);
        if (httpClientConnectionManagers.isEmpty()) {
          return;
        }

        Class<?> httpClientConnectionManagerClass =
            this.classLoader.loadClass("org.apache.http.conn.HttpClientConnectionManager");
        Method removeConnectionManager =
            idleConnectionReaperClass.getMethod("removeConnectionManager", httpClientConnectionManagerClass);
        for (Object connectionManager : httpClientConnectionManagers) {
          boolean removed = (boolean) removeConnectionManager.invoke(null, connectionManager);
          if (!removed && logger.isDebugEnabled()) {
            logger
                .debug(format("Unable to unregister HttpClientConnectionManager instance [%s] associated to AWS's IdleConnectionReaperThread",
                              connectionManager));
          }
        }

      } finally {
        Method shutdown = idleConnectionReaperClass.getMethod("shutdown");
        shutdown.invoke(null);
      }

    } catch (ClassNotFoundException | NoSuchMethodException | IllegalArgumentException e) {
      // If the class or method is not found, there is nothing to dispose
    } catch (SecurityException | IllegalAccessException | InvocationTargetException e) {
      logger.warn("Unable to shutdown AWS's IdleConnectionReaperThread, an error occurred: " + e.getMessage(), e);
    }
  }

}
