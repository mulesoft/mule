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

import org.mule.umo.UMOEvent;
import org.mule.umo.security.UMOCredentialsAccessor;

/**
 * @author ariva
 */
public class FakeCredentialAccessor implements UMOCredentialsAccessor
{

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.umo.security.UMOCredentialsAccessor#getCredentials(org.mule.umo.UMOEvent)
     */
    public Object getCredentials(UMOEvent event)
    {
        return "Mule client <mule_client@mule.com>";
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.umo.security.UMOCredentialsAccessor#setCredentials(org.mule.umo.UMOEvent,
     *      java.lang.Object)
     */
    public void setCredentials(UMOEvent event, Object credentials)
    {
        // dummy
    }

}
