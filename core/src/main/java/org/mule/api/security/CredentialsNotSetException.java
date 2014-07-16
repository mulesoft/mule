/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.api.security;

import org.mule.api.MuleEvent;
import org.mule.config.i18n.Message;

import java.net.URI;

/**
 * <code>CredentialsNotSetException</code> is thrown when user credentials cannot
 * be obtained from the current message
 */
public class CredentialsNotSetException extends UnauthorisedException
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = -6271648179641734580L;

    public CredentialsNotSetException(Message message, MuleEvent event)
    {
        super(message, event);
    }

    public CredentialsNotSetException(Message message, MuleEvent event, Throwable cause)
    {
        super(message, event, cause);
    }

    public CredentialsNotSetException(MuleEvent event, SecurityContext context, SecurityFilter filter)
    {
        super(event, context, filter);
    }
    
    @Deprecated
    public CredentialsNotSetException(MuleEvent event,
                                      SecurityContext context,
                                      URI endpointUri,
                                      SecurityFilter filter)
    {
        super(event, context, endpointUri, filter);
    }
}
