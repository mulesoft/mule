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
                            synchronized (this)
                            {
                                doConnect(connectable);
                            }
                        }
                        catch (FatalConnectException e)
                        {
                            synchronized (this)
                            {
                                resetState();
                            }
                            // TODO: this cast is evil
                            if (connectable instanceof AbstractMessageReceiver)
                            {
                                ((AbstractMessageReceiver)connectable).handleException(e);
                            }
                        }
                    }
                });
            }
            catch (WorkException e)
            {
                synchronized (this)
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
                synchronized (this)
                {
                    doConnect(connectable);
                }
            }
            finally
            {
                synchronized (this)
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
