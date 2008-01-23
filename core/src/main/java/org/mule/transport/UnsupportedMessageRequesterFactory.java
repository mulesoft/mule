/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport;

import org.mule.api.MuleException;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.transport.MessageRequester;

public final class UnsupportedMessageRequesterFactory extends AbstractMessageRequesterFactory
{

    public MessageRequester create(ImmutableEndpoint endpoint) throws MuleException
    {
        return new UnsupportedMessageRequester(endpoint);
    }

}