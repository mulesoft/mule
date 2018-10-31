/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.domain.tls;

import static org.junit.Assert.assertEquals;

import org.junit.Rule;
import org.junit.Test;
import org.mule.api.MuleMessage;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.tck.junit4.rule.SystemProperty;

public class TlsCustomTruststoreTestCase extends FunctionalTestCase
{

    private static final String RESPONSE = "test";

    @Rule
    public DynamicPort port = new DynamicPort("port");

    @Rule
    public DynamicPort portSsl = new DynamicPort("portSsl");

    @Rule
    public SystemProperty customTrustStoreEnabled = new SystemProperty("javax.net.ssl.trustStore", "src/test/resources/chain-cert-truststore.jks");

    @Override
    protected String getConfigFile()
    {
        return "tls/tls-clustom-truststore-config.xml";
    }

    @Test
    public void testUsingCustomTlsTrustManager() throws Exception
    {
        MuleMessage message = runFlow("flow-custom").getMessage();
        assertEquals(RESPONSE, message.getPayloadAsString());
    }

    @Test
    public void testUsingDefaultTlsTrustManager() throws Exception
    {
        MuleMessage message = runFlow("flow-default").getMessage();
        assertEquals(RESPONSE, message.getPayloadAsString());
    }
}
