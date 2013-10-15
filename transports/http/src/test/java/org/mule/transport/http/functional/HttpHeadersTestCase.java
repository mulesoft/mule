/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.http.functional;

import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.transport.http.HttpConstants;

import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class HttpHeadersTestCase extends FunctionalTestCase
{

    @Rule
    public DynamicPort dynamicPort1 = new DynamicPort("port1");

    @Rule
    public DynamicPort dynamicPort2 = new DynamicPort("port2");

    @Override
    protected String getConfigResources()
    {
        return "http-headers-config.xml";
    }

    @Test
    public void testJettyHeaders() throws Exception 
    {
        MuleClient client = new MuleClient(muleContext);
        MuleMessage result = client.send("clientEndpoint", null, null);

        String contentTypeProperty = result.getInboundProperty(HttpConstants.HEADER_CONTENT_TYPE);
        assertNotNull(contentTypeProperty); 
        assertEquals("application/x-download", contentTypeProperty);

        String contentDispositionProperty = result.getInboundProperty(HttpConstants.HEADER_CONTENT_DISPOSITION);
        assertNotNull(contentDispositionProperty);
        assertEquals("attachment; filename=foo.zip", contentDispositionProperty);
    }

}
