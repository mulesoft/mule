/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.db.integration;

import static junit.framework.TestCase.fail;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import org.mule.runtime.core.api.MessagingException;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.module.db.integration.model.AbstractTestDatabase;

import java.util.List;

import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runners.Parameterized;

public abstract class AbstractQueryTimeoutTestCase extends AbstractDbIntegrationTestCase {

  public static final String QUERY_TIMEOUT_FLOW = "queryTimeout";

  public AbstractQueryTimeoutTestCase(String dataSourceConfigResource, AbstractTestDatabase testDatabase) {
    super(dataSourceConfigResource, testDatabase);
  }

  @Parameterized.Parameters
  public static List<Object[]> parameters() {
    return TestDbConfig.getResources();
  }

  /**
   * Verifies that queryTimeout is used and query execution is aborted with an error. As different DB drivers thrown different
   * type of exceptions instead of throwing SQLTimeoutException, the test firsts executes the flow using no timeout, which must
   * pass, and then using a timeout which must fail. Because the first execution was successful is assumed that the error is
   * because of an aborted execution.
   * 
   * @throws Exception
   */
  @Test
  public void timeoutsQuery() throws Exception {
    MuleEvent responseEvent = flowRunner(QUERY_TIMEOUT_FLOW).withPayload(0).run();

    MuleMessage response = responseEvent.getMessage();
    assertThat(responseEvent.getError(), is(Matchers.nullValue()));
    assertThat(response.getPayload(), is(notNullValue()));

    try {
      flowRunner(QUERY_TIMEOUT_FLOW).withPayload(5).run();
      fail("Expected query to timeout");
    } catch (MessagingException e) {
      // Expected
    }
  }

  @Before
  public void setupDelayFunction() throws Exception {
    testDatabase.createDelayFunction(getDefaultDataSource());
  }
}
