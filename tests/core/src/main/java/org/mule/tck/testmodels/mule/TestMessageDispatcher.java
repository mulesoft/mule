/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.tck.testmodels.mule;

import org.mule.providers.AbstractMessageDispatcher;
import org.mule.umo.UMOEvent;
import org.mule.umo.UMOMessage;
import org.mule.umo.UMOException;
import org.mule.umo.routing.RoutingException;
import org.mule.umo.endpoint.UMOImmutableEndpoint;

/**
 * todo document
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class TestMessageDispatcher extends AbstractMessageDispatcher
{

    public TestMessageDispatcher(final UMOImmutableEndpoint endpoint)
    {
        super(endpoint);
    }

    protected void doDispose()
    {
        // template method
    }

    protected void doDispatch(UMOEvent event) throws Exception
    {
        if (event.getEndpoint().getEndpointURI().toString().equals("test://AlwaysFail"))
        {
            throw new RoutingException(event.getMessage(), event.getEndpoint());
        }
    }

    protected UMOMessage doSend(UMOEvent event) throws Exception
    {
        if (event.getEndpoint().getEndpointURI().toString().equals("test://AlwaysFail"))
        {
            throw new RoutingException(event.getMessage(), event.getEndpoint());
        }
        return event.getMessage();
    }

    public UMOMessage doReceive(UMOImmutableEndpoint endpoint, long timeout) throws Exception
    {
        return null;
    }

    public Object getDelegateSession() throws UMOException
    {
        return null;
    }

    protected void doConnect(UMOImmutableEndpoint endpoint) throws Exception
    {
        // no op
    }

    protected void doDisconnect() throws Exception
    {
        // no op
    }

}
