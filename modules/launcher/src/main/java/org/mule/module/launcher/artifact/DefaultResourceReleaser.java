/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.launcher.artifact;

import static java.lang.Integer.toHexString;
import static java.lang.String.format;
import static java.lang.management.ManagementFactory.getPlatformMBeanServer;
import static java.sql.DriverManager.deregisterDriver;
import static java.sql.DriverManager.getDrivers;

import java.sql.Driver;
import java.util.Enumeration;
import java.util.Hashtable;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultResourceReleaser implements ResourceReleaser
{

    public static final String DIAGNOSABILITY_BEAN_NAME = "diagnosability";
    private Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    public void release()
    {
        deregisterJdbcDrivers();
    }

    private void deregisterJdbcDrivers()
    {
        Enumeration<Driver> drivers = getDrivers();
        while (drivers.hasMoreElements())
        {
            Driver driver = drivers.nextElement();

            // Only unregister drivers that were loaded by the classloader that called this releaser.
            if (isDriverLoadedByThisClassLoader(driver))
            {
                doDeregisterDriver(driver);
            }
            else
            {
                if (logger.isDebugEnabled())
                {
                    logger.debug(format("Skipping deregister driver %s. It wasn't loaded by the classloader of the artifact being released.", driver.getClass()));
                }
            }
        }
    }

    /**
     * @param driver the JDBC driver to check its {@link ClassLoader} for.
     * @return {@code true} if the {@link ClassLoader} of the driver is a descendant of the {@link ClassLoader} of this
     *         releaser, {@code false} otherwise.
     */
    private boolean isDriverLoadedByThisClassLoader(Driver driver)
    {
        ClassLoader driverClassLoader = driver.getClass().getClassLoader();
        while (driverClassLoader != null)
        {
            if (driverClassLoader.equals(getClass().getClassLoader()))
            {
                return true;
            }
            driverClassLoader = driverClassLoader.getParent();
        }

        return false;
    }

    private void doDeregisterDriver(Driver driver)
    {
        try
        {
            logger.debug("Deregistering driver: {}", driver.getClass());
            deregisterDriver(driver);

            if (isOracleDriver(driver))
            {
                deregisterOracleDiagnosabilityMBean();
            }
        }
        catch (Exception e)
        {
            logger.warn(format("Can not deregister driver %s. This can cause a memory leak.", driver.getClass()), e);
        }
    }

    private boolean isOracleDriver(Driver driver)
    {
        return "oracle.jdbc.OracleDriver".equals(driver.getClass().getName());
    }

    private void deregisterOracleDiagnosabilityMBean()
    {
        final ClassLoader cl = this.getClass().getClassLoader();
        final MBeanServer mBeanServer = getPlatformMBeanServer();
        final Hashtable<String, String> keys = new Hashtable<String, String>();
        keys.put("type", DIAGNOSABILITY_BEAN_NAME);
        keys.put("name", cl.getClass().getName() + "@" + toHexString(cl.hashCode()).toLowerCase());

        try
        {
            mBeanServer.unregisterMBean(new ObjectName("com.oracle.jdbc", keys));
        }
        catch (javax.management.InstanceNotFoundException e)
        {
            if (logger.isDebugEnabled())
            {
                logger.debug(format("No Oracle's '%s' MBean found.", DIAGNOSABILITY_BEAN_NAME));
            }
        }
        catch (Throwable e)
        {
            logger.warn("Unable to unregister Oracle's mbeans");
        }
    }

}
