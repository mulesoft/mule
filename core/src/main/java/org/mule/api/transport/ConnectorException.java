/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.api.transport;

import org.mule.api.MuleException;
import org.mule.config.i18n.CoreMessages;
import org.mule.config.i18n.Message;

/**
 * <code>ConnectorException</code> Is thrown in the context of a Connector,
 * usually some sort of transport level error where the connection has failed. This
 * exception maintains a reference to the connector.
 * 
 * @see Connector
 */
public class ConnectorException extends MuleException
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = 4729481487016346035L;

    /**
     * The connector relevant to this exception
     */
    private transient Connector connector;

    /**
     * @param message the exception message
     * @param connector where the exception occurred or is being thrown
     */
    public ConnectorException(Message message, Connector connector)
    {
        super(generateMessage(message, connector));
        this.connector = connector;
    }

    /**
     * @param message the exception message
     * @param connector where the exception occurred or is being thrown
     * @param cause the exception that cause this exception to be thrown
     */
    public ConnectorException(Message message, Connector connector, Throwable cause)
    {
        super(generateMessage(message, connector), cause);
        this.connector = connector;
    }

    private static Message generateMessage(Message message, Connector connector)
    {
        Message m = CoreMessages.connectorCausedError(connector);
        if (message != null)
        {
            message.setNextMessage(m);
        }
        return message;
    }

    public Connector getConnector()
    {
        return connector;
    }
}
