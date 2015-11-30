/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.message.processing;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.mule.api.DefaultMuleException;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.exception.SystemExceptionHandler;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.execution.EndPhaseTemplate;
import org.mule.execution.FlowProcessingPhase;
import org.mule.execution.FlowProcessingPhaseTemplate;
import org.mule.execution.MessageProcessContext;
import org.mule.execution.MessageProcessPhase;
import org.mule.execution.MessageProcessTemplate;
import org.mule.execution.MuleMessageProcessingManager;
import org.mule.execution.PhaseResultNotifier;
import org.mule.execution.ValidationPhase;
import org.mule.execution.ValidationPhaseTemplate;
import org.mule.tck.size.SmallTest;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

@RunWith(MockitoJUnitRunner.class)
@SmallTest
public class MuleMessageProcessingManagerTestCase extends org.mule.tck.junit4.AbstractMuleTestCase
{

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private MuleContext mockMuleContext;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private TestMessageProcessTemplateAndContext completeMessageProcessTemplateAndContext;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private SystemExceptionHandler mockExceptionListener;

    @Test
    public void nullMessageProcessPhaseInRegistry() throws Exception
    {
        processAndVerifyDefaultPhasesUsingRegistryPhases(null);
    }

    @Test
    public void emptyMessageProcessPhaseInRegistry() throws Exception
    {
        processAndVerifyDefaultPhasesUsingRegistryPhases(Collections.<MessageProcessPhase>emptyList());
    }

    @Test
    public void notSupportedMessageProcessPhaseInRegistry() throws Exception
    {
        MessageProcessPhase notSupportedPhase = createNotSupportedPhase();
        processAndVerifyDefaultPhasesUsingRegistryPhases(Arrays.asList(notSupportedPhase));
    }

    @Test
    public void messageConsumedPreventsNextPhaseToBeExecuted() throws Exception
    {
        PhaseAfterValidationBeforeFlow messageProcessPhase = createPhaseAfterValidation();
        when(completeMessageProcessTemplateAndContext.validateMessage()).thenReturn(true);
        when(messageProcessPhase.compareTo(any(MessageProcessPhase.class))).thenCallRealMethod();
        when(messageProcessPhase.supportsTemplate(any(MessageProcessTemplate.class))).thenCallRealMethod();
        doCallRealMethod().when(messageProcessPhase).runPhase(any(MessageProcessTemplate.class), any(MessageProcessContext.class), any(PhaseResultNotifier.class));
        MuleMessageProcessingManager manager = createManagerUsingPhasesInRegistry(Arrays.<MessageProcessPhase>asList(messageProcessPhase));
        manager.processMessage(completeMessageProcessTemplateAndContext, completeMessageProcessTemplateAndContext);
        verify(completeMessageProcessTemplateAndContext, times(0)).routeEvent(any(MuleEvent.class));
        verify(completeMessageProcessTemplateAndContext, times(1)).validateMessage();
        verify(completeMessageProcessTemplateAndContext, times(1)).messageProcessingEnded();
    }

    private PhaseAfterValidationBeforeFlow createPhaseAfterValidation()
    {
        return mock(PhaseAfterValidationBeforeFlow.class,Answers.RETURNS_DEEP_STUBS.get());
    }

    @Test
    public void testExceptionHandlerIsCalledDuringPhaseFailure() throws Exception
    {
        MessageProcessPhase failureMessageProcessPhase = createFailureMessageProcessPhase();
        when(mockMuleContext.getExceptionListener()).thenReturn(mockExceptionListener);
        MuleMessageProcessingManager manager = createManagerUsingPhasesInRegistry(Arrays.asList(failureMessageProcessPhase));
        manager.processMessage(completeMessageProcessTemplateAndContext, completeMessageProcessTemplateAndContext);
        verify(mockExceptionListener, times(1)).handleException(any(MuleException.class));
    }

    private MessageProcessPhase createFailureMessageProcessPhase()
    {
        FailureMessageProcessPhase failureMessageProcessPhase = mock(FailureMessageProcessPhase.class, Answers.RETURNS_DEEP_STUBS.get());
        when(failureMessageProcessPhase.supportsTemplate(any(MessageProcessTemplate.class))).thenCallRealMethod();
        doAnswer(new Answer()
        {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable
            {
                PhaseResultNotifier phaseResultNotifier = (PhaseResultNotifier) invocationOnMock.getArguments()[2];
                phaseResultNotifier.phaseFailure(new DefaultMuleException("error"));
                return null;
            }
        }).when(failureMessageProcessPhase).runPhase(any(MessageProcessTemplate.class), any(MessageProcessContext.class), any(PhaseResultNotifier.class));
        return failureMessageProcessPhase;
    }

    private MessageProcessPhase createNotSupportedPhase()
    {
        MessageProcessPhase notSupportedPhase = mock(MessageProcessPhase.class, Answers.RETURNS_DEEP_STUBS.get());
        when(notSupportedPhase.supportsTemplate(any(MessageProcessTemplate.class))).thenReturn(false);
        return notSupportedPhase;
    }

    private void processAndVerifyDefaultPhasesUsingRegistryPhases(Collection<MessageProcessPhase> phasesInRegistry) throws Exception
    {
        MuleMessageProcessingManager manager = createManagerUsingPhasesInRegistry(phasesInRegistry);
        processAndVerifyDefaultPhasesAreExecuted(manager);
    }

    private MuleMessageProcessingManager createManagerUsingPhasesInRegistry(Collection<MessageProcessPhase> phasesInRegistry) throws InitialisationException
    {
        MuleMessageProcessingManager manager = new MuleMessageProcessingManager();
        manager.setMuleContext(mockMuleContext);
        when(mockMuleContext.getRegistry().lookupObjects(MessageProcessPhase.class)).thenReturn(phasesInRegistry);
        manager.initialise();
        return manager;
    }

    private void processAndVerifyDefaultPhasesAreExecuted(MuleMessageProcessingManager manager) throws Exception
    {
        when(completeMessageProcessTemplateAndContext.validateMessage()).thenReturn(true);

        manager.processMessage(completeMessageProcessTemplateAndContext,completeMessageProcessTemplateAndContext);
        InOrder verifyInOrder = Mockito.inOrder(completeMessageProcessTemplateAndContext);
        verifyInOrder.verify(completeMessageProcessTemplateAndContext, times(1)).validateMessage();
        verifyInOrder.verify(completeMessageProcessTemplateAndContext, times(1)).routeEvent(Mockito.any(MuleEvent.class));
        verifyInOrder.verify(completeMessageProcessTemplateAndContext, times(1)).messageProcessingEnded();
    }

    public interface TestMessageProcessTemplateAndContext extends ValidationPhaseTemplate, FlowProcessingPhaseTemplate, EndPhaseTemplate, MessageProcessContext
    {
    }

    public abstract class PhaseAfterValidationBeforeFlow implements MessageProcessPhase, Comparable<MessageProcessPhase>
    {

        @Override
        public boolean supportsTemplate(MessageProcessTemplate messageProcessTemplate)
        {
            return true;
        }

        @Override
        public int compareTo(MessageProcessPhase messageProcessPhase)
        {
            if (messageProcessPhase instanceof ValidationPhase)
            {
                return 1;
            }
            if (messageProcessPhase instanceof FlowProcessingPhase)
            {
                return -1;
            }
            return 0;
        }

        @Override
        public void runPhase(MessageProcessTemplate messageProcessTemplate, MessageProcessContext messageProcessContext, PhaseResultNotifier phaseResultNotifier)
        {
            phaseResultNotifier.phaseConsumedMessage();
        }
    }

    public abstract class FailureMessageProcessPhase implements MessageProcessPhase, Comparable<MessageProcessPhase>
    {

        @Override
        public boolean supportsTemplate(MessageProcessTemplate messageProcessTemplate)
        {
            return true;
        }

        @Override
        public int compareTo(MessageProcessPhase messageProcessPhase)
        {
            return -1;
        }

    }

}
