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
 */
package org.mule.umo.provider;

import org.mule.umo.endpoint.UMOEndpointURI;
import org.mule.umo.UMOException;
import org.mule.config.i18n.Message;
import org.mule.config.i18n.Messages;



/**
 * <code>ReceiveException</code> is specifically thrown by the Provider
 * receive method if something fails in the underlying transport
 *
 * @author <a href="mailto:ross.mason@cubis.co.uk">Ross Mason</a>
 * @version $Revision$
 */
public class ReceiveException extends UMOException
{

    /**
     * @param message the exception message
     */
    public ReceiveException(Message message, UMOEndpointURI endpointUri, long timeout)
    {
        super(message);
        addInfo("Endpoint", endpointUri.toString());
        addInfo("Timeout", new Long(timeout).toString());
    }

    /**
     * @param message the exception message
     * @param cause   the exception that cause this exception to be thrown
     */
    public ReceiveException(Message message, UMOEndpointURI endpointUri, long timeout, Throwable cause)
    {
        super(message, cause);
        addInfo("Endpoint", endpointUri.toString());
        addInfo("Timeout", new Long(timeout).toString());
    }

    public ReceiveException(UMOEndpointURI endpointUri, long timeout, Throwable cause)
    {
        super(new Message(Messages.FAILED_TO_RECEIVE_OVER_X_TIMEOUT_X, endpointUri, String.valueOf(timeout)), cause);
    }
}
