/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.acegi;

import org.mule.api.security.Authentication;
import org.mule.api.security.SecurityContext;

import org.acegisecurity.context.SecurityContextHolder;

/**
 * <code>AcegiSecurityContext</code> is a SecurityContext wrapper used to
 * interface with an Acegi SecurityContext
 */

public class AcegiSecurityContext implements SecurityContext
{
    private org.acegisecurity.context.SecurityContext delegate;
    private AcegiAuthenticationAdapter authentication;

    public AcegiSecurityContext(org.acegisecurity.context.SecurityContext delegate)
    {
        this.delegate = delegate;
        SecurityContextHolder.setContext(this.delegate);
    }

    public void setAuthentication(Authentication authentication)
    {
        this.authentication = ((AcegiAuthenticationAdapter)authentication);
        delegate.setAuthentication(this.authentication.getDelegate());
        SecurityContextHolder.setContext(delegate);
    }

    public Authentication getAuthentication()
    {
        return this.authentication;
    }
}
