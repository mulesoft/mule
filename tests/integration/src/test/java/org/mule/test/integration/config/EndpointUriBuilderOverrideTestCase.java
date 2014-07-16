/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.config;

import static org.junit.Assert.assertTrue;

import org.mule.api.MuleContext;
import org.mule.api.client.LocalMuleClient;
import org.mule.api.endpoint.EndpointURI;
import org.mule.api.endpoint.MalformedEndpointException;
import org.mule.endpoint.UrlEndpointURIBuilder;
import org.mule.tck.junit4.FunctionalTestCase;

import java.net.URI;

import org.junit.Test;

public class EndpointUriBuilderOverrideTestCase extends FunctionalTestCase
{
    private static boolean invokedOverriddenUriBuilder;

    @Override
    protected String getConfigFile()
    {
        return "org/mule/test/integration/config/endpoint-uri-builder-service-override-config.xml";
    }

    @Test
    public void usesServiceOverrideInEndpoint() throws Exception
    {
        LocalMuleClient client = muleContext.getClient();

        client.send("vm://testIn", TEST_MESSAGE, null);

        assertTrue("Connector service overrides were not applied to endpoint", invokedOverriddenUriBuilder);
    }

    public static class TestUriBuilder extends UrlEndpointURIBuilder
    {

        @Override
        public EndpointURI build(URI uri, MuleContext muleContext) throws MalformedEndpointException
        {

            EndpointURI build = super.build(uri, muleContext);
            invokedOverriddenUriBuilder = true;
            return build;
        }
    }
}
