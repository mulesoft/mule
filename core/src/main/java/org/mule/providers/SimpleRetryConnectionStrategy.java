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

import org.mule.config.ExceptionHelper;
import org.mule.config.i18n.Message;
import org.mule.config.i18n.Messages;
import org.mule.umo.provider.UMOConnectable;

import edu.emory.mathcs.backport.java.util.concurrent.atomic.AtomicInteger;

/**
 * A simple connection retry strategy where the a connection will be attempted X
 * number of retryCount every Y milliseconds. The <i>retryCount</i> and <i>frequency</i>
 * properties can be set to customise the behaviour.
 */

public class SimpleRetryConnectionStrategy extends AbstractConnectionStrategy
{

    protected static class RetryCounter extends ThreadLocal
    {
        public int countRetry()
        {
            return ((AtomicInteger)get()).incrementAndGet();
        }
        
        public void reset()
        {
            ((AtomicInteger)get()).set(0);
        }

        // //@Override
        protected Object initialValue()
        {
            return new AtomicInteger(0);
        }
    }

    protected static final RetryCounter retryCounter = new RetryCounter();

    private volatile int retryCount = 2;
    private volatile long frequency = 2000;

    protected void doConnect(UMOConnectable connectable) throws FatalConnectException
    {
        while (true)
        {
            int currentCount = retryCounter.countRetry();

            try
            {
                connectable.connect();
                if (logger.isDebugEnabled())
                {
                    logger.debug("Successfully connected to " + getDescription(connectable));
                }
                break;
            }
            catch (InterruptedException ie)
            {
                // If we were interrupted it's probably because the server is
                // shutting down
                throw new FatalConnectException(new Message(Messages.RECONNECT_STRATEGY_X_FAILED_ENDPOINT_X,
                    getClass().getName(), getDescription(connectable)), ie, connectable);
            }
            catch (Exception e)
            {
                if (currentCount == retryCount)
                {
                    throw new FatalConnectException(new Message(
                        Messages.RECONNECT_STRATEGY_X_FAILED_ENDPOINT_X, getClass().getName(),
                        getDescription(connectable)), e, connectable);
                }

                if (logger.isErrorEnabled())
                {
                    StringBuffer msg = new StringBuffer(512);
                    msg.append("Failed to connect/reconnect on endpoint: ").append(
                        getDescription(connectable));
                    Throwable t = ExceptionHelper.getRootException(e);
                    msg.append(". Root Exception was: ").append(ExceptionHelper.writeException(t));
                    logger.error(msg.toString(), e);
                }

                if (logger.isInfoEnabled())
                {
                    logger.info("Waiting for " + frequency + "ms before reconnecting. Failed attempt "
                                + currentCount + " of " + retryCount);
                }

                try
                {
                    Thread.sleep(frequency);
                }
                catch (InterruptedException e1)
                {
                    throw new FatalConnectException(new Message(
                        Messages.RECONNECT_STRATEGY_X_FAILED_ENDPOINT_X, getClass().getName(),
                        getDescription(connectable)), e, connectable);
                }
            }
        }
    }

    /**
     * Resets any state stored in the retry strategy
     */
    public synchronized void resetState()
    {
        retryCounter.reset();
    }

    public int getRetryCount()
    {
        return retryCount;
    }

    public void setRetryCount(int retryCount)
    {
        this.retryCount = retryCount;
    }

    public long getFrequency()
    {
        return frequency;
    }

    public void setFrequency(long frequency)
    {
        this.frequency = frequency;
    }
}
