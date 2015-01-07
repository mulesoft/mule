/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.cxf;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static org.mule.module.http.api.client.HttpRequestOptionsBuilder.newOptions;
import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.module.http.api.HttpConstants;
import org.mule.tck.junit4.rule.DynamicPort;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class CxfConfigurationWsdlTestCase extends org.mule.tck.junit4.FunctionalTestCase
{
    @Rule
    public DynamicPort httpPort = new DynamicPort("httpPort");

    @Parameter
    public String config;

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][] {
                {"cxf-configuration-wsdl-config.xml"},
                {"cxf-configuration-wsdl-config-httpn.xml"}
        });
    }

    @Override
    protected String getConfigFile()
    {
        return config;
    }

    @Test
    public void test() throws Exception
    {
        MuleClient client = muleContext.getClient();

        MuleMessage result = client.send(String.format("http://localhost:%s?wsdl", httpPort.getNumber()), new DefaultMuleMessage(null, muleContext), newOptions().method(HttpConstants.Methods.POST.name()).build());

        // Don't want to compare full WSDL, just checking one tag's content
        String serviceLocation = String.format("<soap:address location=\"http://localhost:%s/\"/>", httpPort.getNumber());
        assertThat(result.getPayloadAsString(), containsString(serviceLocation));
    }
}
