/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.domain.http;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mule.module.http.api.client.HttpRequestOptionsBuilder.newOptions;
import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.module.http.api.client.HttpRequestOptionsBuilder;
import org.mule.tck.junit4.DomainFunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.tck.junit4.rule.SystemProperty;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class HttpSharePortTestCase extends DomainFunctionalTestCase
{

    public static final String HELLO_WORLD_SERVICE_APP = "helloWorldServiceApp";
    public static final String HELLO_MULE_SERVICE_APP = "helloMuleServiceApp";

    @Rule
    public DynamicPort dynamicPort = new DynamicPort("port1");
    @Rule
    public SystemProperty endpointScheme = getEndpointSchemeSystemProperty();
    private String hellowWordAppConfig;
    private String helloMuleAppConfig;
    private String domainConfig;

    public HttpSharePortTestCase(String domainConfig, String helloWorldAppConfig, String helloMuleAppConfig)
    {
        this.domainConfig = domainConfig;
        this.hellowWordAppConfig = helloWorldAppConfig;
        this.helloMuleAppConfig = helloMuleAppConfig;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][] {
                {"domain/http/transport/http-shared-connector.xml", "domain/http/transport/http-hello-world-app.xml", "domain/http/transport/http-hello-mule-app.xml"},
                {"domain/http/http-shared-listener-config.xml", "domain/http/http-hello-world-app.xml", "domain/http/http-hello-mule-app.xml"}});
    }

    @Override
    protected String getDomainConfig()
    {
        return domainConfig;
    }

    @Override
    public ApplicationConfig[] getConfigResources()
    {
        return new ApplicationConfig[] {new ApplicationConfig(HELLO_WORLD_SERVICE_APP, new String[] {hellowWordAppConfig}),
                new ApplicationConfig(HELLO_MULE_SERVICE_APP, new String[] {helloMuleAppConfig})
        };
    }

    @Test
    public void bothServicesBindCorrectly() throws Exception
    {
        MuleMessage helloWorldServiceResponse = getMuleContextForApp(HELLO_WORLD_SERVICE_APP).getClient().send(String.format("%s://localhost:%d/service/helloWorld",
                                 endpointScheme.getValue(), dynamicPort.getNumber()), new DefaultMuleMessage("test-data", getMuleContextForApp(HELLO_WORLD_SERVICE_APP)), getOptionsBuilder().build());
        assertThat(helloWorldServiceResponse.getPayloadAsString(), is("hello world"));
        MuleMessage helloMuleServiceResponse = getMuleContextForApp(HELLO_MULE_SERVICE_APP).getClient().send(String.format("%s://localhost:%d/service/helloMule",
                                 endpointScheme.getValue(), dynamicPort.getNumber()), new DefaultMuleMessage("test-data", getMuleContextForApp(HELLO_MULE_SERVICE_APP)), getOptionsBuilder().build());
        assertThat(helloMuleServiceResponse.getPayloadAsString(), is("hello mule"));
    }

    protected SystemProperty getEndpointSchemeSystemProperty()
    {
        return new SystemProperty("scheme", "http");
    }

    protected HttpRequestOptionsBuilder getOptionsBuilder()
    {
        return newOptions();
    }

}
