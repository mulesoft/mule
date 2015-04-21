/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.api.transport;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.*;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mule.api.MessagingException;
import org.mule.api.MuleEvent;
import org.mule.api.exception.ExceptionHandler;
import org.mule.api.exception.MessagingExceptionHandler;
import org.mule.execution.AsyncResponseFlowProcessingPhaseTemplate;
import org.mule.tck.SensingNullCompletionHandler;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import org.hamcrest.CoreMatchers;
import org.hamcrest.Matcher;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

@RunWith(MockitoJUnitRunner.class)
@SmallTest
public class ErrorHandlingNonBlockingResponseReplyToHandlerTestCase extends AbstractMuleTestCase
{

    @Mock
    private MessagingExceptionHandler messagingExceptionHandler;
    @Mock
    private MuleEvent sourceEvent;
    @Mock
    private MuleEvent handledEvent;

    @Test
    public void handleException()
    {
        SensingNullCompletionHandler completionHandler = new SensingNullCompletionHandler();
        ErrorHandlingNonBlockingResponseReplyToHandler replyToHandler = new
                ErrorHandlingNonBlockingResponseReplyToHandler(completionHandler, messagingExceptionHandler);
        MessagingException messagingException = new MessagingException(sourceEvent, new RuntimeException());

        when(messagingExceptionHandler.handleException(messagingException, sourceEvent)).thenAnswer(new Answer<MuleEvent>()
        {
            @Override
            public MuleEvent answer(InvocationOnMock invocation) throws Throwable
            {
                return handledEvent;
            }
        });

        replyToHandler.processExceptionReplyTo(sourceEvent, messagingException, null);

        verify(messagingExceptionHandler, Mockito.times(1)).handleException(messagingException, sourceEvent);
        assertThat(completionHandler.exception, CoreMatchers.<Exception>equalTo(messagingException));
        assertThat(((MessagingException) completionHandler.exception).getEvent(), equalTo(handledEvent));
        assertThat(completionHandler.event, nullValue());
    }

    @Test
    public void handleExceptionAndMarkHandled()
    {
        SensingNullCompletionHandler completionHandler = new SensingNullCompletionHandler();
        ErrorHandlingNonBlockingResponseReplyToHandler replyToHandler = new
                ErrorHandlingNonBlockingResponseReplyToHandler(completionHandler, messagingExceptionHandler);
        MessagingException messagingException = new MessagingException(sourceEvent, new RuntimeException());

        when(messagingExceptionHandler.handleException(messagingException, sourceEvent)).thenAnswer(new Answer<MuleEvent>()
        {
            @Override
            public MuleEvent answer(InvocationOnMock invocation) throws Throwable
            {
                ((MessagingException) invocation.getArguments()[0]).setHandled(true);
                return handledEvent;
            }
        });

        replyToHandler.processExceptionReplyTo(sourceEvent, messagingException, null);

        verify(messagingExceptionHandler, Mockito.times(1)).handleException(messagingException, sourceEvent);
        assertThat(completionHandler.exception, nullValue());
        assertThat(completionHandler.event, equalTo(handledEvent));
    }

}