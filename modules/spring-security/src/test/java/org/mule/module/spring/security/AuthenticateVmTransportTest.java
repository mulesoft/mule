/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.spring.security;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.mule.api.MuleMessage;
import org.mule.api.security.UnauthorisedException;
import org.mule.client.DefaultLocalMuleClient;
import org.mule.tck.junit4.FunctionalTestCase;

import java.util.HashMap;

import org.junit.Test;

public class AuthenticateVmTransportTest extends FunctionalTestCase
{
    @Override
    protected String getConfigResources()
    {
        return "auth-vm-transport-config.xml";
    }

    @Test
    public void testExplicitAttributes() throws Exception
    {
        testVM("vm://test");
    }

    @Test
    public void testDefaultAttributes() throws Exception
    {
        testVM("vm://default-attributes");
    }

    protected void testVM(String endpoint) throws Exception
    {
        DefaultLocalMuleClient client = new DefaultLocalMuleClient(muleContext);

        HashMap<String, Object> props = new HashMap<String,Object>();
        props.put("username", "ross");
        props.put("password", "ross");
        MuleMessage result = client.send(endpoint, "hi", props);
        assertNull(result.getExceptionPayload());

        props.put("password", "badpass");
        MuleMessage result2 = client.send(endpoint, "hi", props);
        assertNotNull(result2);
        assertNotNull(result2.getExceptionPayload());
        assertEquals(UnauthorisedException.class, result2.getExceptionPayload().getException().getClass());

    }
}
