/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport;

import org.mule.api.context.WorkManager;
import org.mule.api.transport.Connectable;
import org.mule.api.transport.ConnectionStrategy;
import org.mule.api.transport.Connector;
import org.mule.api.transport.MessageReceiver;
import org.mule.config.i18n.MessageFactory;

import javax.resource.spi.work.Work;
import javax.resource.spi.work.WorkException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * TODO document
 */
public abstract class AbstractConnectionStrategy implements ConnectionStrategy
{
    /**
     * logger used by this class
     */
    protected transient Log logger = LogFactory.getLog(getClass());

    private volatile boolean doThreading = false;

    private WorkManager workManager;

    private final Object reconnectLock = new Object();

    public final void connect(final Connectable connectable) throws FatalConnectException
    {
        if (doThreading)
        {
            try
            {
                WorkManager wm = getWorkManager();
                if (wm == null)
                {
                    throw new FatalConnectException(MessageFactory.createStaticMessage("No WorkManager is available"), connectable);
                }
                
                wm.scheduleWork(new Work()
                {
                    public void release()
                    {
                        // nothing to do
                    }

                    public void run()
                    {
                        try
                        {
                            synchronized (reconnectLock)
                            {
                                doConnect(connectable);
                            }
                        }
                        catch (FatalConnectException e)
                        {
                            synchronized (reconnectLock)
                            {
                                resetState();
                            }
                            // TODO should really extract an interface for
                            // classes capable of handling an exception
                            if (connectable instanceof Connector)
                            {
                                ((Connector) connectable).handleException(e);
                            }
                            // TODO: this cast is evil
                            else if (connectable instanceof AbstractMessageReceiver)
                            {
                                ((AbstractMessageReceiver) connectable).handleException(e);
                            }
                            // if it's none of the above, it's not handled and Mule just sits doing nothing
                        }
                    }
                });
            }
            catch (WorkException e)
            {
                synchronized (reconnectLock)
                {
                    resetState();
                }
                throw new FatalConnectException(e, connectable);
            }
        }
        else
        {
            try
            {
                synchronized (reconnectLock)
                {
                    doConnect(connectable);
                }
            }
            finally
            {
                synchronized (reconnectLock)
                {
                    resetState();
                }
            }
        }
    }

    public boolean isDoThreading()
    {
        return doThreading;
    }

    public void setDoThreading(boolean doThreading)
    {
        this.doThreading = doThreading;
    }


    public WorkManager getWorkManager()
    {
        return workManager;
    }

    public void setWorkManager(WorkManager workManager)
    {
        this.workManager = workManager;
    }

    protected abstract void doConnect(Connectable connectable) throws FatalConnectException;

    /**
     * Resets any state stored in the retry strategy
     */
    public abstract void resetState();

    protected String getDescription(Connectable connectable)
    {
        if (connectable instanceof MessageReceiver)
        {
            return ((MessageReceiver) connectable).getEndpointURI().toString();
        }
        else
        {
            return connectable.toString();
        }
    }

}
