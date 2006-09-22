/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.providers.xmpp;

import org.mule.umo.UMOException;
import org.mule.umo.endpoint.UMOImmutableEndpoint;
import org.mule.umo.provider.UMOMessageDispatcher;
import org.mule.umo.provider.UMOMessageDispatcherFactory;

/**
 * <code>XmppMessageDispatcherFactory</code> creates a dispatcher responsible for writing Xmpp packets to a an Jabber chat
 * 
 * @author Peter Braswell
 * @version $Revision$
 */

public class XmppMessageDispatcherFactory implements UMOMessageDispatcherFactory
{
    public UMOMessageDispatcher create(UMOImmutableEndpoint endpoint) throws UMOException
    {
        return new XmppMessageDispatcher( endpoint);
    }
}
