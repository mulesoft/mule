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
 */
package org.mule.providers.stream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.providers.AbstractMessageDispatcher;
import org.mule.umo.UMOEvent;
import org.mule.umo.UMOException;
import org.mule.umo.UMOMessage;
import org.mule.umo.endpoint.UMOEndpointURI;
import org.mule.umo.provider.UMOConnector;

/**
 * <code>StreamMessageDispatcher</code> TODO
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */

public class StreamMessageDispatcher extends AbstractMessageDispatcher
{
    /**
     * logger used by this class
     */
    private static transient Log logger = LogFactory.getLog(StreamMessageDispatcher.class);

    private StreamConnector connector;

    public StreamMessageDispatcher(StreamConnector connector)
    {
        super(connector);
        this.connector = connector;
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

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.umo.provider.UMOConnector#dispatch(org.mule.umo.UMOEvent)
     */
    public void doDispatch(UMOEvent event) throws Exception
    {
        Object data = event.getTransformedMessage();
        if(data instanceof byte[]) {
            connector.getOutputStream().write((byte[])data);
        } else {
            connector.getOutputStream().write(data.toString().getBytes());
        }
        connector.getOutputStream().flush();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.umo.provider.UMOConnector#send(org.mule.umo.UMOEvent)
     */
    public UMOMessage doSend(UMOEvent event) throws Exception
    {
        doDispatch(event);
        return event.getMessage();
    }

    public UMOMessage receive(UMOEndpointURI endpointUri, long timeout) throws Exception
    {
        throw new UnsupportedOperationException("Receive not implemented on the Stream connector");
    }

    public UMOConnector getConnector()
    {
        return connector;
    }

    public void doDispose()
    {
    }
}
