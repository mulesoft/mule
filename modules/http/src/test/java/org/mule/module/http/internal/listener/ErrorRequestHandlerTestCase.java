/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.http.internal.listener;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static org.apache.commons.lang.StringEscapeUtils.escapeHtml;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.module.http.internal.listener.NoListenerRequestHandler.NO_LISTENER_ENTITY_FORMAT;
import static org.mule.module.http.internal.listener.NoMethodRequestHandler.METHOD_NOT_ALLOWED_FORMAT;
import static org.mule.module.http.internal.listener.BadRequestHandler.BAD_REQUEST_ENTITY_FORMAT;
import static org.mule.module.http.internal.listener.ServiceTemporarilyUnavailableListenerRequestHandler.SERVICE_NOT_AVAILABLE_FORMAT;

import org.mule.module.http.internal.domain.InputStreamHttpEntity;
import org.mule.module.http.internal.domain.request.HttpRequestContext;
import org.mule.module.http.internal.domain.response.HttpResponse;
import org.mule.module.http.internal.listener.async.HttpResponseReadyCallback;
import org.mule.module.http.internal.listener.async.ResponseStatusCallback;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.util.IOUtils;

import java.util.Collection;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

@RunWith(Parameterized.class)
public class ErrorRequestHandlerTestCase extends AbstractMuleTestCase
{
    private final static String TEST_REQUEST_INVALID_URI  = "http://localhost:8081/<script>alert('hello');</script>";
    private final HttpRequestContext context = mock(HttpRequestContext.class, RETURNS_DEEP_STUBS);
    private final HttpResponseReadyCallback responseReadyCallback = mock(HttpResponseReadyCallback.class);

    @Parameter(0)
    public ErrorRequestHandler errorRequestHandler;

    @Parameter(1)
    public String format;

    @Parameters(name = "{0}")
    public static Collection<Object[]> parameters()
    {
        final List<Object[]> objects = asList(new Object[][]{
                {NoListenerRequestHandler.getInstance(), NO_LISTENER_ENTITY_FORMAT},
                {NoMethodRequestHandler.getInstance(), METHOD_NOT_ALLOWED_FORMAT},
                {BadRequestHandler.getInstance(), BAD_REQUEST_ENTITY_FORMAT},
                {ServiceTemporarilyUnavailableListenerRequestHandler.getInstance(), SERVICE_NOT_AVAILABLE_FORMAT}
        });
        return objects;
    }

    @Before
    public void setUp() throws Exception
    {
        when(context.getRequest().getUri()).thenReturn(TEST_REQUEST_INVALID_URI);
    }

    @Test
    public void testInvalidEndpointWithSpecialCharacters() throws Exception
    {
        final String[] result = new String[1];
        doAnswer(new Answer()
        {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable
            {
                HttpResponse response = (HttpResponse) invocation.getArguments()[0];
                InputStreamHttpEntity inputStreamHttpEntity = (InputStreamHttpEntity) response.getEntity();
                result[0] = IOUtils.toString(inputStreamHttpEntity.getInputStream());
                inputStreamHttpEntity.getInputStream().close();
                return  null ;
            }
        }).when(responseReadyCallback).responseReady(any(HttpResponse.class), any(ResponseStatusCallback.class));
        errorRequestHandler.handleRequest(context, responseReadyCallback);
        assertThat(result[0], is(format(format, escapeHtml(TEST_REQUEST_INVALID_URI))));
    }
}
