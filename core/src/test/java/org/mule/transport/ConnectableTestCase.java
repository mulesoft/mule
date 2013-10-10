/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport;

import org.mule.api.context.WorkManager;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.transport.Connectable;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import org.junit.Test;

import static org.junit.Assert.fail;

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
