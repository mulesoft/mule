/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.api.security;

import org.mule.api.MuleEvent;

/**
 * <code>CredentialsAccessor</code> is a template for obtaining user credentials
 * from the current message and writing the user credentials to an outbound message
 */
public interface CredentialsAccessor
{
    Object getCredentials(MuleEvent event);

    void setCredentials(MuleEvent event, Object credentials);
}
