/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.tcp;

import org.mule.DefaultMuleMessage;
import org.mule.api.ExceptionPayload;
import org.mule.api.MuleMessage;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.lifecycle.CreateException;
import org.mule.api.transport.Connector;
import org.mule.message.DefaultExceptionPayload;
import org.mule.transport.AbstractMessageReceiver;
import org.mule.transport.NullPayload;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import javax.resource.spi.work.Work;

/**
 * Extends {@link TcpMessageReceiver} providing managing of protocol error conditions.
 * {@link TcpMessageReceiver.TcpWorker#getNextMessage(Object)} is extended so, in case
 * of an protocol error it will try to send the exception back to the client instead
 * of ignoring it. If an exception is thrown managing the error it will ignored.
 */
public class ExceptionReturnTcpMessageReceiver extends TcpMessageReceiver
{

    public ExceptionReturnTcpMessageReceiver(Connector connector, FlowConstruct flowConstruct,
         InboundEndpoint endpoint)
            throws CreateException
    {
        super(connector, flowConstruct, endpoint);
    }

    @Override
    protected Work createWork(Socket socket) throws IOException
    {
        return new TcpWorker(socket, this);
    }

    protected class TcpWorker extends TcpMessageReceiver.TcpWorker
    {

        public TcpWorker(Socket socket, AbstractMessageReceiver receiver) throws IOException
        {
            super(socket, receiver);
        }

        @Override
        protected Object getNextMessage(Object resource) throws Exception
        {
            try
            {
                return super.getNextMessage(resource);
            }
            catch (Exception e)
            {
                manageException(e);
                return null;
            }
        }

        private void manageException(Exception readingException) throws Exception
        {
            try
            {
                logger.warn("Failed to read message: " + readingException);

                MuleMessage msg = new DefaultMuleMessage(NullPayload.getInstance(), getEndpoint().getMuleContext());
                ExceptionPayload exceptionPayload = new DefaultExceptionPayload(readingException);
                msg.setExceptionPayload(exceptionPayload);
                List msgList = new ArrayList(1);
                msgList.add(msg);

                handleResults(msgList);
            }
            catch (Exception writingException)
            {
                logger.warn("Failed to write exception back to client: " + writingException);
                throw writingException;
            }
        }
    }
}
