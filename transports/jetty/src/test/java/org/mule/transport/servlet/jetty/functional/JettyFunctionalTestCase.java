/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.servlet.jetty.functional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.transformer.DataType;
import org.mule.module.client.MuleClient;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.util.IOUtils;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;

/**
 * Functional tests specific to Jetty.
 */
public class JettyFunctionalTestCase extends FunctionalTestCase
{

    @Rule
    public DynamicPort dynamicPort = new DynamicPort("port1");

    @Override
    protected String getConfigResources()
    {
        return "jetty-functional-test.xml";
    }

    @Test
    public void testNormalExecutionFlow() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        MuleMessage response = client.send("http://localhost:" + dynamicPort.getNumber() + "/normal", TEST_MESSAGE, null);
        assertEquals("200", response.getInboundProperty("http.status"));
        assertEquals(TEST_MESSAGE + " received", IOUtils.toString((InputStream) response.getPayload()));
    }
    
    @Test
    public void testExceptionExecutionFlow() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        MuleMessage response = client.send("http://localhost:" + dynamicPort.getNumber() + "/exception", TEST_MESSAGE, null);
        assertEquals("500", response.getInboundProperty("http.status"));
        assertNotNull(response.getExceptionPayload());
    }

}
