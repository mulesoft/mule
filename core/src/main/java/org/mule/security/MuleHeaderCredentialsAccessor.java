/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.security;

import org.mule.api.MuleEvent;
import org.mule.api.config.MuleProperties;
import org.mule.api.security.CredentialsAccessor;

/**
 * <code>MuleHeaderCredentialsAccessor</code> obtains and sets the user credentials
 * as Mule property headers.
 */
public class MuleHeaderCredentialsAccessor implements CredentialsAccessor
{
    public Object getCredentials(MuleEvent event)
    {
        return event.getMessage().getInboundProperty(MuleProperties.MULE_USER_PROPERTY);
    }

    public void setCredentials(MuleEvent event, Object credentials)
    {
        event.getMessage().setOutboundProperty(MuleProperties.MULE_USER_PROPERTY, credentials);
    }
}
