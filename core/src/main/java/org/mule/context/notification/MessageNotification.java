/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.context.notification;

import org.mule.api.MuleMessage;
import org.mule.api.endpoint.ImmutableEndpoint;

/**
 * These notifications are fire when either a message is received via an endpoint, or
 * dispatcher of if a receive call is made on a dispatcher.
 * 
 * @deprecated renamed to EndpointMessageNotification
 */
public class MessageNotification extends EndpointMessageNotification
{

    /**
     * Serial version
     */
    private static final long serialVersionUID = -5118299601117624094L;

    /**
     * @param resource
     * @param endpoint
     * @param identifier
     * @param action
     * @deprecated
     */
    public MessageNotification(MuleMessage resource, ImmutableEndpoint endpoint, String identifier, int action)
    {
        super(resource, endpoint, identifier, action);
    }

}
