/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.routing;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import org.mule.runtime.core.exception.MessagingException;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleMessage;

import java.util.List;

import org.junit.Test;

public class DynamicAllTestCase extends DynamicRouterTestCase {

  private static final String DYNAMIC_ALL = "dynamicAll";

  @Override
  protected String getConfigFile() {
    return "org/mule/test/integration/routing/dynamic-all-config.xml";
  }

  @Override
  public String getFlowName() {
    return "dynamicAll";
  }

  @Test
  public void withRoutes() throws Exception {
    CustomRouteResolver.routes.add(new CustomRouteResolver.AddLetterMessageProcessor(LETTER_A));
    CustomRouteResolver.routes.add(new CustomRouteResolver.AddLetterMessageProcessor(LETTER_B));
    runFlowAndAssertResponse(DYNAMIC_ALL, LETTER_A, LETTER_B);
  }

  @Test(expected = MessagingException.class)
  public void worksWithFirstFailingRouteAndSecondGood() throws Exception {
    CustomRouteResolver.routes.add(new CustomRouteResolver.FailingMessageProcessor());
    CustomRouteResolver.routes.add(new CustomRouteResolver.AddLetterMessageProcessor(LETTER_B));
    runFlowAndAssertResponse(DYNAMIC_ALL, DOES_NOT_MATTER);
  }

  @Test(expected = MessagingException.class)
  public void worksWithFirstRouteGoodAndSecondFails() throws Exception {
    CustomRouteResolver.routes.add(new CustomRouteResolver.FailingMessageProcessor());
    CustomRouteResolver.routes.add(new CustomRouteResolver.AddLetterMessageProcessor(LETTER_B));
    runFlowAndAssertResponse(DYNAMIC_ALL, DOES_NOT_MATTER);
  }

  @Test
  public void oneRoute() throws Exception {
    CustomRouteResolver.routes.add(new CustomRouteResolver.AddLetterMessageProcessor(LETTER_A));
    MuleEvent result = flowRunner(DYNAMIC_ALL).withPayload(TEST_MESSAGE).run();
    assertThat(getPayloadAsString(result.getMessage()), is(LETTER_A));
  }

  @Test
  public void oneRouteWithCustomResultAggregator() throws Exception {
    CustomRouteResolver.routes.add(new CustomRouteResolver.AddLetterMessageProcessor(LETTER_A));
    runFlowAndAssertResponse("dynamicAllResultAggregator", (Object) TEST_MESSAGE, LETTER_A);
  }

  private MuleEvent runFlowAndAssertResponse(String flowName, String... letters) throws Exception {
    return runFlowAndAssertResponse(flowName, TEST_MESSAGE, letters);
  }

  private MuleEvent runFlowAndAssertResponse(String flowName, Object payload, String... letters) throws Exception {
    MuleEvent resultEvent = flowRunner(flowName).withPayload(payload).run();
    MuleMessage messageCollection = resultEvent.getMessage();
    for (int i = 0; i < letters.length; i++) {
      MuleMessage message = ((List<MuleMessage>) messageCollection.getPayload()).get(i);
      assertThat(getPayloadAsString(message), is(letters[i]));
    }
    return resultEvent;
  }

}
