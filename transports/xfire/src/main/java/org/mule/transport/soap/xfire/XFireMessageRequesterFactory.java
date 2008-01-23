/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.soap.xfire;

import org.mule.api.MuleException;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.transport.MessageRequester;
import org.mule.transport.AbstractMessageRequesterFactory;

/**
 * Creates an XFire Message requester used for making XFire soap requests using the
 * XFire client.
 */
public class XFireMessageRequesterFactory extends AbstractMessageRequesterFactory
{
    public MessageRequester create(ImmutableEndpoint endpoint) throws MuleException
    {
        return new XFireMessageRequester(endpoint);
    }
}