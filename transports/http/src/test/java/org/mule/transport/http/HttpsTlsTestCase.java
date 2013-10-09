/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.http;

import org.junit.Test;
import org.mule.api.MuleContext;
import org.mule.api.transport.Connector;
import org.mule.config.spring.parsers.processors.CheckExclusiveAttributes;
import org.mule.tck.junit4.FunctionalTestCase;

import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class HttpsTlsTestCase extends FunctionalTestCase
{
    @Override
    protected String getConfigResources()
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