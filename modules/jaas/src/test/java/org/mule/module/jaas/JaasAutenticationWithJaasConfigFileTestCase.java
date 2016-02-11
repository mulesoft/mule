/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.jaas;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.mule.api.MuleEvent;
import org.mule.api.security.UnauthorisedException;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class JaasAutenticationWithJaasConfigFileTestCase extends AbstractJaasFunctionalTestCase
{
    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Override
    protected String getConfigFile()
    {
        return "mule-conf-for-jaas-conf-file-flow.xml";
    }

    @Test
    public void goodAuthentication() throws Exception
    {
        SecurityHeader securityHeader = createSecurityHeader("Marie.Rizzo", "dragon");
        MuleEvent result = flowRunner("TestUMO").withInboundProperty(securityHeader.getKey(), securityHeader.getValue()).withPayload("Test").run();

        assertNotNull(result);
        assertTrue(result.getMessage().getPayload() instanceof String);
        assertEquals("Test Received", getPayloadAsString(result.getMessage()));
    }

    @Test
    public void anotherGoodAuthentication() throws Exception
    {
        SecurityHeader securityHeader = createSecurityHeader("anon", "anon");
        MuleEvent result = flowRunner("TestUMO").withInboundProperty(securityHeader.getKey(), securityHeader.getValue()).withPayload("Test").run();

        assertNotNull(result);
        assertTrue(result.getMessage().getPayload() instanceof String);
        assertEquals("Test Received", getPayloadAsString(result.getMessage()));
    }

    @Test
    public void wrongCombinationOfCorrectUsernameAndPassword() throws Exception
    {
        SecurityHeader securityHeader = createSecurityHeader("Marie.Rizzo", "anon");
        expectedException.expect(UnauthorisedException.class);
        flowRunner("TestUMO").withInboundProperty(securityHeader.getKey(), securityHeader.getValue()).withPayload("Test").run();
    }

    @Test
    public void badUserName() throws Exception
    {
        SecurityHeader securityHeader = createSecurityHeader("Evil", "dragon");
        expectedException.expect(UnauthorisedException.class);
        flowRunner("TestUMO").withInboundProperty(securityHeader.getKey(), securityHeader.getValue()).withPayload("Test").run();
    }

    @Test
    public void badPassword() throws Exception
    {
        SecurityHeader securityHeader = createSecurityHeader("Marie.Rizzo", "evil");
        expectedException.expect(UnauthorisedException.class);
        flowRunner("TestUMO").withInboundProperty(securityHeader.getKey(), securityHeader.getValue()).withPayload("Test").run();
    }
}
