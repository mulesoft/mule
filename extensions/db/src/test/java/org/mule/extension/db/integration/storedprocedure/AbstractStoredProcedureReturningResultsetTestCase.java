/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.extension.db.integration.storedprocedure;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;
import static org.mule.extension.db.integration.TestRecordUtil.assertRecords;
import static org.mule.extension.db.integration.TestRecordUtil.getAllPlanetRecords;
import org.mule.extension.db.integration.AbstractDbIntegrationTestCase;
import org.mule.extension.db.integration.model.MySqlTestDatabase;

import java.util.Map;

import org.junit.Before;
import org.junit.Test;

public abstract class AbstractStoredProcedureReturningResultsetTestCase extends AbstractDbIntegrationTestCase {

  @Test
  public void testRequestResponse() throws Exception {
    Map<String, Object> payload = runProcedure("getResultSet");
    if (testDatabase instanceof MySqlTestDatabase) {
      assertThat(payload.size(), equalTo(2));
      assertThat(payload.get("updateCount1"), equalTo(0));
    } else {
      assertThat(payload.size(), equalTo(1));
    }

    assertRecords(payload.get("resultSet1"), getAllPlanetRecords());
  }

  @Before
  public void setupStoredProcedure() throws Exception {
    testDatabase.createStoredProcedureGetRecords(getDefaultDataSource());
  }
}
