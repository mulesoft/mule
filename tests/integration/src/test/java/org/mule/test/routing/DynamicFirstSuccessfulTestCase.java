/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.routing;

import org.mule.runtime.core.exception.MessagingException;

import org.junit.Test;

public class DynamicFirstSuccessfulTestCase extends DynamicRouterTestCase {

  private static final String DYNAMIC_FIRST_SUCCESSFUL = "dynamicFirstSuccessful";
  private static final String DYNAMIC_FIRST_SUCCESSFUL_WITH_EXPRESSION = "dynamicFirstSuccessfulWithExpression";
  private static final String LETTER_F = "f";
  private static final String RANDOM_TEXT_1 = "fafa";
  private static final String RANDOM_TEXT_2 = "fofo";

  @Override
  protected String getConfigFile() {
    return "org/mule/test/integration/routing/dynamic-first-successful-config.xml";
  }

  @Override
  public String getFlowName() {
    return DYNAMIC_FIRST_SUCCESSFUL;
  }

  @Test
  public void withRoutes() throws Exception {
    CustomRouteResolver.routes.add(new CustomRouteResolver.AddLetterMessageProcessor(LETTER_A));
    CustomRouteResolver.routes.add(new CustomRouteResolver.AddLetterMessageProcessor(LETTER_B));
    runFlowAndAssertResponse(DYNAMIC_FIRST_SUCCESSFUL, LETTER_A);
  }

  @Test
  public void worksWithFirstFailingRouteAndSecondGood() throws Exception {
    CustomRouteResolver.routes.add(new CustomRouteResolver.FailingMessageProcessor());
    CustomRouteResolver.routes.add(new CustomRouteResolver.AddLetterMessageProcessor(LETTER_B));
    runFlowAndAssertResponse(DYNAMIC_FIRST_SUCCESSFUL, LETTER_B);
  }

  @Test(expected = MessagingException.class)
  public void worksWithAllFailingProcessor() throws Exception {
    CustomRouteResolver.routes.add(new CustomRouteResolver.FailingMessageProcessor());
    CustomRouteResolver.routes.add(new CustomRouteResolver.FailingMessageProcessor());
    runFlowAndAssertResponse(DYNAMIC_FIRST_SUCCESSFUL, LETTER_B);
  }

  @Test
  public void allRoutesReceiveSameMessage() throws Exception {
    CustomRouteResolver.routes.add(new CustomRouteResolver.AddLetterThenFailsMessageProcessor(LETTER_A));
    CustomRouteResolver.routes.add(new CustomRouteResolver.AddLetterMessageProcessor(LETTER_B));
    runFlowAndAssertResponse(DYNAMIC_FIRST_SUCCESSFUL, LETTER_B);
  }

  @Test
  public void failureExpressionNotFailingNotMatchingExpression() throws Exception {
    CustomRouteResolver.routes.add(new CustomRouteResolver.AddLetterMessageProcessor(LETTER_A));
    CustomRouteResolver.routes.add(new CustomRouteResolver.AddLetterMessageProcessor(LETTER_B));
    runFlowAndAssertResponse(DYNAMIC_FIRST_SUCCESSFUL_WITH_EXPRESSION, LETTER_A);
  }

  @Test
  public void failureExpressionNotFailingButMatchingExpression() throws Exception {
    CustomRouteResolver.routes.add(new CustomRouteResolver.AddLetterMessageProcessor(LETTER_F));
    CustomRouteResolver.routes.add(new CustomRouteResolver.AddLetterMessageProcessor(LETTER_B));
    runFlowAndAssertResponse(DYNAMIC_FIRST_SUCCESSFUL_WITH_EXPRESSION, LETTER_B);
  }

  @Test(expected = MessagingException.class)
  public void failureExpressionFailingAndMatchingExpression() throws Exception {
    CustomRouteResolver.routes.add(new CustomRouteResolver.FailingMessageProcessor());
    CustomRouteResolver.routes.add(new CustomRouteResolver.AddLetterMessageProcessor(LETTER_F));
    runFlowAndAssertResponse(DYNAMIC_FIRST_SUCCESSFUL_WITH_EXPRESSION, DOES_NOT_MATTER);
  }

  @Test(expected = MessagingException.class)
  public void allFailingExpression() throws Exception {
    CustomRouteResolver.routes.add(new CustomRouteResolver.AddLetterMessageProcessor(RANDOM_TEXT_1));
    CustomRouteResolver.routes.add(new CustomRouteResolver.AddLetterMessageProcessor(RANDOM_TEXT_2));
    runFlowAndAssertResponse(DYNAMIC_FIRST_SUCCESSFUL_WITH_EXPRESSION, DOES_NOT_MATTER);
  }

}
