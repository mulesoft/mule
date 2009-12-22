/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport;

import org.mule.api.context.WorkManager;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.tck.AbstractMuleTestCase;

public class ConnectableTestCase extends AbstractMuleTestCase
{

    /**
     * MULE-4531
     */
    public void testDoNotConnectIfConnected() throws Exception
    {
        AbstractConnectable connectable = new TestConnectable(getTestInboundEndpoint("test"), true);
        connectable.connect();
    }

    class TestConnectable extends AbstractConnectable
    {
        public TestConnectable(ImmutableEndpoint endpoint, boolean connected)
        {
            super(endpoint);
            this.connected.set(connected);
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
