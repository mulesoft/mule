/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.ejb;

import org.mule.tck.providers.AbstractConnectorTestCase;
import org.mule.umo.provider.UMOConnector;

public class EjbConnectorTestCase extends AbstractConnectorTestCase
{

    public UMOConnector createConnector() throws Exception
    {
        EjbConnector c = new EjbConnector();
        c.setName("EjbConnector");
        c.setSecurityManager(null);
        return c;
    }

    public String getTestEndpointURI()
    {
        return "ejb://localhost:1099";
    }

    public Object getValidMessage() throws Exception
    {
        return "Hello".getBytes();
    }

    public void testProperties() throws Exception
    {
        EjbConnector c = (EjbConnector) getConnector();

        String securityPolicy = "rmi.policy";
        String serverCodebase = "file:///E:/projects/MyTesting/JAVA/rmi/classes/";

        c.setSecurityPolicy(securityPolicy);
        assertNotNull(c.getSecurityPolicy());
        c.setServerCodebase(serverCodebase);
        assertEquals(serverCodebase, c.getServerCodebase());
    }

}
