/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.spring.security;

import org.mule.api.EncryptionStrategy;
import org.mule.api.MuleMessage;
import org.mule.api.config.MuleProperties;
import org.mule.api.security.CredentialsNotSetException;
import org.mule.module.client.MuleClient;
import org.mule.security.MuleCredentials;
import org.mule.tck.FunctionalTestCase;
import org.mule.transport.http.HttpConnector;
import org.mule.transport.http.HttpConstants;
import org.mule.util.ExceptionUtils;

import java.util.HashMap;
import java.util.Map;

import org.springframework.security.authentication.BadCredentialsException;

public class EncryptionFunctionalTestCase extends FunctionalTestCase
{

    protected String getConfigResources()
    {
        return "encryption-test.xml";
    }

    public void testAuthenticationFailureNoContext() throws Exception
    {
        try
        {
            muleContext.getClient().send("vm://my.queue", "foo", null);
            fail("Exception expected");
        }
        catch (Exception e)
        {
            assertTrue(ExceptionUtils.getRootCause(e) instanceof CredentialsNotSetException);
        }
    }

    public void testAuthenticationFailureBadCredentials() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        Map props = new HashMap();
        EncryptionStrategy strategy = muleContext
            .getSecurityManager()
            .getEncryptionStrategy("PBE");
        String header = MuleCredentials.createHeader("anonX", "anonX", "PBE", strategy);
        props.put(MuleProperties.MULE_USER_PROPERTY, header);

        try
        {
            muleContext.getClient().send("vm://my.queue", "foo", props);
            fail("Exception expected");
        }
        catch (Exception e)
        {
            assertTrue(ExceptionUtils.getRootCause(e) instanceof BadCredentialsException);
        }
    }

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
