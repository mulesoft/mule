/*
 * $Header$ $Revision$ $Date$
 * ------------------------------------------------------------------------------------------------------
 * 
 * Copyright (c) Cubis Limited. All rights reserved. http://www.cubis.co.uk
 * 
 * The software in this package is published under the terms of the BSD style
 * license a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 *  
 */

package org.mule.providers;

import EDU.oswego.cs.dl.util.concurrent.PooledExecutor;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.config.MuleProperties;
import org.mule.config.ThreadingProfile;
import org.mule.impl.RequestContext;
import org.mule.umo.UMOEvent;
import org.mule.umo.UMOException;
import org.mule.umo.UMOMessage;
import org.mule.umo.provider.UMOConnector;
import org.mule.umo.provider.UMOMessageDispatcher;

import java.beans.ExceptionListener;

/**
 * <p/>
 * <code>AbstractMessageDispatcher</code> TODO (document class)
 *
 * @author <a href="mailto:ross.mason@cubis.co.uk">Ross Mason</a>
 * @version $Revision$
 */
public abstract class AbstractMessageDispatcher implements UMOMessageDispatcher, ExceptionListener
{
    /**
     * logger used by this class
     */
    protected transient Log logger = LogFactory.getLog(getClass());

    /**
     * Thread pool of Connector sessions
     */
    protected PooledExecutor threadPool = null;

    protected boolean disposeOnCompletion = false;

    protected AbstractConnector connector;

    protected boolean disposed = false;

    protected boolean doThreading = true;

    public AbstractMessageDispatcher(AbstractConnector connector)
    {
        init(connector);
        disposeOnCompletion = ((AbstractConnector) connector).isDisposeDispatcherOnCompletion();
    }

    private void init(AbstractConnector connector)
    {
        this.connector = connector;
        if (connector instanceof AbstractConnector)
        {
            ThreadingProfile profile = ((AbstractConnector) connector).getDispatcherThreadingProfile();
            threadPool = profile.createPool(connector.getName());
            doThreading = profile.isDoThreading();
        }
    }

    /*
	 * (non-Javadoc)
	 * 
	 * @see org.mule.umo.provider.UMOMessageDispatcher#dispatch(org.mule.umo.UMOEvent)
	 */
    public final void dispatch(UMOEvent event) throws Exception
    {
        try
        {
            event.setSynchronous(false);
            event.setProperty(MuleProperties.MULE_ENDPOINT_PROPERTY, event.getEndpoint().getEndpointURI().toString());
            RequestContext.setEvent(event);
            if (doThreading && !event.isSynchronous())
            {
                    threadPool.execute(new Worker(event));
            } else
            {
                try
                {
                    doDispatch(event);
                } finally
                {
                    if (disposeOnCompletion)
                    {
                        dispose();
                    }
                }
            }
        } catch (Exception e)
        {
            //automatically dispose if there were failures
            logger.info("Exception occurred while executing on this dispatcher. disposing before continuing");
            dispose();
            throw e;
        }
    }


    public final UMOMessage send(UMOEvent event) throws Exception
    {
        try
        {
            try
            {
                event.setSynchronous(true);
                event.setProperty(MuleProperties.MULE_ENDPOINT_PROPERTY, event.getEndpoint().getEndpointURI().toString());
                RequestContext.setEvent(event);
                UMOMessage result = doSend(event);

                return result;
            } catch (Exception e)
            {
                //automatically dispose if there were failures
                logger.info("Exception occurred while executing on this dispatcher. disposing before continuing");
                dispose();
                throw e;
            }
        } finally
        {
            if (disposeOnCompletion)
            {
                dispose();
            }
        }
    }


    /*
	 * (non-Javadoc)
	 * 
	 * @see org.mule.util.ExceptionListener#onException(java.lang.Throwable)
	 */
    public void exceptionThrown(Exception e)
    {
        getConnector().handleException("Exception caught in ThreadPool: " + e.getMessage(), e);

    }

    private class Worker implements Runnable
    {
        private UMOEvent event;
        public Worker(UMOEvent event) {
            this.event = event;
        }
    /*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Runnable#run()
	 */
    public void run()
    {
        try
        {
            RequestContext.setEvent(event);
            doDispatch(event);
            
        } catch (Exception e)
        {
            try
            {
                dispose();
            } catch (UMOException e1)
            {
                //ignore
            }
            getConnector().handleException("Failed to intercept doDispatch: " + e.getMessage(), e);
        } finally
        {
            if (disposeOnCompletion)
            {
                try
                {
                    dispose();
                } catch (UMOException e)
                {
                    logger.error("Failed to dispose dispatcher: " + e, e);
                }
            }
        }

    }
    }

    public boolean isDisposed()
    {
        return disposed;
    }

    /**
     * Template method to destroy any resources.  some connector will want to cache
     * dispatchers and destroy them themselves
     *
     * @throws UMOException
     */
    public final void dispose() throws UMOException
    {
        if(!disposed) {
            try
            {
                doDispose();
            } finally
            {
               connector.getDispatchers().values().remove(this);
               disposed = true;
            }
        }

    }

    public UMOConnector getConnector()
    {
        return connector;
    }

    public abstract void doDispose() throws UMOException;

    public abstract void doDispatch(UMOEvent event) throws Exception;

    public abstract UMOMessage doSend(UMOEvent event) throws Exception;
}
