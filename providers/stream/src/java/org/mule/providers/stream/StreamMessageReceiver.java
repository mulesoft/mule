/*
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 * 
 * Copyright (c) SymphonySoft Limited. All rights reserved. http://www.symphonysoft.com
 * 
 * The software in this package is published under the terms of the BSD style
 * license a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 *  
 */
package org.mule.providers.stream;

import org.mule.impl.MuleMessage;
import org.mule.providers.PollingMessageReceiver;
import org.mule.umo.UMOComponent;
import org.mule.umo.UMOMessage;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.umo.provider.UMOConnector;
import org.mule.util.Utility;

import java.io.InputStream;

/**
 * <code>StreamMessageReceiver</code> is a listener of events from a mule
 * components which then simply passes the events on to the target components.
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class StreamMessageReceiver extends PollingMessageReceiver
{
    public static final int DEFAULT_BUFFER_SIZE = 1024 * 8;

    private int bufferSize = DEFAULT_BUFFER_SIZE;
    private InputStream inputStream;
    private StreamConnector connector;

    public StreamMessageReceiver(UMOConnector connector,
                                 UMOComponent component,
                                 UMOEndpoint endpoint,
                                 InputStream stream,
                                 Long checkFrequency) throws InitialisationException
    {

        super(connector, component, endpoint, checkFrequency);
        this.connector = (StreamConnector)connector;
        inputStream = stream;
        if(connector instanceof SystemStreamConnector) {
            String promptMessage = (String)endpoint.getProperties().get("promptMessage");
            String messageDelayTime = (String)endpoint.getProperties().get("messageDelayTime");
            if(promptMessage!=null) {
                ((SystemStreamConnector)connector).setPromptMessage(promptMessage);
            }
            if(messageDelayTime!=null) {
                ((SystemStreamConnector)connector).setMessageDelayTime(new Long(messageDelayTime).longValue());
            }
        }
    }

    public void doConnect() throws Exception
    {
        // noop
    }

    public void doDisconnect() throws Exception
    {
        // noop
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.util.timer.TimeEventListener#timeExpired(org.mule.util.timer.TimeEvent)
     */
    public void poll()
    {
        try {
            // anything to read?
            if (inputStream.available() == 0) {
                return;
            }

            byte[] buf = new byte[getBufferSize()];
            int len = inputStream.read(buf);
            if (len == -1) {
                return;
            }

            String message = new String(buf, 0, len);

            //remove any trailing CR/LF
            if (message.endsWith(Utility.CRLF)) {
            	message = message.substring(0, message.length() - Utility.CRLF.length());
            }

            UMOMessage umoMessage = new MuleMessage(connector.getMessageAdapter(message));
            routeMessage(umoMessage, endpoint.isSynchronous());

            ((StreamConnector) endpoint.getConnector()).reinitialise();
        } catch (Exception e) {
            handleException(e);
        }
    }

    public InputStream getInputStream()
    {
        return inputStream;
    }

    public void setInputStream(InputStream inputStream)
    {
        this.inputStream = inputStream;
    }

    public int getBufferSize()
    {
        return bufferSize;
    }

    public void setBufferSize(int bufferSize)
    {
        this.bufferSize = bufferSize;
    }
}
