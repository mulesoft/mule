/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.acegi;

import org.mule.api.EncryptionStrategy;
import org.mule.api.MuleMessage;
import org.mule.api.config.MuleProperties;
import org.mule.api.security.CredentialsNotSetException;
import org.mule.api.security.UnauthorisedException;
import org.mule.config.ExceptionHelper;
import org.mule.module.client.MuleClient;
import org.mule.security.MuleCredentials;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.transport.http.HttpConnector;
import org.mule.transport.http.HttpConstants;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class EncryptionFunctionalTestCase extends FunctionalTestCase
{

    @Override
    protected String getConfigResources()
    {
        return "encryption-test.xml";
    }

    @Test
    public void testAuthenticationFailureNoContext() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        MuleMessage m = client.send("vm://my.queue", "foo", null);
        assertNotNull(m);
        assertNotNull(m.getExceptionPayload());
        assertEquals(ExceptionHelper.getErrorCode(CredentialsNotSetException.class), m.getExceptionPayload()
            .getCode());
    }

    @Test
    public void testAuthenticationFailureBadCredentials() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        Map props = new HashMap();
        EncryptionStrategy strategy = muleContext
            .getSecurityManager()
            .getEncryptionStrategy("PBE");
        String header = MuleCredentials.createHeader("anonX", "anonX", "PBE", strategy);
        props.put(MuleProperties.MULE_USER_PROPERTY, header);

        MuleMessage m = client.send("vm://my.queue", "foo", props);
        assertNotNull(m);
        assertNotNull(m.getExceptionPayload());
        assertEquals(ExceptionHelper.getErrorCode(UnauthorisedException.class), m.getExceptionPayload()
            .getCode());
    }

    @Test
    public void testAuthenticationAuthorised() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);

        Map props = new HashMap();
        EncryptionStrategy strategy = muleContext
            .getSecurityManager()
            .getEncryptionStrategy("PBE");
        String header = MuleCredentials.createHeader("anon", "anon", "PBE", strategy);
        props.put(MuleProperties.MULE_USER_PROPERTY, header);

        MuleMessage m = client.send("vm://my.queue", "foo", props);
        assertNotNull(m);
        assertNull(m.getExceptionPayload());
    }

    @Test
    public void testAuthenticationFailureBadCredentialsHttp() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        Map props = new HashMap();
        EncryptionStrategy strategy = muleContext
            .getSecurityManager()
            .getEncryptionStrategy("PBE");
        String header = MuleCredentials.createHeader("anonX", "anonX", "PBE", strategy);
        props.put(MuleProperties.MULE_USER_PROPERTY, header);

        MuleMessage m = client.send("http://localhost:4567/index.html", "", props);
        assertNotNull(m);

        int status = m.getInboundProperty(HttpConnector.HTTP_STATUS_PROPERTY, -1);
        assertEquals(HttpConstants.SC_UNAUTHORIZED, status);
    }

    @Test
    public void testAuthenticationAuthorisedHttp() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);

        Map props = new HashMap();
        EncryptionStrategy strategy = muleContext
            .getSecurityManager()
            .getEncryptionStrategy("PBE");
        String header = MuleCredentials.createHeader("anon", "anon", "PBE", strategy);
        props.put(MuleProperties.MULE_USER_PROPERTY, header);

        MuleMessage m = client.send("http://localhost:4567/index.html", "", props);
        assertNotNull(m);
        int status = m.getInboundProperty(HttpConnector.HTTP_STATUS_PROPERTY, -1);
        assertEquals(HttpConstants.SC_OK, status);
    }

}
