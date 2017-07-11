/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.http.internal.listener;

import static java.lang.String.format;
import static org.apache.commons.lang.StringEscapeUtils.escapeHtml;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.mule.module.http.internal.domain.InputStreamHttpEntity;
import org.mule.module.http.internal.domain.request.HttpRequestContext;
import org.mule.module.http.internal.domain.response.HttpResponse;
import org.mule.module.http.internal.listener.async.HttpResponseReadyCallback;
import org.mule.module.http.internal.listener.async.ResponseStatusCallback;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.util.IOUtils;

import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;


public class ErrorRequestHandlerTestCase extends AbstractMuleTestCase
{
    private final static int TEST_STATUS_CODE = 404;
    private final static String TEST_REASON_PHRASE = "Test reason phrase";
    private final static String TEST_REQUEST_INVALID_URI  = "http://localhost:8081/<script>alert('hello');</script>";
    private final static String TEST_ENTITY_FORMAT = "Error Handler for: %s";
    private final HttpRequestContext context = mock(HttpRequestContext.class, RETURNS_DEEP_STUBS);
    private final HttpResponseReadyCallback responseReadyCallback = mock(HttpResponseReadyCallback.class);
    private ErrorRequestHandler errorRequestHandler;

    @Before
    public void setUp() throws Exception
    {
        errorRequestHandler = new ErrorRequestHandler(TEST_STATUS_CODE, TEST_REASON_PHRASE, TEST_ENTITY_FORMAT);
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
        assertThat(result[0], is(format(TEST_ENTITY_FORMAT, escapeHtml(TEST_REQUEST_INVALID_URI))));
    }
}