/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension.nb;

import static java.lang.Thread.currentThread;
import static java.util.Optional.empty;
import static java.util.concurrent.ConcurrentHashMap.newKeySet;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.functional.api.exception.ExpectedError.none;
import static org.mule.functional.api.flow.TransactionConfigEnum.ACTION_ALWAYS_BEGIN;
import static org.mule.functional.junit4.matchers.ThrowableMessageMatcher.hasMessage;
import static org.mule.runtime.module.extension.api.util.MuleExtensionUtils.getInitialiserEvent;
import static org.mule.tck.probe.PollingProber.probe;
import static org.mule.test.marvel.ironman.IronManOperations.FLIGHT_PLAN;
import static org.mule.test.marvel.model.MissileProofVillain.MISSILE_PROOF;
import static org.mule.test.marvel.model.Villain.KABOOM;
import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.getConfigurationInstanceFromRegistry;

import io.qameta.allure.Description;
import org.hamcrest.Matcher;
import org.junit.After;
import org.junit.Before;
import org.mule.functional.api.exception.ExpectedError;
import org.mule.runtime.api.component.AbstractComponent;
import org.mule.runtime.api.component.location.Location;
import org.mule.runtime.api.event.Event;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.tx.TransactionException;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.transaction.Transaction;
import org.mule.runtime.core.api.transaction.TransactionCoordination;
import org.mule.runtime.core.privileged.event.BaseEventContext;
import org.mule.runtime.core.privileged.transaction.TransactionAdapter;
import org.mule.tck.testmodels.mule.TestTransactionFactory;
import org.mule.test.marvel.ironman.IronMan;
import org.mule.test.marvel.model.MissileProofVillain;
import org.mule.test.marvel.model.Villain;
import org.mule.test.module.extension.AbstractExtensionFunctionalTestCase;

import java.util.HashSet;
import java.util.Set;

import io.qameta.allure.Issue;
import org.junit.Rule;
import org.junit.Test;

public class NonBlockingOperationsTestCase extends AbstractExtensionFunctionalTestCase {

  @Rule
  public ExpectedError expectedException = none();

  @Override
  protected String getConfigFile() {
    return "iron-man-config.xml";
  }

  @Before
  public void setUp() {
    ThreadCaptor.setCapturedThreads(newKeySet());
  }

  @After
  public void tearDown() {
    ThreadCaptor.setCapturedThreads(null);
  }

  @Test
  public void nonBlockingConnectedOperation() throws Exception {
    fireMissileAndAssert("fireMissile");
    assertCapturedThreadsNameMatch(startsWith("SimpleUnitTestSupportScheduler."));
  }

  @Test
  @Issue("MULE-18124")
  public void failingNonBlockingConnectedOperationThrownInsteadOfCallback() throws Exception {
    flowRunner("fireMissileMishap")
        .withPayload(new Villain())
        .runExpectingException(hasMessage("Ultron jammed the missile system!"));
  }

  @Test
  public void failingNonBlockingConnectedOperation() throws Exception {
    Villain villain = new MissileProofVillain();

    try {
      flowRunner("fireMissile").withPayload(villain).run();
    } catch (Exception e) {
      assertThat(villain.isAlive(), is(true));
      assertCapturedThreadsNameMatch(startsWith("SimpleUnitTestSupportScheduler."));

      expectedException.expectFailingComponent(is(locator
          .find(Location.builder().globalName("fireMissile").addProcessorsPart()
              .addIndexPart(0)
              .addProcessorsPart()
              .addIndexPart(0)
              .build())
          .get()));
      expectedException.expectMessage(is(MISSILE_PROOF));
      expectedException.expectCause(instanceOf(UnsupportedOperationException.class));

      throw e;
    }
  }

  @Test
  @Issue("MULE-19537")
  @Description("Tests that when a transaction is active in a thread obtained through a custom scheduler, no thread " +
      "switch takes place when an error occurs")
  public void failingNonBlockingConnectedOperationInsideTransaction() throws Exception {
    Villain villain = new MissileProofVillain();
    Transaction transaction = createTransactionMock();

    try {
      flowRunner("nonBlockingOperationFailureInsideTransaction")
          .transactionally(ACTION_ALWAYS_BEGIN, new TestTransactionFactory(transaction)).withPayload(villain).run();
    } catch (Exception e) {
      assertThat(ThreadCaptor.capturedThreads, hasSize(1));
      assertCapturedThreadsNameMatch(not(startsWith("SimpleUnitTestSupportScheduler.")));

      expectedException.expectMessage(is(MISSILE_PROOF));
      expectedException.expectCause(instanceOf(UnsupportedOperationException.class));

      throw e;
    } finally {
      TransactionCoordination.getInstance().unbindTransaction(transaction);
    }
  }

  @Test
  public void nonBlockingOperationReconnection() throws Exception {
    fireMissileAndAssert("warMachineFireMissile");
    IronMan warMachine = getIronMan("warMachine");
    assertThat(warMachine.getMissilesFired(), is(2));
    assertCapturedThreadsNameMatch(startsWith("SimpleUnitTestSupportScheduler."));
  }

  @Test
  public void voidNonBlockingOperation() throws Exception {
    IronMan ironMan = getIronMan("ironMan");
    final String payload = "take me to the avengers tower";
    Event event = flowRunner("computeFlightPlan").withPayload(payload).run();
    // TODO: MULE-18119 - uncomment the following line when this issue is fixed
    // assertCapturedThreadsNameMatch(startsWith("SimpleUnitTestSupportScheduler."));
    assertThat(event.getMessage().getPayload().getValue().toString(), equalTo(payload));
    probe(1000, 1000, () -> FLIGHT_PLAN.equals(ironMan.getFlightPlan()));
  }

  private IronMan getIronMan(String name) {
    CoreEvent initialiserEvent = null;
    try {
      initialiserEvent = getInitialiserEvent(muleContext);
      return (IronMan) getConfigurationInstanceFromRegistry(name, initialiserEvent, muleContext).getValue();
    } finally {
      if (initialiserEvent != null) {
        ((BaseEventContext) initialiserEvent.getContext()).success();
      }
    }
  }

  private void fireMissileAndAssert(String flowName) throws Exception {
    Villain villain = new Villain();
    String result = (String) flowRunner(flowName)
        .withPayload(villain)
        .run().getMessage().getPayload().getValue();

    assertThat(villain.isAlive(), is(false));
    assertThat(result, is(KABOOM));
  }

  private Transaction createTransactionMock() throws TransactionException {
    TransactionAdapter transaction = mock(TransactionAdapter.class);
    doAnswer((invocationOnMock -> {
      TransactionCoordination.getInstance().bindTransaction(transaction);
      return null;
    })).when(transaction).begin();
    when(transaction.getComponentLocation()).thenReturn(empty());
    return transaction;
  }

  private void assertCapturedThreadsNameMatch(Matcher<String> matcher) {
    for (Thread thread : ThreadCaptor.getCapturedThreads()) {
      assertThat(thread.getName(), matcher);
    }
  }

  public static class ThreadCaptor extends AbstractComponent implements Processor {

    private static Set<Thread> capturedThreads = new HashSet<>();

    @Override
    public CoreEvent process(CoreEvent event) throws MuleException {
      capturedThreads.add(currentThread());

      return event;
    }

    public static void setCapturedThreads(Set<Thread> capturedThreads) {
      ThreadCaptor.capturedThreads = capturedThreads;
    }

    public static Set<Thread> getCapturedThreads() {
      return capturedThreads;
    }
  }

}
