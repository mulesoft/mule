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

package org.mule.providers;

import org.mule.umo.lifecycle.InitialisationException;
import org.mule.umo.UMOComponent;
import org.mule.umo.UMOException;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.provider.UMOConnector;

/**
 * <p><code>PollingMessageReceiver</code> implements a polling message receiver.
 * The receiver provides a poll method that implementations should implement to
 * execute their custom code.  Note that the receiver will not poll if the associated
 * connector is not started.
 *
 * @author <a href="mailto:ross.mason@cubis.co.uk">Ross Mason</a>
 * @author Guillaume Nodet
 * @version $Revision$
 */
public abstract class PollingMessageReceiver extends AbstractMessageReceiver implements Runnable
{
    public static final long DEFAULT_POLL_FREQUENCY = 1000;
    public static final long STARTUP_DELAY = 1000;

    private long frequency = DEFAULT_POLL_FREQUENCY;
    private Thread thread;

    public PollingMessageReceiver() {
    }
    
    public PollingMessageReceiver(UMOConnector connector,
                                  UMOComponent component,
                                  final UMOEndpoint endpoint, Long frequency) throws InitialisationException
    {
        create(connector, component, endpoint);
        this.frequency = frequency.longValue();
        thread = new Thread(this, getClass().getName());
        thread.start();
    }
    
    public void run() {
    	try {
    		Thread.sleep(STARTUP_DELAY);
	    	while (!connector.isDisposed() && !disposing.get()) {
	            if (connector.isStarted()) {
    	            poll();
	            }
	            Thread.sleep(frequency);
	    	}
    	} catch (Exception e) {
    		// Only handle exception if it was not due to the connector stopping
    		if (!disposing.get()) {
	   			logger.error("An error occurred when polling", e);
	            try {
	                connector.stop();
	            } catch (Exception e2) {
	                logger.error("Failed to stop endpoint: " + e2.getMessage(), e2);
	            }
    		}
    	}
    }

    public void setFrequency(long l)
    {
        if (l <= 0)
        {
            frequency = DEFAULT_POLL_FREQUENCY;
        }
        else
        {
            frequency = l;
        }
    }

    public long getFrequency()
    {
        return frequency;
    }

    protected void doDispose() throws UMOException
    {
    	if (thread != null) {
    		thread.interrupt();
    	}
    }

    public abstract void poll() throws Exception;
}