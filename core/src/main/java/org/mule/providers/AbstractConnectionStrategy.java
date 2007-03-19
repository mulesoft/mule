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

import org.mule.umo.manager.UMOWorkManager;
import org.mule.umo.provider.UMOConnectable;
import org.mule.umo.provider.UMOMessageReceiver;
import org.mule.umo.provider.UMOConnector;

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

    private UMOWorkManager workManager;

    private final Object reconnectLock = new Object();

    public final void connect(final UMOConnectable connectable) throws FatalConnectException
    {
        if (doThreading)
        {
            try
            {
                getWorkManager().scheduleWork(new Work()
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
                            if (connectable instanceof UMOConnector) {
                                ((UMOConnector) connectable).handleException(e);
                            }
                            // TODO: this cast is evil
                            else if (connectable instanceof AbstractMessageReceiver)
                            {
                                ((AbstractMessageReceiver)connectable).handleException(e);
                            }
                            // TODO MULE-863: And if it's not?
                            // AP if it's not, it's not handled and Mule just sits doing nothing
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


    public UMOWorkManager getWorkManager()
    {
        return workManager;
    }

    public void setWorkManager(UMOWorkManager workManager)
    {
        this.workManager = workManager;
    }

    protected abstract void doConnect(UMOConnectable connectable) throws FatalConnectException;

    /**
     * Resets any state stored in the retry strategy
     */
    public abstract void resetState();

    protected String getDescription(UMOConnectable connectable)
    {
        if (connectable instanceof UMOMessageReceiver)
        {
            return ((UMOMessageReceiver)connectable).getEndpointURI().toString();
        }
        else
        {
            return connectable.toString();
        }
    }

}
