/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.launcher;

import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Enumeration;

public class DriverManagerCleaner implements Runnable
{

    @Override
    public void run() {
        unregisterDbDrivers();
    }

    private void unregisterDbDrivers()
    {
        Enumeration<Driver> drivers = DriverManager.getDrivers();
        while (drivers.hasMoreElements()) {
            Driver driver = drivers.nextElement();
            try {
                DriverManager.deregisterDriver(driver);
            }
            catch (SQLException e)
            {
                throw new RuntimeException("Can not deregister driver: " + driver.getClass(), e);
            }
        }
    }

}
