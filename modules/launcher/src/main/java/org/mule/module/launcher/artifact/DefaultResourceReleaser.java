/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.launcher.artifact;

import java.sql.Driver;
import java.sql.DriverManager;
import java.util.Enumeration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultResourceReleaser implements ResourceReleaser
{

    private Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    public void release()
    {
        deregisterDrivers();
    }

    private void deregisterDrivers()
    {
        Enumeration<Driver> drivers = DriverManager.getDrivers();
        while (drivers.hasMoreElements())
        {
            Driver driver = drivers.nextElement();
            try
            {
                logger.debug("Deregistering driver: " + driver.getClass());
                DriverManager.deregisterDriver(driver);
            }
            catch (Exception e)
            {
                logger.warn("Can not deregister driver: " + driver.getClass(), e);
            }
        }
    }

}
