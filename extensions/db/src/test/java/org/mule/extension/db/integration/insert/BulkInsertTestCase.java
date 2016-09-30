/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.extension.db.integration.insert;

import static java.sql.Statement.SUCCESS_NO_INFO;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.core.AnyOf.anyOf;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mule.extension.db.integration.TestDbConfig.getResources;
import org.mule.extension.db.integration.AbstractDbIntegrationTestCase;
import org.mule.extension.db.integration.model.AbstractTestDatabase;
import org.mule.runtime.api.message.Message;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.junit.runners.Parameterized;

public class BulkInsertTestCase extends AbstractDbIntegrationTestCase {

  public BulkInsertTestCase(String dataSourceConfigResource, AbstractTestDatabase testDatabase) {
    super(dataSourceConfigResource, testDatabase);
  }

  @Parameterized.Parameters
  public static List<Object[]> parameters() {
    return getResources();
  }

  @Override
  protected String[] getFlowConfigurationResources() {
    return new String[] {"integration/insert/bulk-insert-config.xml"};
  }

  @Test
  public void dynamicBulkInsert() throws Exception {
    Message response = flowRunner("bulkInsert").withPayload(values()).run().getMessage();
    assertBulkInsert(response.getPayload().getValue());
  }

  @Test
  public void bulkInsertWithOverriddenType() throws Exception {
    Message response = flowRunner("bulkInsertWithOverriddenType").withPayload(values()).run().getMessage();
    assertBulkInsert(response.getPayload().getValue());
  }

  private List<Map<String, Object>> values() {
    List<Map<String, Object>> values = new ArrayList<>();
    addRecord(values, "Pluto", 777);
    addRecord(values, "Saturn", 777);
    return values;
  }

  private void addRecord(List<Map<String, Object>> values, String planetName, int position) {
    Map<String, Object> record = new HashMap<>();
    record.put("name", planetName);
    record.put("position", position);
    values.add(record);
  }

  private void assertBulkInsert(Object payload) throws SQLException {
    assertTrue(payload instanceof int[]);
    int[] counters = (int[]) payload;
    assertThat(counters.length, is(2));
    assertThat(counters[0], anyOf(equalTo(1), equalTo(SUCCESS_NO_INFO)));
    assertThat(counters[1], anyOf(equalTo(1), equalTo(SUCCESS_NO_INFO)));
    assertPlanetRecordsFromQuery("Pluto", "Saturn");
  }
}
