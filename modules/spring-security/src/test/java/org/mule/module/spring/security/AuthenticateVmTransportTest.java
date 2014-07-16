/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
    protected String getConfigFile()
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
