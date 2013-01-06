/*
 * $Id: HttpsHandshakeTimingTestCase.java 25119 2012-12-10 21:20:57Z pablo.lagreca $
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.message.processing;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.mule.api.exception.SystemExceptionHandler;
import org.mule.tck.size.SmallTest;

import java.util.ArrayList;
import java.util.List;

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
public class PhaseExecutionEngineTestCase
{

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private SystemExceptionHandler mockExceptionHandler;
    private List<MessageProcessPhase> phaseList = new ArrayList<MessageProcessPhase>();
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private EndProcessPhase mockEndPhase;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private MessageProcessPhase mockProcessPhase1;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private MessageProcessPhase mockProcessPhase2;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private MessageProcessPhase mockProcessPhase3;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private MessageProcessPhase mockFailingPhase;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private EndPhaseTemplate mockTemplate;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private MessageProcessContext mockContext;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private PhaseResultNotifier mockNotifier;

    @Test
    public void allPhasesRun() throws Exception
    {
        when(mockEndPhase.supportsTemplate(mockTemplate)).thenReturn(true);
        verifyAllPhasesAreRun();
        verify(mockEndPhase, Mockito.times(1)).runPhase(any(EndPhaseTemplate.class), any(MessageProcessContext.class), any(PhaseResultNotifier.class));
    }

    @Test
    public void endPhaseDoesNotRun() throws Exception
    {
        when(mockEndPhase.supportsTemplate(mockTemplate)).thenReturn(false);
        verifyAllPhasesAreRun();
        verify(mockEndPhase, Mockito.times(0)).runPhase(any(EndPhaseTemplate.class), any(MessageProcessContext.class), any(PhaseResultNotifier.class));
    }

    @Test
    public void exceptionHandlerIsCalledOnFailure() throws Exception
    {
        addPhase(mockFailingPhase);
        addPhase(mockProcessPhase1);
        when(mockEndPhase.supportsTemplate(mockTemplate)).thenReturn(true);
        PhaseExecutionEngine phaseExecutionEngine = new PhaseExecutionEngine(phaseList, mockExceptionHandler, mockEndPhase);
        phaseExecutionEngine.process(mockTemplate,mockContext);
        verify(mockEndPhase,times(1)).runPhase(any(EndPhaseTemplate.class),any(MessageProcessContext.class), any(PhaseResultNotifier.class));
    }

    private void verifyAllPhasesAreRun()
    {
        PhaseExecutionEngine engine = new PhaseExecutionEngine(phaseList,mockExceptionHandler, mockEndPhase);
        addAllPhases();
        engine.process(mockTemplate, mockContext);
        verify(mockProcessPhase1, Mockito.times(1)).runPhase(any(MessageProcessTemplate.class), any(MessageProcessContext.class), any(PhaseResultNotifier.class));
        verify(mockProcessPhase2, Mockito.times(1)).runPhase(any(MessageProcessTemplate.class), any(MessageProcessContext.class), any(PhaseResultNotifier.class));
        verify(mockProcessPhase3, Mockito.times(1)).runPhase(any(MessageProcessTemplate.class), any(MessageProcessContext.class), any(PhaseResultNotifier.class));
    }

    private void addAllPhases()
    {
        addPhase(mockProcessPhase1);
        addPhase(mockProcessPhase2);
        addPhase(mockProcessPhase3);
    }

    private void addPhase(MessageProcessPhase mockProcessPhase)
    {
        phaseList.add(mockProcessPhase);
        when(mockProcessPhase.supportsTemplate(mockTemplate)).thenReturn(true);
        Mockito.doAnswer(new Answer()
        {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable
            {
                ((PhaseResultNotifier)invocationOnMock.getArguments()[2]).phaseSuccessfully();
                return null;
            }
        }).when(mockProcessPhase).runPhase(any(MessageProcessTemplate.class), any(MessageProcessContext.class), any(PhaseResultNotifier.class));
    }
}
