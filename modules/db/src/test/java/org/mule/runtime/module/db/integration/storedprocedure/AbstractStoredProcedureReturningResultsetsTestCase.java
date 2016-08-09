/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.db.integration.storedprocedure;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.module.db.integration.TestRecordUtil.assertRecords;
import static org.mule.runtime.module.db.integration.TestRecordUtil.getEarthRecord;
import static org.mule.runtime.module.db.integration.TestRecordUtil.getMarsRecord;
import static org.mule.runtime.module.db.integration.TestRecordUtil.getVenusRecord;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.module.db.integration.AbstractDbIntegrationTestCase;
import org.mule.runtime.module.db.integration.model.AbstractTestDatabase;
import org.mule.runtime.module.db.integration.model.MySqlTestDatabase;

import java.util.Map;

import org.junit.Before;
import org.junit.Test;

public abstract class AbstractStoredProcedureReturningResultsetsTestCase extends AbstractDbIntegrationTestCase {

  public AbstractStoredProcedureReturningResultsetsTestCase(String dataSourceConfigResource, AbstractTestDatabase testDatabase) {
    super(dataSourceConfigResource, testDatabase);
  }

  @Test
  public void testRequestResponse() throws Exception {
    final MuleEvent responseEvent = flowRunner("defaultQueryRequestResponse").withPayload(TEST_MESSAGE).run();

    final MuleMessage response = responseEvent.getMessage();
    Map payload = (Map) response.getPayload();

    if (testDatabase instanceof MySqlTestDatabase) {
      assertThat(payload.size(), equalTo(3));
      assertThat(payload.get("updateCount1"), equalTo(0));
    } else {
      assertThat(payload.size(), equalTo(2));
    }

    assertRecords(payload.get("resultSet1"), getVenusRecord());
    assertRecords(payload.get("resultSet2"), getEarthRecord(), getMarsRecord());
  }

  @Before
  public void setupStoredProcedure() throws Exception {
    testDatabase.createStoredProcedureGetSplitRecords(getDefaultDataSource());
  }
}
