/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport;

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
            receiver.getConnector().getMuleContext().getExceptionListener().handleException(e);
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
