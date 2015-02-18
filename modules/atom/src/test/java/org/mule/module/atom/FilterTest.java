/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.atom;

import static org.junit.Assert.assertEquals;
import static org.mule.module.http.api.client.HttpRequestOptionsBuilder.newOptions;
import org.mule.api.MuleMessage;
import org.mule.api.client.LocalMuleClient;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.transport.http.HttpConstants;

import org.junit.Test;

public class FilterTest extends FunctionalTestCase
{

    @Override
    protected String getConfigFile()
    {
        return "filter-conf.xml";
    }

    @Test
    public void testAcceptFilter() throws Exception
    {
        LocalMuleClient client = muleContext.getClient();

        MuleMessage result;
        result = client.send("http://localhost:9002/baz", getTestMuleMessage(),
                             newOptions().method(org.mule.module.http.api.HttpConstants.Methods.POST.name()).build());
        assertEquals("test received", result.getPayloadAsString());
    }

    @Test
    public void testUnAcceptFilter() throws Exception
    {
        LocalMuleClient client = muleContext.getClient();

        MuleMessage result = client.send("http://localhost:9002/baz", getTestMuleMessage(),
                                         newOptions().disableStatusCodeValidation().method(org.mule.module.http.api.HttpConstants.Methods.HEAD.name()).build());
        //assertEquals(new Integer(0), result.getInboundProperty(HttpConstants.HEADER_CONTENT_LENGTH, new Integer(-1)));
        assertEquals(new Integer(HttpConstants.SC_NOT_ACCEPTABLE), result.getInboundProperty(org.mule.module.http.api.HttpConstants.ResponseProperties.HTTP_STATUS_PROPERTY, new Integer(-1)));

        result = client.send("http://localhost:9002/quo", getTestMuleMessage(), newOptions().disableStatusCodeValidation().method(org.mule.module.http.api.HttpConstants.Methods.POST.name()).build());
        //assertEquals(new Integer(0), result.getInboundProperty(HttpConstants.HEADER_CONTENT_LENGTH, new Integer(-1)));
        assertEquals(new Integer(HttpConstants.SC_NOT_ACCEPTABLE), result.getInboundProperty(org.mule.module.http.api.HttpConstants.ResponseProperties.HTTP_STATUS_PROPERTY, new Integer(-1)));
    }

}
