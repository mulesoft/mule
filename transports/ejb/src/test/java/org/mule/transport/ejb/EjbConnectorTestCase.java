/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.ejb;

import org.mule.api.transport.Connector;
import org.mule.transport.AbstractConnectorTestCase;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class EjbConnectorTestCase extends AbstractConnectorTestCase
{

    public Connector createConnector() throws Exception
    {
        EjbConnector c = new EjbConnector(muleContext);
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

    @Test
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
