/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.jdbc.functional;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import org.mule.api.MuleEvent;
import org.mule.construct.Flow;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.util.MuleDerbyTestDatabase;
import org.mule.util.concurrent.Latch;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import org.apache.commons.dbcp.BasicDataSource;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class JdbcTxOutboundEndpointsTestCase extends FunctionalTestCase
{

    private static MuleDerbyTestDatabase derbyTestDatabase = new MuleDerbyTestDatabase("database.name");

    @Override
    protected String getConfigFile()
    {
        return "jdbc-tx-outbound-endpoints-config.xml";
    }

    @BeforeClass
    public static void startDatabase() throws Exception
    {
        derbyTestDatabase.startDatabase();
    }

    @AfterClass
    public static void stopDatabase() throws SQLException
    {
        derbyTestDatabase.stopDatabase();
    }

    /**
     * This test simulates a deadlock scenario processing two concurrent requests in a flow that contains
     * a transaction with two outbound endpoints (MULE-7729)
     * This indirectly verifies that the default behavior of the JdbcConnector is not to use a pool of dispatcher
     * threads (instead the same MessageDispatcher will be used per endpoint). If a dispatcher pool is used, the
     * deadlock happens and the test fails.
     */
    @Test
    public void concurrentRequestsDoNotGenerateDeadlock() throws Exception
    {
        ExecutorService executorService = Executors.newFixedThreadPool(2);

        // Send first event through the flow (thread 1)
        processEvent(executorService, true);

        // Wait until the first query is executed (now thread 1 holds a connection to the DB as the flow is transactional)
        Latch firstQueryExecutedLatch = muleContext.getRegistry().lookupObject("firstQueryExecutedLatch");
        firstQueryExecutedLatch.await();

        // Send second event through the flow (thread 2)
        processEvent(executorService, false);

        // Thread 2 will skip the first outbound endpoint and will try to execute the second one. It will borrow
        // a dispatcher from the pool (the only one, which is available because thread 1 already released it), but
        // it will block when trying to get a connection from the data source pool (as there is only 1 connection
        // that is held by thread 1). When requesting this connection to the data source, the connectionRequestedLatch
        // is released. Now thread 1 tries to execute the second outbound endpoint. If there is a pool of dispatchers
        // with only 1 dispatcher, it will block because the only dispatcher is already held by thread 2.

        // Wait until both threads finish successfully.
        CountDownLatch finishedLatch = muleContext.getRegistry().lookupObject("finishedLatch");
        assertThat(finishedLatch.await(RECEIVE_TIMEOUT, TimeUnit.MILLISECONDS), is(true));

    }


    private void processEvent(ExecutorService executorService, final boolean executeFirstQuery)
    {
        executorService.submit(new Runnable()
        {
            @Override
            public void run()
            {
                try
                {
                    Flow flow = (Flow) getFlowConstruct("test");
                    MuleEvent event = getTestEvent(TEST_MESSAGE);
                    event.setFlowVariable("executeFirstQuery", executeFirstQuery);
                    flow.process(event);
                }
                catch (Exception e)
                {
                    throw new RuntimeException(e);
                }
            }
        });
    }


    /**
     * Custom data source pool that holds a latch and releases it after "connectionRequestsUntilRelease" connections
     * are requested to this pool.
     */
    public static class LatchReleasingDataSource extends BasicDataSource
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

        @Override
        public Logger getParentLogger() throws SQLFeatureNotSupportedException
        {
            throw new SQLFeatureNotSupportedException();
        }
    }

}
