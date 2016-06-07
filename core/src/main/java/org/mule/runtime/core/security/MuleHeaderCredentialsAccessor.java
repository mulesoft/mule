/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.security;

import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.config.MuleProperties;
import org.mule.runtime.core.api.security.CredentialsAccessor;

import java.io.Serializable;

/**
 * <code>MuleHeaderCredentialsAccessor</code> obtains and sets the user credentials
 * as Mule property headers.
 */
public class MuleHeaderCredentialsAccessor implements CredentialsAccessor
{
    public Serializable getCredentials(MuleEvent event)
    {
        return event.getMessage().getInboundProperty(MuleProperties.MULE_USER_PROPERTY);
    }

    public void setCredentials(MuleEvent event, Serializable credentials)
    {
        event.getMessage().setOutboundProperty(MuleProperties.MULE_USER_PROPERTY, credentials);
    }
}
