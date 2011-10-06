/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.servlet.jetty;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.client.DefaultLocalMuleClient;
import org.mule.transport.http.HttpConnector;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class WebappsDefaultsTestCase extends AbstractWebappsTestCase
{
    @Override
    protected String getConfigResources()
    {
        return "jetty-webapps-with-defaults.xml";
    }

    @Test
    public void testWebappsDefaults() throws Exception
    {
        Map<String,Object> props = new HashMap<String,Object>();
        props.put(HttpConnector.HTTP_METHOD_PROPERTY, "GET");

        DefaultLocalMuleClient client = new DefaultLocalMuleClient(muleContext);
        MuleMessage result = client.send("http://localhost:8585/test/hello",
            new DefaultMuleMessage("", muleContext),
            props);

        assertEquals("Hello", result.getPayloadAsString());
    }
}
