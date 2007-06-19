/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.http.servlet;

import org.mule.providers.AbstractMessageReceiver;
import org.mule.umo.UMOComponent;
import org.mule.umo.UMOException;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.lifecycle.CreateException;
import org.mule.umo.provider.UMOConnector;

/**
 * <code>ServletMessageReceiver</code> is a receiver that is invoked from a Servlet
 * when an event is received. There is a one-to-one mapping between a
 * ServletMessageReceiver and a servlet in the serving webapp.
 */

public class ServletMessageReceiver extends AbstractMessageReceiver
{
    public ServletMessageReceiver(UMOConnector connector, UMOComponent component, UMOEndpoint endpoint)
            throws CreateException
    {

        super(connector, component, endpoint);

    }

    protected void doDispose()
    {
        // template method
    }

    protected void doConnect() throws Exception
    {
        // nothing to do
    }

    protected void doDisconnect() throws Exception
    {
        // nothing to do
    }

    protected void doStart() throws UMOException
    {
        // nothing to do
    }

    protected void doStop() throws UMOException
    {
        // nothing to do
    }

}
