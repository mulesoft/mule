/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.oracle.jms;

import org.mule.providers.jms.JmsMessageDispatcherFactory;
import org.mule.umo.UMOException;
import org.mule.umo.endpoint.UMOImmutableEndpoint;
import org.mule.umo.provider.UMOMessageDispatcher;

/**
 * @author <a href="mailto:carlson@hotpop.com">Travis Carlson</a>
 */
public class OracleJmsMessageDispatcherFactory extends JmsMessageDispatcherFactory
{
    public UMOMessageDispatcher create(UMOImmutableEndpoint endpoint) throws UMOException {
        return new OracleJmsMessageDispatcher(endpoint);
    }
}