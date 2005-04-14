/* 

 * $Header$

 * $Revision$

 * $Date$

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


import EDU.oswego.cs.dl.util.concurrent.ConcurrentHashMap;
import org.mule.MuleManager;
import org.mule.config.QueueProfile;
import org.mule.config.i18n.Message;
import org.mule.config.i18n.Messages;
import org.mule.impl.MuleMessage;
import org.mule.impl.endpoint.MuleEndpointURI;
import org.mule.providers.AbstractServiceEnabledConnector;
import org.mule.routing.filters.WildcardFilter;
import org.mule.umo.MessagingException;
import org.mule.umo.UMOComponent;
import org.mule.umo.endpoint.EndpointException;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.endpoint.UMOEndpointURI;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.umo.provider.MessageTypeNotSupportedException;
import org.mule.umo.provider.UMOMessageAdapter;
import org.mule.umo.provider.UMOMessageReceiver;
import org.mule.util.ClassHelper;
import org.mule.util.queue.BoundedPersistentQueue;

import java.util.Iterator;


/**
 * <code>VMConnector</code> A simple endpoint wrapper to allow a Mule component to
 * <p/>
 * be accessed from an endpoint
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */

public class VMConnector extends AbstractServiceEnabledConnector
{
    private boolean queueEvents = false;
    private int maxQueues = 16;
    private ConcurrentHashMap queues = null;
    private QueueProfile queueProfile;
    private Class adapterClass = null;


    /* (non-Javadoc)
     * @see org.mule.providers.AbstractConnector#create()
     */
    public void doInitialise() throws InitialisationException
    {
        super.doInitialise();
        if(queueEvents) {
            queues = new ConcurrentHashMap(maxQueues);
            if(queueProfile== null) {
                queueProfile = MuleManager.getConfiguration().getQueueProfile();
            }
        }

        try
        {
            adapterClass = ClassHelper.loadClass(serviceDescriptor.getMessageAdapter(), getClass());
        } catch (ClassNotFoundException e)
        {
            throw new InitialisationException(new Message(Messages.FAILED_LOAD_X, "Message Adapter: " +
                    serviceDescriptor.getMessageAdapter()), e);
        }
    }

    /* (non-Javadoc)
     * @see org.mule.umo.provider.UMOConnector#registerListener(org.mule.umo.UMOSession, org.mule.umo.endpoint.UMOEndpoint)
     */
    public UMOMessageReceiver createReceiver(UMOComponent component, UMOEndpoint endpoint) throws Exception
    {
        BoundedPersistentQueue queue = null;
        if(queueEvents) {
            queue = queueProfile.createQueue(endpoint.getEndpointURI().getAddress());
            queues.put(endpoint.getEndpointURI().getAddress(), queue);
        }
        return serviceDescriptor.createMessageReceiver(this, component, endpoint, new Object[]{queue});
    }

    /* (non-Javadoc)
     * @see org.mule.umo.provider.UMOConnector#getMessageAdapter(java.lang.Object)
     */
    public UMOMessageAdapter getMessageAdapter(Object message) throws MessagingException
    {
        if(message==null) {
            throw new MessageTypeNotSupportedException(null, adapterClass);

        }else if(message instanceof MuleMessage) {
            return ((MuleMessage)message).getAdapter();
        } else if(message instanceof UMOMessageAdapter) {
            return (UMOMessageAdapter)message;
        } else {
            throw new MessageTypeNotSupportedException(message, adapterClass);
        }
    }

    /* (non-Javadoc)
     * @see org.mule.umo.provider.UMOConnector#getProtocol()
     */
    public String getProtocol()
    {
        return "VM";
    }

    /* (non-Javadoc)
     * @see org.mule.providers.AbstractConnector#disposeConnector()
     */
    protected void disposeConnector()
    {
        if(queues!=null) {
            BoundedPersistentQueue queue;
            for (Iterator iterator = queues.values().iterator(); iterator.hasNext();)
            {
                queue = (BoundedPersistentQueue)iterator.next();
                queue.dispose();
            }
            queues.clear();
        }
    }

    public boolean isQueueEvents()
    {
        return queueEvents;
    }

    public void setQueueEvents(boolean queueEvents)
    {
        this.queueEvents = queueEvents;
    }

    public QueueProfile getQueueProfile()
    {
        return queueProfile;
    }

    public void setQueueProfile(QueueProfile queueProfile)
    {
        this.queueProfile = queueProfile;
    }

    public void setMaxQueues(int maxQueues)
    {
        this.maxQueues = maxQueues;
    }

    BoundedPersistentQueue getQueue(String endpoint)
    {
        return (BoundedPersistentQueue)queues.get(endpoint);
    }

    VMMessageReceiver getReceiver(UMOEndpointURI endpointUri) throws EndpointException
    {
        return (VMMessageReceiver)getRecieverByEndpoint(endpointUri);
    }

    BoundedPersistentQueue createQueue(String endpoint) throws InitialisationException
    {
        BoundedPersistentQueue queue = (BoundedPersistentQueue)queues.get(endpoint);
        if(queue!=null)  {
            return queue;
        } else {
            queue = queueProfile.createQueue(endpoint);
            queues.put(endpoint, queue);
            return queue;
        }
    }

    protected UMOMessageReceiver getRecieverByEndpoint(UMOEndpointURI endpointUri) throws EndpointException
    {
        if(logger.isDebugEnabled()) logger.debug("Lookng up vm reciever for address: " + endpointUri.toString());
        UMOMessageReceiver receiver;
        //If we have an exact match, use it
        receiver = (UMOMessageReceiver)receivers.get(endpointUri.getAddress());
        if(receiver != null) {
            logger.debug("Found exact receiver match on endpointUri: " + endpointUri);
            return receiver;
        }

        //otherwise check each one against a wildcard match
        for (Iterator iterator = receivers.values().iterator(); iterator.hasNext();)
        {
            receiver = (UMOMessageReceiver)iterator.next();
            String filterAddress = receiver.getEndpointURI().getAddress();
            WildcardFilter filter = new WildcardFilter(filterAddress);
            if(filter.accept(endpointUri.getAddress()))
            {
                receiver.getEndpoint().setEndpointURI(new MuleEndpointURI(endpointUri, filterAddress));

                logger.debug("Found receiver match on endpointUri: " + receiver.getEndpointURI() + " against " + endpointUri);
                return receiver;
            }
        }
        logger.debug("No receiver found for endpointUri: " + endpointUri);
        return null;
    }
}