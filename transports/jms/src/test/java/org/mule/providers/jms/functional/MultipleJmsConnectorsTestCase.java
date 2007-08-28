/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.jms.functional;

import org.mule.tck.FunctionalTestCase;
import org.mule.umo.endpoint.UMOEndpoint;

public class MultipleJmsConnectorsTestCase extends FunctionalTestCase
{
    protected String getConfigResources()
    {
        return "jms-multiple-connectors.xml";
    }
    
    public void testMultipleJmsClientConnections() throws Exception
    {
        UMOEndpoint ep1 = managementContext.getRegistry().lookupEndpoint("ep1");
        ep1.dispatch(getTestEvent("testing"));
        UMOEndpoint ep2 = managementContext.getRegistry().lookupEndpoint("ep2");
        ep2.dispatch(getTestEvent("testing"));

        // wait a bit to let the messages go on their way
        Thread.sleep(3000);

        assertEquals(2, managementContext.getRegistry().getConnectors().size());
    }
}
