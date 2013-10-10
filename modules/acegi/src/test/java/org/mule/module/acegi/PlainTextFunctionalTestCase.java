/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.acegi;

import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.transport.http.HttpConnector;
import org.mule.transport.http.HttpConstants;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class PlainTextFunctionalTestCase extends FunctionalTestCase
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
        MuleMessage m = client.send("http://localhost:4567/index.html", "", null);
        assertNotNull(m);
        int status = m.getInboundProperty(HttpConnector.HTTP_STATUS_PROPERTY, -1);
        assertEquals(HttpConstants.SC_UNAUTHORIZED, status);
    }

    @Test
    public void testAuthenticationFailureBadCredentials() throws Exception
    {
        MuleClient client = new MuleClient("anonX", "anonX");
        MuleMessage m = client.send("http://localhost:4567/index.html", "", null);
        assertNotNull(m);
        int status = m.getInboundProperty(HttpConnector.HTTP_STATUS_PROPERTY, -1);
        assertEquals(HttpConstants.SC_UNAUTHORIZED, status);
    }

    @Test
    public void testAuthenticationAuthorised() throws Exception
    {
        MuleClient client = new MuleClient("anon", "anon");
        MuleMessage m = client.send("http://localhost:4567/index.html", "", null);
        assertNotNull(m);
        int status = m.getInboundProperty(HttpConnector.HTTP_STATUS_PROPERTY, -1);
        assertEquals(HttpConstants.SC_OK, status);
    }

}
