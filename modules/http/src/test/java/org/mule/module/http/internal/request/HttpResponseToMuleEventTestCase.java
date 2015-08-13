/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.internal.request;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mule.api.transport.PropertyScope.INBOUND;
import static org.mule.module.http.api.HttpConstants.ResponseProperties.HTTP_REASON_PROPERTY;
import static org.mule.module.http.api.HttpConstants.ResponseProperties.HTTP_STATUS_PROPERTY;
import org.mule.api.MessagingException;
import org.mule.api.MuleEvent;
import org.mule.module.http.internal.domain.InputStreamHttpEntity;
import org.mule.module.http.internal.domain.response.HttpResponse;
import org.mule.module.http.internal.domain.response.HttpResponseBuilder;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.util.AttributeEvaluator;
import org.mule.util.IOUtils;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;


public class HttpResponseToMuleEventTestCase extends AbstractMuleContextTestCase
{

    private static final String TEST_HEADER = "TestHeader";
    private static final String TEST_MULTIPLE_HEADER = "TestMultipleHeader";
    private static final String TEST_VALUE = "TestValue";
    private DefaultHttpRequester httpRequester;
    private HttpResponseToMuleEvent httpResponseToMuleEvent;
    private HttpResponse httpResponse;
    private MuleEvent event;

    @Before
    public void setup() throws Exception
    {
        httpRequester = new DefaultHttpRequester();
        httpRequester.setConfig(new DefaultHttpRequesterConfig());
        httpResponseToMuleEvent = new HttpResponseToMuleEvent(httpRequester, muleContext, new AttributeEvaluator("true"));

        HttpResponseBuilder builder = new HttpResponseBuilder();
        builder.setEntity(new InputStreamHttpEntity(new ByteArrayInputStream(TEST_MESSAGE.getBytes())));
        builder.addHeader(TEST_HEADER, TEST_VALUE);
        builder.addHeader(TEST_MULTIPLE_HEADER, TEST_VALUE);
        builder.addHeader(TEST_MULTIPLE_HEADER, TEST_VALUE);
        builder.setStatusCode(200);
        builder.setReasonPhrase("OK");
        httpResponse = builder.build();
        event = getTestEvent(null);
    }

    @Test
    public void responseHeadersAreMappedAsInboundProperties() throws MessagingException
    {
        httpResponseToMuleEvent.convert(event, httpResponse, null);
        assertThat((String) event.getMessage().getInboundProperty(TEST_HEADER), equalTo(TEST_VALUE));
        assertThat((List<String>) event.getMessage().getInboundProperty(TEST_MULTIPLE_HEADER), equalTo(Arrays.asList(TEST_VALUE, TEST_VALUE)));
    }

    @Test
    public void previousInboundPropertiesAreRemoved() throws MessagingException
    {
        event.getMessage().setProperty("TestInboundProperty", TEST_VALUE, INBOUND);
        httpResponseToMuleEvent.convert(event, httpResponse, null);
        assertThat(event.getMessage().getInboundProperty("TestInboundProperty"), nullValue());
    }

    @Test
    public void responseBodyIsSetAsPayload() throws MessagingException
    {
        httpResponseToMuleEvent.convert(event, httpResponse, null);
        InputStream responseStream = (InputStream) event.getMessage().getPayload();
        assertThat(IOUtils.toString(responseStream), equalTo(TEST_MESSAGE));
    }

    @Test
    public void statusCodeIsSetAsInboundProperty() throws MessagingException
    {
        httpResponseToMuleEvent.convert(event, httpResponse, null);
        assertThat((Integer) event.getMessage().getInboundProperty(HTTP_STATUS_PROPERTY), equalTo(200));
    }

    @Test
    public void responsePhraseIsSetAsInboundProperty() throws MessagingException
    {
        httpResponseToMuleEvent.convert(event, httpResponse, null);
        assertThat((String) event.getMessage().getInboundProperty(HTTP_REASON_PROPERTY), equalTo("OK"));
    }

}
