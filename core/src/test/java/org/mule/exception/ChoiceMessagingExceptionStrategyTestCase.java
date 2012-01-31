/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.exception;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.internal.verification.VerificationModeFactory;
import org.mockito.runners.MockitoJUnitRunner;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleRuntimeException;
import org.mule.api.exception.ChoiceMessagingExceptionHandler;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

@RunWith(MockitoJUnitRunner.class)
@SmallTest
public class ChoiceMessagingExceptionStrategyTestCase extends AbstractMuleTestCase
{

    @Mock
    private ChoiceMessagingExceptionHandler mockTestExceptionStrategy1;
    @Mock
    private ChoiceMessagingExceptionHandler mockTestExceptionStrategy2;
    @Mock
    private ChoiceMessagingExceptionHandler mockDefaultTestExceptionStrategy2;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private MuleEvent mockMuleEvent;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private MuleContext mockMuleContext;
    private Exception mockException = new Exception();

    @Test
    public void testNonMatchThenCallDefault() throws Exception
    {
        ChoiceMessagingExceptionStrategy choiceMessagingExceptionStrategy = new ChoiceMessagingExceptionStrategy();
        choiceMessagingExceptionStrategy.setExceptionListeners(new ArrayList<ChoiceMessagingExceptionHandler>(Arrays.<ChoiceMessagingExceptionHandler>asList(mockTestExceptionStrategy1, mockTestExceptionStrategy2)));
        when(mockMuleContext.getDefaultExceptionStrategy()).thenReturn(mockDefaultTestExceptionStrategy2);
        choiceMessagingExceptionStrategy.setMuleContext(mockMuleContext);
        choiceMessagingExceptionStrategy.initialise();
        when(mockTestExceptionStrategy1.accept(mockMuleEvent)).thenReturn(false);
        when(mockTestExceptionStrategy2.accept(mockMuleEvent)).thenReturn(false);
        when(mockDefaultTestExceptionStrategy2.accept(mockMuleEvent)).thenReturn(true);
        choiceMessagingExceptionStrategy.handleException(mockException,mockMuleEvent);
        verify(mockTestExceptionStrategy1, VerificationModeFactory.times(0)).handleException(any(Exception.class),any(MuleEvent.class));
        verify(mockTestExceptionStrategy2, VerificationModeFactory.times(0)).handleException(any(Exception.class),any(MuleEvent.class));
        verify(mockDefaultTestExceptionStrategy2, VerificationModeFactory.times(1)).handleException(mockException, mockMuleEvent);
    }

    @Test(expected = MuleRuntimeException.class)
    public void testNoneMatchEvenDefault() throws Exception
    {
        ChoiceMessagingExceptionStrategy choiceMessagingExceptionStrategy = new ChoiceMessagingExceptionStrategy();
        choiceMessagingExceptionStrategy.setExceptionListeners(new ArrayList<ChoiceMessagingExceptionHandler>(Arrays.<ChoiceMessagingExceptionHandler>asList(mockTestExceptionStrategy1, mockTestExceptionStrategy2)));
        when(mockMuleContext.getDefaultExceptionStrategy()).thenReturn(mockDefaultTestExceptionStrategy2);
        choiceMessagingExceptionStrategy.setMuleContext(mockMuleContext);
        choiceMessagingExceptionStrategy.initialise();
        when(mockTestExceptionStrategy1.accept(mockMuleEvent)).thenReturn(false);
        when(mockTestExceptionStrategy2.accept(mockMuleEvent)).thenReturn(false);
        when(mockDefaultTestExceptionStrategy2.accept(mockMuleEvent)).thenReturn(false);
        choiceMessagingExceptionStrategy.handleException(mockException, mockMuleEvent);
    }

    @Test
    public void testSecondMatches() throws Exception
    {
        ChoiceMessagingExceptionStrategy choiceMessagingExceptionStrategy = new ChoiceMessagingExceptionStrategy();
        choiceMessagingExceptionStrategy.setExceptionListeners(new ArrayList<ChoiceMessagingExceptionHandler>(Arrays.<ChoiceMessagingExceptionHandler>asList(mockTestExceptionStrategy1, mockTestExceptionStrategy2)));
        when(mockMuleContext.getDefaultExceptionStrategy()).thenReturn(mockDefaultTestExceptionStrategy2);
        choiceMessagingExceptionStrategy.setMuleContext(mockMuleContext);
        choiceMessagingExceptionStrategy.initialise();
        when(mockTestExceptionStrategy1.accept(mockMuleEvent)).thenReturn(false);
        when(mockTestExceptionStrategy2.accept(mockMuleEvent)).thenReturn(true);
        when(mockDefaultTestExceptionStrategy2.accept(mockMuleEvent)).thenReturn(true);
        choiceMessagingExceptionStrategy.handleException(mockException,mockMuleEvent);
        verify(mockTestExceptionStrategy1, VerificationModeFactory.times(0)).handleException(any(Exception.class),any(MuleEvent.class));
        verify(mockDefaultTestExceptionStrategy2, VerificationModeFactory.times(0)).handleException(any(Exception.class),any(MuleEvent.class));
        verify(mockTestExceptionStrategy2, VerificationModeFactory.times(1)).handleException(mockException, mockMuleEvent);
    }

    @Test
    public void testFirstAcceptsAllMatches() throws Exception
    {
        ChoiceMessagingExceptionStrategy choiceMessagingExceptionStrategy = new ChoiceMessagingExceptionStrategy();
        choiceMessagingExceptionStrategy.setExceptionListeners(new ArrayList<ChoiceMessagingExceptionHandler>(Arrays.<ChoiceMessagingExceptionHandler>asList(mockTestExceptionStrategy1, mockTestExceptionStrategy2)));
        when(mockMuleContext.getDefaultExceptionStrategy()).thenReturn(mockDefaultTestExceptionStrategy2);
        choiceMessagingExceptionStrategy.setMuleContext(mockMuleContext);
        when(mockTestExceptionStrategy1.acceptsAll()).thenReturn(true);
        when(mockTestExceptionStrategy2.acceptsAll()).thenReturn(false);
        when(mockDefaultTestExceptionStrategy2.acceptsAll()).thenReturn(true);
        choiceMessagingExceptionStrategy.initialise();
    }

}
