/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.endpoint;

import org.mule.api.MuleMessage;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.config.MuleManifest;

public class DefaultInboundEndpoint extends AbstractEndpoint implements InboundEndpoint
{

    private static final long serialVersionUID = -4752772777414636142L;

    public MuleMessage request(long timeout) throws Exception
    {
        if (connector != null)
        {
            return connector.request(this, timeout);
        }
        else
        {
            // TODO Either remove because this should never happen or i18n the
            // message
            throw new IllegalStateException("The connector on the endpoint: " + toString()
                                            + " is null. Please contact " + MuleManifest.getDevListEmail());
        }
    }

}
