/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.message.processing;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.mule.execution.EndPhaseTemplate;
import org.mule.execution.EndProcessPhase;
import org.mule.execution.MessageProcessContext;
import org.mule.execution.MessageProcessTemplate;
import org.mule.execution.PhaseResultNotifier;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
@SmallTest
public class EndProcessPhaseTestCase extends AbstractMuleTestCase
{
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private MessageProcessTemplate notSupportedTemplate;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private EndPhaseTemplate supportedTemplate;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private PhaseResultNotifier mockPhaseResultNotifier;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private MessageProcessContext mockMessageContext;
    private EndProcessPhase endProcessPhase = new EndProcessPhase();

    @Test
    public void supportedTemplates()
    {
        new PhaseSupportTestHelper<EndPhaseTemplate>(EndPhaseTemplate.class).testSupportTemplates(endProcessPhase);
    }

    @Test
    public void phaseExecution()
    {
        endProcessPhase.runPhase(supportedTemplate,mockMessageContext,mockPhaseResultNotifier);
        verify(mockPhaseResultNotifier, times(0)).phaseConsumedMessage();
        verify(mockPhaseResultNotifier, times(0)).phaseFailure(any(Exception.class));
        verify(mockPhaseResultNotifier, times(0)).phaseConsumedMessage();
        verify(supportedTemplate, times(1)).messageProcessingEnded();
    }

}
