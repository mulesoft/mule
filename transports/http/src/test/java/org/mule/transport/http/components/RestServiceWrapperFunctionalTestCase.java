/*
 * $Id: RestServiceWrapperFunctionalTestCase.java 10538 2008-01-25 14:43:19Z marie.rizzo $
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.http.components;

import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.FunctionalTestCase;
import org.mule.transport.NullPayload;

import java.util.HashMap;
import java.util.Map;

public class RestServiceWrapperFunctionalTestCase extends FunctionalTestCase
{
    protected static String TEST_MESSAGE = "Test Http Request";

    protected String getConfigResources()
    {
        return "/http-rest-service-wrapper-functional-test.xml";
    }

    public void testErrorExpressionOnRegexFilterFail() throws Exception
    {
        MuleClient client = new MuleClient();
        MuleMessage result = client.send("restServiceEndpoint", TEST_MESSAGE, null);
        assertTrue(result.getPayload() instanceof NullPayload);
    }

    public void testErrorExpressionOnRegexFilterPass() throws Exception
    {
        MuleClient client = new MuleClient();
        MuleMessage result = client.send("restServiceEndpoint2", TEST_MESSAGE, null);
        assertEquals("echo=" + TEST_MESSAGE,result.getPayloadAsString());
    }

    public void testRequiredParameters() throws Exception
    {
        MuleClient client = new MuleClient();
        Map props = new HashMap();
        props.put("baz-header", "baz");
        props.put("bar-optional-header", "bar");
        MuleMessage result = client.send("restServiceEndpoint3", null, props);
        assertEquals("foo=boo&faz=baz&far=bar",result.getPayloadAsString());
    }

    public void testOptionalParametersMissing() throws Exception
    {
        MuleClient client = new MuleClient();
        Map props = new HashMap();
        props.put("baz-header", "baz");
        MuleMessage result = client.send("restServiceEndpoint3", null, props);
        assertEquals("foo=boo&faz=baz",result.getPayloadAsString());
    }

    public void testRequiredParametersMissing() throws Exception
    {
        MuleClient client = new MuleClient();
        Map props = new HashMap();

        MuleMessage result = client.send("restServiceEndpoint3", null, props);
        assertEquals(NullPayload.getInstance(),result.getPayload());
        assertNotNull(result.getExceptionPayload());
    }
}