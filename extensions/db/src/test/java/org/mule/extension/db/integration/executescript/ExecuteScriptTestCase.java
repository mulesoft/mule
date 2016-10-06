/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.extension.db.integration.executescript;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mule.extension.db.integration.DbTestUtil.selectData;
import static org.mule.extension.db.integration.TestRecordUtil.assertRecords;
import org.mule.extension.db.integration.AbstractDbIntegrationTestCase;
import org.mule.extension.db.integration.model.Field;
import org.mule.extension.db.integration.model.Record;
import org.mule.runtime.api.message.Message;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import org.junit.Test;

public class ExecuteScriptTestCase extends AbstractDbIntegrationTestCase {

  @Override
  protected String[] getFlowConfigurationResources() {
    return new String[] {"integration/executescript/execute-script-config.xml"};
  }

  @Test
  public void updatesDataRequestResponse() throws Exception {
    Message response = flowRunner("executeScript").run().getMessage();
    assertBulkModeResult(response.getPayload().getValue());
  }

  @Test
  public void executeScriptFromFile() throws Exception {
    Message response = flowRunner("executeScriptFromFile").run().getMessage();
    assertBulkModeResult(response.getPayload().getValue());
  }

  private void assertBulkModeResult(Object payload) throws SQLException {
    assertTrue(payload instanceof int[]);
    int[] counters = (int[]) payload;
    assertEquals(2, counters.length);
    assertEquals(0, counters[0]);
    assertEquals(1, counters[1]);

    List<Map<String, String>> result = selectData("select * from PLANET where POSITION=0 or POSITION=4", getDefaultDataSource());
    assertRecords(result, new Record(new Field("NAME", "Mercury"), new Field("POSITION", 4)));
  }

}
