/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension.source;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mule.tck.junit4.matcher.ErrorTypeMatcher.errorType;
import static org.mule.tck.probe.PollingProber.probe;
import static org.mule.test.heisenberg.extension.HeisenbergExtension.sourceTimesStarted;
import static org.mule.test.heisenberg.extension.HeisenbergSource.CORE_POOL_SIZE_ERROR_MESSAGE;
import static org.mule.test.heisenberg.extension.HeisenbergSource.TerminateStatus.ERROR_BODY;
import static org.mule.test.heisenberg.extension.HeisenbergSource.TerminateStatus.ERROR_PARAMETER;
import static org.mule.test.heisenberg.extension.HeisenbergSource.TerminateStatus.SUCCESS;
import static org.mule.test.heisenberg.extension.exception.HeisenbergConnectionExceptionEnricher.ENRICHED_MESSAGE;
import org.mule.runtime.api.message.Error;
import org.mule.runtime.core.api.construct.Flow;
import org.mule.runtime.extension.api.OnTerminateInformation;
import org.mule.test.heisenberg.extension.HeisenbergSource;
import org.mule.test.module.extension.AbstractExtensionFunctionalTestCase;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.Optional;

public class HeisenbergMessageSourceTestCase extends AbstractExtensionFunctionalTestCase {

  public static final int TIMEOUT_MILLIS = 5000;
  public static final int POLL_DELAY_MILLIS = 100;
  public static final String SOURCE_RESPONSE_PARAMETERS = "SOURCE_RESPONSE_PARAMETERS";
  public static final String MULE = "MULE";

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Override
  protected String getConfigFile() {
    return "heisenberg-source-config.xml";
  }

  @Before
  public void setUp() throws Exception {
    sourceTimesStarted = 0;
    HeisenbergSource.receivedGroupOnSource = false;
    HeisenbergSource.receivedInlineOnError = false;
    HeisenbergSource.receivedInlineOnSuccess = false;
    HeisenbergSource.receivedOnTerminateInfo = false;
    HeisenbergSource.executedOnSuccess = false;
    HeisenbergSource.executedOnError = false;
    HeisenbergSource.executedOnTerminate = false;
    HeisenbergSource.gatheredMoney = 0;
  }

  @Test
  public void source() throws Exception {
    startFlow("source");

    probe(TIMEOUT_MILLIS, POLL_DELAY_MILLIS, () -> HeisenbergSource.gatheredMoney > 100
        && HeisenbergSource.receivedGroupOnSource
        && HeisenbergSource.receivedInlineOnSuccess);
  }

  @Test
  public void onException() throws Exception {
    startFlow("sourceFailed");

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
  public void sourceOnSuccessCallsOnTerminate() throws Exception {
    startFlow("source");

    probe(TIMEOUT_MILLIS, POLL_DELAY_MILLIS,
          () -> HeisenbergSource.executedOnSuccess
              && !HeisenbergSource.executedOnError
              && HeisenbergSource.executedOnTerminate
              && HeisenbergSource.receivedOnTerminateInfo);

    assertThat(HeisenbergSource.terminateStatus, is(SUCCESS));
  }

  @Test
  public void sourceFailsOnSuccessParametersCallsOnErrorAndOnTerminate() throws Exception {
    startFlow("sourceWithInvalidSuccessParameter");

    probe(TIMEOUT_MILLIS, POLL_DELAY_MILLIS,
          () -> !HeisenbergSource.executedOnSuccess
              && HeisenbergSource.executedOnError
              && HeisenbergSource.executedOnTerminate
              && HeisenbergSource.receivedOnTerminateInfo);

    assertThat(HeisenbergSource.terminateStatus, is(ERROR_PARAMETER));

    OnTerminateInformation terminateInfo = HeisenbergSource.onTerminateInfo;
    Optional<Error> optionalError = terminateInfo.getError();
    assertThat(optionalError.isPresent(), is(true));
    Error error = optionalError.get();
    assertThat(error.getErrorType(), is(errorType(MULE, SOURCE_RESPONSE_PARAMETERS)));
  }

  @Test
  public void sourceFailsOnSuccessBodyCallsOnErrorAndOnTerminate() throws Exception {
    startFlow("sourceFailsOnSuccessBodyCallsOnErrorAndOnTerminate");

    probe(TIMEOUT_MILLIS, POLL_DELAY_MILLIS,
          () -> !HeisenbergSource.executedOnSuccess
              && HeisenbergSource.executedOnError
              && HeisenbergSource.executedOnTerminate
              && HeisenbergSource.receivedOnTerminateInfo);

    assertThat(HeisenbergSource.terminateStatus, is(ERROR_PARAMETER));

    OnTerminateInformation terminateInfo = HeisenbergSource.onTerminateInfo;
    Optional<Error> optionalError = terminateInfo.getError();
    assertThat(optionalError.isPresent(), is(true));
    Error error = optionalError.get();
    assertThat(error.getErrorType(), is(errorType(MULE, "SOURCE_ERROR")));
  }

  @Test
  public void sourceFailsOnSuccessAndOnErrorParametersCallsOnTerminate() throws Exception {
    startFlow("sourceWithInvalidSuccessAndErrorParameters");

    probe(TIMEOUT_MILLIS, POLL_DELAY_MILLIS,
          () -> !HeisenbergSource.executedOnSuccess
              && !HeisenbergSource.executedOnError
              && HeisenbergSource.executedOnTerminate
              && HeisenbergSource.receivedOnTerminateInfo);

    assertThat(HeisenbergSource.terminateStatus, is(ERROR_PARAMETER));

    OnTerminateInformation terminateInfo = HeisenbergSource.onTerminateInfo;
    Optional<Error> optionalError = terminateInfo.getError();
    assertThat(optionalError.isPresent(), is(true));
    Error error = optionalError.get();
    assertThat(error.getErrorType(), is(errorType(MULE, SOURCE_RESPONSE_PARAMETERS)));
  }

  @Test
  public void sourceFailsInsideOnErrorAndCallsOnTerminate() throws Exception {
    startFlow("sourceFailsInsideOnError");

    probe(TIMEOUT_MILLIS, POLL_DELAY_MILLIS,
          () -> !HeisenbergSource.executedOnSuccess
              && HeisenbergSource.executedOnError
              && HeisenbergSource.executedOnTerminate
              && HeisenbergSource.receivedOnTerminateInfo);

    assertThat(HeisenbergSource.terminateStatus, is(ERROR_BODY));

    OnTerminateInformation terminateInfo = HeisenbergSource.onTerminateInfo;
    Optional<Error> optionalError = terminateInfo.getError();
    assertThat(optionalError.isPresent(), is(true));
    Error error = optionalError.get();
  }

  @Test
  public void reconnectWithEnrichedException() throws Exception {
    startFlow("sourceFailedOnRuntime");
    probe(TIMEOUT_MILLIS, POLL_DELAY_MILLIS, () -> sourceTimesStarted > 2);
  }

  private void startFlow(String flowName) throws Exception {
    ((Flow) getFlowConstruct(flowName)).start();
  }
}
