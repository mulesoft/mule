/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension.scopes;

import static java.util.concurrent.Executors.newFixedThreadPool;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.event.Event;
import org.mule.runtime.api.util.Reference;
import org.mule.runtime.api.util.concurrent.Latch;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.test.module.extension.AbstractExtensionFunctionalTestCase;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class RoutersExecutionTestCase extends AbstractExtensionFunctionalTestCase {

  private static final String KILL_REASON = "I'm the one who knocks";

  @Rule
  public SystemProperty maxRedelivery = new SystemProperty("killingReason", KILL_REASON);

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Override
  protected String[] getConfigFiles() {
    return new String[] {"scopes/heisenberg-router-config.xml"};
  }

  @Override
  protected boolean isDisposeContextPerClass() {
    return true;
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
   * Executes the same flow concurrently to check that no race condition exists because
   * two different instances of Chain are being used
   */
  @Test
  public void concurrentRouterExecution() throws Exception {

    final ExecutorService executor = newFixedThreadPool(2);

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
  public void twoRoutesRouterNone() throws Exception {
    expectedException.expectCause(instanceOf(ConnectionException.class));
    expectedException.expectMessage("No route executed");
    runFlow("twoRoutesRouterNone");
  }

  @Test
  public void stereotypedRoutes() throws Exception {
    Event routeEvent = flowRunner("stereotypedRoutes").run();
    assertThat(routeEvent.getMessage().getPayload().getValue(), is("bye bye, someName"));
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
}
