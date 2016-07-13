/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.domain.http;

import org.mule.runtime.module.http.api.client.HttpRequestOptionsBuilder;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.runtime.module.tls.internal.DefaultTlsContextFactory;

import java.io.IOException;

import org.junit.Before;
import org.junit.Ignore;

@Ignore
public class HttpsSharePortTestCase extends HttpSharePortTestCase
{

    private DefaultTlsContextFactory tlsContextFactory;

    @Before
    public void setup() throws IOException
    {
        tlsContextFactory = new DefaultTlsContextFactory();

        // Configure trust store in the client with the certificate of the server.
        tlsContextFactory.setTrustStorePath("ssltest-cacerts.jks");
        tlsContextFactory.setTrustStorePassword("changeit");
    }

    @Override
    protected String getDomainConfig()
    {
        return "domain/http/https-shared-listener-config.xml";
    }

    @Override
    public ApplicationConfig[] getConfigResources()
    {
        return new ApplicationConfig[] {new ApplicationConfig(HELLO_WORLD_SERVICE_APP, new String[] {"domain/http/http-hello-world-app.xml"}),
                new ApplicationConfig(HELLO_MULE_SERVICE_APP, new String[] {"domain/http/http-hello-mule-app.xml"})
        };
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
