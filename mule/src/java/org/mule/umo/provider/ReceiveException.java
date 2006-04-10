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

import org.apache.commons.lang.ObjectUtils;
import org.mule.config.i18n.Message;
import org.mule.config.i18n.Messages;
import org.mule.umo.UMOException;
import org.mule.umo.endpoint.UMOImmutableEndpoint;

/**
 * <code>ReceiveException</code> is specifically thrown by the Provider
 * receive method if something fails in the underlying transport
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class ReceiveException extends UMOException
{

    private UMOImmutableEndpoint endpoint;

    /**
     * @param message
     *            the exception message
     */
    public ReceiveException(Message message, UMOImmutableEndpoint endpoint, long timeout)
    {
        super(message);
        this.endpoint = endpoint;
        addInfo("Endpoint", ObjectUtils.toString(this.endpoint, "null"));
        addInfo("Timeout", String.valueOf(timeout));
    }

    /**
     * @param message
     *            the exception message
     * @param cause
     *            the exception that cause this exception to be thrown
     */
    public ReceiveException(Message message, UMOImmutableEndpoint endpoint, long timeout, Throwable cause)
    {
        super(message, cause);
        this.endpoint = endpoint;
        addInfo("Endpoint", ObjectUtils.toString(this.endpoint, "null"));
        addInfo("Timeout", String.valueOf(timeout));
    }

    public ReceiveException(UMOImmutableEndpoint endpoint, long timeout, Throwable cause)
    {
        this(new Message(Messages.FAILED_TO_RECEIVE_OVER_X_TIMEOUT_X, endpoint,
                String.valueOf(timeout)), endpoint, timeout, cause);
    }

}
