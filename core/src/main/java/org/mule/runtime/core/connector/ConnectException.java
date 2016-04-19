/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.connector;

import org.mule.runtime.core.api.LocatedMuleException;
import org.mule.runtime.core.api.connector.Connectable;
import org.mule.runtime.core.config.i18n.Message;
import org.mule.runtime.core.transport.AbstractTransportMessageHandler;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

/** 
 * When this exception is thrown it will trigger a retry (reconnection) policy to go into effect if one is configured.
 */
public class ConnectException extends LocatedMuleException
{
    /** Serial version */
    private static final long serialVersionUID = -7802483584780922653L;

    /** Resource which has disconnected */
    private transient Connectable failed;
    
    public ConnectException(Message message, Connectable failed)
    {
        super(message, failed);
        // In the case of a MessageReceiver/MessageDispatcher, what we really want to reconnect is the Connector
        this.failed = failed instanceof AbstractTransportMessageHandler ? ((AbstractTransportMessageHandler) failed).getConnector() : failed;
        ;
    }

    public ConnectException(Message message, Throwable cause, Connectable failed)
    {
        super(message, cause, failed);
        // In the case of a MessageReceiver/MessageDispatcher, what we really want to reconnect is the Connector
        this.failed = failed instanceof AbstractTransportMessageHandler ? ((AbstractTransportMessageHandler) failed).getConnector() : failed;
        ;
    }

    public ConnectException(Throwable cause, Connectable failed)
    {
        super(cause, failed);
        // In the case of a MessageReceiver/MessageDispatcher, what we really want to reconnect is the Connector
        this.failed = failed instanceof AbstractTransportMessageHandler ? ((AbstractTransportMessageHandler) failed).getConnector() : failed;
        ;
    }
    
    public Connectable getFailed()
    {
        return failed;
    }
    
    private void writeObject(ObjectOutputStream out) throws Exception
    {
        out.defaultWriteObject();
        if (this.failed instanceof Serializable)
        {
            out.writeBoolean(true);
            out.writeObject(this.failed);
        }
        else
        {
            out.writeBoolean(false);
        }
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException
    {
        in.defaultReadObject();

        boolean failedWasSerialized = in.readBoolean();
        if (failedWasSerialized)
        {
            this.failed = (Connectable) in.readObject();
        }
    }
}
