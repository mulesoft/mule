/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
    protected String getConfigResources()
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
