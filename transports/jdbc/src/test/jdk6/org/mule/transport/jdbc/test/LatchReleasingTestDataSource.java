/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.jdbc.test;

import org.mule.util.concurrent.Latch;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.dbcp.BasicDataSource;

/**
 * Custom data source pool that holds a latch and releases it after "connectionRequestsUntilRelease" connections
 * are requested to this pool.
 */
public class LatchReleasingTestDataSource extends BasicDataSource
{

    private AtomicInteger connectionRequestsCount = new AtomicInteger(0);
    private int connectionRequestsUntilRelease;
    private Latch latch;

    @Override
    public Connection getConnection() throws SQLException
    {
        if (connectionRequestsCount.incrementAndGet() == connectionRequestsUntilRelease)
        {
            latch.release();
        }
        return super.getConnection();
    }

    public void setLatch(Latch latch)
    {
        this.latch = latch;
    }

    public void setConnectionRequestsUntilRelease(int connectionRequestsUntilRelease)
    {
        this.connectionRequestsUntilRelease = connectionRequestsUntilRelease;
    }

}