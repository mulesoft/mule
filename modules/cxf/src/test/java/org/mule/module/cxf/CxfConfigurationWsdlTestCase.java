/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
    protected String getConfigFile()
    {
        return "cxf-configuration-wsdl-config.xml";
    }

    @Test
    public void test() throws Exception
    {
        MuleClient client = muleContext.getClient();

        MuleMessage result = client.request(String.format("http://localhost:%s?wsdl", httpPort.getNumber()), RECEIVE_TIMEOUT);

        // Don't want to compare full WSDL, just checking one tag's content
        String serviceLocation = String.format("<soap:address location=\"http://localhost:%s/\"/>", httpPort.getNumber());
        assertTrue(result.getPayloadAsString().contains(serviceLocation));
    }
}
