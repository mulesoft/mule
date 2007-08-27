/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.umo.provider;

import org.mule.config.i18n.CoreMessages;
import org.mule.config.i18n.Message;
import org.mule.umo.UMOException;
import org.mule.umo.endpoint.UMOImmutableEndpoint;
import org.mule.util.ObjectUtils;

/**
 * <code>ReceiveException</code> is specifically thrown by the Provider receive
 * method if something fails in the underlying transport
 */
public class ReceiveException extends UMOException
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = 1960304517882133951L;

    private UMOImmutableEndpoint endpoint;

    /**
     * @param message the exception message
     */
    public ReceiveException(Message message, UMOImmutableEndpoint endpoint, long timeout)
    {
        super(message);
        this.endpoint = endpoint;
        addInfo("Endpoint", ObjectUtils.toString(this.endpoint, "null"));
        addInfo("Timeout", String.valueOf(timeout));
    }

    /**
     * @param message the exception message
     * @param cause the exception that cause this exception to be thrown
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
        this(CoreMessages.failedToRecevieWithTimeout(endpoint, timeout),
            endpoint, timeout, cause);
    }
}
