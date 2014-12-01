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
import org.mule.api.MessagingException;
import org.mule.api.MuleEvent;
import org.mule.api.transport.PropertyScope;
import org.mule.module.http.api.HttpConstants;
import org.mule.module.http.internal.domain.InputStreamHttpEntity;
import org.mule.module.http.internal.domain.response.HttpResponse;
import org.mule.module.http.internal.domain.response.HttpResponseBuilder;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.util.AttributeEvaluator;
import org.mule.util.IOUtils;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.junit.Before;
import org.junit.Test;


public class HttpResponseToMuleEventTestCase extends AbstractMuleContextTestCase
{

    private DefaultHttpRequester httpRequester;
    private HttpResponseToMuleEvent httpResponseToMuleEvent;
    private HttpResponse httpResponse;
    private MuleEvent event;

    @Before
    public void setup() throws Exception
    {
        httpRequester = new DefaultHttpRequester();
        httpResponseToMuleEvent = new HttpResponseToMuleEvent(httpRequester, muleContext, new AttributeEvaluator("true"));

        HttpResponseBuilder builder = new HttpResponseBuilder();
        builder.setEntity(new InputStreamHttpEntity(new ByteArrayInputStream(TEST_MESSAGE.getBytes())));
        builder.addHeader("TestHeader", "TestValue");
        builder.setStatusCode(200);
        httpResponse = builder.build();
        event = getTestEvent(null);
    }

    @Test
    public void responseHeadersAreMappedAsInboundProperties() throws MessagingException
    {
        httpResponseToMuleEvent.convert(event, httpResponse);
        assertThat((String) event.getMessage().getInboundProperty("TestHeader"), equalTo("TestValue"));
    }

    @Test
    public void previousInboundPropertiesAreRemoved() throws MessagingException
    {
        event.getMessage().setProperty("TestInboundProperty", "TestValue", PropertyScope.INBOUND);
        httpResponseToMuleEvent.convert(event, httpResponse);
        assertThat(event.getMessage().getInboundProperty("TestInboundProperty"), nullValue());
    }

    @Test
    public void responseBodyIsSetAsPayload() throws MessagingException
    {
        httpResponseToMuleEvent.convert(event, httpResponse);
        InputStream responseStream = (InputStream) event.getMessage().getPayload();
        assertThat(IOUtils.toString(responseStream), equalTo(TEST_MESSAGE));
    }

    @Test
    public void statusCodeIsSetAsInboundProperty() throws MessagingException
    {
        httpResponseToMuleEvent.convert(event, httpResponse);
        assertThat((Integer) event.getMessage().getInboundProperty(HttpConstants.ResponseProperties.HTTP_STATUS_PROPERTY), equalTo(200));
    }

}
