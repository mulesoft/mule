/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.pgp;

import org.mule.api.security.Authentication;
import org.mule.api.security.SecurityContext;

public class PGPSecurityContext implements SecurityContext
{
    private transient PGPAuthentication authentication;

    public PGPSecurityContext(PGPAuthentication authentication)
    {
        this.setAuthentication(authentication);
    }

    public void setAuthentication(Authentication authentication)
    {
        this.authentication = (PGPAuthentication)authentication;
    }

    public Authentication getAuthentication()
    {
        return authentication;
    }
}
