/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.domain.http.transport;

import org.mule.module.http.api.client.HttpRequestOptionsBuilder;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.test.integration.domain.http.HttpSharePortTestCase;
import org.mule.transport.ssl.DefaultTlsContextFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

import org.junit.Before;
import org.junit.runners.Parameterized;

public class HttpsSharePortTestCase extends HttpSharePortTestCase
{

    private DefaultTlsContextFactory tlsContextFactory;

    public HttpsSharePortTestCase(String domainConfig, String helloWorldAppConfig, String helloMuleAppConfig)
    {
        super(domainConfig, helloWorldAppConfig, helloMuleAppConfig);
    }

    @Before
    public void setup() throws IOException
    {
        tlsContextFactory = new DefaultTlsContextFactory();

        // Configure trust store in the client with the certificate of the server.
        tlsContextFactory.setTrustStorePath("trustStore");
        tlsContextFactory.setTrustStorePassword("mulepassword");
    }

    @Parameterized.Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][] {
                {"domain/http/transport/https-shared-connector.xml", "domain/http/transport/http-hello-world-app.xml", "domain/http/transport/http-hello-mule-app.xml"},
                {"domain/http/https-shared-listener-config.xml", "domain/http/http-hello-world-app.xml", "domain/http/http-hello-mule-app.xml"}});
    }

    @Override
    protected SystemProperty getEndpointSchemeSystemProperty()
    {
        return new SystemProperty("scheme", "https");
    }

    @Override
    protected HttpRequestOptionsBuilder getOptionsBuilder()
    {
        return super.getOptionsBuilder().tlsContextFactory(tlsContextFactory);
    }
}
