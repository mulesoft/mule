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
import org.mule.tck.DynamicPortTestCase;

import java.util.HashMap;
import java.util.Map;

public class RestServiceWrapperFunctionalTestCase extends DynamicPortTestCase
{
    protected static String TEST_REQUEST = "Test Http Request";

    @Override
    protected String getConfigResources()
    {
        return "http-rest-service-wrapper-functional-test.xml";
    }

    public void testErrorExpressionOnRegexFilterFail() throws Exception
    {
        try
        {
            muleContext.getClient().send("restServiceEndpoint", TEST_REQUEST, null);
            fail("Exception expected");
        }
        catch (Exception e)
        {
            // expected
        }
    }

    public void testErrorExpressionOnRegexFilterPass() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        MuleMessage result = client.send("restServiceEndpoint2", TEST_REQUEST, null);
        assertEquals("echo=" + TEST_REQUEST,result.getPayloadAsString());
    }

    public void testRequiredParameters() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        Map<String, Object> props = new HashMap<String, Object>();
        props.put("baz-header", "baz");
        props.put("bar-optional-header", "bar");
        MuleMessage result = client.send("restServiceEndpoint3", null, props);
        assertEquals("foo=boo&faz=baz&far=bar",result.getPayloadAsString());
    }

    public void testOptionalParametersMissing() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        Map<String, Object> props = new HashMap<String, Object>();
        props.put("baz-header", "baz");
        MuleMessage result = client.send("restServiceEndpoint3", null, props);
        assertEquals("foo=boo&faz=baz",result.getPayloadAsString());
    }

    public void testRequiredParametersMissing() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        Map<String, Object> props = new HashMap<String, Object>();

        try
        {
            muleContext.getClient().send("restServiceEndpoint3", null, props);
            fail("Exception expected");
        }
        catch (Exception e)
        {
            // expected
        }
    }

    public void testRestServiceComponentInFlow() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        
        MuleMessage result = client.send("vm://toFlow", TEST_REQUEST, null);
        assertNotNull(result);
        assertEquals("echo=Test Http Request", result.getPayloadAsString());
    }
    
    @Override
    protected int getNumPortsToFind()
    {
        return 1;
    }
}
