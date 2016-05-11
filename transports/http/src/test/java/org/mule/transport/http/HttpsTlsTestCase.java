/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.http;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.mule.api.transport.Connector;
import org.mule.tck.junit4.FunctionalTestCase;

import org.junit.Test;

public class HttpsTlsTestCase extends FunctionalTestCase
{
    @Override
    protected String getConfigFile()
    {
        return "https-tls-config.xml";
    }

    @Test
    public void testConfig() throws Exception
    {
        Connector connector = muleContext.getRegistry().lookupConnector("httpsConnector");
        assertNotNull(connector);
        assertTrue(connector instanceof HttpsConnector);
        HttpsConnector https = (HttpsConnector) connector;
        assertEquals("jks", https.getClientKeyStoreType());
        assertEquals("JkS", https.getKeyStoreType());
        assertEquals("JKS", https.getTrustStoreType());
    }
}