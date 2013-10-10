/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.pgp;

import org.mule.api.security.Authentication;
import org.mule.api.security.SecurityContext;
import org.mule.api.security.SecurityContextFactory;

public class PGPSecurityContextFactory implements SecurityContextFactory
{
    public SecurityContext create(Authentication authentication)
    {
        return new PGPSecurityContext((PGPAuthentication) authentication);
    }
}
