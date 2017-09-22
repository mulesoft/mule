/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.functional.requester;

import static java.lang.String.format;
import static org.mule.module.http.api.HttpConstants.Protocols.HTTPS;
import static org.mule.module.http.api.client.HttpRequestOptionsBuilder.newOptions;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.equalTo;

import org.junit.Rule;
import org.junit.Test;
import org.mule.api.MuleMessage;
import org.mule.module.http.api.client.HttpRequestOptions;
import org.mule.module.http.api.requester.HttpRequesterConfig;
import org.mule.module.http.api.requester.HttpRequesterConfigBuilder;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.transport.NullPayload;
import org.mule.transport.ssl.api.TlsContextFactory;

/**
 * Test to verify that the inbound properties can be serialized through Jackson (or through any other serialization
 * method which is based on getters).
 */
public class HttpsInboundPropertiesSerializationThroughJackson extends FunctionalTestCase
{

    private static final String RESPONSE = "response";
    @Rule
    public DynamicPort httpsPort = new DynamicPort("httpsPort");

    @Override
    protected String getConfigFile()
    {
        return "http-request-serialization-inbound-properties-https.xml";
    }

    /**
     * In case every inbound property is serialized without raising an exception, then the test will succeed.
     */
    @Test
    public void inboundPropertiesCanBeSerializedInHttpsRequest() throws Exception
    {
        final MuleMessage message = getTestMuleMessage(NullPayload.class);
        HttpRequesterConfig requestConfig = null;
        try
        {
            requestConfig = new HttpRequesterConfigBuilder(muleContext).setProtocol(HTTPS).setTlsContext(muleContext.getRegistry().<TlsContextFactory> get("tlsContext")).build();
            requestConfig.start();
            final HttpRequestOptions options = newOptions().disableStatusCodeValidation().requestConfig(requestConfig).build();
            final MuleMessage response = muleContext.getClient().send(format("https://localhost:%s/", httpsPort.getNumber()), message, options);
            assertThat(response.getPayloadAsString(), equalTo(RESPONSE));
        }
        finally
        {
            if (requestConfig != null)
            {
                requestConfig.stop();
            }
        }
    }
}