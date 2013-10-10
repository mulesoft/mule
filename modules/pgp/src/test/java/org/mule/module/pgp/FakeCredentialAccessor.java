/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
