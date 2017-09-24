/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.execution;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mule.runtime.core.internal.exception.ErrorTypeRepositoryFactory.createDefaultErrorTypeRepository;
import static org.mule.tck.util.MuleContextUtils.mockContextWithServices;

import org.mule.runtime.api.artifact.Registry;
import org.mule.runtime.api.exception.DefaultMuleException;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.exception.SystemExceptionHandler;
import org.mule.runtime.core.internal.context.MuleContextWithRegistries;
import org.mule.runtime.core.privileged.execution.EndPhaseTemplate;
import org.mule.runtime.core.privileged.execution.FlowProcessingPhaseTemplate;
import org.mule.runtime.core.privileged.execution.MessageProcessContext;
import org.mule.runtime.core.privileged.execution.MessageProcessTemplate;
import org.mule.runtime.core.privileged.execution.ValidationPhaseTemplate;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.Collection;

@RunWith(MockitoJUnitRunner.class)
@SmallTest
public class MuleMessageProcessingManagerTestCase extends AbstractMuleTestCase {

  private MuleContextWithRegistries mockMuleContext = mockContextWithServices();
  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  private Registry registry;
  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  private TestMessageProcessTemplateAndContext completeMessageProcessTemplateAndContext;
  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  private SystemExceptionHandler mockExceptionListener;
  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  private FlowConstruct flowConstruct;

  @Before
  public void setUp() {
    String flowName = "root";
    when(completeMessageProcessTemplateAndContext.getMessageSource().getRootContainerName()).thenReturn(flowName);
    when(registry.lookupByName(flowName)).thenReturn(of(flowConstruct));
    when(mockMuleContext.getErrorTypeRepository()).thenReturn(createDefaultErrorTypeRepository());

    when(completeMessageProcessTemplateAndContext.getTransactionConfig()).thenReturn(empty());
  }

  @Test
  public void nullMessageProcessPhaseInRegistry() throws Exception {
    processAndVerifyDefaultPhasesUsingRegistryPhases(null);
  }

  @Test
  public void emptyMessageProcessPhaseInRegistry() throws Exception {
    processAndVerifyDefaultPhasesUsingRegistryPhases(emptyList());
  }

  @Test
  public void notSupportedMessageProcessPhaseInRegistry() throws Exception {
    MessageProcessPhase notSupportedPhase = createNotSupportedPhase();
    processAndVerifyDefaultPhasesUsingRegistryPhases(asList(notSupportedPhase));
  }

  @Test
  public void messageConsumedPreventsNextPhaseToBeExecuted() throws Exception {
    PhaseAfterValidationBeforeFlow messageProcessPhase = createPhaseAfterValidation();
    when(completeMessageProcessTemplateAndContext.validateMessage()).thenReturn(true);
    when(messageProcessPhase.compareTo(any(MessageProcessPhase.class))).thenCallRealMethod();
    when(messageProcessPhase.supportsTemplate(any(MessageProcessTemplate.class))).thenCallRealMethod();
    doCallRealMethod().when(messageProcessPhase).runPhase(any(MessageProcessTemplate.class), any(MessageProcessContext.class),
                                                          any(PhaseResultNotifier.class));
    MuleMessageProcessingManager manager =
        createManagerUsingPhasesInRegistry(asList(messageProcessPhase));
    manager.processMessage(completeMessageProcessTemplateAndContext, completeMessageProcessTemplateAndContext);
    verify(completeMessageProcessTemplateAndContext, times(0)).routeEvent(any(CoreEvent.class));
    verify(completeMessageProcessTemplateAndContext, times(1)).validateMessage();
    verify(completeMessageProcessTemplateAndContext, times(1)).messageProcessingEnded();
  }

  private PhaseAfterValidationBeforeFlow createPhaseAfterValidation() {
    return mock(PhaseAfterValidationBeforeFlow.class, Answers.RETURNS_DEEP_STUBS.get());
  }

  @Test
  public void testExceptionHandlerIsCalledDuringPhaseFailure() throws Exception {
    MessageProcessPhase failureMessageProcessPhase = createFailureMessageProcessPhase();
    when(mockMuleContext.getExceptionListener()).thenReturn(mockExceptionListener);
    MuleMessageProcessingManager manager = createManagerUsingPhasesInRegistry(Arrays.asList(failureMessageProcessPhase));
    manager.processMessage(completeMessageProcessTemplateAndContext, completeMessageProcessTemplateAndContext);
    verify(mockExceptionListener, times(1)).handleException(any(MuleException.class));
  }

  private MessageProcessPhase createFailureMessageProcessPhase() {
    FailureMessageProcessPhase failureMessageProcessPhase =
        mock(FailureMessageProcessPhase.class, Answers.RETURNS_DEEP_STUBS.get());
    when(failureMessageProcessPhase.supportsTemplate(any(MessageProcessTemplate.class))).thenCallRealMethod();
    doAnswer(invocationOnMock -> {
      PhaseResultNotifier phaseResultNotifier = (PhaseResultNotifier) invocationOnMock.getArguments()[2];
      phaseResultNotifier.phaseFailure(new DefaultMuleException("error"));
      return null;
    }).when(failureMessageProcessPhase).runPhase(any(MessageProcessTemplate.class), any(MessageProcessContext.class),
                                                 any(PhaseResultNotifier.class));
    return failureMessageProcessPhase;
  }

  private MessageProcessPhase createNotSupportedPhase() {
    MessageProcessPhase notSupportedPhase = mock(MessageProcessPhase.class, Answers.RETURNS_DEEP_STUBS.get());
    when(notSupportedPhase.supportsTemplate(any(MessageProcessTemplate.class))).thenReturn(false);
    return notSupportedPhase;
  }

  private void processAndVerifyDefaultPhasesUsingRegistryPhases(Collection<MessageProcessPhase> phasesInRegistry)
      throws Exception {
    MuleMessageProcessingManager manager = createManagerUsingPhasesInRegistry(phasesInRegistry);
    processAndVerifyDefaultPhasesAreExecuted(manager);
  }

  private MuleMessageProcessingManager createManagerUsingPhasesInRegistry(Collection<MessageProcessPhase> phasesInRegistry)
      throws InitialisationException {
    MuleMessageProcessingManager manager = new MuleMessageProcessingManager();
    manager.setMuleContext(mockMuleContext);
    manager.setRegistry(registry);
    manager.setRegistryMessageProcessPhases(ofNullable(phasesInRegistry));
    manager.initialise();
    return manager;
  }

  private void processAndVerifyDefaultPhasesAreExecuted(MuleMessageProcessingManager manager) throws Exception {
    when(completeMessageProcessTemplateAndContext.validateMessage()).thenReturn(true);

    manager.processMessage(completeMessageProcessTemplateAndContext, completeMessageProcessTemplateAndContext);
    InOrder verifyInOrder = inOrder(completeMessageProcessTemplateAndContext);
    verifyInOrder.verify(completeMessageProcessTemplateAndContext, times(1)).validateMessage();
    verifyInOrder.verify(completeMessageProcessTemplateAndContext, times(1)).routeEvent(Mockito.any(CoreEvent.class));
    verifyInOrder.verify(completeMessageProcessTemplateAndContext, times(1)).messageProcessingEnded();
  }

  public interface TestMessageProcessTemplateAndContext
      extends ValidationPhaseTemplate, FlowProcessingPhaseTemplate, EndPhaseTemplate, MessageProcessContext {
  }

  public abstract class PhaseAfterValidationBeforeFlow implements MessageProcessPhase, Comparable<MessageProcessPhase> {

    @Override
    public boolean supportsTemplate(MessageProcessTemplate messageProcessTemplate) {
      return true;
    }

    @Override
    public int compareTo(MessageProcessPhase messageProcessPhase) {
      if (messageProcessPhase instanceof ValidationPhase) {
        return 1;
      }
      if (messageProcessPhase instanceof FlowProcessingPhase) {
        return -1;
      }
      return 0;
    }

    @Override
    public void runPhase(MessageProcessTemplate messageProcessTemplate, MessageProcessContext messageProcessContext,
                         PhaseResultNotifier phaseResultNotifier) {
      phaseResultNotifier.phaseConsumedMessage();
    }
  }

  public abstract class FailureMessageProcessPhase implements MessageProcessPhase, Comparable<MessageProcessPhase> {

    @Override
    public boolean supportsTemplate(MessageProcessTemplate messageProcessTemplate) {
      return true;
    }

    @Override
    public int compareTo(MessageProcessPhase messageProcessPhase) {
      return -1;
    }

  }

}
