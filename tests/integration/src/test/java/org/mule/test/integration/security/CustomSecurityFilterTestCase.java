/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.security;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.mule.api.MuleMessage;
import org.mule.client.DefaultLocalMuleClient;
import org.mule.tck.junit4.FunctionalTestCase;

import java.util.HashMap;

import org.junit.Test;
import org.springframework.security.authentication.BadCredentialsException;

/**
 * See MULE-4916: spring beans inside a security filter
 */
public class CustomSecurityFilterTestCase extends FunctionalTestCase
{
    @Override
    protected String getConfigFile()
    {
        return "org/mule/test/integration/security/custom-security-filter-test.xml";
    }

    @Test
    public void testOutboundAutenticationSend() throws Exception
    {
        DefaultLocalMuleClient client = new DefaultLocalMuleClient(muleContext);

        HashMap<String, Object> props = new HashMap<String, Object>();
        props.put("username", "ross");
        props.put("pass", "ross");
        MuleMessage result = client.send("vm://test", "hi", props);
        assertNull(result.getExceptionPayload());

        props.put("pass", "badpass");

        MuleMessage resultMessage = client.send("vm://test", "hi", props);
        assertNotNull(resultMessage);
        assertNotNull(resultMessage.getExceptionPayload());
        assertEquals(BadCredentialsException.class, resultMessage.getExceptionPayload()
            .getRootException()
            .getClass());
    }
}
