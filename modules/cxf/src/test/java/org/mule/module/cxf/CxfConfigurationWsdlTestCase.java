/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.cxf;

import static org.junit.Assert.assertTrue;
import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.tck.junit4.rule.DynamicPort;

import org.junit.Rule;
import org.junit.Test;

public class CxfConfigurationWsdlTestCase extends org.mule.tck.junit4.FunctionalTestCase
{

    @Rule
    public DynamicPort httpPort = new DynamicPort("httpPort");

    @Override
    protected String getConfigResources()
    {
        return "cxf-configuration-wsdl-config.xml";
    }

    @Test
    public void test() throws Exception {
        MuleClient client = muleContext.getClient();

        MuleMessage result = client.request(String.format("http://localhost:%s?wsdl", httpPort.getNumber()), RECEIVE_TIMEOUT);

        // Don't want to compare full WSDL, just checking one tag's content
        String serviceLocation = String.format("<soap:address location=\"http://localhost:%s/\"/>", httpPort.getNumber());
        assertTrue(result.getPayloadAsString().contains(serviceLocation));
    }
}
