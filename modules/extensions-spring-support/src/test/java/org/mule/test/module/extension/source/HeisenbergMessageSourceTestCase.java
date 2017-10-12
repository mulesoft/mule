/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension.source;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static org.mule.functional.junit4.matchers.MessageMatchers.hasPayload;
import static org.mule.runtime.api.exception.MuleException.INFO_LOCATION_KEY;
import static org.mule.runtime.api.exception.MuleException.INFO_SOURCE_XML_KEY;
import static org.mule.runtime.core.api.exception.Errors.ComponentIdentifiers.Handleable.SOURCE_ERROR_RESPONSE_GENERATE;
import static org.mule.runtime.core.api.exception.Errors.ComponentIdentifiers.Handleable.SOURCE_ERROR_RESPONSE_SEND;
import static org.mule.runtime.core.api.rx.Exceptions.unwrap;
import static org.mule.tck.junit4.matcher.ErrorTypeMatcher.errorType;
import static org.mule.tck.junit4.matcher.IsEmptyOptional.empty;
import static org.mule.tck.probe.PollingProber.probe;
import static org.mule.test.heisenberg.extension.HeisenbergConnectionProvider.SAUL_OFFICE_NUMBER;
import static org.mule.test.heisenberg.extension.HeisenbergExtension.sourceTimesStarted;
import static org.mule.test.heisenberg.extension.HeisenbergSource.CORE_POOL_SIZE_ERROR_MESSAGE;
import static org.mule.test.heisenberg.extension.HeisenbergSource.TerminateStatus.ERROR_BODY;
import static org.mule.test.heisenberg.extension.HeisenbergSource.TerminateStatus.ERROR_INVOKE;
import static org.mule.test.heisenberg.extension.HeisenbergSource.TerminateStatus.SUCCESS;
import static org.mule.test.heisenberg.extension.exception.HeisenbergConnectionExceptionEnricher.ENRICHED_MESSAGE;
import static org.mule.test.heisenberg.extension.model.HealthStatus.CANCER;
import org.mule.functional.api.component.TestConnectorQueueHandler;
import org.mule.runtime.api.component.Component;
import org.mule.runtime.api.component.location.Location;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.message.Error;
import org.mule.runtime.core.api.construct.Flow;
import org.mule.runtime.extension.api.runtime.config.ConfigurationState;
import org.mule.runtime.extension.api.runtime.config.ConfiguredComponent;
import org.mule.runtime.extension.api.runtime.source.ParameterizedSource;
import org.mule.test.heisenberg.extension.HeisenbergSource;
import org.mule.test.module.extension.AbstractExtensionFunctionalTestCase;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

import org.hamcrest.Matcher;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class HeisenbergMessageSourceTestCase extends AbstractExtensionFunctionalTestCase {

  public static final int TIMEOUT_MILLIS = 50000;
  public static final int POLL_DELAY_MILLIS = 100;

  private static final String OUT = "out";

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Override
  protected String getConfigFile() {
    return "heisenberg-source-config.xml";
  }

  @Override
  protected void doSetUp() throws Exception {
    sourceTimesStarted = 0;
    reset();
  }

  @Override
  protected void doTearDown() throws Exception {
    reset();
  }

  private void reset() {
    HeisenbergSource.receivedGroupOnSource = false;
    HeisenbergSource.receivedInlineOnError = false;
    HeisenbergSource.receivedInlineOnSuccess = false;
    HeisenbergSource.executedOnSuccess = false;
    HeisenbergSource.executedOnError = false;
    HeisenbergSource.executedOnTerminate = false;
    HeisenbergSource.error = Optional.empty();
    HeisenbergSource.gatheredMoney = 0;
    HeisenbergSource.configName = null;
    HeisenbergSource.location = null;
  }

  @Test
  public void source() throws Exception {
    startFlow("source");

    assertSourceCompleted();
  }

  protected void assertSourceCompleted() {
    probe(TIMEOUT_MILLIS, POLL_DELAY_MILLIS, () -> HeisenbergSource.gatheredMoney > 100
        && HeisenbergSource.receivedGroupOnSource
        && HeisenbergSource.receivedInlineOnSuccess);
  }

  @Test
  public void onException() throws Exception {
    startFlow("sourceFailed");
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
    startFlow("sourceFailedOnStart");
  }

  @Test
  public void reconnectWithEnrichedException() throws Exception {
    startFlow("sourceFailedOnRuntime");
    probe(TIMEOUT_MILLIS, POLL_DELAY_MILLIS, () -> sourceTimesStarted > 2);
  }

  @Test
  public void sourceOnSuccessCallsOnTerminate() throws Exception {
    startFlow("source");

    probe(TIMEOUT_MILLIS, POLL_DELAY_MILLIS,
          () -> assertState(true, false, true));

    assertThat(HeisenbergSource.terminateStatus, is(SUCCESS));
    assertThat(HeisenbergSource.error, empty());
  }

  @Test
  public void sourceFailsOnSuccessParametersCallsOnErrorAndOnTerminate() throws Exception {
    startFlow("sourceWithInvalidSuccessParameter");

    probe(TIMEOUT_MILLIS, POLL_DELAY_MILLIS,
          () -> assertState(false, true, true));

    assertThat(HeisenbergSource.terminateStatus, is(ERROR_INVOKE));
    assertThat(HeisenbergSource.error, not(empty()));

    TestConnectorQueueHandler queueHandler = new TestConnectorQueueHandler(registry);
    assertThat(queueHandler.read(OUT, RECEIVE_TIMEOUT).getMessage(), hasPayload(equalTo("Expected.")));
  }

  @Test
  public void sourceFailsOnSuccessBodyCallsOnErrorAndOnTerminate() throws Exception {
    startFlow("sourceFailsOnSuccessBodyCallsOnErrorAndOnTerminate");

    probe(TIMEOUT_MILLIS, POLL_DELAY_MILLIS,
          () -> assertState(true, true, true));

    assertThat(HeisenbergSource.terminateStatus, is(ERROR_BODY));
    assertThat(HeisenbergSource.error, not(empty()));

    TestConnectorQueueHandler queueHandler = new TestConnectorQueueHandler(registry);
    assertThat(queueHandler.read(OUT, RECEIVE_TIMEOUT).getMessage(), hasPayload(equalTo("Expected.")));
  }

  @Test
  public void sourceFailsOnSuccessAndOnErrorParametersCallsOnTerminate() throws Exception {
    startFlow("sourceWithInvalidSuccessAndErrorParameters");

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
    startFlow("sourceFailsInsideOnError");

    probe(TIMEOUT_MILLIS, POLL_DELAY_MILLIS, () -> assertState(false, true, true));

    assertThat(HeisenbergSource.terminateStatus, is(ERROR_BODY));

    Optional<Error> optionalError = HeisenbergSource.error;
    assertThat(optionalError, is(not(empty())));
    assertThat(optionalError.get().getErrorType(), is(errorType(SOURCE_ERROR_RESPONSE_SEND)));
  }

  @Test
  public void failureInFlowCallsOnErrorDirectlyAndHandlesItCorrectly() throws Exception {
    startFlow("failureInFlowCallsOnErrorDirectly");
    probe(TIMEOUT_MILLIS, POLL_DELAY_MILLIS, () -> assertState(false, true, true));
  }

  @Test
  public void failureInFlowCallsOnErrorDirectlyAndFailsHandlingIt() throws Exception {
    startFlow("failureInFlowCallsOnErrorDirectlyAndFailsHandlingIt");
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
  public void obtainSourceParameters() throws Exception {
    Component element = locator.find(Location.builder().globalName("source").addSourcePart().build()).get();
    assertThat(element, is(instanceOf(ParameterizedSource.class)));

    ParameterizedSource source = (ParameterizedSource) element;
    Map<String, Object> parameters = source.getInitialisationParameters();
    assertThat(parameters.get("initialBatchNumber"), is(0));
    assertThat(parameters.get("corePoolSize"), is(1));
  }

  @Test
  public void obtainSourceConfigParameters() throws Exception {
    Component element = locator.find(Location.builder().globalName("source").addSourcePart().build()).get();
    assertThat(element, is(instanceOf(ConfiguredComponent.class)));

    ConfiguredComponent source = (ConfiguredComponent) element;
    ConfigurationState configurationState = source.getConfigurationInstance().get().getState();

    Map<String, Object> configParameters = configurationState.getConfigParameters();

    assertThat(configParameters.size(), is(13));
    assertParameter(configParameters, "enemies", hasSize(0));
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
    startFlow("source");
    assertThat(HeisenbergSource.location, is("source/source"));
  }

  @Test
  public void configNameInjected() throws Exception {
    startFlow("source");
    assertThat(HeisenbergSource.configName, is("heisenberg"));
  }

  private void assertParameter(Map<String, Object> parameters, String propertyName, Matcher matcher) {
    assertThat(parameters.get(propertyName), matcher);
  }

  protected void startFlow(String flowName) throws Exception {
    ((Flow) getFlowConstruct(flowName)).start();
  }

  private boolean assertState(boolean executedOnSuccess, boolean executedOnError, boolean executedOnTerminate) {
    return HeisenbergSource.executedOnSuccess == executedOnSuccess
        && HeisenbergSource.executedOnError == executedOnError
        && HeisenbergSource.executedOnTerminate == executedOnTerminate;
  }
}
