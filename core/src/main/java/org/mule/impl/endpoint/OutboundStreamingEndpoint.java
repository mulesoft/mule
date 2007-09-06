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

public class OutboundStreamingEndpoint extends OutboundEndpoint
{

    private static final long serialVersionUID = -7565594337514235463L;

    public boolean isStreaming()
    {
        return true;
    }
}
