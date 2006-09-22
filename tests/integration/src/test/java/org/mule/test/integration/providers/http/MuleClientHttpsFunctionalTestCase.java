/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.integration.providers.http;

import org.mule.extras.client.MuleClient;
import org.mule.impl.endpoint.MuleEndpointURI;
import org.mule.providers.http.HttpsConnector;
import org.mule.tck.functional.AbstractProviderFunctionalTestCase;
import org.mule.umo.UMOMessage;
import org.mule.umo.endpoint.MalformedEndpointException;
import org.mule.umo.endpoint.UMOEndpointURI;
import org.mule.umo.provider.UMOConnector;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class MuleClientHttpsFunctionalTestCase extends AbstractProviderFunctionalTestCase
{
    protected static final String TEST_MESSAGE = "Test Https Request";

    private List results = new ArrayList();

    protected UMOEndpointURI getInDest()
    {
        try {
            return new MuleEndpointURI("https://localhost:50198");
        } catch (MalformedEndpointException e) {
            fail(e.getMessage());
            return null;
        }
    }

    protected UMOEndpointURI getOutDest()
    {
        return null;
    }

    protected UMOConnector createConnector() throws Exception
    {
        HttpsConnector connector = new HttpsConnector();
        connector.setName("testHttps");
        connector.getDispatcherThreadingProfile().setDoThreading(false);
        connector.setKeyStore("muletest.keystore");
        connector.setStorePassword("password");
        connector.setKeyPassword("keypassword");
        connector.setTrustStorePassword("password");
        connector.setTrustStore("muletest.truststore");
        return connector;
    }

    protected void sendTestData(int iterations) throws Exception
    {
        MuleClient client = new MuleClient();
        for (int i = 0; i < iterations; i++) {
            UMOMessage m = client.send(getInDest().toString(), TEST_MESSAGE + i, null);
            assertNotNull(m);
            results.add(m.getPayload());
        }
    }

    protected void receiveAndTestResults() throws Exception
    {
        int i=0;
        for (Iterator iterator = results.iterator(); iterator.hasNext();i++) {
            byte[] result = (byte[]) iterator.next();
            assertNotNull(result);
            assertEquals(TEST_MESSAGE + i + " Received", new String(result));
        }
    }
}
