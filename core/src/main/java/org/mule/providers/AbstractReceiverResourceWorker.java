/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.providers;

import java.io.OutputStream;
import java.util.ArrayList;

/**
 * This is a Message receiver worker used by transports that do not have a way for the underlying transport
 * to call back to the receiver when a message is available such as Jms. This worker provides a
 * callback {@link #getNextMessage(Object)} where the receiver can read the next message from the underlying
 * transport.
 */
public abstract class AbstractReceiverResourceWorker extends AbstractReceiverWorker
{
    protected Object resource;

    public AbstractReceiverResourceWorker(Object resource, AbstractMessageReceiver receiver)
    {
        this(resource, receiver, null);
    }

    public AbstractReceiverResourceWorker(Object resource, AbstractMessageReceiver receiver, OutputStream out)
    {
        super(new ArrayList(), receiver, out);
        this.resource = resource;
    }


    /**
    * (non-Javadoc)
    *
    */
    //@Override
    public void doRun()
    {
        try
        {
            Object message = getNextMessage(resource);
            while (message != null)
            {
                messages.add(message);
                super.doRun();
                message = getNextMessage(resource);
            }

        }
        catch (Exception e)
        {
            handleException(e);
        }
    }

    /**
     * The method used to read the next message from the underlying transport.
     * @param resource the resource to read from, this may be a socket, a directory or some higher level
     * representation.
     * @return the message read from the resource.  This can be raw data such as a byte[] or a UMOMessageAdapter.
     * @throws Exception
     */
    protected abstract Object getNextMessage(Object resource) throws Exception;
}
