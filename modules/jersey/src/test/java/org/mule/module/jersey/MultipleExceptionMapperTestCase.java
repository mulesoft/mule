/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.jersey;

import org.junit.Rule;
import org.junit.Test;
import org.mule.api.MuleMessage;
import org.mule.api.client.LocalMuleClient;
import org.mule.module.client.MuleClient;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.transport.http.HttpConnector;
import org.mule.transport.http.HttpConstants;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class MultipleExceptionMapperTestCase extends org.mule.tck.junit4.FunctionalTestCase
{

    @Rule
    public DynamicPort port = new DynamicPort("port");

    @Test
    public void testBeanBadRequestException() throws Exception
    {
        LocalMuleClient client = muleContext.getClient();
        Map<String, Object> props = new HashMap<String, Object>();
        props.put(HttpConnector.HTTP_METHOD_PROPERTY, HttpConstants.METHOD_GET);
        MuleMessage result = client.send("http://localhost:"  + port.getNumber() + "/helloworld/throwBadRequestException", "", props);
        assertEquals((Integer)400, result.getInboundProperty(HttpConnector.HTTP_STATUS_PROPERTY, 0));
    }

    @Test
    public void helloWorlException() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);

        Map<String, String> props = new HashMap<String, String>();
        props.put(HttpConnector.HTTP_METHOD_PROPERTY, HttpConstants.METHOD_GET);
        MuleMessage result = client.send("http://localhost:"  + port.getNumber() + "/helloworld/throwException", "", props);
        assertEquals( (Integer)503, result.getInboundProperty(HttpConnector.HTTP_STATUS_PROPERTY, 0));
    }


    @Override
    protected String getConfigResources() {
        return "multiple-exception-mapper-conf-flow.xml";
    }
}
