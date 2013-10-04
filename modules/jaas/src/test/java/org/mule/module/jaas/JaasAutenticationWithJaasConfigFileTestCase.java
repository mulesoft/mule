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

import org.mule.api.MuleMessage;
import org.mule.api.security.UnauthorisedException;
import org.mule.util.ExceptionUtils;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

public class JaasAutenticationWithJaasConfigFileTestCase extends AbstractJaasFunctionalTestCase
{
    public JaasAutenticationWithJaasConfigFileTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{{ConfigVariant.SERVICE, "mule-conf-for-jaas-conf-file-service.xml"},
            {ConfigVariant.FLOW, "mule-conf-for-jaas-conf-file-flow.xml"}});
    }

    @Test
    public void goodAuthentication() throws Exception
    {
        Map<String, Object> props = createMessagePropertiesWithCredentials("Marie.Rizzo", "dragon");
        MuleMessage m = muleContext.getClient().send("vm://test", "Test", props);

        assertNotNull(m);
        assertTrue(m.getPayload() instanceof String);
        assertEquals("Test Received", m.getPayloadAsString());
    }

    @Test
    public void anotherGoodAuthentication() throws Exception
    {
        Map<String, Object> props = createMessagePropertiesWithCredentials("anon", "anon");
        MuleMessage m = muleContext.getClient().send("vm://test", "Test", props);

        assertNotNull(m);
        assertTrue(m.getPayload() instanceof String);
        assertEquals("Test Received", m.getPayloadAsString());
    }

    @Test
    public void wrongCombinationOfCorrectUsernameAndPassword() throws Exception
    {
        Map<String, Object> props = createMessagePropertiesWithCredentials("Marie.Rizzo", "anon");

        MuleMessage message = muleContext.getClient().send("vm://test", "Test", props);
        assertNotNull(message);
        assertNotNull(message.getExceptionPayload());
        assertTrue(ExceptionUtils.containsType(message.getExceptionPayload().getException(),
            UnauthorisedException.class));
    }

    @Test
    public void badUserName() throws Exception
    {
        Map<String, Object> props = createMessagePropertiesWithCredentials("Evil", "dragon");

        MuleMessage message = muleContext.getClient().send("vm://test", "Test", props);
        assertNotNull(message);
        assertNotNull(message.getExceptionPayload());
        assertTrue(ExceptionUtils.containsType(message.getExceptionPayload().getException(),
            UnauthorisedException.class));
    }

    @Test
    public void badPassword() throws Exception
    {
        Map<String, Object> props = createMessagePropertiesWithCredentials("Marie.Rizzo", "evil");

        MuleMessage message = muleContext.getClient().send("vm://test", "Test", props);
        assertNotNull(message);
        assertNotNull(message.getExceptionPayload());
        assertTrue(ExceptionUtils.containsType(message.getExceptionPayload().getException(),
            UnauthorisedException.class));

    }
}
