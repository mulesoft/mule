/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.module.artifact.classloader;

import static java.lang.String.format;

import static org.slf4j.LoggerFactory.getLogger;

import org.mule.runtime.module.artifact.api.classloader.ResourceReleaser;
import org.mule.sdk.api.artifact.lifecycle.ArtifactLifecycleListener;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import org.slf4j.Logger;

/**
 * Shutdowns the AWS IdleConnectionReaper Thread if one is present, since it will cause a leak if not closed correctly.
 *
 * @since 4.5.0 (it used to be part of {@link ClassLoaderResourceReleaser} before that).
 * @deprecated Since 4.5.0, this releaser has been deprecated in favor of an {@link ArtifactLifecycleListener} in the extensions
 *             that are using the AWS SDK. We still keep it to support legacy extensions.
 */
@Deprecated
public class AwsIdleConnectionReaperResourceReleaser implements ResourceReleaser {

  private static final Logger LOGGER = getLogger(AwsIdleConnectionReaperResourceReleaser.class);

  private final ClassLoader classLoader;

  public AwsIdleConnectionReaperResourceReleaser(ClassLoader classLoader) {
    this.classLoader = classLoader;
  }

  @Override
  public void release() {
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
          if (!removed && LOGGER.isDebugEnabled()) {
            LOGGER
                .warn(format("Unable to unregister HttpClientConnectionManager instance [%s] associated to AWS's IdleConnectionReaperThread",
                             connectionManager));
          }
        }

      } finally {
        Method shutdown = idleConnectionReaperClass.getMethod("shutdown");
        shutdown.invoke(null);
      }

    } catch (ClassNotFoundException | NoSuchMethodException | IllegalArgumentException e) {
      // If the class or method is not found, there is nothing to dispose
    } catch (SecurityException | IllegalAccessException e) {
      LOGGER.warn("Unable to shutdown AWS's IdleConnectionReaperThread, an error occurred: " + e.getMessage(), e);
    } catch (InvocationTargetException e) {
      LOGGER.warn("Unable to shutdown AWS's IdleConnectionReaperThread, an error occurred: " + e.getCause().getMessage(),
                  e.getCause());
    }
  }
}
