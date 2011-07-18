/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.http.components;

import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.transport.NullPayload;

import java.util.HashMap;
import java.util.Map;

import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class RestServiceWrapperFunctionalTestCase extends FunctionalTestCase
{
    protected static String TEST_REQUEST = "Test Http Request";

    @Rule
    public DynamicPort dynamicPort = new DynamicPort("port1");

    @Override
    protected String getConfigResources()
    {
        return "http-rest-service-wrapper-functional-test.xml";
    }

    @Test
    public void testErrorExpressionOnRegexFilterFail() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        MuleMessage result = client.send("restServiceEndpoint", TEST_REQUEST, null);
        assertTrue(result.getPayload() instanceof NullPayload);
    }

    @Test
    public void testErrorExpressionOnRegexFilterPass() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        MuleMessage result = client.send("restServiceEndpoint2", TEST_REQUEST, null);
        assertEquals("echo=" + TEST_REQUEST,result.getPayloadAsString());
    }

    @Test
    public void testRequiredParameters() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        Map<String, Object> props = new HashMap<String, Object>();
        props.put("baz-header", "baz");
        props.put("bar-optional-header", "bar");
        MuleMessage result = client.send("restServiceEndpoint3", null, props);
        assertEquals("foo=boo&faz=baz&far=bar",result.getPayloadAsString());
    }

    @Test
    public void testOptionalParametersMissing() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        Map<String, Object> props = new HashMap<String, Object>();
        props.put("baz-header", "baz");
        MuleMessage result = client.send("restServiceEndpoint3", null, props);
        assertEquals("foo=boo&faz=baz",result.getPayloadAsString());
    }

    @Test
    public void testRequiredParametersMissing() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        Map<String, Object> props = new HashMap<String, Object>();

        MuleMessage result = client.send("restServiceEndpoint3", null, props);
        assertEquals(NullPayload.getInstance(),result.getPayload());
        assertNotNull(result.getExceptionPayload());
    }

    @Test
    public void testRestServiceComponentInFlow() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        
        MuleMessage result = client.send("vm://toFlow", TEST_REQUEST, null);
        assertNotNull(result);
        assertEquals("echo=Test Http Request", result.getPayloadAsString());
    }
    
}
