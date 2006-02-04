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
package org.mule.providers;

import org.mule.config.i18n.Message;
import org.mule.config.i18n.Messages;
import org.mule.config.ExceptionHelper;
import org.mule.umo.provider.UMOConnectable;

/**
 * A simple connection retry strategy where the a connection will be attempted X
 * number of retryCount every Y milliseconds. The <i>retryCount</i> and
 * <i>frequency</i> properties can be set to customise the behaviour.
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */

public class SimpleRetryConnectionStrategy extends AbstractConnectionStrategy
{

    private int retryCount = 2;
    private long frequency = 2000;
    private int count = 0;

    public void doConnect(UMOConnectable connectable) throws FatalConnectException
    {
        while (true) {
            try {
                count++;
                connectable.connect();
                // reset counter
                count = 0;
                if (logger.isDebugEnabled()) {
                	logger.debug("Successfully connected to " + getDescription(connectable));
                }
                break;
            } catch (Exception e) {
                if (count == retryCount) {
                    throw new FatalConnectException(new Message(Messages.RECONNECT_STRATEGY_X_FAILED_ENDPOINT_X,
                                                                getClass().getName(),
                                                                getDescription(connectable)), e, connectable);
                }
                if(logger.isErrorEnabled()) {
                    StringBuffer msg = new StringBuffer();
                    msg.append("Failed to connect/reconnect on endpoint: ").append(getDescription(connectable));
                    Throwable t = ExceptionHelper.getRootException(e);
                    msg.append(". Root Exception was: ").append(ExceptionHelper.writeException(t));
                    logger.error(msg.toString(), e);
                }
                if (logger.isDebugEnabled()) {
                	logger.debug("Waiting for " + frequency + "ms before reconnecting");
                }
                try {
                    Thread.sleep(frequency);
                } catch (InterruptedException e1) {
                    throw new FatalConnectException(new Message(Messages.RECONNECT_STRATEGY_X_FAILED_ENDPOINT_X,
                            getClass().getName(),
                            getDescription(connectable)), e, connectable);
                }
            }
        }
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
