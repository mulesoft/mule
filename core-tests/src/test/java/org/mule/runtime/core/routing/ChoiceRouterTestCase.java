/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.routing;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.rules.ExpectedException.none;
import static org.mule.runtime.api.message.Message.of;
import static org.mule.runtime.core.api.management.stats.RouterStatistics.TYPE_OUTBOUND;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.management.stats.RouterStatistics;
import org.mule.tck.junit4.AbstractReactiveProcessorTestCase;
import org.mule.tck.testmodels.mule.TestMessageProcessor;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class ChoiceRouterTestCase extends AbstractReactiveProcessorTestCase {

  @Rule
  public ExpectedException thrown = none();

  private ChoiceRouter choiceRouter;

  public ChoiceRouterTestCase(Mode mode) {
    super(mode);
  }

  @Override
  protected void doSetUp() throws Exception {
    super.doSetUp();
    choiceRouter = new ChoiceRouter();
  }

  @Test
  public void testNoRoute() throws Exception {
    Event inputEvent = fooEvent();
    assertThat(process(choiceRouter, inputEvent), is(inputEvent));
  }

  @Test
  public void testOnlyDefaultRoute() throws Exception {
    choiceRouter.setDefaultRoute(new TestMessageProcessor("default"));
    assertThat(process(choiceRouter, fooEvent()).getMessage().getPayload().getValue(), is("foo:default"));
  }

  @Test
  public void testNoMatchingNorDefaultRoute() throws Exception {
    choiceRouter.addRoute(payloadZapExpression(), new TestMessageProcessor("bar"));
    Event inputEvent = fooEvent();
    assertThat(process(choiceRouter, inputEvent), is(inputEvent));
  }

  @Test
  public void testNoMatchingRouteWithDefaultRoute() throws Exception {
    choiceRouter.addRoute(payloadZapExpression(), new TestMessageProcessor("bar"));
    choiceRouter.setDefaultRoute(new TestMessageProcessor("default"));
    assertThat(process(choiceRouter, fooEvent()).getMessage().getPayload().getValue(), is("foo:default"));
  }

  @Test
  public void testMatchingRouteWithDefaultRoute() throws Exception {
    choiceRouter.addRoute(payloadZapExpression(), new TestMessageProcessor("bar"));
    choiceRouter.setDefaultRoute(new TestMessageProcessor("default"));
    assertThat(process(choiceRouter, zapEvent()).getMessage().getPayload().getValue(), is("zap:bar"));
  }

  @Test
  public void testMatchingRouteWithStatistics() throws Exception {
    choiceRouter.addRoute(payloadZapExpression(), new TestMessageProcessor("bar"));
    choiceRouter.setRouterStatistics(new RouterStatistics(TYPE_OUTBOUND));
    assertThat(process(choiceRouter, zapEvent()).getMessage().getPayload().getValue(), is("zap:bar"));
  }

  @Test
  public void testAddAndDeleteRoute() throws Exception {
    TestMessageProcessor mp = new TestMessageProcessor("bar");
    choiceRouter.addRoute(payloadZapExpression(), mp);
    choiceRouter.removeRoute(mp);
    choiceRouter.setRouterStatistics(new RouterStatistics(TYPE_OUTBOUND));

    Event inputEvent = zapEvent();
    assertThat(process(choiceRouter, inputEvent), is(inputEvent));
  }

  @Test
  public void testUpdateRoute() throws Exception {
    TestMessageProcessor mp = new TestMessageProcessor("bar");
    choiceRouter.addRoute(payloadPazExpression(), mp);
    choiceRouter.updateRoute(payloadZapExpression(), mp);
    assertThat(process(choiceRouter, zapEvent()).getMessage().getPayload().getValue(), is("zap:bar"));
  }

  protected Event fooEvent() throws MuleException {
    return eventBuilder().message(of("foo")).build();
  }

  protected Event zapEvent() throws MuleException {
    return eventBuilder().message(of("zap")).build();
  }

  @Test
  public void testRemovingUpdatingMissingRoutes() {
    choiceRouter.updateRoute(payloadZapExpression(), new TestMessageProcessor("bar"));
    choiceRouter.removeRoute(new TestMessageProcessor("rab"));
  }

  public String payloadZapExpression() {
    return "payload == 'zap'";
  }

  public String payloadPazExpression() {
    return "payload == 'paz'";
  }
}

