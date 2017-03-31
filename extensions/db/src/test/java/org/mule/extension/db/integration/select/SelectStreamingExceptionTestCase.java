/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.integration.select;

import static java.lang.Boolean.FALSE;
import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import org.mule.extension.db.integration.AbstractDbIntegrationTestCase;
import org.mule.extension.db.integration.model.DerbyTestDatabase;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.core.exception.MessagingException;

import java.util.List;

import javax.sql.DataSource;

import org.junit.Test;
import org.junit.runners.Parameterized;

public class SelectStreamingExceptionTestCase extends AbstractDbIntegrationTestCase {

  private static final int POOL_CONNECTIONS = 2;

  @Parameterized.Parameters(name = "{2}")
  public static List<Object[]> parameters() {
    final DerbyTestDatabase derbyTestDatabase = new DerbyTestDatabase();
    return singletonList(new Object[] {"integration/config/derby-pooling-db-config.xml", derbyTestDatabase,
        derbyTestDatabase.getDbType()});
  }

  @Override
  protected String[] getFlowConfigurationResources() {
    return new String[] {"integration/select/select-streaming-exception-config.xml"};
  }

  @Override
  protected DataSource getDefaultDataSource() {
    return getDefaultDataSource("pooledJdbcConfig");
  }

  @Test
  public void streamingException() throws Exception {
    for (int i = 0; i < POOL_CONNECTIONS + 1; ++i) {
      try {
        flowRunner("selectStreamingException").run();
        fail("Expected 'Table does not exist' exception.");
      } catch (MessagingException e) {
        assertThat("Iteration " + i, e.getMessage(), containsString("Table/View 'NOT_EXISTS' does not exist."));
      }
    }
  }

  @Test
  public void selectExceptionClosesPreviousResultSets() throws Exception {
    Message response = flowRunner("selectExceptionClosesPreviousResultSets").run().getMessage();
    assertThat(response.getPayload().getValue(), is(FALSE));
  }
}
