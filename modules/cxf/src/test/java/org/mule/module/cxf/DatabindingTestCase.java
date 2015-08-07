/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.cxf;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assume.assumeThat;
import static org.junit.runners.Parameterized.Parameter;
import static org.junit.runners.Parameterized.Parameters;
import static org.mule.module.http.api.HttpConstants.Methods.POST;
import static org.mule.module.http.api.client.HttpRequestOptionsBuilder.newOptions;
import org.mule.api.MuleMessage;
import org.mule.module.http.api.client.HttpRequestOptions;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class DatabindingTestCase extends FunctionalTestCase
{

    private static final HttpRequestOptions HTTP_REQUEST_OPTIONS = newOptions().method(POST.name()).build();
    private static final String DATABINDING_CONF_HTTPN_XML = "databinding-conf-httpn.xml";

    @Rule
    public DynamicPort dynamicPort = new DynamicPort("port1");

    @Parameter(0)
    public String configFile;

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][] {
                {"databinding-conf.xml"},
                {DATABINDING_CONF_HTTPN_XML}
        });
    }

    @Override
    protected String getConfigFile()
    {
        return configFile;
    }

    @Test
    public void testEchoWsdl() throws Exception
    {
        assumeThat(configFile, is(not(equalTo(DATABINDING_CONF_HTTPN_XML)))); // New http module doesn't support urls like "cxf:*"
        doTest("Echo");
    }

    @Test
    public void testEchoWsdlAegisBinding() throws Exception
    {
        doTest("aegis");
    }

    @Test
    public void testEchoWsdlSourceBinding() throws Exception
    {
        doTest("source");
    }

    @Test
    public void testEchoWsdlJaxbBinding() throws Exception
    {
        doTest("jaxb");
    }

    @Test
    public void testEchoWsdlJibxBinding() throws Exception
    {
        doTest("jibx");
    }

    @Test
    public void testEchoWsdlStaxBinding() throws Exception
    {
        doTest("stax");
    }

    @Test
    public void testEchoWsdlCustomBinding() throws Exception
    {
        doTest("custom");
    }

    private void doTest(String service) throws Exception
    {
        MuleMessage result = muleContext.getClient().send(String.format("http://localhost:%d/services/%s?wsdl", dynamicPort.getNumber(), service), getTestMuleMessage(null), HTTP_REQUEST_OPTIONS);
        assertNotNull(result.getPayload());
    }
}
