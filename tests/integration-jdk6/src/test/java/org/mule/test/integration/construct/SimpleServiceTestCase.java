/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.integration.construct;

import org.mule.api.MuleMessage;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.transport.http.HttpConnector;
import org.mule.transport.http.HttpConstants;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class SimpleServiceTestCase extends FunctionalTestCase
{
    @Override
    protected void doSetUp() throws Exception
    {
        setDisposeContextPerClass(true);
        super.doSetUp();
    }

    @Override
    protected String getConfigResources()
    {
        return "org/mule/test/integration/construct/simple-service-config.xml";
    }

    @Test
    public void testJaxRsService() throws Exception
    {
        final Map<String, Object> props = new HashMap<String, Object>();
        props.put(HttpConnector.HTTP_METHOD_PROPERTY, HttpConstants.METHOD_POST);
        props.put(HttpConstants.HEADER_CONTENT_TYPE, "application/xml");
        MuleMessage result = muleContext.getClient().send("http://localhost:6099/rest/weather-report",
            "<fake_report/>", props);
        assertEquals((Integer) 201, result.getInboundProperty(HttpConnector.HTTP_STATUS_PROPERTY, 0));
    }
}


