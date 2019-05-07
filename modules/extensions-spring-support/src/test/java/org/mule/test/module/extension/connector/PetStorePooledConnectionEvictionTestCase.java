/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.module.extension.connector;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.not;

import org.mule.test.module.extension.AbstractExtensionFunctionalTestCase;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

public class PetStorePooledConnectionEvictionTestCase extends AbstractExtensionFunctionalTestCase {

  private static int AMOUNT_OF_TRIES = 5;
  private static int TIME_BETWEEN_TRIES_IN_MILLIS = 8000;
  private static Long IMPOSSIBLE_CONNECTION_AGE = 5000L;

  @Override
  protected String getConfigFile() {
    return "petstore-pooled-connection-eviction.xml";
  }

  @Test
  public void connectionIsEvicted() throws Exception {
    List<Long> connectionAgesAtExecution = new ArrayList<>();
    for (int i = 0; i < AMOUNT_OF_TRIES; i++) {
      connectionAgesAtExecution.add((Long) flowRunner("get-connection-age").run().getMessage().getPayload().getValue());
      Thread.sleep(TIME_BETWEEN_TRIES_IN_MILLIS);
    }
    for (Long connectionAgeAtExecution : connectionAgesAtExecution) {
      assertThat(connectionAgeAtExecution, not(greaterThan(IMPOSSIBLE_CONNECTION_AGE)));
    }
  }

}
