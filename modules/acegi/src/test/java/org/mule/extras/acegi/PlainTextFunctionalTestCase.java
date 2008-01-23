/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.extras.acegi;

import org.mule.api.MuleMessage;
import org.mule.extras.client.MuleClient;
import org.mule.tck.FunctionalTestCase;
import org.mule.transport.http.HttpConnector;
import org.mule.transport.http.HttpConstants;

public class PlainTextFunctionalTestCase extends FunctionalTestCase
{
    protected String getConfigResources()
    {
        return "encryption-test.xml";
    }

    public void testAuthenticationFailureNoContext() throws Exception
    {
        MuleClient client = new MuleClient();
        MuleMessage m = client.send("http://localhost:4567/index.html", "", null);
        assertNotNull(m);
        int status = m.getIntProperty(HttpConnector.HTTP_STATUS_PROPERTY, -1);
        assertEquals(HttpConstants.SC_UNAUTHORIZED, status);
    }

    public void testAuthenticationFailureBadCredentials() throws Exception
    {
        MuleClient client = new MuleClient("anonX", "anonX");
        MuleMessage m = client.send("http://localhost:4567/index.html", "", null);
        assertNotNull(m);
        int status = m.getIntProperty(HttpConnector.HTTP_STATUS_PROPERTY, -1);
        assertEquals(HttpConstants.SC_UNAUTHORIZED, status);
    }

    public void testAuthenticationAuthorised() throws Exception
    {
        MuleClient client = new MuleClient("anon", "anon");
        MuleMessage m = client.send("http://localhost:4567/index.html", "", null);
        assertNotNull(m);
        int status = m.getIntProperty(HttpConnector.HTTP_STATUS_PROPERTY, -1);
        assertEquals(HttpConstants.SC_OK, status);
    }

}
