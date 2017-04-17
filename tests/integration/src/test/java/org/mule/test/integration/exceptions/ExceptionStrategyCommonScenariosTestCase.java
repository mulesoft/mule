/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.exceptions;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.internal.message.InternalMessage;
import org.mule.runtime.core.component.ComponentException;
import org.mule.runtime.core.exception.AbstractMessagingExceptionStrategy;
import org.mule.runtime.core.exception.MessagingException;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.test.AbstractIntegrationTestCase;

import org.hamcrest.CoreMatchers;
import org.junit.Rule;
import org.junit.Test;

public class ExceptionStrategyCommonScenariosTestCase extends AbstractIntegrationTestCase {

  public static final String MESSAGE_TO_SEND = "A message";
  public static final String MESSAGE_MODIFIED = "A message with some text added";
  public static final int TIMEOUT = 5000;

  @Rule
  public DynamicPort dynamicPort1 = new DynamicPort("port1");

  @Rule
  public DynamicPort dynamicPort2 = new DynamicPort("port2");

  @Rule
  public DynamicPort dynamicPort3 = new DynamicPort("port3");

  @Rule
  public DynamicPort dynamicPort4 = new DynamicPort("port4");

  @Rule
  public DynamicPort dynamicPort5 = new DynamicPort("port5");

  @Rule
  public DynamicPort dynamicPort6 = new DynamicPort("port6");


  @Rule
  public DynamicPort dynamicPort7 = new DynamicPort("port7");

  @Override
  protected String getConfigFile() {
    return "org/mule/test/integration/exceptions/exception-strategy-common-scenarios-flow.xml";
  }

  @Test
  public void testPreservePayloadExceptionStrategy() throws Exception {
    try {
      flowRunner("PreservePayloadExceptionStrategy").withPayload(MESSAGE_TO_SEND).run();
    } catch (MessagingException e) {
      assertThat(e.getCause(), CoreMatchers.is(instanceOf(ComponentException.class)));
      assertThat(e.getEvent().getMessage(), notNullValue());
      assertThat(getPayloadAsString(e.getEvent().getMessage()), is(MESSAGE_MODIFIED));
    }
  }


  public static class PreservePayloadExceptionStrategy extends AbstractMessagingExceptionStrategy {

    public PreservePayloadExceptionStrategy() {}

    @Override
    public Event handleException(MessagingException e, Event event) {
      return Event.builder(super.handleException(e, event))
          .message(InternalMessage.builder(event.getMessage()).payload(event.getMessage().getPayload().getValue()).build())
          .build();
    }
  }
}
