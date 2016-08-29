/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.exceptions;

import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThat;
import org.mule.runtime.core.exception.MessagingException;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.exception.MessagingExceptionHandler;
import org.mule.runtime.core.component.ComponentException;
import org.mule.runtime.core.exception.AbstractExceptionListener;
import org.mule.runtime.core.exception.ErrorHandler;
import org.mule.test.AbstractIntegrationTestCase;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Ignore;
import org.junit.Test;

public class ReferenceExceptionStrategyTestCase extends AbstractIntegrationTestCase {

  public static final int TIMEOUT = 5000;
  public static final String JSON_RESPONSE =
      "{\"errorMessage\":\"error processing news\",\"userId\":15,\"title\":\"News title\"}";
  public static final String JSON_REQUEST = "{\"userId\":\"15\"}";

  @Override
  protected String getConfigFile() {
    return "org/mule/test/integration/exceptions/reference-flow-exception-strategy.xml";
  }

  @Test
  public void testFlowUsingGlobalExceptionStrategy() throws Exception {
    MuleMessage response = flowRunner("referenceExceptionStrategyFlow").withPayload(JSON_REQUEST).run().getMessage();
    assertThat(response, notNullValue());
    // compare the structure and values but not the attributes' order
    ObjectMapper mapper = new ObjectMapper();
    JsonNode actualJsonNode = mapper.readTree(getPayloadAsString(response));
    JsonNode expectedJsonNode = mapper.readTree(JSON_RESPONSE);
    assertThat(actualJsonNode, is(expectedJsonNode));
  }

  @Test
  public void testFlowUsingConfiguredExceptionStrategy() throws Exception {
    MessagingException e = flowRunner("configuredExceptionStrategyFlow").withPayload(JSON_REQUEST).runExpectingException();
    assertThat(e, instanceOf(ComponentException.class));
    assertThat(e.getEvent().getMessage(), notNullValue());
    assertThat(e.getEvent().getMessage().getPayload(), is(nullValue()));
    assertThat(e.getEvent().getError(), notNullValue());
  }

  @Test
  @Ignore("MULE-10323 Define handlers reutilisation (refs)")
  public void testTwoFlowsReferencingSameExceptionStrategyGetDifferentInstances() {
    MessagingExceptionHandler firstExceptionStrategy =
        muleContext.getRegistry().lookupFlowConstruct("otherFlowWithSameReferencedExceptionStrategy").getExceptionListener();
    MessagingExceptionHandler secondExceptionStrategy =
        muleContext.getRegistry().lookupFlowConstruct("referenceExceptionStrategyFlow").getExceptionListener();
    assertThat(firstExceptionStrategy, not(sameInstance(secondExceptionStrategy)));
  }

  @Test
  public void testTwoFlowsReferencingDifferentExceptionStrategy() {
    MessagingExceptionHandler firstExceptionStrategy =
        muleContext.getRegistry().lookupFlowConstruct("otherFlowWithSameReferencedExceptionStrategy").getExceptionListener();
    MessagingExceptionHandler secondExceptionStrategy =
        muleContext.getRegistry().lookupFlowConstruct("anotherFlowUsingDifferentExceptionStrategy").getExceptionListener();
    assertThat(firstExceptionStrategy, not(secondExceptionStrategy));
    AbstractExceptionListener exceptionListener =
        (AbstractExceptionListener) ((ErrorHandler) firstExceptionStrategy).getExceptionListeners().get(0);
    assertThat(exceptionListener.getMessageProcessors().size(), is(2));
    assertThat(secondExceptionStrategy, instanceOf(ErrorHandler.class));
  }

}
