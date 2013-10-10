/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.ibeans.annotations;

import org.mule.api.MuleMessage;

import org.ibeans.annotation.IntegrationBean;
import org.ibeans.api.Response;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class ParamFactoryTestCase extends AbstractIBeansTestCase
{
    @IntegrationBean
    private TestParamsFactoryIBean testIBean;

    @Test
    public void testUriParamsOnMethod() throws Exception
    {
        testIBean.init("shhh".getBytes());

        String result = testIBean.doMethodUriParam("secret", new FirstParamFactory());
        assertNotNull(result);
        assertEquals("The key is shhh for secret. Param2 is: 'shhh secret'", result);
    }

    @Test
    public void testParamsFieldOrdering() throws Exception
    {
        testIBean.init("shhh".getBytes());

        String result = testIBean.doUriParams("secret");
        assertNotNull(result);
        assertEquals("The key is shhh for secret. Param2 is: 'shhh secret'", result);
    }

    @Test
    public void testHeaderParams() throws Exception
    {
        testIBean.init("shhh".getBytes());

        MuleMessage result = testIBean.doHeaderParam("secret");
        assertNotNull(result);
        assertEquals("Value is: secret", result.getPayloadAsString());
        //TODO switch inbound/outbound depending on logic in HttpMessageFactory
        assertEquals("shhh", result.getInboundProperty("header1"));
        assertEquals("shhh secret", result.getInboundProperty("header2"));
    }

    @Test
    public void testHeaderParamsOnMethod() throws Exception
    {
        testIBean.init("shhh".getBytes());

        MuleMessage result = testIBean.doMethodHeaderParam("secret", new EchoParamFactory());
        assertNotNull(result);
        assertEquals("Value is: secret", result.getPayloadAsString());
        assertEquals("shhh", result.getInboundProperty("header1"));
        assertEquals("shhh secret", result.getInboundProperty("header2"));
        assertEquals("echoHeader", result.getInboundProperty("echoHeader"));
    }

    @Test
    public void testPropertyParamsOnMethod() throws Exception
    {
        testIBean.init("shhh".getBytes());

        MuleMessage result = testIBean.doMethodPropertyParam("secret", "hello", new ReversePropertyParamFactory("customProperty"));
        assertNotNull(result);
        assertEquals("Value is: secret", result.getPayloadAsString());
        assertEquals("shhh", result.getInboundProperty("header1"));
        assertEquals("shhh secret", result.getInboundProperty("header2"));
        assertEquals("olleh", result.getInboundProperty("propHeader"));
    }

    @Test
    public void testHeadersWithNoParams() throws Exception
    {
        testIBean.init("shhh".getBytes());
        MuleMessage result = testIBean.doTestHeadersWithNoParams();
        assertNotNull(result);
        assertEquals("shhh", result.getInboundProperty("header1"));
    }

    @Test
    public void testHeaderParamsAndResponse() throws Exception
    {
        testIBean.init("shhh".getBytes());

        Response result = testIBean.doHeaderParamAndResponse("secret");
        assertNotNull(result);
        assertEquals("Value is: secret", result.getPayload());
        assertEquals("shhh", result.getHeader("header1"));
        assertEquals("shhh secret", result.getHeader("header2"));
    }

    @Test
    public void testHeaderParamsOnMethodAndResponse() throws Exception
    {
        testIBean.init("shhh".getBytes());

        Response result = testIBean.doMethodHeaderParamAndResponse("secret", new EchoParamFactory());
        assertNotNull(result);
        assertEquals("Value is: secret", result.getPayload());
        assertEquals("shhh", result.getHeader("header1"));
        assertEquals("shhh secret", result.getHeader("header2"));
        assertEquals("echoHeader", result.getHeader("echoHeader"));
    }

    @Test
    public void testPropertyParamsOnMethodAndResponse() throws Exception
    {
        testIBean.init("shhh".getBytes());

        Response result = testIBean.doMethodPropertyParamAndResponse("secret", "hello", new ReversePropertyParamFactory("customProperty"));
        assertNotNull(result);
        assertEquals("Value is: secret", result.getPayload());
        assertEquals("shhh", result.getHeader("header1"));
        assertEquals("shhh secret", result.getHeader("header2"));
        assertEquals("olleh", result.getHeader("propHeader"));
    }

    @Test
    public void testHeadersWithNoParamsAndResponse() throws Exception
    {
        testIBean.init("shhh".getBytes());
        Response result = testIBean.doTestHeadersWithNoParamsAndResponse();
        assertNotNull(result);
        assertEquals("shhh", result.getHeader("header1"));
    }
}
