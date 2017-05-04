/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension;

import static org.mule.test.heisenberg.extension.HeisenbergExtension.sourceTimesStarted;
import static org.mule.test.heisenberg.extension.HeisenbergSource.CORE_POOL_SIZE_ERROR_MESSAGE;
import static org.mule.test.heisenberg.extension.exception.HeisenbergConnectionExceptionEnricher.ENRICHED_MESSAGE;
import org.mule.runtime.core.api.construct.Flow;
import org.mule.tck.probe.JUnitLambdaProbe;
import org.mule.tck.probe.PollingProber;
import org.mule.test.heisenberg.extension.HeisenbergSource;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class HeisenbergMessageSourceTestCase extends AbstractExtensionFunctionalTestCase {

  public static final int TIMEOUT_MILLIS = 5000;
  public static final int POLL_DELAY_MILLIS = 100;

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
    HeisenbergSource.gatheredMoney = 0;
  }

  @Test
  public void source() throws Exception {
    startFlow("source");
    new PollingProber(TIMEOUT_MILLIS, POLL_DELAY_MILLIS)
        .check(new JUnitLambdaProbe(() -> HeisenbergSource.gatheredMoney > 100
            && HeisenbergSource.receivedGroupOnSource
            && HeisenbergSource.receivedInlineOnSuccess));
  }

  @Test
  public void onException() throws Exception {
    startFlow("sourceFailed");
    new PollingProber(TIMEOUT_MILLIS, POLL_DELAY_MILLIS)
        .check(new JUnitLambdaProbe(() -> HeisenbergSource.gatheredMoney == -1
            && HeisenbergSource.receivedGroupOnSource
            && HeisenbergSource.receivedInlineOnError));
  }

  @Test
  public void enrichExceptionOnStart() throws Exception {
    expectedException.expectMessage(ENRICHED_MESSAGE + CORE_POOL_SIZE_ERROR_MESSAGE);
    startFlow("sourceFailedOnStart");
  }

  @Test
  public void reconnectWithEnrichedException() throws Exception {
    startFlow("sourceFailedOnRuntime");
    new PollingProber(TIMEOUT_MILLIS, POLL_DELAY_MILLIS).check(new JUnitLambdaProbe(() -> sourceTimesStarted > 2));
  }

  private void startFlow(String flowName) throws Exception {
    ((Flow) getFlowConstruct(flowName)).start();
  }
}
