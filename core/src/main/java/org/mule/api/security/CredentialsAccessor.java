/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
