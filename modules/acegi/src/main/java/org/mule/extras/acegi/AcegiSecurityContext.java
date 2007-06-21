/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.extras.acegi;

import org.mule.umo.security.UMOAuthentication;
import org.mule.umo.security.UMOSecurityContext;

import org.acegisecurity.context.SecurityContext;
import org.acegisecurity.context.SecurityContextHolder;

/**
 * <code>AcegiSecurityContext</code> is a UMOSecurityContext wrapper used to
 * interface with an Acegi SecurityContext
 */

public class AcegiSecurityContext implements UMOSecurityContext
{
    private SecurityContext delegate;
    private AcegiAuthenticationAdapter authentication;

    public AcegiSecurityContext(SecurityContext delegate)
    {
        this.delegate = delegate;
        SecurityContextHolder.setContext(this.delegate);
    }

    public void setAuthentication(UMOAuthentication authentication)
    {
        this.authentication = ((AcegiAuthenticationAdapter)authentication);
        delegate.setAuthentication(this.authentication.getDelegate());
        SecurityContextHolder.setContext(delegate);
    }

    public UMOAuthentication getAuthentication()
    {
        return this.authentication;
    }
}
