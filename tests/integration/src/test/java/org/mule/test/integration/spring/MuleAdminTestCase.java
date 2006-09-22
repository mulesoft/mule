/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.spring;

import org.mule.config.ConfigurationBuilder;
import org.mule.extras.client.MuleClient;
import org.mule.extras.client.RemoteDispatcher;
import org.mule.extras.spring.config.SpringConfigurationBuilder;
import org.mule.tck.FunctionalTestCase;
import org.mule.umo.UMOMessage;

/**
 * @author <a href="mailto:gnt@codehaus.org">Guillaume Nodet</a>
 * @version $Revision$
 */
public class MuleAdminTestCase extends FunctionalTestCase {

    protected String getConfigResources() {
        return "org/mule/test/integration/spring/mule-admin-spring.xml";
    }

    protected ConfigurationBuilder getBuilder() {
        return new SpringConfigurationBuilder();
    }

    public void testMuleAdminChannelInSpring() throws Exception
    {
        MuleClient mc = new MuleClient();
        RemoteDispatcher rd = mc.getRemoteDispatcher("tcp://localhost:60504");
        UMOMessage result = rd.sendToRemoteComponent("appleComponent", "string", null);
        assertNotNull(result);
    }

}
