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

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;
import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.AbstractServiceAndFlowTestCase;
import org.mule.transport.http.HttpConnector;
import org.mule.transport.http.HttpConstants;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class MultipleExceptionMapperTestCase extends AbstractServiceAndFlowTestCase
{

    public MultipleExceptionMapperTestCase(ConfigVariant variant, String configResources) {
        super(variant, configResources);
    }

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
                {ConfigVariant.SERVICE, "multiple-exception-mapper-conf-flow.xml"},
        });
    }

    @Test
    @Ignore
    public void testBeanBadRequestException() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        Map<String, String> props = new HashMap<String, String>();
        props.put(HttpConnector.HTTP_METHOD_PROPERTY, HttpConstants.METHOD_GET);
        MuleMessage result = client.send("http://localhost:63081/helloworld/throwBadRequestException", "", props);
        assertEquals((Integer)400, result.getInboundProperty(HttpConnector.HTTP_STATUS_PROPERTY, 0));

    }

    @Test
    @Ignore
    public void testHelloWorlException() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);

        Map<String, String> props = new HashMap<String, String>();
        props.put(HttpConnector.HTTP_METHOD_PROPERTY, HttpConstants.METHOD_GET);
        MuleMessage result = client.send("http://localhost:63081/helloworld/throwException", "", props);
        assertEquals( (Integer)503, result.getInboundProperty(HttpConnector.HTTP_STATUS_PROPERTY, 0));

    }


}
