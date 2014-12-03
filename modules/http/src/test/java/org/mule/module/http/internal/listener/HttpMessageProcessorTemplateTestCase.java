/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.internal.listener;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.mule.api.MessagingException;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.processor.MessageProcessor;
import org.mule.config.i18n.CoreMessages;
import org.mule.module.http.internal.domain.response.HttpResponse;
import org.mule.module.http.internal.listener.async.HttpResponseReadyCallback;
import org.mule.module.http.internal.listener.async.ResponseStatusCallback;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;
import org.mule.transport.NullPayload;

import java.util.Collections;

import org.junit.Test;

@SmallTest
public class HttpMessageProcessorTemplateTestCase extends AbstractMuleTestCase
{

    private static final int INTERNAL_SERVER_ERROR = 500;
    private static final String TEST_MESSAGE = "";

    @Test
    public void statusCodeOnFailures() throws Exception
    {
        MuleEvent testEvent = createMockEvent();

        HttpMessageProcessorTemplate httpMessageProcessorTemplate = new HttpMessageProcessorTemplate(
                testEvent,
                mock(MessageProcessor.class),
                new HttpResponseReadyCallback()
                {
                    @Override
                    public void responseReady(HttpResponse response, ResponseStatusCallback responseStatusCallback)
                    {
                        assertThat(response.getStatusCode(), is(INTERNAL_SERVER_ERROR));
                    }
                },
                null,
                HttpResponseBuilder.emptyInstance(mock(MuleContext.class)));

        httpMessageProcessorTemplate.sendFailureResponseToClient(
                new MessagingException(CoreMessages.createStaticMessage(TEST_MESSAGE), testEvent), null);
    }

    private MuleEvent createMockEvent()
    {
        MuleMessage testMessage = mock(MuleMessage.class);
        when(testMessage.getOutboundPropertyNames()).thenReturn(Collections.<String>emptySet());
        when(testMessage.getPayload()).thenReturn(NullPayload.getInstance());

        MuleEvent testEvent = mock(MuleEvent.class);
        when(testEvent.getMessage()).thenReturn(testMessage);

        when(testEvent.getMuleContext()).thenReturn(mock(MuleContext.class, RETURNS_DEEP_STUBS));
        return testEvent;
    }

}
