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
import org.mule.umo.endpoint.EndpointException;

/**
 * <code>NoReceiverForEndpointException</code> is thrown when an enpoint is specified for a
 * receiver but no such receiver exists.
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */

public class NoReceiverForEndpointException extends EndpointException
{
    /**
     * @param endpoint the endpoint that could not be located
     */
    public NoReceiverForEndpointException(String endpoint)
    {
        super(new Message(Messages.ENDPOINT_X_NOT_FOUND, endpoint));
    }

    /**
     * @param message the exception message
     */
    public NoReceiverForEndpointException(Message message)
    {
        super(message);
    }

    /**
     * @param message the exception message
     * @param cause   the exception that cause this exception to be thrown
     */
    public NoReceiverForEndpointException(Message message, Throwable cause)
    {
        super(message, cause);
    }

    public NoReceiverForEndpointException(Throwable cause)
    {
        super(cause);
    }

}
