/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.transport;

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
        super(new ArrayList<Object>(), receiver, out);
        this.resource = resource;
    }

    @Override
    public void doRun()
    {
        try
        {
            Object message;
            do 
            {
                message = getNextMessage(resource);
                messages.add(message);
                super.doRun();
            }
            while (message != null && hasMoreMessages(message));
        }
        catch (Exception e)
        {
            endpoint.getMuleContext().getExceptionListener().handleException(e);
        }
    }

    protected boolean hasMoreMessages(Object message)
    {
        return true;
    }
    
    /**
     * The method used to read the next message from the underlying transport.
     * @param resource the resource to read from, this may be a socket, a directory or some higher level
     * representation.
     * @return the message read from the resource.  This can be raw data such as a byte[].
     * @throws Exception
     */
    protected abstract Object getNextMessage(Object resource) throws Exception;
}
