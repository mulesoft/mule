/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.cxf.functional;

import static org.junit.Assert.assertTrue;
import static org.mule.module.http.api.HttpConstants.Methods.POST;
import static org.mule.module.http.api.client.HttpRequestOptionsBuilder.newOptions;
import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.module.cxf.example.HelloWorld;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

import javax.jws.WebService;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class UnwrapsComponentExceptionTestCase extends FunctionalTestCase
{
    public static final String ERROR_MESSAGE = "Changos!!!";

    private static final String requestPayload =
            "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\"\n" +
            "           xmlns:hi=\"http://example.cxf.module.mule.org/\">\n" +
            "<soap:Body>\n" +
            "<hi:sayHi>\n" +
            "    <arg0>Hello</arg0>\n" +
            "</hi:sayHi>\n" +
            "</soap:Body>\n" +
            "</soap:Envelope>";

    @Rule
    public DynamicPort dynamicPort = new DynamicPort("port1");

    @Parameterized.Parameter
    public String configFile;

    @Parameterized.Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][] {
                {"unwraps-component-exception-config.xml"},
                {"unwraps-component-exception-config-httpn.xml"}
        });
    }

    @Override
    protected String getConfigFile()
    {
        return configFile;
    }

    @Test
    public void testReceivesComponentExceptionMessage() throws Exception
    {
        MuleMessage request = new DefaultMuleMessage(requestPayload, (Map<String, Object>) null, muleContext);

        MuleMessage received = muleContext.getClient().send("http://localhost:" + dynamicPort.getNumber() + "/hello", request, newOptions().method(POST.name()).disableStatusCodeValidation().build());

        assertTrue("Component exception was not managed", received.getPayloadAsString().contains(ERROR_MESSAGE));
    }

    @WebService(endpointInterface = "org.mule.module.cxf.example.HelloWorld", serviceName = "HelloWorld")
    public static class HelloWorldImpl implements HelloWorld
    {

        public String sayHi(String text)
        {
            throw new RuntimeException(ERROR_MESSAGE);
        }
    }
}
