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
import org.mule.runtime.core.construct.Flow;
import org.mule.test.AbstractIntegrationTestCase;

import org.junit.Before;
import org.junit.Test;

public abstract class DynamicRouterTestCase extends AbstractIntegrationTestCase {

  protected static final String LETTER_A = "a";
  protected static final String LETTER_B = "b";
  protected static final String LETTER_C = "c";
  protected static final String LETTER_D = "d";
  protected static final String DOES_NOT_MATTER = "doesnotmatter";

  @Before
  public void clearRoutes() {
    CustomRouteResolver.routes.clear();
  }

  @Test(expected = MessagingException.class)
  public void noRoutes() throws Exception {
    flowRunner(getFlowName()).withPayload(TEST_MESSAGE).run();
  }

  public abstract String getFlowName();

  protected MuleEvent runFlowAndAssertResponse(String flowName, Object expectedMessage) throws Exception {
    MuleEvent event = flowRunner(flowName).withPayload(TEST_MESSAGE).run();
    assertThat(event.getMessageAsString(muleContext), is(expectedMessage));
    return event;
  }

  protected Flow getTestFlow(String flow) throws Exception {
    return (Flow) getFlowConstruct(flow);
  }
}
