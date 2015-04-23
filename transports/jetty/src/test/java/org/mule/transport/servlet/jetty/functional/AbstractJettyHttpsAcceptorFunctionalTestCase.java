/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.servlet.jetty.functional;

import org.mule.module.http.api.client.HttpRequestOptionsBuilder;
import org.mule.transport.ssl.DefaultTlsContextFactory;

import java.io.IOException;

import org.junit.Before;

public abstract class AbstractJettyHttpsAcceptorFunctionalTestCase extends AbstractJettyAcceptorFunctionalTestCase
{

    private DefaultTlsContextFactory tlsContextFactory;

    @Before
    public void setup() throws IOException
    {
        tlsContextFactory = new DefaultTlsContextFactory();

        // Configure trust store in the client with the certificate of the server.
        tlsContextFactory.setTrustStorePath("trustStore");
        tlsContextFactory.setTrustStorePassword("mulepassword");
    }

    @Override
    protected HttpRequestOptionsBuilder getRequestOptionsBuilder()
    {
        return super.getRequestOptionsBuilder().tlsContextFactory(tlsContextFactory);
    }
}
