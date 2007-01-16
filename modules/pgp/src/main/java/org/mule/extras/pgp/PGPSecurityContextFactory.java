/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.extras.pgp;

import org.mule.umo.security.UMOAuthentication;
import org.mule.umo.security.UMOSecurityContext;
import org.mule.umo.security.UMOSecurityContextFactory;

public class PGPSecurityContextFactory implements UMOSecurityContextFactory
{

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.umo.security.UMOSecurityContextFactory#create(org.mule.umo.security.UMOAuthentication)
     */
    public UMOSecurityContext create(UMOAuthentication authentication)
    {
        return new PGPSecurityContext((PGPAuthentication)authentication);
    }

}
