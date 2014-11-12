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

import org.mule.api.EncryptionStrategy;
import org.mule.api.MuleMessage;
import org.mule.api.config.MuleProperties;
import org.mule.api.security.CredentialsNotSetException;
import org.mule.api.security.CryptoFailureException;
import org.mule.security.MuleCredentials;
import org.mule.tck.AbstractServiceAndFlowTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.transport.http.HttpConnector;
import org.mule.transport.http.HttpConstants;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

public class EncryptionFunctionalTestCase extends AbstractServiceAndFlowTestCase
{
    @Rule
    public DynamicPort port1 = new DynamicPort("port1");

    public EncryptionFunctionalTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {ConfigVariant.SERVICE, "encryption-test-service.xml"},
            {ConfigVariant.FLOW, "encryption-test-flow.xml"}
        });
    }      

    @Test
    public void testAuthenticationFailureNoContext() throws Exception
    {
        MuleMessage result = muleContext.getClient().send("vm://my.queue", "foo", null);
        assertNotNull(result);
        assertNotNull(result.getExceptionPayload());
        assertEquals(CredentialsNotSetException.class, result.getExceptionPayload().getException().getClass());
    }

    @Test
    public void testAuthenticationFailureBadCredentials() throws Exception
    {
        Map<String, Object> props = createMessagePropertiesWithCredentials("anonX", "anonX");

        MuleMessage result = muleContext.getClient().send("vm://my.queue", "foo", null);
        assertNotNull(result);
        assertNotNull(result.getExceptionPayload());
        assertEquals(CredentialsNotSetException.class, result.getExceptionPayload().getException().getClass());
    }

    @Test
    public void testAuthenticationAuthorised() throws Exception
    {
        Map<String, Object> props = createMessagePropertiesWithCredentials("anon", "anon");
        MuleMessage m = muleContext.getClient().send("vm://my.queue", "foo", props);
        assertNotNull(m);
        assertNull(m.getExceptionPayload());
    }

    @Test
    public void testAuthenticationFailureBadCredentialsHttp() throws Exception
    {
        Map<String, Object> props = createMessagePropertiesWithCredentials("anonX", "anonX");
        MuleMessage m = muleContext.getClient().send(getUrl(), "", props);
        assertNotNull(m);

        int status = m.getInboundProperty(HttpConnector.HTTP_STATUS_PROPERTY, -1);
        assertEquals(HttpConstants.SC_UNAUTHORIZED, status);
    }

    @Test
    public void testAuthenticationAuthorisedHttp() throws Exception
    {
        Map<String, Object> props = createMessagePropertiesWithCredentials("anon", "anon");
        MuleMessage m = muleContext.getClient().send(getUrl(), "", props);
        assertNotNull(m);
        int status = m.getInboundProperty(HttpConnector.HTTP_STATUS_PROPERTY, -1);
        assertEquals(HttpConstants.SC_OK, status);
    }

    private Map<String, Object> createMessagePropertiesWithCredentials(String username, String password) throws CryptoFailureException
    {
        Map<String, Object> props = new HashMap<String, Object>();
        EncryptionStrategy strategy = muleContext.getSecurityManager().getEncryptionStrategy("PBE");
        String header = MuleCredentials.createHeader(username, password, "PBE", strategy);
        props.put(MuleProperties.MULE_USER_PROPERTY, header);
        return props;
    }

    private String getUrl()
    {
        return String.format("http://localhost:%s/index.html", port1.getNumber());
    }
}
