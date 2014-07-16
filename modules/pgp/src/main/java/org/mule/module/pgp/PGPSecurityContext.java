/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
