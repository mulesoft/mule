/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.domain;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import org.mule.api.MuleMessage;
import org.mule.tck.DomainFunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;

import org.junit.Rule;
import org.junit.Test;

public class HttpSamePortTestCase extends DomainFunctionalTestCase
{

    public static final String HELLO_WORLD_SERVICE_APP = "helloWorldServiceApp";
    public static final String HELLO_MULE_SERVICE_APP = "helloMuleServiceApp";
    @Rule
    public DynamicPort dynamicPort = new DynamicPort("port1");

    @Override
    protected String getDomainConfig()
    {
        return "domain/http-shared-connector.xml";
    }

    @Override
    public ApplicationConfig[] getConfigResources()
    {
        return new ApplicationConfig[] {new ApplicationConfig(HELLO_WORLD_SERVICE_APP, new String[] {"domain/http-hello-world-service-app.xml"}),
                new ApplicationConfig(HELLO_MULE_SERVICE_APP, new String[] {"domain/http-hello-mule-service-app.xml"})
        };
    }

    @Test
    public void bothServicesBindCorrectly() throws Exception
    {
        MuleMessage helloWorldServiceResponse = getMuleContextForApp(HELLO_WORLD_SERVICE_APP).getClient().send(String.format("http://localhost:%d/service/helloWorld", dynamicPort.getNumber()), "test-data", null);
        assertThat(helloWorldServiceResponse.getPayloadAsString(), is("hello world"));
        MuleMessage helloMuleServiceResponse = getMuleContextForApp(HELLO_MULE_SERVICE_APP).getClient().send(String.format("http://localhost:%d/service/helloMule", dynamicPort.getNumber()), "test-data", null);
        assertThat(helloMuleServiceResponse.getPayloadAsString(), is("hello mule"));
    }

}
