/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.extension.db.integration.storedprocedure;

import static org.mule.extension.db.integration.TestRecordUtil.assertMessageContains;
import static org.mule.extension.db.integration.TestRecordUtil.getAllPlanetRecords;
import org.mule.extension.db.integration.AbstractDbIntegrationTestCase;
import org.mule.extension.db.integration.model.AbstractTestDatabase;
import org.mule.runtime.api.message.Message;

import org.junit.Before;
import org.junit.Test;

public abstract class AbstractStoredProcedureReturningStreamingResultsetsTestCase extends AbstractDbIntegrationTestCase {

  public AbstractStoredProcedureReturningStreamingResultsetsTestCase(String dataSourceConfigResource,
                                                                     AbstractTestDatabase testDatabase) {
    super(dataSourceConfigResource, testDatabase);
  }

  @Test
  public void getResultSet() throws Exception {
    Message response = flowRunner("getResultSet").run().getMessage();
    assertResponse(response);
  }

  protected void assertResponse(Message response) {
    assertMessageContains(response, getAllPlanetRecords());
  }

  @Before
  public void setupStoredProcedure() throws Exception {
    testDatabase.createStoredProcedureGetSplitRecords(getDefaultDataSource());
  }
}
