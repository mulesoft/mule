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

import javax.management.MBeanServer;
import javax.management.ObjectName;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Driver;
import java.sql.SQLException;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.ResourceBundle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default implementation used for every artifact created on the container.
 * <p/>
 * IMPORTANT: this class is on a different package than the rest of the classes in this module. The reason of that is that this
 * class must be loaded by each artifact class loader that is being disposed. So, it cannot contain any of the prefixes that force
 * a class to be loaded from the container.
 */
public class DefaultResourceReleaser implements ResourceReleaser {

  public static final String DIAGNOSABILITY_BEAN_NAME = "diagnosability";
  private final transient Logger logger = LoggerFactory.getLogger(getClass());

  @Override
  public void release() {
    deregisterJdbcDrivers();

    cleanUpResourceBundle();
  }

  private void cleanUpResourceBundle() {
    try {
      ResourceBundle.clearCache(this.getClass().getClassLoader());
    } catch (Exception e) {
      logger.warn("Couldn't clean up ResourceBundle. This can cause a memory leak.", e);
    }

    try {
      // Even the cache is cleaned up there is a reference to a CacheKey that is hold by ResouceBundle.NONEXISTENT_BUNDLE
      // so we have to cleanup this reference too instead of just leaving it and waiting for a next
      // ResourceBundle.getBundle() call to a bundle that is missing that would change this reference
      Field nonExistentBundleField = ResourceBundle.class.getDeclaredField("NONEXISTENT_BUNDLE");
      nonExistentBundleField.setAccessible(true);

      ResourceBundle resourceBundle = (ResourceBundle) nonExistentBundleField.get(null);

      Field cacheKeyField = ResourceBundle.class.getDeclaredField("cacheKey");
      cacheKeyField.setAccessible(true);
      cacheKeyField.set(resourceBundle, null);
    } catch (Exception e) {
      logger.warn("Couldn't clean up ResourceBundle references. This can cause a memory leak.", e);
    }
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
   *         {@code false} otherwise.
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
    return isDriver(driver, "com.mysql.jdbc.Driver");
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
      Class<?> classAbandonedConnectionCleanupThread =
          this.getClass().getClassLoader().loadClass("com.mysql.jdbc.AbandonedConnectionCleanupThread");
      Method methodShutdown = classAbandonedConnectionCleanupThread.getMethod("shutdown");
      methodShutdown.invoke(null);
    } catch (ClassNotFoundException | NoSuchMethodException | SecurityException | IllegalAccessException
        | IllegalArgumentException | InvocationTargetException e) {
      logger.warn("Unable to shutdown MySql's AbandonedConnectionCleanupThread", e);
    }
  }
}
