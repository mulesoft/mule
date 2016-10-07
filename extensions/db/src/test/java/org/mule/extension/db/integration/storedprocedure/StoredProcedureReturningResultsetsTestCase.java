/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.extension.db.integration.storedprocedure;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assume.assumeThat;
import static org.mule.extension.db.integration.TestRecordUtil.assertRecords;
import static org.mule.extension.db.integration.TestRecordUtil.getEarthRecord;
import static org.mule.extension.db.integration.TestRecordUtil.getMarsRecord;
import static org.mule.extension.db.integration.TestRecordUtil.getVenusRecord;
import org.mule.extension.db.integration.matcher.SupportsReturningStoredProcedureResultsWithoutParameters;
import org.mule.extension.db.integration.model.MySqlTestDatabase;
import org.mule.runtime.api.message.Message;

import java.util.Map;

public class StoredProcedureReturningResultsetsTestCase extends AbstractStoredProcedureReturningStreamingResultsetsTestCase {

  @Override
  protected String[] getFlowConfigurationResources() {
    return new String[] {"integration/storedprocedure/stored-procedure-returning-resultsets-config.xml"};
  }

  @Override
  protected void assertResponse(Message response) {
    Map payload = (Map) response.getPayload().getValue();

    if (testDatabase instanceof MySqlTestDatabase) {
      assertThat(payload.size(), equalTo(3));
      assertThat(payload.get("updateCount1"), equalTo(0));
    } else {
      assertThat(payload.size(), equalTo(2));
    }

    assertRecords(payload.get("resultSet1"), getVenusRecord());
    assertRecords(payload.get("resultSet2"), getEarthRecord(), getMarsRecord());
  }

  @Override
  public void setupStoredProcedure() throws Exception {
    assumeThat(getDefaultDataSource(), new SupportsReturningStoredProcedureResultsWithoutParameters());
    super.setupStoredProcedure();
  }
}
