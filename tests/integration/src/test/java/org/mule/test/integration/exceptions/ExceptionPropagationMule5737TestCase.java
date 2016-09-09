/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.exceptions;

import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mule.functional.junit4.matchers.ThrowableCauseMatcher.hasCause;

import org.mule.functional.exceptions.FunctionalTestException;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.component.ComponentException;
import org.mule.runtime.core.exception.AbstractMessagingExceptionStrategy;
import org.mule.runtime.core.exception.MessagingException;
import org.mule.test.AbstractIntegrationTestCase;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * Assert that flows do not propagate exceptions via runFlow or use of flow-ref. Also
 * assert that a sub-flow/processor-chain does not handle it's own exception but they are rather handled by
 * calling flow.
 */
public class ExceptionPropagationMule5737TestCase extends AbstractIntegrationTestCase {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Override
  protected String getConfigFile() {
    return "org/mule/test/integration/exceptions/exception-propagation-mule-5737-config.xml";
  }

  @Test
  public void testRequestResponseEndpointExceptionPropagation() throws Exception {
    expectedException.expectCause(instanceOf(ComponentException.class));
    expectedException.expectCause(hasCause(instanceOf(FunctionalTestException.class)));
    runFlow("flow");
  }

  @Test
  public void testFlowWithChildFlowExceptionPropagation() throws Exception {
    FlowConstruct flow = muleContext.getRegistry().lookupFlowConstruct("flowWithChildFlow");
    FlowConstruct childFlow = muleContext.getRegistry().lookupFlowConstruct("childFlow");
    SensingExceptionStrategy parentES = (SensingExceptionStrategy) flow.getExceptionListener();
    SensingExceptionStrategy childFlowES = (SensingExceptionStrategy) childFlow.getExceptionListener();

    runFlow("flowWithChildFlow");

    assertFalse(parentES.caught);
    assertTrue(childFlowES.caught);
  }

  @Test
  public void testFlowWithSubFlowExceptionPropagation() throws Exception {
    SensingExceptionStrategy parentES = (SensingExceptionStrategy) muleContext.getRegistry()
        .lookupFlowConstruct("flowWithSubFlow")
        .getExceptionListener();

    runFlow("flowWithSubFlow");

    assertTrue(parentES.caught);
  }

  @Test
  public void testFlowWithChildServiceExceptionPropagation() throws Exception {
    SensingExceptionStrategy parentES = (SensingExceptionStrategy) muleContext.getRegistry()
        .lookupFlowConstruct("flowWithChildService")
        .getExceptionListener();
    SensingExceptionStrategy childServiceES = (SensingExceptionStrategy) muleContext.getRegistry()
        .lookupFlowConstruct("childService")
        .getExceptionListener();

    runFlow("flowWithChildService");

    assertFalse(parentES.caught);
    assertTrue(childServiceES.caught);
  }

  public static class SensingExceptionStrategy extends AbstractMessagingExceptionStrategy {

    public SensingExceptionStrategy() {
      super(null);
    }

    boolean caught;

    @Override
    public MuleEvent handleException(MessagingException e, MuleEvent event) {
      caught = true;
      MuleEvent resultEvent = super.handleException(e, event);
      e.setHandled(true);
      return MuleEvent.builder(resultEvent).message(MuleMessage.builder(resultEvent.getMessage()).exceptionPayload(null).build())
          .error(null).build();
    }

  }

}
