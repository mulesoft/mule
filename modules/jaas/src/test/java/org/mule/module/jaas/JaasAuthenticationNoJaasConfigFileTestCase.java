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
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.security.UnauthorisedException;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class JaasAuthenticationNoJaasConfigFileTestCase extends AbstractJaasFunctionalTestCase
{

    @Rule
    public ExpectedException exceptionException = ExpectedException.none();

    @Override
    protected String getConfigFile()
    {
        return "mule-conf-with-no-jaas-config-file-flow.xml";
    }

    @Test
    public void goodAuthentication() throws Exception
    {
        SecurityHeader securityHeader = createSecurityHeader("Marie.Rizzo", "dragon");
        MuleMessage message = flowRunner("TestUMO")
                .withInboundProperty(securityHeader.getKey(), securityHeader.getValue())
                .withPayload("Test").run().getMessage();

        assertNotNull(message);
        assertTrue(message.getPayload() instanceof String);
        assertEquals("Test Received", getPayloadAsString(message));
    }

    @Test
    public void anotherGoodAuthentication() throws Exception
    {
        SecurityHeader securityHeader = createSecurityHeader("anon", "anon");
        MuleMessage message = flowRunner("TestUMO")
                .withInboundProperty(securityHeader.getKey(), securityHeader.getValue())
                .withPayload("Test").run().getMessage();

        assertNotNull(message);
        assertTrue(message.getPayload() instanceof String);
        assertEquals("Test Received", getPayloadAsString(message));
    }

    @Test
    public void wrongCombinationOfCorrectUsernameAndPassword() throws Exception
    {
        SecurityHeader securityHeader = createSecurityHeader("Marie.Rizzo", "anon");
        exceptionException.expect(UnauthorisedException.class);
        flowRunner("TestUMO").withInboundProperty(securityHeader.getKey(), securityHeader.getValue()).run().getMessage();
    }

    @Test
    public void badUserName() throws Exception
    {
        SecurityHeader securityHeader = createSecurityHeader("Evil", "dragon");
        exceptionException.expect(UnauthorisedException.class);
        flowRunner("TestUMO").withInboundProperty(securityHeader.getKey(), securityHeader.getValue()).run().getMessage();
    }

    @Test
    public void badPassword() throws Exception
    {
        SecurityHeader securityHeader = createSecurityHeader("Marie.Rizzo", "evil");
        exceptionException.expect(UnauthorisedException.class);
        flowRunner("TestUMO").withInboundProperty(securityHeader.getKey(), securityHeader.getValue()).run().getMessage();
    }

    public static class AddNotSerializableProperty implements MessageProcessor
    {
        @Override
        public MuleEvent process(MuleEvent event) throws MuleException
        {
            event.getMessage().setInvocationProperty("notSerializableProperty",new Object());
            return event;
        }
    }
}
