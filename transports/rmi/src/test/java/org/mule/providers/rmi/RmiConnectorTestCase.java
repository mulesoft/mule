/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.rmi;

import org.mule.tck.providers.AbstractConnectorTestCase;
import org.mule.umo.provider.UMOConnector;

public class RmiConnectorTestCase extends AbstractConnectorTestCase
{

    public UMOConnector getConnector() throws Exception
    {
        RmiConnector c = new RmiConnector();
        c.setName("RmiConnector");
        c.setSecurityManager(null);
        c.initialise(managementContext);
        return c;
    }

    public String getTestEndpointURI()
    {
        return "rmi://localhost:1099";
    }

    public Object getValidMessage() throws Exception
    {
        return "Hello".getBytes();
    }

    public void testProperties() throws Exception
    {
        RmiConnector c = (RmiConnector)connector;

        String securityPolicy = "rmi.policy";
        String serverCodebase = "file:///E:/projects/MyTesting/JAVA/rmi/classes/";

        c.setSecurityPolicy(securityPolicy);
        assertNotNull(c.getSecurityPolicy());
        c.setServerCodebase(serverCodebase);
        assertEquals(serverCodebase, c.getServerCodebase());
    }

}
