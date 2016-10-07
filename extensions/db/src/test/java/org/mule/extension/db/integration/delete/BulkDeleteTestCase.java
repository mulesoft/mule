/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.extension.db.integration.delete;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mule.extension.db.integration.DbTestUtil.assertExpectedUpdateCount;
import static org.mule.extension.db.integration.model.Planet.MARS;
import static org.mule.extension.db.integration.model.Planet.VENUS;

import org.mule.extension.db.integration.AbstractDbIntegrationTestCase;
import org.mule.runtime.api.message.Message;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

public class BulkDeleteTestCase extends AbstractDbIntegrationTestCase {

  @Override
  protected String[] getFlowConfigurationResources() {
    return new String[] {"integration/delete/bulk-delete-config.xml"};
  }

  @Test
  public void bulkDelete() throws Exception {
    Message response = flowRunner("bulkDelete").withPayload(values()).run().getMessage();
    assertBulkDelete(response);
  }

  @Test
  public void bulkDeleteWithOverriddenType() throws Exception {
    Message response = flowRunner("bulkDeleteWithOverriddenType").withPayload(values()).run().getMessage();
    assertBulkDelete(response);
  }

  private List<Map<String, Object>> values() {
    List<Map<String, Object>> values = new ArrayList<>();
    addRecord(values, "name", VENUS.getName());
    addRecord(values, "name", MARS.getName());

    return values;
  }

  private void addRecord(List<Map<String, Object>> values, String key, Object value) {
    Map<String, Object> record = new HashMap<>();
    record.put(key, value);
    values.add(record);
  }

  private void assertBulkDelete(Message response) throws SQLException {
    assertTrue(response.getPayload().getValue() instanceof int[]);
    int[] counters = (int[]) response.getPayload().getValue();
    assertEquals(2, counters.length);
    assertExpectedUpdateCount(1, counters[0]);
    assertExpectedUpdateCount(1, counters[1]);

    assertDeletedPlanetRecords(VENUS.getName());
  }
}
