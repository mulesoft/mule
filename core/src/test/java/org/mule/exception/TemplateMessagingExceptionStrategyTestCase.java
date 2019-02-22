/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.exception;

import static java.lang.System.setProperty;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mule.api.config.MuleProperties.DISABLE_ERROR_COUNT_ON_ERROR_NOTIFICATION_DISABLED;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.config.MuleProperties;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.transaction.Transaction;
import org.mule.management.stats.FlowConstructStatistics;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.transaction.TransactionCoordination;

@RunWith(MockitoJUnitRunner.class)
public class TemplateMessagingExceptionStrategyTestCase extends AbstractMuleContextTestCase
{

    private MuleContext mockMuleContext = mock(MuleContext.class, Answers.RETURNS_DEEP_STUBS.get());
    @Mock
    private Exception mockException;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private MuleEvent mockMuleEvent;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private MuleMessage mockMuleMessage;
    @Mock
    private FlowConstruct mockFlowConstruct;
    @Mock
    private FlowConstructStatistics mockStatistics;


    @Mock(answer = RETURNS_DEEP_STUBS)
    protected MuleContext muleContext;


    public TemplateMessagingExceptionStrategy createExceptionStrategy() throws Exception
    {
        TestMessagingExceptionStrategyTestCase messagingExceptionStrategy = new TestMessagingExceptionStrategyTestCase();
        messagingExceptionStrategy.setMuleContext(mockMuleContext);
        when(mockMuleEvent.getMessage()).thenReturn(mockMuleMessage);
        when(mockMuleEvent.getFlowConstruct()).thenReturn(mockFlowConstruct);
        when(mockFlowConstruct.getStatistics()).thenReturn(mockStatistics);
        when(mockStatistics.isEnabled()).thenReturn(true);
        return messagingExceptionStrategy;
    }

    @Test
    public void statisticsErrorCountIncreaseByDefault() throws Exception
    {
        testErrorCount(false, false, 1);
        testErrorCount(false, true, 2);
    }

    @Test
    public void statisticsErrorCountDoesNotIncreaseIfDisableErrorCountAndErrorNotificationDisabled() throws Exception
    {
        testErrorCount(true, false, 0);
    }

    public void statisticsErrorCountDoesIncreaseIfDisableErrorCountAndErrorNotificationEnabled() throws Exception
    {
        testErrorCount(false, true, 1);
    }

    private void testErrorCount(Boolean disableErrorCount, boolean enableNotifications, int expectedExcecutionErrorCount) throws Exception
    {
        setProperty(DISABLE_ERROR_COUNT_ON_ERROR_NOTIFICATION_DISABLED, disableErrorCount.toString());
        TemplateMessagingExceptionStrategy messagingExceptionStrategy = createExceptionStrategy();
        messagingExceptionStrategy.setEnableNotifications(enableNotifications);
        messagingExceptionStrategy.handleException(mockException, mockMuleEvent);
        verify(mockStatistics, Mockito.times(expectedExcecutionErrorCount)).incExecutionError();
    }

    private static class TestMessagingExceptionStrategyTestCase extends TemplateMessagingExceptionStrategy
    {

    }


}
