/*
 * $Id$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.extras.pgp;

import org.mule.umo.security.UMOAuthentication;
import org.mule.umo.security.UMOSecurityContext;
import org.mule.umo.security.UMOSecurityContextFactory;

/**
 * @author ariva
 * 
 */
public class PGPSecurityContextFactory implements UMOSecurityContextFactory
{

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.umo.security.UMOSecurityContextFactory#create(org.mule.umo.security.UMOAuthentication)
     */
    public UMOSecurityContext create(UMOAuthentication authentication)
    {
        return new PGPSecurityContext((PGPAuthentication) authentication);
    }

}
