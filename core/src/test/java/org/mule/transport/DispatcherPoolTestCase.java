/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport;

import org.mule.api.config.ThreadingProfile;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.transport.MessageDispatcher;
import org.mule.config.ImmutableThreadingProfile;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.tck.testmodels.mule.TestConnector;

import org.apache.commons.pool.impl.GenericKeyedObjectPool;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class DispatcherPoolTestCase extends AbstractMuleContextTestCase
{

    @Test
    public void testDefaultDispatcherPoolConfiguration() throws Exception
    {
        final TestConnector connector = createConnectorWithSingleObjectDispatcherPool(ThreadingProfile.WHEN_EXHAUSTED_RUN);

        // ThreadingProfile exhausted action default is RUN
        assertEquals(ThreadingProfile.WHEN_EXHAUSTED_RUN, connector.getDispatcherThreadingProfile()
            .getPoolExhaustedAction());
        assertEquals(2, connector.dispatchers.getMaxActive());
        // This must equal maxActive dispatchers because low maxIdle would result in
        // a lot of dispatcher churn
        assertEquals(2, connector.dispatchers.getMaxIdle());
        assertEquals(GenericKeyedObjectPool.WHEN_EXHAUSTED_BLOCK,
            connector.dispatchers.getWhenExhaustedAction());
        assertEquals(-1, connector.dispatchers.getMaxWait());
    }

    @Test
    public void testDefaultDispatcherPoolConfigurationThreadingProfileWait() throws Exception
    {
        final TestConnector connector = createConnectorWithSingleObjectDispatcherPool(ThreadingProfile.WHEN_EXHAUSTED_WAIT);

        assertEquals(ThreadingProfile.WHEN_EXHAUSTED_WAIT, connector.getDispatcherThreadingProfile()
            .getPoolExhaustedAction());
        assertEquals(1, connector.dispatchers.getMaxActive());
        assertEquals(1, connector.dispatchers.getMaxIdle());
        assertEquals(GenericKeyedObjectPool.WHEN_EXHAUSTED_BLOCK,
            connector.dispatchers.getWhenExhaustedAction());
        assertEquals(-1, connector.dispatchers.getMaxWait());
    }

    @Test
    public void testDispatcherPoolDefaultBlockExhaustedAction() throws Exception
    {
        final TestConnector connector = createConnectorWithSingleObjectDispatcherPool(ThreadingProfile.WHEN_EXHAUSTED_WAIT);
        connector.setDispatcherPoolMaxWait(100);

        assertEquals(1, connector.dispatchers.getMaxActive());
        assertEquals(100, connector.dispatchers.getMaxWait());

        final OutboundEndpoint endpoint = getTestOutboundEndpoint("test", "test://test");

        new Thread(new Runnable()
        {
            public void run()
            {
                try
                {
                    MessageDispatcher messageDispatcher = (MessageDispatcher) connector.dispatchers.borrowObject(endpoint);
                    Thread.sleep(50);
                    connector.dispatchers.returnObject(endpoint, messageDispatcher);
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }

            }
        }).start();
        Thread.sleep(10);
        assertEquals(1, connector.dispatchers.getNumActive());
        connector.dispatchers.borrowObject(endpoint);
        assertEquals(1, connector.dispatchers.getNumActive());

    }

    @Test
    public void testDispatcherPoolBlockTimeoutExhaustedAction() throws Exception
    {
        final TestConnector connector = createConnectorWithSingleObjectDispatcherPool(ThreadingProfile.WHEN_EXHAUSTED_WAIT);
        connector.setDispatcherPoolMaxWait(10);

        assertEquals(1, connector.dispatchers.getMaxActive());
        assertEquals(10, connector.dispatchers.getMaxWait());

        final OutboundEndpoint endpoint = getTestOutboundEndpoint("test", "test://test");

        new Thread(new Runnable()
        {
            public void run()
            {
                try
                {
                    MessageDispatcher messageDispatcher = (MessageDispatcher) connector.dispatchers.borrowObject(endpoint);
                    Thread.sleep(200);
                    connector.dispatchers.returnObject(endpoint, messageDispatcher);
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }

            }
        }).start();
        Thread.sleep(10);
        assertEquals(1, connector.dispatchers.getNumActive());
        try
        {
            connector.dispatchers.borrowObject(endpoint);
            fail("Exception expected");
        }
        catch (Exception e)
        {
            assertEquals(1, connector.dispatchers.getNumActive());
        }
    }

    @Test
    public void testDispatcherPoolGrowExhaustedAction() throws Exception
    {
        final TestConnector connector = createConnectorWithSingleObjectDispatcherPool(ThreadingProfile.WHEN_EXHAUSTED_WAIT);
        connector.setDispatcherPoolWhenExhaustedAction(GenericKeyedObjectPool.WHEN_EXHAUSTED_GROW);

        assertEquals(1, connector.dispatchers.getMaxActive());

        final OutboundEndpoint endpoint = getTestOutboundEndpoint("test", "test://test");

        connector.dispatchers.borrowObject(endpoint);
        connector.dispatchers.borrowObject(endpoint);
        assertEquals(2, connector.dispatchers.getNumActive());

    }

    @Test
    public void testDispatcherPoolFailExhaustedAction() throws Exception
    {
        final TestConnector connector = createConnectorWithSingleObjectDispatcherPool(ThreadingProfile.WHEN_EXHAUSTED_WAIT);
        connector.setDispatcherPoolWhenExhaustedAction(GenericKeyedObjectPool.WHEN_EXHAUSTED_FAIL);

        assertEquals(1, connector.dispatchers.getMaxActive());

        final OutboundEndpoint endpoint = getTestOutboundEndpoint("test", "test://test");

        connector.dispatchers.borrowObject(endpoint);
        try
        {
            connector.dispatchers.borrowObject(endpoint);
            fail("Exception expected");
        }
        catch (Exception e)
        {
            assertEquals(1, connector.dispatchers.getNumActive());
        }
    }

    private TestConnector createConnectorWithSingleObjectDispatcherPool(int exhaustedAction) throws Exception
    {
        TestConnector connector = new TestConnector(muleContext);
        ThreadingProfile threadingProfile = new ImmutableThreadingProfile(1, 1, 1, 1, 1, exhaustedAction,
            true, null, null);
        connector.setDispatcherThreadingProfile(threadingProfile);
        connector.createReceiver(getTestFlow(), getTestInboundEndpoint("test", "test://test"));
        muleContext.getRegistry().registerConnector(connector);
        return connector;
    }

}
