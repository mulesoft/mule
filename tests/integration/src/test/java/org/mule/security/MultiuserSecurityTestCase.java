/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.security;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import org.mule.api.EncryptionStrategy;
import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.api.config.MuleProperties;
import org.mule.tck.junit4.FunctionalTestCase;

import java.util.HashMap;
import java.util.Map;

import org.junit.Ignore;
import org.junit.Test;

/**
 * Tests multi-user security against a security provider which only authenticates
 * a single user at a time (i.e., authentication of a new user overwrites the
 * previous authentication).
 *
 * see EE-979
 */
@Ignore
public class MultiuserSecurityTestCase extends FunctionalTestCase
{

    @Override
    protected String[] getConfigFiles()
    {
        return new String[] {
                "multiuser-security-test-flow.xml",
                "singleuser-security-provider.xml"
        };
    }

    @Test
    public void testMultipleAuthentications() throws Exception
    {
        MuleClient client = muleContext.getClient();
        MuleMessage reply;
        Map<String, Object> props;

        EncryptionStrategy strategy = muleContext.getSecurityManager().getEncryptionStrategy("PBE");

        props = new HashMap<String, Object>();
        props.put(MuleProperties.MULE_USER_PROPERTY, MuleCredentials.createHeader("marie", "marie", "PBE", strategy));
        reply = client.send("vm://test", "Data1", props);
        assertNotNull(reply);
        assertEquals("user = marie, logins = 1, color = bright red", reply.getPayload());

        props = new HashMap<String, Object>();
        props.put(MuleProperties.MULE_USER_PROPERTY, MuleCredentials.createHeader("stan", "stan", "PBE", strategy));
        reply = client.send("vm://test", "Data2", props);
        assertNotNull(reply);
        assertEquals("user = stan, logins = 1, color = metallic blue", reply.getPayload());

        props = new HashMap<String, Object>();
        props.put(MuleProperties.MULE_USER_PROPERTY, MuleCredentials.createHeader("cindy", "cindy", "PBE", strategy));
        reply = client.send("vm://test", "Data3", props);
        assertEquals("user = cindy, logins = 1, color = dark violet", reply.getPayload());

        props = new HashMap<String, Object>();
        props.put(MuleProperties.MULE_USER_PROPERTY, MuleCredentials.createHeader("marie", "marie", "PBE", strategy));
        reply = client.send("vm://test", "Data4", props);
        assertNotNull(reply);
        assertEquals("user = marie, logins = 2, color = bright red", reply.getPayload());

        props = new HashMap<String, Object>();
        props.put(MuleProperties.MULE_USER_PROPERTY, MuleCredentials.createHeader("marie", "marie", "PBE", strategy));
        reply = client.send("vm://test", "Data4", props);
        assertNotNull(reply);
        assertEquals("user = marie, logins = 3, color = bright red", reply.getPayload());

        props = new HashMap<String, Object>();
        props.put(MuleProperties.MULE_USER_PROPERTY, MuleCredentials.createHeader("stan", "stan", "PBE", strategy));
        reply = client.send("vm://test", "Data2", props);
        assertNotNull(reply);
        assertEquals("user = stan, logins = 2, color = metallic blue", reply.getPayload());
    }
}
