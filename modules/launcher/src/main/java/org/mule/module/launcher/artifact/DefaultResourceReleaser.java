/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.launcher.artifact;

import java.lang.management.ManagementFactory;
import java.sql.Driver;
import java.sql.DriverManager;
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
        Enumeration<Driver> drivers = DriverManager.getDrivers();
        while (drivers.hasMoreElements())
        {
            Driver driver = drivers.nextElement();
            try
            {
                logger.debug("Deregistering driver: {}", driver.getClass());
                DriverManager.deregisterDriver(driver);

                if (isOracleDriver(driver))
                {
                    deregisterOracleDiagnosabilityMBean();
                }
            }
            catch (Exception e)
            {
                logger.warn(String.format("Can not deregister driver %s. This can cause a memory leak.", driver.getClass()), e);
            }
        }
    }

    private boolean isOracleDriver(Driver driver)
    {
        return "oracle.jdbc.OracleDriver".equals(driver.getClass().getName());
    }

    private void deregisterOracleDiagnosabilityMBean()
    {
        final ClassLoader cl = Thread.currentThread().getContextClassLoader();
        final MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
        final Hashtable<String, String> keys = new Hashtable<String, String>();
        keys.put("type", DIAGNOSABILITY_BEAN_NAME);
        keys.put("name", cl.getClass().getName() + "@" + Integer.toHexString(cl.hashCode()).toLowerCase());

        try
        {
            mBeanServer.unregisterMBean(new ObjectName("com.oracle.jdbc", keys));
        }
        catch (javax.management.InstanceNotFoundException e)
        {
            if (logger.isDebugEnabled())
            {
                logger.debug(String.format("No Oracle's '%s' MBean found.", DIAGNOSABILITY_BEAN_NAME);
            }
        }
        catch (Throwable e)
        {
            logger.warn("Unable to unregister Oracle's mbeans");
        }
    }

}
