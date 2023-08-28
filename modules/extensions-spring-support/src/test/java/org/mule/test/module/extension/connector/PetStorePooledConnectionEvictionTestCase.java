/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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

  private static int AMOUNT_OF_TRIES = 2;
  private static int TIME_BETWEEN_TRIES_IN_MILLIS = 4000;
  private static Long IMPOSSIBLE_CONNECTION_AGE = 3000L;

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
