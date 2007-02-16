/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.jms.weblogic;

import org.mule.providers.jms.JmsMessageDispatcher;
import org.mule.umo.endpoint.UMOImmutableEndpoint;

/**
 * Weblogic-specific JMS message dispatcher.
 */
public class WeblogicJmsMessageDispatcher extends JmsMessageDispatcher
{
    /**
     * Create an instance of the dispatcher.
     * @param endpoint endpoint
     */
    public WeblogicJmsMessageDispatcher(UMOImmutableEndpoint endpoint)
    {
        super(endpoint);
    }
}
