/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.cxf.functional;


import static org.junit.Assert.assertEquals;
import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;

import org.junit.Rule;
import org.junit.Test;

public class CxfJaxWsServiceAndClientTestCase extends FunctionalTestCase
{
    @Rule
    public DynamicPort port = new DynamicPort("port");

    @Override
    protected String getConfigFile()
    {
        return "cxf-jaxws-service-and-client-config.xml";
    }

    @Test
    public void jaxWsClientReadsMuleMethodPropertySetByJaxWsService() throws Exception
    {
        String url = "cxf:http://localhost:" + port.getNumber() + "/hello?method=sayHi";
        MuleClient client = muleContext.getClient();

        MuleMessage result = client.send(url, TEST_MESSAGE, null);

        assertEquals("Hello\u2297 " + TEST_MESSAGE, result.getPayloadAsString());
    }
}
