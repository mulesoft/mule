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
package org.mule.umo.provider;

import org.mule.config.i18n.Message;
import org.mule.config.i18n.Messages;
import org.mule.umo.UMOException;

/**
 * <code>ConnectorException</code> TODO
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class ConnectorException extends UMOException
{
    private transient UMOConnector connector;

    /**
     * @param message the exception message
     */
    public ConnectorException(Message message, UMOConnector connector)
    {
        super(generateMessage(message, connector));
        this.connector = connector;
    }

    /**
     * @param message the exception message
     * @param cause   the exception that cause this exception to be thrown
     */
    public ConnectorException(Message message, UMOConnector connector, Throwable cause)
    {
        super(generateMessage(message, connector), cause);
        this.connector = connector;
    }

     private static Message generateMessage(Message message, UMOConnector connector) {
         Message m = new Message(Messages.CONNECTOR_CAUSED_ERROR, connector);
         if(message!=null) {
             message.setNextMessage(m);
             return message;
         } else {
             message = new Message(-1);
             return m;
         }
    }

    public UMOConnector getConnector() {
        return connector;
    }
}
