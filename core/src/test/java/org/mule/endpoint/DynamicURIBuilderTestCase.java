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

        createExpressionManager(uriTemplate);

        URIBuilder uriBuilder = new URIBuilder(uriTemplate, muleContext);

        doDynamicUriResolverTest(uriBuilder);
    }

    @Test
    public void resolvesDynamicPort() throws Exception
    {
        String templatePort = "#[expression]";

        createExpressionManager("http://admin%40abc:admin%40123@localhost:#[expression]/test?foo=bar");

        URIBuilder uriBuilder = createDefaultUriBuilder(muleContext);
        uriBuilder.setPort(templatePort);

        doDynamicUriResolverTest(uriBuilder);
    }

    @Test
    public void resolvesDynamicHost() throws Exception
    {
        createExpressionManager("http://admin%40abc:admin%40123@#[expression]:8080/test?foo=bar");

        URIBuilder uriBuilder = createDefaultUriBuilder(muleContext);
        uriBuilder.setHost(ATTRIBUTE_EXPRESSION);

        doDynamicUriResolverTest(uriBuilder);
    }

    @Test
    public void resolvesDynamicPath() throws Exception
    {
        createExpressionManager("http://admin%40abc:admin%40123@localhost:8080/#[expression]");

        URIBuilder uriBuilder = createDefaultUriBuilder(muleContext);
        uriBuilder.setPath(ATTRIBUTE_EXPRESSION);

        doDynamicUriResolverTest(uriBuilder);
    }

    @Test
    public void resolvesDynamicPassword() throws Exception
    {
        createExpressionManager("http://admin%40abc:#[expression]@localhost:8080/test?foo=bar");

        URIBuilder uriBuilder = createDefaultUriBuilder(muleContext);
        uriBuilder.setPassword(ATTRIBUTE_EXPRESSION);

        doDynamicUriResolverTest(uriBuilder);
    }

    @Test
    public void resolvesDynamicUser() throws Exception
    {
        createExpressionManager("http://#[expression]:admin%40123@localhost:8080/test?foo=bar");

        URIBuilder uriBuilder = createDefaultUriBuilder(muleContext);
        uriBuilder.setUser(ATTRIBUTE_EXPRESSION);

        doDynamicUriResolverTest(uriBuilder);
    }

    private void createExpressionManager(String templateUri)
    {
        ExpressionManager expressionManager = mock(ExpressionManager.class);

        when(muleContext.getExpressionManager()).thenReturn(expressionManager);
        when(expressionManager.isExpression(templateUri)).thenReturn(true);
        when(expressionManager.parse(templateUri, event, true)).thenReturn(EXPECTED_ADDRESS);
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

        uriBuilder.setUser("admin%40abc");
        uriBuilder.setPassword("admin%40123");
        uriBuilder.setHost("localhost");
        uriBuilder.setPath("test?foo=bar");
        uriBuilder.setProtocol("http");
        uriBuilder.setPort("8080");

        return uriBuilder;
    }
}