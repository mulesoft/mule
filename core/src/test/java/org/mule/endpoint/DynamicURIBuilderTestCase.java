/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.endpoint;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.endpoint.MalformedEndpointException;
import org.mule.api.expression.ExpressionManager;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;

import org.junit.Test;

@SmallTest
public class DynamicURIBuilderTestCase extends AbstractMuleTestCase
{

    public static final String EXPECTED_ADDRESS = "http://admin%40abc:admin%40123@localhost:8080/test?foo=bar";
    private final MuleEvent event = mock(MuleEvent.class);
    private final MuleContext muleContext = mock(MuleContext.class);
    public static final String ATTRIBUTE_EXPRESSION = "#[expression]";

    @Test
    public void resolvesDynamicAddress() throws Exception
    {
        String uriTemplate = "http://admin%40abc:admin%40123@localhost:8080/#[expression]";

        createExpressionManager(uriTemplate, EXPECTED_ADDRESS);

        URIBuilder uriBuilder = new URIBuilder(uriTemplate, muleContext);

        doDynamicUriResolverTest(uriBuilder);
    }

    @Test
    public void resolvesDynamicPort() throws Exception
    {
        String templatePort = "#[expression]";

        createExpressionManager(ATTRIBUTE_EXPRESSION, "8080");

        URIBuilder uriBuilder = createDefaultUriBuilder(muleContext);
        uriBuilder.setPort(templatePort);

        doDynamicUriResolverTest(uriBuilder);
    }

    @Test
    public void resolvesDynamicHost() throws Exception
    {
        createExpressionManager(ATTRIBUTE_EXPRESSION, "localhost");

        URIBuilder uriBuilder = createDefaultUriBuilder(muleContext);
        uriBuilder.setHost(ATTRIBUTE_EXPRESSION);

        doDynamicUriResolverTest(uriBuilder);
    }

    @Test
    public void resolvesDynamicPath() throws Exception
    {
        createExpressionManager(ATTRIBUTE_EXPRESSION, "test?foo=bar");

        URIBuilder uriBuilder = createDefaultUriBuilder(muleContext);
        uriBuilder.setPath(ATTRIBUTE_EXPRESSION);

        doDynamicUriResolverTest(uriBuilder);
    }

    @Test
    public void resolvesDynamicPassword() throws Exception
    {
        createExpressionManager(ATTRIBUTE_EXPRESSION, "admin@123");

        URIBuilder uriBuilder = createDefaultUriBuilder(muleContext);
        uriBuilder.setPassword(ATTRIBUTE_EXPRESSION);

        doDynamicUriResolverTest(uriBuilder);
    }

    @Test
    public void resolvesDynamicUser() throws Exception
    {
        createExpressionManager(ATTRIBUTE_EXPRESSION, "admin@abc");

        URIBuilder uriBuilder = createDefaultUriBuilder(muleContext);
        uriBuilder.setUser(ATTRIBUTE_EXPRESSION);

        doDynamicUriResolverTest(uriBuilder);
    }

    @Test(expected = MalformedEndpointException.class)
    public void throwsErrorOnInvalidUrl() throws Exception
    {
        URIBuilder uriBuilder = new URIBuilder("#[expression]://localhost", muleContext);

        DynamicURIBuilder dynamicURIBuilder = new DynamicURIBuilder(uriBuilder);
        dynamicURIBuilder.build(event);
    }

    private void createExpressionManager(String expression, final String expressionValue)
    {
        ExpressionManager expressionManager = mock(ExpressionManager.class);

        when(muleContext.getExpressionManager()).thenReturn(expressionManager);
        when(expressionManager.isExpression(expression)).thenReturn(true);
        when(expressionManager.parse(expression, event, true)).thenReturn(expressionValue);
    }

    private void doDynamicUriResolverTest(URIBuilder uriBuilder) throws URISyntaxException, UnsupportedEncodingException, MalformedEndpointException
    {
        DynamicURIBuilder dynamicURIBuilder = new DynamicURIBuilder(uriBuilder);
        String uri = dynamicURIBuilder.build(event);

        assertThat(uri, equalTo(EXPECTED_ADDRESS));
    }

    private URIBuilder createDefaultUriBuilder(MuleContext muleContext)
    {
        URIBuilder uriBuilder = new URIBuilder(muleContext);

        uriBuilder.setUser("admin@abc");
        uriBuilder.setPassword("admin@123");
        uriBuilder.setHost("localhost");
        uriBuilder.setPath("test?foo=bar");
        uriBuilder.setProtocol("http");
        uriBuilder.setPort("8080");

        return uriBuilder;
    }
}