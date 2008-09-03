/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.retry.async;

import org.mule.transport.AbstractConnector;
import org.mule.util.concurrent.Latch;

/**
 * A latch that will release once the associated {@link org.mule.providers.AbstractConnector} is connected.
 * TODO: this is only a prototype implementation, need to improve it
 */
public class ConnectLatch extends Latch
{
    private AbstractConnector connector;

    public ConnectLatch(AbstractConnector connector)
    {
        this.connector = connector;
    }

    //@java.lang.Override
    public void await() throws InterruptedException
    {
        while(!connector.isConnected())
        {
            Thread.sleep(200);
        }
    }

}
