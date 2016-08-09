/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.scripting;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.functional.junit4.FunctionalTestCase;

import org.junit.Test;

public class GreedyTestCase extends FunctionalTestCase {

  @Override
  protected String getConfigFile() {
    return "greedy-config.xml";
  }

  @Test
  public void testDollars() throws Exception {
    runTest(2.33, "USD", "[9 quarters, 0 dimes, 1 nickels, 3 pennies]");
  }

  @Test
  public void testPounds() throws Exception {
    runTest(1.28, "GBP", "[1 pounds, 1 twenty_pence, 1 five_pence, 1 two_pence, 1 pennies]");
  }

  private void runTest(double amount, String currency, String expectedResult) throws Exception {
    MuleEvent response = flowRunner("greedy").withPayload(amount * 100).withFlowVariable("currency", currency).run();
    assertThat(getPayloadAsString(response.getMessage()), equalTo(expectedResult));
  }
}
