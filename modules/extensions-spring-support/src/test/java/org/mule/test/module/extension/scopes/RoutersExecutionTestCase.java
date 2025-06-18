/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension.scopes;

import static org.mule.test.allure.AllureConstants.Sdk.Components.SDK_ROUTERS_LIST_OF_ROUTES;
import static org.mule.test.heisenberg.extension.HeisenbergOperationLifecycleValidator.DISPOSE_CALL_COUNT;
import static org.mule.test.heisenberg.extension.HeisenbergOperationLifecycleValidator.INITIALIZE_CALL_COUNT;
import static org.mule.test.heisenberg.extension.HeisenbergOperationLifecycleValidator.START_CALL_COUNT;
import static org.mule.test.heisenberg.extension.HeisenbergOperationLifecycleValidator.STOP_CALL_COUNT;

import static java.util.concurrent.Executors.newFixedThreadPool;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNot.not;

import static org.junit.Assert.assertThrows;
import static org.junit.Assert.fail;

import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.event.Event;
import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.api.lifecycle.Stoppable;
import org.mule.runtime.api.util.Reference;
import org.mule.runtime.api.util.concurrent.Latch;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.test.heisenberg.extension.model.Ricin;
import org.mule.test.module.extension.AbstractExtensionFunctionalTestCase;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import io.qameta.allure.Feature;

public class RoutersExecutionTestCase extends AbstractExtensionFunctionalTestCase {

  private static final String KILL_REASON = "I'm the one who knocks";

  @Rule
  public SystemProperty maxRedelivery = new SystemProperty("killingReason", KILL_REASON);

  private ExecutorService executor = null;

  @Override
  protected String[] getConfigFiles() {
    return new String[] {"scopes/heisenberg-router-config.xml"};
  }

  @Override
  protected boolean isDisposeContextPerClass() {
    return true;
  }

  @Before
  public void before() {
    resetExtensionLifecycleCounters();
  }

  @After
  public void after() {
    if (executor != null) {
      executor.shutdownNow();
    }
  }

  @Test
  public void voidRouter() throws Exception {
    CoreEvent internalEvent = flowRunner("voidRouter").withPayload("message").withAttributes("other").run();

    assertThat(internalEvent.getMessage().getPayload().getValue(), is("message"));
    assertThat(internalEvent.getMessage().getAttributes().getValue(), is("other"));
    assertThat(internalEvent.getVariables().get("newAttributes"), is(nullValue()));
  }

  @Test
  public void sdkVoidRouter() throws Exception {
    CoreEvent internalEvent = flowRunner("sdkVoidRouter").withPayload("message").withAttributes("other").run();

    assertThat(internalEvent.getMessage().getPayload().getValue(), is("message"));
    assertThat(internalEvent.getMessage().getAttributes().getValue(), is("other"));
    assertThat(internalEvent.getVariables().get("newAttributes"), is(nullValue()));
  }

  @Test
  public void fieldParameterInjection() throws Exception {
    Integer value = (Integer) flowRunner("routerField")
        .withVariable("expected", 0)
        .withVariable("newValue", 1)
        .run().getMessage().getPayload().getValue();
    assertThat(value, is(1));

    value = (Integer) flowRunner("routerField")
        .withVariable("expected", 1)
        .withVariable("newValue", 5)
        .run().getMessage().getPayload().getValue();
    assertThat(value, is(5));
  }

  @Test
  public void singleRouteRouter() throws Exception {
    CoreEvent internalEvent = flowRunner("singleRouteRouter").withPayload("message").withAttributes("other").run();

    assertThat(internalEvent.getMessage().getPayload().getValue(), is("message"));
    assertThat(internalEvent.getVariables().get("newPayload").getValue(), is("message"));
    assertThat(internalEvent.getVariables().get("newAttributes").getValue(), is("other"));
  }

  /**
   * Executes once a router that internally calls concurrently the same Chain
   */
  @Test
  public void concurrentRouter() throws Exception {
    CoreEvent internalEvent = flowRunner("concurrentRouteExecutor").run();

    assertThat(internalEvent.getMessage().getPayload().getValue(), is("SUCCESS"));
  }

  /**
   * Executes the same flow concurrently to check that no race condition exists because two different instances of Chain are being
   * used
   */
  @Test
  public void concurrentRouterExecution() throws Exception {
    executor = newFixedThreadPool(2);

    final Latch beginLatch = new Latch();
    final CountDownLatch assertLatch = new CountDownLatch(2);
    final Consumer<Reference<CoreEvent>> runner = reference -> {
      try {
        beginLatch.await(10000, MILLISECONDS);
        reference.set(flowRunner("singleRouteRouter")
            .withPayload("CustomPayload")
            .run());
        assertLatch.countDown();
      } catch (Exception e) {
        fail(e.getMessage());
      }
    };

    final Reference<CoreEvent> first = new Reference<>();
    final Reference<CoreEvent> second = new Reference<>();

    executor.submit(() -> runner.accept(first));
    executor.submit(() -> runner.accept(second));

    beginLatch.release();
    assertLatch.await(10000, MILLISECONDS);

    CoreEvent firstResult = first.get();
    assertThat(firstResult, is(notNullValue()));
    CoreEvent secondResult = second.get();
    assertThat(secondResult, is(notNullValue()));

    assertThat(secondResult, is(not(sameInstance(firstResult))));

    assertThat(firstResult.getMessage().getPayload().getValue(), is("CustomPayload"));
    assertThat(secondResult.getMessage().getPayload().getValue(), is("CustomPayload"));
  }

  @Test
  public void twoRoutesRouterWhen() throws Exception {
    CoreEvent internalEvent = flowRunner("twoRoutesRouter")
        .withVariable("executeWhen", true)
        .withVariable("executeOther", false).run();

    assertThat(internalEvent.getMessage().getPayload().getValue(), is("mule:set-payload"));
    assertThat(internalEvent.getVariables().get("newPayload").getValue(), is("mule:set-payload"));
  }

  @Test
  public void twoRoutesRouterOther() throws Exception {
    CoreEvent internalEvent = flowRunner("twoRoutesRouter")
        .withVariable("executeWhen", false)
        .withVariable("executeOther", true).run();

    assertThat(internalEvent.getMessage().getPayload().getValue(), is("mule:set-payload"));
    assertThat(internalEvent.getVariables().get("newPayload"), is(nullValue()));
    assertThat(internalEvent.getVariables().get("newAttributes"), is(nullValue()));
  }

  @Test
  public void twoRoutesRouterNone() {
    var thrown = assertThrows(Exception.class, () -> runFlow("twoRoutesRouterNone"));
    assertThat(thrown.getCause(), instanceOf(ConnectionException.class));
    assertThat(thrown.getMessage(), containsString("No route executed"));
  }

  @Test
  @Feature(SDK_ROUTERS_LIST_OF_ROUTES)
  public void listOfRoutesRouterWhen() throws Exception {
    CoreEvent internalEvent = flowRunner("listOfRoutesRouter")
        .withVariable("executeWhen1", true)
        .withVariable("executeWhen2", false)
        .withVariable("executeWhen3", true)
        .withVariable("executeOther", false)
        .run();

    assertThat(internalEvent.getMessage().getPayload().getValue(), is("mule:set-payload"));
    assertThat(internalEvent.getVariables().get("newPayload1").getValue(), is("mule:set-payload"));
    assertThat(internalEvent.getVariables().get("newPayload2"), is(nullValue()));
    assertThat(internalEvent.getVariables().get("newPayload3"), is(nullValue()));
  }

  @Test
  @Feature(SDK_ROUTERS_LIST_OF_ROUTES)
  public void listOfRoutesRouterOther() throws Exception {
    CoreEvent internalEvent = flowRunner("listOfRoutesRouter")
        .withVariable("executeWhen1", false)
        .withVariable("executeWhen2", false)
        .withVariable("executeWhen3", false)
        .withVariable("executeOther", true).run();

    assertThat(internalEvent.getMessage().getPayload().getValue(), is("mule:set-payload"));
    assertThat(internalEvent.getVariables().get("newPayload1"), is(nullValue()));
    assertThat(internalEvent.getVariables().get("newPayload2"), is(nullValue()));
    assertThat(internalEvent.getVariables().get("newPayload3"), is(nullValue()));
    assertThat(internalEvent.getVariables().get("newAttributes1"), is(nullValue()));
    assertThat(internalEvent.getVariables().get("newAttributes2"), is(nullValue()));
    assertThat(internalEvent.getVariables().get("newAttributes3"), is(nullValue()));
  }

  @Test
  public void stereotypedRoutes() throws Exception {
    Event routeEvent = flowRunner("stereotypedRoutes").run();
    Ricin ricin = ((List<Ricin>) routeEvent.getMessage().getPayload().getValue()).get(0);
    assertThat(ricin.getDestination().getVictim(), is("bye bye, someName"));
  }

  @Test
  public void munitSpy() throws Exception {
    CoreEvent internalEvent = flowRunner("munitSpy").run();
    assertThat(internalEvent.getVariables().get("before").getValue(), is("true"));
    assertThat(internalEvent.getVariables().get("after").getValue(), is("true"));
  }

  @Test
  public void munitSpyNoBefore() throws Exception {
    CoreEvent internalEvent = flowRunner("munitSpyNoBefore").run();
    assertThat(internalEvent.getMessage().getPayload().getValue(), is("1"));
    assertThat(internalEvent.getVariables().get("before"), is(nullValue()));
    assertThat(internalEvent.getVariables().get("after").getValue(), is("true"));
  }

  @Test
  public void munitSpyNoAfter() throws Exception {
    CoreEvent internalEvent = flowRunner("munitSpyNoAfter").run();
    assertThat(internalEvent.getMessage().getPayload().getValue(), is("2"));
    assertThat(internalEvent.getVariables().get("before").getValue(), is("true"));
    assertThat(internalEvent.getVariables().get("after"), is(nullValue()));
  }

  @Test
  public void munitSpyNoAttributes() throws Exception {
    CoreEvent internalEvent = flowRunner("munitSpyNoAttributes").run();
    assertThat(internalEvent.getMessage().getPayload().getValue(), is(nullValue()));
    assertThat(internalEvent.getVariables().get("before"), is(nullValue()));
    assertThat(internalEvent.getVariables().get("after"), is(nullValue()));
  }

  @Test
  public void twoRoutesRouterLazilyStarted() throws Exception {
    CoreEvent internalEvent = flowRunner("twoRoutesRouterLazilyStarted")
        .withVariable("executeWhen", true)
        .withVariable("executeOther", false).run();

    assertThat(internalEvent.getMessage().getPayload().getValue(), is("mule:set-payload"));
    assertThat(internalEvent.getVariables().get("newPayload").getValue(), is("mule:set-payload"));
  }

  @Test
  public void twoRoutesRouterWithCustomOperationLazilyStarted() throws Exception {
    CoreEvent internalEvent = flowRunner("twoRoutesRouterWithCustomOperationLazilyStarted")
        .withVariable("executeWhen", true)
        .withVariable("executeOther", false).run();

    assertThat(internalEvent.getMessage().getPayload().getValue(), is("mule:set-payload"));
    assertThat(internalEvent.getVariables().get("newPayload").getValue(), is("mule:set-payload"));
  }

  @Test
  public void routerLifecycleIsAttachedToFlowLifecycle() throws Exception {
    FlowConstruct flow = getFlowConstruct("routerLifecycleIsAttachedToFlowLifecycle");
    assertThat(flow.getLifecycleState().isStopped(), is(true));

    resetExtensionLifecycleCounters();
    ((Startable) flow).start();
    assertLifecycleMethodsCalls(0, 2, 0, 0);

    resetExtensionLifecycleCounters();
    ((Stoppable) flow).stop();
    assertLifecycleMethodsCalls(0, 0, 2, 0);

    resetExtensionLifecycleCounters();
    ((Disposable) flow).dispose();
    assertLifecycleMethodsCalls(0, 0, 0, 2);
  }

  private void assertLifecycleMethodsCalls(int initializeCalls, int startCalls, int stopCalls, int disposeCalls) {
    assertThat(INITIALIZE_CALL_COUNT, is(initializeCalls));
    assertThat(START_CALL_COUNT, is(startCalls));
    assertThat(STOP_CALL_COUNT, is(stopCalls));
    assertThat(DISPOSE_CALL_COUNT, is(disposeCalls));
  }

  private void resetExtensionLifecycleCounters() {
    INITIALIZE_CALL_COUNT = START_CALL_COUNT = STOP_CALL_COUNT = DISPOSE_CALL_COUNT = 0;
  }
}
