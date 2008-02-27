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

import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.transport.DispatchException;
import org.mule.config.MuleManifest;

public class DefaultOutboundEndpoint extends AbstractEndpoint implements OutboundEndpoint
{

    private static final long serialVersionUID = 8860985949279708638L;

    public void dispatch(MuleEvent event) throws DispatchException
    {
        if (connector != null)
        {
            connector.dispatch(this, event);
        }
        else
        {
            // TODO Either remove because this should never happen or i18n the
            // message
            throw new IllegalStateException("The connector on the endpoint: " + toString()
                                            + " is null. Please contact " + MuleManifest.getDevListEmail());
        }
    }

    public MuleMessage send(MuleEvent event) throws DispatchException
    {
        if (connector != null)
        {
            return connector.send(this, event);
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
