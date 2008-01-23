/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.extras.jaas;

import org.mule.api.security.Authentication;
import org.mule.api.security.SecurityContext;

public class JaasSecurityContext implements SecurityContext
{

    private JaasAuthentication authentication;

    /**
     * Constructor for the class
     * 
     * @param authentication
     */
    public JaasSecurityContext(JaasAuthentication authentication)
    {
        this.authentication = authentication;
    }

    /**
     * Returns the authentication
     * 
     * @return authentication
     */
    public final Authentication getAuthentication()
    {
        return authentication;
    }

    /**
     * Sets the Authentication
     * 
     * @param authentication
     */
    public final void setAuthentication(Authentication authentication)
    {
        this.authentication = (JaasAuthentication) authentication;
    }
}
