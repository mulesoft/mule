/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.tcp.issues;

import org.mule.providers.tcp.TcpConnector;
import org.mule.providers.tcp.TcpFunctionalTestCase;

/**
 * This is just to check that the Boolean (rather than boolean) doesn't cause any problems
 */
public class ReuseMule2069TestCase extends TcpFunctionalTestCase
{

    protected String getConfigResources()
    {
        return "reuse-mule-2069.xml";
    }

    public void testReuseSetOnConnector()
    {
        assertTrue(((TcpConnector) managementContext.getRegistry().lookupConnector("tcp")).isReuseAddress().booleanValue());
    }
    
}
