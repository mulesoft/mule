/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.db.integration.delete;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mule.runtime.module.db.integration.DbTestUtil.assertExpectedUpdateCount;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.module.db.integration.AbstractDbIntegrationTestCase;
import org.mule.runtime.module.db.integration.TestDbConfig;
import org.mule.runtime.module.db.integration.model.AbstractTestDatabase;

import java.sql.SQLException;
import java.util.List;

import org.junit.Test;
import org.junit.runners.Parameterized;

public class DeleteBulkSourceTestCase extends AbstractDbIntegrationTestCase {

  public DeleteBulkSourceTestCase(String dataSourceConfigResource, AbstractTestDatabase testDatabase) {
    super(dataSourceConfigResource, testDatabase);
  }

  @Parameterized.Parameters
  public static List<Object[]> parameters() {
    return TestDbConfig.getResources();
  }

  @Override
  protected String[] getFlowConfigurationResources() {
    return new String[] {"integration/delete/delete-bulk-source-config.xml"};
  }

  @Test
  public void deletesInBulkModeFromCustomSource() throws Exception {
    final MuleEvent responseEvent = flowRunner("deleteBulkCustomSource").withPayload(TEST_MESSAGE).run();

    final MuleMessage response = responseEvent.getMessage();
    assertBulkDelete(response);
  }

  private void assertBulkDelete(MuleMessage response) throws SQLException {
    assertTrue(response.getPayload() instanceof int[]);
    int[] counters = (int[]) response.getPayload();
    assertEquals(2, counters.length);
    assertExpectedUpdateCount(1, counters[0]);
    assertExpectedUpdateCount(1, counters[1]);

    assertDeletedPlanetRecords("Pluto", "Venus");
  }
}
