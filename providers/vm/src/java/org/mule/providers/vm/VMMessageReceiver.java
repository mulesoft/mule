/* 
 * $Id$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 *
 */
package org.mule.providers.vm;

import org.mule.MuleException;
import org.mule.config.i18n.Message;
import org.mule.config.i18n.Messages;
import org.mule.impl.MuleMessage;
import org.mule.providers.TransactedPollingMessageReceiver;
import org.mule.umo.UMOComponent;
import org.mule.umo.UMOEvent;
import org.mule.umo.UMOException;
import org.mule.umo.UMOMessage;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.umo.provider.UMOConnector;
import org.mule.util.queue.Queue;
import org.mule.util.queue.QueueSession;

import java.util.List;

/**
 * <code>VMMessageReceiver</code> is a listener of events from a mule
 * component which then simply <p/> passes the events on to the target
 * component.
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @author <a href="mailto:gnt@codehaus.org">Guillaume Nodet</a>
 * @version $Revision$
 */
public class VMMessageReceiver extends TransactedPollingMessageReceiver
{
    private VMConnector connector;
    private Object lock = new Object();

    public VMMessageReceiver(UMOConnector connector, UMOComponent component, UMOEndpoint endpoint)
            throws InitialisationException
    {
        super(connector, component, endpoint, new Long(10));
        this.connector = (VMConnector) connector;
        receiveMessagesInTransaction = endpoint.getTransactionConfig().isTransacted();
    }

    public void doConnect() throws Exception
    {
        if (connector.isQueueEvents()) {
            // Ensure we can create a vm queue
            QueueSession queueSession = connector.getQueueSession();
            Queue q = queueSession.getQueue(endpoint.getEndpointURI().getAddress());
            if(logger.isDebugEnabled()) {
                logger.debug("Current queue depth for queue: " + endpoint.getEndpointURI().getAddress() + " is: " + q.size());
            }
        }
    }

    public void doDisconnect() throws Exception
    {
        // template method
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.umo.UMOEventListener#onEvent(org.mule.umo.UMOEvent)
     */
    public void onEvent(UMOEvent event) throws UMOException
    {
        if (connector.isQueueEvents()) {
            QueueSession queueSession = connector.getQueueSession();
            Queue queue = queueSession.getQueue(endpoint.getEndpointURI().getAddress());
            try {
                queue.put(event);
            } catch (InterruptedException e) {
                throw new MuleException(new Message(Messages.INTERRUPTED_QUEUING_EVENT_FOR_X,
                                                    this.endpoint.getEndpointURI()), e);
            }
        } else {
            UMOMessage msg = new MuleMessage(event.getTransformedMessage(), event.getMessage());
            synchronized(lock) {
                routeMessage(msg);
            }
        }
    }


    public Object onCall(UMOEvent event) throws UMOException
    {
        return routeMessage(new MuleMessage(event.getTransformedMessage(), event.getMessage()), event.isSynchronous());
    }

    protected List getMessages() throws Exception
    {
        QueueSession qs = connector.getQueueSession();
        Queue queue = qs.getQueue(endpoint.getEndpointURI().getAddress());
        UMOEvent event = (UMOEvent) queue.take();
        if(event!=null) {
            routeMessage(new MuleMessage(event.getTransformedMessage(), event.getMessage()));
        }
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.providers.TransactionEnabledPollingMessageReceiver#processMessage(java.lang.Object)
     */
    protected void processMessage(Object msg) throws Exception
    {
        // This method is never called as the message is processed when received
    }

}
