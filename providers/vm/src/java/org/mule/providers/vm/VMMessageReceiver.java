/* 

 * $Header$

 * $Revision$

 * $Date$

 * ------------------------------------------------------------------------------------------------------

 * 

 * Copyright (c) Cubis Limited. All rights reserved.

 * http://www.cubis.co.uk 

 * 

 * The software in this package is published under the terms of the BSD

 * style license a copy of which has been included with this distribution in

 * the LICENSE.txt file. 

 *

 */

package org.mule.providers.vm;


import org.mule.InitialisationException;
import org.mule.MuleException;
import org.mule.impl.MuleMessage;
import org.mule.providers.AbstractMessageReceiver;
import org.mule.umo.UMOComponent;
import org.mule.umo.UMOEvent;
import org.mule.umo.UMOException;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.provider.UMOConnector;
import org.mule.umo.provider.UMOMessageAdapter;
import org.mule.util.queue.BoundedPersistentQueue;


/**
 * <code>VMMessageReceiver</code> is a listener of events from a mule component which then simply
 * <p/>
 * passes the events on to the target component.
 *
 * @author <a href="mailto:ross.mason@cubis.co.uk">Ross Mason</a>
 * @version $Revision$
 */

public class VMMessageReceiver extends AbstractMessageReceiver implements Runnable
{
    private BoundedPersistentQueue queue;
    private Thread worker;

    public VMMessageReceiver(UMOConnector connector, UMOComponent component, UMOEndpoint endpoint, BoundedPersistentQueue queue) throws InitialisationException
    {
        create(connector, component, endpoint);
        this.queue = queue;
        if(queue!=null) {
            worker = new Thread(this);
            worker.start();
        }
    }



    /* (non-Javadoc)
     * @see org.mule.umo.UMOEventListener#onEvent(org.mule.umo.UMOEvent)
     */
    public void onEvent(UMOEvent event) throws UMOException
    {
        if(queue!=null) {
            try
            {
                queue.put(event);
            } catch (InterruptedException e)
            {
                throw new MuleException("Failed to queue event: " + event.toString(), e);
            }
        } else {
            UMOMessageAdapter adapter = connector.getMessageAdapter(new MuleMessage(event.getTransformedMessage(), event.getProperties()));
            routeMessage(new MuleMessage(adapter), event.isSynchronous());
        }
    }

    /* (non-Javadoc)
     * @see org.mule.umo.UMOSyncChainSupport#onCall(org.mule.umo.UMOEvent)
     */
    public Object onCall(UMOEvent event) throws UMOException
    {
        UMOMessageAdapter adapter = connector.getMessageAdapter(new MuleMessage(event.getTransformedMessage(), event.getProperties()));
        return routeMessage(new MuleMessage(adapter), event.isSynchronous());
    }

    public void run()
    {
        while(!disposing.get())
        {
            if(connector.isStarted())
            {
                UMOEvent event = null;
                try
                {
                    try
                    {
                        event = (UMOEvent)queue.take();
                        UMOMessageAdapter adapter = connector.getMessageAdapter(new MuleMessage(event.getTransformedMessage(), event.getProperties()));
                        routeMessage(new MuleMessage(adapter), event.isSynchronous());
                    } catch (InterruptedException e)
                    {
                        //ignore
                    }
                } catch (Exception e)
                {
                    logger.error("Failed to dispatch event from VM receiver: " + e.getMessage(), e);
                    connector.getExceptionStrategy().handleException("Failed to dispatch event from VM receiver: " + e.getMessage(), e);
                } finally {
                    if(event!=null) {
                        queue.remove(event);
                    }
                }
            }
        }
    }

    BoundedPersistentQueue getQueue() {
        return queue;
    }

    public void doDispose() throws UMOException
    {
        if(worker!=null) worker.interrupt();
        worker=null;
    }
}