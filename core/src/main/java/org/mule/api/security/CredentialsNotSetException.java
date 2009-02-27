/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.api.security;

import org.mule.api.MuleMessage;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.config.i18n.Message;

/**
 * <code>CredentialsNotSetException</code> is thrown when user credentials cannot
 * be obtained from the current message
 */
public class CredentialsNotSetException extends UnauthorisedException
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = -6271648179641734579L;

    public CredentialsNotSetException(Message message, MuleMessage muleMessage)
    {
        super(message, muleMessage);
    }

    public CredentialsNotSetException(Message message, MuleMessage muleMessage, Throwable cause)
    {
        super(message, muleMessage, cause);
    }

    public CredentialsNotSetException(MuleMessage muleMessage,
                                      SecurityContext context,
                                      ImmutableEndpoint endpoint,
                                      EndpointSecurityFilter filter)
    {
        super(muleMessage, context, endpoint, filter);
    }
}
