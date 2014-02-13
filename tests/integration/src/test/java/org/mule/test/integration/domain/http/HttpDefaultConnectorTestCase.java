/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.domain.http;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import org.mule.api.MuleMessage;
import org.mule.tck.junit4.DomainFunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.tck.junit4.rule.SystemProperty;

import org.junit.Rule;
import org.junit.Test;

public class HttpDefaultConnectorTestCase extends DomainFunctionalTestCase
{

    public static final String HELLO_WORLD_SERVICE_APP = "helloWorldServiceApp";
    public static final String HELLO_MULE_SERVICE_APP = "helloMuleServiceApp";

    @Rule
    public SystemProperty endpointScheme = getEndpointSchemeSystemProperty();

    @Rule
    public DynamicPort dynamicPort = new DynamicPort("port1");

    @Override
    protected String getDomainConfig()
    {
        return "domain/empty-domain-config.xml";
    }

    @Override
    public ApplicationConfig[] getConfigResources()
    {
        return new ApplicationConfig[] {new ApplicationConfig(HELLO_WORLD_SERVICE_APP, new String[] {"domain/http/http-hello-world-app-no-connector.xml"}),
                new ApplicationConfig(HELLO_MULE_SERVICE_APP, new String[] {"domain/http/http-hello-mule-app-no-connector.xml"})
        };
    }

    @Test
    public void bothServicesBindCorrectly() throws Exception
    {
        MuleMessage helloWorldServiceResponse = getMuleContextForApp(HELLO_WORLD_SERVICE_APP).getClient().send(String.format("%s://localhost:%d/service/helloWorld", endpointScheme.getValue(), dynamicPort.getNumber()), "test-data", null);
        assertThat(helloWorldServiceResponse.getPayloadAsString(), is("hello world"));
        MuleMessage helloMuleServiceResponse = getMuleContextForApp(HELLO_MULE_SERVICE_APP).getClient().send(String.format("%s://localhost:%d/service/helloMule", endpointScheme.getValue(), dynamicPort.getNumber()), "test-data", null);
        assertThat(helloMuleServiceResponse.getPayloadAsString(), is("hello mule"));
    }

    protected SystemProperty getEndpointSchemeSystemProperty()
    {
        return new SystemProperty("scheme", "http");
    }

}
