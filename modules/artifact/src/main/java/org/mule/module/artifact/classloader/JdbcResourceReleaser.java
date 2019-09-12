/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.artifact.classloader;

import static java.lang.Integer.toHexString;
import static java.lang.String.format;
import static java.lang.management.ManagementFactory.getPlatformMBeanServer;
import static java.sql.DriverManager.deregisterDriver;
import static java.sql.DriverManager.getDrivers;

import org.mule.runtime.module.artifact.api.classloader.ResourceReleaser;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Driver;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ReousrceReleaser implementation used for every artifact created on the container that is loaded dynamically as it has
 * to use {@link java.sql.DriverManager} to unregister {@link Driver} registered by the artifact class loader.
 * <p>
 * <p/>
 * IMPORTANT: this class is on a different package than the rest of the classes in this module. The reason of that is that this
 * class must be loaded by each artifact class loader that is being disposed. So, it cannot contain any of the prefixes that force
 * a class to be loaded from the container.
 */
public class JdbcResourceReleaser implements ResourceReleaser {

  public static final String DIAGNOSABILITY_BEAN_NAME = "diagnosability";
  private final transient Logger logger = LoggerFactory.getLogger(getClass());
  private static final List<String> CONNECTION_CLEANUP_THREAD_KNOWN_CLASS_ADDRESES =
      Arrays.asList("com.mysql.jdbc.AbandonedConnectionCleanupThread", "com.mysql.cj.jdbc.AbandonedConnectionCleanupThread");

  @Override
  public void release() {
    deregisterJdbcDrivers();
  }

  private void deregisterJdbcDrivers() {
    Enumeration<Driver> drivers = getDrivers();
    while (drivers.hasMoreElements()) {
      Driver driver = drivers.nextElement();

      // Only unregister drivers that were loaded by the classloader that called this releaser.
      if (isDriverLoadedByThisClassLoader(driver)) {
        doDeregisterDriver(driver);
      } else {
        if (logger.isDebugEnabled()) {
          logger
              .debug(format("Skipping deregister driver %s. It wasn't loaded by the classloader of the artifact being released.",
                            driver.getClass()));
        }
      }
    }
  }

  /**
   * @param driver the JDBC driver to check its {@link ClassLoader} for.
   * @return {@code true} if the {@link ClassLoader} of the driver is a descendant of the {@link ClassLoader} of this releaser,
   * {@code false} otherwise.
   */
  private boolean isDriverLoadedByThisClassLoader(Driver driver) {
    ClassLoader driverClassLoader = driver.getClass().getClassLoader();
    while (driverClassLoader != null) {
      // It has to be the same reference not equals to
      if (driverClassLoader == getClass().getClassLoader()) {
        return true;
      }
      driverClassLoader = driverClassLoader.getParent();
    }

    return false;
  }

  private void doDeregisterDriver(Driver driver) {
    try {
      if (logger.isDebugEnabled()) {
        logger.debug("Deregistering driver: {}", driver.getClass());
      }
      deregisterDriver(driver);

      if (isOracleDriver(driver)) {
        deregisterOracleDiagnosabilityMBean();
      }
      if (isMySqlDriver(driver)) {
        shutdownMySqlAbandonedConnectionCleanupThread();
      }

      if (isDerbyEmbeddedDriver(driver)) {
        leakPreventionForDerbyEmbeddedDriver(driver);
      }
    } catch (Exception e) {
      logger.warn(format("Can not deregister driver %s. This can cause a memory leak.", driver.getClass()), e);
    }
  }

  private boolean isOracleDriver(Driver driver) {
    return isDriver(driver, "oracle.jdbc.OracleDriver");
  }

  private boolean isMySqlDriver(Driver driver) {
    return isDriver(driver, "com.mysql.jdbc.Driver") || isDriver(driver, "com.mysql.cj.jdbc.Driver");
  }

  private boolean isDerbyEmbeddedDriver(Driver driver) {
    // This is the dummy driver which is registered with the DriverManager and which is autoloaded by JDBC4
    return isDriver(driver, "org.apache.derby.jdbc.AutoloadedDriver");
  }

  private boolean isDriver(Driver driver, String expectedDriverClass) {
    try {
      return driver.getClass().getClassLoader().loadClass(expectedDriverClass).isAssignableFrom(driver.getClass());
    } catch (ClassNotFoundException e) {
      // If the class is not found, there is no such driver.
      return false;
    }
  }

  private void deregisterOracleDiagnosabilityMBean() {
    ClassLoader cl = this.getClass().getClassLoader();
    MBeanServer mBeanServer = getPlatformMBeanServer();
    final Hashtable<String, String> keys = new Hashtable<>();
    keys.put("type", DIAGNOSABILITY_BEAN_NAME);
    keys.put("name", cl.getClass().getName() + "@" + toHexString(cl.hashCode()).toLowerCase());

    try {
      mBeanServer.unregisterMBean(new ObjectName("com.oracle.jdbc", keys));
    } catch (javax.management.InstanceNotFoundException e) {
      if (logger.isDebugEnabled()) {
        logger.debug(format("No Oracle's '%s' MBean found.", DIAGNOSABILITY_BEAN_NAME));
      }
    } catch (Throwable e) {
      logger.warn("Unable to unregister Oracle's mbeans", e);
    }
  }

  private void leakPreventionForDerbyEmbeddedDriver(Object driverObject) {
    try {
      if (hasDeclaredMethod(driverObject.getClass(), "connect", String.class, java.util.Properties.class)) {
        Method m = driverObject.getClass().getDeclaredMethod("connect", String.class, java.util.Properties.class);
        m.invoke(driverObject, "jdbc:derby:;shutdown=true", null);
      }
    } catch (Throwable e) {
      Throwable cause = e.getCause();
      if (cause instanceof SQLException) {
        // A successful shutdown always results in an SQLException to indicate that Derby has shut down and that
        // there is no other exception.
        if (logger.isDebugEnabled()) {
          logger.debug("Expected exception when unregister Derby's embedded driver", e);
        }
      } else {
        logger.warn("Unable to unregister Derby's embedded driver", e);
      }
    }
  }

  private boolean hasDeclaredMethod(Class<?> clazz, String methodName, Class<?>... parameterTypes) {
    try {
      return clazz.getDeclaredMethod(methodName, parameterTypes) != null;
    } catch (NoSuchMethodException ex) {
      return false;
    }
  }

  /**
   * Workaround for http://bugs.mysql.com/bug.php?id=65909
   */
  private void shutdownMySqlAbandonedConnectionCleanupThread() {
    try {
      Class<?> cleanupThreadsClass = findMySqlDriverClass();
      shutdownMySqlConnectionCleanupThreads(cleanupThreadsClass);
      // The cleanup threads are fired from a single-thread ThreadPoolExecutor, which is created inside a
      // lambda, which wraps the thread pool into a finalizable wrapper. This leads to retention in the
      // artifact classloaders when several redeployments are performed.
      cleanMySqlCleanupThreadsThreadFactory(cleanupThreadsClass);
    } catch (ClassNotFoundException | NoSuchMethodException | SecurityException | IllegalAccessException
        | IllegalArgumentException | InvocationTargetException e) {
      logger.warn("Unable to shutdown MySql's AbandonedConnectionCleanupThread", e);
    }
  }

  /**
   * Cleans a reference existing from the cleanupThread's class to a lambda-created-threadPoolExecutor, which retains a reference
   * to the DB connector artifact classLoader.
   *
   * @param cleanupThreadsClass The AbandonedConnectionCleanupThread class object
   */
  private void cleanMySqlCleanupThreadsThreadFactory(Class<?> cleanupThreadsClass) {
    // In new mysql driver versions (at least 8), the executor service is wrapped inside a delegate class
    // (DelegatedExecutorService) that exposes only the ExecutorService interface. In order to clean the threadPoolExecutor
    // classloader reference, it has to be extracted manually though reflection from each delegate/wrapper class.
    // Hierarchy leading to real ThreadPoolExecutor is: AbandonedConnectionCleanupThread.cleanupThreadExecutorService ->
    // class DelegatedExecutorService.e -> class ThreadPoolExecutor.
    // Note that the field 'cleanupThreadExcecutorService' is mispelled. There's actually a typo in MySql driver code.
    try {
      Field cleanupExecutorServiceField = cleanupThreadsClass
          .getDeclaredField("cleanupThreadExcecutorService");
      cleanupExecutorServiceField.setAccessible(true);
      ExecutorService delegateCleanupExecutorService =
          (ExecutorService) cleanupExecutorServiceField.get(cleanupThreadsClass);

      Field realExecutorServiceField = delegateCleanupExecutorService.getClass().getSuperclass().getDeclaredField("e");
      realExecutorServiceField.setAccessible(true);
      ThreadPoolExecutor realExecutorService =
          (ThreadPoolExecutor) realExecutorServiceField.get(delegateCleanupExecutorService);

      // Set cleanup thread executor service thread factory to one whose classloader is the system one
      realExecutorService.setThreadFactory(Executors.defaultThreadFactory());
    } catch (NoSuchFieldException | IllegalAccessException e) {
      logger.warn("Error cleaning threadFactory from AbandonedConnectionCleanupThread executor service", e);
    }
  }

  /**
   * Sends a shutdown message to MySql's connection cleanup thread class.
   *
   * @param classAbandonedConnectionCleanupThread
   * @throws IllegalAccessException
   * @throws InvocationTargetException
   * @throws NoSuchMethodException
   */
  private void shutdownMySqlConnectionCleanupThreads(Class<?> classAbandonedConnectionCleanupThread)
      throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
    try {
      Method uncheckedShutdown = classAbandonedConnectionCleanupThread.getMethod("uncheckedShutdown");
      uncheckedShutdown.invoke(null);
    } catch (NoSuchMethodException e) {
      Method checkedShutdown = classAbandonedConnectionCleanupThread.getMethod("shutdown");
      checkedShutdown.invoke(null);
    }
  }

  /**
   * Tries to find the MySql driver AbandonedConnectionCleanupThread class, with the known class addresses.
   *
   * @return The MySql driver AbandonedConnectionCleanupThread class object, if found.
   */
  private Class<?> findMySqlDriverClass() throws ClassNotFoundException {
    for (String knownCleanupThreadClassAddress : CONNECTION_CLEANUP_THREAD_KNOWN_CLASS_ADDRESES) {
      try {
        return this.getClass().getClassLoader().loadClass(knownCleanupThreadClassAddress);
      } catch (ClassNotFoundException e) {
        logger.warn("No AbandonedConnectionCleanupThread registered with class address " + knownCleanupThreadClassAddress);
      }
    }
    throw new ClassNotFoundException("No MySql's AbandonedConnectionCleanupThread class was found");
  }

}
