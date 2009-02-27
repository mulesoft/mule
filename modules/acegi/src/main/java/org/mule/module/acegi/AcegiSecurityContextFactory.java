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
import org.mule.api.security.SecurityContextFactory;

import org.acegisecurity.context.SecurityContextHolder;
import org.acegisecurity.context.SecurityContextImpl;

/**
 * <code>AcegiSecurityContextFactory</code> creates an AcegiSecurityContext for an
 * {@link Authentication} object
 */
public class AcegiSecurityContextFactory implements SecurityContextFactory
{
    public SecurityContext create(Authentication authentication)
    {
        org.acegisecurity.context.SecurityContext context = new SecurityContextImpl();
        context.setAuthentication(((AcegiAuthenticationAdapter)authentication).getDelegate());

        if (authentication.getProperties() != null)
        {
            if ((authentication.getProperties().containsKey("securityMode")))
            {
                SecurityContextHolder.setStrategyName((String)authentication.getProperties().get(
                    "securityMode"));
            }
        }
        SecurityContextHolder.setContext(context);
        return new AcegiSecurityContext(context);
    }
}
