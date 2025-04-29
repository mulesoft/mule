/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.routing;

import static org.mule.functional.junit4.matchers.ThrowableMessageMatcher.hasMessage;
import static org.mule.runtime.api.component.AbstractComponent.LOCATION_KEY;
import static org.mule.runtime.api.message.Message.of;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.disposeIfNeeded;
import static org.mule.runtime.core.api.management.stats.RouterStatistics.TYPE_OUTBOUND;
import static org.mule.runtime.core.privileged.processor.MessageProcessors.newChain;
import static org.mule.tck.junit4.rule.DataWeaveExpressionLanguage.dataWeaveRule;
import static org.mule.tck.processor.ContextPropagationChecker.assertContextPropagation;
import static org.mule.tck.util.MuleContextUtils.eventBuilder;
import static org.mule.test.allure.AllureConstants.RoutersFeature.ROUTERS;
import static org.mule.test.allure.AllureConstants.RoutersFeature.ChoiceStory.CHOICE;

import static java.util.Collections.singletonMap;
import static java.util.Optional.empty;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThrows;
import static org.slf4j.LoggerFactory.getLogger;

import org.mule.runtime.api.el.ExpressionExecutionException;
import org.mule.runtime.api.exception.DefaultMuleException;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.expression.ExpressionRuntimeException;
import org.mule.runtime.core.api.management.stats.RouterStatistics;
import org.mule.runtime.core.internal.profiling.DummyComponentTracerFactory;
import org.mule.runtime.core.privileged.processor.chain.MessageProcessorChain;
import org.mule.tck.junit4.AbstractReactiveProcessorTestCase;
import org.mule.tck.junit4.rule.DataWeaveExpressionLanguage;
import org.mule.tck.processor.ContextPropagationChecker;
import org.mule.tck.testmodels.mule.TestMessageProcessor;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;

import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Story;

@Feature(ROUTERS)
@Story(CHOICE)
public class ChoiceRouterTestCase extends AbstractReactiveProcessorTestCase {

  @Rule
  public DataWeaveExpressionLanguage dw = dataWeaveRule();

  private ChoiceRouter choiceRouter;

  public ChoiceRouterTestCase(Mode mode) {
    super(mode);
  }

  @Override
  protected void doSetUp() throws Exception {
    super.doSetUp();
    choiceRouter = new ChoiceRouter(new DummyComponentTracerFactory());
    choiceRouter.setAnnotations(singletonMap(LOCATION_KEY, TEST_CONNECTOR_LOCATION));
    choiceRouter.setExpressionManager(dw.getExpressionManager());
  }

  @After
  public void after() {
    disposeIfNeeded(choiceRouter, getLogger(getClass()));
  }

  @Test
  public void noRoute() throws Exception {
    initialise();

    CoreEvent inputEvent = fooEvent();
    assertThat(process(choiceRouter, inputEvent), is(inputEvent));
  }

  @Test
  public void onlyDefaultRoute() throws Exception {
    choiceRouter.setDefaultRoute(newChain(empty(), new TestMessageProcessor("default")));
    initialise();

    assertThat(process(choiceRouter, fooEvent()).getMessage().getPayload().getValue(), is("foo:default"));
  }

  @Test
  public void noMatchingNorDefaultRoute() throws Exception {
    choiceRouter.addRoute(payloadZapExpression(), newChain(empty(), new TestMessageProcessor("bar")));
    initialise();

    CoreEvent inputEvent = fooEvent();
    assertThat(process(choiceRouter, inputEvent), is(inputEvent));
  }

  @Test
  public void noMatchingRouteWithDefaultRoute() throws Exception {
    choiceRouter.addRoute(payloadZapExpression(), newChain(empty(), new TestMessageProcessor("bar")));
    choiceRouter.setDefaultRoute(newChain(empty(), new TestMessageProcessor("default")));
    initialise();

    assertThat(process(choiceRouter, fooEvent()).getMessage().getPayload().getValue(), is("foo:default"));
  }

  @Test
  public void matchingRouteWithDefaultRoute() throws Exception {
    choiceRouter.addRoute(payloadZapExpression(), newChain(empty(), new TestMessageProcessor("bar")));
    choiceRouter.setDefaultRoute(newChain(empty(), new TestMessageProcessor("default")));
    initialise();

    assertThat(process(choiceRouter, zapEvent()).getMessage().getPayload().getValue(), is("zap:bar"));
  }

  @Test
  public void matchingRouteWithStatistics() throws Exception {
    TestMessageProcessor processor = new TestMessageProcessor("bar");
    choiceRouter.addRoute(payloadZapExpression(), newChain(empty(), processor));
    RouterStatistics routerStatistics = new RouterStatistics(TYPE_OUTBOUND);
    routerStatistics.setEnabled(true);
    choiceRouter.setRouterStatistics(routerStatistics);
    initialise();

    assertThat(process(choiceRouter, zapEvent()).getMessage().getPayload().getValue(), is("zap:bar"));
    assertThat(process(choiceRouter, zapEvent()).getMessage().getPayload().getValue(), is("zap:bar"));
    assertThat(routerStatistics.getRouted(), hasEntry(containsString(processor.toString()), is((long) 2)));
  }

  @Test
  @Issue("MULE-19512")
  public void failingExpression() throws Exception {
    MessageProcessorChain mp = newChain(empty(), new TestMessageProcessor("bar"));
    choiceRouter.addRoute("wat", mp);
    initialise();

    final var zapEvent = zapEvent();
    var thrown = assertThrows(ExpressionRuntimeException.class, () -> process(choiceRouter, zapEvent));
    assertThat(thrown, hasMessage(containsString("evaluating expression: \"wat\"")));
    assertThat(thrown.getCause(), instanceOf(ExpressionExecutionException.class));
  }

  @Test
  public void failingRoute() throws Exception {
    final DefaultMuleException expected = new DefaultMuleException("Oops");

    MessageProcessorChain mp = newChain(empty(), event -> {
      throw expected;
    });
    choiceRouter.addRoute(payloadZapExpression(), mp);
    initialise();

    final var zapEvent = zapEvent();
    var thrown = assertThrows(Exception.class, () -> process(choiceRouter, zapEvent));
    assertThat(thrown, sameInstance(expected));
  }

  @Test
  public void failingRouteDefault() throws Exception {
    final DefaultMuleException expected = new DefaultMuleException("Oops");

    MessageProcessorChain mp = newChain(empty(), event -> {
      throw expected;
    });
    choiceRouter.addRoute(payloadZapExpression(), mp);
    choiceRouter.setDefaultRoute(newChain(empty(), new TestMessageProcessor("bar")));
    initialise();

    final var zapEvent = zapEvent();
    var thrown = assertThrows(Exception.class, () -> process(choiceRouter, zapEvent));
    assertThat(thrown, sameInstance(expected));
  }

  @Test
  public void subscriberContextPropagation() throws MuleException {
    final ContextPropagationChecker contextPropagationChecker = new ContextPropagationChecker();

    MessageProcessorChain mp = newChain(empty(), contextPropagationChecker);
    choiceRouter.addRoute(payloadZapExpression(), mp);
    initialise();

    assertContextPropagation(zapEvent(), choiceRouter, contextPropagationChecker);
  }

  @Test
  public void subscriberContextPropagationDefaultRoute() throws MuleException {
    final ContextPropagationChecker contextPropagationChecker = new ContextPropagationChecker();

    MessageProcessorChain mp = newChain(empty(), contextPropagationChecker);
    choiceRouter.addRoute(payloadZapExpression(), event -> event);
    choiceRouter.setDefaultRoute(mp);
    initialise();

    assertContextPropagation(fooEvent(), choiceRouter, contextPropagationChecker);
  }

  private void initialise() throws InitialisationException {
    choiceRouter.setMuleContext(muleContext);
    choiceRouter.initialise();
  }

  private CoreEvent fooEvent() throws MuleException {
    return eventBuilder(muleContext).message(of("foo")).build();
  }

  private CoreEvent zapEvent() throws MuleException {
    return eventBuilder(muleContext).message(of("zap")).build();
  }

  private String payloadZapExpression() {
    return "payload == 'zap'";
  }

}

