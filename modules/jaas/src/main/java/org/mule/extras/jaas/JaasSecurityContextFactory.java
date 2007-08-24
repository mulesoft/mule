/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.extras.jaas;

import org.mule.impl.security.MuleAuthentication;
import org.mule.umo.security.UMOAuthentication;
import org.mule.umo.security.UMOSecurityContext;
import org.mule.umo.security.UMOSecurityContextFactory;

public class JaasSecurityContextFactory implements UMOSecurityContextFactory
{
    /**
     * Creates the Jaas Security Context
     * 
     * @param authentication
     * @return JaasSecurityContext((MuleAuthentication) authentication)
     */
    public final UMOSecurityContext create(UMOAuthentication authentication)
    {
        return new JaasSecurityContext((MuleAuthentication)authentication);
    }

}
