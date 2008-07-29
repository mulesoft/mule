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

import org.mule.api.MuleContext;
import org.mule.api.context.MuleContextAware;
import org.mule.api.context.notification.MuleContextNotificationListener;
import org.mule.api.context.notification.ServerNotification;
import org.mule.api.transport.Connectable;
import org.mule.api.transport.ConnectionStrategy;
import org.mule.api.transport.Connector;
import org.mule.api.transport.MessageReceiver;
import org.mule.config.i18n.MessageFactory;
import org.mule.context.notification.MuleContextNotification;
import org.mule.context.notification.NotificationException;
import org.mule.util.ClassUtils;

import javax.resource.spi.work.Work;
import javax.resource.spi.work.WorkException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public abstract class AbstractConnectionStrategy implements ConnectionStrategy, MuleContextAware
{
    protected transient Log logger = LogFactory.getLog(getClass());

    protected final Object reconnectLock = new Object();

    protected ThreadLocal isInitialThread = new ThreadLocal()
    {
      protected synchronized Object initialValue()
      {
          return new Boolean(true);
      }
    };
    
    private MuleContext muleContext;

    private volatile boolean doThreading = false;
    
    private volatile boolean isConnecting = false;    

    public final void connect(Connectable connectable) throws FatalConnectException
    {
        if (doThreading && !isConnecting)
        {
            connectAfterServerStartup(connectable);
        }
        else if (!isInitialThread() || !isConnecting)
        {
            connectImmediately(connectable);
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

    public boolean isConnecting()
    {
        return isConnecting;
    }

    public boolean isInitialThread()
    {
        return ((Boolean) isInitialThread.get()).booleanValue();
    }

    public MuleContext getMuleContext()
    {
        return muleContext;
    }

    public void setMuleContext(MuleContext muleContext)
    {
        this.muleContext = muleContext;
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

    public int hashCode()
    {
        if (muleContext != null)
        {
            return ClassUtils.hash(new Object[]{doThreading ? Boolean.TRUE : Boolean.FALSE, muleContext.getWorkManager()});
        }
        else
        {
            return ClassUtils.hash(new Object[]{doThreading ? Boolean.TRUE : Boolean.FALSE});
        }
    }

    public boolean equals(Object obj)
    {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        final AbstractConnectionStrategy other = (AbstractConnectionStrategy) obj;
        return ClassUtils.equal(new Boolean(doThreading), new Boolean(other.doThreading))
               && ClassUtils.equal(muleContext.getWorkManager(), other.muleContext.getWorkManager());

    }

    protected void connectAfterServerStartup(final Connectable connectable) throws FatalConnectException
    {
        isConnecting = true;
        doThreading = false;
        
        try
        {
            muleContext.registerListener(new MuleContextNotificationListener()
            {
                public void onNotification(ServerNotification notification)
                {
                    if (notification.getAction() == MuleContextNotification.CONTEXT_STARTED)
                    {
                        try
                        {
                            if (muleContext.getWorkManager() == null)
                            {
                                logger.error(MessageFactory.createStaticMessage("No WorkManager is available."));
                            }
                            else
                            {
                                muleContext.getWorkManager().scheduleWork(new Work()
                                {
                                    public void release()
                                    {
                                        // ignore
                                    }
        
                                    public void run()
                                    {
                                        isInitialThread.set(new Boolean(false));
        
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
                                        finally
                                        {
                                            isConnecting = false;
                                        }
                                    }
                                });
                            }
                        }
                        catch (WorkException e)
                        {
                            synchronized (reconnectLock)
                            {
                                resetState();
                            }
                        }
                    }
                }
            });
        }
        catch (NotificationException e)
        {
            synchronized (reconnectLock)
            {
                resetState();
            }
            throw new FatalConnectException(e, connectable);
        }
    }
    
    protected void connectImmediately(Connectable connectable) throws FatalConnectException
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
