/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension.source;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static org.mule.functional.junit4.matchers.MessageMatchers.hasPayload;
import static org.mule.runtime.api.exception.MuleException.INFO_LOCATION_KEY;
import static org.mule.runtime.api.exception.MuleException.INFO_SOURCE_XML_KEY;
import static org.mule.runtime.core.api.error.Errors.ComponentIdentifiers.Handleable.SOURCE_ERROR_RESPONSE_GENERATE;
import static org.mule.runtime.core.api.error.Errors.ComponentIdentifiers.Handleable.SOURCE_ERROR_RESPONSE_SEND;
import static org.mule.runtime.core.api.rx.Exceptions.unwrap;
import static org.mule.tck.junit4.matcher.ErrorTypeMatcher.errorType;
import static org.mule.tck.junit4.matcher.IsEmptyOptional.empty;
import static org.mule.tck.probe.PollingProber.check;
import static org.mule.tck.probe.PollingProber.checkNot;
import static org.mule.tck.probe.PollingProber.probe;
import static org.mule.test.heisenberg.extension.HeisenbergConnectionProvider.SAUL_OFFICE_NUMBER;
import static org.mule.test.heisenberg.extension.HeisenbergExtension.sourceTimesStarted;
import static org.mule.test.heisenberg.extension.HeisenbergSource.CORE_POOL_SIZE_ERROR_MESSAGE;
import static org.mule.test.heisenberg.extension.HeisenbergSource.resetHeisenbergSource;
import static org.mule.test.heisenberg.extension.HeisenbergSource.TerminateStatus.ERROR_BODY;
import static org.mule.test.heisenberg.extension.HeisenbergSource.TerminateStatus.ERROR_INVOKE;
import static org.mule.test.heisenberg.extension.HeisenbergSource.TerminateStatus.SUCCESS;
import static org.mule.test.heisenberg.extension.exception.HeisenbergConnectionExceptionEnricher.ENRICHED_MESSAGE;
import static org.mule.test.heisenberg.extension.model.HealthStatus.CANCER;

import org.mule.runtime.api.component.Component;
import org.mule.runtime.api.component.location.Location;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.message.Error;
import org.mule.runtime.core.api.construct.Flow;
import org.mule.runtime.extension.api.runtime.config.ConfigurationState;
import org.mule.runtime.extension.api.runtime.config.ConfiguredComponent;
import org.mule.runtime.extension.api.runtime.source.ParameterizedSource;
import org.mule.tck.probe.JUnitLambdaProbe;
import org.mule.tck.probe.PollingProber;
import org.mule.test.heisenberg.extension.HeisenbergSource;
import org.mule.test.module.extension.AbstractExtensionFunctionalTestCase;
import org.mule.tests.api.TestQueueManager;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;

import org.hamcrest.Matcher;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import io.qameta.allure.Issue;

public class HeisenbergMessageSourceTestCase extends AbstractExtensionFunctionalTestCase {

  public static final int TIMEOUT_MILLIS = 30000;
  public static final int POLL_DELAY_MILLIS = 300;
  public static final int TIME_WAIT_MILLIS = 3000;
  public static final int FLOW_STOP_TIMEOUT = 2000;

  private static final String OUT = "out";

  @Inject
  private TestQueueManager queueManager;

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private Flow flow;

  @Override
  protected String getConfigFile() {
    return "heisenberg-source-config.xml";
  }

  @Override
  protected void doSetUp() throws Exception {
    sourceTimesStarted = 0;
    resetHeisenbergSource();
    super.doSetUp();
  }

  @Override
  protected void doTearDown() throws Exception {
    if (flow != null) {
      requestFlowToStopAndWait(flow.getName());
    }

    super.doTearDown();
    resetHeisenbergSource();
  }

  @Test
  public void source() throws Exception {
    requestFlowToStartAndWait("source");

    assertSourceCompleted();
  }

  @Test
  public void sourceRestartedWithDynamicConfig() throws Exception {
    final Long gatheredMoney = HeisenbergSource.gatheredMoney;
    requestFlowToStartAndWait("source");

    check(TIMEOUT_MILLIS, POLL_DELAY_MILLIS,
          () -> {
            assertThat(HeisenbergSource.gatheredMoney, greaterThan(gatheredMoney));
            return true;
          });

    requestFlowToStopAndWait("source");

    final Long gatheredMoneyAfterStop = HeisenbergSource.gatheredMoney;

    // Check that money is NOT gathered while flow is stopped
    checkNot(TIME_WAIT_MILLIS, POLL_DELAY_MILLIS,
             () -> {
               assertThat(HeisenbergSource.gatheredMoney, greaterThan(gatheredMoneyAfterStop));
               return true;
             });

    requestFlowToStartAndWait("source");

    // Check that money is gathered after flow is restarted
    check(TIMEOUT_MILLIS, POLL_DELAY_MILLIS,
          () -> {
            assertThat(HeisenbergSource.gatheredMoney, greaterThan(gatheredMoneyAfterStop));
            return true;
          });
  }

  protected void assertSourceCompleted() {
    probe(TIMEOUT_MILLIS, POLL_DELAY_MILLIS, () -> HeisenbergSource.gatheredMoney > 100
        && HeisenbergSource.receivedGroupOnSource
        && HeisenbergSource.receivedInlineOnSuccess);
  }

  @Test
  public void onException() throws Exception {
    requestFlowToStartAndWait("sourceFailed");

    assertSourceFailed();
  }

  protected void assertSourceFailed() {
    probe(TIMEOUT_MILLIS, POLL_DELAY_MILLIS,
          () -> HeisenbergSource.gatheredMoney == -1
              && HeisenbergSource.receivedGroupOnSource
              && HeisenbergSource.receivedInlineOnError);
  }

  @Test
  public void enrichExceptionOnStart() throws Exception {
    expectedException.expectMessage(ENRICHED_MESSAGE + CORE_POOL_SIZE_ERROR_MESSAGE);
    requestFlowToStartAndWait("sourceFailedOnStart");
  }

  @Test
  public void reconnectWithEnrichedException() throws Exception {
    requestFlowToStartAndWait("sourceFailedOnRuntime");
    probe(TIMEOUT_MILLIS, POLL_DELAY_MILLIS, () -> sourceTimesStarted > 2);
  }

  @Test
  public void sourceOnSuccessCallsOnTerminate() throws Exception {
    requestFlowToStartAndWait("source");

    probe(TIMEOUT_MILLIS, POLL_DELAY_MILLIS,
          () -> assertState(true, false, true));

    assertThat(HeisenbergSource.terminateStatus, is(SUCCESS));
    assertThat(HeisenbergSource.error, empty());

  }

  @Test
  public void sourceFailsOnSuccessParametersCallsOnErrorAndOnTerminate() throws Exception {
    requestFlowToStartAndWait("sourceWithInvalidSuccessParameter");

    probe(TIMEOUT_MILLIS, POLL_DELAY_MILLIS,
          () -> assertState(false, true, true));

    assertThat(HeisenbergSource.terminateStatus, is(ERROR_INVOKE));
    assertThat(HeisenbergSource.error, not(empty()));

    assertThat(queueManager.read(OUT, RECEIVE_TIMEOUT, MILLISECONDS).getMessage(), hasPayload(equalTo("Expected.")));
  }

  @Test
  public void sourceFailsOnSuccessBodyCallsOnErrorAndOnTerminate() throws Exception {
    requestFlowToStartAndWait("sourceFailsOnSuccessBodyCallsOnErrorAndOnTerminate");

    probe(TIMEOUT_MILLIS, POLL_DELAY_MILLIS,
          () -> assertState(true, true, true));

    assertThat(HeisenbergSource.terminateStatus, is(ERROR_BODY));
    assertThat(HeisenbergSource.error, not(empty()));

    assertThat(queueManager.read(OUT, RECEIVE_TIMEOUT, MILLISECONDS).getMessage(), hasPayload(equalTo("Expected.")));
  }

  @Test
  public void sourceFailsOnSuccessAndOnErrorParametersCallsOnTerminate() throws Exception {
    requestFlowToStartAndWait("sourceWithInvalidSuccessAndErrorParameters");

    probe(TIMEOUT_MILLIS, POLL_DELAY_MILLIS,
          () -> assertState(false, false, true));

    assertThat(HeisenbergSource.terminateStatus, is(ERROR_INVOKE));

    Optional<Error> optionalError = HeisenbergSource.error;
    assertThat(optionalError, is(not(empty())));
    assertThat(optionalError.get().getErrorType(), is(errorType(SOURCE_ERROR_RESPONSE_GENERATE)));

    MuleException me = (MuleException) unwrap(optionalError.get().getCause());
    assertThat((String) me.getInfo().get(INFO_LOCATION_KEY),
               containsString("sourceWithInvalidSuccessAndErrorParameters/source"));
    assertThat((String) me.getInfo().get(INFO_SOURCE_XML_KEY), containsString("heisenberg:success-info"));
  }

  @Test
  public void sourceFailsInsideOnErrorAndCallsOnTerminate() throws Exception {
    requestFlowToStartAndWait("sourceFailsInsideOnError");

    probe(TIMEOUT_MILLIS, POLL_DELAY_MILLIS, () -> assertState(false, true, true));

    assertThat(HeisenbergSource.terminateStatus, is(ERROR_BODY));

    Optional<Error> optionalError = HeisenbergSource.error;
    assertThat(optionalError, is(not(empty())));
    assertThat(optionalError.get().getErrorType(), is(errorType(SOURCE_ERROR_RESPONSE_SEND)));
  }

  @Test
  public void failureInFlowCallsOnErrorDirectlyAndHandlesItCorrectly() throws Exception {
    requestFlowToStartAndWait("failureInFlowCallsOnErrorDirectly");
    probe(TIMEOUT_MILLIS, POLL_DELAY_MILLIS, () -> assertState(false, true, true));
  }

  @Test
  public void failureInFlowErrorHandlerCallsOnErrorDirectlyAndHandlesItCorrectly() throws Exception {
    requestFlowToStartAndWait("failureInFlowErrorHandlerCallsOnErrorDirectly");
    probe(TIMEOUT_MILLIS, POLL_DELAY_MILLIS, () -> assertState(false, true, true));
  }

  @Test
  public void failureInFlowCallsOnErrorDirectlyAndFailsHandlingIt() throws Exception {
    requestFlowToStartAndWait("failureInFlowCallsOnErrorDirectlyAndFailsHandlingIt");
    probe(TIMEOUT_MILLIS, POLL_DELAY_MILLIS, () -> assertState(false, false, true));

    assertThat(HeisenbergSource.terminateStatus, is(ERROR_INVOKE));

    Optional<Error> optionalError = HeisenbergSource.error;
    assertThat(optionalError, is(not(empty())));
    assertThat(optionalError.get().getErrorType(), is(errorType(SOURCE_ERROR_RESPONSE_GENERATE)));

    MuleException me = (MuleException) unwrap(optionalError.get().getCause());
    assertThat((String) me.getInfo().get(INFO_LOCATION_KEY),
               containsString("failureInFlowCallsOnErrorDirectlyAndFailsHandlingIt/source"));
    assertThat((String) me.getInfo().get(INFO_SOURCE_XML_KEY), containsString("heisenberg:success-info"));
  }

  @Test
  public void obtainSourceParameters() {
    Component element = locator.find(Location.builder().globalName("source").addSourcePart().build()).get();
    assertThat(element, is(instanceOf(ParameterizedSource.class)));

    ParameterizedSource source = (ParameterizedSource) element;
    Map<String, Object> parameters = source.getInitialisationParameters();
    assertThat(parameters.get("initialBatchNumber"), is(0));
    assertThat(parameters.get("corePoolSize"), is(1));
  }

  @Test
  public void obtainSourceConfigParameters() {
    Component element = locator.find(Location.builder().globalName("source").addSourcePart().build()).get();
    assertThat(element, is(instanceOf(ConfiguredComponent.class)));

    ConfiguredComponent source = (ConfiguredComponent) element;
    ConfigurationState configurationState = source.getConfigurationInstance().get().getState();

    Map<String, Object> configParameters = configurationState.getConfigParameters();

    assertThat(configParameters.size(), is(14));
    assertParameter(configParameters, "monthlyIncomes", hasSize(2));
    assertParameter(configParameters, "cancer", is(true));
    assertParameter(configParameters, "money", equalTo(new BigDecimal("0")));
    assertParameter(configParameters, "initialHealth", is(CANCER));
    assertParameter(configParameters, "endingHealth", is(CANCER));
    assertParameter(configParameters, "name", is("Heisenberg"));
    assertParameter(configParameters, "age", is(50));
    assertParameter(configParameters, "brotherInLaw", is(notNullValue()));

    Map<String, Object> connectionParameters = configurationState.getConnectionParameters();
    assertThat(connectionParameters.size(), is(2));
    assertParameter(connectionParameters, "saulPhoneNumber", equalTo(SAUL_OFFICE_NUMBER));
  }

  @Test
  public void componentLocationInjected() throws Exception {
    requestFlowToStartAndWait("source");
    assertThat(HeisenbergSource.location, is("source/source"));
  }

  @Test
  public void configNameInjected() throws Exception {
    requestFlowToStartAndWait("source");
    assertThat(HeisenbergSource.configName, is("heisenberg"));
  }

  @Test
  @Issue("MULE-18759")
  public void sameChildInBothCallbacks() throws Exception {
    requestFlowToStartAndWait("sameChildInBothCallbacks");

    assertSourceCompleted();
  }

  private void assertParameter(Map<String, Object> parameters, String propertyName, Matcher matcher) {
    assertThat(parameters.get(propertyName), matcher);
  }

  protected void startFlow(String flowName) throws Exception {
    flow = (Flow) getFlowConstruct(flowName);
    flow.start();
  }

  protected void stopFlow(String flowName) throws Exception {
    flow = (Flow) getFlowConstruct(flowName);
    flow.stop();
  }

  protected void requestFlowToStopAndWait(String flowName) throws Exception {
    stopFlow(flowName);
    checkFlowIsStopped(flowName);
  }

  protected void checkFlowIsStopped(String flowName) throws Exception {
    flow = (Flow) getFlowConstruct(flowName);
    new PollingProber(FLOW_STOP_TIMEOUT, POLL_DELAY_MILLIS)
        .check(new JUnitLambdaProbe(() -> flow.getLifecycleState().isStopped(),
                                    "The flow did not stop in a reasonable amount of time"));
  }

  protected void requestFlowToStartAndWait(String flowName) throws Exception {
    startFlow(flowName);
    checkFlowIsStarted(flowName);
  }

  private void checkFlowIsStarted(String flowName) throws Exception {
    flow = (Flow) getFlowConstruct(flowName);
    new PollingProber(FLOW_STOP_TIMEOUT, POLL_DELAY_MILLIS)
        .check(new JUnitLambdaProbe(() -> flow.getLifecycleState().isStarted(),
                                    "The flow did not start in a reasonable amount of time"));
  }

  private boolean assertState(boolean executedOnSuccess, boolean executedOnError, boolean executedOnTerminate) {
    assertThat("OnSuccess", HeisenbergSource.executedOnSuccess, is(executedOnSuccess));
    assertThat("OnError", HeisenbergSource.executedOnError, is(executedOnError));
    assertThat("OnTerminate", HeisenbergSource.executedOnTerminate, is(executedOnTerminate));

    return true;
  }
}
