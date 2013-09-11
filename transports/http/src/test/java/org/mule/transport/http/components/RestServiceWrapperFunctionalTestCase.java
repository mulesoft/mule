/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.http.components;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.mule.api.MuleEventContext;
import org.mule.api.MuleMessage;
import org.mule.api.lifecycle.Callable;
import org.mule.component.ComponentException;
import org.mule.tck.AbstractServiceAndFlowTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.transport.http.HttpConstants;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

public class RestServiceWrapperFunctionalTestCase extends AbstractServiceAndFlowTestCase
{
    protected static String TEST_REQUEST = "Test Http Request";

    @Rule
    public DynamicPort port1 = new DynamicPort("port1");

    @Rule
    public DynamicPort port2 = new DynamicPort("port2");

    public RestServiceWrapperFunctionalTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
        setDisposeContextPerClass(true);
    }

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {ConfigVariant.SERVICE, "http-rest-service-wrapper-functional-test-service.xml"},
            {ConfigVariant.FLOW, "http-rest-service-wrapper-functional-test-flow.xml"}
        });
    }

    @Test
    public void testErrorExpressionOnRegexFilterFail() throws Exception
    {
        MuleMessage result = muleContext.getClient().send("restServiceEndpoint", TEST_REQUEST, null);
        assertNotNull(result);
        assertNotNull(result.getExceptionPayload());
        assertEquals(RestServiceException.class, result.getExceptionPayload().getException().getClass());
    }

    @Test
    public void testErrorExpressionOnRegexFilterPass() throws Exception
    {
        MuleMessage result = muleContext.getClient().send("restServiceEndpoint2", TEST_REQUEST, null);
        assertEquals("echo=" + TEST_REQUEST,result.getPayloadAsString());
    }

    @Test
    public void testRequiredParameters() throws Exception
    {
        Map<String, Object> props = new HashMap<String, Object>();
        props.put("baz-header", "baz");
        props.put("bar-optional-header", "bar");

        MuleMessage result = muleContext.getClient().send("restServiceEndpoint3", null, props);
        assertEquals("foo=boo&faz=baz&far=bar",result.getPayloadAsString());
    }

    @Test
    public void testOptionalParametersMissing() throws Exception
    {
        Map<String, Object> props = new HashMap<String, Object>();
        props.put("baz-header", "baz");

        MuleMessage result = muleContext.getClient().send("restServiceEndpoint3", null, props);
        assertEquals("foo=boo&faz=baz",result.getPayloadAsString());
    }

    @Test
    public void testRequiredParametersMissing() throws Exception
    {
        Map<String, Object> props = new HashMap<String, Object>();

        MuleMessage result = muleContext.getClient().send("restServiceEndpoint3", null, props);
        assertNotNull(result);
        assertNotNull(result.getExceptionPayload());
        assertEquals(ComponentException.class, result.getExceptionPayload().getException().getClass());
    }

    @Test
    public void testRestServiceComponentInFlow() throws Exception
    {
        MuleMessage result = muleContext.getClient().send("vm://toFlow", TEST_REQUEST, null);
        assertNotNull(result);
        assertEquals("echo=Test Http Request", result.getPayloadAsString());
    }

    @Test
    public void restServiceComponentShouldPreserveContentTypeOnIncomingMessage() throws Exception
    {
        MuleMessage result = muleContext.getClient().send("vm://restservice4", TEST_REQUEST, null);
        assertNotNull(result);
        assertEquals("foo/bar", result.getPayloadAsString());
    }

    public static class CopyContentTypeFromRequest implements Callable
    {
        @Override
        public Object onCall(MuleEventContext eventContext) throws Exception
        {
            return eventContext.getMessage().getInboundProperty(HttpConstants.HEADER_CONTENT_TYPE);
        }
    }
}
