/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.transport;

import static org.junit.Assert.fail;

import org.mule.runtime.core.api.connector.Connectable;
import org.mule.runtime.core.api.context.WorkManager;
import org.mule.runtime.core.api.endpoint.ImmutableEndpoint;
import org.mule.runtime.core.transport.AbstractTransportMessageHandler;
import org.mule.runtime.core.transport.ConnectableLifecycleManager;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import org.junit.Test;

public class ConnectableTestCase extends AbstractMuleContextTestCase
{

    /**
     * MULE-4531
     */
    @Test
    public void testDoNotConnectIfConnected() throws Exception
    {
        Connectable connectable = new TestConnectable(getTestInboundEndpoint("test"), true);
        connectable.connect();
    }

    class TestConnectable extends AbstractTransportMessageHandler
    {
        public TestConnectable(ImmutableEndpoint endpoint, boolean connected)
        {
            super(endpoint);
            this.connected.set(connected);
        }

        @Override
        protected ConnectableLifecycleManager createLifecycleManager()
        {
            return new ConnectableLifecycleManager("test", this);
        }

        @Override
        protected WorkManager getWorkManager()
        {
            return null;
        }

        @Override
        protected void doConnect() throws Exception
        {
            if (connected.get())
            {
                fail("Should not attempt connection");
            }
            super.doConnect();
        }

    }

}
