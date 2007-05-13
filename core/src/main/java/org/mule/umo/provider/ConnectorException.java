/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.umo.provider;

import org.mule.config.i18n.CoreMessages;
import org.mule.config.i18n.Message;
import org.mule.umo.UMOException;

/**
 * <code>ConnectorException</code> Is thrown in the context of a UMOConnector,
 * usually some sort of transport level error where the connection has failed. This
 * exception maintains a reference to the connector.
 * 
 * @see UMOConnector
 */
public class ConnectorException extends UMOException
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = 4729481487016346035L;

    /**
     * The connector relevant to this exception
     */
    private transient UMOConnector connector;

    /**
     * @param message the exception message
     * @param connector where the exception occurred or is being thrown
     */
    public ConnectorException(Message message, UMOConnector connector)
    {
        super(generateMessage(message, connector));
        this.connector = connector;
    }

    /**
     * @param message the exception message
     * @param connector where the exception occurred or is being thrown
     * @param cause the exception that cause this exception to be thrown
     */
    public ConnectorException(Message message, UMOConnector connector, Throwable cause)
    {
        super(generateMessage(message, connector), cause);
        this.connector = connector;
    }

    private static Message generateMessage(Message message, UMOConnector connector)
    {
        Message m = CoreMessages.connectorCausedError(connector);
        if (message != null)
        {
            message.setNextMessage(m);
        }
        return message;
    }

    public UMOConnector getConnector()
    {
        return connector;
    }
}
