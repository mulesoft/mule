/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.impl.endpoint;

import org.mule.umo.endpoint.UMOEndpoint;

public class GlobalEndpoint extends MuleEndpoint
{

    private static final long serialVersionUID = 987360887269899791L;

    public String getType()
    {
        return UMOEndpoint.ENDPOINT_TYPE_GLOBAL;
    }

}
