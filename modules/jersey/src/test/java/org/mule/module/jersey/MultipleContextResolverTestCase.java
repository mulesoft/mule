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
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.transport.http.HttpConnector;
import org.mule.transport.http.HttpConstants;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class MultipleContextResolverTestCase extends org.mule.tck.junit4.FunctionalTestCase
{

    @Rule
    public DynamicPort port = new DynamicPort("port");

    @Test
    public void multipleContextResolver() throws Exception
    {
        LocalMuleClient client =muleContext.getClient();

        Map<String, Object> props = new HashMap<String, Object>();
        props.put(HttpConnector.HTTP_METHOD_PROPERTY, HttpConstants.METHOD_GET);
        MuleMessage result = client.send("http://localhost:" + port.getNumber() +"/helloworld/sayHelloWorldWithJson", "", props);
        assertEquals((Integer)200, result.getInboundProperty(HttpConnector.HTTP_STATUS_PROPERTY, 0));
        assertEquals(getHelloWorldMessage(), result.getPayloadAsString());
    }


    private String getHelloWorldMessage () {
        return "{\"message\":\"Hello World \",\"number\":0}";
    }

    @Override
    protected String getConfigResources() {
        return "multiple-context-resolver-conf-flow.xml";
    }
}
