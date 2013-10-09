/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.api.transport;

import org.mule.api.MuleException;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.config.i18n.CoreMessages;
import org.mule.config.i18n.Message;
import org.mule.util.ObjectUtils;

/**
 * <code>ReceiveException</code> is specifically thrown by the Provider receive
 * method if something fails in the underlying transport
 */
public class ReceiveException extends MuleException
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = 1960304517882133951L;

    private ImmutableEndpoint endpoint;

    /**
     * @param message the exception message
     */
    public ReceiveException(Message message, ImmutableEndpoint endpoint, long timeout)
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
    public ReceiveException(Message message, ImmutableEndpoint endpoint, long timeout, Throwable cause)
    {
        super(message, cause);
        this.endpoint = endpoint;
        addInfo("Endpoint", ObjectUtils.toString(this.endpoint, "null"));
        addInfo("Timeout", String.valueOf(timeout));
    }

    public ReceiveException(ImmutableEndpoint endpoint, long timeout, Throwable cause)
    {
        this(CoreMessages.failedToRecevieWithTimeout(endpoint, timeout),
            endpoint, timeout, cause);
    }
}
