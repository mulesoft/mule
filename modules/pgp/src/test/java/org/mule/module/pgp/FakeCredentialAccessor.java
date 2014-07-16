/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.pgp;

import org.mule.api.MuleEvent;
import org.mule.api.security.CredentialsAccessor;

public class FakeCredentialAccessor implements CredentialsAccessor
{
    private String credentials = "Mule client <mule_client@mule.com>";
    
    public FakeCredentialAccessor()
    {

    }
    
    public FakeCredentialAccessor(String string)
    {
        this.credentials = string;
    }

    public String getCredentials()
    {
        return credentials;
    }

    public void setCredentials(String credentials)
    {
        this.credentials = credentials;
    }

    public Object getCredentials(MuleEvent event)
    {
        return this.credentials;
    }

    public void setCredentials(MuleEvent event, Object credentials)
    {
        // dummy
    }

}
