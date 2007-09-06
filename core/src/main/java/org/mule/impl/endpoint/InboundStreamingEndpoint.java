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

public class InboundStreamingEndpoint extends InboundEndpoint
{

    private static final long serialVersionUID = -4985435727516238801L;

    public boolean isStreaming()
    {
        return true;
    }

}
