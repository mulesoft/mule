/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.vm;

/**
 * A short for of the {@link org.mule.transport.vm.VMConnector} with the {@link #setQueueEvents(boolean)} set to true.
 * This allows the URI short form <pre>vmq://my.queue</pre> which will create a VM Queueing connector.
 */
public class VMQueueConnector extends VMConnector
{
    public static final String PROTOCOL = "vmq";

    public VMQueueConnector()
    {
        setQueueEvents(true);
    }

    @Override
    public String getProtocol()
    {
        return PROTOCOL;
    }
}
