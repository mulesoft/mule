/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.integration.update;

import static org.junit.Assume.assumeThat;
import static org.mule.extension.db.integration.DbTestUtil.selectData;
import static org.mule.extension.db.integration.TestRecordUtil.assertRecords;
import org.mule.extension.db.api.StatementResult;
import org.mule.extension.db.integration.AbstractDbIntegrationTestCase;
import org.mule.extension.db.integration.matcher.SupportsReturningStoredProcedureResultsWithoutParameters;
import org.mule.extension.db.integration.model.DerbyTestDatabase;
import org.mule.extension.db.integration.model.Field;
import org.mule.extension.db.integration.model.Record;
import org.mule.runtime.api.message.Message;

import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

public class UpdateStoredProcedure extends AbstractDbIntegrationTestCase {

  @Override
  protected String[] getFlowConfigurationResources() {
    return new String[] {"integration/update/update-stored-procedure-config.xml"};
  }

  @Before
  public void setupStoredProcedure() throws Exception {
    assumeThat(getDefaultDataSource(), new SupportsReturningStoredProcedureResultsWithoutParameters());
    testDatabase.createStoredProcedureUpdateTestType1(getDefaultDataSource());
  }

  @Test
  public void testRequestResponse() throws Exception {
    Message response = flowRunner("updateStoredProcedure").run().getMessage();

    assertAffectedRows((StatementResult) response.getPayload().getValue(), testDatabase instanceof DerbyTestDatabase ? 0 : 1);
    List<Map<String, String>> result = selectData("select * from PLANET where POSITION=4", getDefaultDataSource());
    assertRecords(result, new Record(new Field("NAME", "Mercury"), new Field("POSITION", 4)));
  }
}
