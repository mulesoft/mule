/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
