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

import org.mule.umo.UMOEventContext;
import org.mule.umo.lifecycle.Callable;

public class EchoMsg implements Callable
{

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.umo.lifecycle.Callable#onCall(org.mule.umo.UMOEventContext)
     */
    public Object onCall(UMOEventContext eventContext) throws Exception
    {
        return eventContext.getMessageAsString(null);
    }

}
