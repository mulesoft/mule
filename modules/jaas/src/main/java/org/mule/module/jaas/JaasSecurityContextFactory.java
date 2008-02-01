/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.jaas;

import org.mule.api.security.Authentication;
import org.mule.api.security.SecurityContext;
import org.mule.api.security.SecurityContextFactory;

public class JaasSecurityContextFactory implements SecurityContextFactory
{
    /**
     * Creates the Jaas Security Context
     * 
     * @param authentication
     * @return JaasSecurityContext((Authentication) authentication)
     */
    public final SecurityContext create(Authentication authentication)
    {
        return new JaasSecurityContext((JaasAuthentication) authentication);
    }

}
