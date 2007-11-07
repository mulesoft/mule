/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.integration.spring;

import org.mule.extras.client.MuleClient;
import org.mule.extras.client.RemoteDispatcher;
import org.mule.tck.FunctionalTestCase;
import org.mule.umo.UMOMessage;

public class MuleAdminTestCase extends FunctionalTestCase
{
    protected String getConfigResources()
    {
        return "org/mule/test/integration/spring/mule-admin-spring.xml";
    }

    public void testMuleAdminChannelInSpring() throws Exception
    {
        MuleClient mc = new MuleClient();
        RemoteDispatcher rd = mc.getRemoteDispatcher("tcp://localhost:60504");
        UMOMessage result = rd.sendToRemoteComponent("appleComponent", "string", null);
        assertNotNull(result);
    }
}
