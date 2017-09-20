/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.execution;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.internal.execution.PhaseResultNotifier;
import org.mule.runtime.core.internal.execution.ValidationPhase;
import org.mule.runtime.core.privileged.execution.MessageProcessContext;
import org.mule.runtime.core.privileged.execution.ValidationPhaseTemplate;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.tck.size.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
@SmallTest
public class ValidationPhaseTestCase extends AbstractMuleContextTestCase {

  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  private ValidationPhaseTemplate mockTemplate;
  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  private MessageProcessContext mockContext;
  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  private PhaseResultNotifier mockPhaseResultNotifier;
  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  private MuleException mockMulException;

  @Test
  public void supportsTemplate() {
    new PhaseSupportTestHelper<ValidationPhaseTemplate>(ValidationPhaseTemplate.class)
        .testSupportTemplates(new ValidationPhase());
  }

  @Test
  public void valid() {
    when(mockTemplate.validateMessage()).thenReturn(true);
    new ValidationPhase().runPhase(mockTemplate, mockContext, mockPhaseResultNotifier);
    verify(mockPhaseResultNotifier, Mockito.times(1)).phaseSuccessfully();
  }

  @Test
  public void invalid() throws Exception {
    when(mockTemplate.validateMessage()).thenReturn(false);
    new ValidationPhase().runPhase(mockTemplate, mockContext, mockPhaseResultNotifier);
    verify(mockTemplate, times(1)).discardInvalidMessage();
    verify(mockPhaseResultNotifier, Mockito.times(1)).phaseConsumedMessage();
  }

  @Test
  public void validationFails() throws Exception {
    when(mockTemplate.validateMessage()).thenReturn(false);
    doThrow(mockMulException).when(mockTemplate).discardInvalidMessage();
    new ValidationPhase().runPhase(mockTemplate, mockContext, mockPhaseResultNotifier);
    verify(mockPhaseResultNotifier, Mockito.times(1)).phaseFailure(mockMulException);
  }
}
