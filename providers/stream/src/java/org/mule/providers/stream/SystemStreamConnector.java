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

package org.mule.providers.stream;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;

import org.mule.umo.UMOComponent;
import org.mule.umo.UMOException;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.umo.provider.UMOConnector;
import org.mule.umo.provider.UMOMessageReceiver;

/**
 * <code>SystemStreamConnector</code> TODO
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */

public class SystemStreamConnector extends StreamConnector
{

    private String promptMessage;

    InputStream inputStream;

    PrintStream outputStream;

    private long messageDelayTime = 0;

    public SystemStreamConnector()
    {
        super();
    }

    public void doInitialise() throws InitialisationException
    {
        super.doInitialise();
        inputStream = System.in;
        outputStream = System.out;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.providers.stream.StreamConnector#getInputStream()
     */
    public InputStream getInputStream()
    {
        return inputStream;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.providers.AbstractConnector#doStart()
     */
    public synchronized void doStart()
    {
        if (receivers.size() > 0) {
            reinitialise();
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.providers.stream.StreamConnector#getOutputStream()
     */
    public OutputStream getOutputStream()
    {
        return outputStream;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.providers.stream.StreamConnector#reinitialise()
     */
    public void reinitialise()
    {
        DelayedMessageWriter writer = new DelayedMessageWriter(getMessageDelayTime());
        writer.start();
    }

    /**
     * @return Returns the promptMessage.
     */
    public String getPromptMessage()
    {
        return promptMessage;
    }

    /**
     * @param promptMessage The promptMessage to set.
     */
    public void setPromptMessage(String promptMessage)
    {
        this.promptMessage = promptMessage;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.umo.provider.UMOMessageDispatcher#getConnector()
     */
    public UMOConnector getConnector()
    {
        return this;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.umo.provider.UMOMessageDispatcher#getDelegateSession()
     */
    public Object getDelegateSession() throws UMOException
    {
        return null;
    }

    public UMOMessageReceiver registerListener(UMOComponent component, UMOEndpoint endpoint) throws Exception
    {
        if (receivers.size() > 0) {
            throw new UnsupportedOperationException("You can only register one listener per system stream connector");
        }
        UMOMessageReceiver receiver = super.registerListener(component, endpoint);
        reinitialise();
        return receiver;
    }

    private class DelayedMessageWriter extends Thread
    {
        private long delay = 0;

        public DelayedMessageWriter(long delay)
        {
            this.delay = delay;
        }

        public void run()
        {
            if (delay > 0) {
                try {
                    // Allow all other console message to be printed out first
                    sleep(delay);
                } catch (InterruptedException e1) {
                }
            }
            outputStream.println("\n" + promptMessage);
        }
    }

    /**
     * @return Returns the messageDelayTime.
     */
    public long getMessageDelayTime()
    {
        return messageDelayTime;
    }

    /**
     * @param messageDelayTime The messageDelayTime to set.
     */
    public void setMessageDelayTime(long messageDelayTime)
    {
        this.messageDelayTime = messageDelayTime;
    }
}
