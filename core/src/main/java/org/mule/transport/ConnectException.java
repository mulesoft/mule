/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport;

import org.mule.api.MuleException;
import org.mule.api.transport.Connectable;
import org.mule.config.i18n.Message;

/** 
 * When this exception is thrown it will trigger a retry (reconnection) policy to go into effect if one is configured.
 */
public class ConnectException extends MuleException
{
    /** Serial version */
    private static final long serialVersionUID = -7802483584780922653L;

    /** Resource which has disconnected */
    private Connectable failed;
    
    public ConnectException(Message message, Connectable failed)
    {
        super(message);
        // In the case of a MessageReceiver/MessageDispatcher, what we really want to reconnect is the Connector
        this.failed = failed instanceof AbstractTransportMessageHandler ? ((AbstractTransportMessageHandler) failed).getConnector() : failed;
    }

    public ConnectException(Message message, Throwable cause, Connectable failed)
    {
        super(message, cause);
        // In the case of a MessageReceiver/MessageDispatcher, what we really want to reconnect is the Connector
        this.failed = failed instanceof AbstractTransportMessageHandler ? ((AbstractTransportMessageHandler) failed).getConnector() : failed;
    }

    public ConnectException(Throwable cause, Connectable failed)
    {
        super(cause);
        // In the case of a MessageReceiver/MessageDispatcher, what we really want to reconnect is the Connector
        this.failed = failed instanceof AbstractTransportMessageHandler ? ((AbstractTransportMessageHandler) failed).getConnector() : failed;
    }
    
    public Connectable getFailed()
    {
        return failed;
    }
}
