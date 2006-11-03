/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.vm;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.MuleManager;
import org.mule.config.i18n.Message;
import org.mule.config.i18n.Messages;
import org.mule.impl.MuleMessage;
import org.mule.providers.AbstractMessageDispatcher;
import org.mule.transformers.simple.ObjectToByteArray;
import org.mule.umo.UMOEvent;
import org.mule.umo.UMOException;
import org.mule.umo.UMOMessage;
import org.mule.umo.endpoint.UMOEndpointURI;
import org.mule.umo.endpoint.UMOImmutableEndpoint;
import org.mule.umo.provider.DispatchException;
import org.mule.umo.provider.NoReceiverForEndpointException;
import org.mule.umo.provider.UMOConnector;
import org.mule.umo.provider.UMOStreamMessageAdapter;
import org.mule.util.queue.Queue;
import org.mule.util.queue.QueueSession;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

/**
 * <code>VMMessageDispatcher</code> is used for providing in memory interaction
 * between components.
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @author <a href="mailto:gnt@codehaus.org">Guillaume Nodet</a>
 * @version $Revision$
 */
public class VMMessageDispatcher extends AbstractMessageDispatcher
{
    /**
     * logger used by this class
     */
    private static Log logger = LogFactory.getLog(VMMessageDispatcher.class);

    private VMConnector connector;

    private ObjectToByteArray objectToByteArray;

    public VMMessageDispatcher(UMOImmutableEndpoint endpoint)
    {
        super(endpoint);
        this.connector = (VMConnector)endpoint.getConnector();
        objectToByteArray = new ObjectToByteArray();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.umo.provider.UMOMessageDispatcher#getDelegateSession()
     */
    public Object getDelegateSession() throws UMOException
    {
        return null;
    }

    /**
     * Make a specific request to the underlying transport
     * 
     * @param endpoint the endpoint to use when connecting to the resource
     * @param timeout the maximum time the operation should block before returning.
     *            The call should return immediately if there is data available. If
     *            no data becomes available before the timeout elapses, null will be
     *            returned
     * @return the result of the request wrapped in a UMOMessage object. Null will be
     *         returned if no data was avaialable
     * @throws Exception if the call to the underlying protocal cuases an exception
     */
    protected UMOMessage doReceive(UMOImmutableEndpoint endpoint, long timeout) throws Exception
    {

        if (!connector.isQueueEvents())
        {
            throw new UnsupportedOperationException("Receive only supported on the VM Queue Connector");
        }
        QueueSession queueSession;
        try
        {
            queueSession = connector.getQueueSession();
            Queue queue = queueSession.getQueue(endpoint.getEndpointURI().getAddress());
            if (queue == null)
            {
                if (logger.isDebugEnabled())
                {
                    logger.debug("No queue with name " + endpoint.getEndpointURI().getAddress());
                }
                return null;
            }
            else
            {
                UMOEvent event = null;
                if (logger.isDebugEnabled())
                {
                    logger.debug("Waiting for a message on " + endpoint.getEndpointURI().getAddress());
                }
                try
                {
                    event = (UMOEvent)queue.poll(timeout);
                }
                catch (InterruptedException e)
                {
                    logger.error("Failed to receive event from queue: " + endpoint.getEndpointURI());
                }
                if (event != null)
                {
                    if (logger.isDebugEnabled())
                    {
                        logger.debug("Event received: " + event);
                    }
                    return event.getMessage();
                }
                else
                {
                    if (logger.isDebugEnabled())
                    {
                        logger.debug("No event received after " + timeout + " ms");
                    }
                    return null;
                }
            }
        }
        catch (Exception e)
        {
            throw e;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.umo.provider.UMOConnector#dispatch(org.mule.umo.UMOEvent)
     */
    protected void doDispatch(UMOEvent event) throws Exception
    {
        UMOEndpointURI endpointUri = event.getEndpoint().getEndpointURI();

        if (endpointUri == null)
        {
            throw new DispatchException(new Message(Messages.X_IS_NULL, "Endpoint"), event.getMessage(),
                event.getEndpoint());
        }
        if (connector.isQueueEvents())
        {
            QueueSession session = connector.getQueueSession();
            Queue queue = session.getQueue(endpointUri.getAddress());
            queue.put(event);
        }
        else
        {
            VMMessageReceiver receiver = connector.getReceiver(event.getEndpoint().getEndpointURI());
            if (receiver == null)
            {
                logger.warn("No receiver for endpointUri: " + event.getEndpoint().getEndpointURI());
                return;
            }

            if (event.isStreaming())
            {

                PipedInputStream in = new PipedInputStream();
                PipedOutputStream out = new PipedOutputStream(in);
                UMOStreamMessageAdapter sma = connector.getStreamMessageAdapter(in, out);
                sma.write(event);
            }
            receiver.onEvent(event);
        }
        if (logger.isDebugEnabled())
        {
            logger.debug("dispatched Event on endpointUri: " + endpointUri);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.umo.provider.UMOConnector#send(org.mule.umo.UMOEvent)
     */
    protected UMOMessage doSend(UMOEvent event) throws Exception
    {
        UMOMessage retMessage;
        UMOEndpointURI endpointUri = event.getEndpoint().getEndpointURI();
        VMMessageReceiver receiver = connector.getReceiver(endpointUri);
        if (receiver == null)
        {
            if (connector.isQueueEvents())
            {
                if (logger.isDebugEnabled())
                {
                    logger.debug("Writing to queue as there is no receiver on connector: "
                                 + connector.getName() + ", for endpointUri: "
                                 + event.getEndpoint().getEndpointURI());
                }
                doDispatch(event);
                return null;
            }
            else
            {
                throw new NoReceiverForEndpointException(new Message(Messages.NO_RECEIVER_X_FOR_ENDPOINT_X,
                    connector.getName(), event.getEndpoint().getEndpointURI()));
            }
        }
        if (event.isStreaming())
        {
            PipedInputStream in = new PipedInputStream();
            PipedOutputStream out = new PipedOutputStream(in);
            UMOStreamMessageAdapter sma = connector.getStreamMessageAdapter(in, out);
            sma.write(event);
        }

        retMessage = (UMOMessage)receiver.onCall(event);

        if (event.isStreaming() && retMessage != null)
        {
            InputStream in;
            if (retMessage.getPayload() instanceof InputStream)
            {
                in = (InputStream)retMessage.getPayload();
            }
            else
            {
                in = new ByteArrayInputStream((byte[])objectToByteArray.transform(retMessage.getPayload()));
            }
            UMOStreamMessageAdapter sma = connector.getStreamMessageAdapter(in, null);
            retMessage = new MuleMessage(sma, retMessage);
        }

        if (logger.isDebugEnabled())
        {
            logger.debug("sent event on endpointUri: " + event.getEndpoint().getEndpointURI());
        }

        return retMessage;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.umo.provider.UMOMessageDispatcher#getConnector()
     */
    public UMOConnector getConnector()
    {
        return connector;
    }

    protected void doDispose()
    {
        // template method
    }

    protected void doConnect(UMOImmutableEndpoint endpoint) throws Exception
    {
        if (connector.isQueueEvents())
        {
            // use the default queue profile to configure this queue.
            // Todo We may want to allow users to specify this at the connector level
            MuleManager.getConfiguration().getQueueProfile().configureQueue(
                endpoint.getEndpointURI().getAddress());
        }
    }

    protected void doDisconnect() throws Exception
    {
        // template method
    }

}
