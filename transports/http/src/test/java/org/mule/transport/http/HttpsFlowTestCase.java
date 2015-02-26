/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.http;

import static org.junit.Assert.assertEquals;
import static org.mule.api.security.tls.TlsConfiguration.DISABLE_SYSTEM_PROPERTIES_MAPPING_PROPERTY;

import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.tck.junit4.rule.SystemProperty;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.junit.Rule;
import org.junit.Test;

public class HttpsFlowTestCase extends FunctionalTestCase
{

    @Rule
    public SystemProperty disablePropertiesMapping = new SystemProperty(DISABLE_SYSTEM_PROPERTIES_MAPPING_PROPERTY, "false");

    @Rule
    public DynamicPort dynamicPort = new DynamicPort("port1");

    @Override
    protected String getConfigFile()
    {
        return "https-flow-config.xml";
    }

    @Test
    public void testSecureFlow() throws Exception
    {
        String url = String.format("https://localhost:%1d/?message=Hello", dynamicPort.getNumber());

        GetMethod method = new GetMethod(url);
        HttpClient client = new HttpClient();

        int responseCode = client.executeMethod(method);
        assertEquals(HttpConstants.SC_OK, responseCode);

        String result = method.getResponseBodyAsString();
        assertEquals("/?message=Hello received", result);
    }
}


