/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.extras.acegi.broken;

import org.mule.extras.client.MuleClient;
import org.mule.providers.http.HttpConnector;
import org.mule.providers.http.HttpConstants;
import org.mule.tck.FunctionalTestCase;
import org.mule.umo.UMOMessage;

public class MuleEndpointPlainTextFilterTestCase extends FunctionalTestCase
{
    protected String getConfigResources()
    {
        return "../../../../../../resources/broken/test-acegi-encrypt-config.xml";
    }

    public void testAuthenticationFailureNoContext() throws Exception
    {
        MuleClient client = new MuleClient();
        UMOMessage m = client.send("http://localhost:4567/index.html", "", null);
        assertNotNull(m);
        int status = m.getIntProperty(HttpConnector.HTTP_STATUS_PROPERTY, -1);
        assertEquals(HttpConstants.SC_UNAUTHORIZED, status);
    }

    public void testAuthenticationFailureBadCredentials() throws Exception
    {
        MuleClient client = new MuleClient("anonX", "anonX");
        UMOMessage m = client.send("http://localhost:4567/index.html", "", null);
        assertNotNull(m);
        int status = m.getIntProperty(HttpConnector.HTTP_STATUS_PROPERTY, -1);
        assertEquals(HttpConstants.SC_UNAUTHORIZED, status);
    }

    public void testAuthenticationAuthorised() throws Exception
    {
        MuleClient client = new MuleClient("anon", "anon");
        UMOMessage m = client.send("http://localhost:4567/index.html", "", null);
        assertNotNull(m);
        int status = m.getIntProperty(HttpConnector.HTTP_STATUS_PROPERTY, -1);
        assertEquals(HttpConstants.SC_OK, status);
    }

}
