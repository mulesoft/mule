/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers;

import org.mule.umo.UMOMessage;
import org.mule.umo.UMOEvent;
import org.mule.umo.endpoint.UMOImmutableEndpoint;

public final class UnsupportedMessageDispatcher extends AbstractMessageDispatcher
{

    public UnsupportedMessageDispatcher(UMOImmutableEndpoint endpoint)
    {
        super(endpoint);
    }

    protected void doDispatch(UMOEvent event) throws Exception
    {
        throw new UnsupportedOperationException("Dispatch not supported for this transport.");
    }

    protected UMOMessage doSend(UMOEvent event) throws Exception
    {
        throw new UnsupportedOperationException("Send not supported for this transport.");
    }

    protected void doDispose()
    {
        // empty
    }

    protected void doConnect() throws Exception
    {
        // empty
    }

    protected void doDisconnect() throws Exception
    {
        // empty
    }

}
