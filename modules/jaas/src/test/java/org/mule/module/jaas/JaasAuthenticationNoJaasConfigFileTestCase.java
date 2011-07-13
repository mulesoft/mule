/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.jaas;

import org.mule.api.MuleMessage;
import org.mule.api.security.UnauthorisedException;
import org.mule.util.ExceptionUtils;

import java.util.Map;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class JaasAuthenticationNoJaasConfigFileTestCase extends AbstractJaasFunctionalTestCase
{
    @Override
    protected String getConfigResources()
    {
        return "mule-conf-with-no-jaas-config-file.xml";
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

        try
        {
            muleContext.getClient().send("vm://test", "Test", props);
            fail("Exception expected");
        }
        catch (Exception e)
        {
            assertTrue(ExceptionUtils.containsType(e, UnauthorisedException.class));
        }
    }

    @Test
    public void badUserName() throws Exception
    {
        Map<String, Object> props = createMessagePropertiesWithCredentials("Evil", "dragon");

        try
        {
            muleContext.getClient().send("vm://test", "Test", props);
            fail("Exception expected");
        }
        catch (Exception e)
        {
            assertTrue(ExceptionUtils.containsType(e, UnauthorisedException.class));
        }
    }

    @Test
    public void badPassword() throws Exception
    {
        Map<String, Object> props = createMessagePropertiesWithCredentials("Marie.Rizzo", "evil");

        try
        {
            muleContext.getClient().send("vm://test", "Test", props);
            fail("Exception expected");
        }
        catch (Exception e)
        {
            assertTrue(ExceptionUtils.containsType(e, UnauthorisedException.class));
        }
    }
}
