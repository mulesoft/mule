/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.internal.listener;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mule.module.http.api.HttpHeaders.Names.CONTENT_LENGTH;
import org.mule.api.MuleEvent;
import org.mule.module.http.internal.domain.response.HttpResponse;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.junit.Test;

public class HttpResponseBuilderTestCase extends AbstractMuleContextTestCase
{

    private static final String EXAMPLE_STRING = "exampleString";

    @Test
    public void testContentLengthIsOverridden() throws Exception
    {
        HttpResponseBuilder httpResponseBuilder = new HttpResponseBuilder();
        int contentLengthDifferingPayloadSize = 12;
        MuleEvent event = mockEvent(new ByteArrayInputStream(EXAMPLE_STRING.getBytes(UTF_8)), contentLengthDifferingPayloadSize);

        HttpResponse httpResponse = httpResponseBuilder.build(new org.mule.module.http.internal.domain.response.HttpResponseBuilder(), event);
        assertThat(httpResponse.getHeaderValue(CONTENT_LENGTH), is(String.valueOf(EXAMPLE_STRING.length())));
    }

    private MuleEvent mockEvent(InputStream payload, int contentLength) throws Exception
    {
        MuleEvent event = getTestEvent(payload);
        event.getMessage().setOutboundProperty(CONTENT_LENGTH, contentLength);
        return event;
    }
}
